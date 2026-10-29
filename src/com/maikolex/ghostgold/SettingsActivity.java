package com.maikolex.ghostgold;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Driver, vehicle and the permissions GHOST GOLD needs, plus shortcuts to the radio's own settings. */
public class SettingsActivity extends Activity {

    private LinearLayout list;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = G.column(this);
        root.setPadding(G.dp(this, 28), G.dp(this, 18), G.dp(this, 28), G.dp(this, 12));
        root.setBackgroundColor(G.VOID);
        root.addView(G.header(this, "Ghost Gold · Sistema", "Ajustes"));
        ScrollView sv = new ScrollView(this);
        list = G.column(this);
        sv.addView(list);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 0, 1f);
        lp.topMargin = G.dp(this, 12);
        root.addView(sv, lp);
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        show();
    }

    private void show() {
        list.removeAllViews();
        section("Conductor y vehículo");
        row("Nombre", Store.name(this).isEmpty() ? "Sin nombre" : Store.name(this), this::askName);
        row("Placa termina en", Store.plate(this) + " · restricción los " + Rules.dayName(this), this::askPlate);
        row("App de mapas", Store.GMAPS.equals(Store.maps(this)) ? "Google Maps" : "Waze", () -> {
            Store.setMaps(this, Store.GMAPS.equals(Store.maps(this)) ? Store.WAZE : Store.GMAPS);
            show();
        });
        row("Kilometraje", Car.odoSet(this) ? Car.fmt(Car.km(this)) + " km" : "Poner kilometraje",
                () -> startActivity(new Intent(this, GarageActivity.class)));

        section("Permisos");
        check("Pantalla de inicio", isHome(), "Elige GHOST GOLD como inicio",
                () -> open(new Intent(Settings.ACTION_HOME_SETTINGS)));
        check("Ubicación", checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED,
                "Velocidad, kilometraje y clima", () -> requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1));
        check("Ver la música", Media.allowed(this), "Spotify, YouTube y Android Auto en el inicio",
                () -> open(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));
        check("Mostrar encima", Settings.canDrawOverlays(this), "Volumen dorado y panel sobre Android Auto",
                () -> open(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        check("Panel automático en Android Auto", Panel.usageAllowed(this), "Aparece solo al abrir ZLink",
                () -> open(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));

        section("Radio");
        row("Sonido y ecualizador", "Ajustes de audio del radio", () -> Head.open(this, Head.EQ));
        row("Presión de llantas", "Pantalla TPMS de la caja CAN", () -> Head.open(this, Head.TPMS));
        row("Ajustes del radio", "Pantalla, Bluetooth, sistema", () -> {
            if (!Head.open(this, Head.SYSTEM_SETTINGS)) open(new Intent(Settings.ACTION_SETTINGS));
        });
        row("Ver resumen de viaje", "Lo que sale al apagar el carro", () -> startActivity(new Intent(this, ByeActivity.class)));
    }

    private void section(String s) {
        TextView t = G.label(this, s);
        t.setTextColor(G.GOLD);
        t.setPadding(0, G.dp(this, 20), 0, G.dp(this, 6));
        list.addView(t);
    }

    private void row(String title, String value, Runnable tap) {
        LinearLayout r = G.row(this);
        r.setMinimumHeight(G.dp(this, 64));
        LinearLayout t = G.column(this);
        t.addView(G.text(this, title, 18, G.TEXT));
        TextView v = G.text(this, value, 14, G.MUTED);
        v.setPadding(0, G.dp(this, 4), 0, 0);
        t.addView(v);
        r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        G.press(r);
        r.setOnClickListener(x -> tap.run());
        list.addView(r);
        divider();
    }

    private void check(String title, boolean ok, String why, Runnable fix) {
        LinearLayout r = G.row(this);
        r.setMinimumHeight(G.dp(this, 64));
        View dot = new View(this);
        dot.setBackground(G.round(ok ? G.GOLD : 0, ok ? 0 : G.AMBER, G.dp(this, 2)));
        r.addView(dot, new LinearLayout.LayoutParams(G.dp(this, 12), G.dp(this, 12)));
        LinearLayout t = G.column(this);
        t.setPadding(G.dp(this, 16), 0, 0, 0);
        t.addView(G.text(this, title, 18, G.TEXT));
        TextView w = G.text(this, ok ? "Activado · " + why : why, 14, ok ? G.MUTED : G.AMBER);
        w.setPadding(0, G.dp(this, 4), 0, 0);
        t.addView(w);
        r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        if (!ok) r.addView(G.chip(this, "Activar", fix));
        list.addView(r);
        divider();
    }

    private void divider() {
        View d = new View(this);
        d.setBackgroundColor(G.LINE);
        list.addView(d, new LinearLayout.LayoutParams(-1, G.dp(this, 1)));
    }

    private boolean isHome() {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        android.content.pm.ResolveInfo r = getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        return r != null && r.activityInfo != null && getPackageName().equals(r.activityInfo.packageName);
    }

    private void open(Intent i) {
        try { startActivity(i); } catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
    }

    private void askName() {
        EditText in = new EditText(this);
        in.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        in.setText(Store.name(this));
        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle("Tu nombre").setView(in)
                .setPositiveButton("Guardar", (d, w) -> { Store.setName(this, in.getText().toString()); show(); })
                .setNegativeButton("Cancelar", null).show();
    }

    private void askPlate() {
        String[] d = new String[10];
        for (int i = 0; i < 10; i++) d[i] = "Termina en " + i;
        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle("Último número de la placa")
                .setSingleChoiceItems(d, Store.plate(this), (dlg, w) -> { Store.setPlate(this, w); dlg.dismiss(); show(); })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int c, String[] p, int[] r) {
        CarService.start(this);
        show();
    }
}
