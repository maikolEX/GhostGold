package com.maikolex.ghostgold;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Top view of the truck with the pressure of each tyre beside it (PSI); amber when low or high. */
public class TyreMap extends View {

    private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG), tyre = new Paint(Paint.ANTI_ALIAS_FLAG),
            num = new Paint(Paint.ANTI_ALIAS_FLAG), unit = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    public TyreMap(Context c) {
        super(c);
        body.setStyle(Paint.Style.STROKE);
        body.setStrokeWidth(G.dp(c, 1.2f));
        body.setColor(0x99D4AF37);
        num.setTypeface(G.light(c));
        num.setTextSize(G.dp(c, 22));
        unit.setTypeface(G.wide(c));
        unit.setTextSize(G.dp(c, 7));
        unit.setColor(G.MUTED);
        unit.setLetterSpacing(0.2f);
    }

    @Override
    protected void onDraw(Canvas c) {
        float d = G.dp(getContext(), 1), w = getWidth(), h = getHeight();
        float cw = 34 * d, ch = h - 8 * d, cx = w / 2f, top = 4 * d;
        // Body: cab and bed.
        r.set(cx - cw / 2, top, cx + cw / 2, top + ch);
        c.drawRoundRect(r, 9 * d, 9 * d, body);
        c.drawLine(cx - cw / 2 + 4 * d, top + ch * 0.48f, cx + cw / 2 - 4 * d, top + ch * 0.48f, body);
        float[][] pos = {{-1, 0.2f}, {1, 0.2f}, {-1, 0.8f}, {1, 0.8f}};
        for (int i = 0; i < 4; i++) {
            boolean warn = Tyres.level(i) > 0;
            tyre.setColor(warn ? G.AMBER : Tyres.known() ? G.GOLD : 0x66D4AF37);
            float tx = cx + pos[i][0] * (cw / 2 + 2 * d), ty = top + ch * pos[i][1];
            r.set(tx - 3.5f * d, ty - 8 * d, tx + 3.5f * d, ty + 8 * d);
            c.drawRoundRect(r, 2 * d, 2 * d, tyre);
            num.setColor(warn ? G.AMBER : Tyres.known() ? G.WHITE : G.DIM);
            String v = Tyres.value(i);
            boolean left = pos[i][0] < 0;
            num.setTextAlign(left ? Paint.Align.RIGHT : Paint.Align.LEFT);
            unit.setTextAlign(left ? Paint.Align.RIGHT : Paint.Align.LEFT);
            float x = left ? tx - 12 * d : tx + 12 * d;
            c.drawText(v, x, ty + 4 * d, num);
            c.drawText("PSI", x, ty + 15 * d, unit);
        }
    }
}
