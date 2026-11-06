package com.example.nafa;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReviewsActivity extends BaseActivity {
    private LinearLayout body;
    private String userId, userName;

    @Override
    protected void onReady(Bundle saved) {
        userId = getIntent().getStringExtra("userId");
        userName = getIntent().getStringExtra("userName");
        boolean mine = userId == null || userId.equals(uid);
        if (mine) userId = uid;
        Ui.Screen s = Ui.screen(this, mine ? "تقييماتي" : "تقييمات " + Data.firstName(userName), true);
        body = s.body;
        body.addView(Ui.loading(this));
        load(mine);
    }

    private void load(boolean mine) {
        Data.forgetRating(userId);
        Data.user(db, userId, u -> db.collection("reviews").whereEqualTo("reviewedUserId", userId).get()
                .addOnSuccessListener(q -> {
                    List<DocumentSnapshot> list = new ArrayList<>(q.getDocuments());
                    Collections.sort(list, (x, y) -> Long.compare(Data.millis(y, "createdAt"), Data.millis(x, "createdAt")));
                    render(u, list, mine);
                })
                .addOnFailureListener(e -> {
                    body.removeAllViews();
                    body.addView(Ui.empty(this, R.drawable.ic_error_outline, "تعذر تحميل التقييمات", "تحقق من الاتصال ثم حاول مجددا"));
                }));
    }

    private void render(DocumentSnapshot u, List<DocumentSnapshot> list, boolean mine) {
        body.removeAllViews();
        int[] dist = new int[6];
        double sum = 0;
        for (DocumentSnapshot d : list) {
            int r = (int) Math.round(Data.dbl(d.getDouble("rating")));
            r = Math.max(1, Math.min(5, r));
            dist[r]++;
            sum += Data.dbl(d.getDouble("rating"));
        }
        double avg = list.isEmpty() ? 0 : sum / list.size();

        LinearLayout card = Ui.card(this);
        card.setBackground(Ui.shape(this, Ui.WHITE, 10, Ui.LINE, 1));
        LinearLayout who = Ui.row(this);
        String name = u == null ? "" : Data.str(u, "fullName");
        if (name.isEmpty() && userName != null) name = userName;
        who.addView(Ui.avatar(this, name, u == null ? null : u.getString("profileImageBase64"), 48));
        LinearLayout wt = Ui.col(this);
        wt.addView(Ui.lines(Ui.text(this, name.isEmpty() ? "مستخدم نفع" : name, 17, Ui.INK, true), 1));
        String hood = u == null ? "" : Data.hoodName(u.getString("neighborhoodId"));
        if (!hood.isEmpty()) wt.addView(Ui.text(this, "حي " + hood, 13, Ui.MUTED, false));
        who.addView(wt, Ui.m(this, Ui.lp(this, 0, Ui.WRAP, 1), 12, 0, 0, 0));
        card.addView(who);
        card.addView(Ui.divider(this, 0), Ui.m(this, Ui.lp(this, Ui.MATCH, 1), 0, 14, 0, 14));

        LinearLayout sum_ = Ui.row(this);
        LinearLayout big = Ui.col(this);
        big.setGravity(Gravity.CENTER_HORIZONTAL);
        big.addView(Ui.text(this, list.isEmpty() ? "—" : Data.rating(avg), 42, Ui.INK, true));
        big.addView(Ui.stars(this, avg, 16));
        big.addView(Ui.text(this, Data.count(list.size(), "تقييم واحد", "تقييمان", "تقييمات", "تقييما"), 12, Ui.MUTED, false),
                Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 0, 4, 0, 0));
        sum_.addView(big, Ui.lp(this, 120, Ui.WRAP));
        LinearLayout bars = Ui.col(this);
        int max = 1;
        for (int i = 1; i <= 5; i++) max = Math.max(max, dist[i]);
        for (int i = 5; i >= 1; i--) bars.addView(bar(i, dist[i], list.size()), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 2, 0, 2));
        sum_.addView(bars, Ui.m(this, Ui.lp(this, 0, Ui.WRAP, 1), 12, 0, 0, 0));
        card.addView(sum_);
        body.addView(card, Ui.stack(this));

        if (list.isEmpty()) {
            body.addView(Ui.empty(this, R.drawable.ic_star_outline, "لا توجد تقييمات بعد",
                    mine ? "بعد إتمام أول إعارة سيتمكن جيرانك من تقييمك" : "لم يحصل هذا المستخدم على تقييمات حتى الآن"));
            return;
        }
        body.addView(Ui.section(this, "آراء الجيران", null, null));
        for (DocumentSnapshot d : list) body.addView(review(d), Ui.stack(this));
    }

    private View bar(int star, int n, int total) {
        LinearLayout r = Ui.row(this);
        TextView lbl = Ui.medium(this, String.valueOf(star), 12, Ui.INK);
        r.addView(lbl, Ui.lp(this, 12, Ui.WRAP));
        r.addView(Ui.iv(this, R.drawable.ic_star, Ui.AMBER, 12), Ui.m(this, Ui.lp(this, 12, 12), 2, 0, 8, 0));
        LinearLayout track = Ui.row(this);
        track.setBackground(Ui.shape(this, Ui.FIELD, 4));
        float w = total == 0 ? 0 : (float) n / total;
        View fill = new View(this);
        fill.setBackground(Ui.shape(this, Ui.AMBER, 4));
        track.addView(fill, Ui.lp(this, 0, 8, w));
        View rest = new View(this);
        track.addView(rest, Ui.lp(this, 0, 8, 1 - w));
        track.setWeightSum(1f);
        r.addView(track, Ui.lp(this, 0, 8, 1));
        r.addView(Ui.text(this, String.valueOf(n), 12, Ui.MUTED, false), Ui.m(this, Ui.lp(this, 22, Ui.WRAP), 8, 0, 0, 0));
        return r;
    }

    private View review(DocumentSnapshot d) {
        return reviewCard(this, db, d);
    }

    static View reviewCard(android.app.Activity a, com.google.firebase.firestore.FirebaseFirestore db, DocumentSnapshot d) {
        LinearLayout c = Ui.card(a);
        LinearLayout top = Ui.row(a);
        String rn = Data.str(d, "reviewerName");
        android.widget.FrameLayout av = new android.widget.FrameLayout(a);
        av.addView(Ui.avatar(a, rn, null, 40));
        top.addView(av, Ui.lp(a, 40, 40));
        String rid = d.getString("reviewerId");
        Data.user(db, rid, u -> {
            String img = u == null ? null : u.getString("profileImageBase64");
            if (img != null && !img.isEmpty()) { av.removeAllViews(); av.addView(Ui.avatar(a, rn, img, 40)); }
        });
        LinearLayout t = Ui.col(a);
        t.addView(Ui.lines(Ui.text(a, rn.isEmpty() ? "جار من نفع" : rn, 15, Ui.INK, true), 1));
        String role = "lender".equals(d.getString("role")) ? "أعاره أداة" : "استعار منه";
        String tool = Data.str(d, "toolName");
        t.addView(Ui.lines(Ui.text(a, role + (tool.isEmpty() ? "" : " · " + tool), 12, Ui.MUTED, false), 1));
        top.addView(t, Ui.m(a, Ui.lp(a, 0, Ui.WRAP, 1), 10, 0, 8, 0));
        LinearLayout right = Ui.col(a);
        right.setGravity(Gravity.END);
        right.addView(Ui.stars(a, Data.dbl(d.getDouble("rating")), 14));
        right.addView(Ui.text(a, Data.ago(Data.millis(d, "createdAt")), 11, Ui.MUTED, false), Ui.m(a, Ui.lp(a, Ui.WRAP, Ui.WRAP), 0, 4, 0, 0));
        top.addView(right);
        c.addView(top);
        Ui.tap(top, null, 12, v -> Data.openProfile(a, rid, rn));
        String cm = Data.str(d, "comment");
        if (!cm.isEmpty()) {
            TextView ct = Ui.text(a, cm, 14, 0xFF4A4F4D, false);
            ct.setBackground(Ui.shape(a, Ui.CANVAS, 10));
            Ui.pad(ct, 12, 10, 12, 10);
            c.addView(ct, Ui.m(a, Ui.lp(a, Ui.MATCH, Ui.WRAP), 0, 12, 0, 0));
        }
        return c;
    }
}
