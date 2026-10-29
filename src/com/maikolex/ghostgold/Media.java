package com.maikolex.ghostgold;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

/**
 * Now playing, from any app with a media session (Spotify, YouTube, ZLink...) through the
 * notification-listener permission, and from the phone over Bluetooth (AVRCP).
 */
final class Media {

    interface Listener { void onMedia(); }

    static final class Track {
        String title = "", artist = "", app = "";
        boolean playing;
        Bitmap art;
        long position, duration;
    }

    private static Media one;

    static Media get(Context c) {
        if (one == null) one = new Media(c.getApplicationContext());
        return one;
    }

    final Phone phone = new Phone();
    private final Context ctx;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new ArrayList<>();
    private MediaController session;
    private final Track bt = new Track();
    private boolean btMode;
    private boolean watching;

    private Media(Context c) {
        ctx = c;
        bt.app = "Bluetooth";
        phone.bind(c, () -> {
            bt.playing = phone.musicPlaying();
            phone.askSong();
        });
        IntentFilter f = new IntentFilter();
        f.addAction("com.bt.ACTION_AVRCP_MUSIC_ID3");
        f.addAction("com.bt.ACTION_BT_MUSIC_PLAY");
        f.addAction("com.bt.ACTION_BT_MUSIC_PAUSE");
        c.registerReceiver(btEvents, f);
        attach();
    }

    private final BroadcastReceiver btEvents = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            String a = i.getAction();
            if ("com.bt.ACTION_AVRCP_MUSIC_ID3".equals(a)) {
                bt.title = nz(i.getStringExtra("extra_avrcp_id3_title"));
                bt.artist = nz(i.getStringExtra("extra_avrcp_id3_artist"));
            } else if ("com.bt.ACTION_BT_MUSIC_PLAY".equals(a)) {
                if (!bt.playing) phone.askSong();
                bt.playing = true;
                btMode = true;
            } else {
                bt.playing = false;
            }
            notifyAll2();
        }
    };

    // ---------- Media sessions ----------

    private final MediaController.Callback cb = new MediaController.Callback() {
        @Override public void onMetadataChanged(MediaMetadata m) { notifyAll2(); }
        @Override public void onPlaybackStateChanged(PlaybackState s) {
            if (s != null && s.getState() == PlaybackState.STATE_PLAYING) btMode = false;
            notifyAll2();
        }
        @Override public void onSessionDestroyed() { attach(); }
    };

    static boolean allowed(Context c) {
        String l = android.provider.Settings.Secure.getString(c.getContentResolver(), "enabled_notification_listeners");
        return l != null && l.contains(c.getPackageName());
    }

    /** Re-reads the active sessions; called when the listener connects or sessions change. */
    void attach() {
        if (!allowed(ctx)) return;
        try {
            MediaSessionManager m = (MediaSessionManager) ctx.getSystemService(Context.MEDIA_SESSION_SERVICE);
            ComponentName me = new ComponentName(ctx, MediaListener.class);
            if (!watching) {
                m.addOnActiveSessionsChangedListener(l -> main.post(this::attach), me);
                watching = true;
            }
            List<MediaController> list = m.getActiveSessions(me);
            MediaController pick = null;
            for (MediaController mc : list) {
                PlaybackState s = mc.getPlaybackState();
                if (s != null && s.getState() == PlaybackState.STATE_PLAYING) { pick = mc; break; }
            }
            if (pick == null && !list.isEmpty()) pick = list.get(0);
            if (session != null) session.unregisterCallback(cb);
            session = pick;
            if (session != null) session.registerCallback(cb, main);
        } catch (SecurityException ignored) {
        }
        notifyAll2();
    }

    private Track fromSession() {
        Track t = new Track();
        if (session == null) return null;
        MediaMetadata m = session.getMetadata();
        PlaybackState s = session.getPlaybackState();
        if (m != null) {
            t.title = nz(m.getString(MediaMetadata.METADATA_KEY_TITLE));
            t.artist = nz(m.getString(MediaMetadata.METADATA_KEY_ARTIST));
            t.art = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (t.art == null) t.art = m.getBitmap(MediaMetadata.METADATA_KEY_ART);
            t.duration = m.getLong(MediaMetadata.METADATA_KEY_DURATION);
        }
        if (s != null) {
            t.playing = s.getState() == PlaybackState.STATE_PLAYING;
            t.position = s.getPosition();
        }
        t.app = appName(session.getPackageName());
        return t;
    }

    private String appName(String pkg) {
        if (pkg == null) return "";
        if (pkg.contains("spotify")) return "Spotify";
        if (pkg.contains("youtube")) return "YouTube";
        if (pkg.contains("zlink")) return "Android Auto";
        try {
            return ctx.getPackageManager().getApplicationLabel(ctx.getPackageManager().getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    /** What to show: Bluetooth when it is what plays, otherwise the media session. */
    Track now() {
        Track s = fromSession();
        if (btMode && (bt.playing || s == null || !s.playing)) return bt;
        if (s != null) return s;
        return bt.title.isEmpty() ? null : bt;
    }

    boolean isBluetooth() {
        return now() == bt;
    }

    void playPause() {
        Track t = now();
        if (t == bt || (t == null && phone.connected())) {
            phone.music(bt.playing ? Phone.PAUSE : Phone.PLAY);
            if (!bt.playing) Head.selectBluetooth(ctx);
        } else if (session != null) {
            PlaybackState s = session.getPlaybackState();
            if (s != null && s.getState() == PlaybackState.STATE_PLAYING) session.getTransportControls().pause();
            else session.getTransportControls().play();
        }
    }

    void skip(boolean next) {
        if (isBluetooth()) phone.music(next ? Phone.NEXT : Phone.PREV);
        else if (session != null) {
            if (next) session.getTransportControls().skipToNext();
            else session.getTransportControls().skipToPrevious();
        }
    }

    /** Opens the app that is playing (Spotify, YouTube...), or Spotify by default. */
    void open(Context c) {
        String pkg = session != null && !isBluetooth() ? session.getPackageName() : "com.spotify.music";
        if (!Head.openApp(c, pkg)) Head.openApp(c, "com.spotify.music");
    }

    void add(Listener l) { if (!listeners.contains(l)) listeners.add(l); }
    void remove(Listener l) { listeners.remove(l); }

    private void notifyAll2() {
        for (Listener l : new ArrayList<>(listeners)) l.onMedia();
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}
