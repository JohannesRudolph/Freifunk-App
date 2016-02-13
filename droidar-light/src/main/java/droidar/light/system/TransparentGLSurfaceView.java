package droidar.light.system;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

/**
 * This is the custom {@link GLSurfaceView} which is used to render the OpenGL content.
*/
public class TransparentGLSurfaceView extends GLSurfaceView {
	/**
	 * enables the opengl es debug output but reduces the frame-rate a lot!
	 */
	private static final boolean DEBUG_OUTPUT_ENABLED = false;


	public TransparentGLSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initGLSurfaceView();
	}

	public TransparentGLSurfaceView(Context context) {
		super(context);
		initGLSurfaceView();
	}

	private void initGLSurfaceView() {
		if (DEBUG_OUTPUT_ENABLED) {
			// Turn on error-checking and logging
			setDebugFlags(DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS);
		}

		this.setFocusableInTouchMode(true);

		// Set 8888 pixel format because that's required for
		// a translucent window:
		this.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

		// Use a surface format with an Alpha channel:
		this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
		this.setZOrderMediaOverlay(true);
	}
}
