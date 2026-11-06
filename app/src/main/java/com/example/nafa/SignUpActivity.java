package com.example.nafa;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignUpActivity extends BaseActivity {
    private Ui.Field name, email, phone, pass, pass2;
    private Ui.Select hood;
    private CheckBox terms;
    private TextView submit;
    private List<String> hoodIds;

    @Override protected boolean needsAuth() { return false; }

    @Override
    protected void onReady(Bundle saved) {
        Ui.Screen s = Ui.screen(this, "إنشاء حساب جديد", true);
        s.body.addView(Ui.text(this, "أدخل بياناتك للانضمام إلى مجتمع نفع في حيك", 14, Ui.MUTED, false),
                Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 4, 0, 4, 12));

        name = Ui.field(this, "الاسم الكامل", R.drawable.ic_person, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        email = Ui.field(this, "البريد الإلكتروني", R.drawable.ic_email, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        phone = Ui.field(this, "رقم الجوال (05xxxxxxxx)", R.drawable.ic_phone, InputType.TYPE_CLASS_PHONE);
        pass = Ui.password(this, "كلمة المرور (6 أحرف على الأقل)");
        pass2 = Ui.password(this, "تأكيد كلمة المرور");
        hood = Ui.select(this, "اختر الحي", R.drawable.ic_location_on, new String[0]);
        refreshHoods();
        Data.loadHoods(db, this::refreshHoods);

        for (LinearLayout box : new LinearLayout[]{name.box, email.box, phone.box, hood.box, pass.box, pass2.box})
            s.body.addView(box, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 0, 0, 12));

        LinearLayout tr = Ui.row(this);
        terms = new CheckBox(this);
        terms.setButtonTintList(android.content.res.ColorStateList.valueOf(Ui.GREEN));
        tr.addView(terms);
        TextView tt = Ui.text(this, "", 14, Ui.INK, false);
        String full = "أوافق على الشروط والأحكام وسياسة الخصوصية";
        android.text.SpannableString sp = new android.text.SpannableString(full);
        linkSpan(sp, full, "الشروط والأحكام", "terms");
        linkSpan(sp, full, "سياسة الخصوصية", "privacy");
        tt.setText(sp);
        tt.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        tt.setHighlightColor(0);
        tr.addView(tt, Ui.lp(this, 0, Ui.WRAP, 1));
        s.body.addView(tr, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 0, 0, 12));

        submit = Ui.button(this, "إنشاء الحساب", Ui.PRIMARY, 0);
        submit.setOnClickListener(v -> register());
        s.body.addView(submit, Ui.lp(this, Ui.MATCH, 54));

        LinearLayout lr = Ui.row(this);
        lr.setGravity(android.view.Gravity.CENTER);
        lr.addView(Ui.text(this, "لديك حساب بالفعل؟", 14, Ui.MUTED, false));
        lr.addView(Ui.link(this, "تسجيل الدخول", v -> finish()));
        s.body.addView(lr, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 12, 0, 0));
    }

    private void linkSpan(android.text.SpannableString sp, String full, String part, String type) {
        int i = full.indexOf(part);
        sp.setSpan(new android.text.style.ClickableSpan() {
            @Override public void onClick(android.view.View w) { InfoActivity.open(SignUpActivity.this, type); }
            @Override public void updateDrawState(android.text.TextPaint ds) { ds.setColor(Ui.GREEN); ds.setFakeBoldText(true); ds.setUnderlineText(false); }
        }, i, i + part.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void refreshHoods() {
        hoodIds = Data.hoodIds();
        String prev = hood.index >= 0 && hood.index < hood.items.length ? hood.items[hood.index] : null;
        hood.items = Data.hoodNames().toArray(new String[0]);
        int idx = prev == null ? -1 : Data.hoodNames().indexOf(prev);
        hood.set(idx);
    }

    static boolean validPhone(String p) {
        String d = p.replaceAll("[^0-9]", "");
        return d.matches("05\\d{8}") || d.matches("9665\\d{8}") || d.matches("5\\d{8}");
    }

    private void register() {
        String n = name.value(), e = email.value(), ph = phone.value(), pw = pass.edit.getText().toString();
        if (n.length() < 3) { name.error("أدخل اسمك الكامل"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) { email.error("أدخل بريدا إلكترونيا صحيحا"); return; }
        if (!validPhone(ph)) { phone.error("أدخل رقم جوال سعودي صحيح يبدأ بـ 05"); return; }
        if (hood.index < 0) { Ui.toast(this, "اختر الحي الذي تسكن فيه"); return; }
        if (pw.length() < 6) { pass.error("كلمة المرور 6 أحرف على الأقل"); return; }
        if (!pw.equals(pass2.edit.getText().toString())) { pass2.error("كلمتا المرور غير متطابقتين"); return; }
        if (!terms.isChecked()) { Ui.toast(this, "يجب الموافقة على الشروط والأحكام للمتابعة"); return; }

        Ui.busy(submit, true, "جار إنشاء الحساب...");
        String hoodId = hoodIds.get(hood.index);
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(e, pw).addOnSuccessListener(r -> {
            FirebaseUser u = r.getUser();
            if (u == null) { Ui.busy(submit, false, null); return; }
            Map<String, Object> m = new HashMap<>();
            m.put("fullName", n);
            m.put("email", e);
            m.put("phone", ph);
            m.put("phoneNormalized", ph.replaceAll("[^0-9+]", ""));
            m.put("neighborhoodId", hoodId);
            m.put("profileImageBase64", "");
            m.put("ratingAverage", 0.0);
            m.put("ratingCount", 0L);
            m.put("isActive", true);
            m.put("isVerified", false);
            m.put("notificationsEnabled", true);
            m.put("role", "user");
            m.put("createdAt", FieldValue.serverTimestamp());
            m.put("updatedAt", FieldValue.serverTimestamp());
            FirebaseAuth.getInstance().setLanguageCode("ar");
            u.sendEmailVerification();
            db.collection("users").document(u.getUid()).set(m).addOnSuccessListener(x -> {
                Data.ensurePublicProfile(db, u.getUid());
                Data.notify(db, u.getUid(), u.getUid(), "system", "مرحبا بك في نفع",
                        "أضف أول أداة لديك وساعد جيرانك، أو ابحث عما تحتاجه في حيك.", "");
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }).addOnFailureListener(x -> {
                Ui.busy(submit, false, null);
                Ui.toast(this, "تم إنشاء الحساب لكن تعذر حفظ الملف الشخصي، حاول تسجيل الدخول");
            });
        }).addOnFailureListener(x -> { Ui.busy(submit, false, null); Ui.toast(this, Data.authError(x)); });
    }
}
