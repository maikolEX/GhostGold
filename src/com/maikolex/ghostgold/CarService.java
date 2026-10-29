package com.maikolex.ghostgold;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;

/**
 * Always-on background part of GHOST GOLD:
 * - GPS speed, odometer and trip (the head unit's GPS reports bogus speeds while parked, so
 *   speed comes from the distance between two good fixes);
 * - ignition (ACC) and screen wake: replays the intro, shows the trip summary at switch-off and
 *   brings the radio / Bluetooth audio back at switch-on.
 */
public class CarService extends Service implements LocationListener {

    static final String WAKE = "com.maikolex.ghostgold.WAKE";
    /** elapsedRealtime of the last ignition-on or screen-on. */
    static volatile long wokeAt;

    private final Handler h = new Handler();
    private final Tuner tuner = new Tuner();
    private LocationManager lm;
    private Location last;
    private float pending;

    static void start(Context c) {
        c.startService(new Intent(c, CarService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        tuner.bind(this);
        listenGps();
        IntentFilter f = new IntentFilter(Head.ACC);
        f.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(events, f);
        // Head unit just booted: restore the audio like after an ignition-on.
        if (SystemClock.elapsedRealtime() < 3 * 60_000) h.postDelayed(this::restoreAudio, 4000);
    }

    void listenGps() {
        if (lm != null) return;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1500, 5, this);
        } catch (Exception e) {
            lm = null;
        }
    }

    @Override
    public int onStartCommand(Intent i, int flags, int id) {
        listenGps(); // permission may have been granted since
        return START_STICKY;
    }

    // ---------- GPS ----------

    @Override
    public void onLocationChanged(Location l) {
        if (!l.hasAccuracy() || l.getAccuracy() > 25) return;
        if (last != null) {
            float d = last.distanceTo(l);
            float s = (l.getTime() - last.getTime()) / 1000f;
            if (s >= 0.5f && s <= 10f) {
                float kmh = d / s * 3.6f;
                if (kmh < 200) {
                    float rep = l.hasSpeed() ? l.getSpeed() * 3.6f : -1;
                    float v = rep >= 0 && rep < 200 && Math.abs(rep - kmh) < 25 ? rep : kmh;
                    Car.speed = v < 4 ? 0 : v;
                    Car.speedAt = SystemClock.elapsedRealtime();
                    if (kmh > 4) {
                        pending += d;
                        Car.tripMove(this, d, v, s);
                    }
                }
            }
        }
        last = l;
        if (pending >= 100) {
            Car.addMeters(this, pending);
            pending = 0;
        }
    }

    @Override public void onStatusChanged(String p, int s, Bundle b) { }
    @Override public void onProviderEnabled(String p) { }
    @Override public void onProviderDisabled(String p) { }

    // ---------- Ignition ----------

    private final BroadcastReceiver events = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            if (Intent.ACTION_SCREEN_ON.equals(i.getAction())) {
                wake();
                return;
            }
            int state = i.getByteExtra("extra_mcu_state", (byte) -1);
            if (state == 3) carOff();
            else if (state == 1) {
                wake();
                h.postDelayed(CarService.this::restoreAudio, 3000);
            }
        }
    };

    private void wake() {
        wokeAt = SystemClock.elapsedRealtime();
        sendBroadcast(new Intent(WAKE).setPackage(getPackageName()));
    }

    private void carOff() {
        if (pending > 0) { Car.addMeters(this, pending); pending = 0; }
        int src = Head.source(this);
        if (Media.get(this).isBluetooth() && Media.get(this).now() != null && Media.get(this).now().playing) src = Head.SRC_BT;
        Car.prefs(this).edit().putInt("last_source", src).apply();

        boolean recent = System.currentTimeMillis() - Car.tripLastMove(this) < 30 * 60_000;
        boolean shown = Car.prefs(this).getBoolean("trip_shown", false);
        Car.prefs(this).edit().putBoolean("trip_over", true).apply();
        if (recent && !shown && Car.tripKm(this) >= 0.3f) {
            Car.prefs(this).edit().putBoolean("trip_shown", true).apply();
            try {
                startActivity(new Intent(this, ByeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception ignored) { }
        }
    }

    /** The kernel would leave GHOST GOLD's "no source" in front: bring back what was playing. */
    private void restoreAudio() {
        if (!Head.isNwd(this)) return;
        int src = Car.prefs(this).getInt("last_source", Head.source(this));
        if (src == Head.SRC_RADIO) tuner.select(this);
        else if (src == Head.SRC_BT) {
            Head.selectBluetooth(this);
            h.postDelayed(() -> Media.get(this).phone.music(Phone.PLAY), 1500);
        } else if (src == Head.SRC_USB) Head.requestSource(this, Head.SRC_USB);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(events);
        if (lm != null) lm.removeUpdates(this);
        tuner.unbind(this);
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    /** Starts GHOST GOLD's background service when the head unit boots. */
    public static class Boot extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent i) {
            start(c);
        }
    }
}
