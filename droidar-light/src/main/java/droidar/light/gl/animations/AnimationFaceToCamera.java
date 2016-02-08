package droidar.light.gl.animations;

import javax.microedition.khronos.opengles.GL10;

import droidar.light.gl.GLCamera;
import droidar.light.util.Vec;

public class AnimationFaceToCamera extends GLAnimation {

    private static final String TAG = AnimationFaceToCamera.class.getSimpleName();

    private GLCamera camera;
    private Vec rotationVec = new Vec();
    private Vec newRotationVec = new Vec();

    private float lastUpdateAway;
    private final float myUpdateDelay = 0.1f;

    public AnimationFaceToCamera(GLCamera targetCamera) {
        camera = targetCamera;
    }

    @Override
    public boolean update(float timeDelta) {

		/*
         * TODO use mesh instead of assigning a mesh while creating this
		 * animation!
		 */
        timeDelta = Math.abs(timeDelta);
        lastUpdateAway += timeDelta;

        if (lastUpdateAway > myUpdateDelay) {
            updateRotation();
            lastUpdateAway = 0;
        }

        synchronized (rotationVec) {
            boolean rotateThreeD = true;
            if (!rotateThreeD) {
                Vec.morphToNewAngleVec(rotationVec, 0, 0, newRotationVec.z, timeDelta);
            } else {
                Vec.morphToNewAngleVec(rotationVec, newRotationVec.x, newRotationVec.y, newRotationVec.z, timeDelta);
            }
        }
        return true;
    }


    private void updateRotation() {
        Vec absolutePosition = getTarget().calculateAbsolutePosition();

        newRotationVec.toAngleVec(absolutePosition, camera.getPosition());
        /*
         * substract 90 from the x value becaute calcanglevec returns 90 if
         * the rotation should be the horizon (which would mean no object
         * rotation)
         */
        newRotationVec.x -= 90;
        newRotationVec.z *= -1;
    }


    @Override
    public void render(GL10 gl) {
        synchronized (rotationVec) {
            gl.glRotatef(rotationVec.z, 0, 0, 1);
            gl.glRotatef(rotationVec.x, 1, 0, 0);
            gl.glRotatef(rotationVec.y, 0, 1, 0);
        }
    }
}
