package com.maikolex.ghostgold;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcel;

/**
 * The head unit's Bluetooth module (factory service com.bt.bc03, interface com.bt.BTFeature):
 * phone connection, dialling and Bluetooth music control.
 */
final class Phone implements ServiceConnection {

    private static final String IFACE = "com.bt.BTFeature";
    private static final int CONNECTED = 6, DIAL = 13, MUSIC = 23, MUSIC_PLAYING = 24, DEVICE = 38, ID3 = 60;
    static final int PLAY = 0, PAUSE = 1, NEXT = 2, PREV = 3;

    private IBinder b;
    private Runnable ready;

    void bind(Context c, Runnable onReady) {
        ready = onReady;
        try {
            c.bindService(new Intent("com.bt.ACTION_START_SERVICE").setPackage(Head.BT_PKG), this, Context.BIND_AUTO_CREATE);
        } catch (Exception ignored) { }
    }

    @Override
    public void onServiceConnected(ComponentName n, IBinder s) {
        b = s;
        if (ready != null) ready.run();
    }

    @Override public void onServiceDisconnected(ComponentName n) { b = null; }

    boolean bound() {
        return b != null && b.isBinderAlive();
    }

    private Parcel call(int code, Object arg) {
        if (!bound()) return null;
        Parcel in = Parcel.obtain(), out = Parcel.obtain();
        try {
            in.writeInterfaceToken(IFACE);
            if (arg instanceof Integer) in.writeInt((Integer) arg);
            else if (arg instanceof String) in.writeString((String) arg);
            b.transact(code, in, out, 0);
            out.readException();
            return out;
        } catch (Exception e) {
            out.recycle();
            return null;
        } finally {
            in.recycle();
        }
    }

    private boolean bool(int code, Object arg) {
        Parcel r = call(code, arg);
        if (r == null) return false;
        try { return r.readInt() != 0; } finally { r.recycle(); }
    }

    boolean connected() { return bool(CONNECTED, null); }
    boolean musicPlaying() { return bool(MUSIC_PLAYING, null); }
    boolean dial(String number) { return bool(DIAL, number); }

    void music(int command) {
        Parcel r = call(MUSIC, command);
        if (r != null) r.recycle();
    }

    /** Asks for the current song; the answer is a com.bt.ACTION_AVRCP_MUSIC_ID3 broadcast. */
    void askSong() {
        Parcel r = call(ID3, null);
        if (r != null) r.recycle();
    }

    /** Name of the connected phone, or null. */
    String phoneName() {
        Parcel r = call(DEVICE, null);
        if (r == null) return null;
        try {
            if (r.readInt() == 0) return null;
            r.readString(); // address
            return r.readString();
        } catch (Exception e) {
            return null;
        } finally {
            r.recycle();
        }
    }
}
