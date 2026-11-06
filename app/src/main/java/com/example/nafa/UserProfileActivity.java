package com.example.nafa;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserProfileActivity extends BaseActivity {
    private String userId, userName;
    private LinearLayout body;
    private boolean mine;

    @Override
    protected void onReady(Bundle saved) {
        userId = getIntent().getStringExtra("userId");
        userName = getIntent().getStringExtra("userName");
        if (userId == null || userId.isEmpty()) { finish(); return; }
        mine = userId.equals(uid);
        bars(Ui.GREEN, Ui.CANVAS, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!ready || userId == null) return;
        Data.forgetUser(userId);
        Data.user(db, userId, this::render);
    }

    private void render(DocumentSnapshot u) {
        if (u == null) {
            Ui.Screen s = Ui.screen(this, "الملف الشخصي", true);
            bars(Ui.CANVAS, Ui.CANVAS, true);
            s.body.addView(Ui.empty(this, R.drawable.ic_person, "هذا الحساب غير متاح", "قد يكون الملف العام غير منشأ بعد أو تعذر الاتصال"));
            return;
        }
        String name = Data.str(u, "fullName");
        if (name.isEmpty()) name = userName == null || userName.isEmpty() ? "مستخدم نفع" : userName;
        final String fname = name;

        ScrollView sc = Ui.scroll(this);
        sc.setBackgroundColor(Ui.CANVAS);
        LinearLayout col = Ui.col(this);
        sc.addView(col);
        setContentView(sc);

        LinearLayout head = Ui.row(this);
        android.graphics.drawable.GradientDrawable hb = Ui.gradient(this, Ui.GREEN, Ui.GREEN_DARK, 0);
        head.setBackground(hb);
        head.setGravity(Gravity.TOP);
        Ui.pad(head, 12, 10, 12, 20);
        head.addView(Ui.iconButton(this, R.drawable.ic_arrow_forward, Ui.WHITE, 0x26FFFFFF, 42, v -> finish()));
        head.addView(Ui.center(Ui.text(this, mine ? "ملفي العام" : "الملف الشخصي", 18, Ui.WHITE, true)), Ui.lp(this, 0, 42, 1));
        if (!mine) head.addView(Ui.iconButton(this, R.drawable.ic_more_vert, Ui.WHITE, 0x26FFFFFF, 42, v -> Ui.options(this, null,
                new String[]{"مراسلة " + Data.firstName(fname), "الإبلاغ عن المستخدم"},
                new int[]{R.drawable.ic_chat, R.drawable.ic_flag}, new int[]{Ui.GREEN, Ui.WARN}, i -> {
                    if (i == 0) Data.openChat(this, db, uid, userId, fname);
                    else Actions.report(this, db, uid, "user", userId, fname);
                })));
        else head.addView(new View(this), Ui.lp(this, 42, 42));
        col.addView(head, Ui.lp(this, Ui.MATCH, Ui.WRAP));

        body = Ui.col(this);
        Ui.pad(body, 16, 0, 16, 28);
        LinearLayout.LayoutParams bp = Ui.lp(this, Ui.MATCH, Ui.WRAP);
        bp.topMargin = Ui.dp(this, 16);
        col.addView(body, bp);

        LinearLayout card = Ui.card(this);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setBackground(Ui.shape(this, Ui.WHITE, 10, Ui.LINE, 1));
        FrameLayout ring = new FrameLayout(this);
        ring.setBackground(Ui.oval(Ui.MINT));
        Ui.pad(ring, 3);
        ring.addView(Ui.avatar(this, name, u.getString("profileImageBase64"), 86));
        card.addView(ring, Ui.lp(this, 92, 92));
        card.addView(Ui.center(Ui.lines(Ui.text(this, name, 21, Ui.INK, true), 1)), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 10, 0, 4));
        LinearLayout chips = Ui.row(this);
        chips.setGravity(Gravity.CENTER);
        chips.addView(Ui.chip(this, "حي " + Data.hoodName(u.getString("neighborhoodId")), Ui.GREEN_DARK, Ui.MINT, R.drawable.ic_location_on));
        if (Boolean.TRUE.equals(u.getBoolean("isVerified"))) {
            TextView vc = Ui.chip(this, "موثق", Ui.GREEN_DARK, Ui.MINT, R.drawable.ic_verified);
            chips.addView(vc);
            ((LinearLayout.LayoutParams) vc.getLayoutParams()).setMarginStart(Ui.dp(this, 6));
        }
        card.addView(chips);
        long since = Data.millis(u, "createdAt");
        if (since > 0) card.addView(Ui.center(Ui.text(this, "عضو في نفع منذ " + Data.monthYear(since), 12, Ui.MUTED, false)),
                Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 8, 0, 0));

        LinearLayout stats = Ui.row(this);
        stats.setBackground(Ui.shape(this, Ui.CANVAS, 10));
        Ui.pad(stats, 0, 12, 0, 12);
        TextView toolsNum = stat(stats, "—", "أدوات معروضة");
        stats.addView(vline());
        TextView rateNum = stat(stats, "—", "التقييم");
        stats.addView(vline());
        TextView cntNum = stat(stats, "—", "عدد التقييمات");
        Data.forgetRating(userId);
        Data.ratingOf(db, userId, rt -> {
            rateNum.setText(rt[1] == 0 ? "—" : Data.rating(rt[0]) + "");
            cntNum.setText(String.valueOf((long) rt[1]));
        });
        card.addView(stats, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 14, 0, 12));

        LinearLayout acts = Ui.row(this);
        if (mine) {
            TextView edit = Ui.button(this, "تعديل الملف", Ui.PRIMARY, R.drawable.ic_edit);
            edit.setOnClickListener(v -> go(EditProfileActivity.class));
            acts.addView(edit, Ui.lp(this, 0, 48, 1));
        } else {
            TextView chat = Ui.button(this, "مراسلة", Ui.PRIMARY, R.drawable.ic_chat);
            chat.setOnClickListener(v -> Data.openChat(this, db, uid, userId, fname));
            acts.addView(chat, Ui.lp(this, 0, 48, 1));
        }
        TextView rev = Ui.button(this, "التقييمات", Ui.SOFT, R.drawable.ic_star);
        rev.setOnClickListener(v -> openReviews(fname));
        acts.addView(rev, Ui.m(this, Ui.lp(this, 0, 48, 1), 8, 0, 0, 0));
        card.addView(acts, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        body.addView(card, Ui.stack(this));

        body.addView(Ui.section(this, mine ? "أدواتي المعروضة" : "أدوات " + Data.firstName(name), null, null));
        LinearLayout tools = Ui.col(this);
        tools.addView(Ui.loading(this));
        body.addView(tools, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        db.collection("tools").whereEqualTo("ownerId", userId).get().addOnSuccessListener(q -> {
            tools.removeAllViews();
            List<DocumentSnapshot> list = new ArrayList<>();
            for (DocumentSnapshot d : q.getDocuments()) if (!"paused".equals(d.getString("status"))) list.add(d);
            Collections.sort(list, (x, y) -> Long.compare(Data.millis(y, "createdAt"), Data.millis(x, "createdAt")));
            toolsNum.setText(String.valueOf(list.size()));
            if (list.isEmpty()) {
                tools.addView(Ui.empty(this, R.drawable.ic_handyman, "لا توجد أدوات معروضة",
                        mine ? "أضف أدواتك ليستفيد منها جيرانك" : "لم يعرض هذا المستخدم أي أداة حاليا"));
                return;
            }
            Cards.fillGrid(this, db, tools, list);
        }).addOnFailureListener(e -> {
            tools.removeAllViews();
            tools.addView(Ui.empty(this, R.drawable.ic_error_outline, "تعذر تحميل الأدوات", "تحقق من اتصالك بالإنترنت"));
        });

        LinearLayout reviews = Ui.col(this);
        body.addView(reviews, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        db.collection("reviews").whereEqualTo("reviewedUserId", userId).get().addOnSuccessListener(q -> {
            List<DocumentSnapshot> l = new ArrayList<>(q.getDocuments());
            if (l.isEmpty()) return;
            Collections.sort(l, (x, y) -> Long.compare(Data.millis(y, "createdAt"), Data.millis(x, "createdAt")));
            reviews.addView(Ui.section(this, "آراء الجيران", l.size() > 3 ? "عرض الكل" : null, v -> openReviews(fname)));
            for (int i = 0; i < Math.min(3, l.size()); i++) reviews.addView(ReviewsActivity.reviewCard(this, db, l.get(i)), Ui.stack(this));
        });
    }

    private void openReviews(String name) {
        Intent i = new Intent(this, ReviewsActivity.class);
        i.putExtra("userId", userId);
        i.putExtra("userName", name);
        startActivity(i);
    }

    private TextView stat(LinearLayout into, String value, String label) {
        LinearLayout c = Ui.col(this);
        c.setGravity(Gravity.CENTER);
        TextView n = Ui.center(Ui.text(this, value, 18, Ui.GREEN_DARK, true));
        c.addView(n, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        c.addView(Ui.center(Ui.lines(Ui.text(this, label, 12, Ui.MUTED, false), 1)), Ui.lp(this, Ui.MATCH, Ui.WRAP));
        into.addView(c, Ui.lp(this, 0, Ui.WRAP, 1));
        return n;
    }

    private View vline() {
        View v = new View(this);
        v.setBackgroundColor(Ui.LINE);
        v.setLayoutParams(Ui.lp(this, 1, 32));
        return v;
    }
}
