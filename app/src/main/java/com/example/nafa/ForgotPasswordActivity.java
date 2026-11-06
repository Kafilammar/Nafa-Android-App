package com.example.nafa;

import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends BaseActivity {
    @Override protected boolean needsAuth() { return false; }

    @Override
    protected void onReady(Bundle saved) {
        Ui.Screen s = Ui.screen(this, "", true);
        LinearLayout form = Ui.col(this);
        form.setGravity(Gravity.CENTER_HORIZONTAL);
        Ui.pad(form, 8, 12, 8, 0);
        form.addView(Ui.badgeIcon(this, R.drawable.ic_lock, Ui.GREEN, Ui.MINT, 92, 44));
        form.addView(Ui.center(Ui.text(this, "استعادة كلمة المرور", 22, Ui.INK, true)), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 18, 0, 6));
        form.addView(Ui.center(Ui.text(this, "أدخل بريدك الإلكتروني المسجل وسنرسل لك رابطا لتعيين كلمة مرور جديدة.", 14, Ui.MUTED, false)),
                Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 0, 0, 24));
        Ui.Field email = Ui.field(this, "البريد الإلكتروني", R.drawable.ic_email,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        form.addView(email.box, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        TextView send = Ui.button(this, "إرسال رابط الاستعادة", Ui.PRIMARY, 0);
        form.addView(send, Ui.m(this, Ui.lp(this, Ui.MATCH, 54), 0, 18, 0, 0));

        LinearLayout done = Ui.col(this);
        done.setGravity(Gravity.CENTER_HORIZONTAL);
        done.setBackground(Ui.shape(this, Ui.MINT, 10));
        Ui.pad(done, 18);
        done.setVisibility(View.GONE);
        done.addView(Ui.iv(this, R.drawable.ic_mark_email, Ui.GREEN, 36));
        TextView dt = Ui.center(Ui.text(this, "", 14, Ui.GREEN_DARK, false));
        done.addView(dt, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 8, 0, 0));
        form.addView(done, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 20, 0, 0));

        form.addView(Ui.link(this, "العودة لتسجيل الدخول", v -> finish()), Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 0, 16, 0, 0));
        s.body.addView(form);

        send.setOnClickListener(v -> {
            String m = email.value();
            if (!Patterns.EMAIL_ADDRESS.matcher(m).matches()) { email.error("أدخل بريدا إلكترونيا صحيحا"); return; }
            Ui.busy(send, true, "جار الإرسال...");
            FirebaseAuth.getInstance().setLanguageCode("ar");
            FirebaseAuth.getInstance().sendPasswordResetEmail(m)
                    .addOnSuccessListener(a -> {
                        Ui.busy(send, false, null);
                        send.setText("إعادة الإرسال");
                        dt.setText("إذا كان البريد " + m + " مسجلا لدينا فستصلك رسالة خلال دقائق. تحقق أيضا من مجلد الرسائل غير المرغوب فيها.");
                        done.setVisibility(View.VISIBLE);
                    })
                    .addOnFailureListener(a -> { Ui.busy(send, false, null); Ui.toast(this, Data.authError(a)); });
        });
    }
}
