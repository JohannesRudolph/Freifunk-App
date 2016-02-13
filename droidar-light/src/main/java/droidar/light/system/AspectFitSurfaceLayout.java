package droidar.light.system;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;

/**
 * Fits a child SurfaceView while maintaining the original aspect ratio.
 * See http://stackoverflow.com/a/24671000/125407 for terminology
 */
public class AspectFitSurfaceLayout extends ViewGroup {

    private static final String TAG = AspectFitSurfaceLayout.class.getSimpleName();

    private final SurfaceView child;
    private Size childSize;
    private boolean forceSurfaceLayout;

    public AspectFitSurfaceLayout(Context context, SurfaceView child) {
        super(context);

        this.child = child;
        addView(child, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    public SurfaceView getChild() {
        return child;
    }

    public void setChildSize(int width, int height) {
        childSize = new Size(width, height);
        forceSurfaceLayout();
    }

    public void resetChildSize() {
        childSize = null;
    }

    protected void forceSurfaceLayout() {
        forceSurfaceLayout = true;
        requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.v(TAG, String.format("onLayout(%b, %d, %d, %d, %d), forceSurfaceLayout %b", changed, l, t, r, b, forceSurfaceLayout));

        if (changed || forceSurfaceLayout) {
            forceSurfaceLayout = false;

            final int width = r - l;
            final int height = b - t;

            int childWidth = width;
            int childHeight = height;

            if (childSize != null) {
                childWidth = childSize.width;
                childHeight = childSize.height;
            }

            Log.v(TAG, String.format("onLayout, dimensions: %dw|%dh, preview: %dw|%dh", width, height, childWidth, childHeight));

            // aspect fit layout
            int cl, ct, cr, cb;

            // Center the child SurfaceView within the parent
            // decide whether to fill the height or width of the screen
            if (width * childHeight > height * childWidth) {
                // fill height
                final int scaledChildWidth = childWidth * height / childHeight;
                cl = (width - scaledChildWidth) / 2;
                ct = 0;
                cr = (width + scaledChildWidth) / 2;
                cb = height;
            } else {
                // fill width
                final int scaledChildHeight = childHeight * width / childWidth;
                cl = 0;
                ct = (height - scaledChildHeight) / 2;
                cr = width;
                cb = (height + scaledChildHeight) / 2;
            }

            Log.v(TAG, String.format("onLayout, child.layout(%d, %d, %d, %d)", cl, ct, cr, cb));
            child.layout(cl, ct, cr, cb);
        }
    }

    private class Size{
        public final int width;
        public final int height;

        public Size(int width, int height) {

            this.width = width;
            this.height = height;
        }
    }
}
