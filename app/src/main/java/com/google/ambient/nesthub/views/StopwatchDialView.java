package com.google.ambient.nesthub.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Locale;

public class StopwatchDialView extends View {

    private Paint casePaint;
    private Paint caseBezelPaint;
    private Paint crownPaint;
    private Paint dialBackgroundPaint;
    private Paint tickMajorPaint;
    private Paint tickMinorPaint;
    private Paint tickTextPaint;
    private Paint handPaint;
    private Paint subHandPaint;
    private Paint centerPinPaint;
    private Paint digitalBoxPaint;
    private Paint digitalTextPaint;

    private long elapsedTimeMs = 0;
    private boolean isRunning = false;

    public StopwatchDialView(Context context) {
        super(context);
        init();
    }

    public StopwatchDialView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StopwatchDialView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        casePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        casePaint.setColor(0xFF2C303B);
        casePaint.setStyle(Paint.Style.FILL);

        caseBezelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        caseBezelPaint.setColor(0xFF454B5C);
        caseBezelPaint.setStyle(Paint.Style.STROKE);

        crownPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crownPaint.setColor(0xFF5A6278);
        crownPaint.setStyle(Paint.Style.FILL);

        dialBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dialBackgroundPaint.setColor(0xFF14161C);
        dialBackgroundPaint.setStyle(Paint.Style.FILL);

        tickMajorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickMajorPaint.setColor(0xFFE0E0E6);
        tickMajorPaint.setStrokeCap(Paint.Cap.ROUND);

        tickMinorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickMinorPaint.setColor(0x60FFFFFF);
        tickMinorPaint.setStrokeCap(Paint.Cap.ROUND);

        tickTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickTextPaint.setColor(0xFF9E9EA8);
        tickTextPaint.setTextAlign(Paint.Align.CENTER);
        tickTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

        handPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handPaint.setColor(0xFFFF5252); // Chronograph red needle
        handPaint.setStrokeCap(Paint.Cap.ROUND);
        handPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        subHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subHandPaint.setColor(0xFFA8C7FA); // Subdial accent
        subHandPaint.setStrokeCap(Paint.Cap.ROUND);

        centerPinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPinPaint.setColor(Color.WHITE);
        centerPinPaint.setStyle(Paint.Style.FILL);

        digitalBoxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        digitalBoxPaint.setColor(0x2E000000);
        digitalBoxPaint.setStyle(Paint.Style.FILL);

        digitalTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        digitalTextPaint.setColor(Color.WHITE);
        digitalTextPaint.setTextAlign(Paint.Align.CENTER);
        digitalTextPaint.setTypeface(Typeface.MONOSPACE);
    }

    public void setElapsedTime(long millis, boolean running) {
        this.elapsedTimeMs = millis;
        this.isRunning = running;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float cx = width / 2f;
        // Shift center down slightly to accommodate top crown pusher
        float cy = height * 0.54f;
        float radius = Math.min(width, height) * 0.40f;

        // 1. Draw Mechanical Stopwatch Top Crown Pusher at 12 o'clock
        float crownW = radius * 0.28f;
        float crownH = radius * 0.22f;
        float crownTop = cy - radius - crownH * 1.15f;
        RectF crownRect = new RectF(cx - crownW / 2, crownTop, cx + crownW / 2, cy - radius);
        canvas.drawRoundRect(crownRect, 6, 6, crownPaint);

        // Neck connecting crown to casing
        float neckW = crownW * 0.50f;
        RectF neckRect = new RectF(cx - neckW / 2, cy - radius - crownH * 0.5f, cx + neckW / 2, cy - radius);
        canvas.drawRect(neckRect, crownPaint);

        // 2. Stopwatch Outer Body Casing
        canvas.drawCircle(cx, cy, radius * 1.08f, casePaint);
        caseBezelPaint.setStrokeWidth(radius * 0.04f);
        canvas.drawCircle(cx, cy, radius * 1.08f, caseBezelPaint);

        // 3. Dial Face
        canvas.drawCircle(cx, cy, radius, dialBackgroundPaint);

        // 4. Dial Ticks (60 seconds)
        tickTextPaint.setTextSize(radius * 0.12f);
        for (int sec = 0; sec < 60; sec++) {
            float angle = (float) (sec * (Math.PI / 30.0));
            float sin = (float) Math.sin(angle);
            float cos = (float) Math.cos(angle);

            if (sec % 5 == 0) {
                // Major 5-second tick
                tickMajorPaint.setStrokeWidth(radius * 0.035f);
                float startR = radius * 0.82f;
                float stopR = radius * 0.94f;
                canvas.drawLine(cx + startR * sin, cy - startR * cos,
                                cx + stopR * sin, cy - stopR * cos, tickMajorPaint);

                // Numeral label (05, 10, 15, ..., 60)
                float numR = radius * 0.70f;
                String numStr = String.format(Locale.getDefault(), "%02d", (sec == 0 ? 60 : sec));
                canvas.drawText(numStr, cx + numR * sin, cy - numR * cos + radius * 0.04f, tickTextPaint);
            } else {
                // Minor 1-second tick
                tickMinorPaint.setStrokeWidth(radius * 0.015f);
                float startR = radius * 0.88f;
                float stopR = radius * 0.94f;
                canvas.drawLine(cx + startR * sin, cy - startR * cos,
                                cx + stopR * sin, cy - stopR * cos, tickMinorPaint);
            }
        }

        // 5. 30-Minute Subdial at top (between center and 12 o'clock)
        float subCy = cy - radius * 0.35f;
        float subR = radius * 0.28f;
        canvas.drawCircle(cx, subCy, subR, casePaint);
        // Subdial ticks
        for (int m = 0; m < 30; m += 5) {
            float angle = (float) (m * (Math.PI / 15.0));
            float sin = (float) Math.sin(angle);
            float cos = (float) Math.cos(angle);
            canvas.drawLine(cx + subR * 0.7f * sin, subCy - subR * 0.7f * cos,
                            cx + subR * 0.9f * sin, subCy - subR * 0.9f * cos, tickMajorPaint);
        }
        // Subdial needle (minutes fraction)
        long totalMins = elapsedTimeMs / (60 * 1000);
        float subAngle = (totalMins % 30) * 12.0f;
        canvas.save();
        canvas.rotate(subAngle, cx, subCy);
        subHandPaint.setStrokeWidth(radius * 0.025f);
        canvas.drawLine(cx, subCy, cx, subCy - subR * 0.75f, subHandPaint);
        canvas.restore();
        canvas.drawCircle(cx, subCy, radius * 0.03f, centerPinPaint);

        // 6. Center Digital LCD Box Readout
        float boxW = radius * 1.05f;
        float boxH = radius * 0.32f;
        float boxTop = cy + radius * 0.22f;
        RectF digBox = new RectF(cx - boxW / 2, boxTop, cx + boxW / 2, boxTop + boxH);
        canvas.drawRoundRect(digBox, 10, 10, digitalBoxPaint);

        int hundredths = (int) ((elapsedTimeMs % 1000) / 10);
        int seconds = (int) ((elapsedTimeMs / 1000) % 60);
        int minutes = (int) ((elapsedTimeMs / (1000 * 60)) % 60);
        String digitalTime = String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths);
        digitalTextPaint.setTextSize(radius * 0.20f);
        canvas.drawText(digitalTime, cx, boxTop + boxH * 0.72f, digitalTextPaint);

        // 7. Sweeping Central Chronograph Second Hand
        float secFraction = (elapsedTimeMs % 60000) / 1000.0f;
        float handAngle = secFraction * 6.0f; // 360 degrees / 60 seconds

        canvas.save();
        canvas.rotate(handAngle, cx, cy);

        // Needle blade
        handPaint.setStrokeWidth(radius * 0.025f);
        canvas.drawLine(cx, cy + radius * 0.18f, cx, cy - radius * 0.88f, handPaint);

        // Counterbalance teardrop
        Path tail = new Path();
        tail.moveTo(cx - radius * 0.04f, cy);
        tail.lineTo(cx + radius * 0.04f, cy);
        tail.lineTo(cx, cy + radius * 0.20f);
        tail.close();
        canvas.drawPath(tail, handPaint);
        canvas.restore();

        // 8. Center Pin Cap
        centerPinPaint.setColor(0xFFFF5252);
        canvas.drawCircle(cx, cy, radius * 0.05f, centerPinPaint);
        centerPinPaint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, radius * 0.025f, centerPinPaint);
    }
}

