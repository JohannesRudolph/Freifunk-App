package droidar.light.world;

import droidar.light.gl.scenegraph.MeshComponent;
import droidar.light.util.Vec;
import droidar.light.world.World;
import droidar.light.world.WorldObject;

/**
 * This is a subclass of {@link WorldObject} which has a fixed GPS position in the
 * virtual world. It is the default class to create any object with a location
 *
 * @author Spobo
 */
public class GeoWorldObject extends WorldObject {

    private static final String LOG_TAG = "GeoWorldObject";

    private final MeshComponent meshComponent;

    private double latitude;
    private double longitude;
    private double altitude;

    public GeoWorldObject(MeshComponent renderable, double latitude, double longitude, double altitude) {
        super(renderable);

        meshComponent = renderable;

        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    @Override
    public void update(float timeDelta, World world) {
        super.update(timeDelta, world);
    }

    @Override
    public void onAddedToWorld(World world) {
        super.onAddedToWorld(world);

        updateVirtualPosition(world);
    }

    private void updateVirtualPosition(World world) {
        Vec position = world.getVirtualPosition(latitude, longitude, altitude);
        meshComponent.setPosition(position);
    }
}
