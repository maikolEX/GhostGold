package com.maikolex.ghostgold;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;

/**
 * The NWD head unit (Allwinner T3, Android 7.1): its MCU state lives in Settings.System keys
 * and it is driven with broadcasts. Facts observed on Maikol's K2001N radio.
 */
final class Head {

    private Head() { }

    // Broadcasts
    static final String ACC = "com.nwd.action.ACTION_MCU_STATE_CHANGE";            // extra_mcu_state: 1 on, 3 off
    static final String ILL = "com.nwd.ACTION_ILL_STATE_CHANGE";                   // extra_ill_state: headlights
    static final String VOLUME = "com.nwd.action.ACTION_VOLUME_STATE_CHANGE";
    static final String OS_VOLUME = "com.nwd.android.ACTION_OS_VOLUME_STATE_CHANGE";
    static final String REQUEST_SOURCE = "com.nwd.action.ACTION_REQUEST_CHANGE_SOURCE"; // extra_source_id (byte)
    static final String SOURCE_CHANGED = "com.nwd.action.ACTION_CHANGE_SOURCE";
    static final String APP_IN_OUT = "com.nwd.action.ACTION_APP_IN_OUT";

    // Settings.System keys
    static final String K_SOURCE = "mcu_current_source", K_VOLUME = "mcu_system_volume",
            K_MAX_VOLUME = "mcu_max_volume", K_DEVICES = "mcu_no_source_device_state";

    // Audio sources (byte ids)
    static final int SRC_RADIO = 4, SRC_BT = -125, SRC_USB = -128;

    // Factory apps
    static final String RADIO_PKG = "com.nwd.radio.service", BT_PKG = "com.bt.bc03";
    static final String EQ = "com.nwd.audioset/.home_horizontalActivity";
    static final String SYSTEM_SETTINGS = "com.android.car.setting/.home_horizontalActivity";
    static final String FACTORY_PHONE = "com.nwd.android.phone/.home_horizontalActivity";
    static final String TPMS = "com.nwd.can.setting/.ui.MainActivity";
    static final String ZLINK = "com.zjinnova.zlink";

    private static Boolean nwd;

    static boolean isNwd(Context c) {
        if (nwd == null) nwd = installed(c, RADIO_PKG) && installed(c, BT_PKG);
        return nwd;
    }

    static boolean installed(Context c, String pkg) {
        try {
            c.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    static int sys(Context c, String key, int def) {
        return Settings.System.getInt(c.getContentResolver(), key, def);
    }

    static int source(Context c) {
        return sys(c, K_SOURCE, 0);
    }

    static boolean headlights(Context c) {
        return (sys(c, K_DEVICES, 0) & 2) != 0;
    }

    /** Asks the MCU to route an audio source to the speakers. */
    static void requestSource(Context c, int id) {
        c.sendBroadcast(new Intent(REQUEST_SOURCE).putExtra("extra_source_id", (byte) id));
    }

    /** Bluetooth music: announced as the BT music app (id 15), then lift the A2DP mute. */
    static void selectBluetooth(Context c) {
        c.sendBroadcast(new Intent(APP_IN_OUT).putExtra("extra_app_id", 15)
                .putExtra("extra_app_operation", 1).putExtra("extra_app_event", 0));
        new android.os.Handler().postDelayed(() -> c.sendBroadcast(new Intent("com.bt.ACTION_A2DP_UNMUTE")), 500);
    }

    /** Opens an activity by "package/.Class", returns false if it is not there. */
    static boolean open(Context c, String component) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN).setComponent(ComponentName.unflattenFromString(component));
            c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static boolean openApp(Context c, String pkg) {
        Intent i = c.getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) return false;
        try {
            c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
