package com.example.nafa;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AddToolActivity extends BaseActivity {
    private static final int PICK = 20, MAX_IMAGES = 4;

    private String toolId, thumbImage = "", status = "available";
    private final List<String> images = new ArrayList<>();
    private boolean imagesChanged, busy;
    private Ui.Field name, desc, notes, place;
    private Ui.Select condition, unit;
    private int category = -1, maxValue = 3;
    private final List<TextView> catChips = new ArrayList<>();
    private ImageView preview;
    private LinearLayout pickHint, thumbs;
    private TextView changeChip, save, maxText, photoCount;
    private Switch available;

    @Override
    protected void onReady(Bundle saved) {
        toolId = getIntent().getStringExtra("toolId");
        boolean edit = toolId != null;
        Ui.Screen s = Ui.screen(this, edit ? "تعديل الأداة" : "إضافة أداة جديدة", true);
        LinearLayout b = s.body;

        LinearLayout ph = Ui.row(this);
        ph.addView(Ui.medium(this, "صور الأداة", 14, Ui.INK), Ui.lp(this, 0, Ui.WRAP, 1));
        photoCount = Ui.text(this, "", 12, Ui.MUTED, false);
        ph.addView(photoCount);
        b.addView(ph, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 4, 4, 4, 8));

        FrameLayout photo = new FrameLayout(this);
        photo.setClipToOutline(true);
        pickHint = Ui.col(this);
        pickHint.setGravity(Gravity.CENTER);
        pickHint.addView(Ui.badgeIcon(this, R.drawable.ic_add_a_photo, Ui.GREEN, Ui.WHITE, 60, 28));
        pickHint.addView(Ui.center(Ui.text(this, "أضف صورا واضحة للأداة", 15, Ui.GREEN_DARK, true)), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 10, 0, 2));
        pickHint.addView(Ui.center(Ui.text(this, "حتى 4 صور · الصور الجيدة تزيد فرص الاستعارة", 12, Ui.GREEN_DARK, false)), Ui.lp(this, Ui.MATCH, Ui.WRAP));
        photo.addView(pickHint, new FrameLayout.LayoutParams(-1, -1));
        preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setVisibility(View.GONE);
        photo.addView(preview, new FrameLayout.LayoutParams(-1, -1));
        changeChip = Ui.chip(this, "إضافة صور", Ui.WHITE, 0x99000000, R.drawable.ic_photo_camera);
        changeChip.setVisibility(View.GONE);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START);
        cp.setMargins(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 12));
        photo.addView(changeChip, cp);
        Ui.tap(photo, Ui.shape(this, Ui.MINT, 10, 0x6600852B, 1.5f), 10, v -> {
            if (images.isEmpty() || images.size() < MAX_IMAGES) pick();
            else Cards.viewer(this, images, 0);
        });
        int ph1 = Ui.clamp(Math.round(Ui.screenDp(this) * 0.55f), 180, 260);
        b.addView(photo, Ui.lp(this, Ui.MATCH, ph1));

        thumbs = Ui.row(this);
        b.addView(Ui.hscroll(this, thumbs), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 10, 0, 0));

        b.addView(Ui.label(this, "اسم الأداة"));
        name = Ui.field(this, "مثال: مثقاب كهربائي بوش", R.drawable.ic_handyman, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        name.edit.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(60)});
        b.addView(name.box, Ui.lp(this, Ui.MATCH, Ui.WRAP));

        b.addView(Ui.label(this, "التصنيف"));
        LinearLayout cats = Ui.row(this);
        for (int i = 0; i < Data.CAT_IDS.length; i++) {
            TextView c = Ui.choice(this, Data.CAT_NAMES[i], Data.CAT_ICONS[i]);
            final int idx = i;
            c.setOnClickListener(v -> setCategory(idx));
            catChips.add(c);
            cats.addView(c, Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 0, 0, 8, 0));
        }
        b.addView(Ui.hscroll(this, cats), Ui.lp(this, Ui.MATCH, Ui.WRAP));

        b.addView(Ui.label(this, "الوصف"));
        desc = Ui.area(this, "صف الأداة وحالتها وما تحتاجه لتشغيلها...", 4);
        desc.edit.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(800)});
        b.addView(desc.box, Ui.lp(this, Ui.MATCH, Ui.WRAP));

        b.addView(Ui.label(this, "حالة الأداة"));
        condition = Ui.select(this, "اختر حالة الأداة", R.drawable.ic_verified, Data.CONDITION_NAMES);
        b.addView(condition.box, Ui.lp(this, Ui.MATCH, Ui.WRAP));

        b.addView(Ui.label(this, "أقصى مدة للإعارة"));
        LinearLayout dur = Ui.row(this);
        LinearLayout stepper = Ui.row(this);
        stepper.setBackground(Ui.shape(this, Ui.FIELD, 10));
        stepper.setMinimumHeight(Ui.dp(this, 54));
        Ui.pad(stepper, 6, 0, 6, 0);
        stepper.addView(Ui.iconButton(this, R.drawable.ic_add, Ui.GREEN, Ui.WHITE, 40, v -> setMax(maxValue + 1)));
        maxText = Ui.center(Ui.text(this, "", 18, Ui.INK, true));
        stepper.addView(maxText, Ui.lp(this, 0, Ui.WRAP, 1));
        TextView minus = Ui.center(Ui.text(this, "−", 22, Ui.GREEN, true));
        FrameLayout mb = new FrameLayout(this);
        mb.addView(minus, new FrameLayout.LayoutParams(-1, -1));
        Ui.tap(mb, Ui.oval(Ui.WHITE), 10, v -> setMax(maxValue - 1));
        stepper.addView(mb, Ui.lp(this, 40, 40));
        dur.addView(stepper, Ui.m(this, Ui.lp(this, 0, Ui.WRAP, 1), 0, 0, 6, 0));
        unit = Ui.select(this, "الوحدة", R.drawable.ic_schedule, Data.UNIT_NAMES);
        unit.set(1);
        dur.addView(unit.box, Ui.m(this, Ui.lp(this, 0, Ui.WRAP, 1), 6, 0, 0, 0));
        b.addView(dur, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        setMax(3);

        b.addView(Ui.label(this, "مكان الاستلام (اختياري)"));
        place = Ui.field(this, "مثال: بجوار مسجد الحي", R.drawable.ic_location_on, InputType.TYPE_CLASS_TEXT);
        b.addView(place.box, Ui.lp(this, Ui.MATCH, Ui.WRAP));

        b.addView(Ui.label(this, "شروط الإعارة (اختياري)"));
        notes = Ui.area(this, "مثال: يرجى إعادتها نظيفة مع الشاحن", 2);
        b.addView(notes.box, Ui.lp(this, Ui.MATCH, Ui.WRAP));

        LinearLayout av = Ui.row(this);
        av.setBackground(Ui.shape(this, Ui.WHITE, 10, Ui.LINE, 1));
        Ui.pad(av, 16, 10, 12, 10);
        LinearLayout avt = Ui.col(this);
        avt.addView(Ui.medium(this, "متاحة للإعارة الآن", 15, Ui.INK));
        avt.addView(Ui.text(this, "أوقفها مؤقتا إن لم تكن متاحة", 12, Ui.MUTED, false));
        av.addView(avt, Ui.lp(this, 0, Ui.WRAP, 1));
        available = Ui.toggle(this);
        available.setChecked(true);
        av.addView(available);
        av.setOnClickListener(v -> { if (available.isEnabled()) available.toggle(); });
        b.addView(av, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 16, 0, 0));

        LinearLayout foot = Ui.footer(this, s);
        save = Ui.button(this, edit ? "حفظ التعديلات" : "نشر الأداة", Ui.PRIMARY, R.drawable.ic_check);
        save.setOnClickListener(v -> save());
        foot.addView(save, Ui.lp(this, 0, 54, 1));

        renderPhotos();
        if (edit) loadExisting();
    }

    private void setCategory(int i) {
        category = i;
        for (int k = 0; k < catChips.size(); k++) Ui.setChoice(catChips.get(k), k == i);
    }

    private void setMax(int v) {
        maxValue = Math.max(1, Math.min(30, v));
        maxText.setText(String.valueOf(maxValue));
    }

    private void loadExisting() {
        Ui.enable(save, false);
        db.collection("tools").document(toolId).get().addOnSuccessListener(d -> {
            if (!d.exists() || !uid.equals(d.getString("ownerId"))) { Ui.toast(this, "لا يمكنك تعديل هذه الأداة"); finish(); return; }
            name.set(d.getString("toolName"));
            desc.set(d.getString("description"));
            notes.set(d.getString("details"));
            place.set(d.getString("locationText"));
            setCategory(Data.catIndex(d.getString("categoryId")));
            String cond = d.getString("condition");
            for (int i = 0; i < Data.CONDITION_IDS.length; i++) if (Data.CONDITION_IDS[i].equals(cond)) condition.set(i);
            if (d.getLong("maxDurationValue") != null) setMax((int) Data.num(d.getLong("maxDurationValue")));
            unit.set(Data.unitIndex(d.getString("maxDurationUnit")));
            status = d.getString("status") == null ? "available" : d.getString("status");
            available.setChecked(!"paused".equals(status));
            if ("borrowed".equals(status)) available.setEnabled(false);
            thumbImage = Data.str(d, "thumbBase64");
            if (!thumbImage.isEmpty()) { preview.setVisibility(View.VISIBLE); pickHint.setVisibility(View.INVISIBLE); Img.load(preview, thumbImage, Img.THUMB_PX); }
            db.collection("tool_images").whereEqualTo("toolId", toolId).get().addOnSuccessListener(q -> {
                images.clear();
                images.addAll(Cards.sortedImages(q.getDocuments()));
                renderPhotos();
                Ui.enable(save, true);
            }).addOnFailureListener(e -> Ui.enable(save, true));
        }).addOnFailureListener(e -> { Ui.toast(this, "تعذر تحميل الأداة"); finish(); });
    }

    private void renderPhotos() {
        boolean has = !images.isEmpty();
        preview.setVisibility(has ? View.VISIBLE : View.GONE);
        pickHint.setVisibility(has ? View.INVISIBLE : View.VISIBLE);
        changeChip.setVisibility(has ? View.VISIBLE : View.GONE);
        changeChip.setText(images.size() < MAX_IMAGES ? "إضافة صور" : "عرض الصور");
        photoCount.setText(images.size() + " / " + MAX_IMAGES);
        if (has) Img.load(preview, images.get(0), Ui.dp(this, 360));

        thumbs.removeAllViews();
        if (!has) { ((View) thumbs.getParent()).setVisibility(View.GONE); return; }
        ((View) thumbs.getParent()).setVisibility(View.VISIBLE);
        for (int i = 0; i < images.size(); i++) {
            final int idx = i;
            FrameLayout t = new FrameLayout(this);
            t.setBackground(Ui.shape(this, Ui.MINT, 10, i == 0 ? Ui.GREEN : Ui.LINE, i == 0 ? 2f : 1f));
            t.setClipToOutline(true);
            ImageView im = new ImageView(this);
            im.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Img.load(im, images.get(i), Img.THUMB_PX);
            t.addView(im, new FrameLayout.LayoutParams(-1, -1));
            if (i == 0) {
                TextView main = Ui.chip(this, "رئيسية", Ui.WHITE, Ui.GREEN, 0);
                main.setTextSize(10);
                t.addView(main, new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));
            }
            FrameLayout x = Ui.iconButton(this, R.drawable.ic_close, Ui.WHITE, 0xAA000000, 24, v -> {
                images.remove(idx);
                imagesChanged = true;
                renderPhotos();
            });
            FrameLayout.LayoutParams xp = new FrameLayout.LayoutParams(Ui.dp(this, 24), Ui.dp(this, 24), Gravity.TOP | Gravity.END);
            xp.setMargins(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), 0);
            t.addView(x, xp);
            t.setOnClickListener(v -> {
                if (idx == 0) { Cards.viewer(this, images, 0); return; }
                String m = images.remove(idx);
                images.add(0, m);
                imagesChanged = true;
                renderPhotos();
                Ui.toast(this, "أصبحت الصورة الرئيسية");
            });
            thumbs.addView(t, Ui.m(this, Ui.lp(this, 74, 74), 0, 0, 8, 0));
        }
        if (images.size() < MAX_IMAGES) {
            LinearLayout add = Ui.col(this);
            add.setGravity(Gravity.CENTER);
            add.addView(Ui.iv(this, R.drawable.ic_add, Ui.GREEN, 24));
            add.addView(Ui.text(this, "إضافة", 11, Ui.GREEN_DARK, true));
            Ui.tap(add, Ui.shape(this, Ui.WHITE, 10, 0x6600852B, 1.5f), 10, v -> pick());
            thumbs.addView(add, Ui.lp(this, 74, 74));
        }
    }

    @SuppressWarnings("deprecation")
    private void pick() {
        if (busy) return;
        if (images.size() >= MAX_IMAGES) { Ui.toast(this, "الحد الأقصى 4 صور، احذف صورة لإضافة أخرى"); return; }
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try {
            startActivityForResult(Intent.createChooser(i, "اختر صور الأداة"), PICK);
        } catch (Exception e) {
            Ui.toast(this, "لا يوجد تطبيق لاختيار الصور");
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != PICK || res != RESULT_OK || data == null) return;
        List<Uri> uris = new ArrayList<>();
        ClipData clip = data.getClipData();
        if (clip != null) for (int i = 0; i < clip.getItemCount(); i++) uris.add(clip.getItemAt(i).getUri());
        else if (data.getData() != null) uris.add(data.getData());
        int room = MAX_IMAGES - images.size();
        if (uris.size() > room) Ui.toast(this, "سيتم إضافة " + room + " فقط (الحد 4 صور)");
        while (uris.size() > room) uris.remove(uris.size() - 1);
        if (uris.isEmpty()) return;
        busy = true;
        Ui.busy(save, true, "جار تجهيز الصور...");
        encodeNext(uris, 0);
    }

    private void encodeNext(List<Uri> uris, int i) {
        if (i >= uris.size()) {
            busy = false;
            Ui.busy(save, false, null);
            renderPhotos();
            return;
        }
        Img.encodeAsync(this, uris.get(i), Img.FULL_PX, Img.FULL_MAX, full -> {
            if (full == null) Ui.toast(this, "تعذر قراءة إحدى الصور، جرب صورة أخرى");
            else { images.add(full); imagesChanged = true; }
            encodeNext(uris, i + 1);
        });
    }

    private void save() {
        if (busy) return;
        String n = name.value(), d = desc.value();
        if (images.isEmpty()) { Ui.toast(this, "أضف صورة واحدة على الأقل ليسهل على جيرانك التعرف على الأداة"); return; }
        if (n.length() < 2) { name.error("أدخل اسم الأداة"); return; }
        if (category < 0) { Ui.toast(this, "اختر تصنيف الأداة"); return; }
        if (d.length() < 10) { desc.error("اكتب وصفا من 10 أحرف على الأقل"); return; }
        busy = true;
        Ui.busy(save, true, "جار الحفظ...");
        if (imagesChanged || thumbImage.isEmpty()) {
            Img.thumbAsync(this, images.get(0), t -> { thumbImage = t == null ? "" : t; write(n, d); });
        } else write(n, d);
    }

    private void write(String n, String d) {
        Data.user(db, uid, me -> {
            String newStatus = "borrowed".equals(status) ? "borrowed" : (available.isChecked() ? "available" : "paused");
            Map<String, Object> x = new HashMap<>();
            x.put("ownerId", uid);
            x.put("ownerName", Data.str(me, "fullName"));
            x.put("toolName", n);
            x.put("description", d);
            x.put("categoryId", Data.CAT_IDS[category]);
            x.put("condition", condition.index >= 0 ? Data.CONDITION_IDS[condition.index] : "good");
            x.put("maxDurationValue", (long) maxValue);
            x.put("maxDurationUnit", Data.UNIT_IDS[unit.index < 0 ? 1 : unit.index]);
            x.put("locationText", place.value());
            x.put("details", notes.value());
            x.put("neighborhoodId", Data.str(me, "neighborhoodId"));
            x.put("status", newStatus);
            x.put("isAvailable", "available".equals(newStatus));
            x.put("imageCount", (long) images.size());
            x.put("thumbBase64", thumbImage);
            x.put("updatedAt", FieldValue.serverTimestamp());

            WriteBatch b = db.batch();
            DocumentReference ref;
            if (toolId == null) {
                ref = db.collection("tools").document();
                x.put("createdAt", FieldValue.serverTimestamp());
                x.put("borrowCount", 0L);
                b.set(ref, x);
            } else {
                ref = db.collection("tools").document(toolId);
                b.update(ref, x);
            }
            Set<String> keep = new HashSet<>();
            if (imagesChanged || toolId == null) {
                for (int i = 0; i < images.size(); i++) {
                    Map<String, Object> im = new HashMap<>();
                    im.put("toolId", ref.getId());
                    im.put("ownerId", uid);
                    im.put("imageBase64", images.get(i));
                    im.put("imageOrder", (long) i);
                    im.put("isPrimary", i == 0);
                    im.put("createdAt", FieldValue.serverTimestamp());
                    String docId = ref.getId() + "_" + i;
                    keep.add(docId);
                    b.set(db.collection("tool_images").document(docId), im);
                }
            }
            boolean cleanup = imagesChanged && toolId != null;
            b.commit().addOnSuccessListener(v -> {
                if (cleanup) cleanupOldImages(ref.getId(), keep);
                Ui.toast(this, toolId == null ? "تم نشر الأداة بنجاح" : "تم حفظ التعديلات");
                if (toolId == null) Cards.openTool(this, ref.getId());
                finish();
            }).addOnFailureListener(e -> {
                busy = false;
                Ui.busy(save, false, null);
                Ui.toast(this, "تعذر الحفظ، تحقق من الاتصال وحاول مجددا");
            });
        });
    }

    private void cleanupOldImages(String id, Set<String> keep) {
        db.collection("tool_images").whereEqualTo("toolId", id).get().addOnSuccessListener(q -> {
            for (DocumentSnapshot d : q.getDocuments()) if (!keep.contains(d.getId())) d.getReference().delete();
        });
    }
}
