package de.inmotion_sst.freifunkfinder.ar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import de.inmotion_sst.freifunkfinder.R;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class CameraFinderActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 500;
    private static SurroundingNodesSetup staticSetupHolder;

    private final Handler hideHandler;
    private final Runnable hideRunnable;
    private final Runnable hidePart2Runnable;
    private final Runnable showPart2Runnable;
    private final SurroundingNodesSetup setup;

    private View contentView;

    private boolean visible;

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener delayHideTouchListener;

    public static void startWithSetup(Activity currentActivity, @NonNull SurroundingNodesSetup setupToUse) {
        // this is a hack for starting the activity with a setup, without making that setup serializable
        CameraFinderActivity.staticSetupHolder = setupToUse;

        // do not keep this activity on the stack, i.e. it can't be saved and recrated
        Intent intent = new Intent(currentActivity, CameraFinderActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        currentActivity.startActivity(intent);
    }

    public CameraFinderActivity() {
        hideHandler = new Handler();
        hidePart2Runnable = () -> {
            // Delayed removal of status and navigation bar
            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            contentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        };
        showPart2Runnable = () -> {
        }; // can be removed in case we do't need to show any controls
        hideRunnable = () -> hide();

        delayHideTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (AUTO_HIDE) {
                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                }
                return false;
            }
        };

        setup = staticSetupHolder; // this is a hack. find out what happens when switching back and forth on the activity stack
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera_finder);

        // get the content view we added
        contentView = ((ViewGroup) this.findViewById(android.R.id.content)).getChildAt(0);

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(view -> toggle());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        visible = true;

        // triggering a hide and a show (even though that may seem superfluous) sets the views size correctly
        // and avoids jerky animation behavior
        hide();
        show();

        setup.run(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggle() {
        if (visible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        visible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        hideHandler.removeCallbacks(showPart2Runnable);
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        contentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        visible = true;

        // Schedule a runnable to display UI elements after a delay
        hideHandler.removeCallbacks(hidePart2Runnable);
        hideHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        hideHandler.removeCallbacks(hideRunnable);
        hideHandler.postDelayed(hideRunnable, delayMillis);
    }


    @Override
    protected void onRestart() {
        super.onRestart();

        setup.onRestart(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        setup.onStart(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setup.onResume(this);
    }

    @Override
    protected void onPause() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setup.onPause(this);

        super.onPause();
    }

    @Override
    protected void onStop() {
        setup.onStop(this);

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        setup.onDestroy(this);

        super.onDestroy();
    }
}
