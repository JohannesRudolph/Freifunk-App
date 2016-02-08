package de.inmotion_sst.freifunkfinder;

import android.app.Application;
import android.os.AsyncTask;

public class FreifunkApplication extends Application {

    private NodeRepository nodeRepository;

    public NodeRepository getNodeRepository() {
        return nodeRepository;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        nodeRepository = new NodeRepository(getApplicationContext());

        loadNodesInBackground();
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
            }
        };

        loadNodesTask.execute();
    }
}
