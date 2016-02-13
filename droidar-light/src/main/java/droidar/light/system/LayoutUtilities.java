package droidar.light.system;

import android.util.Log;
import android.view.View;

public class LayoutUtilities {

    private static final String TAG = LayoutUtilities.class.getSimpleName();

    public static void aspectFitChild(View child, int width, int height, int previewWidth, int previewHeight) {
        int cl, ct, cr, cb;

        // Center the child SurfaceView within the parent
        // decide whether to fill the height or width of the screen
        if (width * previewHeight > height * previewWidth) {
            // fill height
            final int scaledChildWidth = previewWidth * height / previewHeight;
            cl = (width - scaledChildWidth) / 2;
            ct = 0;
            cr = (width + scaledChildWidth) / 2;
            cb = height;
        } else {
            // fill width
            final int scaledChildHeight = previewHeight * width / previewWidth;
            cl = 0;
            ct = (height - scaledChildHeight) / 2;
            cr = width;
            cb = (height + scaledChildHeight) / 2;
        }

        Log.v(TAG, String.format("aspectFitChild, child.layout(%d, %d, %d, %d)", cl, ct, cr, cb));
        child.layout(cl, ct, cr, cb);
    }
}
