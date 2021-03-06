package com.cylan.jiafeigou.widget.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;

import com.cylan.panorama.CameraParam;

import org.webrtc.videoengine.ViEAndroidGLES20;

/**
 * Created by cylan-hunt on 16-11-30.
 */

public class ViEAndroidGLES20_Ext extends ViEAndroidGLES20 implements VideoViewFactory.IVideoView {
    private VideoViewFactory.InterActListener interActListener;
    private float mScaleFactor = 1.0f;

    public ViEAndroidGLES20_Ext(Context context) {
        super(context);
    }

    public ViEAndroidGLES20_Ext(Context context, boolean translucent, int depth, int stencil) {
        super(context, translucent, depth, stencil);
    }

    @Override
    public void config360(CameraParam cameraParam) {

    }

    @Override
    public void setMode(int mode) {

    }

//    @Override
//    public void onDrawFrame(GL10 gl) {
//        super.onDrawFrame(gl);
////        gl.glScalef(mScaleFactor, mScaleFactor, 1);
//    }

    public void setScaleFactor(float scaleFactor) {
        mScaleFactor = scaleFactor;
    }

    @Override
    public void setInterActListener(VideoViewFactory.InterActListener interActListener) {
        this.interActListener = interActListener;
        setEventListener(new EventListener() {
            @Override
            public boolean onSingleTap(MotionEvent motionEvent) {
                return interActListener != null && interActListener.onSingleTap(motionEvent.getX(), motionEvent.getY());
            }

            @Override
            public void onSnapshot(Bitmap bitmap, boolean b) {
                if (interActListener != null) interActListener.onSnapshot(bitmap, b);
            }
        });
    }

    @Override
    public void config720() {

    }

    @Override
    public boolean isPanoramicView() {
        return false;
    }

    @Override
    public void onDestroy() {
        this.interActListener = null;
    }

    @Override
    public void loadBitmap(Bitmap bitmap) {

    }

    @Override
    public void takeSnapshot(boolean tag) {

    }

    @Override
    public void performTouch() {
        // Obtain MotionEvent object
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 100;
        float x = 0.0f;
        float y = 0.0f;
        // List of meta states found here: developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
        int metaState = 0;
        MotionEvent motionEvent = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                x,
                y,
                metaState
        );
        // Dispatch touch event to view
        dispatchTouchEvent(motionEvent);
    }

    @Override
    public void detectOrientationChanged() {

    }

    @Override
    public Bitmap getCacheBitmap() {
        setDrawingCacheEnabled(true);
        // this is the important code :)
        // Without it the view will have a dimension of 0,0 and the bitmap will be null
        measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
        buildDrawingCache(true);
        Bitmap source = getDrawingCache();
        Log.d("getCacheBitmap", "getCacheBitmap result?" + (source == null));
        if (source == null) return null;
        Bitmap b = Bitmap.createBitmap(source);
        setDrawingCacheEnabled(false); // clear drawing cache
        return b;
    }
}
