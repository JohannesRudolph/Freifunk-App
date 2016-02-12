package de.inmotion_sst.freifunkfinder.clustering;

import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.algo.Algorithm;
import com.google.maps.android.clustering.algo.StaticCluster;
import com.google.maps.android.geometry.Bounds;
import com.google.maps.android.geometry.Point;
import com.google.maps.android.projection.SphericalMercatorProjection;
import com.google.maps.android.quadtree.PointQuadTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.inmotion_sst.freifunkfinder.Node;
import java8.util.stream.StreamSupport;

/**
 * A simple clustering algorithm with O(nlog n) performance. Resulting clusters are not
 * hierarchical. This algorithm will compute clusters only in visible area.
 * <p>
 * High level algorithm:<br>
 * 1. Iterate over items in the order they were added (candidate clusters).<br>
 * 2. Create a cluster with the center of the item. <br>
 * 3. Add all items that are within a certain distance to the cluster. <br>
 * 4. Move any items out of an existing cluster if they are closer to another cluster. <br>
 * 5. Remove those items from the list of candidate clusters.
 * <p>
 * Clusters have the center of the first element (not the centroid of the items within it).
 */

// this is based on https://github.com/googlemaps/android-maps-utils/pull/217
public class VisibleNonHierarchicalDistanceBasedAlgorithm<T extends ClusterItem>
        implements Algorithm<T>, GoogleMap.OnCameraChangeListener {

    public static final int MAX_DISTANCE_AT_ZOOM = 100; // essentially 100 dp.

    /**
     * Any modifications should be synchronized on mQuadTree.
     */
    private final Collection<QuadItem<T>> mItems = new ArrayList<QuadItem<T>>();

    // our world is represented in a (0,1)|(0,1) coordinate system

    /**
     * Any modifications should be synchronized on mQuadTree.
     */
    private final PointQuadTree<QuadItem<T>> mQuadTree = new PointQuadTree<QuadItem<T>>(0, 1, 0, 1);

    private static final SphericalMercatorProjection PROJECTION = new SphericalMercatorProjection(1);
    private final int mScreenWidth;
    private final int mScreenHeight;
    private LatLng mMapCenter;

    public VisibleNonHierarchicalDistanceBasedAlgorithm(int screenWidth, int screenHeight) {
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
    }

    @Override
    public void addItem(T item) {
        final QuadItem<T> quadItem = new QuadItem<T>(item);
        synchronized (mQuadTree) {
            mItems.add(quadItem);
            mQuadTree.add(quadItem);
        }
    }

    @Override
    public void addItems(Collection<T> items) {
        for (T item : items) {
            addItem(item);
        }
    }

    @Override
    public void clearItems() {
        synchronized (mQuadTree) {
            mItems.clear();
            mQuadTree.clear();
        }
    }

    @Override
    public void removeItem(T item) {
        // TODO: delegate QuadItem#hashCode and QuadItem#equals to its item.
        throw new UnsupportedOperationException("VisibleNonHierarchicalDistanceBasedAlgorithm.remove not implemented");
    }

    @Override
    public Set<? extends Cluster<T>> getClusters(double zoom) {
        final int discreteZoom = (int) zoom;

        final double zoomSpecificSpan = MAX_DISTANCE_AT_ZOOM / Math.pow(2, discreteZoom) / 256;

        final Set<QuadItem<T>> visitedCandidates = new HashSet<QuadItem<T>>();
        final Set<Cluster<T>> resultClusters = new HashSet<Cluster<T>>();
        final Map<QuadItem<T>, Double> minDistanceToCluster = new HashMap<QuadItem<T>, Double>();
        final Map<QuadItem<T>, StaticCluster<T>> itemToClusterMapping = new HashMap<QuadItem<T>, StaticCluster<T>>();

        synchronized (mQuadTree) {

            Bounds visibleBounds = getVisibleBounds(discreteZoom);

            // first, find all visible nodes
            Collection<QuadItem<T>> visibleNodes = mQuadTree.search(visibleBounds);

            for (QuadItem<T> candidate : visibleNodes) {
                // Candidate is already part of a cluster, nothing to do for it
                if (visitedCandidates.contains(candidate)) {
                    continue;
                }

                // search items close to this node
                Bounds searchBounds = createBoundsFromSpan(candidate.getPoint(), zoomSpecificSpan);
                Collection<QuadItem<T>> nearbyNodes= mQuadTree.search(searchBounds);

                if (nearbyNodes.size() == 1) {
                    // Only the current marker is in range. Just add the single item to the resultClusters.
                    resultClusters.add(candidate);
                    visitedCandidates.add(candidate);
                    minDistanceToCluster.put(candidate, 0d); // 0d = its at the center of its N=1 cluster
                    continue;
                }

                // build a new cluster around this node
                StaticCluster<T> cluster = new StaticCluster<T>(candidate.mClusterItem.getPosition());
                resultClusters.add(cluster);

                for (QuadItem<T> clusterItem : nearbyNodes) {
                    Double existingDistance = minDistanceToCluster.get(clusterItem);
                    double distance = distanceSquared(clusterItem.getPoint(), candidate.getPoint());

                    boolean itemBelongsToAnotherCluster = existingDistance != null;
                    if (itemBelongsToAnotherCluster) {
                        boolean isAlreadyInCloserCluster = existingDistance < distance;
                        if (isAlreadyInCloserCluster) {
                            continue;
                        }

                        // remove item from current cluster
                        itemToClusterMapping.get(clusterItem).remove(clusterItem.mClusterItem);
                    }

                    minDistanceToCluster.put(clusterItem, distance); // update min distance
                    cluster.add(clusterItem.mClusterItem); // add to new cluster
                    itemToClusterMapping.put(clusterItem, cluster); // record mapping
                }

                // record all nearbyNodes as visited
                visitedCandidates.addAll(nearbyNodes);
            }
        }
        return resultClusters;
    }

    @Override
    public Collection<T> getItems() {
        final List<T> items = new ArrayList<T>();
        synchronized (mQuadTree) {
            for (QuadItem<T> quadItem : mItems) {
                items.add(quadItem.mClusterItem);
            }
        }
        return items;
    }

    private double distanceSquared(Point a, Point b) {
        return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
    }

    private Bounds createBoundsFromSpan(Point p, double span) {
        // TODO: Use a span that takes into account the visual size of the marker, not just its LatLng.
        double halfSpan = span / 2;
        return new Bounds(
                p.x - halfSpan, p.x + halfSpan,
                p.y - halfSpan, p.y + halfSpan);
    }

    private Bounds getVisibleBounds(int zoom) {
        if (mMapCenter == null) {
            return new Bounds(0, 0, 0, 0);
        }

        Point p = PROJECTION.toPoint(mMapCenter);

        final double halfWidthSpan = mScreenWidth / Math.pow(2, zoom) / 256 / 2;
        final double halfHeightSpan = mScreenHeight / Math.pow(2, zoom) / 256 / 2;

        return new Bounds(
                p.x - halfWidthSpan, p.x + halfWidthSpan,
                p.y - halfHeightSpan, p.y + halfHeightSpan);
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        mMapCenter = cameraPosition.target;
    }

    public List<T> getItems(LatLngBounds latLngBounds) {
        Point ne = PROJECTION.toPoint(latLngBounds.northeast);
        Point sw = PROJECTION.toPoint(latLngBounds.southwest);

        Bounds searchBounds = new Bounds(Math.min(ne.x, sw.x), Math.max(ne.x, sw.x), Math.min(ne.y, sw.y), Math.max(ne.y, sw.y));

        final List<T> items = new ArrayList<T>();
        synchronized (mQuadTree) {
            Collection<QuadItem<T>> found = mQuadTree.search(searchBounds);
            for (QuadItem<T> quadItem : found) {
                items.add(quadItem.mClusterItem);
            }
        }
        return items;
    }

    public List<T> findClosestItems(Location center, int n, float initialRadius) {
        final int incFactor = 2;
        int found = 0;
        float currentDist = initialRadius;

        LatLng latLng = new LatLng(center.getLatitude(), center.getLongitude());

        List<T> nodes = null;
        while (found < n) {
            LatLngBounds bounds = LocationUtilities.calculateBoundingBox(latLng, currentDist);
            nodes = getItems(bounds);
            currentDist *= incFactor;

            found = nodes.size();
        }

        double mlat = center.getLatitude();
        double mlon = center.getLongitude();

        List<T> result = new ArrayList<>();
        StreamSupport
                .stream(nodes)
                .sorted((x, y) ->
                        Double.compare(
                                distFrom(mlat, mlon, x.getPosition().latitude, x.getPosition().longitude),
                                distFrom(mlat, mlon, y.getPosition().latitude, y.getPosition().longitude)))
                .limit(n)
                .forEach(x -> result.add(x));

        return result;
    }

    public static double distFrom(double lat1, double lng1, double lat2, double lng2) {
        // calculate distance using Haversine formula
        final double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = (earthRadius * c);

        return dist;
    }

    private static class QuadItem<T extends ClusterItem> implements PointQuadTree.Item, Cluster<T> {
        private final T mClusterItem;
        private final Point mPoint;
        private final LatLng mPosition;
        private Set<T> singletonSet;

        private QuadItem(T item) {
            mClusterItem = item;
            mPosition = item.getPosition();
            mPoint = PROJECTION.toPoint(mPosition);
            singletonSet = Collections.singleton(mClusterItem);
        }

        @Override
        public Point getPoint() {
            return mPoint;
        }

        @Override
        public LatLng getPosition() {
            return mPosition;
        }

        @Override
        public Set<T> getItems() {
            return singletonSet;
        }

        @Override
        public int getSize() {
            return 1;
        }
    }
}

