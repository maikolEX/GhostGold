package com.maikolex.ghostgold;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

/** All apps, in a calm grid. Icons are slightly desaturated so the gold stays the only colour. */
public class AppsActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = G.column(this);
        root.setPadding(G.dp(this, 28), G.dp(this, 18), G.dp(this, 28), G.dp(this, 12));
        root.setBackgroundColor(G.VOID);
        root.addView(G.header(this, "Ghost Gold", "Aplicaciones"));

        PackageManager pm = getPackageManager();
        List<ResolveInfo> apps = pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        Collections.sort(apps, (x, y) -> x.loadLabel(pm).toString().compareToIgnoreCase(y.loadLabel(pm).toString()));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(7);
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0.55f);
        ColorMatrixColorFilter calm = new ColorMatrixColorFilter(cm);
        for (ResolveInfo ri : apps) {
            if (ri.activityInfo.packageName.equals(getPackageName())) continue;
            LinearLayout cell = G.column(this);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(G.dp(this, 6), G.dp(this, 14), G.dp(this, 6), G.dp(this, 14));
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(ri.loadIcon(pm));
            icon.setColorFilter(calm);
            cell.addView(icon, new LinearLayout.LayoutParams(G.dp(this, 64), G.dp(this, 64)));
            TextView name = G.text(this, ri.loadLabel(pm), 13, G.TEXT);
            name.setGravity(Gravity.CENTER);
            name.setPadding(0, G.dp(this, 8), 0, 0);
            cell.addView(name, new LinearLayout.LayoutParams(G.dp(this, 128), -2));
            G.press(cell);
            String pkg = ri.activityInfo.packageName, cls = ri.activityInfo.name;
            cell.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                            .setClassName(pkg, cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
                } catch (Exception ignored) { }
            });
            grid.addView(cell);
        }
        ScrollView sv = new ScrollView(this);
        sv.addView(grid);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 0, 1f);
        lp.topMargin = G.dp(this, 10);
        root.addView(sv, lp);
        setContentView(root);
    }
}
