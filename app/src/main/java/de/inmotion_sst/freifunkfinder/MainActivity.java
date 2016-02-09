package de.inmotion_sst.freifunkfinder;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

import de.inmotion_sst.freifunkfinder.ar.CameraFinderActivity;
import de.inmotion_sst.freifunkfinder.ar.SurroundingNodesSetup;
import de.inmotion_sst.freifunkfinder.settings.SettingsActivity;
import java8.util.stream.StreamSupport;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private NodeMapFragment mapFragment;
    private Fragment currentFragment;
    private FloatingActionButton cameraActionButton;
    private NodeRepository nodeRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        nodeRepository = ((FreifunkApplication) getApplication()).getNodeRepository();
        loadNodesInBackground();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupActionButtons();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        navigationView.getMenu().performIdentifierAction(R.id.nav_map, 0);
    }

    private void setupActionButtons() {
        cameraActionButton = (FloatingActionButton) findViewById(R.id.action_button_ar);
        cameraActionButton.setOnClickListener(view -> launchCameraActivity());
        updateActionButtonEnabled();
    }

    private void updateActionButtonEnabled() {
        cameraActionButton.setEnabled(nodeRepository.hasNodes());
    }

    private void launchCameraActivity() {
        Location myLocation = mapFragment.getCurrentLocation();
        if (myLocation == null){
            showError("Could not determine precise location. Improve GPS reception, then retry.");
            return;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int n = Integer.parseInt(preferences.getString(getResources().getString(R.string.prefkey_ar_nodes), "5"));

        List<Node> nodes = mapFragment.findNodesWithin(myLocation, n, 200.0f);

        SurroundingNodesSetup setup = new SurroundingNodesSetup(myLocation, StreamSupport.stream(nodes));

        Log.d(TAG, String.format("starting AR view with %d nodes at location (%f,%f,%.1fm)", nodes.size(), myLocation.getLatitude(), myLocation.getLongitude(), myLocation.getAltitude()));
        CameraFinderActivity.startWithSetup(this, setup);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            updateNodesFromServer();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_map:
                if (mapFragment == null) {
                    mapFragment = makeMapFragment();
                }
                swapMainContent(mapFragment);
                break;
            case R.id.nav_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            case R.id.nav_setting:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            default:
                throw new UnsupportedOperationException("Unknown menu entry");
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void swapMainContent(Fragment fragment) {
        if (currentFragment == fragment)
            return;

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_main, fragment)
                .commit();

        currentFragment = fragment;
    }


    @NonNull
    private NodeMapFragment makeMapFragment() {
        // Create a new fragment and specify the planet to show based on position
        mapFragment = new NodeMapFragment();

        Bundle args = new Bundle();
        mapFragment.setArguments(args);

        return mapFragment;
    }

    private void loadNodesInBackground() {
        AsyncTask<Void, Void, NodeRepository> loadNodesTask = new AsyncTask<Void, Void, NodeRepository>() {
            @Override
            protected NodeRepository doInBackground(Void... voids) {
                NodeRepository loader = new NodeRepository(getApplicationContext());
                loader.load();

                return loader;
            }

            @Override
            protected void onPostExecute(NodeRepository loader) {
                nodeRepository.setNodes(loader);
                updateActionButtonEnabled();
                promptUserForNodeDataIfNecessary();
            }
        };

        loadNodesTask.execute();
    }

    private void promptUserForNodeDataIfNecessary() {
        if (nodeRepository.hasNodes())
            return;

        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    updateNodesFromServer();
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setMessage("No node data. Do you want to download node data now?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .show();
    }

    private void updateNodesFromServer() {
        ProgressDialog progressDialog = ProgressDialog.show(this, "Updating Nodes", "Downloading node data", true, false);

        AsyncTask<Void, Void, List<Node>> refreshNodesTask = new AsyncTask<Void, Void, List<Node>>() {
            private Exception exception;

            @Override
            protected List<Node> doInBackground(Void... voids) {
                try {

                    List<Node> fetched = NodeRepository.fetchNodeList();

                    // saving is slow too (approx 1s), so we do it on a background thread too
                    // we can use a second repo instance for that, but still have to add to the repo that represents our ViewModel
                    NodeRepository repo = new NodeRepository(getApplicationContext());
                    repo.setNodes(fetched);
                    repo.save();

                    return fetched;
                } catch (Exception e) {
                    this.exception = e;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Node> nodes) {
                // the progressDialog needs to be dismissed before any toast is shown...
                // so make sure it is dismissed in all exit paths of this method
                if (exception != null) {
                    progressDialog.dismiss();
                    showError(exception.toString());
                    return;
                }

                nodeRepository.setNodes(nodes);

                updateSyncInformation(nodes);
                updateActionButtonEnabled();

                progressDialog.dismiss();

                View view = MainActivity.this.findViewById(R.id.content_main);
                Snackbar.make(view, "Nodes updated", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
            }
        };

        refreshNodesTask.execute();
    }

    private void updateSyncInformation(List<Node> nodes) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        SharedPreferences.Editor edit = preferences.edit();
        edit.putString(getString(R.string.prefkey_sync_last), new Date().toString());
        edit.putString(getString(R.string.prefkey_sync_nodes), Integer.toString(nodes.size()));
        edit.commit();
    }

    private void showError(String text) {
        View view = MainActivity.this.findViewById(R.id.content_main);

        Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE)
                .setAction("Action", null);

        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.RED);

        TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);

        snackbar.show();
    }

}

