package com.google.ambient.nesthub.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Calendar;

public class AnalogClockView extends View {

    private Paint dialPaint;
    private Paint tickPaint;
    private Paint hourHandPaint;
    private Paint minuteHandPaint;
    private Paint secondHandPaint;
    private Paint centerDotPaint;
    private Paint accentPaint;

    private boolean showSeconds = true;
    private boolean isStandByMode = false;
    private int accentColor = 0xFFA8C7FA;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            invalidate();
            handler.postDelayed(this, 1000);
        }
    };

    public AnalogClockView(Context context) {
        super(context);
        init();
    }

    public AnalogClockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnalogClockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        dialPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dialPaint.setStyle(Paint.Style.FILL);
        dialPaint.setColor(0x1AFFFFFF);

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setColor(0x80FFFFFF);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        hourHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hourHandPaint.setColor(Color.WHITE);
        hourHandPaint.setStyle(Paint.Style.STROKE);
        hourHandPaint.setStrokeCap(Paint.Cap.ROUND);

        minuteHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        minuteHandPaint.setColor(0xEEFFFFFF);
        minuteHandPaint.setStyle(Paint.Style.STROKE);
        minuteHandPaint.setStrokeCap(Paint.Cap.ROUND);

        secondHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        secondHandPaint.setColor(0xFFFF5252);
        secondHandPaint.setStyle(Paint.Style.STROKE);
        secondHandPaint.setStrokeCap(Paint.Cap.ROUND);

        centerDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerDotPaint.setStyle(Paint.Style.FILL);
        centerDotPaint.setColor(Color.WHITE);

        accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        accentPaint.setStyle(Paint.Style.STROKE);
        accentPaint.setColor(accentColor);
    }

    public void setStandByMode(boolean standByMode) {
        this.isStandByMode = standByMode;
        invalidate();
    }

    public void setAccentColor(int color) {
        this.accentColor = color;
        this.secondHandPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.post(ticker);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(ticker);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float cx = width / 2f;
        float cy = height / 2f;
        float radius = Math.min(cx, cy) * 0.90f;

        // Dial outline/fill
        if (!isStandByMode) {
            canvas.drawCircle(cx, cy, radius, dialPaint);
        }

        // Draw Hour and Minute ticks
        for (int i = 0; i < 60; i++) {
            float angle = (float) (i * Math.PI / 30);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            if (i % 5 == 0) {
                // Major tick
                tickPaint.setStrokeWidth(radius * 0.025f);
                tickPaint.setColor(i % 15 == 0 ? Color.WHITE : 0xB3FFFFFF);
                float startR = radius * 0.82f;
                float stopR = radius * 0.94f;
                canvas.drawLine(cx + startR * sin, cy - startR * cos,
                        cx + stopR * sin, cy - stopR * cos, tickPaint);
            } else if (isStandByMode) {
                // Minor tick
                tickPaint.setStrokeWidth(radius * 0.012f);
                tickPaint.setColor(0x40FFFFFF);
                float startR = radius * 0.88f;
                float stopR = radius * 0.94f;
                canvas.drawLine(cx + startR * sin, cy - startR * cos,
                        cx + stopR * sin, cy - stopR * cos, tickPaint);
            }
        }

        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR);
        int minute = c.get(Calendar.MINUTE);
        int second = c.get(Calendar.SECOND);

        float minuteFraction = minute / 60.0f;
        float hourAngle = ((hour + minuteFraction) / 12.0f) * 360.0f;
        float minuteAngle = ((minute + second / 60.0f) / 60.0f) * 360.0f;
        float secondAngle = (second / 60.0f) * 360.0f;

        // Hour Hand
        hourHandPaint.setStrokeWidth(radius * 0.06f);
        canvas.save();
        canvas.rotate(hourAngle, cx, cy);
        canvas.drawLine(cx, cy + radius * 0.10f, cx, cy - radius * 0.50f, hourHandPaint);
        canvas.restore();

        // Minute Hand
        minuteHandPaint.setStrokeWidth(radius * 0.04f);
        canvas.save();
        canvas.rotate(minuteAngle, cx, cy);
        canvas.drawLine(cx, cy + radius * 0.12f, cx, cy - radius * 0.72f, minuteHandPaint);
        canvas.restore();

        // Second Hand
        if (showSeconds) {
            secondHandPaint.setStrokeWidth(radius * 0.018f);
            canvas.save();
            canvas.rotate(secondAngle, cx, cy);
            canvas.drawLine(cx, cy + radius * 0.15f, cx, cy - radius * 0.84f, secondHandPaint);
            // Center ring on second hand
            canvas.drawCircle(cx, cy, radius * 0.04f, secondHandPaint);
            canvas.restore();
        }

        // Center dot
        centerDotPaint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, radius * 0.025f, centerDotPaint);
    }
}

