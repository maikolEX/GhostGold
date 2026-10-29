package com.maikolex.ghostgold;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** GHOST GOLD design system: palette, type, and the few building blocks every screen uses. */
final class G {

    private G() { }

    // ---------- Palette ----------
    static final int VOID = 0xFF050505, ONYX = 0xFF0A0A0A, GRAPHITE = 0xFF111111;
    static final int GOLD = 0xFFD4AF37, IGNITION = 0xFFFFD700, CHAMPAGNE = 0xFFC8A951, BRONZE = 0xFF8A6614;
    static final int WHITE = 0xFFFFFFFF, TEXT = 0xFFEAEAEA, MUTED = 0xFFB5B5B5, DIM = 0xFF5A5A5A, LINE = 0xFF1C1C1C;
    static final int HAIR = 0x59D4AF37, TINT = 0x1FD4AF37, AMBER = 0xFFE8A33D;

    // ---------- Type ----------
    private static Typeface wide, light, book;

    /** Michroma: wide technical face for labels and numerals. */
    static Typeface wide(Context c) {
        if (wide == null) wide = font(c, "Michroma-Regular", Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return wide;
    }

    /** Barlow Light: large values. */
    static Typeface light(Context c) {
        if (light == null) light = font(c, "Barlow-Light", Typeface.create("sans-serif-light", Typeface.NORMAL));
        return light;
    }

    /** Barlow Regular: text. */
    static Typeface book(Context c) {
        if (book == null) book = font(c, "Barlow-Regular", Typeface.DEFAULT);
        return book;
    }

    private static Typeface font(Context c, String name, Typeface fallback) {
        try {
            return Typeface.createFromAsset(c.getAssets(), "fonts/" + name + ".ttf");
        } catch (Exception e) {
            return fallback;
        }
    }

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    private static TextView base(Context c, CharSequence s, float sp, int color, Typeface tf) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(tf);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setIncludeFontPadding(false);
        return t;
    }

    /** "VELOCIDAD": small, uppercase, widely tracked. */
    static TextView label(Context c, CharSequence s) {
        TextView t = base(c, s.toString().toUpperCase(), 10, MUTED, wide(c));
        t.setLetterSpacing(0.22f);
        return t;
    }

    static TextView value(Context c, CharSequence s, float sp, int color) {
        return base(c, s, sp, color, light(c));
    }

    static TextView text(Context c, CharSequence s, float sp, int color) {
        return base(c, s, sp, color, book(c));
    }

    // ---------- Shapes ----------

    static GradientDrawable pill(Context c, int fill, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(c, 24));
        d.setColor(fill);
        if (stroke != 0) d.setStroke(dp(c, 1), stroke);
        return d;
    }

    static GradientDrawable round(int fill, int stroke, int strokePx) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(fill);
        if (stroke != 0) d.setStroke(strokePx, stroke);
        return d;
    }

    static GradientDrawable panel(Context c) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{GRAPHITE, ONYX});
        d.setCornerRadius(dp(c, 20));
        d.setStroke(dp(c, 1), 0x33D4AF37);
        return d;
    }

    /** Polished gold disc (primary action). */
    static GradientDrawable goldDisc() {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFFFFE9A0, IGNITION, GOLD, BRONZE});
        d.setShape(GradientDrawable.OVAL);
        return d;
    }

    // ---------- Controls ----------

    /** Round icon button: gold glyph on a tinted ring, or black glyph on a gold disc when primary. */
    static ImageView button(Context c, int glyph, int sizeDp, boolean primary, Runnable action) {
        ImageView b = new ImageView(c);
        b.setImageDrawable(new Glyph(c, glyph, primary ? VOID : GOLD));
        int pad = Math.round(sizeDp * 0.27f);
        b.setPadding(dp(c, pad), dp(c, pad), dp(c, pad), dp(c, pad));
        b.setBackground(primary ? goldDisc() : round(TINT, HAIR, dp(c, 1)));
        press(b);
        b.setOnClickListener(v -> action.run());
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return b;
    }

    /** Text action in a gold-outlined pill. */
    static TextView chip(Context c, CharSequence s, Runnable action) {
        TextView t = label(c, s);
        t.setTextColor(GOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 18), dp(c, 13), dp(c, 18), dp(c, 13));
        t.setBackground(pill(c, 0, HAIR));
        press(t);
        t.setOnClickListener(v -> action.run());
        return t;
    }

    /** Subtle scale on touch for anything tappable. */
    static void press(View v) {
        v.setClickable(true);
        v.setOnTouchListener((view, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) view.animate().scaleX(0.93f).scaleY(0.93f).setDuration(90).start();
            else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL)
                view.animate().scaleX(1f).scaleY(1f).setDuration(160).start();
            return false;
        });
    }

    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static View hairline(Context c) {
        View v = new View(c);
        v.setBackgroundColor(HAIR);
        return v;
    }

    /** Standard screen header: back button, small gold kicker and a light title. */
    static LinearLayout header(android.app.Activity a, String kicker, String title) {
        LinearLayout h = row(a);
        h.addView(button(a, Glyph.BACK, 48, false, a::finish));
        LinearLayout t = column(a);
        t.setPadding(dp(a, 20), 0, 0, 0);
        TextView k = label(a, kicker);
        k.setTextColor(GOLD);
        t.addView(k);
        TextView ti = value(a, title, 32, WHITE);
        ti.setPadding(0, dp(a, 6), 0, 0);
        t.addView(ti);
        h.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        return h;
    }
}
