package com.maikolex.ghostgold;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

/**
 * Driving view: the truck from behind on a gold-edged road running to the horizon. Lane dashes
 * rush towards the viewer and the tyres roll at the real speed; the tail lamps glow on braking.
 */
public class Road extends View {

    private final Bitmap rear, tread;
    private float kmh, phase, roll;
    private long frame, brakeUntil;
    private boolean live;

    private final Paint surface = new Paint(Paint.ANTI_ALIAS_FLAG), edge = new Paint(Paint.ANTI_ALIAS_FLAG),
            dash = new Paint(Paint.ANTI_ALIAS_FLAG), haze = new Paint(Paint.ANTI_ALIAS_FLAG),
            under = new Paint(Paint.ANTI_ALIAS_FLAG), shadow = new Paint(Paint.ANTI_ALIAS_FLAG),
            img = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG),
            tyre = new Paint(Paint.ANTI_ALIAS_FLAG), tyreShade = new Paint(Paint.ANTI_ALIAS_FLAG),
            lamp = new Paint(Paint.ANTI_ALIAS_FLAG), post = new Paint(Paint.ANTI_ALIAS_FLAG),
            reflector = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path road = new Path();
    private final RectF car = new RectF(), r = new RectF();
    private final Matrix m = new Matrix(), tm = new Matrix();
    private BitmapShader treadShader;
    private float vx, vy;

    // Inside the rear photo (fractions of width/height).
    private static final float[][] TYRES = {{0.040f, 0.848f, 0.120f, 0.136f}, {0.845f, 0.848f, 0.120f, 0.136f}};
    private static final float[][] LAMPS = {{0.000f, 0.321f, 0.080f, 0.283f}, {0.915f, 0.321f, 0.080f, 0.283f},
            {0.048f, 0.698f, 0.130f, 0.050f}, {0.810f, 0.698f, 0.130f, 0.050f}};

    public Road(Context c) {
        super(c);
        rear = load(c, "car_rear");
        tread = load(c, "tread");
        if (tread != null) treadShader = new BitmapShader(tread, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        edge.setStyle(Paint.Style.STROKE);
        dash.setStyle(Paint.Style.STROKE);
        dash.setStrokeCap(Paint.Cap.ROUND);
        dash.setColor(G.IGNITION);
    }

    private static Bitmap load(Context c, String n) {
        int id = c.getResources().getIdentifier(n, "drawable", c.getPackageName());
        if (id == 0) return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        return BitmapFactory.decodeResource(c.getResources(), id, o);
    }

    boolean ready() { return rear != null; }

    void speed(float v) {
        if (v < kmh - 6) brakeUntil = SystemClock.uptimeMillis() + 1200;
        kmh = v;
    }

    void run(boolean on) {
        live = on;
        frame = 0;
        if (on) postInvalidateOnAnimation();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        vx = w / 2f;
        vy = h * 0.36f;
        road.reset();
        road.moveTo(vx - 4, vy); road.lineTo(vx + 4, vy); road.lineTo(w * 1.06f, h); road.lineTo(-w * 0.06f, h); road.close();
        surface.setShader(new LinearGradient(0, vy, 0, h, new int[]{0x000A0A0A, 0xFF0B0B0B, 0xFF121212},
                new float[]{0, 0.25f, 1}, Shader.TileMode.CLAMP));
        haze.setShader(new RadialGradient(vx, vy, w * 0.36f, 0x73D4AF37, 0, Shader.TileMode.CLAMP));
        if (rear != null) {
            float bw = w * 0.36f, bh = bw * rear.getHeight() / rear.getWidth();
            if (bh > h * 0.62f) { bh = h * 0.62f; bw = bh * rear.getWidth() / rear.getHeight(); }
            car.set(vx - bw / 2, h * 0.95f - bh, vx + bw / 2, h * 0.95f);
            under.setShader(new RadialGradient(vx, car.bottom, bw * 0.62f, 0x52FFD700, 0, Shader.TileMode.CLAMP));
            shadow.setShader(new RadialGradient(vx, car.bottom, bw * 0.56f, new int[]{0xE6000000, 0xE6000000, 0},
                    new float[]{0, 0.4f, 1}, Shader.TileMode.CLAMP));
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        long now = SystemClock.uptimeMillis();
        float dt = frame == 0 ? 0 : Math.min(0.1f, (now - frame) / 1000f);
        frame = now;
        float v = Math.max(kmh, 5);
        phase = (phase + dt * v / 55f) % 1f;
        roll += dt * v * 3.2f * getResources().getDisplayMetrics().density;

        c.save(); c.scale(1, 0.2f, vx, vy); c.drawCircle(vx, vy, w * 0.36f, haze); c.restore();
        edge.setStrokeWidth(G.dp(getContext(), 1)); edge.setColor(0x5AD4AF37);
        c.drawLine(w * 0.15f, vy, w * 0.85f, vy, edge);
        c.drawPath(road, surface);
        edge.setStrokeWidth(G.dp(getContext(), 2)); edge.setColor(0xB3D4AF37);
        c.drawLine(vx - 2, vy, -w * 0.06f, h, edge);
        c.drawLine(vx + 2, vy, w * 1.06f, h, edge);

        for (int side = -1; side <= 1; side += 2) {
            float x1 = vx + side * w * 0.20f, y1 = h * 0.86f, x2 = vx + side * w * 0.23f, y2 = h * 0.94f;
            for (int i = 0; i < 4; i++) {
                float p = (phase + i * 0.25f) % 1f, s = 0.03f + 1.27f * p * p;
                dash.setAlpha(Math.round(255 * Math.min(1f, p / 0.12f)));
                dash.setStrokeWidth(G.dp(getContext(), 5) * s);
                c.drawLine(vx + (x1 - vx) * s, vy + (y1 - vy) * s, vx + (x2 - vx) * s, vy + (y2 - vy) * s, dash);
            }
        }

        // Roadside posts with gold reflectors rushing past.
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 5; i++) {
                float p = (phase * 0.8f + i * 0.2f) % 1f, s = 0.02f + 1.4f * p * p;
                float x = vx + side * w * 0.62f * s, y = vy + (h * 0.98f - vy) * s, tall = G.dp(getContext(), 110) * s;
                int a = Math.round(255 * Math.min(1f, p / 0.15f));
                post.setStrokeWidth(Math.max(1, G.dp(getContext(), 6) * s));
                post.setColor(0x1D1A12 | (a << 24));
                c.drawLine(x, y, x, y - tall, post);
                reflector.setColor(0xFFD700 | (a << 24));
                float rw = G.dp(getContext(), 3) * s;
                c.drawRect(x - rw, y - tall, x + rw, y - tall + G.dp(getContext(), 12) * s, reflector);
            }
        }

        if (rear != null) {
            c.save(); c.scale(1, 0.14f, vx, car.bottom); c.drawCircle(vx, car.bottom, car.width() * 0.62f, under); c.restore();
            c.save(); c.scale(1, 0.12f, vx, car.bottom); c.drawCircle(vx, car.bottom, car.width() * 0.56f, shadow); c.restore();
            m.setRectToRect(new RectF(0, 0, rear.getWidth(), rear.getHeight()), car, Matrix.ScaleToFit.FILL);
            c.drawBitmap(rear, m, img);
            if (treadShader != null) {
                for (float[] t : TYRES) {
                    part(t);
                    float tile = r.height() * 0.66f;
                    tm.setScale(r.width() / tread.getWidth(), tile / tread.getHeight());
                    tm.postTranslate(r.left, r.top + roll % tile);
                    treadShader.setLocalMatrix(tm);
                    tyre.setShader(treadShader);
                    float rad = r.width() * 0.18f;
                    c.drawRoundRect(r, rad, rad, tyre);
                    tyreShade.setShader(new LinearGradient(0, r.top, 0, r.bottom, new int[]{0xBF000000, 0, 0, 0xCC000000},
                            new float[]{0, 0.3f, 0.7f, 1}, Shader.TileMode.CLAMP));
                    c.drawRoundRect(r, rad, rad, tyreShade);
                }
            }
            if (now < brakeUntil) {
                for (float[] t : LAMPS) {
                    part(t);
                    lamp.setShader(new RadialGradient(r.centerX(), r.centerY(), Math.max(r.width(), r.height()) * 0.7f,
                            0xE6FF4628, 0, Shader.TileMode.CLAMP));
                    c.drawOval(r, lamp);
                }
            }
        }
        if (live) postInvalidateOnAnimation();
    }

    private void part(float[] t) {
        r.set(car.left + car.width() * t[0], car.top + car.height() * t[1],
                car.left + car.width() * (t[0] + t[2]), car.top + car.height() * (t[1] + t[3]));
    }
}
