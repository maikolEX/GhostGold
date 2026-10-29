package com.maikolex.ghostgold;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Aircraft-style FM tuning tape: the band scrolls under a fixed gold needle. Drag to tune;
 * on release it snaps to 0.1 MHz and reports the frequency. Saved stations show as gold pips.
 */
public class Dial extends View {

    interface OnTune { void tune(int freq100); }

    static final int MIN = 8750, MAX = 10800; // MHz * 100
    private float freq = 9750;                // shown frequency (MHz*100, fractional while dragging)
    private final List<Integer> pips = new ArrayList<>();
    private OnTune onTune;

    private final Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG), num = new Paint(Paint.ANTI_ALIAS_FLAG),
            needle = new Paint(Paint.ANTI_ALIAS_FLAG), pip = new Paint(Paint.ANTI_ALIAS_FLAG),
            edges = new Paint();
    private float downX, downFreq;
    private boolean dragging;
    private ValueAnimator settle;

    public Dial(Context c) {
        super(c);
        tick.setStrokeCap(Paint.Cap.ROUND);
        num.setTypeface(G.wide(c));
        num.setTextAlign(Paint.Align.CENTER);
        num.setTextSize(G.dp(c, 12));
        needle.setColor(G.IGNITION);
        needle.setStrokeWidth(G.dp(c, 2));
        needle.setShadowLayer(G.dp(c, 8), 0, 0, 0xB3FFD700);
        setLayerType(LAYER_TYPE_SOFTWARE, null); // for the needle glow
        pip.setColor(G.GOLD);
    }

    void onTune(OnTune t) { onTune = t; }

    void setFrequency(int f) {
        if (dragging) return;
        animateTo(f);
    }

    void setPresets(List<Integer> list) {
        pips.clear();
        pips.addAll(list);
        invalidate();
    }

    /** Pixels per 0.1 MHz. */
    private float step() { return G.dp(getContext(), 14); }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        edges.setShader(new LinearGradient(0, 0, w, 0, new int[]{G.VOID, 0x00050505, 0x00050505, G.VOID},
                new float[]{0, 0.18f, 0.82f, 1}, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        float mid = w / 2f, base = h * 0.62f, s = step();
        int first = (int) Math.floor((freq - mid / s * 10) / 10) * 10;
        for (int f = first; f <= freq + mid / s * 10 + 10; f += 10) {
            if (f < MIN || f > MAX) continue;
            float x = mid + (f - freq) / 10f * s;
            boolean whole = f % 100 == 0, half = f % 50 == 0;
            float len = whole ? h * 0.30f : half ? h * 0.2f : h * 0.12f;
            tick.setStrokeWidth(G.dp(getContext(), whole ? 2 : 1));
            tick.setColor(whole ? G.GOLD : half ? 0xB3D4AF37 : 0x59D4AF37);
            c.drawLine(x, base - len, x, base, tick);
            if (whole) {
                num.setColor(Math.abs(x - mid) < s * 5 ? G.WHITE : G.MUTED);
                c.drawText(String.valueOf(f / 100), x, base + G.dp(getContext(), 22), num);
            }
        }
        for (int p : pips) {
            float x = mid + (p - freq) / 10f * s;
            if (x > -10 && x < w + 10) c.drawCircle(x, base - h * 0.42f, G.dp(getContext(), 3), pip);
        }
        c.drawRect(0, 0, w, h, edges);
        c.drawLine(mid, h * 0.12f, mid, base + G.dp(getContext(), 4), needle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (settle != null) settle.cancel();
                dragging = true;
                downX = e.getX();
                downFreq = freq;
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                freq = clamp(downFreq - (e.getX() - downX) / step() * 10);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                int target = Math.round(freq / 10f) * 10;
                animateTo(target);
                if (onTune != null && Math.abs(target - downFreq) >= 5) onTune.tune(target);
                return true;
        }
        return false;
    }

    private void animateTo(int f) {
        if (settle != null) settle.cancel();
        settle = ValueAnimator.ofFloat(freq, clamp(f)).setDuration(450);
        settle.setInterpolator(new DecelerateInterpolator(2f));
        settle.addUpdateListener(a -> { freq = (float) a.getAnimatedValue(); invalidate(); });
        settle.start();
    }

    private static float clamp(float f) {
        return Math.max(MIN, Math.min(MAX, f));
    }

    static String label(int f) {
        return String.format(Locale.US, "%.1f", f / 100f);
    }
}
