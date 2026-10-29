package com.maikolex.ghostgold;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

/** Maintenance log: odometer, every service with what is left, and a "done" button for each. */
public class GarageActivity extends Activity {

    private LinearLayout list;
    private TextView odo;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = G.column(this);
        root.setPadding(G.dp(this, 28), G.dp(this, 18), G.dp(this, 28), G.dp(this, 12));
        root.setBackgroundColor(G.VOID);
        LinearLayout head = G.header(this, "Ghost Gold · Vehículo", "Mantenimiento");
        odo = G.text(this, "", 16, G.TEXT);
        odo.setPadding(G.dp(this, 16), G.dp(this, 11), G.dp(this, 16), G.dp(this, 11));
        odo.setBackground(G.pill(this, G.TINT, G.HAIR));
        G.press(odo);
        odo.setOnClickListener(v -> ask("Kilometraje del tablero", Car.km(this), km -> Car.setKm(this, km)));
        head.addView(odo);
        root.addView(head);
        ScrollView sv = new ScrollView(this);
        list = G.column(this);
        sv.addView(list);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 0, 1f);
        lp.topMargin = G.dp(this, 14);
        root.addView(sv, lp);
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        show();
    }

    private void show() {
        odo.setText(Car.odoSet(this) ? "Tablero ≈ " + Car.fmt(Car.km(this)) + " km" : "Poner kilometraje");
        list.removeAllViews();
        for (Garage.Job j : Garage.JOBS) list.addView(row(j));
    }

    private View row(Garage.Job j) {
        int lv = Garage.level(this, j);
        LinearLayout r = G.row(this);
        r.setMinimumHeight(G.dp(this, 76));
        r.setPadding(G.dp(this, 4), G.dp(this, 10), G.dp(this, 4), G.dp(this, 10));
        View dot = new View(this);
        dot.setBackground(G.round(lv == 2 ? G.AMBER : lv == 1 ? G.CHAMPAGNE : G.GOLD, 0, 0));
        r.addView(dot, new LinearLayout.LayoutParams(G.dp(this, 10), G.dp(this, 10)));
        LinearLayout t = G.column(this);
        t.setPadding(G.dp(this, 18), 0, G.dp(this, 12), 0);
        t.addView(G.text(this, j.name, 19, G.TEXT));
        TextView s = G.text(this, Garage.status(this, j), 14, lv > 0 ? G.AMBER : G.MUTED);
        s.setSingleLine(false);
        s.setMaxLines(2);
        s.setPadding(0, G.dp(this, 4), 0, 0);
        t.addView(s);
        r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        r.addView(G.chip(this, Garage.MARCHAMO.equals(j.id) ? "Pagado" : "Hecho hoy", () -> {
            if (!j.yearly() && !Car.odoSet(this)) { toast("Primero pon el kilometraje del tablero"); return; }
            Garage.done(this, j);
            toast("Registrado: " + j.name);
            show();
        }));
        G.press(r);
        r.setOnClickListener(v -> edit(j));
        LinearLayout w = G.column(this);
        w.addView(r);
        View line = new View(this);
        line.setBackgroundColor(G.LINE);
        w.addView(line, new LinearLayout.LayoutParams(-1, G.dp(this, 1)));
        return w;
    }

    private void edit(Garage.Job j) {
        int y = Calendar.getInstance().get(Calendar.YEAR);
        if (j.yearly()) {
            boolean m = Garage.MARCHAMO.equals(j.id);
            String[] o = m ? new String[]{"Tengo pagado el de " + y, "Tengo pagado el de " + (y + 1)}
                    : new String[]{"La pasé en " + (y - 1), "La pasé en " + y};
            int[] v = m ? new int[]{y, y + 1} : new int[]{y - 1, y};
            new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle(j.name)
                    .setItems(o, (d, w) -> { Garage.setYear(this, j, v[w]); show(); })
                    .setNegativeButton("Cancelar", null).show();
            return;
        }
        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle(j.name)
                .setItems(new String[]{"El último fue a los… km", "Hacerlo cada… km"}, (d, w) -> {
                    if (w == 0) ask("Kilometraje del último servicio", Garage.last(this, j), km -> Garage.setLast(this, j, km));
                    else ask("Cada cuántos km", Garage.every(this, j), km -> Garage.setEvery(this, j, km));
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private interface Km { void set(int km); }

    private void ask(String title, int value, Km sink) {
        EditText in = new EditText(this);
        in.setInputType(InputType.TYPE_CLASS_NUMBER);
        in.setText(String.valueOf(value));
        in.setSelectAllOnFocus(true);
        in.setGravity(Gravity.CENTER);
        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle(title).setView(in)
                .setPositiveButton("Guardar", (d, w) -> {
                    try { sink.set(Integer.parseInt(in.getText().toString().trim())); show(); }
                    catch (NumberFormatException ignored) { }
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
