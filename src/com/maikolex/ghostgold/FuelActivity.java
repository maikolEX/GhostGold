package com.maikolex.ghostgold;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Fuel log: real consumption between full tanks, money spent this month and every fill-up. */
public class FuelActivity extends Activity {

    private LinearLayout stats, list;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = G.column(this);
        root.setPadding(G.dp(this, 28), G.dp(this, 18), G.dp(this, 28), G.dp(this, 12));
        root.setBackgroundColor(G.VOID);
        LinearLayout head = G.header(this, "Ghost Gold · Vehículo", "Combustible");
        head.addView(G.chip(this, "Registrar carga", this::askFill));
        root.addView(head);
        stats = G.row(this);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = G.dp(this, 20);
        root.addView(stats, sp);
        TextView k = G.label(this, "Cargas · mantén presionada una para borrarla");
        k.setPadding(0, G.dp(this, 22), 0, G.dp(this, 4));
        root.addView(k);
        ScrollView sv = new ScrollView(this);
        list = G.column(this);
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        show();
    }

    private void show() {
        float[] e = Fuel.economy(this);
        List<Fuel.Fill> all = Fuel.all(this);
        Fuel.Fill last = all.isEmpty() ? null : all.get(all.size() - 1);
        stats.removeAllViews();
        stat("Rendimiento", e[1] > 0 ? String.format(Locale.US, "%.1f", e[1]) : "--", "km/L promedio", true);
        stat("Último tanque", e[0] > 0 ? String.format(Locale.US, "%.1f", e[0]) : "--", "km/L", false);
        stat("Consumo", e[1] > 0 ? String.format(Locale.US, "%.1f", 100f / e[1]) : String.format(Locale.US, "%.1f", Car.L_PER_100),
                e[1] > 0 ? "L/100 km" : "L/100 km (caja CAN)", false);
        stat("Este mes", "₡" + Car.fmt(Fuel.monthSpend(this)), "en combustible", false);
        stat("Último litro", last == null ? "--" : "₡" + Car.fmt(Math.round(last.pricePerLitre())), "precio", false);

        list.removeAllViews();
        if (all.isEmpty()) {
            TextView t = G.text(this, "Registra tus cargas con \"Tanque lleno\" marcado: desde la segunda verás tu rendimiento real.", 15, G.DIM);
            t.setSingleLine(false);
            t.setPadding(0, G.dp(this, 14), 0, 0);
            list.addView(t);
            return;
        }
        SimpleDateFormat df = new SimpleDateFormat("d MMM yyyy", new Locale("es", "CR"));
        for (int i = all.size() - 1; i >= 0; i--) {
            Fuel.Fill f = all.get(i);
            LinearLayout r = G.row(this);
            r.setMinimumHeight(G.dp(this, 64));
            LinearLayout t = G.column(this);
            t.addView(G.text(this, String.format(Locale.US, "%.1f L · ₡%s", f.litres, Car.fmt(f.colones)), 19, G.TEXT));
            TextView s = G.text(this, df.format(new Date(f.time)) + " · " + Car.fmt(f.km) + " km" + (f.full ? " · tanque lleno" : ""), 14, G.MUTED);
            s.setPadding(0, G.dp(this, 4), 0, 0);
            t.addView(s);
            r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView ppl = G.label(this, "₡" + Car.fmt(Math.round(f.pricePerLitre())) + " / L");
            ppl.setTextColor(G.CHAMPAGNE);
            r.addView(ppl);
            r.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle("¿Borrar esta carga?")
                        .setPositiveButton("Borrar", (d, w) -> { Fuel.remove(this, f); show(); })
                        .setNegativeButton("Cancelar", null).show();
                return true;
            });
            list.addView(r);
            View line = new View(this);
            line.setBackgroundColor(G.LINE);
            list.addView(line, new LinearLayout.LayoutParams(-1, G.dp(this, 1)));
        }
    }

    private void stat(String label, String value, String unit, boolean hero) {
        LinearLayout s = G.column(this);
        s.addView(G.label(this, label));
        TextView v = G.value(this, value, hero ? 52 : 34, hero ? G.IGNITION : G.WHITE);
        v.setPadding(0, G.dp(this, 8), 0, 0);
        s.addView(v);
        TextView u = G.label(this, unit);
        u.setTextSize(8);
        u.setPadding(0, G.dp(this, 6), 0, 0);
        s.addView(u);
        stats.addView(s, new LinearLayout.LayoutParams(0, -2, hero ? 1.3f : 1f));
    }

    private EditText field(LinearLayout box, String hint, String value, boolean decimal) {
        TextView l = G.label(this, hint);
        l.setPadding(0, G.dp(this, 12), 0, 0);
        box.addView(l);
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | (decimal ? InputType.TYPE_NUMBER_FLAG_DECIMAL : 0));
        e.setText(value);
        e.setTextSize(22);
        e.setGravity(Gravity.CENTER);
        e.setSelectAllOnFocus(true);
        box.addView(e);
        return e;
    }

    private void askFill() {
        LinearLayout box = G.column(this);
        box.setPadding(G.dp(this, 24), G.dp(this, 4), G.dp(this, 24), 0);
        EditText litres = field(box, "Litros", "", true);
        EditText money = field(box, "Colones pagados", "", false);
        EditText km = field(box, "Kilometraje del tablero", Car.odoSet(this) ? String.valueOf(Car.km(this)) : "", false);
        CheckBox full = new CheckBox(this);
        full.setText("Tanque lleno");
        full.setTextColor(G.TEXT);
        full.setChecked(true);
        box.addView(full);
        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle("Nueva carga").setView(box)
                .setPositiveButton("Guardar", (d, w) -> {
                    try {
                        float l = Float.parseFloat(litres.getText().toString().replace(',', '.'));
                        int crc = Integer.parseInt(money.getText().toString().replace(".", "").trim());
                        int k = Integer.parseInt(km.getText().toString().replace(".", "").trim());
                        Fuel.add(this, new Fuel.Fill(System.currentTimeMillis(), k, l, crc, full.isChecked()));
                        show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Revisa los números", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null).show();
    }
}
