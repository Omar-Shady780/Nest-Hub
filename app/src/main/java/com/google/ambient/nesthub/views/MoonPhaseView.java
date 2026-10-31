package com.google.ambient.nesthub.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Calendar;

public class MoonPhaseView extends View {

    private Paint darkPaint;
    private Paint litPaint;
    private Paint glowPaint;
    private double moonPhase = 0.5; // 0.0 = New Moon, 0.5 = Full Moon, 1.0 = New Moon
    private String phaseName = "Full Moon";

    public MoonPhaseView(Context context) {
        super(context);
        init();
    }

    public MoonPhaseView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MoonPhaseView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        darkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        darkPaint.setColor(0xFF1E222D);
        darkPaint.setStyle(Paint.Style.FILL);

        litPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        litPaint.setColor(0xFFFFF7D6);
        litPaint.setStyle(Paint.Style.FILL);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(0x22FFF7D6);
        glowPaint.setStyle(Paint.Style.FILL);

        calculateMoonPhase();
    }

    public void calculateMoonPhase() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        if (month < 3) {
            year--;
            month += 12;
        }
        int a = year / 100;
        int b = a / 4;
        int c = 2 - a + b;
        double e = 365.25 * (year + 4716);
        double f = 30.6001 * (month + 1);
        double jd = c + day + e + f - 1524.5;
        double daysSinceNew = (jd - 2451549.5) / 29.53058867;
        moonPhase = daysSinceNew - Math.floor(daysSinceNew);

        if (moonPhase < 0.06 || moonPhase > 0.94) {
            phaseName = "New Moon";
        } else if (moonPhase < 0.22) {
            phaseName = "Waxing Crescent";
        } else if (moonPhase < 0.28) {
            phaseName = "First Quarter";
        } else if (moonPhase < 0.44) {
            phaseName = "Waxing Gibbous";
        } else if (moonPhase < 0.56) {
            phaseName = "Full Moon";
        } else if (moonPhase < 0.72) {
            phaseName = "Waning Gibbous";
        } else if (moonPhase < 0.78) {
            phaseName = "Last Quarter";
        } else {
            phaseName = "Waning Crescent";
        }
        invalidate();
    }

    public String getPhaseName() {
        return phaseName;
    }

    public double getPhase() {
        return moonPhase;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) * 0.85f;

        // Subtle outer glow
        canvas.drawCircle(cx, cy, radius * 1.08f, glowPaint);

        // Draw base moon disk
        canvas.drawCircle(cx, cy, radius, darkPaint);

        // Draw lit portion
        float phaseAngle = (float) (moonPhase * 2 * Math.PI);
        RectF oval = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);

        Path path = new Path();
        if (moonPhase <= 0.5) {
            // Waxing: right side lit
            path.arcTo(oval, -90, 180, true);
            float innerW = (float) Math.abs(Math.cos(phaseAngle)) * radius;
            RectF innerOval = new RectF(cx - innerW, cy - radius, cx + innerW, cy + radius);
            if (moonPhase < 0.25) {
                path.arcTo(innerOval, 90, -180, false);
            } else {
                path.arcTo(innerOval, 90, 180, false);
            }
        } else {
            // Waning: left side lit
            path.arcTo(oval, 90, 180, true);
            float innerW = (float) Math.abs(Math.cos(phaseAngle)) * radius;
            RectF innerOval = new RectF(cx - innerW, cy - radius, cx + innerW, cy + radius);
            if (moonPhase < 0.75) {
                path.arcTo(innerOval, -90, 180, false);
            } else {
                path.arcTo(innerOval, -90, -180, false);
            }
        }
        path.close();
        canvas.drawPath(path, litPaint);
    }
}

