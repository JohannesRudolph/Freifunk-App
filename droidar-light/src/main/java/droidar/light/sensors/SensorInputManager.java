package droidar.light.sensors;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;

import java8.util.stream.StreamSupport;

public class SensorInputManager implements SensorEventListener, android.location.LocationListener {
    private static final String TAG = SensorInputManager.class.getSimpleName();

    private final List<LocationListener> locationListeners;
    private final List<RotationMatrixListener> rotationMatrixListeners;
    private final Activity activity;

    public SensorInputManager(Activity activity) {
        this.activity = activity;

        locationListeners = new ArrayList<>();
        rotationMatrixListeners = new ArrayList<>();
    }

    public void addLocationListener(LocationListener listener){
        locationListeners.add(listener);
    }

    public void addRotationMatrixListener(RotationMatrixListener listener){
        rotationMatrixListeners.add(listener);
    }

    public void registerSensors() {
        SensorManager sensorManager = getSensorManager();

        int sensorDelay = SensorManager.SENSOR_DELAY_GAME;

        Sensor magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(this, magnetSensor, sensorDelay);

        Sensor accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelSensor, sensorDelay);

        Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, gyro, sensorDelay);

        Sensor sensorFusion = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(this, sensorFusion, sensorDelay);

        LocationManager locationManager = getLocationManager();
        LocationProvider gps = locationManager.getProvider(LocationManager.GPS_PROVIDER);

        // todo: jr check gps enabled, ask permission, display snackbar
        // locationManager.isProviderEnabled()

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(gps.getName(), 0, 0, this);

    }

    public void unregisterSensors(){
        getSensorManager().unregisterListener(this);
        getLocationManager().removeUpdates(this);
    }

    private android.hardware.SensorManager getSensorManager() {
        return (android.hardware.SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
    }
    private LocationManager getLocationManager() {
        return (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
    }


    // SensorListener

    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {
        final int sensorType = sensorEvent.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_ROTATION_VECTOR:
                updateRotation(sensorEvent);
                break;

            // forward all sensor events to processor
            default:
                //Log.e(TAG, "Unsupported sensorEvent type: " + sensorType);
        }
    }

    private void updateRotation(SensorEvent sensorEvent) {
        // could spare a few allocs here by reusing these, but then no one may hold onto the data therein
        float[] rawRotationMatrix = new float[16];
        float[] remappedRotationMatrix = new float[16];

        // rotation matrix is in rawRotationMatrix after this call
        SensorManager.getRotationMatrixFromVector(rawRotationMatrix, sensorEvent.values);

        // corrected rotation matrix is in remappedRotationMatrix
        remapCoordinateSystemAccordingToScreenRotation(rawRotationMatrix, remappedRotationMatrix);

        // notify listeners
        StreamSupport.stream(rotationMatrixListeners).forEach(x -> x.onRotationMatrixChanged(remappedRotationMatrix));
    }

    public float[] remapCoordinateSystemAccordingToScreenRotation(float[] in, float[] out) {
        int screenRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int axisX;
        int axisY;

        switch (screenRotation) {
            case Surface.ROTATION_90:
                axisX = SensorManager.AXIS_Y;
                axisY = SensorManager.AXIS_MINUS_X;
                break;
            case Surface.ROTATION_180:
                axisX = SensorManager.AXIS_MINUS_X;
                axisY = SensorManager.AXIS_MINUS_Y;
                break;
            case Surface.ROTATION_270:
                axisX = SensorManager.AXIS_MINUS_Y;
                axisY = SensorManager.AXIS_X;
                break;
            case Surface.ROTATION_0:
            default:
                axisX = SensorManager.AXIS_X;
                axisY = SensorManager.AXIS_Y;
                break;
        }

        SensorManager.remapCoordinateSystem(in, axisX, axisY, out);

        return out;
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // todo: jr warn when accuracy low (especially magnetometer!) -> post toast or SnackBar
    }

    // LocationListener

    @Override
    public void onLocationChanged(Location location) {
        // todo: jr filter locations
        StreamSupport.stream(locationListeners).forEach(x -> x.onLocationChanged(location));
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
