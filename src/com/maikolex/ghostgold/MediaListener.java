package com.maikolex.ghostgold;

import android.service.notification.NotificationListenerService;

/** Only exists so Android lets GHOST GOLD read what other apps (Spotify, YouTube) are playing. */
public class MediaListener extends NotificationListenerService {
    @Override
    public void onListenerConnected() {
        Media.get(this).attach();
    }
}
