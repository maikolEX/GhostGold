package com.maikolex.ghostgold;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * GHOST GOLD home. The truck is the centre of everything: around it the time, weather,
 * restriction, speed, radio, music and alerts. Parked: front view, showroom after two idle
 * minutes. Driving: the truck from behind on the road.
 */
public class HomeActivity extends Activity implements Media.Listener {

    private static boolean introPlayed;
    private static long introAt;

    private final Handler h = new Handler();
    private final Tuner tuner = new Tuner();
    private Media media;
    private boolean night;

    private FrameLayout stageBox, intro;
    private Stage stage;
    private Road road;
    private Scenery scenery;
    private View rail, railLine, caption;
    private final List<View> hud = new ArrayList<>();

    private TextView greet, restrictText, temp, sky, place, speed, odo, band, freq, src, title, artist, status;
    private ImageView skyIcon, play;
    private TyreMap tyreMap;
    private TextView tyresLabel;
    private View restrictDot;
    private GradientDrawable restrictBg;

    private boolean showroom, driving;
    private long touchedAt = SystemClock.elapsedRealtime();
    private int fast, slow, tick;
    private long testUntil;
    private long weatherAt;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        night = Head.headlights(this) || isNightHour();
        media = Media.get(this);

        FrameLayout root = new FrameLayout(this);
        root.setBackground(background());
        LinearLayout row = new LinearLayout(this);
        rail = buildRail();
        row.addView(rail, new LinearLayout.LayoutParams(G.dp(this, 92), -1));
        railLine = new View(this);
        railLine.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0, 0x80D4AF37, 0x80D4AF37, 0}));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(G.dp(this, 1), -1);
        lp.setMargins(0, G.dp(this, 40), 0, G.dp(this, 40));
        row.addView(railLine, lp);
        stageBox = buildStage();
        row.addView(stageBox, new LinearLayout.LayoutParams(0, -1, 1f));
        root.addView(row);
        intro = buildIntro();
        root.addView(intro, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        tuner.bind(this);
        CarService.start(this);
        startService(new Intent(this, Panel.class));
        if (Head.isNwd(this)) startService(new Intent(this, VolumeHud.class));
        if (!introPlayed) { introPlayed = true; playIntro(); } else intro.setVisibility(View.GONE);
    }

    private static boolean isNightHour() {
        int hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hr >= 18 || hr < 6;
    }

    private android.graphics.drawable.Drawable background() {
        GradientDrawable base = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{G.ONYX, G.VOID, 0xFF000000});
        GradientDrawable glow = new GradientDrawable();
        glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        glow.setGradientRadius(G.dp(this, 620));
        glow.setGradientCenter(0.55f, 0.55f);
        glow.setColors(new int[]{night ? 0x14D4AF37 : 0x24D4AF37, 0});
        return new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{base, glow});
    }

    // ======================= Rail =======================

    private View buildRail() {
        LinearLayout r = G.column(this);
        r.setGravity(Gravity.CENTER_HORIZONTAL);
        r.setPadding(0, G.dp(this, 16), 0, G.dp(this, 12));
        Emblem e = new Emblem(this);
        r.addView(e, new LinearLayout.LayoutParams(G.dp(this, 48), G.dp(this, 48)));
        G.press(e);
        e.setOnClickListener(v -> playIntro());
        e.setOnLongClickListener(v -> { startActivity(new Intent(this, ByeActivity.class)); return true; });

        LinearLayout items = G.column(this);
        items.setGravity(Gravity.CENTER);
        items.addView(item(Glyph.NAV, "Mapa", this::maps, this::chooseMaps));
        items.addView(item(Glyph.AUTO, "Android Auto", this::androidAuto, null));
        items.addView(item(Glyph.MUSIC, "Música", () -> media.open(this), null));
        items.addView(item(Glyph.PHONE, "Teléfono", () -> go(PhoneActivity.class), null));
        items.addView(item(Glyph.RADIO, "Radio", () -> go(RadioActivity.class), null));
        items.addView(item(Glyph.APPS, "Apps", () -> go(AppsActivity.class), null));
        items.addView(item(Glyph.WRENCH, "Mantenimiento", () -> go(GarageActivity.class), null));
        items.addView(item(Glyph.CAR, "Showroom", this::toggleShowroom, null));
        items.addView(item(Glyph.SETTINGS, "Ajustes", () -> go(SettingsActivity.class), null));
        r.addView(items, new LinearLayout.LayoutParams(-1, 0, 1f));
        r.addView(item(Glyph.MOON, "Noche", () -> go(NightActivity.class), null));
        return r;
    }

    private View item(int glyph, String name, Runnable tap, Runnable hold) {
        ImageView v = new ImageView(this);
        Glyph g = new Glyph(this, glyph, G.MUTED);
        v.setImageDrawable(g);
        v.setContentDescription(name);
        int p = G.dp(this, 13);
        v.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(G.dp(this, 50), G.dp(this, 50));
        lp.setMargins(0, G.dp(this, 2), 0, G.dp(this, 2));
        v.setLayoutParams(lp);
        G.press(v);
        v.setOnClickListener(x -> {
            v.setBackground(G.round(G.TINT, G.HAIR, G.dp(this, 1)));
            g.setColor(G.IGNITION);
            h.postDelayed(() -> { v.setBackground(null); g.setColor(G.MUTED); }, 900);
            tap.run();
        });
        if (hold != null) v.setOnLongClickListener(x -> { hold.run(); return true; });
        return v;
    }

    private void go(Class<?> c) {
        startActivity(new Intent(this, c));
    }

    // ======================= Stage =======================

    private FrameLayout buildStage() {
        FrameLayout s = new FrameLayout(this);
        scenery = new Scenery(this);
        scenery.setNight(night);
        s.addView(scenery, new FrameLayout.LayoutParams(-1, -1));
        stage = new Stage(this);
        stage.setNight(night);
        s.addView(stage, new FrameLayout.LayoutParams(-1, -1));
        stage.setOnClickListener(v -> toggleShowroom());
        road = new Road(this);
        road.setAlpha(0f);
        road.setVisibility(View.GONE);
        s.addView(road, new FrameLayout.LayoutParams(-1, -1));

        s.addView(hud(clock()), at(Gravity.TOP | Gravity.START, 36, 22, 0, 0));
        s.addView(hud(restriction()), at(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 26, 0, 0));
        s.addView(hud(weather()), at(Gravity.TOP | Gravity.END, 0, 22, 36, 0));
        s.addView(hud(vehicle()), at(Gravity.START | Gravity.CENTER_VERTICAL, 36, 60, 0, 0));
        s.addView(hud(radio()), at(Gravity.END | Gravity.CENTER_VERTICAL, 0, 0, 36, 90));
        s.addView(hud(music()), at(Gravity.END | Gravity.BOTTOM, 0, 0, 36, 58));
        s.addView(hud(statusLine()), at(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0, 0, 18));

        LinearLayout cap = G.column(this);
        cap.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView k = G.label(this, "Showroom");
        k.setTextColor(G.GOLD);
        k.setLetterSpacing(0.5f);
        cap.addView(k);
        TextView t = G.label(this, "Dongfeng Rich 6");
        t.setTextSize(22);
        t.setLetterSpacing(0.35f);
        t.setTextColor(G.WHITE);
        t.setPadding(0, G.dp(this, 10), 0, 0);
        cap.addView(t);
        cap.setAlpha(0f);
        caption = cap;
        s.addView(cap, at(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 40, 0, 0));
        return s;
    }

    private View hud(View v) { hud.add(v); return v; }

    private FrameLayout.LayoutParams at(int g, int l, int t, int r, int b) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, g);
        lp.setMargins(G.dp(this, l), G.dp(this, t), G.dp(this, r), G.dp(this, b));
        return lp;
    }

    // ---------- Clock ----------

    private View clock() {
        LinearLayout c = G.column(this);
        TextClock t = new TextClock(this);
        t.setFormat12Hour("h:mm");
        t.setFormat24Hour("H:mm");
        t.setTextSize(56);
        t.setTextColor(G.WHITE);
        t.setTypeface(G.light(this));
        t.setIncludeFontPadding(false);
        c.addView(t);
        TextClock d = new TextClock(this);
        d.setFormat12Hour("EEEE · d MMM");
        d.setFormat24Hour("EEEE · d MMM");
        d.setAllCaps(true);
        d.setTextSize(10);
        d.setLetterSpacing(0.22f);
        d.setTypeface(G.wide(this));
        d.setTextColor(G.MUTED);
        d.setPadding(0, G.dp(this, 6), 0, 0);
        c.addView(d);
        greet = G.text(this, "", 16, G.TEXT);
        greet.setPadding(0, G.dp(this, 12), 0, 0);
        c.addView(greet);
        return c;
    }

    // ---------- Restriction ----------

    private View restriction() {
        LinearLayout p = G.row(this);
        p.setPadding(G.dp(this, 16), G.dp(this, 10), G.dp(this, 18), G.dp(this, 10));
        restrictBg = G.pill(this, G.TINT, G.HAIR);
        p.setBackground(restrictBg);
        restrictDot = new View(this);
        p.addView(restrictDot, new LinearLayout.LayoutParams(G.dp(this, 8), G.dp(this, 8)));
        LinearLayout t = G.column(this);
        t.setPadding(G.dp(this, 12), 0, 0, 0);
        TextView k = G.label(this, "Restricción · San José");
        k.setTextSize(8);
        k.setTextColor(G.GOLD);
        t.addView(k);
        restrictText = G.text(this, "", 15, G.WHITE);
        restrictText.setPadding(0, G.dp(this, 3), 0, 0);
        t.addView(restrictText);
        p.addView(t);
        G.press(p);
        p.setOnClickListener(v -> choosePlate());
        return p;
    }

    private void showRestriction() {
        boolean now = Rules.now(this);
        restrictText.setText(Rules.line(this));
        restrictDot.setBackground(G.round(now ? G.AMBER : Rules.today(this) ? G.CHAMPAGNE : G.GOLD, 0, 0));
        restrictBg.setColor(now ? 0x26E8A33D : G.TINT);
        restrictBg.setStroke(G.dp(this, 1), now ? 0xB3E8A33D : G.HAIR);
    }

    private void choosePlate() {
        String[] d = new String[10];
        for (int i = 0; i < 10; i++) d[i] = "Placa terminada en " + i + " · " + dayFor(i);
        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Último número de la placa")
                .setSingleChoiceItems(d, Store.plate(this), (dlg, w) -> { Store.setPlate(this, w); refresh(); dlg.dismiss(); })
                .show();
    }

    private static String dayFor(int digit) {
        String[] n = {"", "", "lunes", "martes", "miércoles", "jueves", "viernes"};
        return n[Rules.day(digit)];
    }

    // ---------- Weather ----------

    private View weather() {
        LinearLayout c = G.column(this);
        c.setGravity(Gravity.END);
        LinearLayout top = G.row(this);
        skyIcon = new ImageView(this);
        skyIcon.setImageDrawable(new Glyph(this, Glyph.CLOUD, G.GOLD));
        top.addView(skyIcon, new LinearLayout.LayoutParams(G.dp(this, 42), G.dp(this, 42)));
        temp = G.value(this, "--°", 56, G.WHITE);
        temp.setPadding(G.dp(this, 14), 0, 0, 0);
        top.addView(temp);
        c.addView(top);
        sky = G.text(this, "Clima", 16, G.TEXT);
        sky.setGravity(Gravity.END);
        sky.setPadding(0, G.dp(this, 6), 0, 0);
        c.addView(sky, new LinearLayout.LayoutParams(G.dp(this, 260), -2));
        place = G.label(this, "");
        place.setGravity(Gravity.END);
        place.setPadding(0, G.dp(this, 5), 0, 0);
        c.addView(place, new LinearLayout.LayoutParams(G.dp(this, 260), -2));
        G.press(c);
        c.setOnClickListener(v -> loadWeather(true));
        return c;
    }

    private void showWeather(Weather w) {
        if (w == null) return;
        skyIcon.setImageDrawable(new Glyph(this, w.glyph(), w.glyph() == Glyph.SUN ? G.IGNITION : G.GOLD));
        temp.setText(w.temp + "°");
        sky.setText(w.sky());
        place.setText((w.place + " · Máx " + w.max + " · Mín " + w.min).toUpperCase());
    }

    private void loadWeather(boolean force) {
        long now = SystemClock.elapsedRealtime();
        if (!force && weatherAt != 0 && now - weatherAt < 30 * 60_000) return;
        weatherAt = now;
        Weather.fetch(this, this::showWeather);
    }

    // ---------- Speed, odometer, tyres ----------

    private View vehicle() {
        LinearLayout c = G.column(this);
        c.addView(G.label(this, "Velocidad"));
        LinearLayout v = new LinearLayout(this);
        v.setGravity(Gravity.BOTTOM);
        speed = G.value(this, "0", 62, G.IGNITION);
        speed.setShadowLayer(G.dp(this, 14), 0, 0, 0x59FFD700);
        v.addView(speed);
        TextView u = G.label(this, "km/h");
        u.setPadding(G.dp(this, 8), 0, 0, G.dp(this, 12));
        v.addView(u);
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(-2, -2);
        vlp.topMargin = G.dp(this, 6);
        c.addView(v, vlp);
        View line = G.hairline(this);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(G.dp(this, 150), G.dp(this, 1));
        llp.setMargins(0, G.dp(this, 14), 0, G.dp(this, 14));
        c.addView(line, llp);
        c.addView(G.label(this, "Odómetro"));
        odo = G.text(this, "", 20, G.WHITE);
        odo.setPadding(0, G.dp(this, 6), 0, 0);
        c.addView(odo);

        LinearLayout tyres = G.column(this);
        tyres.setPadding(0, G.dp(this, 18), 0, 0);
        tyresLabel = G.label(this, "Llantas · TPMS");
        tyres.addView(tyresLabel);
        tyreMap = new TyreMap(this);
        LinearLayout.LayoutParams tm = new LinearLayout.LayoutParams(G.dp(this, 170), G.dp(this, 96));
        tm.topMargin = G.dp(this, 8);
        tyres.addView(tyreMap, tm);
        G.press(tyres);
        tyres.setOnClickListener(x -> { if (!Head.open(this, Head.TPMS)) toast("Pantalla de TPMS no disponible"); });
        c.addView(tyres);

        G.press(c);
        c.setOnClickListener(x -> go(GarageActivity.class));
        c.setOnLongClickListener(x -> { testDrive(); return true; });
        return c;
    }

    private int kmh() {
        long now = SystemClock.elapsedRealtime();
        if (now < testUntil) {
            long left = testUntil - now;
            return left < 8000 ? (int) (45 * left / 8000f) : 45 + (int) (6 * Math.sin(now / 2500.0));
        }
        return Car.speed();
    }

    private void testDrive() {
        long now = SystemClock.elapsedRealtime();
        if (now < testUntil) testUntil = now + 8000;
        else { testUntil = now + 45_000; toast("Prueba de conducción: 45 segundos"); }
    }

    // ---------- Radio ----------

    private View radio() {
        LinearLayout c = G.column(this);
        c.setGravity(Gravity.END);
        band = G.label(this, "Radio FM");
        band.setGravity(Gravity.END);
        c.addView(band, new LinearLayout.LayoutParams(G.dp(this, 220), -2));
        LinearLayout r = G.row(this);
        r.addView(G.button(this, Glyph.PREV, 40, false, () -> station(false)));
        freq = G.value(this, "--.-", 40, G.WHITE);
        freq.setPadding(G.dp(this, 14), 0, G.dp(this, 14), 0);
        r.addView(freq);
        r.addView(G.button(this, Glyph.NEXT, 40, false, () -> station(true)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = G.dp(this, 8);
        c.addView(r, lp);
        G.press(c);
        c.setOnClickListener(v -> go(RadioActivity.class));
        return c;
    }

    private void station(boolean next) {
        if (!Head.isNwd(this)) { go(RadioActivity.class); return; }
        if (!Tuner.isSource(this)) { tuner.select(this); h.postDelayed(this::showRadio, 2500); return; }
        tuner.step(next);
        h.postDelayed(this::showRadio, 300);
    }

    private void showRadio() {
        Tuner.Station s = tuner.current();
        if (s == null) return;
        band.setText((s.name.isEmpty() ? "Radio " + s.band() : s.band() + " · " + s.name).toUpperCase());
        freq.setText(s.freq() + (s.am() ? " kHz" : ""));
    }

    // ---------- Music ----------

    private View music() {
        LinearLayout c = G.column(this);
        c.setGravity(Gravity.END);
        src = G.label(this, "Música");
        src.setGravity(Gravity.END);
        c.addView(src, new LinearLayout.LayoutParams(G.dp(this, 290), -2));
        title = G.text(this, "", 20, G.WHITE);
        title.setGravity(Gravity.END);
        title.setPadding(0, G.dp(this, 6), 0, 0);
        c.addView(title, new LinearLayout.LayoutParams(G.dp(this, 290), -2));
        artist = G.text(this, "", 15, G.MUTED);
        artist.setGravity(Gravity.END);
        artist.setPadding(0, G.dp(this, 4), 0, 0);
        c.addView(artist, new LinearLayout.LayoutParams(G.dp(this, 290), -2));
        LinearLayout k = G.row(this);
        k.addView(G.button(this, Glyph.PREV, 44, false, () -> media.skip(false)));
        play = G.button(this, Glyph.PLAY, 58, true, () -> media.playPause());
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(G.dp(this, 58), G.dp(this, 58));
        plp.setMargins(G.dp(this, 14), 0, G.dp(this, 14), 0);
        k.addView(play, plp);
        k.addView(G.button(this, Glyph.NEXT, 44, false, () -> media.skip(true)));
        LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(-2, -2);
        klp.topMargin = G.dp(this, 12);
        c.addView(k, klp);
        G.press(c);
        c.setOnClickListener(v -> media.open(this));
        return c;
    }

    @Override
    public void onMedia() {
        Media.Track t = media.now();
        boolean none = t == null || t.title.isEmpty();
        src.setText(none ? "MÚSICA" : t.app.toUpperCase());
        title.setText(none ? "Nada sonando" : t.title);
        artist.setText(none ? (Media.allowed(this) ? "Toca para abrir Spotify" : "Activa el acceso en Ajustes para ver Spotify") : t.artist);
        play.setImageDrawable(new Glyph(this, t != null && t.playing ? Glyph.PAUSE : Glyph.PLAY, G.VOID));
    }

    // ---------- Status / alerts ----------

    private View statusLine() {
        status = G.label(this, "");
        status.setGravity(Gravity.CENTER);
        status.setPadding(G.dp(this, 18), G.dp(this, 10), G.dp(this, 18), G.dp(this, 10));
        G.press(status);
        status.setOnClickListener(v -> go(GarageActivity.class));
        return status;
    }

    private void showStatus() {
        List<Alerts.Alert> a = Alerts.now(this);
        if (!a.isEmpty()) {
            Alerts.Alert x = a.get((tick / 6) % a.size());
            status.setText(("⚠  " + x.title + " · " + x.detail).toUpperCase());
            status.setTextColor(x.level == 2 ? G.AMBER : G.CHAMPAGNE);
            status.setBackground(G.pill(this, x.level == 2 ? 0x2EE8A33D : G.TINT, x.level == 2 ? 0xB3E8A33D : G.HAIR));
            return;
        }
        status.setBackground(null);
        status.setTextColor(G.MUTED);
        Garage.Job oil = Garage.oil();
        String o = Car.odoSet(this) ? "Aceite en " + Car.fmt(Math.max(0, Garage.left(this, oil))) + " km" : "Pon el kilometraje";
        status.setText(("Mantenimiento al día · " + o + "     ·     Placa termina en " + Store.plate(this)).toUpperCase());
    }

    // ======================= Modes =======================

    private void toggleShowroom() {
        if (!showroom && driving) return;
        showroom = !showroom;
        touchedAt = SystemClock.elapsedRealtime();
        stage.showroom(showroom);
        for (View v : hud) v.animate().alpha(showroom ? 0f : 1f).setStartDelay(0).setDuration(700).start();
        rail.animate().alpha(showroom ? 0f : 1f).setDuration(700).start();
        railLine.animate().alpha(showroom ? 0f : 1f).setDuration(700).start();
        stage.animate().scaleX(showroom ? 1.16f : 1f).scaleY(showroom ? 1.16f : 1f)
                .translationY(showroom ? G.dp(this, 26) : 0).setDuration(1600)
                .setInterpolator(new DecelerateInterpolator(2f)).start();
        caption.animate().alpha(showroom ? 1f : 0f).setStartDelay(showroom ? 900 : 0).setDuration(900).start();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        touchedAt = SystemClock.elapsedRealtime();
        if (showroom) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) toggleShowroom();
            return true;
        }
        return super.dispatchTouchEvent(e);
    }

    private void drive(boolean on) {
        driving = on;
        scenery.setDriving(on);
        if (showroom) toggleShowroom();
        if (on) {
            road.setVisibility(View.VISIBLE);
            road.run(true);
            road.animate().alpha(1f).setDuration(900).withEndAction(null).start();
            stage.animate().alpha(0f).setDuration(700).start();
        } else {
            road.animate().alpha(0f).setDuration(700).withEndAction(() -> { road.run(false); road.setVisibility(View.GONE); }).start();
            stage.animate().alpha(1f).setDuration(900).start();
        }
    }

    // ======================= Intro =======================

    private FrameLayout buildIntro() {
        FrameLayout f = new FrameLayout(this);
        f.setBackgroundColor(G.VOID);
        f.setClickable(true);
        LinearLayout c = G.column(this);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        Emblem e = new Emblem(this);
        e.setTag("e");
        c.addView(e, new LinearLayout.LayoutParams(G.dp(this, 112), G.dp(this, 112)));
        TextView w1 = G.label(this, "GHOST GOLD");
        w1.setTag("w1");
        w1.setTextSize(30);
        w1.setLetterSpacing(0.42f);
        w1.setTextColor(G.GOLD);
        w1.setShadowLayer(G.dp(this, 16), 0, 0, 0x73D4AF37);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(-2, -2);
        p1.topMargin = G.dp(this, 30);
        c.addView(w1, p1);
        View line = new View(this);
        line.setTag("line");
        line.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0, G.GOLD, G.IGNITION, G.GOLD, 0}));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(G.dp(this, 520), G.dp(this, 1));
        lp.setMargins(0, G.dp(this, 22), 0, G.dp(this, 22));
        c.addView(line, lp);
        TextView w2 = G.label(this, "System online");
        w2.setTag("w2");
        w2.setTextSize(11);
        w2.setTextColor(G.TEXT);
        c.addView(w2);
        TextView w3 = G.label(this, "Vehicle ready");
        w3.setTag("w3");
        w3.setTextSize(11);
        w3.setTextColor(G.IGNITION);
        w3.setPadding(0, G.dp(this, 8), 0, 0);
        c.addView(w3);
        f.addView(c, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        return f;
    }

    /** Seal draws, name lights up, then the truck emerges and flashes its headlights twice. */
    private void playIntro() {
        introAt = SystemClock.elapsedRealtime();
        Chime.play(this);
        if (showroom) toggleShowroom();
        Emblem e = intro.findViewWithTag("e");
        View w1 = intro.findViewWithTag("w1"), line = intro.findViewWithTag("line"),
                w2 = intro.findViewWithTag("w2"), w3 = intro.findViewWithTag("w3");
        DecelerateInterpolator ease = new DecelerateInterpolator(2f);
        h.removeCallbacksAndMessages("intro");
        intro.animate().cancel();
        intro.setAlpha(1f);
        intro.setVisibility(View.VISIBLE);
        e.setAlpha(0f); e.setDrawn(0f); e.setGlint(-1f);
        w1.setAlpha(0f); w1.setScaleX(1.08f); w1.setScaleY(1.08f);
        line.setScaleX(0f);
        w2.setAlpha(0f); w2.setTranslationY(G.dp(this, 8));
        w3.setAlpha(0f); w3.setTranslationY(G.dp(this, 8));
        stageBox.setAlpha(0f);

        e.animate().alpha(1f).setStartDelay(200).setDuration(900).setInterpolator(ease).start();
        ValueAnimator ring = ValueAnimator.ofFloat(0f, 1f).setDuration(1200);
        ring.setStartDelay(200);
        ring.setInterpolator(ease);
        ring.addUpdateListener(a -> e.setDrawn((float) a.getAnimatedValue()));
        ring.start();
        ValueAnimator glint = ValueAnimator.ofFloat(0f, 1f).setDuration(900);
        glint.setStartDelay(1400);
        glint.addUpdateListener(a -> e.setGlint((float) a.getAnimatedValue()));
        glint.start();
        w1.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(1100).setDuration(1100).setInterpolator(ease).start();
        line.animate().scaleX(1f).setStartDelay(1500).setDuration(1000).setInterpolator(ease).start();
        w2.animate().alpha(1f).translationY(0).setStartDelay(2400).setDuration(500).start();
        w3.animate().alpha(1f).translationY(0).setStartDelay(3000).setDuration(500).start();

        h.postAtTime(() -> {
            intro.animate().alpha(0f).setStartDelay(0).setDuration(900).withEndAction(() -> intro.setVisibility(View.GONE)).start();
            stageBox.setAlpha(1f);
            View truck = driving ? road : stage;
            truck.setAlpha(0f);
            truck.setTranslationY(G.dp(this, 24));
            truck.animate().alpha(1f).translationY(0).setStartDelay(300).setDuration(2000).setInterpolator(ease).start();
            for (View v : hud) { v.setAlpha(0f); v.animate().alpha(1f).setStartDelay(1800).setDuration(1000).start(); }
            if (!driving) flashHeadlights();
        }, "intro", SystemClock.uptimeMillis() + 4300);
    }

    /** Two quick headlight flashes as the truck appears (the "lights on" photo). */
    private void flashHeadlights() {
        boolean on = Head.headlights(this);
        ValueAnimator a = ValueAnimator.ofFloat(0, 1, 0, 1, on ? 1 : 0).setDuration(1400);
        a.setStartDelay(1500);
        a.addUpdateListener(x -> stage.setHeadlights((float) x.getAnimatedValue()));
        a.start();
    }

    private final BroadcastReceiver events = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            if (Head.ILL.equals(i.getAction())) {
                boolean on = i.getByteExtra("extra_ill_state", (byte) 0) != 0;
                ValueAnimator a = ValueAnimator.ofFloat(stage.headlights(), on ? 1 : 0).setDuration(600);
                a.addUpdateListener(x -> stage.setHeadlights((float) x.getAnimatedValue()));
                a.start();
                h.postDelayed(() -> { if ((Head.headlights(HomeActivity.this) || isNightHour()) != night) recreate(); }, 1500);
            } else if (SystemClock.elapsedRealtime() - introAt > 20_000) {
                playIntro();
            }
        }
    };

    // ======================= Refresh =======================

    private void refresh() {
        greet.setText(Store.greeting(this));
        showRestriction();
        showStatus();
        odo.setText(Car.odoSet(this) ? Car.fmt(Car.km(this)) + " km" : "Configurar");
        loadWeather(false);
    }

    private final Runnable second = new Runnable() {
        public void run() {
            tick++;
            int v = kmh();
            speed.setText(String.valueOf(v));
            tyreMap.invalidate();
            tyresLabel.setText(Tyres.known() ? "LLANTAS · TPMS" : "LLANTAS · TOCA PARA VER");
            if (road.ready()) {
                road.speed(v);
                scenery.setSpeed(v);
                if (v >= 8) { fast++; slow = 0; } else { slow++; fast = 0; }
                if (!driving && fast >= 3) drive(true);
                else if (driving && slow >= 8) drive(false);
            }
            long now = SystemClock.elapsedRealtime();
            if (!showroom && !driving && v == 0 && now - touchedAt > 120_000 && intro.getVisibility() != View.VISIBLE) toggleShowroom();
            if (tick % 6 == 0) showStatus();
            if (tick % 60 == 0) {
                refresh();
                if ((Head.headlights(HomeActivity.this) || isNightHour()) != night) { recreate(); return; }
            }
            if (Head.isNwd(HomeActivity.this) && tick % 2 == 0) showRadio();
            h.postDelayed(this, 1000);
        }
    };

    // ======================= Apps =======================

    private void maps() {
        String pkg = Store.maps(this);
        if (!Head.openApp(this, pkg) && !Head.openApp(this, Store.WAZE) && !Head.openApp(this, Store.GMAPS))
            toast("No hay app de mapas");
    }

    private void chooseMaps() {
        String[] n = {"Waze", "Google Maps"};
        String[] p = {Store.WAZE, Store.GMAPS};
        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("App de mapas")
                .setSingleChoiceItems(n, Store.GMAPS.equals(Store.maps(this)) ? 1 : 0,
                        (d, w) -> { Store.setMaps(this, p[w]); d.dismiss(); maps(); })
                .show();
    }

    /** Android Auto through ZLink, with the GHOST GOLD panel floating on top. */
    private void androidAuto() {
        if (!Head.openApp(this, Head.ZLINK)) { toast("No se encontró ZLink"); return; }
        if (!android.provider.Settings.canDrawOverlays(this)) toast("Activa \"Mostrar encima\" en Ajustes para ver GHOST GOLD sobre Android Auto");
        h.postDelayed(() -> Panel.show(this), 1200);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    // ======================= Lifecycle =======================

    @Override
    protected void onResume() {
        super.onResume();
        if ((Head.headlights(this) || isNightHour()) != night) { recreate(); return; }
        refresh();
        showWeather(Weather.cached(this));
        stage.setHeadlights(Head.headlights(this) ? 1 : 0);
        IntentFilter f = new IntentFilter(Head.ILL);
        f.addAction(CarService.WAKE);
        registerReceiver(events, f);
        if (CarService.wokeAt > introAt && SystemClock.elapsedRealtime() - CarService.wokeAt < 60_000) playIntro();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        media.add(this);
        media.attach();
        onMedia();
        if (driving) road.run(true);
        h.post(second);
        Panel.hide(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(events);
        media.remove(this);
        road.run(false);
        h.removeCallbacks(second);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] p, int[] r) {
        if (r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) {
            CarService.start(this);
            loadWeather(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        h.removeCallbacksAndMessages(null);
        tuner.unbind(this);
    }

    @Override
    public void onBackPressed() {
        if (showroom) toggleShowroom();
    }
}
