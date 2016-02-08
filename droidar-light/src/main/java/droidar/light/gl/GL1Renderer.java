package droidar.light.gl;

import android.opengl.GLU;
import android.os.SystemClock;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import droidar.light.gl.textures.TextureManager;
import droidar.light.world.World;

/**
 * This is the OpenGL renderer used for the {@link TransparentGLSurfaceView}
 *
 * @author Spobo
 */
public class GL1Renderer extends GLRenderer {

    private static final String TAG = GL1Renderer.class.getSimpleName();

    private final TextureManager textureManager;
    private World world;

    public GL1Renderer(TextureManager textureManager) {
        this.textureManager = textureManager;
    }

    public void setWorld(World world) {
        this.world = world;
    }


    @Override
    public void onDrawFrame(GL10 gl) {

        if (pauseRenderer) {
            startPauseLoop();
        }

        final long currentTime = SystemClock.uptimeMillis();

        // update textures
        textureManager.updateTextures(gl);

        // Clears the screen and depth buffer.
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        world.render(gl);


        final float delta = (currentTime - lastTimeInMs);
        lastTimeInMs = currentTime;

        if (delta > 0 && 1000 / delta > MAX_FPS) {
            // System.out.println("delta=" + delta);
            // System.out.println("FPS=" + 1000 / delta);
            // System.out.println("1000/MAX_FPS-delta=" + (long) (1000 / MAX_FPS - delta));
            try {
                Thread.sleep((long) (1000 / MAX_FPS - delta));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * do not kill the rendering thread, instead pause it this way because
     * otherwise the opengl resources would be released and the thread cant be
     * resatarted!
     */
    private void startPauseLoop() {
        Log.d("OpenGL", "Renderer paused");
        while (pauseRenderer) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d("OpenGL", "Renderer woken up");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        Log.d(TAG, String.format("onSurfaceChanged %dw|%dh", width, height));

        // Sets the current view port to the new size.
        gl.glViewport(0, 0, width, height);

		/*
         * Select the projection matrix which transforms the point from view
		 * space to homogeneous clipping space. Clip space is a right-handed
		 * coordinate droidar.light.system (+Z into the screen) contained within a canonical
		 * clipping volume extending from (-1,-1,-1) to (+1,+1,+1):
		 */
        gl.glMatrixMode(GL10.GL_PROJECTION);

        // Reset the projection matrix
        gl.glLoadIdentity();

		/*
         * GLU.gluPerspective parameters (see
		 * http://www.zeuscmd.com/tutorials/opengles/12-Perspective.php):
		 * 
		 * fovy - This specifies the field of view. A 90 degree angle means that
		 * you can see everything directly to the left right around to the right
		 * of you. This is not how humans see things. 45 degrees is a good value
		 * to start.
		 * 
		 * aspect - This specifies that aspect ratio that you desire. This is
		 * usually specified as the width divided by the height of the window.
		 * 
		 * zNear and zFar - This specifies the near and far clipping planes as
		 * normal.
		 */
        float aspectRatio = getAspectRatio();
        Log.d(TAG, String.format("onSurfaceChanged FOV %.1fh|%.1fv, aspect %.2f", getFovHorizontal(), getFovVertical(), aspectRatio));
        GLU.gluPerspective(gl, getFovVertical(), aspectRatio, minViewDistance, maxViewDistance);


		/*
		 * Select the modelview matrix which transforms a point from model space
		 * to view space, using a right-handed coordinate droidar.light.system with +Y up, +X
		 * to the right, and -Z into the screen:
		 */
        gl.glMatrixMode(GL10.GL_MODELVIEW);

		/*
		 * update this here to get a goot init value for lastTimeInMs
		 */
        lastTimeInMs = SystemClock.uptimeMillis();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        Log.d("Activity", "GLSurfaceView.onSurfaceCreated");

        // Set the background color to black (and alpha to 0) ( rgba ).
        gl.glClearColor(0, 0, 0, 0);
		/*
		 * To enable flat shading use droidar.light.gl.glShadeModel(GL10.GL_FLAT); default is
		 * GL_SMOOTH and GL_FLAT renders faces always with the same color,
		 * shading... so its a little cheaper then GL_SMOOTH but the polygons
		 * wont look realistic!
		 */
        // Depth buffer setup.
        gl.glClearDepthf(1.0f);
        // Enables depth testing.
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDisable(GL10.GL_DITHER);

        // The type of depth testing to do.
        gl.glDepthFunc(GL10.GL_LEQUAL);
        // Really nice perspective calculations.
        // droidar.light.gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);

		/*
		 * Transparancy
		 * 
		 * "The only sure way to achieve visually correct results is to sort and
		 * render your primitives from back to front."
		 * 
		 * http://www.opengl.org/sdk/docs/man/xhtml/glBlendFunc.xml
		 */
        gl.glEnable(GL10.GL_BLEND);
        // droidar.light.gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_DST_ALPHA);
        gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
        // Enable smooth shading for nice light effects

        gl.glShadeModel(GL10.GL_SMOOTH);
    }

}
