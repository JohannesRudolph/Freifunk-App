package de.inmotion_sst.freifunkfinder.ar;

import android.app.Activity;
import android.location.Location;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import de.inmotion_sst.freifunkfinder.Node;
import droidar.light.actions.ActionMoveCameraBuffered;
import droidar.light.actions.ActionRotateCameraBuffered;
import droidar.light.gl.GL1Renderer;
import droidar.light.gl.GLCamera;
import droidar.light.gl.GLFactory;
import droidar.light.gl.GLRenderer;
import droidar.light.gl.textures.TextureManager;
import droidar.light.sensors.SensorInputManager;
import droidar.light.system.CameraView;
import droidar.light.system.Setup;
import droidar.light.world.SystemUpdater;
import droidar.light.world.World;
import java8.util.stream.Stream;

public class SurroundingNodesSetup extends Setup implements CameraView.CameraParametersCallback {

    private final Location zeroLocation;
    private final Stream<Node> nodes;

    public SurroundingNodesSetup(Location myLocation, Stream<Node> nodes) {
        zeroLocation = myLocation;
        this.nodes = nodes;
    }


    @Override
    protected GLRenderer buildRenderer(TextureManager textureManager) {
        return new GL1Renderer(textureManager);
    }

    @Override
    public World buildWorld(GLCamera camera, GLFactory objectFactory) {
        World world = new World(camera, zeroLocation);

        nodes.map(x -> new NodeGeoWorldObject(x, objectFactory, zeroLocation)).forEach(x -> world.add(x));

        return world;
    }

    @Override
    public void initializeSensorInputListeners(SensorInputManager sensorInput, SystemUpdater updater) {
        ActionRotateCameraBuffered rotate = new ActionRotateCameraBuffered(getCamera());
        //updater.addObjectToUpdateCycle(rotate);

        sensorInput.addRotationMatrixListener(rotate);

        ActionMoveCameraBuffered move = new ActionMoveCameraBuffered(getCamera(), getWorld());
        updater.addObjectToUpdateCycle(move);

        sensorInput.addLocationListener(move);
    }

    @Override
    protected CameraView buildCameraView(Activity a) {
        return new CameraView(a, this);
    }

    @Override
    public View buildGuiOverlayView(Activity activity) {
        // can also inflate from xml when required
        return new FrameLayout(activity);
    }

    @Override
    public void cameraPreviewChanged(int width, int height, double hfov, double vfov) {

        getGlRenderer().setFov((float)Math.toDegrees(hfov), (float)Math.toDegrees(vfov), (float)width/(float)height);
    }
}

