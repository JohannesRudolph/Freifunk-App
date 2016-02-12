package de.inmotion_sst.freifunkfinder.clustering;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.MarkerManager;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.Algorithm;
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;
import com.google.maps.android.clustering.view.ClusterRenderer;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.inmotion_sst.freifunkfinder.Node;


// This ClusterManager is based on the original ClusterManager and changes suggested in https://github.com/googlemaps/android-maps-utils/pull/217

/**
 * Groups many items on a map based on zoom level.
 * <p>
 * NodeClusterManager should be added to the map as an: <ul> <li>{@link com.google.android.gms.maps.GoogleMap.OnCameraChangeListener}</li>
 * <li>{@link com.google.android.gms.maps.GoogleMap.OnMarkerClickListener}</li> </ul>
 */
public class NodeClusterManager<T extends Node> implements GoogleMap.OnCameraChangeListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener {
    private final MarkerManager mMarkerManager;
    private final MarkerManager.Collection mMarkers;
    private final MarkerManager.Collection mClusterMarkers;

    private ClusterAlgorithm<T> mAlgorithm;
    private final ReadWriteLock mAlgorithmLock = new ReentrantReadWriteLock();
    private ClusterRenderer<T> mRenderer;
    private boolean mShowOnlyVisibleArea;

    private GoogleMap mMap;
    private CameraPosition mPreviousCameraPosition;
    private ClusterTask mClusterTask;
    private final ReadWriteLock mClusterTaskLock = new ReentrantReadWriteLock();

    private ClusterManager.OnClusterItemClickListener<T> mOnClusterItemClickListener;
    private ClusterManager.OnClusterInfoWindowClickListener<T> mOnClusterInfoWindowClickListener;
    private ClusterManager.OnClusterItemInfoWindowClickListener<T> mOnClusterItemInfoWindowClickListener;
    private ClusterManager.OnClusterClickListener<T> mOnClusterClickListener;

    public NodeClusterManager(Context context, GoogleMap map) {
        this(context, map, new MarkerManager(map));
    }

    public NodeClusterManager(Context context, GoogleMap map, MarkerManager markerManager) {
        mMap = map;
        mMarkerManager = markerManager;
        mClusterMarkers = markerManager.newCollection();
        mMarkers = markerManager.newCollection();
        mRenderer = new NodeClusterRenderer<>(context, map, this);
        mClusterTask = new ClusterTask();
        mRenderer.onAdd();
    }

    public MarkerManager.Collection getMarkerCollection() {
        return mMarkers;
    }

    public MarkerManager.Collection getClusterMarkerCollection() {
        return mClusterMarkers;
    }

    public MarkerManager getMarkerManager() {
        return mMarkerManager;
    }

    public void setRenderer(ClusterRenderer<T> view) {
        mRenderer.setOnClusterClickListener(null);
        mRenderer.setOnClusterItemClickListener(null);
        mClusterMarkers.clear();
        mMarkers.clear();
        mRenderer.onRemove();
        mRenderer = view;
        mRenderer.onAdd();
        mRenderer.setOnClusterClickListener(mOnClusterClickListener);
        mRenderer.setOnClusterInfoWindowClickListener(mOnClusterInfoWindowClickListener);
        mRenderer.setOnClusterItemClickListener(mOnClusterItemClickListener);
        mRenderer.setOnClusterItemInfoWindowClickListener(mOnClusterItemInfoWindowClickListener);
        cluster();
    }

    public void setAlgorithm(ClusterAlgorithm<T> algorithm) {
        mAlgorithmLock.writeLock().lock();
        try {
            mAlgorithm = algorithm;
            if (mAlgorithm instanceof GoogleMap.OnCameraChangeListener) {
                ((GoogleMap.OnCameraChangeListener) mAlgorithm).onCameraChange(mMap.getCameraPosition());
            }

        } finally {
            mAlgorithmLock.writeLock().unlock();
        }
        cluster();
    }

    public void setClusterOnlyVisibleArea(boolean onlyVisibleArea) {
        mShowOnlyVisibleArea = onlyVisibleArea;
    }

    /**
     * Force a re-cluster. You may want to call this after adding new item(s).
     */
    public void cluster() {
        mClusterTaskLock.writeLock().lock();
        try {
            // Attempt to cancel the in-flight request.
            mClusterTask.cancel(true);
            mClusterTask = new ClusterTask();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                mClusterTask.execute(mMap.getCameraPosition().zoom);
            } else {
                mClusterTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mMap.getCameraPosition().zoom);
            }
        } finally {
            mClusterTaskLock.writeLock().unlock();
        }
    }

    /**
     * Might re-cluster.
     *
     * @param cameraPosition
     */
    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        if (mRenderer instanceof GoogleMap.OnCameraChangeListener) {
            ((GoogleMap.OnCameraChangeListener) mRenderer).onCameraChange(cameraPosition);
        }

        if (mAlgorithm instanceof GoogleMap.OnCameraChangeListener) {
            ((GoogleMap.OnCameraChangeListener) mAlgorithm).onCameraChange(cameraPosition);
        }

        if (mShowOnlyVisibleArea) {
            // algorithm will decide if it is need to recompute clusters
            cluster();
        } else if (mPreviousCameraPosition == null || mPreviousCameraPosition.zoom != cameraPosition.zoom) {
            // Don't re-compute clusters if the map has just been panned/tilted/rotated.
            mPreviousCameraPosition = mMap.getCameraPosition();
            cluster();
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return getMarkerManager().onMarkerClick(marker);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        getMarkerManager().onInfoWindowClick(marker);
    }

    /**
     * Runs the clustering algorithm in a background thread, then re-paints when results come back.
     */
    private class ClusterTask extends AsyncTask<Float, Void, Set<? extends Cluster<T>>> {
        @Override
        protected Set<? extends Cluster<T>> doInBackground(Float... zoom) {
            mAlgorithmLock.readLock().lock();
            try {
                return mAlgorithm.getClusters(zoom[0]);
            } finally {
                mAlgorithmLock.readLock().unlock();
            }
        }

        @Override
        protected void onPostExecute(Set<? extends Cluster<T>> clusters) {
            mRenderer.onClustersChanged(clusters);
        }
    }

    /**
     * Sets a callback that's invoked when a Cluster is tapped. Note: For this listener to function,
     * the NodeClusterManager must be added as a click listener to the map.
     */
    public void setOnClusterClickListener(ClusterManager.OnClusterClickListener<T> listener) {
        mOnClusterClickListener = listener;
        mRenderer.setOnClusterClickListener(listener);
    }

    /**
     * Sets a callback that's invoked when a Cluster is tapped. Note: For this listener to function,
     * the NodeClusterManager must be added as a info window click listener to the map.
     */
    public void setOnClusterInfoWindowClickListener(ClusterManager.OnClusterInfoWindowClickListener<T> listener) {
        mOnClusterInfoWindowClickListener = listener;
        mRenderer.setOnClusterInfoWindowClickListener(listener);
    }

    /**
     * Sets a callback that's invoked when an individual ClusterItem is tapped. Note: For this
     * listener to function, the NodeClusterManager must be added as a click listener to the map.
     */
    public void setOnClusterItemClickListener(ClusterManager.OnClusterItemClickListener<T> listener) {
        mOnClusterItemClickListener = listener;
        mRenderer.setOnClusterItemClickListener(listener);
    }

    /**
     * Sets a callback that's invoked when an individual ClusterItem's Info Window is tapped. Note: For this
     * listener to function, the NodeClusterManager must be added as a info window click listener to the map.
     */
    public void setOnClusterItemInfoWindowClickListener(ClusterManager.OnClusterItemInfoWindowClickListener<T> listener) {
        mOnClusterItemInfoWindowClickListener = listener;
        mRenderer.setOnClusterItemInfoWindowClickListener(listener);
    }
}
