package droidar.light.world;

import android.location.Location;
import android.util.Log;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import droidar.light.gl.GLCamera;
import droidar.light.gl.scenegraph.RenderList;
import droidar.light.util.Vec;

public class World implements Updatable {

    private static final String LOG_TAG = "World";
    /**
     * the camera which is responsible to display the world correctly
     */
    private final GLCamera camera;
    private final ArrayList<WorldObject> worldObjects;

    private Location zeroLocation;

    public World(GLCamera glCamera, Location zeroLocation) {
        this.camera = glCamera;
        this.zeroLocation = zeroLocation;

        this.worldObjects = new ArrayList<>();

        this.camera.setPosition(this.getVirtualPosition(zeroLocation)); // should be (0,0,0)
    }

    public GLCamera getCamera() {
        return camera;
    }

    public void add(WorldObject x) {
        if (worldObjects.contains(x)) {
            throw new UnsupportedOperationException("world already contains object");
        }

        Log.v(LOG_TAG, String.format("Adding %s to %s", x, this));

        worldObjects.add(x);
        x.onAddedToWorld(this);
    }

    public void render(GL10 gl) {
        camera.render(gl);

        for (WorldObject wo : worldObjects) {
            wo.getRenderable().render(gl);
        }
    }

    @Override
    public boolean update(float timeDelta) {
        for (WorldObject wo : worldObjects) {
            wo.update(timeDelta, this);
        }

        return true;
    }

    public Vec getVirtualPosition(Location myLocation) {
        return getVirtualPosition(myLocation.getLatitude(), myLocation.getLongitude(), myLocation.getAltitude());
    }

    public Vec getVirtualPosition(double latitude, double longitude, double altitude) {
        /*
         * The longitude calculation depends on current latitude: The
		 * circumference of a circle at a given latitude is proportional to the
		 * cosine, so the formula is:
		 *
		 * (myLongitude - zeroLongitude) * 40075017 / 360 * cos(zeroLatitude)
		 *
		 * earth circumfence through poles is 40008000
		 *
		 * earth circumfence at equator is 40075017
		 *
		 * degree to radians: PI/180=0.0174532925
		 *
		 * TODO check what happens when myLongi is positive but zeroLongi is
		 * negative for example. this can create problems! both have to be
		 * negative or positive otherwise delta value is wrong! this will nearly
		 * never happen, but for people in Greenwhich eg it might be a problem
		 * when living near the 0 latitude..
		 */

        // TODO: JR - haven't checked whether this algorithm is accurate, better alternatives certainly exist
        // (e.g. linear interpolation based on Haversine calculated box around zero)
        Vec position = new Vec();
        position.x =
                (float) ((longitude - zeroLocation.getLongitude())
                        * 111319.4917
                        * Math.cos(zeroLocation.getLatitude() * 0.0174532925));

        position.y = (float) ((latitude - zeroLocation.getLatitude()) * 111133.3333);
        position.z = (float) (altitude - zeroLocation.getAltitude());

        return position;
    }
}
