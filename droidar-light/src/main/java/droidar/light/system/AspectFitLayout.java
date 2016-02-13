package droidar.light.system;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class AspectFitLayout extends ViewGroup {

    private static final String TAG = AspectFitLayout.class.getSimpleName();

    private final View child;
    private int childWidth;
    private int childHeight;
    private boolean forceSurfaceLayout;

    public AspectFitLayout(Context context, View child) {
        super(context);

        this.child = child;
        addView(child, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        childWidth = 1;
        childHeight = 1;
    }

    public void setChildSize(int width, int height) {
        childWidth = width;
        childHeight = height;

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

            LayoutUtilities.aspectFitChild(child, width, height, childWidth, childHeight);
        }
    }
}
