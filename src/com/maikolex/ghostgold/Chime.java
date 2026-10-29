package com.maikolex.ghostgold;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

/**
 * The GHOST GOLD ignition sound (res/raw/ignition.ogg, synthesised for the app): a low thump
 * with the seal, a gold chord as the name lights up and two bells for "system online" and
 * "vehicle ready". Played as navigation guidance so the head unit mixes it over the radio.
 */
final class Chime {

    private static MediaPlayer player;

    private Chime() { }

    static boolean enabled(Context c) {
        return Store.prefs(c).getBoolean("chime", true);
    }

    static void setEnabled(Context c, boolean on) {
        Store.prefs(c).edit().putBoolean("chime", on).apply();
    }

    static void play(Context c) {
        if (!enabled(c)) return;
        stop();
        try {
            int id = c.getResources().getIdentifier("ignition", "raw", c.getPackageName());
            if (id == 0) return;
            MediaPlayer mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            android.content.res.AssetFileDescriptor fd = c.getResources().openRawResourceFd(id);
            mp.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            fd.close();
            mp.setOnCompletionListener(MediaPlayer::release);
            mp.prepare();
            mp.start();
            player = mp;
        } catch (Exception ignored) {
        }
    }

    static void stop() {
        if (player != null) {
            try { player.release(); } catch (Exception ignored) { }
            player = null;
        }
    }
}
