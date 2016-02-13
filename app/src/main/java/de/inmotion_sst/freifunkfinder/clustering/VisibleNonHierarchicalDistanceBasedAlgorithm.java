package de.inmotion_sst.freifunkfinder.clustering;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.algo.StaticCluster;
import com.google.maps.android.geometry.Bounds;
import com.google.maps.android.geometry.Point;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        implements ClusterAlgorithm<T>, GoogleMap.OnCameraChangeListener {

    public static final int MAX_DISTANCE_AT_ZOOM = 100; // essentially 100 dp.

    private final int mScreenWidth;
    private final int mScreenHeight;
    private final SpatialDataSource<T> mDataSource;
    private LatLng mMapCenter;

    public VisibleNonHierarchicalDistanceBasedAlgorithm(int screenWidth, int screenHeight, SpatialDataSource<T> quadTree) {
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
        mDataSource = quadTree;
    }

    @Override
    public Set<? extends Cluster<T>> getClusters(double zoom) {
        final int discreteZoom = (int) zoom;

        final double zoomSpecificSpan = MAX_DISTANCE_AT_ZOOM / Math.pow(2, discreteZoom) / 256;

        final HashSet<SpatialDataSource.QuadItem<T>> visitedCandidates = new HashSet<>();
        final Set<Cluster<T>> resultClusters = new HashSet<>();
        final Map<SpatialDataSource.QuadItem<T>, Double> minDistanceToCluster = new HashMap<>();
        final Map<SpatialDataSource.QuadItem<T>, StaticCluster<T>> itemToClusterMapping = new HashMap<>();

        synchronized (mDataSource) {

            Bounds visibleBounds = getVisibleBounds(discreteZoom);

            // first, find all visible nodes
            Collection<SpatialDataSource.QuadItem<T>> visibleNodes = mDataSource.search(visibleBounds);

            for (SpatialDataSource.QuadItem<T> candidate : visibleNodes) {
                // Candidate is already part of a cluster, nothing to do for it
                if (visitedCandidates.contains(candidate)) {
                    continue;
                }

                // search items close to this node
                Bounds searchBounds = createBoundsFromSpan(candidate.getPoint(), zoomSpecificSpan);
                Collection<SpatialDataSource.QuadItem<T>> nearbyNodes= mDataSource.search(searchBounds);

                if (nearbyNodes.size() == 1) {
                    // Only the current marker is in range. Just add the single item to the resultClusters.
                    resultClusters.add(candidate);
                    visitedCandidates.add(candidate);
                    minDistanceToCluster.put(candidate, 0d); // 0d = its at the center of its N=1 cluster
                    continue;
                }

                // build a new cluster around this node
                StaticCluster<T> cluster = new StaticCluster<T>(candidate.getClusterItem().getPosition());
                resultClusters.add(cluster);

                for (SpatialDataSource.QuadItem<T> clusterItem : nearbyNodes) {
                    Double existingDistance = minDistanceToCluster.get(clusterItem);
                    double distance = distanceSquared(clusterItem.getPoint(), candidate.getPoint());

                    boolean itemBelongsToAnotherCluster = existingDistance != null;
                    if (itemBelongsToAnotherCluster) {
                        boolean isAlreadyInCloserCluster = existingDistance < distance;
                        if (isAlreadyInCloserCluster) {
                            continue;
                        }

                        // remove item from current cluster
                        itemToClusterMapping.get(clusterItem).remove(clusterItem.getClusterItem());
                    }

                    minDistanceToCluster.put(clusterItem, distance); // update min distance
                    cluster.add(clusterItem.getClusterItem()); // add to new cluster
                    itemToClusterMapping.put(clusterItem, cluster); // record mapping
                }

                // record all nearbyNodes as visited
                visitedCandidates.addAll(nearbyNodes);
            }
        }
        return resultClusters;
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

        Point p = mDataSource.toPoint(mMapCenter);

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
}

