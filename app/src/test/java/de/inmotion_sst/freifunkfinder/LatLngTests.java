package de.inmotion_sst.freifunkfinder;

import com.google.android.gms.maps.model.LatLng;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class LatLngTests {
    @Test
    public void canCreateInstance() throws Exception {
        LatLng sut = new LatLng(1.0, 2.0);
        assertEquals(1.0, sut.latitude, 0.0);
        assertEquals(2.0, sut.longitude, 0.0);
    }
}