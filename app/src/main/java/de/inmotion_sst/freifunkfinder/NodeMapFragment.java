package de.inmotion_sst.freifunkfinder;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.TimingLogger;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Observer;

import de.inmotion_sst.freifunkfinder.clustering.LocationUtilities;
import de.inmotion_sst.freifunkfinder.clustering.NodeClusterManager;
import de.inmotion_sst.freifunkfinder.clustering.NodeClusterRenderer;
import de.inmotion_sst.freifunkfinder.clustering.VisibleNonHierarchicalDistanceBasedAlgorithm;

public class NodeMapFragment extends Fragment implements OnMapReadyCallback {

    public static final String TAG = "NodeMapFragment";

    private MapView mapView;
    private GoogleMap googleMap;
    private NodeRepository nodeRepository;
    private Observer repoObserver;
    private NodeClusterManager<Node> clusterManager;
    private VisibleNonHierarchicalDistanceBasedAlgorithm<Node> clusterAlgorithm;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // inflate and return the layout
        View v = inflater.inflate(R.layout.fragment_node_map, container, false);
        mapView = (MapView) v.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();// needed to get the map to display immediately

        nodeRepository = ((FreifunkApplication) getActivity().getApplication()).getNodeRepository();

        repoObserver = (o, a) -> refreshNodes();
        nodeRepository.addObserver(repoObserver);
        mapView.getMapAsync(this);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        nodeRepository.deleteObserver(repoObserver);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
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

        clusterAlgorithm = new VisibleNonHierarchicalDistanceBasedAlgorithm<>(metrics.widthPixels, metrics.heightPixels);
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

        clusterManager.clearItems();
        nodeRepository.getNodes().forEach((x) -> clusterManager.addItem(x));

        timing.addSplit("update cluster manager");

        // refresh screen
        clusterManager.onCameraChange(googleMap.getCameraPosition());

        timing.addSplit("update map");

        timing.dumpToLog();
    }

    public List<Node> findNodesWithin(Location location, int n, float initialRadius) {

        // todo: ensure this is only called when it makes sense (after map initialised, when we have a location)
        if (clusterAlgorithm == null)
            return new ArrayList<>();

        return clusterAlgorithm.findClosestItems(location, n, initialRadius);
    }


    public Location getCurrentLocation() {
        // even though this is deprecated, this will ensure a consistent user experience
        // where the center of the world is what is currently displayed on the map.
        // retrieving a value from LocationManager typically leads to inconsistent results (or returns null if location is not accurate yet)
        return googleMap.getMyLocation();
    }
}