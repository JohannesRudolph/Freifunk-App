package de.inmotion_sst.freifunkfinder.clustering;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.geometry.Bounds;
import com.google.maps.android.geometry.Point;
import com.google.maps.android.projection.SphericalMercatorProjection;
import com.google.maps.android.quadtree.PointQuadTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import java8.util.stream.StreamSupport;

public class SpatialDataSource<T extends ClusterItem> {
    private static final SphericalMercatorProjection PROJECTION = new SphericalMercatorProjection(1);

    /**
     * Any modifications should be synchronized on mQuadTree.
     */
    private final Collection<QuadItem<T>> mItems = new ArrayList<QuadItem<T>>();

    // our world is represented in a (0,1)|(0,1) coordinate system

    /**
     * Any modifications should be synchronized on mQuadTree.
     */
    private final PointQuadTree<QuadItem<T>> mQuadTree = new PointQuadTree<QuadItem<T>>(0, 1, 0, 1);

    public void addItem(T item) {
        final QuadItem<T> quadItem = new QuadItem<T>(item);
        synchronized (mQuadTree) {
            mItems.add(quadItem);
            mQuadTree.add(quadItem);
        }
    }

    public void addItems(Collection<T> items) {
        for (T item : items) {
            addItem(item);
        }
    }

    public void clearItems() {
        synchronized (mQuadTree) {
            mItems.clear();
            mQuadTree.clear();
        }
    }

    public Collection<QuadItem<T>> search(Bounds searchBounds) {
        synchronized (mQuadTree) {
            return mQuadTree.search(searchBounds);
        }
    }

    public List<T> search(LatLngBounds latLngBounds) {
        Point ne = PROJECTION.toPoint(latLngBounds.northeast);
        Point sw = PROJECTION.toPoint(latLngBounds.southwest);

        Bounds searchBounds = new Bounds(Math.min(ne.x, sw.x), Math.max(ne.x, sw.x), Math.min(ne.y, sw.y), Math.max(ne.y, sw.y));

        final List<T> items = new ArrayList<T>();
        synchronized (mQuadTree) {
            Collection<SpatialDataSource.QuadItem<T>> found = mQuadTree.search(searchBounds);
            for (SpatialDataSource.QuadItem<T> quadItem : found) {
                items.add(quadItem.getClusterItem());
            }
        }
        return items;
    }

    public List<T> findNodesWithin(Location location, int n, float initialRadius) {
        return findClosestItems(location, n, initialRadius);
    }

    public List<T> findClosestItems(Location center, int n, float initialRadius) {
        final int incFactor = 2;
        int found = 0;
        float currentDist = initialRadius;

        LatLng latLng = new LatLng(center.getLatitude(), center.getLongitude());

        List<T> nodes = null;
        while (found < n) {
            LatLngBounds bounds = LocationUtilities.calculateBoundingBox(latLng, currentDist);
            nodes = search(bounds);
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

    public static class QuadItem<T extends ClusterItem> implements PointQuadTree.Item, Cluster<T> {
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

        public T getClusterItem() {
            return mClusterItem;
        }
    }


}
