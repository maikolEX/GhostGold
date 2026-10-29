package com.maikolex.ghostgold;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * GHOST GOLD phone over the head unit's Bluetooth: connection status, favourites, recent
 * numbers and a gold dial pad. The factory phone app remains one tap away for the phonebook.
 */
public class PhoneActivity extends Activity {

    private final Handler h = new Handler();
    private Phone phone;
    private TextView link, number;
    private LinearLayout favs, recent;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        phone = Media.get(this).phone;
        LinearLayout root = G.column(this);
        root.setPadding(G.dp(this, 28), G.dp(this, 18), G.dp(this, 28), G.dp(this, 14));
        root.setBackgroundColor(G.VOID);
        LinearLayout head = G.header(this, "Ghost Gold · Bluetooth", "Teléfono");
        link = G.label(this, "Buscando teléfono…");
        link.setPadding(G.dp(this, 16), G.dp(this, 11), G.dp(this, 16), G.dp(this, 11));
        link.setBackground(G.pill(this, G.TINT, G.HAIR));
        head.addView(link);
        TextView book = G.chip(this, "Contactos", () -> {
            if (!Head.open(this, Head.FACTORY_PHONE)) toast("No se encontró el teléfono del radio");
        });
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-2, -2);
        bl.leftMargin = G.dp(this, 12);
        head.addView(book, bl);
        root.addView(head);

        LinearLayout body = new LinearLayout(this);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, 0, 1f);
        blp.topMargin = G.dp(this, 16);
        root.addView(body, blp);

        // Left: favourites + recent.
        ScrollView sv = new ScrollView(this);
        LinearLayout lists = G.column(this);
        lists.addView(G.label(this, "Favoritos"));
        favs = G.column(this);
        lists.addView(favs);
        TextView rl = G.label(this, "Recientes");
        rl.setPadding(0, G.dp(this, 20), 0, 0);
        lists.addView(rl);
        recent = G.column(this);
        lists.addView(recent);
        sv.addView(lists);
        body.addView(sv, new LinearLayout.LayoutParams(0, -1, 1f));

        // Right: dial pad.
        LinearLayout pad = G.column(this);
        pad.setGravity(Gravity.CENTER_HORIZONTAL);
        pad.setPadding(G.dp(this, 24), 0, 0, 0);
        LinearLayout disp = G.row(this);
        number = G.value(this, "", 40, G.WHITE);
        number.setGravity(Gravity.CENTER);
        disp.addView(number, new LinearLayout.LayoutParams(0, -2, 1f));
        disp.addView(G.button(this, Glyph.BACKSPACE, 48, false, () -> {
            String s = number.getText().toString();
            if (!s.isEmpty()) number.setText(s.substring(0, s.length() - 1));
        }));
        pad.addView(disp, new LinearLayout.LayoutParams(G.dp(this, 420), G.dp(this, 64)));
        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};
        for (int r = 0; r < 4; r++) {
            LinearLayout line = G.row(this);
            for (int k = 0; k < 3; k++) {
                String d = keys[r * 3 + k];
                TextView key = G.value(this, d, 30, G.WHITE);
                key.setGravity(Gravity.CENTER);
                key.setBackground(G.round(0, G.HAIR, G.dp(this, 1)));
                G.press(key);
                key.setOnClickListener(v -> number.append(d));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(G.dp(this, 76), G.dp(this, 76));
                lp.setMargins(G.dp(this, 14), G.dp(this, 6), G.dp(this, 14), G.dp(this, 6));
                line.addView(key, lp);
            }
            pad.addView(line);
        }
        View call = G.button(this, Glyph.CALL, 72, true, this::callTyped);
        call.setOnLongClickListener(v -> { saveFavourite(number.getText().toString()); return true; });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(G.dp(this, 72), G.dp(this, 72));
        clp.topMargin = G.dp(this, 10);
        pad.addView(call, clp);
        TextView hint = G.label(this, "Mantén presionado para guardar en favoritos");
        hint.setTextSize(8);
        hint.setPadding(0, G.dp(this, 8), 0, 0);
        pad.addView(hint);
        body.addView(pad);
        setContentView(root);
    }

    // ---------- Calls ----------

    private void callTyped() {
        call(number.getText().toString().trim());
    }

    private void call(String n) {
        if (n.isEmpty()) return;
        if (!phone.connected()) { toast("Conecta el teléfono por Bluetooth"); return; }
        if (phone.dial(n)) {
            remember(n);
            // The factory phone screen handles the call itself (audio, hang up).
            h.postDelayed(() -> Head.open(this, Head.FACTORY_PHONE), 600);
        } else toast("No se pudo marcar");
    }

    // ---------- Lists ----------

    private SharedPreferences p() { return getSharedPreferences("phone", MODE_PRIVATE); }

    private List<String[]> read(String key) {
        List<String[]> l = new ArrayList<>();
        for (String e : p().getString(key, "").split(";")) {
            if (e.isEmpty()) continue;
            String[] parts = e.split("\\|", 2);
            l.add(parts.length == 2 ? parts : new String[]{"", parts[0]});
        }
        return l;
    }

    private void write(String key, List<String[]> l) {
        StringBuilder s = new StringBuilder();
        for (String[] e : l) s.append(e[0].replace("|", " ").replace(";", " ")).append('|').append(e[1]).append(';');
        p().edit().putString(key, s.toString()).apply();
    }

    private void remember(String n) {
        List<String[]> l = read("recent");
        for (int i = l.size() - 1; i >= 0; i--) if (l.get(i)[1].equals(n)) l.remove(i);
        l.add(0, new String[]{"", n});
        while (l.size() > 8) l.remove(l.size() - 1);
        write("recent", l);
        showLists();
    }

    private void saveFavourite(String n) {
        if (n.isEmpty()) { toast("Escribe primero el número"); return; }
        EditText in = new EditText(this);
        in.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        in.setHint("Nombre");
        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Guardar " + n)
                .setView(in)
                .setPositiveButton("Guardar", (d, w) -> {
                    List<String[]> l = read("favs");
                    l.add(new String[]{in.getText().toString().trim(), n});
                    write("favs", l);
                    showLists();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showLists() {
        fill(favs, read("favs"), true);
        fill(recent, read("recent"), false);
    }

    private void fill(LinearLayout box, List<String[]> l, boolean fav) {
        box.removeAllViews();
        if (l.isEmpty()) {
            TextView e = G.text(this, fav ? "Marca un número y mantén presionado el botón dorado" : "Sin llamadas todavía", 14, G.DIM);
            e.setSingleLine(false);
            e.setPadding(0, G.dp(this, 10), 0, 0);
            box.addView(e);
            return;
        }
        for (String[] e : l) {
            LinearLayout r = G.row(this);
            r.setPadding(0, G.dp(this, 12), 0, G.dp(this, 12));
            LinearLayout t = G.column(this);
            t.addView(G.text(this, e[0].isEmpty() ? e[1] : e[0], 19, G.TEXT));
            if (!e[0].isEmpty()) {
                TextView n = G.text(this, e[1], 13, G.MUTED);
                n.setPadding(0, G.dp(this, 3), 0, 0);
                t.addView(n);
            }
            r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
            r.addView(G.button(this, Glyph.CALL, 44, false, () -> call(e[1])));
            if (fav) {
                r.setOnLongClickListener(v -> {
                    List<String[]> all = read("favs");
                    for (int i = 0; i < all.size(); i++) if (all.get(i)[1].equals(e[1])) { all.remove(i); break; }
                    write("favs", all);
                    showLists();
                    toast("Quitado de favoritos");
                    return true;
                });
            }
            box.addView(r);
            View line = new View(this);
            line.setBackgroundColor(G.LINE);
            box.addView(line, new LinearLayout.LayoutParams(-1, G.dp(this, 1)));
        }
    }

    private final Runnable poll = new Runnable() {
        public void run() {
            String n = phone.phoneName();
            boolean on = phone.connected();
            link.setText((on ? "● " + (n == null ? "Teléfono conectado" : n) : "Sin teléfono").toUpperCase());
            link.setTextColor(on ? G.IGNITION : G.MUTED);
            h.postDelayed(this, 3000);
        }
    };

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    @Override protected void onResume() { super.onResume(); showLists(); h.post(poll); }
    @Override protected void onPause() { super.onPause(); h.removeCallbacks(poll); }
}
