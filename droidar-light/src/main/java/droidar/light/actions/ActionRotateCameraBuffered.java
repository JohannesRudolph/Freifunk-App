package droidar.light.actions;

import droidar.light.gl.GLCamera;
import droidar.light.sensors.RotationMatrixListener;

public class ActionRotateCameraBuffered implements RotationMatrixListener {
    private final GLCamera camera;

    public ActionRotateCameraBuffered(GLCamera targetCamera) {
        camera = targetCamera;
    }

    @Override
    public void onRotationMatrixChanged(float[] rotMatrix) {
        camera.setRotationMatrix(rotMatrix);
    }
}
