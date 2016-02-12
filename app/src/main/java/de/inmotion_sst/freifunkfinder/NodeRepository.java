package de.inmotion_sst.freifunkfinder;

import android.content.Context;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.TimingLogger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;

import de.inmotion_sst.freifunkfinder.clustering.SpatialDataSource;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;

public class NodeRepository extends Observable {
    public static final String TAG = "NodeRepository";
    private final Context context;
    private final SpatialDataSource<Node> spatialDataSource;
    private List<Node> nodes;

    public NodeRepository(Context context) {
        this.context = context;
        this.nodes = new ArrayList<>();
        this.spatialDataSource = new SpatialDataSource<>();
    }

    public SpatialDataSource<Node> getSpatialDataSource() {
        return spatialDataSource;
    }

    /**
     * Fetches a node list from the server
     *
     * @return The parsed nodes
     * @throws IOException
     * @throws HttpException
     */
    public static List<Node> fetchNodeList() throws IOException, HttpException {
        TimingLogger timing = new TimingLogger(TAG, "fetchNodeList");

        HttpURLConnection conn = (HttpURLConnection) new URL("http://freifunk.inmotion-sst.de/nodes-de.json").openConnection();
        conn.setRequestMethod("GET");
        conn.setAllowUserInteraction(false);
        conn.setConnectTimeout(3000);
        conn.connect();

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new HttpException("GET did not return 200-OK");
        }

        timing.addSplit("connect");

        InputStream inputStream = conn.getInputStream();
        ObjectMapper mapper = new ObjectMapper();

        List<Node> result = mapper.readValue(inputStream, new TypeReference<ArrayList<Node>>() {
        });

        timing.addSplit("parsed " + result.size());
        timing.dumpToLog();

        return result;
    }

    public Stream<Node> getNodes() {
        return StreamSupport.stream(nodes);
    }


    public void setNodes(NodeRepository loader) {
        setNodes(loader.nodes);
        this.fireNodesChanged();
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
        spatialDataSource.clearItems();
        spatialDataSource.addItems(nodes);
        fireNodesChanged();
    }

    private void fireNodesChanged() {
        this.setChanged();
        this.notifyObservers();
    }

    /**
     * Saves nodes to disk
     */
    public void save() throws IOException {
        TimingLogger timing = new TimingLogger(TAG, "save");

        // yes, parcels are not meant for persisting to disk, but they are convenient and fast
        // we are only serializing "simple" data (no binders etc)
        // + losing serialized data here would not be a problem as users can almost always refresh node data
        Parcel parcel = Parcel.obtain();

        parcel.writeTypedList(this.nodes);

        File f = getFile();
        FileOutputStream fileOutputStream = new FileOutputStream(f);

        byte[] buffer = parcel.marshall();
        parcel.recycle();

        fileOutputStream.write(buffer);
        fileOutputStream.close();

        timing.dumpToLog();
    }

    /**
     * Loads nodes from disk
     */
    public void load() {
        TimingLogger timing = new TimingLogger(TAG, "load");

        try {
            Parcel parcel = Parcel.obtain();

            File f = getFile();
            FileInputStream fileInputStream = new FileInputStream(f);
            byte[] buffer = new byte[(int) f.length()];
            fileInputStream.read(buffer, 0, buffer.length);
            fileInputStream.close();

            ArrayList<Node> nodes = new ArrayList<>();

            parcel.unmarshall(buffer, 0, buffer.length);
            parcel.setDataPosition(0);
            parcel.readTypedList(nodes, Node.CREATOR);
            parcel.recycle();

            timing.addSplit("loaded " + nodes.size());

            // trim
            nodes.removeAll(Collections.singleton(null));
            timing.addSplit("trimmed nulls" + nodes.size());

            setNodes(nodes);
        } catch (Exception e) {
            Log.d(TAG, "load encountered exception, this is not a problem", e);
        }

        timing.dumpToLog();
    }

    @NonNull
    private File getFile() {
        return NodeRepository.getFile(context);
    }

    public static File getFile(Context context) {
        File f = new File(context.getCacheDir(), "nodes.json");
        return f;
    }

    public boolean hasNodes() {
        return this.nodes.size() > 0;
    }
}
