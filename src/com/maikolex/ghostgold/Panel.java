package com.maikolex.ghostgold;

import android.app.AppOpsManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.List;

/**
 * GHOST GOLD panel floating over Android Auto (ZLink). Android Auto cannot be embedded in
 * another app, so this small window sits on top with what Android Auto does not know: speed,
 * restriction, maintenance and alerts. Drag it by its top bar (it snaps to the nearest side),
 * minimise it to a gold seal; an urgent alert opens it again.
 */
public class Panel extends Service {

    private static final String SHOW = "show", HIDE = "hide";

    private final Handler h = new Handler();
    private WindowManager wm;
    private View card, seal;
    private WindowManager.LayoutParams cardLp, sealLp;
    private boolean up, small;
    private int lastLevel;
    private TextView speed, rule, garage, odo, note;
    private String front = "";
    private long since;

    static void show(Context c) {
        if (Settings.canDrawOverlays(c)) c.startService(new Intent(c, Panel.class).setAction(SHOW));
    }

    static void hide(Context c) {
        c.startService(new Intent(c, Panel.class).setAction(HIDE));
    }

    static boolean usageAllowed(Context c) {
        AppOpsManager o = (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
        return o != null && o.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(),
                c.getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        h.post(loop);
    }

    @Override
    public int onStartCommand(Intent i, int f, int id) {
        String a = i == null ? null : i.getAction();
        if (SHOW.equals(a)) open();
        else if (HIDE.equals(a)) close();
        return START_STICKY;
    }

    // ---------- Windows ----------

    private WindowManager.LayoutParams params() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-2, -2,
                Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = prefs().getInt("x", G.dp(this, 40));
        lp.y = prefs().getInt("y", G.dp(this, 230));
        return lp;
    }

    private void open() {
        if (up || !Settings.canDrawOverlays(this)) return;
        if (card == null) build();
        try {
            wm.addView(small ? seal : card, small ? sealLp : cardLp);
            up = true;
            refresh();
        } catch (Exception ignored) { }
    }

    private void close() {
        if (!up) return;
        try { wm.removeView(small ? seal : card); } catch (Exception ignored) { }
        up = false;
    }

    private void minimise(boolean m) {
        if (m == small) return;
        boolean was = up;
        close();
        small = m;
        if (was) open();
    }

    private void build() {
        cardLp = params();
        sealLp = params();

        LinearLayout c = G.column(this);
        c.setBackground(glass(G.dp(this, 18)));
        LinearLayout bar = G.row(this);
        bar.setPadding(G.dp(this, 12), 0, G.dp(this, 2), 0);
        bar.addView(new Emblem(this), new LinearLayout.LayoutParams(G.dp(this, 26), G.dp(this, 26)));
        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("h:mm");
        clock.setFormat24Hour("H:mm");
        clock.setTextSize(10);
        clock.setTypeface(G.wide(this));
        clock.setLetterSpacing(0.2f);
        clock.setTextColor(G.MUTED);
        clock.setGravity(Gravity.CENTER);
        bar.addView(clock, new LinearLayout.LayoutParams(0, -2, 1f));
        ImageView min = new ImageView(this);
        min.setImageDrawable(new Glyph(this, Glyph.MINUS, G.MUTED));
        min.setPadding(G.dp(this, 12), G.dp(this, 12), G.dp(this, 12), G.dp(this, 12));
        min.setOnClickListener(v -> minimise(true));
        bar.addView(min, new LinearLayout.LayoutParams(G.dp(this, 46), G.dp(this, 46)));
        c.addView(bar, new LinearLayout.LayoutParams(G.dp(this, 244), G.dp(this, 46)));
        View hl = new View(this);
        hl.setBackgroundColor(0x33D4AF37);
        c.addView(hl, new LinearLayout.LayoutParams(-1, G.dp(this, 1)));

        LinearLayout body = G.column(this);
        body.setPadding(G.dp(this, 16), G.dp(this, 10), G.dp(this, 16), G.dp(this, 14));
        LinearLayout sp = new LinearLayout(this);
        sp.setGravity(Gravity.BOTTOM);
        speed = G.value(this, "0", 46, G.IGNITION);
        sp.addView(speed);
        TextView u = G.label(this, "km/h");
        u.setPadding(G.dp(this, 6), 0, 0, G.dp(this, 9));
        sp.addView(u);
        body.addView(sp);
        rule = cell(body, "Restricción");
        garage = cell(body, "Mantenim.");
        odo = cell(body, "Odómetro");
        note = G.text(this, "", 13, G.AMBER);
        note.setSingleLine(false);
        note.setMaxLines(2);
        note.setPadding(0, G.dp(this, 8), 0, 0);
        body.addView(note, new LinearLayout.LayoutParams(G.dp(this, 212), -2));
        c.addView(body);
        card = c;
        drag(bar, cardLp, true);

        FrameLayout s = new FrameLayout(this);
        s.setBackground(G.round(0xDB050505, G.GOLD, G.dp(this, 1)));
        s.addView(new Emblem(this), new FrameLayout.LayoutParams(G.dp(this, 44), G.dp(this, 44), Gravity.CENTER));
        FrameLayout holder = new FrameLayout(this);
        holder.addView(s, new FrameLayout.LayoutParams(G.dp(this, 58), G.dp(this, 58)));
        holder.setOnClickListener(v -> minimise(false));
        seal = holder;
        drag(holder, sealLp, false);
    }

    private android.graphics.drawable.GradientDrawable glass(int r) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setCornerRadius(r);
        d.setColor(0xDB050505);
        d.setStroke(G.dp(this, 1), 0x80D4AF37);
        return d;
    }

    private TextView cell(LinearLayout parent, String label) {
        LinearLayout r = G.row(this);
        r.setPadding(0, G.dp(this, 5), 0, G.dp(this, 5));
        TextView l = G.label(this, label);
        l.setTextSize(8);
        r.addView(l, new LinearLayout.LayoutParams(G.dp(this, 96), -2));
        TextView v = G.text(this, "", 15, G.TEXT);
        r.addView(v, new LinearLayout.LayoutParams(G.dp(this, 116), -2));
        parent.addView(r);
        return v;
    }

    /** Drag by the handle; on release snap to the nearer side so the map in the middle stays free. */
    private void drag(View handle, WindowManager.LayoutParams lp, boolean isCard) {
        final float[] s = new float[4];
        final boolean[] moved = {false};
        handle.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    s[0] = e.getRawX(); s[1] = e.getRawY(); s[2] = lp.x; s[3] = lp.y; moved[0] = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = e.getRawX() - s[0], dy = e.getRawY() - s[1];
                    if (Math.abs(dx) + Math.abs(dy) > G.dp(this, 8)) moved[0] = true;
                    lp.x = Math.round(s[2] + dx);
                    lp.y = Math.max(0, Math.round(s[3] + dy));
                    place(isCard ? card : seal, lp);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!moved[0]) { v.performClick(); return true; }
                    int sw = getResources().getDisplayMetrics().widthPixels;
                    boolean left = lp.x + (isCard ? card : seal).getWidth() / 2 < sw / 2;
                    lp.x = left ? G.dp(this, 16) : sw - (isCard ? card : seal).getWidth() - G.dp(this, 16);
                    place(isCard ? card : seal, lp);
                    WindowManager.LayoutParams o = isCard ? sealLp : cardLp;
                    o.y = lp.y;
                    o.x = left ? G.dp(this, 16) : sw - (isCard ? G.dp(this, 58) : G.dp(this, 244)) - G.dp(this, 16);
                    prefs().edit().putInt("x", lp.x).putInt("y", lp.y).apply();
                    return true;
            }
            return false;
        });
    }

    private void place(View v, WindowManager.LayoutParams lp) {
        try { wm.updateViewLayout(v, lp); } catch (Exception ignored) { }
    }

    // ---------- Content ----------

    private final Runnable loop = new Runnable() {
        public void run() {
            follow();
            if (up && !small) refresh();
            if (up) {
                int lv = Alerts.worst(Panel.this);
                if (lv == 2 && lastLevel < 2 && small) minimise(false);
                lastLevel = lv;
            }
            h.postDelayed(this, 1000);
        }
    };

    /** With usage access: open over ZLink, close when anything else comes to the front. */
    private void follow() {
        if (!usageAllowed(this) || !Settings.canDrawOverlays(this)) return;
        UsageStatsManager u = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (u == null) return;
        long now = System.currentTimeMillis();
        UsageEvents ev = u.queryEvents(since == 0 ? now - 10_000 : since, now);
        since = now;
        UsageEvents.Event e = new UsageEvents.Event();
        String fg = null;
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e);
            if (e.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) fg = e.getPackageName();
        }
        if (fg == null || fg.equals(front)) return;
        front = fg;
        if (Head.ZLINK.equals(fg)) open();
        else if (!"android".equals(fg) && !"com.android.systemui".equals(fg)) close();
    }

    private void refresh() {
        if (speed == null) return;
        speed.setText(String.valueOf(Car.speed()));
        boolean blocked = Rules.now(this);
        rule.setText(blocked ? "Hoy no circulas" : "Circulas");
        rule.setTextColor(blocked ? G.AMBER : G.TEXT);
        List<Garage.Job> due = Garage.pending(this);
        garage.setText(due.isEmpty() ? "Al día" : due.get(0).name);
        garage.setTextColor(due.isEmpty() ? G.TEXT : G.AMBER);
        odo.setText(Car.odoSet(this) ? Car.fmt(Car.km(this)) + " km" : "—");
        List<Alerts.Alert> a = Alerts.now(this);
        note.setText(a.isEmpty() ? "" : a.get(0).detail);
        note.setVisibility(a.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private SharedPreferences prefs() { return getSharedPreferences("panel", MODE_PRIVATE); }

    @Override
    public void onDestroy() {
        super.onDestroy();
        h.removeCallbacksAndMessages(null);
        close();
    }

    @Override public IBinder onBind(Intent i) { return null; }
}
