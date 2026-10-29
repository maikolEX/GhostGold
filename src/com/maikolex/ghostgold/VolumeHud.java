package com.maikolex.ghostgold;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

/**
 * Volume as a gold bezel: when the steering-wheel (or panel) volume changes, a ring fills to
 * the level with the number in the middle, then fades. Reads the MCU volume (0..max).
 */
public class VolumeHud extends Service {

    private final Handler h = new Handler();
    private WindowManager wm;
    private Ring ring;
    private boolean up;
    private int last = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        ring = new Ring(this);
        last = Head.sys(this, Head.K_VOLUME, -1);
        getContentResolver().registerContentObserver(Settings.System.getUriFor(Head.K_VOLUME), false, watch);
        IntentFilter f = new IntentFilter(Head.VOLUME);
        f.addAction(Head.OS_VOLUME);
        registerReceiver(events, f);
    }

    @Override public int onStartCommand(Intent i, int f, int id) { return START_STICKY; }

    private final ContentObserver watch = new ContentObserver(h) {
        @Override public void onChange(boolean self) { check(false); }
    };

    private final BroadcastReceiver events = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            // The MCU writes the value just after the broadcast. A volume-key press always shows
            // the ring (even at min/max); other notices only when the value really changed.
            boolean key = Head.VOLUME.equals(i.getAction());
            h.postDelayed(() -> check(key), 60);
            h.postDelayed(() -> check(false), 250);
        }
    };

    private void check(boolean force) {
        int v = Head.sys(this, Head.K_VOLUME, -1);
        if (v < 0 || (v == last && !force)) return;
        last = v;
        show(v, Math.max(1, Head.sys(this, Head.K_MAX_VOLUME, 40)));
    }

    private void show(int v, int max) {
        if (!Settings.canDrawOverlays(this)) return;
        ring.set(v, max);
        if (!up) {
            int s = G.dp(this, 150);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(s, s,
                    Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            lp.y = G.dp(this, 40);
            try {
                ring.setAlpha(0f);
                wm.addView(ring, lp);
                up = true;
            } catch (Exception e) {
                return;
            }
        }
        ring.animate().cancel();
        ring.animate().alpha(1f).setDuration(150).start();
        h.removeCallbacks(hide);
        h.postDelayed(hide, 1600);
    }

    private final Runnable hide = () -> ring.animate().alpha(0f).setDuration(400).withEndAction(() -> {
        if (up) { try { wm.removeView(ring); } catch (Exception ignored) { } up = false; }
    }).start();

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(events);
        getContentResolver().unregisterContentObserver(watch);
        if (up) try { wm.removeView(ring); } catch (Exception ignored) { }
    }

    @Override public IBinder onBind(Intent i) { return null; }

    /** The bezel itself. */
    static class Ring extends View {
        private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG), track = new Paint(Paint.ANTI_ALIAS_FLAG),
                fill = new Paint(Paint.ANTI_ALIAS_FLAG), num = new Paint(Paint.ANTI_ALIAS_FLAG), lab = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF o = new RectF();
        private int v, max = 40;

        Ring(Context c) {
            super(c);
            bg.setColor(0xE6050505);
            track.setStyle(Paint.Style.STROKE);
            track.setStrokeWidth(G.dp(c, 6));
            track.setColor(0x33D4AF37);
            fill.setStyle(Paint.Style.STROKE);
            fill.setStrokeWidth(G.dp(c, 6));
            fill.setStrokeCap(Paint.Cap.ROUND);
            num.setTypeface(G.light(c));
            num.setTextAlign(Paint.Align.CENTER);
            num.setColor(G.WHITE);
            num.setTextSize(G.dp(c, 44));
            lab.setTypeface(G.wide(c));
            lab.setTextAlign(Paint.Align.CENTER);
            lab.setColor(G.GOLD);
            lab.setTextSize(G.dp(c, 9));
            lab.setLetterSpacing(0.3f);
        }

        void set(int v, int max) { this.v = v; this.max = max; invalidate(); }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            fill.setShader(new SweepGradient(w / 2f, h / 2f, new int[]{G.BRONZE, G.GOLD, G.IGNITION, 0xFFFFF1BF, G.BRONZE}, null));
        }

        @Override
        protected void onDraw(Canvas c) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f, r = Math.min(cx, cy) - G.dp(getContext(), 8);
            c.drawCircle(cx, cy, r + G.dp(getContext(), 6), bg);
            o.set(cx - r, cy - r, cx + r, cy + r);
            c.drawArc(o, 135, 270, false, track);
            c.drawArc(o, 135, 270f * v / max, false, fill);
            c.drawText(v == 0 ? "—" : String.valueOf(v), cx, cy + G.dp(getContext(), 14), num);
            c.drawText(v == 0 ? "SILENCIO" : "VOLUMEN", cx, cy + G.dp(getContext(), 44), lab);
        }
    }
}
