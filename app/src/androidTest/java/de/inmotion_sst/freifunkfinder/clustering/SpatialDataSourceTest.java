package de.inmotion_sst.freifunkfinder.clustering;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.clustering.ClusterItem;

import org.junit.Test;

import java.util.List;

import de.inmotion_sst.freifunkfinder.clustering.SpatialDataSource;
import de.inmotion_sst.freifunkfinder.clustering.LocationUtilities;

import static org.junit.Assert.assertEquals;

public class SpatialDataSourceTest {

    @Test
    public void canFindNodesWithinBoundingBox() throws Exception {
        SpatialDataSource<TestClusterItem> sut = new SpatialDataSource<>();

        TestClusterItem a = new TestClusterItem(new LatLng(49.882309679547, 8.6506497859955));
        TestClusterItem b = new TestClusterItem(new LatLng(49.882250917053, 8.6508965492249));

        float distanceBetween = distanceBetween(a.getPosition(), b.getPosition());
        double expected = 18.90106773376465; // calculated using http://www.movable-type.co.uk/scripts/latlong.html
        assertEquals(expected, distanceBetween, 0.001);

        sut.addItem(a); // FF_Emil-2
        sut.addItem(b); // FF_Emil

        List<TestClusterItem> smallBox = sut.search(LocationUtilities.calculateBoundingBox(a.getPosition(), 10));

        assertEquals(1, smallBox.size());
        assertEquals(a, smallBox.get(0));

        LatLngBounds bounds = LocationUtilities.calculateBoundingBox(a.getPosition(), 20);
        List<TestClusterItem> largeBox = sut.search(bounds);

        assertEquals(2, largeBox.size()); // contains both nodes

        //System.out.println("COORDS");
        //System.out.println(formatPosition(a.getPosition()));
        //System.out.println(formatPosition(b.getPosition()));
        //System.out.println(formatPosition(bounds.northeast));
        //System.out.println(formatPosition(bounds.southwest));

    }


    @Test
    public void findClosestItems_enlargesInitialBoundingBox() throws Exception {
        SpatialDataSource<TestClusterItem> sut = new SpatialDataSource<>();

        TestClusterItem a = new TestClusterItem(new LatLng(49.882309679547, 8.6506497859955));
        TestClusterItem b = new TestClusterItem(new LatLng(49.882250917053, 8.6508965492249));

        float distanceBetween = distanceBetween(a.getPosition(), b.getPosition());

        sut.addItem(a); // FF_Emil-2
        sut.addItem(b); // FF_Emil

        List<TestClusterItem> found = sut.findClosestItems(a.getPosition(), 2, distanceBetween/10);

        assertEquals(2, found.size());
    }

    @Test(timeout = 1000)
    public void findClosestItems_LimitsSearch() throws Exception {
        SpatialDataSource<TestClusterItem> sut = new SpatialDataSource<>();

        List<TestClusterItem> found = sut.findClosestItems( new LatLng(49.882309679547, 8.6506497859955), 1, 1.0f);

        assertEquals(0, found.size());
    }

    /*
    private String formatPosition(LatLng position) {
        return String.format("%f %f", position.latitude, position.longitude);
    }*/

    class TestClusterItem implements ClusterItem
    {
        private LatLng position;

        TestClusterItem(LatLng position) {
            this.position = position;
        }

        @Override
        public LatLng getPosition() {
            return position;
        }
    }

    public static float distanceBetween(LatLng first, LatLng second) {
        float[] distance = new float[1];
        Location.distanceBetween(first.latitude, first.longitude, second.latitude, second.longitude, distance);
        return distance[0];
    }
}