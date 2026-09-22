package com.drabdie.tweak;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared programmatic UI helpers - keeps the original DraB look and feel. */
public final class Ui {

    public static final int
            BG = Color.rgb(11, 13, 20),
            CARD = Color.rgb(22, 25, 36),
            CARD_ACTIVE = Color.rgb(40, 43, 67),
            HERO = Color.rgb(37, 40, 67),
            TEXT = Color.rgb(245, 246, 255),
            MUTED = Color.rgb(155, 161, 184),
            ACCENT = Color.rgb(124, 131, 253),
            GREEN = Color.rgb(67, 211, 145),
            ORANGE = Color.rgb(255, 174, 79),
            RED = Color.rgb(255, 107, 107);

    public static int dp(Activity a, float v) {
        return (int) (v * a.getResources().getDisplayMetrics().density + .5f);
    }

    public static TextView tv(Activity a, String s, float size, int color) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    public static GradientDrawable bg(Activity a, int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(a, radius));
        return g;
    }

    public static LinearLayout row(Activity a) {
        LinearLayout l = new LinearLayout(a);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout col(Activity a) {
        LinearLayout l = new LinearLayout(a);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static TextView label(Activity a, String s) {
        TextView t = tv(a, s, 12, MUTED);
        t.setAllCaps(true);
        t.setLetterSpacing(.08f);
        return t;
    }

    /** Section header appended to a container. */
    public static TextView section(Activity a, LinearLayout content, String s) {
        TextView t = label(a, s);
        t.setPadding(0, dp(a, 14), 0, dp(a, 8));
        content.addView(t);
        return t;
    }

    /** Accent button with click handler. */
    public static TextView button(Activity a, String s, View.OnClickListener onClick) {
        TextView b = tv(a, s, 14, TEXT);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(a, 14), 0, dp(a, 14), 0);
        b.setBackground(bg(a, ACCENT, 14));
        b.setOnClickListener(onClick);
        return b;
    }

    /** Muted outline button. */
    public static TextView ghostButton(Activity a, String s, View.OnClickListener onClick) {
        TextView b = tv(a, s, 13, MUTED);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(a, 12), 0, dp(a, 12), 0);
        GradientDrawable g = bg(a, CARD, 14);
        g.setStroke(dp(a, 1), Color.rgb(45, 49, 70));
        b.setBackground(g);
        b.setOnClickListener(onClick);
        return b;
    }

    /** Card container ready for children. */
    public static LinearLayout card(Activity a) {
        LinearLayout l = col(a);
        l.setPadding(dp(a, 16), dp(a, 13), dp(a, 16), dp(a, 13));
        l.setBackground(bg(a, CARD, 16));
        return l;
    }

    /** Info card: title / subtitle / caption. */
    public static View card(Activity a, String title, String sub, String caption, int captionColor) {
        LinearLayout l = card(a);
        TextView t = tv(a, title, 16, TEXT);
        t.setTypeface(null, 1);
        l.addView(t);
        l.addView(tv(a, sub, 12, MUTED));
        l.addView(tv(a, caption, 11, captionColor));
        return l;
    }

    private Ui() {}
}
