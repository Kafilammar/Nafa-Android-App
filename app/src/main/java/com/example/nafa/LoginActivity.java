package com.example.nafa;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends BaseActivity {
    private Ui.Field email, pass;
    private TextView login;

    @Override protected boolean needsAuth() { return false; }

    @Override
    protected void onReady(Bundle saved) {
        bars(Ui.GREEN, Ui.WHITE, false);
        ScrollView sc = Ui.scroll(this);
        sc.setBackgroundColor(Ui.WHITE);
        LinearLayout root = Ui.col(this);
        sc.addView(root);

        LinearLayout head = Ui.col(this);
        head.setGravity(Gravity.CENTER);
        head.setBackground(Ui.gradient(this, Ui.GREEN, Ui.GREEN_DARK, 0));
        Ui.pad(head, 24, 24, 24, 24);
        head.addView(Ui.badgeIcon(this, R.drawable.ic_handyman, Ui.WHITE, 0, 56, 48));
        head.addView(Ui.center(Ui.text(this, "نفع", 30, Ui.WHITE, true)), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 10, 0, 0));
        head.addView(Ui.center(Ui.text(this, "شارك أدواتك مع جيرانك", 14, Ui.WHITE, false)), Ui.lp(this, Ui.MATCH, Ui.WRAP));
        root.addView(head, Ui.lp(this, Ui.MATCH, Ui.WRAP));

        LinearLayout form = Ui.col(this);
        form.setBackgroundColor(Ui.WHITE);
        Ui.pad(form, 24, 28, 24, 24);
        LinearLayout.LayoutParams fp = Ui.lp(this, Ui.MATCH, Ui.WRAP, 1);
                root.addView(form, fp);

        form.addView(Ui.text(this, "تسجيل الدخول", 20, Ui.INK, true));
        form.addView(Ui.text(this, "أهلا بعودتك، أدخل بياناتك للمتابعة", 14, Ui.MUTED, false), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 4, 0, 20));

        email = Ui.field(this, "البريد الإلكتروني", R.drawable.ic_email,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        form.addView(email.box, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        pass = Ui.password(this, "كلمة المرور");
        pass.edit.setImeOptions(EditorInfo.IME_ACTION_DONE);
        pass.edit.setOnEditorActionListener((v, id, ev) -> { if (id == EditorInfo.IME_ACTION_DONE) { doLogin(); return true; } return false; });
        form.addView(pass.box, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 12, 0, 0));

        LinearLayout fr = Ui.row(this);
        fr.addView(Ui.grow(this));
        fr.addView(Ui.link(this, "نسيت كلمة المرور؟", v -> go(ForgotPasswordActivity.class)));
        form.addView(fr, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 4, 0, 8));

        login = Ui.button(this, "تسجيل الدخول", Ui.PRIMARY, 0);
        login.setOnClickListener(v -> doLogin());
        form.addView(login, Ui.lp(this, Ui.MATCH, 54));

        LinearLayout orRow = Ui.row(this);
        orRow.addView(Ui.divider(this, 0), Ui.lp(this, 0, 1, 1));
        orRow.addView(Ui.text(this, "أو", 13, Ui.MUTED, false), Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 12, 0, 12, 0));
        orRow.addView(Ui.divider(this, 0), Ui.lp(this, 0, 1, 1));
        form.addView(orRow, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 22, 0, 22));

        TextView signup = Ui.button(this, "إنشاء حساب جديد", Ui.OUTLINE, R.drawable.ic_person_add);
        signup.setOnClickListener(v -> go(SignUpActivity.class));
        form.addView(signup, Ui.lp(this, Ui.MATCH, 54));

        FrameLayout wrap = new FrameLayout(this);
        wrap.addView(sc);
        setContentView(wrap);
    }

    private void doLogin() {
        String e = email.value(), p = pass.edit.getText().toString();
        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) { email.error("أدخل بريدا إلكترونيا صحيحا"); return; }
        if (p.length() < 6) { pass.error("كلمة المرور 6 أحرف على الأقل"); return; }
        Ui.busy(login, true, "جار الدخول...");
        FirebaseAuth.getInstance().signInWithEmailAndPassword(e, p)
                .addOnSuccessListener(x -> {
                    Data.clearCache();
                    Intent i = new Intent(this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(x -> { Ui.busy(login, false, null); Ui.toast(this, Data.authError(x)); });
    }
}
