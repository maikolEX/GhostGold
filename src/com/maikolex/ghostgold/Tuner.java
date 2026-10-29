package com.maikolex.ghostgold;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * FM/AM tuner of the head unit, reached through the factory radio service's Binder interface
 * (descriptor com.nwd.radio.service.RadioFeature).
 */
final class Tuner implements ServiceConnection {

    private static final String IFACE = "com.nwd.radio.service.RadioFeature";
    private static final int SET = 1, GET = 2, SEEK = 3, BAND = 5, PRESETS = 21, REG = 24, UNREG = 25, STEP = 26;

    static final class Station {
        final int band, freq;   // band 0-2 FM, 3-5 AM, 6-8 FM; freq: FM MHz*100, AM kHz
        final String name;      // RDS
        Station(int band, int freq, String name) {
            this.band = band;
            this.freq = freq;
            this.name = name == null ? "" : name.trim();
        }
        boolean am() { return band >= 3 && band <= 5; }
        float mhz() { return freq / 100f; }
        String band() {
            String[] n = {"FM1", "FM2", "FM3", "AM1", "AM2", "AM3", "FM1", "FM2", "FM3"};
            return band >= 0 && band < n.length ? n[band] : "FM";
        }
        String freq() {
            if (am()) return String.valueOf(freq);
            return String.format(Locale.US, "%.1f", freq / 100f);
        }
    }

    private IBinder b;

    void bind(Context c) {
        try {
            c.bindService(new Intent("com.nwd.radio.service.ACTION_RADIO_SERVICE").setPackage(Head.RADIO_PKG),
                    this, Context.BIND_AUTO_CREATE);
        } catch (Exception ignored) { }
    }

    void unbind(Context c) {
        try { c.unbindService(this); } catch (Exception ignored) { }
        b = null;
    }

    @Override public void onServiceConnected(ComponentName n, IBinder s) { b = s; }
    @Override public void onServiceDisconnected(ComponentName n) { b = null; }

    boolean ready() {
        return b != null && b.isBinderAlive();
    }

    private interface Writer { void write(Parcel p); }
    private interface Reader<T> { T read(Parcel p); }

    private <T> T tx(int code, Writer w, Reader<T> r, T fallback) {
        if (!ready()) return fallback;
        Parcel in = Parcel.obtain(), out = Parcel.obtain();
        try {
            in.writeInterfaceToken(IFACE);
            if (w != null) w.write(in);
            b.transact(code, in, out, 0);
            out.readException();
            return r == null ? fallback : r.read(out);
        } catch (Exception e) {
            return fallback;
        } finally {
            in.recycle();
            out.recycle();
        }
    }

    private static Station station(Parcel p) {
        if (p.readInt() == 0) return null;
        int band = p.readByte();
        String name = p.readString();
        int freq = p.readInt();
        return new Station(band, freq, name);
    }

    Station current() {
        return tx(GET, null, Tuner::station, null);
    }

    List<Station> presets() {
        return tx(PRESETS, null, p -> {
            List<Station> l = new ArrayList<>();
            int n = p.readInt();
            for (int i = 0; i < n; i++) {
                Station s = station(p);
                if (s != null) l.add(s);
            }
            return l;
        }, new ArrayList<>());
    }

    /** Tune to a frequency; slot = preset index (or -1 for a free frequency). */
    void tune(int band, int freq, int slot) {
        tx(SET, p -> { p.writeInt(freq); p.writeByte((byte) band); p.writeInt(slot); }, null, null);
    }

    void seek(boolean up) { tx(SEEK, p -> p.writeInt(up ? 1 : 0), null, null); }
    void step(boolean next) { tx(STEP, p -> p.writeInt(next ? 1 : 0), null, null); }
    void band() { tx(BAND, null, null, null); }

    /**
     * Make the radio the audio source and power the tuner up: the service runs its FM start-up
     * (power, last frequency, unmute) on a "source is radio" notice and when a client registers.
     */
    void select(Context c) {
        Head.requestSource(c, Head.SRC_RADIO);
        new android.os.Handler().postDelayed(() -> {
            c.sendBroadcast(new Intent(Head.SOURCE_CHANGED).setPackage(Head.RADIO_PKG).putExtra("extra_source_id", (byte) Head.SRC_RADIO));
            c.sendBroadcast(new Intent(Head.APP_IN_OUT).setPackage(Head.RADIO_PKG).putExtra("extra_app_id", 8));
            Binder cb = new Binder() {
                @Override protected boolean onTransact(int code, Parcel d, Parcel r, int f) { return true; }
            };
            cb.attachInterface(null, "com.nwd.radio.service.RadioCallback");
            tx(REG, p -> p.writeStrongBinder(cb), null, null);
            tx(UNREG, p -> p.writeStrongBinder(cb), null, null);
        }, 800);
    }

    static boolean isSource(Context c) {
        return Head.source(c) == Head.SRC_RADIO;
    }
}
