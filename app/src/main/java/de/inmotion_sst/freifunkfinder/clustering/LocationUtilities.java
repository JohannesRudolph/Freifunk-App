package de.inmotion_sst.freifunkfinder.clustering;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.geometry.Point;

public class LocationUtilities
{
    static final double DegreesToRadians = Math.PI / 180.0;
    static final double RadiansToDegrees = 1.0/ DegreesToRadians;
    static final double EarthRadius = 6378137.0;   //  WGS-84 ellipsoid parameters

    /**
     * Calculates the end-point from a given source at a given range (meters) and bearing (degrees). This methods uses harversine geometry equations to calculate the end-point.
     * Assumes WGS84 ellipsoid
     * @param source Point of origin
     * @param range Range in meters
     * @param bearing Bearing in degrees
     * @return End-point from the source given the desired range and bearing
     */
    public static LatLng calculateDerivedPosition(LatLng source, double range, double bearing) {
        double latA = source.latitude * DegreesToRadians;
        double lonA = source.longitude * DegreesToRadians;

        double angularDistance = range / EarthRadius;
        double trueCourse = bearing * DegreesToRadians;

        // all in radians, see http://www.movable-type.co.uk/scripts/latlong.html for equations used
        double lat = Math.asin(
                        Math.sin(latA) * Math.cos(angularDistance)
                        + Math.cos(latA) * Math.sin(angularDistance) * Math.cos(trueCourse));

        double lon = lonA
                + Math.atan2(Math.sin(trueCourse) * Math.sin(angularDistance)* Math.cos(latA),
                Math.cos(angularDistance) - Math.sin(latA) * Math.sin(lat));

        return new LatLng(lat * RadiansToDegrees, lon * RadiansToDegrees);
    }

    public static LatLngBounds calculateBoundingBox(LatLng center, double boundingBoxExtentMeters)
    {
        // pythagoras, isosceles triangle with a = boundingBoxExtentMeters
        double edgeDistance = Math.sqrt(boundingBoxExtentMeters*boundingBoxExtentMeters*2);

        LatLng ne = calculateDerivedPosition(center, edgeDistance, 45.0);
        LatLng sw = calculateDerivedPosition(center, edgeDistance, 225.0);

        return new LatLngBounds(sw, ne);
    }

}
