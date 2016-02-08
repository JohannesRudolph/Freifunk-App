package droidar.light.actions;

import android.location.Location;

import droidar.light.gl.GLCamera;
import droidar.light.sensors.LocationListener;
import droidar.light.world.Updatable;
import droidar.light.world.World;

public class ActionMoveCameraBuffered implements Updatable, LocationListener {
    private final World world;
    private final MoveAnimation mover;

    public ActionMoveCameraBuffered(GLCamera camera, World world) {
        this.world = world;

        mover = new MoveAnimation(camera, 3);
    }

    @Override
    public void onLocationChanged(Location location) {
        mover.myTargetPos = world.getVirtualPosition(location);
    }

    @Override
    public boolean update(float timeDelta) {
        return mover.update(timeDelta);
    }
}
