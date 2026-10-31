package com.google.ambient.nesthub.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PinchWidgetCard extends FrameLayout {

    public interface OnWidgetEditListener {
        void onWidgetPinch(int slotIndex);
        void onWidgetLongPress(int slotIndex);
    }

    private int slotIndex = -1;
    private OnWidgetEditListener editListener;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private boolean isPinchTriggered = false;

    public PinchWidgetCard(@NonNull Context context) {
        super(context);
        init(context);
    }

    public PinchWidgetCard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PinchWidgetCard(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                if (!isPinchTriggered && (factor < 0.85f || factor > 1.25f)) {
                    isPinchTriggered = true;
                    if (editListener != null && slotIndex >= 0) {
                        editListener.onWidgetPinch(slotIndex);
                    }
                    return true;
                }
                return super.onScale(detector);
            }

            @Override
            public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
                isPinchTriggered = false;
                return true;
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                if (editListener != null && slotIndex >= 0) {
                    editListener.onWidgetLongPress(slotIndex);
                }
            }
        });
    }

    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public void setOnWidgetEditListener(OnWidgetEditListener listener) {
        this.editListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        scaleDetector.onTouchEvent(ev);
        gestureDetector.onTouchEvent(ev);
        if (ev.getPointerCount() > 1) {
            // Multitouch gesture (pinch) - intercept so children don't swallow
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean scaleHandled = scaleDetector.onTouchEvent(event);
        boolean gestureHandled = gestureDetector.onTouchEvent(event);
        return scaleHandled || gestureHandled || super.onTouchEvent(event);
    }
}

