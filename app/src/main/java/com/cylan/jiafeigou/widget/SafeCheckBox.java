package com.cylan.jiafeigou.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;

/**
 * Created by hds on 17-3-28.
 * setCheck的时候也会触发callback.就会产生无线循环地回调
 */

public class SafeCheckBox extends AppCompatCheckBox {

    private OnCheckedChangeListener mListener;

    public SafeCheckBox(Context context) {
        super(context);
    }

    public SafeCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SafeCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setOnCheckedChangeListener(final OnCheckedChangeListener listener) {
        mListener = listener;
        super.setOnCheckedChangeListener(listener);
    }

    public void setChecked(final boolean checked, final boolean alsoNotify) {
        if (!alsoNotify) {
            super.setOnCheckedChangeListener(null);
            super.setChecked(checked);
            super.setOnCheckedChangeListener(mListener);
            return;
        }
        super.setChecked(checked);
    }

    public void toggle(boolean alsoNotify) {
        if (!alsoNotify) {
            super.setOnCheckedChangeListener(null);
            super.toggle();
            super.setOnCheckedChangeListener(mListener);
        }
        super.toggle();
    }
}
