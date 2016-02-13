package droidar.light.system;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

import java8.util.stream.StreamSupport;

// This ViewGroup creates one child SurfaceView for the live camera feed, this child view may be larger or smaller than the screen (but in any case it is centered on screen)
// TODO: think about whether we want to force this to be always larger than the screen (so no black bars anywhere)

@SuppressWarnings("deprecation") // can't use CameraApi2 <= API 21, hence disable the deprecation warning for the old CameraApi
public class CameraView extends AspectFitSurfaceLayout implements SurfaceHolder.Callback {
    public interface CameraParametersCallback {
        /**
         * @param width  width of preview
         * @param height height of preview
         * @param hfov   horizontal FOV in degrees
         * @param vfov   vertical FOV in degrees
         */
        void cameraPreviewChanged(int width, int height, double hfov, double vfov);
    }

    private static final String TAG = CameraView.class.getSimpleName();

    private final CameraParametersCallback parametersCallback;

    private SurfaceHolder surfaceHolder;
    private Camera.Size previewSize;
    private List<Camera.Size> supportedPreviewSizes;
    private Camera camera;

    public CameraView(Context context, CameraParametersCallback parametersCallback) {
        super(context, new SurfaceView(context));
        this.parametersCallback = parametersCallback;

        // install a SurfaceHolder.Callback for notifications when the underlying surface is created and destroyed
        surfaceHolder = getChild().getHolder();
        surfaceHolder.addCallback(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        setMeasuredDimension(width, height);

        Log.v(TAG, String.format("onMeasure, dimensions %dw|%dh", width, height));
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, String.format("surfaceCreated"));

        openCamera();
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, String.format("surfaceDestroyed"));

        releaseCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.v(TAG, String.format("surfaceChanged, dimensions: %dw|%dh", w, h));

        boolean isFirstSurfaceChanged = previewSize == null;

        // update optimal previewSize
        previewSize = getOptimalPreviewSize(supportedPreviewSizes, getMeasuredWidth(), getMeasuredHeight());

        Camera.Parameters parameters = camera.getParameters();

        // force setting the parameters on the first surface changed event or when the size actually changes
        // important to force setting the parameters on first surface change event because they may already default to the correct size
        boolean previewSizeChanged = isFirstSurfaceChanged || !equalsPreviewSize(parameters.getPreviewSize());

        // now that the size is known, set up the new camera parameters and begin the preview.
        parameters.setPreviewSize(previewSize.width, previewSize.height);

        tryApplyAspectRatioFixForBuggyDevices(parameters);
        trySetAutofocus(parameters);

        if (previewSizeChanged) {
            // need to force surface layout when we change camera preview size, even if view dimensions on screen didn't change
            setChildSize(previewSize.width, previewSize.height);

            // not all devices support changing parameters while preview is running so stop it beforehand
            // this does nothing if camera wasn't started yet
            Log.v(TAG, String.format("surfaceChanged, set camera parameters to: %dw|%dh", previewSize.width, previewSize.height));

            camera.stopPreview();
            camera.setParameters(parameters);

            notifyCallback(parameters);
        }

        // ensure camera preview is started, this does nothing if camera wasn't started yet
        camera.startPreview();
    }

    private void notifyCallback(Camera.Parameters p) {
        final float defaultHfov = 80.0f;

        int zoom = p.getZoomRatios().get(p.getZoom()).intValue();
        Camera.Size sz = p.getPreviewSize();

        double aspect = (double) sz.width / (double) sz.height;
        double thetaV = Math.toRadians(p.getVerticalViewAngle());
        double thetaH = 2d * Math.atan(aspect * Math.tan(thetaV / 2));

        thetaV = 2d * Math.atan(100d * Math.tan(thetaV / 2d) / zoom);
        thetaH = 2d * Math.atan(100d * Math.tan(thetaH / 2d) / zoom);

        double hfov = Math.toDegrees(thetaH);
        double vfov = Math.toDegrees(thetaV);
        if (hfov > 179.0f && hfov < 181.0f) {
            Log.d(TAG, "received nonsensical view angles from camera, setting to default values...");
            hfov = defaultHfov;
            vfov = hfov / aspect;
        }

        Log.v(TAG, String.format("notifyCallback, cameraPreviewChanged(%d, %d, %.1f, %.1f)", sz.width, sz.height, hfov, vfov));
        parametersCallback.cameraPreviewChanged(sz.width, sz.height, hfov, vfov);
    }

    private void trySetAutofocus(Camera.Parameters parameters) {
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
    }

    private void tryApplyAspectRatioFixForBuggyDevices(Camera.Parameters parameters) {
        // see https://code.google.com/p/android/issues/detail?id=73316
        // this is such a royal PITA... affected devices e.g. Nexus 7 2013
        boolean supportsSamePictureSize = StreamSupport
                .stream(parameters.getSupportedPictureSizes())
                .anyMatch(x -> equalsPreviewSize(x));

        if (supportsSamePictureSize)
            parameters.setPictureSize(previewSize.width, previewSize.height);
    }

    private boolean equalsPreviewSize(Camera.Size x) {
        return x.width == previewSize.width && x.height == previewSize.height;
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;

        if (sizes == null) {
            return null;
        }

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        Log.v(TAG, String.format("Selected optimal preview size for %dw|%dh: %dw|%dh", w, h, optimalSize.width, optimalSize.height));
        return optimalSize;
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;

            // reset preview size and supported preview sizes
            previewSize = null;
            resetChildSize();

            supportedPreviewSizes = null;
        }
    }

    private void openCamera() {
        if (camera != null)
            return;

        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            camera = Camera.open();
            camera.setPreviewDisplay(surfaceHolder);

            // reset preview size and supported preview sizes
            previewSize = null;
            resetChildSize();

            supportedPreviewSizes = this.camera.getParameters().getSupportedPreviewSizes();
        } catch (IOException exception) {
            Log.e(TAG, "IOException in surfaceCreated:", exception);
        }
    }
}