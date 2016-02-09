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

    }

}
