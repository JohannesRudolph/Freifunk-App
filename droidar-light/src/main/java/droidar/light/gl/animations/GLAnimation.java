package droidar.light.gl.animations;

import droidar.light.gl.scenegraph.MeshComponent;
import droidar.light.world.Entity;

/**
 * An animation that is used purely for drawing, e.g. it does not affect world state.
 * Add this to a MeshComponent
 *
 */
public abstract class GLAnimation implements Entity {
    private MeshComponent target;

    public void setTarget(MeshComponent target) {
        this.target = target;
    }

    public MeshComponent getTarget() {
        return target;
    }
}
