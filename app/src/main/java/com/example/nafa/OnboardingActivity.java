package com.example.nafa;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OnboardingActivity extends BaseActivity {
    public static final String PREFS = "nafa", KEY = "onboarded";

    private static final int[] ICONS = {R.drawable.ic_handyman, R.drawable.ic_swap_horiz, R.drawable.ic_group};
    private static final String[] TITLES = {"أدوات حيك بين يديك", "استعر بضغطة زر", "مجتمع موثوق ومتعاون"};
    private static final String[] TEXTS = {
            "اكتشف المثاقب والسلالم وأدوات الحديقة المتاحة عند جيرانك بدل شرائها واستخدامها مرة واحدة.",
            "حدد المدة وتاريخ البدء وأرسل طلبك، وتابع حالته وتواصل مع صاحب الأداة عبر المحادثة.",
            "تقييمات متبادلة بعد كل إعارة، وتذكير بمواعيد الإرجاع، ودعم فني يتابع أي بلاغ."};

    private int page, artDp;
    private FrameLayout art;
    private TextView title, text, next;
    private LinearLayout dots, content;

    public static boolean done(android.content.Context c) {
        return c.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY, false);
    }

    @Override protected boolean needsAuth() { return false; }

    @Override
    protected void onReady(Bundle saved) {
        bars(Ui.WHITE, Ui.WHITE, true);
        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(Ui.WHITE);

        LinearLayout top = Ui.row(this);
        Ui.pad(top, 16, 8, 16, 0);
        top.addView(Ui.text(this, "نفع", 22, Ui.GREEN, true), Ui.lp(this, 0, Ui.WRAP, 1));
        top.addView(Ui.link(this, "تخطي", v -> finishOnboarding()));
        root.addView(top, Ui.lp(this, Ui.MATCH, Ui.WRAP));

        content = Ui.col(this);
        content.setGravity(Gravity.CENTER);
        Ui.pad(content, 28, 0, 28, 0);
        artDp = Ui.clamp(Math.round(Ui.screenDp(this) * 0.62f), 200, 300);
        art = new FrameLayout(this);
        content.addView(art, Ui.lp(this, artDp, artDp));
        title = Ui.center(Ui.text(this, "", 25, Ui.INK, true));
        content.addView(title, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 28, 0, 10));
        text = Ui.center(Ui.text(this, "", 15, Ui.MUTED, false));
        text.setLineSpacing(0, 1.35f);
        content.addView(text, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        root.addView(content, Ui.lp(this, Ui.MATCH, 0, 1));

        dots = Ui.row(this);
        dots.setGravity(Gravity.CENTER);
        root.addView(dots, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 0, 0, 20));
        next = Ui.button(this, "التالي", Ui.PRIMARY, 0);
        next.setOnClickListener(v -> { if (page < TITLES.length - 1) show(page + 1, 1); else finishOnboarding(); });
        root.addView(next, Ui.m(this, Ui.lp(this, Ui.MATCH, 54), 24, 0, 24, 24));
        setContentView(root);

        GestureDetector gd = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }

            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (Math.abs(vx) < Math.abs(vy) || Math.abs(vx) < 500) return false;
                int dir = vx > 0 ? 1 : -1;
                int n = page + dir;
                if (n >= 0 && n < TITLES.length) show(n, dir);
                return true;
            }
        });
        content.setOnTouchListener((v, e) -> gd.onTouchEvent(e));
        show(0, 0);
    }

    private void show(int p, int dir) {
        page = p;
        art.removeAllViews();
        int innerDp = Math.round(artDp * 0.5f);
        FrameLayout inner = Ui.badgeIcon(this, ICONS[p], Ui.GREEN, Ui.MINT, innerDp, Math.round(innerDp * 0.45f));
        art.addView(inner, new FrameLayout.LayoutParams(Ui.dp(this, innerDp), Ui.dp(this, innerDp), Gravity.CENTER));
        title.setText(TITLES[p]);
        text.setText(TEXTS[p]);
        next.setText(p == TITLES.length - 1 ? "ابدأ الآن" : "التالي");
        next.setTag(next.getText());

        dots.removeAllViews();
        for (int i = 0; i < TITLES.length; i++) {
            View d = new View(this);
            boolean on = i == p;
            d.setBackground(Ui.shape(this, on ? Ui.GREEN : Ui.LINE, 4));
            dots.addView(d, Ui.m(this, Ui.lp(this, on ? 24 : 8, 8), 4, 0, 4, 0));
        }
    }

    private void finishOnboarding() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY, true).apply();
        startActivity(new Intent(this, LoginActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
