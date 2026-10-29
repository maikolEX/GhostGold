package com.maikolex.ghostgold;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

/** Night screen: almost black, a dim gold clock that drifts slightly to protect the panel. Touch to leave. */
public class NightActivity extends Activity {

    private LinearLayout box;
    private final android.os.Handler h = new android.os.Handler();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 0.05f;
        getWindow().setAttributes(lp);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);
        box = G.column(this);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        TextClock t = new TextClock(this);
        t.setFormat12Hour("h:mm");
        t.setFormat24Hour("H:mm");
        t.setTextSize(96);
        t.setTypeface(G.light(this));
        t.setTextColor(0x99D4AF37);
        box.addView(t);
        TextView k = G.label(this, "Ghost Gold · noche");
        k.setTextColor(0x66D4AF37);
        box.addView(k);
        root.addView(box, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        root.setOnClickListener(v -> finish());
        setContentView(root);
        h.post(drift);
    }

    private final Runnable drift = new Runnable() {
        public void run() {
            box.animate().translationX((float) (Math.random() - 0.5) * G.dp(NightActivity.this, 120))
                    .translationY((float) (Math.random() - 0.5) * G.dp(NightActivity.this, 80)).setDuration(4000).start();
            h.postDelayed(this, 60_000);
        }
    };

    @Override protected void onDestroy() { super.onDestroy(); h.removeCallbacksAndMessages(null); }
}
