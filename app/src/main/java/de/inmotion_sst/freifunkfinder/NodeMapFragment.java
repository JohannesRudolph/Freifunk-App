package de.inmotion_sst.freifunkfinder;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.TimingLogger;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.Observer;

import de.inmotion_sst.freifunkfinder.clustering.NodeClusterManager;
import de.inmotion_sst.freifunkfinder.clustering.NodeClusterRenderer;
import de.inmotion_sst.freifunkfinder.clustering.VisibleNonHierarchicalDistanceBasedAlgorithm;

public class NodeMapFragment extends SupportMapFragment implements OnMapReadyCallback {

    public static final String TAG = "NodeMapFragment";

    private GoogleMap googleMap;
    private NodeRepository nodeRepository;
    private Observer repoObserver;
    private NodeClusterManager<Node> clusterManager;
    private VisibleNonHierarchicalDistanceBasedAlgorithm<Node> clusterAlgorithm;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nodeRepository = ((FreifunkApplication) getActivity().getApplication()).getNodeRepository();

        repoObserver = (o, a) -> refreshNodes();
        nodeRepository.addObserver(repoObserver);

        this.getMapAsync( this );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        nodeRepository.deleteObserver(repoObserver);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        setupMap(googleMap);

        setupClustering(googleMap);

        refreshNodes();
    }

    private void setupMap(GoogleMap googleMap) {
        // disable default android toolbar that offers routing etc.
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        // enable my location button
        googleMap.setMyLocationEnabled(true);

        zoomToMyLocation(googleMap);
    }

    private void zoomToMyLocation(GoogleMap googleMap) {
        Location location = getCurrentLocation();

        if (location != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))
                    .zoom(17)
                    .build();

            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    private void setupClustering(GoogleMap googleMap) {
        DisplayMetrics metrics = getDisplayMetrics();

        clusterManager = new NodeClusterManager<>(getContext(), googleMap);
        clusterManager.setClusterOnlyVisibleArea(true);

        clusterAlgorithm = new VisibleNonHierarchicalDistanceBasedAlgorithm<>(metrics.widthPixels, metrics.heightPixels, nodeRepository.getSpatialDataSource());
        clusterManager.setAlgorithm(clusterAlgorithm);

        // the interface for configuring a cluster manager is less than ideal...
        NodeClusterRenderer renderer = new NodeClusterRenderer(getContext(), googleMap, clusterManager);
        renderer.setShouldAnimate(false);

        clusterManager.setRenderer(renderer);

        this.googleMap.setOnCameraChangeListener(clusterManager);
        this.googleMap.setOnMarkerClickListener(clusterManager);
    }

    @NonNull
    private DisplayMetrics getDisplayMetrics() {
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics;
    }

    private void refreshNodes() {
        boolean mapNotInitialized = clusterManager == null;
        if (mapNotInitialized)
            return;

        TimingLogger timing = new TimingLogger(TAG, "refreshNodes");

        // refresh screen
        clusterManager.onCameraChange(googleMap.getCameraPosition());

        timing.addSplit("update map");

        timing.dumpToLog();
    }


    public Location getCurrentLocation() {
        // even though this is deprecated, this will ensure a consistent user experience
        // where the center of the world is what is currently displayed on the map.
        // retrieving a value from LocationManager typically leads to inconsistent results (or returns null if location is not accurate yet)
        return googleMap.getMyLocation();
    }
}