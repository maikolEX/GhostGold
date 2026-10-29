package com.maikolex.ghostgold;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/**
 * Landscape behind the truck, drawn (no photos): three ranges of Central Valley style
 * mountains and volcanoes in black and gold, a low gold sun by day, moon, stars and town lights
 * by night. Parked it stands still; driving, the horizon rises to the road's vanishing point and
 * the ranges drift with the speed (far ones slower).
 */
public class Scenery extends View {

    private boolean night, driving, live;
    private float kmh;
    private float horizon = 0.5f;          // fraction of the height
    private final float[] off = new float[3];
    private long frame;
    private final float[][] stars = new float[90][3];

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), rim = new Paint(Paint.ANTI_ALIAS_FLAG),
            dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    private static final int[] RANGE_FILL = {0xFF17130B, 0xFF0E0C08, 0xFF090806};
    private static final int[] RANGE_RIM = {0x6BD4AF37, 0x2ED4AF37, 0};
    private static final float[] AMP = {96, 50, 20}, LIFT = {34, 10, 2}, SPEED = {0.5f, 1.3f, 3f};

    public Scenery(Context c) {
        super(c);
        Random r = new Random(7);
        for (float[] s : stars) { s[0] = r.nextFloat(); s[1] = r.nextFloat(); s[2] = r.nextFloat(); }
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(G.dp(c, 1));
    }

    void setNight(boolean n) { night = n; invalidate(); }

    void setSpeed(float v) { kmh = v; }

    void setDriving(boolean d) {
        driving = d;
        live = true;
        frame = 0;
        postInvalidateOnAnimation();
    }

    /** Ridge height at x for range i (sines plus two volcano peaks every 1900 px). */
    private float ridge(float x, int i) {
        float a = G.dp(getContext(), AMP[i]) / G.dp(getContext(), 1);
        double k = x + (i + 1) * 1000;
        double y = 0.5 * Math.sin(k * 0.0042 + (i + 1)) + 0.3 * Math.sin(k * 0.011 + (i + 1) * 2.1) + 0.2 * Math.sin(k * 0.027 + (i + 1) * 0.7);
        double m = ((k % 1900) + 1900) % 1900;
        y += 1.3 * Math.exp(-Math.pow((m - 700) / 150, 2)) + 0.8 * Math.exp(-Math.pow((m - 1350) / 110, 2));
        return (float) (a * y) * G.dp(getContext(), 1);
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        long now = SystemClock.uptimeMillis();
        float dt = frame == 0 ? 0 : Math.min(0.1f, (now - frame) / 1000f);
        frame = now;
        float target = driving ? 0.36f : 0.5f;
        horizon += (target - horizon) * Math.min(1f, dt * 2.2f);
        float v = driving ? Math.max(kmh, 5) : 0, Y = h * horizon, d = G.dp(getContext(), 1);
        for (int i = 0; i < 3; i++) off[i] += v * dt * SPEED[i] * d;

        // Sky.
        fill.setShader(new LinearGradient(0, 0, 0, Y, new int[]{G.VOID, night ? 0xFF07070A : 0xFF0B0906, night ? 0xFF100E0A : 0xFF2A2010},
                new float[]{0, 0.55f, 1}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, Y + 2, fill);
        fill.setShader(null);
        if (night) {
            for (float[] s : stars) {
                float tw = 0.35f + 0.35f * (float) Math.sin(now / 900.0 + s[2] * 20);
                dot.setColor(((int) (255 * tw * s[2]) << 24) | 0xEADCB4);
                c.drawRect(s[0] * w, s[1] * Y * 0.85f, s[0] * w + 1.4f * d, s[1] * Y * 0.85f + 1.4f * d, dot);
            }
            dot.setColor(0xD9F0E1B4);
            c.drawCircle(w * 0.78f, Y * 0.32f, 22 * d, dot);
            dot.setColor(0xFF07070A);
            c.drawCircle(w * 0.78f + 9 * d, Y * 0.32f - 5 * d, 20 * d, dot); // crescent
        } else {
            float sx = w * 0.72f, sy = Y - 18 * d;
            fill.setShader(new RadialGradient(sx, sy, 260 * d, 0x59FFC85A, 0, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, Y, fill);
            fill.setShader(new RadialGradient(sx, sy, 40 * d, new int[]{0xFFFFE9A0, 0xFFF2C24E, 0}, new float[]{0, 0.6f, 1}, Shader.TileMode.CLAMP));
            c.drawCircle(sx, sy, 40 * d, fill);
            fill.setShader(null);
        }

        // Mountain ranges, far to near.
        float step = 6 * d;
        for (int i = 0; i < 3; i++) {
            path.reset();
            path.moveTo(0, Y);
            for (float x = 0; x <= w + step; x += step) path.lineTo(x, Y - LIFT[i] * d - AMP[i] * d - ridge(x + off[i], i));
            path.lineTo(w, Y);
            path.close();
            fill.setColor(RANGE_FILL[i]);
            c.drawPath(path, fill);
            if (RANGE_RIM[i] != 0) {
                path.reset();
                for (float x = 0; x <= w + step; x += step) {
                    float y = Y - LIFT[i] * d - AMP[i] * d - ridge(x + off[i], i);
                    if (x == 0) path.moveTo(x, y); else path.lineTo(x, y);
                }
                rim.setColor(RANGE_RIM[i]);
                c.drawPath(path, rim);
            }
        }
        if (night) { // town lights along the near hills
            dot.setColor(0xBFFFD700);
            for (float x = 8 * d; x < w; x += 23 * d) {
                double n = Math.sin((x + off[2]) * 0.37 / d) * Math.sin((x + off[2]) * 0.071 / d);
                if (n > 0.35) c.drawRect(x, Y - 4 * d, x + 1.6f * d, Y - 2.4f * d, dot);
            }
        }

        // Ground and the warm haze on the horizon.
        fill.setShader(new LinearGradient(0, Y, 0, h, new int[]{0xFF0C0A07, 0xFF070605, G.VOID}, new float[]{0, 0.35f, 1}, Shader.TileMode.CLAMP));
        c.drawRect(0, Y, w, h, fill);
        fill.setShader(new LinearGradient(0, Y - 30 * d, 0, Y + 40 * d, new int[]{0, night ? 0x14D4AF37 : 0x29FFC85A, 0},
                null, Shader.TileMode.CLAMP));
        c.drawRect(0, Y - 30 * d, w, Y + 40 * d, fill);
        fill.setShader(null);

        boolean settling = Math.abs(target - horizon) > 0.002f;
        if (driving || settling) postInvalidateOnAnimation();
        else if (night) postInvalidateDelayed(150); // twinkling stars, gently
        else live = false;
    }
}
