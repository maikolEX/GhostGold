package com.maikolex.ghostgold;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * The truck on its gold-lit pedestal: halo, floor glow with orbit rings, mirror reflection and
 * the headlights photo. In showroom the pedestal turns (front, rear, mirrored front, rear),
 * the halo breathes and a light sweeps across the paint.
 */
public class Stage extends View {

    private final Bitmap front, lights, rear;
    private final Bitmap[] turn8 = new Bitmap[8]; // 360° views every 45°, starting from the front
    private Bitmap mirrored;
    private float lightsOn;     // 0..1 cross-fade to the headlights photo
    private float dim = 1f;     // halo strength (lower at night)
    private float breathe;      // 0..1
    private float angle = -1f;  // turntable 0..360, -1 = still
    private float sweep = -1f;  // light sweep 0..1
    private ValueAnimator turn;

    private final Paint img = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbit = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fade = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF(), fit = new RectF(), oval = new RectF();
    private final Matrix m = new Matrix();

    public Stage(Context c) {
        super(c);
        front = load(c, "car");
        lights = load(c, "car_lights");
        rear = load(c, "car_rear");
        for (int i = 0; i < 8; i++) turn8[i] = load(c, "spin_" + i);
        orbit.setStyle(Paint.Style.STROKE);
        orbit.setStrokeWidth(G.dp(c, 1));
        fade.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        shine.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
    }

    private static Bitmap load(Context c, String name) {
        int id = c.getResources().getIdentifier(name, "drawable", c.getPackageName());
        if (id == 0) return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        return BitmapFactory.decodeResource(c.getResources(), id, o);
    }

    // ---------- Controls ----------

    void setHeadlights(float f) { lightsOn = lights == null ? 0 : f; invalidate(); }
    float headlights() { return lightsOn; }
    void setNight(boolean night) { dim = night ? 0.6f : 1f; invalidate(); }

    void showroom(boolean on) {
        if (turn != null) turn.cancel();
        if (!on) { angle = -1f; sweep = -1f; breathe = 0; invalidate(); return; }
        // One clock drives everything: a full turn every 16 s; sweep and breathing ride on it.
        turn = ValueAnimator.ofFloat(0f, 1f).setDuration(24000); // 3 s per view
        turn.setRepeatCount(ValueAnimator.INFINITE);
        turn.setInterpolator(new LinearInterpolator());
        turn.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            // Start from view 1 (the photo on the home screen) so the turn begins without a jump.
            angle = full() ? (45f + t * 360f) % 360f : rear != null ? t * 360f : -1f;
            float s = (t * 4f) % 1f;            // four sweeps per turn, one per view
            sweep = s < 0.55f ? s / 0.55f : -1f;
            breathe = (float) (0.5 - 0.5 * Math.cos(t * Math.PI * 6));
            invalidate();
        });
        turn.start();
    }

    RectF truck() { return box; }

    private boolean full() {
        for (Bitmap b : turn8) if (b == null) return false;
        return true;
    }

    // ---------- Layout ----------

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        if (front == null) return;
        float ar = front.getWidth() / (float) front.getHeight();
        float bw = w * 0.56f, bh = h * 0.64f;
        if (bw / ar > bh) bw = bh * ar;
        float hh = bw / ar;
        box.set((w - bw) / 2f, h * 0.5f - hh / 2f, (w + bw) / 2f, h * 0.5f + hh / 2f);
    }

    // ---------- Drawing ----------

    @Override
    protected void onDraw(Canvas c) {
        if (front == null || box.isEmpty()) return;
        float cx = box.centerX(), cy = box.centerY(), k = dim * (1f + 0.35f * breathe);

        float r = box.width() * 0.62f;
        glow.setShader(new RadialGradient(cx, cy, r, new int[]{gold(0x3D, k), gold(0x18, k), 0},
                new float[]{0, 0.55f, 1}, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, glow);

        float fy = box.bottom - box.height() * 0.06f, fw = box.width() * 0.62f, fh = box.height() * 0.11f;
        c.save();
        c.scale(1f, fh / fw, cx, fy);
        glow.setShader(new RadialGradient(cx, fy, fw, new int[]{gold(0x55, k), gold(0x14, k), 0},
                new float[]{0, 0.6f, 1}, Shader.TileMode.CLAMP));
        c.drawCircle(cx, fy, fw, glow);
        c.restore();
        float rx = fw * 0.92f, ry = fh * 0.55f;
        orbit.setColor(gold(0x66, k));
        oval.set(cx - rx, fy - ry, cx + rx, fy + ry);
        c.drawOval(oval, orbit);
        orbit.setColor(gold(0x2A, k));
        oval.inset(-fw * 0.12f, -fh * 0.12f);
        c.drawOval(oval, orbit);
        if (angle >= 0) {
            for (int i = 0; i < 3; i++) {
                double a = Math.toRadians(angle + i * 120);
                float near = (float) Math.sin(a);
                dot.setColor(gold(near > 0 ? 0xE6 : 0x55, k));
                c.drawCircle(cx + (float) Math.cos(a) * rx, fy + near * ry, G.dp(getContext(), near > 0 ? 3 : 2), dot);
            }
        }

        // Reflection.
        float refl = box.height() * 0.32f;
        int layer = c.saveLayer(0, box.bottom, getWidth(), box.bottom + refl, null);
        c.save();
        c.scale(1f, -1f, cx, box.bottom);
        img.setAlpha(70);
        truck(c);
        c.restore();
        fade.setShader(new LinearGradient(0, box.bottom, 0, box.bottom + refl, 0xFF000000, 0, Shader.TileMode.CLAMP));
        c.drawRect(0, box.bottom, getWidth(), box.bottom + refl, fade);
        c.restoreToCount(layer);

        // The truck, with the showroom light sweep clipped to its paint.
        img.setAlpha(255);
        if (sweep >= 0) {
            int l2 = c.saveLayer(0, box.top, getWidth(), box.bottom, null);
            truck(c);
            float x = box.left - box.width() * 0.2f + box.width() * 1.4f * sweep, band = box.width() * 0.18f;
            shine.setShader(new LinearGradient(x - band, box.top, x + band, box.bottom,
                    new int[]{0x00FFF1BF, 0x66FFF1BF, 0x00FFF1BF}, null, Shader.TileMode.CLAMP));
            c.drawRect(0, box.top, getWidth(), box.bottom, shine);
            c.restoreToCount(l2);
        } else {
            truck(c);
        }
    }

    /** Draws the current view of the truck into the box. */
    private void truck(Canvas c) {
        if (angle >= 0 && full()) { turn360(c); return; }
        Bitmap b = front;
        float sx = 1f;
        if (angle >= 0) {
            if (mirrored == null) {
                Matrix f = new Matrix();
                f.preScale(-1, 1);
                mirrored = Bitmap.createBitmap(front, 0, 0, front.getWidth(), front.getHeight(), f, true);
            }
            Bitmap[] views = {front, rear, mirrored, rear};
            int seg = (int) (angle / 90f) % 4;
            float t = (angle % 90f) / 90f;
            if (t < 0.78f) { b = views[seg]; sx = 1f - 0.05f * t / 0.78f; }
            else if (t < 0.89f) { b = views[seg]; sx = 0.95f * (float) Math.cos((t - 0.78f) / 0.11f * Math.PI / 2); }
            else { b = views[(seg + 1) % 4]; sx = (float) Math.sin((t - 0.89f) / 0.11f * Math.PI / 2); }
            sx = Math.max(0.02f, sx);
        }
        float ar = b.getWidth() / (float) b.getHeight();
        float hh = b == rear ? box.height() * 0.88f : box.height(), ww = hh * ar;
        if (ww > box.width()) { ww = box.width(); hh = ww / ar; }
        fit.set(box.centerX() - ww / 2, box.bottom - hh, box.centerX() + ww / 2, box.bottom);
        int base = img.getAlpha();
        int a = Math.round(base * (0.55f + 0.45f * sx));
        if (b == front && lightsOn > 0 && angle < 0) {
            img.setAlpha(Math.round(a * (1 - lightsOn)));
            blit(c, front, sx);
            img.setAlpha(Math.round(a * lightsOn));
            blit(c, lights, sx);
        } else {
            img.setAlpha(a);
            blit(c, b, sx);
        }
        img.setAlpha(base);
    }

    /** Eight real views: each holds, then cross-fades into the next while the body narrows slightly. */
    private void turn360(Canvas c) {
        int seg = (int) (angle / 45f) % 8;
        float t = (angle % 45f) / 45f;
        int base = img.getAlpha();
        if (t < 0.75f) {
            view(c, turn8[seg], 1f, base);
        } else {
            float u = (t - 0.75f) / 0.25f, e = u * u * (3 - 2 * u);   // smoothstep
            float sx = 1f - 0.05f * (float) Math.sin(u * Math.PI);
            view(c, turn8[seg], sx, Math.round(base * (1 - e)));
            view(c, turn8[(seg + 1) % 8], sx, Math.round(base * e));
        }
        img.setAlpha(base);
    }

    /** One view standing on the floor line, as tall as the box unless it would be too wide. */
    private void view(Canvas c, Bitmap b, float sx, int alpha) {
        float ar = b.getWidth() / (float) b.getHeight();
        float hh = box.height(), ww = hh * ar, maxW = getWidth() * 0.72f;
        if (ww > maxW) { ww = maxW; hh = ww / ar; }
        fit.set(box.centerX() - ww / 2, box.bottom - hh, box.centerX() + ww / 2, box.bottom);
        img.setAlpha(alpha);
        blit(c, b, sx);
    }

    private void blit(Canvas c, Bitmap b, float sx) {
        m.setRectToRect(new RectF(0, 0, b.getWidth(), b.getHeight()), fit, Matrix.ScaleToFit.FILL);
        m.postScale(sx, 1f, box.centerX(), 0);
        c.drawBitmap(b, m, img);
    }

    private static int gold(int a, float k) {
        return (Math.max(0, Math.min(255, Math.round(a * k))) << 24) | 0xD4AF37;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (turn != null) turn.cancel();
    }
}
