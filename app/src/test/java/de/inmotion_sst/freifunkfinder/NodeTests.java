package de.inmotion_sst.freifunkfinder;

import android.os.Parcel;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class NodeTests {
    String singleNodeJson = "{\n" +
            "  \"id\": \"30b5c2c6b8b8\",\n" +
            "  \"lat\": \"48.35949\",\n" +
            "  \"long\": \"12.51056\",\n" +
            "  \"name\": \"FF-NSV-Pizzeria Il-Giardino\",\n" +
            "  \"community\": \"altdorf\",\n" +
            "  \"status\": \"online\",\n" +
            "  \"clients\": 1\n" +
            "}";

    @Test
    public void node_canDeserializeJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Node node = mapper.readValue(singleNodeJson, Node.class);

        verifyNode(node);
    }

    @Test
    public void node_canRoundtripJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Node node = mapper.readValue(singleNodeJson, Node.class);

        String json = mapper.writeValueAsString(node);
        Node result = mapper.readValue(json, Node.class);
        verifyNode(result);
    }


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
