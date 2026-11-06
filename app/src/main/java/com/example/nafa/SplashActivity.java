package com.example.nafa;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends BaseActivity {
    @Override protected boolean needsAuth() { return false; }

    @Override
    protected void onReady(Bundle saved) {
        bars(Ui.GREEN, Ui.GREEN_DARK, false);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.GREEN);

        LinearLayout c = Ui.col(this);
        c.setGravity(Gravity.CENTER);
        FrameLayout logo = Ui.badgeIcon(this, R.drawable.ic_handyman, Ui.WHITE, 0, 80, 64);
        c.addView(logo);
        TextView name = Ui.center(Ui.text(this, "نفع", 36, Ui.WHITE, true));
        c.addView(name, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 18, 0, 0));
        TextView tag = Ui.center(Ui.text(this, "شارك أدواتك مع جيرانك", 15, Ui.WHITE, false));
        c.addView(tag, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        root.addView(c, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing()) return;
            boolean in = FirebaseAuth.getInstance().getCurrentUser() != null;
            Class<?> next = in ? MainActivity.class : OnboardingActivity.done(this) ? LoginActivity.class : OnboardingActivity.class;
            startActivity(new Intent(this, next));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 1300);
    }
}
