package com.example.nafa;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.List;
import java.util.Map;

public class EditProfileActivity extends BaseActivity {
    private static final int PICK = 31;

    private FrameLayout avatarBox;
    private Ui.Field name, phone, email;
    private Ui.Select hood;
    private TextView save;
    private List<String> hoodIds;
    private String image = "";
    private boolean imageChanged;

    @Override
    protected void onReady(Bundle saved) {
        Ui.Screen s = Ui.screen(this, "تعديل الملف الشخصي", true);
        LinearLayout b = s.body;

        LinearLayout top = Ui.col(this);
        top.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout holder = new FrameLayout(this);
        avatarBox = new FrameLayout(this);
        holder.addView(avatarBox, new FrameLayout.LayoutParams(Ui.dp(this, 104), Ui.dp(this, 104), Gravity.CENTER));
        FrameLayout cam = Ui.iconButton(this, R.drawable.ic_photo_camera, Ui.WHITE, Ui.GREEN, 36, v -> pick());
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(Ui.dp(this, 36), Ui.dp(this, 36), Gravity.BOTTOM | Gravity.START);
        holder.addView(cam, cp);
        top.addView(holder, Ui.lp(this, 110, 110));
        LinearLayout photoActs = Ui.row(this);
        photoActs.setGravity(Gravity.CENTER);
        photoActs.addView(Ui.link(this, "تغيير الصورة", v -> pick()));
        TextView remove = Ui.link(this, "إزالة", v -> { image = ""; imageChanged = true; drawAvatar(); });
        remove.setTextColor(Ui.WARN);
        photoActs.addView(remove, Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 12, 0, 0, 0));
        top.addView(photoActs, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 4, 0, 0));
        b.addView(top, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 8, 0, 4));

        b.addView(Ui.label(this, "الاسم الكامل"));
        name = Ui.field(this, "اكتب اسمك", R.drawable.ic_person, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        b.addView(name.box, Ui.lp(this, Ui.MATCH, Ui.WRAP));

        b.addView(Ui.label(this, "رقم الجوال"));
        phone = Ui.field(this, "05xxxxxxxx", R.drawable.ic_phone, InputType.TYPE_CLASS_PHONE);
        phone.edit.setTextDirection(View.TEXT_DIRECTION_LTR);
        b.addView(phone.box, Ui.lp(this, Ui.MATCH, Ui.WRAP));

        b.addView(Ui.label(this, "الحي"));
        hoodIds = Data.hoodIds();
        hood = Ui.select(this, "اختر حيك", R.drawable.ic_location_on, Data.hoodNames().toArray(new String[0]));
        b.addView(hood.box, Ui.lp(this, Ui.MATCH, Ui.WRAP));

        b.addView(Ui.label(this, "البريد الإلكتروني"));
        email = Ui.field(this, "", R.drawable.ic_email, InputType.TYPE_CLASS_TEXT);
        email.edit.setEnabled(false);
        email.edit.setTextColor(Ui.MUTED);
        email.edit.setTextDirection(View.TEXT_DIRECTION_LTR);
        b.addView(email.box, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        b.addView(Ui.text(this, "لا يمكن تغيير البريد الإلكتروني المرتبط بالحساب", 12, Ui.MUTED, false),
                Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 4, 6, 4, 0));

        LinearLayout foot = Ui.footer(this, s);
        save = Ui.button(this, "حفظ التغييرات", Ui.PRIMARY, R.drawable.ic_check);
        save.setOnClickListener(v -> save());
        foot.addView(save, Ui.lp(this, 0, 52, 1));
        Ui.enable(save, false);

        Data.loadHoods(db, () -> {
            hoodIds = Data.hoodIds();
            int keep = hood.index;
            hood.items = Data.hoodNames().toArray(new String[0]);
            hood.set(keep);
            load();
        });
    }

    private void load() {
        Data.forgetUser(uid);
        db.collection("users").document(uid).get().addOnSuccessListener(d -> {
            name.set(Data.str(d, "fullName"));
            phone.set(Data.str(d, "phone"));
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            String em = Data.str(d, "email");
            email.set(em.isEmpty() && u != null ? u.getEmail() : em);
            hood.set(hoodIds.indexOf(Data.str(d, "neighborhoodId")));
            if (!imageChanged) image = Data.str(d, "profileImageBase64");
            drawAvatar();
            Ui.enable(save, true);
        }).addOnFailureListener(e -> {
            Ui.toast(this, "تعذر تحميل بياناتك، تحقق من الاتصال");
            Ui.enable(save, true);
        });
    }

    private void drawAvatar() {
        avatarBox.removeAllViews();
        View av = Ui.avatar(this, name.value(), image, 104);
        avatarBox.addView(av, new FrameLayout.LayoutParams(-1, -1));
        avatarBox.setBackground(Ui.oval(Ui.WHITE));
        Ui.pad(avatarBox, 3);
    }

    @SuppressWarnings("deprecation")
    private void pick() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(i, "اختر صورة الملف الشخصي"), PICK);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != PICK || res != RESULT_OK || data == null || data.getData() == null) return;
        Ui.busy(save, true, "جار تجهيز الصورة...");
        Img.encodeAsync(this, data.getData(), Img.AVATAR_PX, Img.AVATAR_MAX, b64 -> {
            Ui.busy(save, false, null);
            if (b64 == null) { Ui.toast(this, "تعذر قراءة الصورة، جرب صورة أخرى"); return; }
            image = b64;
            imageChanged = true;
            drawAvatar();
        });
    }

    private void save() {
        String n = name.value(), p = phone.value().replaceAll("[\\s-]", "");
        if (n.length() < 3) { name.error("اكتب اسمك الكامل (3 أحرف على الأقل)"); return; }
        if (!p.matches("^(05\\d{8}|\\+?9665\\d{8}|5\\d{8})$")) { phone.error("رقم جوال سعودي غير صحيح، مثال: 0501234567"); return; }
        if (hood.index < 0) { Ui.toast(this, "اختر الحي الذي تسكن فيه"); return; }
        Ui.busy(save, true, "جار الحفظ...");
        String hoodId = hoodIds.get(hood.index);
        Map<String, Object> m = Data.map(
                "fullName", n,
                "phone", p,
                "phoneNormalized", p.replaceAll("[^0-9+]", ""),
                "neighborhoodId", hoodId,
                "updatedAt", FieldValue.serverTimestamp());
        if (imageChanged) m.put("profileImageBase64", image);
        db.collection("users").document(uid).update(m).addOnSuccessListener(v -> {
            Data.forgetUser(uid);
            Data.ensurePublicProfile(db, uid);
            syncTools(n, hoodId);
            Ui.toast(this, "تم حفظ بياناتك");
            finish();
        }).addOnFailureListener(e -> {
            Ui.busy(save, false, null);
            Ui.toast(this, "تعذر الحفظ، حاول مرة أخرى");
        });
    }

    private void syncTools(String n, String hoodId) {
        db.collection("tools").whereEqualTo("ownerId", uid).get().addOnSuccessListener(q -> {
            if (q.isEmpty()) return;
            com.google.firebase.firestore.WriteBatch b = db.batch();
            for (DocumentSnapshot d : q.getDocuments())
                b.update(d.getReference(), Data.map("ownerName", n, "neighborhoodId", hoodId));
            b.commit();
        });
    }
}
