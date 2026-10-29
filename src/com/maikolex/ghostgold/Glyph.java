package com.maikolex.ghostgold;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** GHOST GOLD line icons, drawn on a 24-unit grid with thin rounded strokes. */
final class Glyph extends Drawable {

    static final int BACK = 1, NAV = 2, MUSIC = 3, PHONE = 4, RADIO = 5, APPS = 6, CAR = 7, WRENCH = 8,
            SETTINGS = 9, MOON = 10, AUTO = 11, PREV = 12, NEXT = 13, PLAY = 14, PAUSE = 15, SOUND = 16,
            BACKSPACE = 17, HANGUP = 18, CALL = 19, SEEK_DOWN = 20, SEEK_UP = 21, STAR = 22, SUN = 23,
            CLOUD = 24, RAIN = 25, STORM = 26, FOG = 27, MINUS = 28, PLUS = 29, CLOSE = 30, TYRE = 31;

    private final int kind;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF r = new RectF();

    Glyph(Context c, int kind, int color) {
        this.kind = kind;
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    void setColor(int color) {
        p.setColor(color);
        invalidateSelf();
    }

    @Override
    public void draw(Canvas c) {
        float s = Math.min(getBounds().width(), getBounds().height()) / 24f;
        c.save();
        c.translate(getBounds().left + (getBounds().width() - 24 * s) / 2f, getBounds().top + (getBounds().height() - 24 * s) / 2f);
        c.scale(s, s);
        p.setStrokeWidth(1.6f);
        p.setStyle(Paint.Style.STROKE);
        path.reset();
        switch (kind) {
            case BACK: line(c, 15, 5, 8, 12); line(c, 8, 12, 15, 19); break;
            case NAV:
                c.drawCircle(12, 12, 9, p);
                path.moveTo(15.5f, 8.5f); path.lineTo(13, 13); path.lineTo(8.5f, 15.5f); path.lineTo(11, 11); path.close();
                c.drawPath(path, p);
                break;
            case MUSIC:
                line(c, 9, 17, 9, 5); line(c, 9, 5, 19, 3.5f); line(c, 19, 3.5f, 19, 15.5f);
                c.drawCircle(6.5f, 17.5f, 2.5f, p); c.drawCircle(16.5f, 15.5f, 2.5f, p);
                break;
            case PHONE: case CALL: case HANGUP:
                c.save();
                if (kind == HANGUP) c.rotate(135, 12, 12);
                path.moveTo(6.5f, 3.5f); path.lineTo(9, 3.5f); path.lineTo(10.5f, 7.5f); path.lineTo(8.6f, 9.2f);
                path.quadTo(10.5f, 13.5f, 14.8f, 15.4f); path.lineTo(16.5f, 13.5f); path.lineTo(20.5f, 15);
                path.lineTo(20.5f, 17.5f); path.quadTo(20.5f, 20.5f, 17.5f, 20.5f);
                path.quadTo(4, 19.5f, 3.5f, 6.5f); path.quadTo(3.5f, 3.5f, 6.5f, 3.5f); path.close();
                c.drawPath(path, p);
                c.restore();
                break;
            case RADIO:
                r.set(3, 8, 21, 20); c.drawRoundRect(r, 2, 2, p);
                line(c, 7, 8, 16, 3.5f);
                c.drawCircle(15.5f, 14, 2.8f, p); line(c, 6.5f, 12, 10, 12); line(c, 6.5f, 15, 10, 15); line(c, 6.5f, 18, 10, 18);
                break;
            case APPS:
                for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) {
                    r.set(4 + i * 6, 4 + j * 6, 8 + i * 6, 8 + j * 6); c.drawRoundRect(r, 1, 1, p);
                }
                break;
            case CAR:
                path.moveTo(3.5f, 15); path.lineTo(5, 10); path.lineTo(7.5f, 6.5f); path.lineTo(16.5f, 6.5f);
                path.lineTo(19, 10); path.lineTo(20.5f, 15); path.lineTo(20.5f, 18); path.lineTo(3.5f, 18); path.close();
                c.drawPath(path, p);
                line(c, 5, 10, 19, 10);
                c.drawCircle(7.5f, 14, 1.2f, p); c.drawCircle(16.5f, 14, 1.2f, p);
                line(c, 5.5f, 18, 5.5f, 20.5f); line(c, 18.5f, 18, 18.5f, 20.5f);
                break;
            case WRENCH:
                path.moveTo(14.5f, 3.8f); path.quadTo(19.5f, 3, 20.2f, 8.3f); path.lineTo(17.6f, 10.9f);
                path.lineTo(15.2f, 10.4f); path.lineTo(13.6f, 8.8f); path.lineTo(13.1f, 6.4f); path.close();
                c.drawPath(path, p);
                line(c, 14.4f, 9.6f, 4.8f, 19.2f);
                c.drawCircle(5.2f, 18.8f, 1.6f, p);
                break;
            case SETTINGS:
                c.drawCircle(12, 12, 3.2f, p);
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    line(c, 12 + (float) Math.cos(a) * 6.2f, 12 + (float) Math.sin(a) * 6.2f,
                            12 + (float) Math.cos(a) * 8.8f, 12 + (float) Math.sin(a) * 8.8f);
                }
                c.drawCircle(12, 12, 6.2f, p);
                break;
            case MOON:
                path.moveTo(15, 3.6f); path.quadTo(8.5f, 5, 8.5f, 12); path.quadTo(8.5f, 19, 15, 20.4f);
                path.quadTo(5, 22.5f, 3.6f, 12); path.quadTo(5, 1.5f, 15, 3.6f);
                c.save(); c.translate(3, 0); c.drawPath(path, p); c.restore();
                break;
            case AUTO:
                r.set(3, 6, 15, 18); c.drawRoundRect(r, 2, 2, p);
                line(c, 6, 15, 12, 15);
                r.set(16.5f, 4, 21, 13); c.drawRoundRect(r, 1.2f, 1.2f, p);
                line(c, 18, 11.2f, 19.5f, 11.2f);
                line(c, 9, 18, 9, 20.5f); line(c, 6, 20.5f, 12, 20.5f);
                break;
            case PREV: fillTri(c, 18, 6, 18, 18, 9, 12); line(c, 6.5f, 6, 6.5f, 18); break;
            case NEXT: fillTri(c, 6, 6, 6, 18, 15, 12); line(c, 17.5f, 6, 17.5f, 18); break;
            case PLAY: fillTri(c, 8, 5, 8, 19, 19, 12); break;
            case PAUSE:
                p.setStyle(Paint.Style.FILL);
                r.set(7, 5, 10, 19); c.drawRoundRect(r, 1, 1, p); r.set(14, 5, 17, 19); c.drawRoundRect(r, 1, 1, p);
                break;
            case SOUND:
                line(c, 5, 4, 5, 20); line(c, 12, 4, 12, 20); line(c, 19, 4, 19, 20);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(5, 14, 2.2f, p); c.drawCircle(12, 8, 2.2f, p); c.drawCircle(19, 16, 2.2f, p);
                break;
            case BACKSPACE:
                path.moveTo(8, 5); path.lineTo(20.5f, 5); path.lineTo(20.5f, 19); path.lineTo(8, 19); path.lineTo(2.5f, 12); path.close();
                c.drawPath(path, p);
                line(c, 11, 9, 17, 15); line(c, 17, 9, 11, 15);
                break;
            case SEEK_DOWN: line(c, 13, 6, 7, 12); line(c, 7, 12, 13, 18); line(c, 18, 6, 12, 12); line(c, 12, 12, 18, 18); break;
            case SEEK_UP: line(c, 11, 6, 17, 12); line(c, 17, 12, 11, 18); line(c, 6, 6, 12, 12); line(c, 12, 12, 6, 18); break;
            case STAR:
                for (int i = 0; i < 5; i++) {
                    double a = Math.toRadians(-90 + i * 144);
                    float x = 12 + (float) Math.cos(a) * 9, y = 12.8f + (float) Math.sin(a) * 9;
                    if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
                }
                path.close(); c.drawPath(path, p);
                break;
            case SUN:
                c.drawCircle(12, 12, 4, p);
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    line(c, 12 + (float) Math.cos(a) * 6.8f, 12 + (float) Math.sin(a) * 6.8f,
                            12 + (float) Math.cos(a) * 9.2f, 12 + (float) Math.sin(a) * 9.2f);
                }
                break;
            case CLOUD: case RAIN: case STORM: case FOG:
                cloud(c, kind == FOG ? 0 : -2);
                if (kind == RAIN) { line(c, 8, 17.5f, 7, 20.5f); line(c, 12, 17.5f, 11, 20.5f); line(c, 16, 17.5f, 15, 20.5f); }
                if (kind == STORM) { line(c, 13, 16, 10.5f, 19); line(c, 10.5f, 19, 13.5f, 19); line(c, 13.5f, 19, 11, 22); }
                if (kind == FOG) { line(c, 4, 19, 20, 19); line(c, 6, 22, 18, 22); }
                break;
            case MINUS: line(c, 5, 12, 19, 12); break;
            case PLUS: line(c, 5, 12, 19, 12); line(c, 12, 5, 12, 19); break;
            case CLOSE: line(c, 6, 6, 18, 18); line(c, 18, 6, 6, 18); break;
            case TYRE: c.drawCircle(12, 12, 8.5f, p); c.drawCircle(12, 12, 4, p); break;
            default: c.drawCircle(12, 12, 8, p);
        }
        c.restore();
    }

    private void cloud(Canvas c, float dy) {
        path.reset();
        path.moveTo(7, 16 + dy);
        path.quadTo(3, 16 + dy, 3.5f, 12.5f + dy);
        path.quadTo(4, 9.5f + dy, 7.5f, 9.8f + dy);
        path.quadTo(9, 5 + dy, 13.5f, 5.5f + dy);
        path.quadTo(17.5f, 6 + dy, 18, 10 + dy);
        path.quadTo(21, 10.5f + dy, 20.8f, 13.4f + dy);
        path.quadTo(20.5f, 16 + dy, 17.5f, 16 + dy);
        path.close();
        c.drawPath(path, p);
    }

    private void line(Canvas c, float x1, float y1, float x2, float y2) {
        c.drawLine(x1, y1, x2, y2, p);
    }

    private void fillTri(Canvas c, float x1, float y1, float x2, float y2, float x3, float y3) {
        path.reset();
        path.moveTo(x1, y1); path.lineTo(x2, y2); path.lineTo(x3, y3); path.close();
        p.setStyle(Paint.Style.FILL);
        c.drawPath(path, p);
        p.setStyle(Paint.Style.STROKE);
    }

    @Override public void setAlpha(int a) { p.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter cf) { p.setColorFilter(cf); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    @Override public int getIntrinsicWidth() { return 48; }
    @Override public int getIntrinsicHeight() { return 48; }
}
