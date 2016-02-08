package de.inmotion_sst.freifunkfinder;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class NodeRepositoryTest {

    private static final String TAG = "NodeRepositoryTest";

    @NonNull
    private NodeRepository makeSut() {
        return new NodeRepository(InstrumentationRegistry.getTargetContext());
    }

    @NonNull
    private Node makeNode() {
        return new Node("id", "name", "community", 1.0, 2.0, 3.0, "connected", 2);
    }

    @Before
    public void Setup() {
        File f = NodeRepository.getFile(InstrumentationRegistry.getTargetContext());
        f.delete();
    }

    @Test
    @LargeTest // this is a slow tests, uses network
    public void fetchNodeList_FetchesDataFromServer() throws Exception {
        // for some reason normal TimingLogger doesnt output anything in androidTest, even if setting correct log level (Log.d returns -1)...
        SysOutTimingLogger timing = new SysOutTimingLogger(TAG, "perf");

        List<Node> result = NodeRepository.fetchNodeList();
        assertTrue(result.size() > 0);
        timing.addSplit("fetch");

        NodeRepository sut = makeSut();
        sut.setNodes(result);
        timing.addSplit("set nodes");

        sut.save();
        timing.addSplit("save");

        sut.load();
        timing.addSplit("load");

        timing.dumpToSysOut();
    }

    @Test
    public void newRepositoryHasEmptyList() throws Exception {
        NodeRepository sut = makeSut();
        assertEquals(0, sut.getNodes().count());
    }

    @Test
    public void setNodes_addsNodes() throws Exception {
        NodeRepository sut = makeSut();

        Node node = makeNode();
        sut.setNodes(Collections.singletonList(node));

        assertEquals(1, sut.getNodes().count());
        assertEquals(node, sut.getNodes().findFirst().get());
    }

    @Test
    public void setNodes_notifiesObserver() throws Exception {
        NodeRepository sut = makeSut();

        // java lambdas are so quirky....
        final Boolean[] fired = {false};
        sut.addObserver((s, a) -> fired[0] = true);

        Node node = makeNode();
        sut.setNodes(Collections.singletonList(node));

        assertTrue(fired[0]);
    }

    @Test
    @MediumTest
    public void save_load_restoresNodes() throws Exception {
        NodeRepository sut = makeSut();

        Node node = makeNode();
        sut.setNodes(Collections.singletonList(node));

        sut.save();

        sut = makeSut();
        sut.load();

        assertEquals(1, sut.getNodes().count());
        assertEquals(node.getId(), sut.getNodes().findFirst().get().getId());
    }

    @Test
    @MediumTest
    public void load_withoutFile_returnsEmpty() throws Exception {
        NodeRepository sut = makeSut();

        sut.load();

        assertEquals(0, sut.getNodes().count());
    }

    @Test
    @MediumTest
    public void load_withTamperedFile_returnsEmpty() throws Exception {
        NodeRepository sut = makeSut();

        File f = NodeRepository.getFile(InstrumentationRegistry.getTargetContext());
        FileOutputStream fileOutputStream = new FileOutputStream(f);
        fileOutputStream.write(1);
        fileOutputStream.close();

        sut.load();

        assertEquals(0, sut.getNodes().count());
    }

    @Test
    public void node_canRoundtripParcelableList() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Node node = mapper.readValue(singleNodeJson, Node.class);

        List<Node> l = new ArrayList<>();
        l.add(node);

        Parcel parcel = Parcel.obtain();
        parcel.writeTypedList(l);
        byte[] bytes = parcel.marshall();

        l.clear();
        parcel.recycle();
        parcel = Parcel.obtain();

        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);

        l = parcel.createTypedArrayList( Node.CREATOR);
        parcel.recycle();

        Node result = l.get(0);
        verifyNode(result);
    }

    @Test
    public void node_canRoundtripParcelable() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Node node = mapper.readValue(singleNodeJson, Node.class);

        Parcel parcel = Parcel.obtain();
        parcel.writeParcelable(node, 0);

        byte[] bytes = parcel.marshall();
        parcel.recycle();

        parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0); // this is extremely important!

        Node result = parcel.readParcelable(Node.class.getClassLoader());
        parcel.recycle();

        verifyNode(result);
    }

    String singleNodeJson = "{\n" +
            "  \"id\": \"30b5c2c6b8b8\",\n" +
            "  \"lat\": \"48.35949\",\n" +
            "  \"long\": \"12.51056\",\n" +
            "  \"name\": \"FF-NSV-Pizzeria Il-Giardino\",\n" +
            "  \"community\": \"altdorf\",\n" +
            "  \"status\": \"online\",\n" +
            "  \"clients\": 1\n" +
            "}";

    private void verifyNode(Node node) {
        assertEquals("30b5c2c6b8b8", node.getId());
        assertEquals(48.35949, node.getLat(), 0.000001);
        assertEquals(12.51056, node.getLon(), 0.000001);
        assertEquals("FF-NSV-Pizzeria Il-Giardino", node.getName());
        assertEquals("altdorf", node.getCommunity());
        assertEquals("online", node.getStatus());
        assertEquals(1, node.getClients());
    }
}