package droidar.light.system;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import droidar.light.gl.GLCamera;
import droidar.light.gl.GLFactory;
import droidar.light.gl.GLRenderer;
import droidar.light.gl.textures.TextureManager;
import droidar.light.sensors.SensorInputManager;
import droidar.light.world.SystemUpdater;
import droidar.light.world.World;


public abstract class Setup {

    private static final String LOG_TAG = "Setup";

    private Activity targetActivity;

    private TextureManager textureManager;
    private GLRenderer glRenderer;
    private GLCamera camera;
    private World world;

    private SystemUpdater worldUpdater;
    private SensorInputManager sensorInput;

    private TransparentGLSurfaceView glView;
    private AspectFitLayout augmentationOverlay;
    private CameraView cameraView;
    private View guiOverlay;
    ;

    public GLCamera getCamera() {
        return camera;
    }

    public World getWorld() {
        return world;
    }

    public GLRenderer getGlRenderer() {
        return glRenderer;
    }

    public AspectFitLayout getAugmentationOverlay() {
        return augmentationOverlay;
    }
    /**
     * This method has to be executed in the activity which want to display the
     * AR content. In your activity do something like this:
     * <p>
     * <pre>
     * public void onCreate(Bundle savedInstanceState) {
     * 	super.onCreate(savedInstanceState);
     * 	new MySetup(this).run();
     * }
     * </pre>
     *
     * @param target
     */
    public void run(Activity target) {
        targetActivity = target;

        textureManager = new TextureManager();
        glRenderer = buildRenderer(textureManager);

        worldUpdater = new SystemUpdater();

        camera = new GLCamera();
        world = buildWorld(camera, new GLFactory(targetActivity, camera, textureManager));
        worldUpdater.addObjectToUpdateCycle(world);

        // attach world to renderer
        glRenderer.setWorld(world);

        // setting up the sensor Listeners
        sensorInput = new SensorInputManager(targetActivity);
        initializeSensorInputListeners(sensorInput, worldUpdater);
        sensorInput.registerSensors();

        // World Update Thread:
        Thread worldThread = new Thread(worldUpdater);
        worldThread.start();

        cameraView = buildCameraView(targetActivity);
        glView = buildGlView(glRenderer);
        augmentationOverlay = new AspectFitLayout(targetActivity, glView);

        guiOverlay = buildGuiOverlayView(targetActivity);

        // add the camera view at bottom, glView in the middle and guiOverlay on top
        targetActivity.addContentView(cameraView, layoutParams(LayoutParams.MATCH_PARENT));
        targetActivity.addContentView(augmentationOverlay, layoutParams(LayoutParams.MATCH_PARENT));
        targetActivity.addContentView(guiOverlay, layoutParams(LayoutParams.MATCH_PARENT));
    }

    @NonNull
    private LayoutParams layoutParams(int matchParent) {
        return new LayoutParams(matchParent, matchParent);
    }

    protected abstract GLRenderer buildRenderer(TextureManager textureManager);

    protected abstract World buildWorld(GLCamera camera, GLFactory objectFactory);

    protected abstract void initializeSensorInputListeners(SensorInputManager sensorInput, SystemUpdater updater);

    protected abstract CameraView buildCameraView(Activity a);

    protected TransparentGLSurfaceView buildGlView(GLRenderer glRenderer) {
        TransparentGLSurfaceView arView = new TransparentGLSurfaceView(targetActivity);
        arView.setRenderer(glRenderer);

        return arView;
    }

    protected abstract View buildGuiOverlayView(Activity activity);

    /**
     * see {@link Activity#onDestroy}
     *
     * @param a
     */
    public final void onDestroy(Activity a)   {
        worldUpdater.killUpdaterThread();
    }

    /**
     * see {@link Activity#onStart}
     *
     * @param a
     */
    public void onStart(Activity a) {
        Log.d(LOG_TAG, "main onStart (setup=" + this + ")");
    }

    /**
     * When this is called the activity is still visible!
     * <p>
     * see {@link Activity#onPause}
     *
     * @param a
     */
    public void onPause(Activity a) {
        Log.d(LOG_TAG, "main onPause (setup=" + this + ")");
    }

    /**
     * see {@link Activity#onResume}
     *
     * @param a
     */
    public void onResume(Activity a) {
        Log.d(LOG_TAG, "main onResume (setup=" + this + ")");
    }

    /**
     * When this is called the activity is no longer visible. Camera see
     * {@link Activity#onStop}
     *
     * @param a
     */
    public void onStop(Activity a) {
        Log.d(LOG_TAG, "main onStop (setup=" + this + ")");
        pauseRenderer();
        pauseUpdater();
        // the cameraView manages itself via Surface created/destroyed
        pauseSensorInput();
    }

    /**
     * see {@link Activity#onRestart}
     *
     * @param a
     */
    public void onRestart(Activity a) {
        Log.d(LOG_TAG, "main onRestart (setup=" + this + ")");
        resumeRenderer();
        resumeUpdater();
        // the cameraView manages itself via Surface created/destroyed
        resumeSensorInput();
    }

    // todo: we can probably spare all the individual null checks and just use an is initialized flag in onRestart()/onStop()

    public void pauseSensorInput() {
        sensorInput.unregisterSensors();
    }

    public void resumeSensorInput() {
        sensorInput.registerSensors();
    }

    public void pauseUpdater() {
        if (worldUpdater != null) {
            Log.d(LOG_TAG, "Pausing world updater now");
            worldUpdater.pauseUpdater();
        }
    }

    public void resumeUpdater() {
        if (worldUpdater != null) {
            worldUpdater.resumeUpdater();
        }
    }


    public void pauseRenderer() {
        if (glRenderer != null) {
            Log.d(LOG_TAG, "Pausing renderer and GLSurfaceView now");
            glRenderer.pause();
            if (glView != null) {
                glView.onPause();
            }
        }
    }

    public void resumeRenderer() {
        if (glRenderer != null) {
            Log.d(LOG_TAG, "Resuming renderer and GLSurfaceView now");
        }
        glRenderer.resume();
        if (glView != null) {
            glView.onResume();
        }
    }
}
