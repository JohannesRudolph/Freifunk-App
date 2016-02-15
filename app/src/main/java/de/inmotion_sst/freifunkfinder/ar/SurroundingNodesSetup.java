package de.inmotion_sst.freifunkfinder.ar;

import android.app.Activity;
import android.location.Location;
import android.view.View;
import android.widget.FrameLayout;

import de.inmotion_sst.freifunkfinder.Node;
import droidar.light.actions.ActionMoveCameraBuffered;
import droidar.light.actions.ActionRotateCamera;
import droidar.light.gl.GL1Renderer;
import droidar.light.gl.GLCamera;
import droidar.light.gl.GLFactory;
import droidar.light.gl.GLRenderer;
import droidar.light.gl.textures.TextureManager;
import droidar.light.sensors.SensorInputManager;
import droidar.light.system.Setup;
import droidar.light.world.SystemUpdater;
import droidar.light.world.World;
import java8.util.stream.Stream;

public class SurroundingNodesSetup extends Setup {

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
        ActionRotateCamera rotate = new ActionRotateCamera(getCamera());
        //updater.addObjectToUpdateCycle(rotate);

        sensorInput.addRotationMatrixListener(rotate);

        ActionMoveCameraBuffered move = new ActionMoveCameraBuffered(getCamera(), getWorld());
        updater.addObjectToUpdateCycle(move);

        sensorInput.addLocationListener(move);
    }

    @Override
    public View buildGuiOverlayView(Activity activity) {
        // can also inflate from xml when required
        return new FrameLayout(activity);
    }

}

