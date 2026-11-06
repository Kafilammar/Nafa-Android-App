package com.example.nafa;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends BaseActivity {
    private Switch notif;
    private boolean binding;

    @Override
    protected void onReady(Bundle saved) {
        Ui.Screen s = Ui.screen(this, "الإعدادات", true);
        LinearLayout b = s.body;
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();

        b.addView(head("الحساب"));
        LinearLayout gp = Ui.group(this);
        gp.addView(Ui.menuRow(this, R.drawable.ic_favorite, "المفضلة", "الأدوات المحفوظة", Ui.WARN, Ui.WARN_SOFT, v -> go(FavoritesActivity.class)));
        gp.addView(Ui.divider(this, 0));
        gp.addView(Ui.menuRow(this, R.drawable.ic_person, "ملفي العام", "كيف يراك جيرانك", Ui.GREEN, Ui.MINT, v -> Data.openProfile(this, uid, "")));
        b.addView(gp, Ui.stack(this));
        LinearLayout g1 = Ui.group(this);
        g1.addView(Ui.menuRow(this, R.drawable.ic_person, "الملف الشخصي", "الاسم والصورة والجوال والحي", Ui.GREEN, Ui.MINT, v -> go(EditProfileActivity.class)));
        g1.addView(Ui.divider(this, 0));
        g1.addView(Ui.menuRow(this, R.drawable.ic_lock, "تغيير كلمة المرور", "حدث كلمة المرور لحماية حسابك", Ui.GREEN, Ui.MINT, v -> changePassword()));
        if (u != null && !u.isEmailVerified()) {
            g1.addView(Ui.divider(this, 0));
            g1.addView(Ui.menuRow(this, R.drawable.ic_mark_email, "توثيق البريد الإلكتروني", "أرسل رابط التوثيق مجددا إلى " + u.getEmail(),
                    0xFF9A6A00, Ui.AMBER_SOFT, v -> u.sendEmailVerification()
                            .addOnSuccessListener(x -> Ui.toast(this, "أرسلنا رابط التوثيق، تحقق من بريدك"))
                            .addOnFailureListener(e -> Ui.toast(this, Data.authError(e)))));
        }
        b.addView(g1, Ui.stack(this));

        b.addView(head("التنبيهات"));
        LinearLayout g2 = Ui.group(this);
        LinearLayout nr = Ui.menuRow(this, R.drawable.ic_notifications, "إشعارات التطبيق", "طلبات الاستعارة والرسائل والتقييمات", Ui.GREEN, Ui.MINT, null);
        notif = Ui.toggle(this);
        notif.setChecked(true);
        nr.addView(notif);
        nr.setOnClickListener(v -> notif.toggle());
        notif.setOnCheckedChangeListener((x, on) -> {
            if (binding) return;
            db.collection("users").document(uid).update("notificationsEnabled", on, "updatedAt", FieldValue.serverTimestamp())
                    .addOnSuccessListener(v -> { Data.forgetUser(uid); Ui.toast(this, on ? "تم تفعيل الإشعارات" : "تم إيقاف الإشعارات"); })
                    .addOnFailureListener(e -> { binding = true; notif.setChecked(!on); binding = false; Ui.toast(this, "تعذر حفظ الإعداد"); });
        });
        g2.addView(nr);
        b.addView(g2, Ui.stack(this));

        b.addView(head("المساعدة والمعلومات"));
        LinearLayout g3 = Ui.group(this);
        g3.addView(Ui.menuRow(this, R.drawable.ic_support_agent, "المساعدة والدعم", "الأسئلة الشائعة ومراسلة فريق نفع", Ui.GREEN, Ui.MINT, v -> go(SupportActivity.class)));
        g3.addView(Ui.divider(this, 0));
        g3.addView(Ui.menuRow(this, R.drawable.ic_description, "الشروط والأحكام", null, Ui.GREEN, Ui.MINT, v -> InfoActivity.open(this, "terms")));
        g3.addView(Ui.divider(this, 0));
        g3.addView(Ui.menuRow(this, R.drawable.ic_privacy_tip, "سياسة الخصوصية", null, Ui.GREEN, Ui.MINT, v -> InfoActivity.open(this, "privacy")));
        g3.addView(Ui.divider(this, 0));
        g3.addView(Ui.menuRow(this, R.drawable.ic_info, "عن نفع", "الإصدار " + InfoActivity.version(this), Ui.GREEN, Ui.MINT, v -> InfoActivity.open(this, "about")));
        g3.addView(Ui.divider(this, 0));
        g3.addView(Ui.menuRow(this, R.drawable.ic_share, "شارك التطبيق", "ادع جيرانك للانضمام", Ui.GREEN, Ui.MINT, v -> Data.shareApp(this)));
        g3.addView(Ui.divider(this, 0));
        g3.addView(Ui.menuRow(this, R.drawable.ic_thumb_up, "قيم نفع على المتجر", null, 0xFF9A6A00, Ui.AMBER_SOFT, v -> Data.rateApp(this)));
        b.addView(g3, Ui.stack(this));

        b.addView(head("الجلسة"));
        LinearLayout g4 = Ui.group(this);
        g4.addView(Ui.menuRow(this, R.drawable.ic_logout, "تسجيل الخروج", u == null ? null : u.getEmail(), Ui.WARN, Ui.WARN_SOFT, v -> logout()));
        g4.addView(Ui.divider(this, 0));
        g4.addView(Ui.menuRow(this, R.drawable.ic_delete_forever, "حذف الحساب نهائيا", "يحذف حسابك وأدواتك وبياناتك", Ui.WARN, Ui.WARN_SOFT, v -> deleteAccount()));
        b.addView(g4, Ui.stack(this));

        b.addView(Ui.center(Ui.text(this, "نفع · الإصدار " + InfoActivity.version(this), 12, Ui.MUTED, false)),
                Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 12, 0, 0));

        LinearLayout adminSlot = Ui.col(this);
        b.addView(adminSlot, 0, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        db.collection("users").document(uid).get().addOnSuccessListener(d -> {
            if (Data.isAdmin(d)) {
                LinearLayout g = Ui.group(this);
                g.addView(Ui.menuRow(this, R.drawable.ic_settings, "لوحة الإدارة", "التحكم الكامل في التطبيق", Ui.GREEN_DEEP, Ui.LIME, v -> go(AdminActivity.class)));
                adminSlot.addView(g, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 4, 0, 4));
            }
            binding = true;
            notif.setChecked(!Boolean.FALSE.equals(d.getBoolean("notificationsEnabled")));
            binding = false;
        });
    }

    private TextView head(String t) {
        TextView h = Ui.text(this, t, 14, Ui.MUTED, true);
        h.setPadding(Ui.dp(this, 4), Ui.dp(this, 14), Ui.dp(this, 4), Ui.dp(this, 8));
        return h;
    }

    private void changePassword() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null || u.getEmail() == null) return;
        LinearLayout c = Ui.col(this);
        Ui.Field cur = Ui.password(this, "كلمة المرور الحالية");
        Ui.Field nw = Ui.password(this, "كلمة المرور الجديدة");
        Ui.Field cf = Ui.password(this, "تأكيد كلمة المرور الجديدة");
        c.addView(cur.box, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 4, 0, 10));
        c.addView(nw.box, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 0, 0, 10));
        c.addView(cf.box, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 0, 0, 6));
        c.addView(Ui.text(this, "استخدم 6 أحرف على الأقل، ويفضل المزج بين الحروف والأرقام.", 12, Ui.MUTED, false), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 4, 0, 4, 14));
        TextView ok = Ui.button(this, "تحديث كلمة المرور", Ui.PRIMARY, R.drawable.ic_check);
        c.addView(ok, Ui.lp(this, Ui.MATCH, 52));
        TextView forgot = Ui.link(this, "نسيت كلمة المرور؟ أرسل رابط إعادة التعيين", null);
        forgot.setGravity(android.view.Gravity.CENTER);
        c.addView(forgot, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 8, 0, 0));
        Dialog d = Ui.sheet(this, "تغيير كلمة المرور", c);
        forgot.setOnClickListener(v -> FirebaseAuth.getInstance().sendPasswordResetEmail(u.getEmail())
                .addOnSuccessListener(x -> { d.dismiss(); Ui.toast(this, "أرسلنا رابط إعادة التعيين إلى " + u.getEmail()); })
                .addOnFailureListener(e -> Ui.toast(this, Data.authError(e))));
        ok.setOnClickListener(v -> {
            String p = nw.value();
            if (cur.value().isEmpty()) { cur.error("أدخل كلمة المرور الحالية"); return; }
            if (p.length() < 6) { nw.error("كلمة المرور 6 أحرف على الأقل"); return; }
            if (!p.equals(cf.value())) { cf.error("كلمتا المرور غير متطابقتين"); return; }
            if (p.equals(cur.value())) { nw.error("اختر كلمة مرور مختلفة عن الحالية"); return; }
            Ui.busy(ok, true, "جار التحديث...");
            AuthCredential cr = EmailAuthProvider.getCredential(u.getEmail(), cur.value());
            u.reauthenticate(cr).continueWithTask(t -> {
                if (!t.isSuccessful()) throw t.getException();
                return u.updatePassword(p);
            }).addOnSuccessListener(x -> {
                d.dismiss();
                Ui.toast(this, "تم تحديث كلمة المرور بنجاح");
            }).addOnFailureListener(e -> {
                Ui.busy(ok, false, null);
                Ui.toast(this, Data.authError(e));
            });
        });
    }

    private void logout() {
        Ui.confirm(this, "تسجيل الخروج", "هل تريد تسجيل الخروج من حسابك؟", "خروج", true, () -> {
            FirebaseAuth.getInstance().signOut();
            Data.clearCache();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }

    private void deleteAccount() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null || u.getEmail() == null) return;
        LinearLayout c = Ui.col(this);
        LinearLayout warn = Ui.row(this);
        warn.setBackground(Ui.shape(this, Ui.WARN_SOFT, 10));
        Ui.pad(warn, 14, 12, 14, 12);
        warn.setGravity(android.view.Gravity.TOP);
        warn.addView(Ui.iv(this, R.drawable.ic_error_outline, Ui.WARN, 22));
        warn.addView(Ui.text(this, "هذا الإجراء نهائي ولا يمكن التراجع عنه. سيتم حذف حسابك وملفك الشخصي وجميع أدواتك وصورها وإشعاراتك ومفضلتك، وإلغاء طلباتك المعلقة.",
                13, Ui.WARN, false), Ui.m(this, Ui.lp(this, 0, Ui.WRAP, 1), 10, 0, 0, 0));
        c.addView(warn, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 4, 0, 14));
        Ui.Field pw = Ui.password(this, "أدخل كلمة المرور للتأكيد");
        c.addView(pw.box, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 0, 0, 14));
        TextView del = Ui.button(this, "حذف حسابي نهائيا", Ui.DANGER, R.drawable.ic_delete_forever);
        c.addView(del, Ui.lp(this, Ui.MATCH, 52));
        Dialog d = Ui.sheet(this, "حذف الحساب", c);
        del.setOnClickListener(v -> {
            if (pw.value().isEmpty()) { pw.error("أدخل كلمة المرور"); return; }
            Ui.busy(del, true, "جار حذف الحساب...");
            u.reauthenticate(EmailAuthProvider.getCredential(u.getEmail(), pw.value()))
                    .continueWithTask(t -> {
                        if (!t.isSuccessful()) throw t.getException();
                        return activeLoans();
                    })
                    .continueWithTask(t -> {
                        if (Boolean.TRUE.equals(t.getResult())) throw new IllegalStateException("active");
                        return wipeData();
                    })
                    .continueWithTask(t -> {
                        if (!t.isSuccessful()) throw t.getException();
                        return u.delete();
                    })
                    .addOnSuccessListener(x -> {
                        d.dismiss();
                        Data.clearCache();
                        Ui.toast(this, "تم حذف حسابك. نتمنى رؤيتك مجددا");
                        Intent i = new Intent(this, LoginActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Ui.busy(del, false, null);
                        if (e instanceof IllegalStateException)
                            Ui.toast(this, "لديك إعارات جارية، أكملها أو ألغها قبل حذف الحساب");
                        else Ui.toast(this, Data.authError(e));
                    });
        });
    }

    private Task<Boolean> activeLoans() {
        Task<QuerySnapshot> a = db.collection("borrow_requests").whereEqualTo("ownerId", uid).whereEqualTo("status", "accepted").get();
        Task<QuerySnapshot> b = db.collection("borrow_requests").whereEqualTo("borrowerId", uid).whereEqualTo("status", "accepted").get();
        List<Task<?>> all = new ArrayList<>();
        all.add(a);
        all.add(b);
        return Tasks.whenAll(all).continueWith(t -> (a.isSuccessful() && !a.getResult().isEmpty())
                || (b.isSuccessful() && !b.getResult().isEmpty()));
    }

    private Task<Void> wipeData() {
        Task<QuerySnapshot> tools = db.collection("tools").whereEqualTo("ownerId", uid).get();
        Task<QuerySnapshot> imgs = db.collection("tool_images").whereEqualTo("ownerId", uid).get();
        Task<QuerySnapshot> notes = db.collection("notifications").whereEqualTo("userId", uid).get();
        Task<QuerySnapshot> pend = db.collection("borrow_requests").whereEqualTo("borrowerId", uid).whereEqualTo("status", "pending").get();
        Task<QuerySnapshot> incoming = db.collection("borrow_requests").whereEqualTo("ownerId", uid).whereEqualTo("status", "pending").get();
        List<Task<?>> all = new ArrayList<>();
        all.add(tools);
        all.add(imgs);
        all.add(notes);
        all.add(pend);
        all.add(incoming);
        return Tasks.whenAll(all).continueWithTask(t -> {
            List<Task<Void>> commits = new ArrayList<>();
            WriteBatch b = db.batch();
            int n = 0;
            List<DocumentSnapshot> docs = new ArrayList<>();
            if (tools.isSuccessful()) docs.addAll(tools.getResult().getDocuments());
            if (imgs.isSuccessful()) docs.addAll(imgs.getResult().getDocuments());
            if (notes.isSuccessful()) docs.addAll(notes.getResult().getDocuments());
            for (DocumentSnapshot d : docs) {
                b.delete(d.getReference());
                if (++n % 400 == 0) { commits.add(b.commit()); b = db.batch(); }
            }
            List<DocumentSnapshot> open = new ArrayList<>();
            if (pend.isSuccessful()) open.addAll(pend.getResult().getDocuments());
            if (incoming.isSuccessful()) open.addAll(incoming.getResult().getDocuments());
            for (DocumentSnapshot d : open) {
                b.update(d.getReference(), "status", d.getString("ownerId") != null && d.getString("ownerId").equals(uid) ? "rejected" : "cancelled",
                        "updatedAt", FieldValue.serverTimestamp());
                if (++n % 400 == 0) { commits.add(b.commit()); b = db.batch(); }
            }
            b.delete(db.collection("users").document(uid));
            commits.add(b.commit());
            return Tasks.whenAll(commits);
        });
    }
}
