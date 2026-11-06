package com.example.nafa;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.Query;


public class AdminActivity extends BaseActivity {
    private LinearLayout body;

    @Override
    protected void onReady(Bundle saved) {
        Ui.Screen s = Ui.screen(this, "لوحة الإدارة", true);
        s.actions.addView(Ui.iconButton(this, R.drawable.ic_refresh, Ui.WHITE, 0, 42, v -> render()));
        body = s.body;
        body.addView(Ui.loading(this));
        Data.forgetUser(uid);
        Data.user(db, uid, me -> {
            if (!Data.isAdmin(me)) { Ui.toast(this, "هذه الصفحة متاحة للمدراء فقط"); finish(); return; }
            render();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ready && body != null && body.getChildCount() > 1) render();
    }

    private void render() {
        body.removeAllViews();


        body.addView(Ui.section(this, "نظرة عامة", null, null));
        LinearLayout r1 = row(), r2 = row(), r3 = row(), r4 = row();
        stat(r1, R.drawable.ic_group, "المستخدمون", Ui.GREEN_DARK, Ui.MINT, db.collection("users"), AdminSectionActivity.USERS, 0, true);
        stat(r1, R.drawable.ic_handyman, "الأدوات", Ui.GREEN_DARK, Ui.MINT, db.collection("tools"), AdminSectionActivity.TOOLS, 0, false);
        stat(r2, R.drawable.ic_swap_horiz, "إعارات جارية", 0xFF9A6A00, Ui.AMBER_SOFT, db.collection("borrow_requests").whereEqualTo("status", "accepted"), AdminSectionActivity.REQUESTS, 2, true);
        stat(r2, R.drawable.ic_hourglass_top, "طلبات معلقة", 0xFF9A6A00, Ui.AMBER_SOFT, db.collection("borrow_requests").whereEqualTo("status", "pending"), AdminSectionActivity.REQUESTS, 1, false);
        stat(r3, R.drawable.ic_flag, "بلاغات مفتوحة", Ui.WARN, Ui.WARN_SOFT, db.collection("reports").whereEqualTo("status", "open"), AdminSectionActivity.REPORTS, 0, true);
        stat(r3, R.drawable.ic_support_agent, "تذاكر دعم مفتوحة", Ui.WARN, Ui.WARN_SOFT, db.collection("support_requests").whereEqualTo("status", "open"), AdminSectionActivity.SUPPORT, 0, false);
        stat(r4, R.drawable.ic_star, "التقييمات", 0xFF9A6A00, Ui.AMBER_SOFT, db.collection("reviews"), AdminSectionActivity.REVIEWS, 0, true);
        stat(r4, R.drawable.ic_assignment_return, "إعارات مكتملة", Ui.GREEN_DARK, Ui.MINT, db.collection("borrow_requests").whereEqualTo("status", "returned"), AdminSectionActivity.REQUESTS, 3, false);
        for (LinearLayout r : new LinearLayout[]{r1, r2, r3, r4}) body.addView(r, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 0, 0, 10));

        body.addView(Ui.section(this, "الإدارة", null, null));
        LinearLayout g1 = Ui.group(this);
        item(g1, R.drawable.ic_group, "إدارة المستخدمين", "توثيق، إيقاف، صلاحيات المدير، إشعار خاص", AdminSectionActivity.USERS, true);
        item(g1, R.drawable.ic_handyman, "إدارة الأدوات", "مراجعة الأدوات، إيقافها أو حذفها", AdminSectionActivity.TOOLS, true);
        item(g1, R.drawable.ic_swap_horiz, "طلبات الاستعارة", "متابعة كل الإعارات والمتأخرات والتدخل عند الحاجة", AdminSectionActivity.REQUESTS, true);
        item(g1, R.drawable.ic_star, "التقييمات", "مراجعة التقييمات وحذف المسيء منها", AdminSectionActivity.REVIEWS, false);
        body.addView(g1, Ui.stack(this));

        LinearLayout g2 = Ui.group(this);
        item(g2, R.drawable.ic_flag, "البلاغات", "مراجعة بلاغات المستخدمين واتخاذ الإجراء", AdminSectionActivity.REPORTS, true);
        item(g2, R.drawable.ic_support_agent, "الدعم الفني", "الرد على تذاكر المستخدمين وتحديث حالتها", AdminSectionActivity.SUPPORT, false);
        body.addView(g2, Ui.stack(this));

        LinearLayout g3 = Ui.group(this);
        item(g3, R.drawable.ic_notifications_active, "إشعار عام", "إرسال إشعار لكل المستخدمين أو لحي محدد", AdminSectionActivity.BROADCAST, true);
        item(g3, R.drawable.ic_location_on, "الأحياء", "إضافة الأحياء وتعديلها وترتيبها", AdminSectionActivity.HOODS, true);
        item(g3, R.drawable.ic_settings, "إعدادات التطبيق", "إعلان الصفحة الرئيسية ووضع الصيانة", AdminSectionActivity.CONFIG, false);
        body.addView(g3, Ui.stack(this));

        body.addView(Ui.center(Ui.text(this, "نفع · لوحة الإدارة · الإصدار " + InfoActivity.version(this), 12, Ui.MUTED, false)),
                Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 8, 0, 0));
    }

    private LinearLayout row() {
        LinearLayout r = Ui.row(this);
        r.setGravity(Gravity.TOP);
        return r;
    }

    private void stat(LinearLayout into, int icon, String label, int fg, int bg, Query q, String section, int filter, boolean first) {
        LinearLayout c = Ui.col(this);
        Ui.pad(c, 14, 14, 14, 14);
        LinearLayout top = Ui.row(this);
        top.addView(Ui.badgeIcon(this, icon, fg, bg, 38, 20));
        top.addView(Ui.grow(this));
        top.addView(Ui.iv(this, R.drawable.ic_chevron_left, 0xFFB8BEBB, 18));
        c.addView(top);
        TextView num = Ui.text(this, "…", 26, Ui.INK, true);
        c.addView(num, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 10, 0, 0));
        c.addView(Ui.lines(Ui.text(this, label, 13, Ui.MUTED, false), 1));
        Ui.tap(c, Ui.shape(this, Ui.WHITE, 10, Ui.LINE, 1), 10, v -> open(section, filter));
        into.addView(c, Ui.m(this, Ui.lp(this, 0, Ui.WRAP, 1), first ? 0 : 5, 0, first ? 5 : 0, 0));
        q.count().get(AggregateSource.SERVER)
                .addOnSuccessListener(r -> num.setText(String.valueOf(r.getCount())))
                .addOnFailureListener(e -> num.setText("—"));
    }

    private void item(LinearLayout g, int icon, String title, String sub, String section, boolean divider) {
        g.addView(Ui.menuRow(this, icon, title, sub, Ui.GREEN, Ui.MINT, v -> open(section, 0)));
        if (divider) g.addView(Ui.divider(this, 0));
    }

    private void open(String section, int filter) {
        Intent i = new Intent(this, AdminSectionActivity.class);
        i.putExtra("section", section);
        i.putExtra("filter", filter);
        startActivity(i);
    }
}
