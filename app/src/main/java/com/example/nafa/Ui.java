package com.example.nafa;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public final class Ui {
    private Ui() {}

    public static final int GREEN = 0xFF00852B, GREEN_DARK = 0xFF006F24, GREEN_DEEP = 0xFF00561C,
            MINT = 0xFFDDF8E6, LIME = 0xFF8DF39A, CANVAS = 0xFFFFFFFF, WHITE = 0xFFFFFFFF,
            FIELD = 0xFFF0F0F0, INK = 0xFF222222, MUTED = 0xFF888888, LINE = 0xFFCCCCCC,
            WARN = 0xFFB84A2A, WARN_SOFT = 0xFFFFF0EB, AMBER = 0xFFF2A900, AMBER_SOFT = 0xFFFFF5DB;

    public static final int MATCH = -1, WRAP = -2;
    public static final int PRIMARY = 0, SOFT = 1, OUTLINE = 2, DANGER = 3, GHOST = 4, LIGHT = 5;

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static int screenDp(Context c) {
        android.util.DisplayMetrics m = c.getResources().getDisplayMetrics();
        return Math.round(m.widthPixels / m.density);
    }

    public static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public static LinearLayout.LayoutParams lp(Context c, int w, int h) {
        return new LinearLayout.LayoutParams(w < 0 ? w : dp(c, w), h < 0 ? h : dp(c, h));
    }

    public static LinearLayout.LayoutParams lp(Context c, int w, int h, float weight) {
        return new LinearLayout.LayoutParams(w < 0 ? w : dp(c, w), h < 0 ? h : dp(c, h), weight);
    }

    public static LinearLayout.LayoutParams m(Context c, LinearLayout.LayoutParams p, int start, int top, int end, int bottom) {
        p.setMarginStart(dp(c, start));
        p.topMargin = dp(c, top);
        p.setMarginEnd(dp(c, end));
        p.bottomMargin = dp(c, bottom);
        return p;
    }

    public static <T extends View> T pad(T v, int start, int top, int end, int bottom) {
        Context c = v.getContext();
        v.setPaddingRelative(dp(c, start), dp(c, top), dp(c, end), dp(c, bottom));
        return v;
    }

    public static <T extends View> T pad(T v, int all) {
        return pad(v, all, all, all, all);
    }

    public static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static View space(Context c, int h) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, h)));
        return v;
    }

    public static View grow(Context c) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1));
        return v;
    }

    public static View divider(Context c, int insetStart) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, Math.max(1, dp(c, 1) / 2 + 1));
        p.setMarginStart(dp(c, insetStart));
        v.setLayoutParams(p);
        return v;
    }

    public static ScrollView scroll(Context c) {
        ScrollView s = new ScrollView(c);
        s.setFillViewport(true);
        s.setOverScrollMode(View.OVER_SCROLL_NEVER);
        s.setVerticalScrollBarEnabled(false);
        return s;
    }

    public static HorizontalScrollView hscroll(Context c, View child) {
        HorizontalScrollView h = new HorizontalScrollView(c);
        h.setHorizontalScrollBarEnabled(false);
        h.setOverScrollMode(View.OVER_SCROLL_NEVER);
        h.setClipToPadding(false);
        h.addView(child);
        return h;
    }

    public static TextView text(Context c, CharSequence s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        t.setLineSpacing(0, 1.12f);
        return t;
    }

    public static TextView medium(Context c, CharSequence s, float sp, int color) {
        TextView t = text(c, s, sp, color, false);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    public static TextView lines(TextView t, int n) {
        t.setMaxLines(n);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    public static TextView center(TextView t) {
        t.setGravity(Gravity.CENTER);
        t.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        return t;
    }

    public static GradientDrawable shape(Context c, int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, Math.min(radius, 5)));
        return g;
    }

    public static GradientDrawable shape(Context c, int color, float radius, int stroke, float strokeDp) {
        GradientDrawable g = shape(c, color, radius);
        g.setStroke(Math.max(1, dp(c, strokeDp)), stroke);
        return g;
    }

    public static GradientDrawable oval(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    public static GradientDrawable gradient(Context c, int from, int to, float radius) {
        return shape(c, from, radius);
    }

    public static GradientDrawable topRounded(Context c, int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(0);
        return g;
    }

    public static Drawable ripple(Context c, Drawable content, float radius) {
        GradientDrawable mask = shape(c, Color.WHITE, radius);
        return new RippleDrawable(ColorStateList.valueOf(0x1F00852B), content, mask);
    }

    public static <T extends View> T tap(T v, Drawable bg, float radius, View.OnClickListener l) {
        v.setBackground(ripple(v.getContext(), bg, radius));
        v.setClickable(true);
        v.setFocusable(true);
        if (l != null) v.setOnClickListener(l);
        return v;
    }

    public static void enable(View v, boolean on) {
        v.setEnabled(on);
        v.setAlpha(on ? 1f : 0.45f);
    }

    public static Drawable icon(Context c, int res, int color, int sizeDp) {
        Drawable d = c.getDrawable(res).mutate();
        d.setTint(color);
        int s = dp(c, sizeDp);
        d.setBounds(0, 0, s, s);
        return d;
    }

    public static ImageView iv(Context c, int res, int color, int sizeDp) {
        ImageView i = new ImageView(c);
        i.setImageDrawable(icon(c, res, color, sizeDp));
        i.setScaleType(ImageView.ScaleType.FIT_CENTER);
        i.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return i;
    }

    public static FrameLayout badgeIcon(Context c, int res, int fg, int bg, int size, int iconSize) {
        FrameLayout f = new FrameLayout(c);
        ImageView i = new ImageView(c);
        i.setImageDrawable(icon(c, res, fg, iconSize));
        f.addView(i, new FrameLayout.LayoutParams(dp(c, iconSize), dp(c, iconSize), Gravity.CENTER));
        f.setLayoutParams(new LinearLayout.LayoutParams(dp(c, size), dp(c, size)));
        return f;
    }

    public static FrameLayout iconButton(Context c, int res, int fg, int bg, int size, View.OnClickListener l) {
        FrameLayout f = badgeIcon(c, res, fg, bg, size, Math.round(size * 0.55f));
        f.setTag(R.id.nafa_icon, res);
        boolean plain = Color.alpha(bg) < 0x80 || bg == MINT || bg == CANVAS || bg == WARN_SOFT || bg == FIELD;
        f.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22000000), plain ? null : oval(bg), oval(Color.WHITE)));
        f.setClickable(true);
        f.setFocusable(true);
        if (l != null) f.setOnClickListener(l);
        return f;
    }

    public static void swapIcon(FrameLayout b, int res, int color) {
        if (b == null || b.getChildCount() == 0 || !(b.getChildAt(0) instanceof ImageView)) return;
        ImageView i = (ImageView) b.getChildAt(0);
        Drawable d = b.getContext().getDrawable(res).mutate();
        d.setTint(color);
        i.setImageDrawable(d);
    }

    public static TextView counter(Context c) {
        TextView t = text(c, "", 10, WHITE, true);
        center(t);
        t.setMinWidth(dp(c, 18));
        t.setPadding(dp(c, 4), 0, dp(c, 4), 0);
        t.setBackground(shape(c, WARN, 9, WHITE, 1.5f));
        t.setVisibility(View.GONE);
        return t;
    }

    public static void setCount(TextView t, int n) {
        if (t == null) return;
        t.setVisibility(n > 0 ? View.VISIBLE : View.GONE);
        t.setText(n > 99 ? "99+" : String.valueOf(n));
    }

    public static FrameLayout iconWithCounter(Context c, int res, int fg, int bg, int size, TextView counter, View.OnClickListener l) {
        FrameLayout wrap = new FrameLayout(c);
        FrameLayout b = iconButton(c, res, fg, bg, size, l);
        wrap.addView(b, new FrameLayout.LayoutParams(dp(c, size), dp(c, size), Gravity.CENTER));
        wrap.addView(counter, new FrameLayout.LayoutParams(-2, dp(c, 18), Gravity.TOP | Gravity.END));
        wrap.setLayoutParams(new LinearLayout.LayoutParams(dp(c, size + 6), dp(c, size + 6)));
        return wrap;
    }

    public static TextView title(Context c, CharSequence s) {
        TextView t = text(c, s, 22, INK, true);
        t.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        t.setLayoutParams(new LinearLayout.LayoutParams(MATCH, dp(c, 56)));
        return t;
    }

    public static Button button(Context c, String label, int style, int iconRes) {
        int fg, bg, stroke = 0;
        switch (style) {
            case SOFT: fg = GREEN_DARK; bg = MINT; break;
            case OUTLINE: fg = GREEN; bg = WHITE; stroke = GREEN; break;
            case DANGER: fg = WARN; bg = WARN_SOFT; break;
            case GHOST: fg = GREEN; bg = Color.TRANSPARENT; break;
            case LIGHT: fg = GREEN_DARK; bg = WHITE; break;
            default: fg = WHITE; bg = GREEN;
        }
        Button b = new Button(c);
        b.setText(label);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setTextColor(fg);
        center(b);
        b.setMinHeight(dp(c, 46));
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setStateListAnimator(null);
        b.setElevation(0);
        b.setIncludeFontPadding(false);
        b.setSingleLine(true);
        b.setEllipsize(TextUtils.TruncateAt.END);
        b.setAllCaps(false);
        b.setPadding(dp(c, 18), 0, dp(c, 18), 0);
        b.setTag(label);
        GradientDrawable g = stroke != 0 ? shape(c, bg, 10, stroke, 1f) : shape(c, bg, 10);
        tap(b, g, 10, null);
        return b;
    }

    public static Button button(Context c, String label, boolean primary) {
        return button(c, label, primary ? PRIMARY : OUTLINE, 0);
    }

    public static void busy(TextView b, boolean busy, String busyLabel) {
        Object orig = b.getTag();
        b.setText(busy ? busyLabel : (orig == null ? b.getText() : orig.toString()));
        enable(b, !busy);
    }

    public static TextView link(Context c, String s, View.OnClickListener l) {
        TextView t = medium(c, s, 14, GREEN);
        t.setPadding(dp(c, 6), dp(c, 8), dp(c, 6), dp(c, 8));
        tap(t, null, 10, l);
        return t;
    }

    public static final class Field {
        public LinearLayout box;
        public EditText edit;

        public String value() { return edit.getText().toString().trim(); }

        public void set(String s) { edit.setText(s == null ? "" : s); }

        public void error(String msg) {
            Context c = box.getContext();
            box.setBackground(shape(c, WHITE, 10, WARN, 1f));
            edit.setError(msg);
            edit.requestFocus();
        }
    }

    public static Field field(Context c, String hint, int iconRes, int inputType) {
        Field f = new Field();
        f.box = row(c);
        f.box.setBackground(shape(c, WHITE, 10, LINE, 1));
        f.box.setPadding(dp(c, 14), 0, dp(c, 14), 0);
        f.box.setMinimumHeight(dp(c, 50));
        EditText e = new EditText(c);
        e.setBackground(null);
        e.setHint(hint);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        e.setTextColor(INK);
        e.setHintTextColor(MUTED);
        e.setInputType(inputType);
        e.setPadding(0, dp(c, 14), 0, dp(c, 14));
        e.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        e.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        f.edit = e;
        f.box.addView(e, lp(c, 0, WRAP, 1));
        final LinearLayout box = f.box;
        e.setOnFocusChangeListener((v, has) -> box.setBackground(has
                ? shape(c, WHITE, 10, GREEN, 1) : shape(c, WHITE, 10, LINE, 1)));
        return f;
    }

    public static Field area(Context c, String hint, int minLines) {
        Field f = field(c, hint, 0, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        f.edit.setMinLines(minLines);
        f.edit.setMaxLines(8);
        f.edit.setGravity(Gravity.START | Gravity.TOP);
        f.box.setGravity(Gravity.TOP);
        return f;
    }

    public static Field password(Context c, String hint) {
        Field f = field(c, hint, R.drawable.ic_lock, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        final EditText e = f.edit;
        final ImageView eye = iv(c, R.drawable.ic_visibility_off, MUTED, 22);
        pad(eye, 2);
        tap(eye, null, 20, v -> {
            boolean hidden = e.getTransformationMethod() instanceof PasswordTransformationMethod;
            e.setTransformationMethod(hidden ? null : PasswordTransformationMethod.getInstance());
            eye.setImageDrawable(icon(c, hidden ? R.drawable.ic_visibility : R.drawable.ic_visibility_off, MUTED, 22));
            e.setSelection(e.getText().length());
        });
        e.setTransformationMethod(PasswordTransformationMethod.getInstance());
        f.box.addView(eye, lp(c, 26, 26));
        return f;
    }

    public static final class Select {
        public LinearLayout box;
        public TextView value;
        public String[] items = new String[0];
        public int index = -1;
        public Runnable onChange;
        String placeholder;

        public void set(int i) {
            index = i;
            boolean has = i >= 0 && i < items.length;
            value.setText(has ? items[i] : placeholder);
            value.setTextColor(has ? INK : MUTED);
            if (onChange != null) onChange.run();
        }
    }

    public static Select select(Activity a, String placeholder, int iconRes, String[] items) {
        Select s = new Select();
        s.placeholder = placeholder;
        s.items = items;
        s.box = row(a);
        s.box.setPadding(dp(a, 14), 0, dp(a, 14), 0);
        s.box.setMinimumHeight(dp(a, 50));
        s.value = text(a, placeholder, 15, MUTED, false);
        s.box.addView(s.value, lp(a, 0, WRAP, 1));
        s.box.addView(iv(a, R.drawable.ic_chevron_down, MUTED, 20));
        tap(s.box, shape(a, WHITE, 10, LINE, 1), 10, v -> {
            AlertDialog d = new AlertDialog.Builder(a).setTitle(placeholder)
                    .setSingleChoiceItems(s.items, s.index, (dlg, w) -> { s.set(w); dlg.dismiss(); })
                    .setNegativeButton("إلغاء", null).create();
            rtl(d);
            d.show();
        });
        return s;
    }

    public static TextView label(Context c, String s) {
        TextView t = medium(c, s, 14, INK);
        t.setPadding(dp(c, 4), dp(c, 16), dp(c, 4), dp(c, 8));
        return t;
    }

    public static Switch toggle(Context c) {
        Switch s = new Switch(c);
        int[][] st = {{android.R.attr.state_checked}, {}};
        s.setThumbTintList(new ColorStateList(st, new int[]{GREEN, WHITE}));
        s.setTrackTintList(new ColorStateList(st, new int[]{0x6600852B, 0xFFC9CFCC}));
        return s;
    }

    public static TextView chip(Context c, String s, int fg, int bg, int iconRes) {
        TextView t = text(c, s, 12, fg, false);
        t.setPadding(dp(c, 6), dp(c, 2), dp(c, 6), dp(c, 2));
        t.setBackground(shape(c, bg, 3));
        t.setSingleLine(true);
        t.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        return t;
    }

    public static TextView choice(Context c, String s, int iconRes) {
        TextView t = text(c, s, 14, INK, false);
        t.setPadding(dp(c, 12), dp(c, 7), dp(c, 12), dp(c, 7));
        t.setSingleLine(true);
        t.setTag(R.id.nafa_icon, iconRes);
        setChoice(t, false);
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    public static void setChoice(TextView t, boolean on) {
        Context c = t.getContext();
        int fg = on ? WHITE : INK;
        t.setTextColor(fg);
        t.setBackground(ripple(c, on ? shape(c, GREEN, 8) : shape(c, WHITE, 8, LINE, 1), 8));
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = col(c);
        l.setBackground(shape(c, WHITE, 10, LINE, 1));
        l.setPadding(dp(c, 12), dp(c, 12), dp(c, 12), dp(c, 12));
        return l;
    }

    public static LinearLayout.LayoutParams stack(Context c) {
        return m(c, lp(c, MATCH, WRAP), 0, 0, 0, 12);
    }

    public static LinearLayout group(Context c) {
        LinearLayout l = col(c);
        l.setBackground(shape(c, WHITE, 10, LINE, 1));
        l.setClipToOutline(true);
        return l;
    }

    public static LinearLayout menuRow(Context c, int iconRes, String title, String sub, int fg, int bg, View.OnClickListener l) {
        LinearLayout r = row(c);
        r.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        r.setMinimumHeight(dp(c, 50));
        LinearLayout t = col(c);
        t.addView(text(c, title, 15, fg == WARN ? WARN : INK, false));
        if (sub != null && !sub.isEmpty()) t.addView(lines(text(c, sub, 12, MUTED, false), 1));
        r.addView(t, lp(c, 0, WRAP, 1));
        r.setTag(t);
        if (l != null) tap(r, null, 0, l);
        return r;
    }

    public static LinearLayout section(Context c, String title, String action, View.OnClickListener l) {
        LinearLayout r = row(c);
        r.setPadding(0, dp(c, 16), 0, dp(c, 8));
        r.addView(text(c, title, 16, INK, true), lp(c, 0, WRAP, 1));
        if (action != null) r.addView(link(c, action, l));
        return r;
    }

    public static View avatar(Context c, String name, String b64, int size) {
        FrameLayout f = new FrameLayout(c);
        f.setBackground(oval(MINT));
        f.setClipToOutline(true);
        if (b64 != null && !b64.isEmpty()) {
            ImageView i = new ImageView(c);
            i.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Img.load(i, b64, dp(c, size));
            f.addView(i, new FrameLayout.LayoutParams(-1, -1));
        } else {
            TextView t = text(c, Data.initials(name), 1, GREEN_DARK, true);
            center(t);
            t.setTextSize(TypedValue.COMPLEX_UNIT_PX, dp(c, size) * 0.36f);
            f.addView(t, new FrameLayout.LayoutParams(-1, -1));
        }
        f.setLayoutParams(new LinearLayout.LayoutParams(dp(c, size), dp(c, size)));
        return f;
    }

    public static LinearLayout stars(Context c, double rating, int size) {
        LinearLayout r = row(c);
        for (int i = 1; i <= 5; i++) {
            int res = rating >= i - 0.25 ? R.drawable.ic_star
                    : rating >= i - 0.75 ? R.drawable.ic_star_half : R.drawable.ic_star_outline;
            ImageView s = iv(c, res, res == R.drawable.ic_star_outline ? 0xFFD5D9D7 : AMBER, size);
            if (res == R.drawable.ic_star_half) s.setScaleX(-1f);
            r.addView(s);
        }
        return r;
    }

    public static Data.Cb<Integer> onRating;

    public static LinearLayout ratingPicker(Context c, int[] holder, int size) {
        LinearLayout r = row(c);
        r.setGravity(Gravity.CENTER);
        ImageView[] all = new ImageView[5];
        for (int i = 0; i < 5; i++) {
            final int val = i + 1;
            ImageView s = iv(c, R.drawable.ic_star_outline, 0xFFD5D9D7, size);
            pad(s, 4);
            s.setLayoutParams(lp(c, size + 8, size + 8));
            all[i] = s;
            tap(s, null, size, v -> {
                holder[0] = val;
                for (int k = 0; k < 5; k++) {
                    all[k].setImageDrawable(icon(c, k < val ? R.drawable.ic_star : R.drawable.ic_star_outline,
                            k < val ? AMBER : 0xFFD5D9D7, size));
                }
                if (onRating != null) onRating.on(val);
            });
            r.addView(s);
        }
        return r;
    }

    public static View empty(Context c, int iconRes, String title, String sub) {
        LinearLayout l = col(c);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.setPadding(dp(c, 24), dp(c, 36), dp(c, 24), dp(c, 36));
        TextView t = center(text(c, title, 15, MUTED, false));
        l.addView(t, lp(c, MATCH, WRAP));
        return l;
    }

    public static View loading(Context c) {
        FrameLayout f = new FrameLayout(c);
        ProgressBar p = new ProgressBar(c);
        p.setIndeterminateTintList(ColorStateList.valueOf(GREEN));
        f.addView(p, new FrameLayout.LayoutParams(dp(c, 36), dp(c, 36), Gravity.CENTER));
        f.setLayoutParams(lp(c, MATCH, 180));
        return f;
    }

    public static final class Screen {
        public LinearLayout root, bar, actions, body;
        public ScrollView scroll;
        public TextView title;
    }

    public static Screen screen(Activity a, String title, boolean back) {
        Screen s = new Screen();
        s.root = col(a);
        s.root.setBackgroundColor(CANVAS);
        s.bar = row(a);
        s.bar.setBackgroundColor(GREEN);
        if (a instanceof BaseActivity) ((BaseActivity) a).bars(GREEN, WHITE, false);
        s.bar.setPadding(dp(a, 8), 0, dp(a, 8), 0);
        s.bar.setMinimumHeight(dp(a, 56));
        if (back) s.bar.addView(iconButton(a, R.drawable.ic_arrow_forward, WHITE, Color.TRANSPARENT, 40, v -> a.finish()));
        s.title = lines(text(a, title, 18, WHITE, true), 1);
        s.bar.addView(s.title, m(a, lp(a, 0, WRAP, 1), back ? 8 : 8, 0, 8, 0));
        s.actions = row(a);
        s.bar.addView(s.actions);
        s.root.addView(s.bar, lp(a, MATCH, WRAP));
        s.scroll = scroll(a);
        s.body = col(a);
        s.body.setPadding(dp(a, 16), dp(a, 12), dp(a, 16), dp(a, 24));
        s.scroll.addView(s.body, new ViewGroup.LayoutParams(-1, -2));
        s.root.addView(s.scroll, lp(a, MATCH, 0, 1));
        a.setContentView(s.root);
        return s;
    }

    public static LinearLayout footer(Activity a, Screen s) {
        LinearLayout f = row(a);
        f.setBackgroundColor(WHITE);
        f.setPadding(dp(a, 16), dp(a, 12), dp(a, 16), dp(a, 12));
        s.root.addView(divider(a, 0));
        s.root.addView(f, lp(a, MATCH, WRAP));
        return f;
    }

    public static void toast(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
    }

    public static void rtl(Dialog d) {
        if (d.getWindow() != null) d.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    }

    public static void confirm(Activity a, String title, String msg, String ok, boolean danger, Runnable r) {
        AlertDialog d = new AlertDialog.Builder(a).setTitle(title).setMessage(msg)
                .setNegativeButton("إلغاء", null).setPositiveButton(ok, (x, y) -> r.run()).create();
        d.setOnShowListener(x -> {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(danger ? WARN : GREEN);
            d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(MUTED);
        });
        rtl(d);
        d.show();
    }

    public static Dialog options(Activity a, String title, String[] labels, int[] icons, int[] colors, Data.Cb<Integer> pick) {
        LinearLayout c = col(a);
        final Dialog[] holder = new Dialog[1];
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            int fg = colors != null && colors[i] != 0 ? colors[i] : GREEN;
            int bg = fg == WARN ? WARN_SOFT : fg == GREEN ? MINT : FIELD;
            LinearLayout r = menuRow(a, icons[i], labels[i], null, fg, bg, null);
            tap(r, null, 16, v -> { if (holder[0] != null) holder[0].dismiss(); pick.on(idx); });
            c.addView(r, lp(a, MATCH, WRAP));
        }
        holder[0] = sheet(a, title, c);
        return holder[0];
    }

    public static Dialog sheet(Activity a, String title, View content) {
        Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout wrap = col(a);
        wrap.setBackground(topRounded(a, WHITE, 14));
        wrap.setPadding(dp(a, 20), dp(a, 16), dp(a, 20), dp(a, 24));
        final int basePad = dp(a, 24);
        wrap.setOnApplyWindowInsetsListener((v, in) -> {
            int b;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                b = in.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.ime()).bottom;
            } else {
                b = in.getSystemWindowInsetBottom();
            }
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), basePad + b);
            return in;
        });
        if (title != null) wrap.addView(text(a, title, 17, INK, true), m(a, lp(a, MATCH, WRAP), 0, 10, 0, 8));
        wrap.addView(content, lp(a, MATCH, WRAP));
        ScrollView sv = scroll(a);
        sv.setFillViewport(false);
        sv.addView(wrap);
        d.setContentView(sv);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setGravity(Gravity.BOTTOM);
            w.setWindowAnimations(android.R.style.Animation_InputMethod);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            w.getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }
        d.show();
        if (w != null) w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        return d;
    }
}
