package com.cylan.jiafeigou.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.RadioButton;

/**
 * Created by yzd on 16-12-19.
 */

public class RecordControllerView extends RadioButton {
    private float mCX;
    private float mCY;
    private float mRadius;
    private Paint mBackgroundPaint;
    private float mStartAngle;
    private float mSweepAngle;
    private Paint mNormalPaint;
    private RectF mArcOval;
    private STATE mState = STATE.RESTORE;
    private float mRestoreRadius;
    private RectF mRecordRect;
    private float mRecordRadius;

    public RecordControllerView(Context context) {
        this(context, null);
    }

    public RecordControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public enum STATE {
        RESTORE, RECORD
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mCX = w / 2;
        mCY = h / 2;
        mArcOval = new RectF(0, 0, w, h);
    }

    private void init() {
        mNormalPaint = new TextPaint();
        mNormalPaint.setStyle(Paint.Style.STROKE);
        mNormalPaint.setColor(Color.WHITE);
        mNormalPaint.setStrokeWidth(5);
        mNormalPaint.setAntiAlias(true);
        mStartAngle = -90;
        mSweepAngle = 90;

    }


    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawArc(mArcOval, -90, 90, false, mNormalPaint);
    }

    public void setMaxTime(int time) {

    }

    public void setRecordTime(int time) {

    }

    public void startRecord() {

    }

    public void restoreRecord() {

    }

    private int dp2px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
}
