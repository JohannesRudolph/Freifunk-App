package de.inmotion_sst.freifunkfinder.ar;

import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;

import javax.microedition.khronos.opengles.GL10;

import de.inmotion_sst.freifunkfinder.Node;
import droidar.light.gl.GLFactory;
import droidar.light.gl.scenegraph.MeshComponent;
import droidar.light.util.Vec;
import droidar.light.world.GeoWorldObject;
import droidar.light.world.World;

class NodeGeoWorldObject extends GeoWorldObject {
    private static final String TAG = NodeGeoWorldObject.class.getSimpleName();

    private static final float nameSize = 10.0f;
    private static final float arrowScale = 5.0f;
    private static final float distanceChangeThreshold = 1.0f;

    private final Node node;
    private final GLFactory glFactory;
    private final MeshComponent mesh;

    private MeshComponent nodeName;
    private float displayedDistance;

    public NodeGeoWorldObject(Node node, GLFactory glFactory, Location zeroLocation) {
        super(buildMeshComponent(), node.getLat(), node.getLon(), getPreferredAltitude(zeroLocation, node));

        this.node = node;
        this.glFactory = glFactory;
        this.mesh = (MeshComponent) getRenderable();

        initializeMesh();
    }

    private static double getPreferredAltitude(Location zeroLocation, Node node) {
        double zeroAlt = zeroLocation.getAltitude();
        double nodeAlt = node.getAlt();

        // some nodes put elevation above ground (not elevation above sea-level = altitude) in the alt field
        // we compensate this here, the values chosen here are arbitrary and can never deal with all cases (e.g. standing on a skyscraper).
        // a better approach would be to deal with this in the crawler that collects the data, but this would require it to use some GeoApi for getting correct elevation at a location

        boolean nodeBelowZero = (nodeAlt - zeroAlt) < - 10; // node is significantly below the current level
        boolean nodeAltitudeIsLikelyElevation = nodeAlt < 10; // node altitude is reasonably close to ground

        if (nodeBelowZero && nodeAltitudeIsLikelyElevation ){
            double correctedAlt = zeroAlt + nodeAlt;

            Log.v(TAG, String.format("Node \"%s\" likely has wrong altitude data (node: %.1fm, zero: %.1fm), correcting to: %.1fm", node.getName(), nodeAlt, zeroAlt, correctedAlt));

            return correctedAlt;
        }

        return nodeAlt;
    }

    @NonNull
    private static MeshComponent buildMeshComponent() {
        MeshComponent group = new MeshComponent() {
            @Override
            public void draw(GL10 gl) {
                // the group has no group-level drawing routines
            }
        };

        return group;
    }


    @NonNull
    private final void initializeMesh() {
        MeshComponent arrow = glFactory.newArrow();
        arrow.setScale(new Vec(arrowScale, arrowScale, arrowScale));

        mesh.addChild(arrow);
    }

    @Override
    public void update(float timeDelta, World world) {
        super.update(timeDelta, world);

        Vec d = mesh.getPosition().copy();
        d.sub(world.getCamera().getPosition()); // result in d

        float distance = d.getLength();

        if(Math.abs(displayedDistance - distance) > distanceChangeThreshold){
            displayedDistance = distance;

            // remove from mesh if we have one
            if (nodeName != null)
                mesh.removeChild(nodeName);

            String text = String.format("%s\n%.0fm", node.getName(), displayedDistance);
            nodeName = glFactory.newTextObject(text, nameScaleForDistance(displayedDistance));
            mesh.addChild(nodeName);

            // we could possibly also apply an inverse scale so that far away nodes do not appear too small
        }
    }

    private float nameScaleForDistance(float displayedDistance) {
        // slightly increase size for nodes that are far away to compensate perspective effect
        return nameSize + (0.05f * (displayedDistance));
    }
}
