package com.maikolex.ghostgold;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** The GHOST GOLD seal: a watch bezel with twelve indices around a "GG" monogram, in gold metal. */
public class Emblem extends View {

    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mono = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arc = new RectF();
    private float drawn = 1f;   // 0..1 of the bezel drawn (intro)
    private float glint = -1f;  // 0..1 position of a light glint across the seal (-1 none)

    public Emblem(Context c) {
        super(c);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeCap(Paint.Cap.ROUND);
        tick.setStrokeCap(Paint.Cap.ROUND);
        mono.setTypeface(G.wide(c));
        mono.setTextAlign(Paint.Align.CENTER);
    }

    void setDrawn(float f) { drawn = f; invalidate(); }
    void setGlint(float g) { glint = g; invalidate(); }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        Shader metal = new LinearGradient(0, 0, w, h, new int[]{0xFFFFF1BF, G.GOLD, 0xFF7A5A12},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP);
        ring.setShader(metal);
        mono.setShader(metal);
    }

    @Override
    protected void onDraw(Canvas c) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f, u = Math.min(getWidth(), getHeight()) / 100f;
        ring.setStrokeWidth(Math.max(1.2f, 2.2f * u));
        arc.set(cx - 46 * u, cy - 46 * u, cx + 46 * u, cy + 46 * u);
        c.drawArc(arc, -90, 360 * drawn, false, ring);
        for (int i = 0; i < 12; i++) {
            if (i / 12f > drawn) break;
            boolean major = i % 3 == 0;
            tick.setStrokeWidth(Math.max(1f, (major ? 2f : 1.3f) * u));
            tick.setColor(major ? G.GOLD : 0x99D4AF37);
            double a = Math.toRadians(i * 30);
            float r1 = 41 * u, r2 = (major ? 33 : 37) * u;
            float sx = (float) Math.sin(a), sy = (float) -Math.cos(a);
            c.drawLine(cx + sx * r1, cy + sy * r1, cx + sx * r2, cy + sy * r2, tick);
        }
        mono.setTextSize(22 * u);
        c.drawText("GG", cx, cy + 8 * u, mono);
        if (glint >= 0 && glint <= 1) {
            Paint g = new Paint(Paint.ANTI_ALIAS_FLAG);
            float x = -30 * u + glint * (getWidth() + 60 * u);
            g.setShader(new LinearGradient(x - 10 * u, 0, x + 10 * u, getHeight(),
                    new int[]{0x00FFF1BF, 0x99FFF1BF, 0x00FFF1BF}, null, Shader.TileMode.CLAMP));
            c.drawCircle(cx, cy, 46 * u, g);
        }
    }
}
