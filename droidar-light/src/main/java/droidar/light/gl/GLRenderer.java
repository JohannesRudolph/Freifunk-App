package droidar.light.gl;

import android.opengl.GLSurfaceView.Renderer;
import android.os.Debug;
import android.os.SystemClock;
import android.util.Log;

import droidar.light.world.World;

public abstract class GLRenderer implements Renderer {
    private static final String TAG = GLRenderer.class.getSimpleName();

    /**
     * The maximum fps rate for the renderer. 40fps to be not so cpu intense
     */
    protected static final float MAX_FPS = 40;

    private float fovHorizontal = 35.0f;
    private float fovVertical = 10.0f;
    private float aspectRatio;

    public static float minViewDistance = 0.1f;
    public static float maxViewDistance = 5000.0f; // meters

    protected boolean pauseRenderer;
    protected long lastTimeInMs = SystemClock.uptimeMillis();


    public GLRenderer() {
        super();
    }


    public float getFovHorizontal() {
        return fovHorizontal;
    }

    public float getFovVertical() {
        return fovVertical;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public abstract void setWorld(World world);

    public void resume() {
        pauseRenderer(false);
    }

    private synchronized void pauseRenderer(boolean pauseRenderer) {
        this.pauseRenderer = pauseRenderer;
    }

    public void pause() {
        this.pauseRenderer(true);
    }

    /**
     * Sets the field of view (FOV) for the renderer
     * @param hfov horizontal FOV in degrees
     * @param vfov vertical FOV in degrees
     */
    public void setFov(float hfov, float vfov, float aspectRatio){
        if (hfov > 179.0f && hfov < 181.0f ){
            Log.d(TAG, "received nonsensical view angles from camera, correcting...");
            hfov = 80.0f;
            vfov = hfov / aspectRatio;
        }

        fovHorizontal = hfov;
        fovVertical = vfov;
        this.aspectRatio = aspectRatio;
    }
}