package droidar.light.actions;

import droidar.light.gl.HasPosition;
import droidar.light.gl.scenegraph.MeshComponent;

import droidar.light.util.Vec;
import droidar.light.world.Updatable;

public class MoveAnimation implements Updatable {

    /**
     * this vector is the new position, where to send the {@link MeshComponent}
     * of the parent {@link HasPosition} to
     */
    public Vec myTargetPos = new Vec();
    private float mySpeedFactor;
    private final HasPosition subject;


    /**
     * @param speedFactor try values from 1 to 10. bigger means faster and 20 looks
     *                    nearly like instant placing so values should be < 20!
     */
    public MoveAnimation(HasPosition subject, float speedFactor) {
        this.subject = subject;
        this.mySpeedFactor = speedFactor;
    }

    public boolean update(float timeDelta) {

        Vec pos = subject.getPosition();

        if (pos != null) {
            Vec.morphToNewVec(pos, myTargetPos, timeDelta * mySpeedFactor);
        }

        return true;
    }
}
