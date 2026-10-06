package com.drabdie.tweak;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * HELLBOOST Premium High-Tech Gaming UI Helpers.
 */
public final class Ui {

    // HELLBOOST High-Tech Dark Gaming Palette
    public static final int
            BG = Color.rgb(10, 11, 16),              // #0A0B10 Dark Carbon
            CARD = Color.rgb(20, 22, 34),            // #141622 Deep Tech Surface
            CARD_ACTIVE = Color.rgb(31, 34, 53),     // #1F2235 Active High-Tech Card
            HERO = Color.rgb(26, 16, 28),            // #1A101C Crimson Hero
            TEXT = Color.rgb(245, 246, 255),          // Pure Crisp White
            MUTED = Color.rgb(138, 145, 172),        // Tech Muted Gray
            ACCENT = Color.rgb(255, 42, 95),         // #FF2A5F HELLBOOST Crimson/Neon Red
            ACCENT_GLOW = Color.rgb(255, 82, 82),    // Neon Flare Red
            CYAN = Color.rgb(0, 229, 255),           // Cyber Cyan
            GREEN = Color.rgb(0, 230, 118),          // High Performance Green
            ORANGE = Color.rgb(255, 152, 0),         // Thermal Alert Orange
            RED = Color.rgb(255, 23, 68),            // Critical Warning Red
            GOLD = Color.rgb(255, 215, 0);           // Premium Gold

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

    public static GradientDrawable bgBorder(Activity a, int color, int borderColor, float radius, float borderWidth) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(a, radius));
        g.setStroke(dp(a, borderWidth), borderColor);
        return g;
    }

    public static GradientDrawable gradientBg(Activity a, int startColor, int endColor, float radius) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{startColor, endColor}
        );
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
        TextView t = tv(a, s, 11, MUTED);
        t.setAllCaps(true);
        t.setLetterSpacing(.10f);
        t.setTypeface(null, 1);
        return t;
    }

    public static TextView section(Activity a, LinearLayout content, String s) {
        TextView t = label(a, "⚡  " + s);
        t.setTextColor(ACCENT);
        t.setPadding(0, dp(a, 16), 0, dp(a, 8));
        content.addView(t);
        return t;
    }

    public static TextView button(Activity a, String s, View.OnClickListener onClick) {
        TextView b = tv(a, s, 14, TEXT);
        b.setGravity(Gravity.CENTER);
        b.setTypeface(null, 1);
        b.setPadding(dp(a, 16), 0, dp(a, 16), 0);
        b.setBackground(gradientBg(a, ACCENT, ACCENT_GLOW, 12));
        b.setOnClickListener(onClick);
        return b;
    }

    public static TextView ghostButton(Activity a, String s, View.OnClickListener onClick) {
        TextView b = tv(a, s, 12, MUTED);
        b.setGravity(Gravity.CENTER);
        b.setTypeface(null, 1);
        b.setPadding(dp(a, 12), 0, dp(a, 12), 0);
        b.setBackground(bgBorder(a, CARD, Color.rgb(45, 49, 70), 12, 1));
        b.setOnClickListener(onClick);
        return b;
    }

    public static LinearLayout card(Activity a) {
        LinearLayout l = col(a);
        l.setPadding(dp(a, 16), dp(a, 13), dp(a, 16), dp(a, 13));
        l.setBackground(bgBorder(a, CARD, Color.rgb(32, 36, 56), 16, 1));
        return l;
    }

    public static View card(Activity a, String title, String sub, String caption, int captionColor) {
        LinearLayout l = card(a);
        TextView t = tv(a, title, 15, TEXT);
        t.setTypeface(null, 1);
        l.addView(t);
        l.addView(tv(a, sub, 12, MUTED));
        TextView cap = tv(a, caption, 11, captionColor);
        cap.setPadding(0, dp(a, 4), 0, 0);
        l.addView(cap);
        return l;
    }

    public static View badge(Activity a, String text, int color) {
        TextView b = tv(a, text, 10, color);
        b.setTypeface(null, 1);
        b.setPadding(dp(a, 8), dp(a, 3), dp(a, 8), dp(a, 3));
        b.setBackground(bgBorder(a, Color.rgb(18, 20, 30), color, 8, 1));
        return b;
    }

    private Ui() {}
}
