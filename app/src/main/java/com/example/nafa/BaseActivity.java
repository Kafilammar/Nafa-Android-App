package com.example.nafa;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseActivity extends Activity {
    protected FirebaseFirestore db;
    protected String uid;
    protected boolean ready;
    protected final List<ListenerRegistration> listeners = new ArrayList<>();

    private int insetTop, insetBottom;
    private boolean imeShown;

    protected void onKeyboard(boolean shown) {}
    private int topColor = Ui.CANVAS, bottomColor = Ui.CANVAS;

    protected boolean needsAuth() { return true; }

    protected abstract void onReady(Bundle saved);

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        db = FirebaseFirestore.getInstance();
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (needsAuth() && u == null) {
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }
        uid = u == null ? null : u.getUid();
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        edgeToEdge();
        ready = true;
        onReady(saved);
    }

    @SuppressWarnings("deprecation")
    private void edgeToEdge() {
        Window w = getWindow();
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) {
            w.setNavigationBarContrastEnforced(false);
            w.setStatusBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
        } else {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        View content = findViewById(android.R.id.content);
        content.setOnApplyWindowInsetsListener((v, in) -> {
            int l, t, r, b;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets s = in.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.ime() | WindowInsets.Type.displayCutout());
                l = s.left; t = s.top; r = s.right; b = s.bottom;
            } else {
                l = in.getSystemWindowInsetLeft(); t = in.getSystemWindowInsetTop();
                r = in.getSystemWindowInsetRight(); b = in.getSystemWindowInsetBottom();
            }
            boolean ime;
            if (Build.VERSION.SDK_INT >= 30) ime = in.isVisible(WindowInsets.Type.ime());
            else ime = b > getResources().getDisplayMetrics().heightPixels / 4;
            v.setPadding(l, t, r, b);
            insetTop = t;
            insetBottom = b;
            paintBars();
            if (ime != imeShown) { imeShown = ime; onKeyboard(ime); }
            return in;
        });
        bars(Ui.CANVAS, Ui.CANVAS, true);
    }

    @SuppressWarnings("deprecation")
    protected void bars(int top, int bottom, boolean lightStatus) {
        topColor = top;
        bottomColor = bottom;
        Window w = getWindow();
        boolean lightNav = Color.luminance(bottom) > 0.5f;
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController ic = w.getInsetsController();
            if (ic != null) {
                int f = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                int v = (lightStatus ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0)
                        | (lightNav ? WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS : 0);
                ic.setSystemBarsAppearance(v, f);
            }
        } else {
            View d = w.getDecorView();
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if (lightStatus) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (lightNav && Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            d.setSystemUiVisibility(flags);
        }
        paintBars();
    }

    private void paintBars() {
        View content = findViewById(android.R.id.content);
        if (content == null) return;
        LayerDrawable ld = new LayerDrawable(new Drawable[]{new ColorDrawable(bottomColor), new ColorDrawable(topColor)});
        ld.setLayerGravity(1, Gravity.TOP | Gravity.FILL_HORIZONTAL);
        ld.setLayerHeight(1, Math.max(insetTop, 1));
        content.setBackground(ld);
    }

    protected int bottomInset() { return insetBottom; }

    protected void go(Class<?> c) { startActivity(new Intent(this, c)); }

    @Override
    protected void onDestroy() {
        for (ListenerRegistration r : listeners) r.remove();
        listeners.clear();
        super.onDestroy();
    }
}
