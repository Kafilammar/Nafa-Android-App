package com.example.nafa;

import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

class ProfileTab {
    private final MainActivity a;
    private TextView notifSub, chatSub, toolsNum, lendNum, rateNum;

    ProfileTab(MainActivity a) { this.a = a; }

    void counts() {
        if (notifSub != null) notifSub.setText(a.unreadNotifs() > 0 ? Data.count(a.unreadNotifs(), "إشعار جديد", "إشعاران جديدان", "إشعارات جديدة", "إشعارا جديدا") : "لا جديد");
        if (chatSub != null) chatSub.setText(a.unreadChats() > 0 ? Data.count(a.unreadChats(), "رسالة غير مقروءة", "رسالتان غير مقروءتين", "رسائل غير مقروءة", "رسالة غير مقروءة") : "تواصل مع جيرانك");
    }

    View view() {
        ScrollView sc = Ui.scroll(a);
        sc.setBackgroundColor(Ui.CANVAS);
        LinearLayout col = Ui.col(a);
        sc.addView(col);

        LinearLayout head = Ui.row(a);
        android.graphics.drawable.GradientDrawable hb = Ui.gradient(a, Ui.GREEN, Ui.GREEN_DARK, 0);
        head.setBackground(hb);
        Ui.pad(head, 16, 0, 8, 0);
        head.setMinimumHeight(Ui.dp(a, 56));
        head.addView(Ui.text(a, "حسابي", 18, Ui.WHITE, true), Ui.lp(a, 0, Ui.WRAP, 1));
        head.addView(Ui.iconButton(a, R.drawable.ic_settings, Ui.WHITE, 0x26FFFFFF, 42, v -> a.go(SettingsActivity.class)));
        col.addView(head, Ui.lp(a, Ui.MATCH, Ui.WRAP));

        LinearLayout body = Ui.col(a);
        Ui.pad(body, 16, 0, 16, 28);
        LinearLayout.LayoutParams bp = Ui.lp(a, Ui.MATCH, Ui.WRAP);
        bp.topMargin = Ui.dp(a, 16);
        col.addView(body, bp);

        LinearLayout card = Ui.card(a);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setBackground(null);
        FrameLayout avHolder = new FrameLayout(a);
        card.addView(avHolder, Ui.lp(a, 72, 72));
        avHolder.setOnClickListener(v -> a.go(EditProfileActivity.class));
        TextView name = Ui.center(Ui.text(a, "...", 18, Ui.INK, true));
        card.addView(name, Ui.m(a, Ui.lp(a, Ui.MATCH, Ui.WRAP), 0, 10, 0, 2));
        TextView email = Ui.center(Ui.text(a, "", 13, Ui.MUTED, false));
        card.addView(email, Ui.lp(a, Ui.MATCH, Ui.WRAP));
        LinearLayout chips = Ui.row(a);
        chips.setGravity(Gravity.CENTER);
        card.addView(chips, Ui.m(a, Ui.lp(a, Ui.MATCH, Ui.WRAP), 0, 10, 0, 0));

        LinearLayout stats = Ui.row(a);
        stats.setBackground(Ui.shape(a, Ui.CANVAS, 10));
        Ui.pad(stats, 0, 12, 0, 12);
        toolsNum = stat(stats, "أدواتي", v -> a.go(MyToolsActivity.class));
        lendNum = stat(stats, "إعارات", v -> a.show(MainActivity.REQUESTS));
        rateNum = stat(stats, "التقييم", v -> a.go(ReviewsActivity.class));
        card.addView(stats, Ui.m(a, Ui.lp(a, Ui.MATCH, Ui.WRAP), 0, 16, 0, 12));
        LinearLayout btns = Ui.row(a);
        TextView edit = Ui.button(a, "تعديل الملف", Ui.SOFT, R.drawable.ic_edit);
        edit.setOnClickListener(v -> a.go(EditProfileActivity.class));
        btns.addView(edit, Ui.lp(a, 0, 48, 1));
        TextView pub = Ui.button(a, "ملفي العام", Ui.OUTLINE, R.drawable.ic_person);
        pub.setOnClickListener(v -> Data.openProfile(a, a.uid, a.me == null ? "" : Data.str(a.me, "fullName")));
        btns.addView(pub, Ui.m(a, Ui.lp(a, 0, 48, 1), 8, 0, 0, 0));
        card.addView(btns, Ui.lp(a, Ui.MATCH, Ui.WRAP));
        body.addView(card, Ui.stack(a));

        LinearLayout adminSlot = Ui.col(a);
        body.addView(adminSlot, Ui.lp(a, Ui.MATCH, Ui.WRAP));

        LinearLayout g1 = Ui.group(a);
        g1.addView(Ui.menuRow(a, R.drawable.ic_inventory_2, "إدارة أدواتي", "أضف وعدل أدواتك المعروضة", Ui.GREEN, Ui.MINT, v -> a.go(MyToolsActivity.class)));
        g1.addView(Ui.divider(a, 0));
        g1.addView(Ui.menuRow(a, R.drawable.ic_favorite, "المفضلة", "الأدوات التي حفظتها للرجوع إليها", Ui.WARN, Ui.WARN_SOFT, v -> a.go(FavoritesActivity.class)));
        g1.addView(Ui.divider(a, 0));
        LinearLayout chats = Ui.menuRow(a, R.drawable.ic_forum, "المحادثات", " ", Ui.GREEN, Ui.MINT, v -> a.go(ConversationsActivity.class));
        chatSub = (TextView) ((LinearLayout) chats.getTag()).getChildAt(1);
        g1.addView(chats);
        g1.addView(Ui.divider(a, 0));
        LinearLayout notes = Ui.menuRow(a, R.drawable.ic_notifications, "الإشعارات", " ", Ui.GREEN, Ui.MINT, v -> a.go(NotificationsActivity.class));
        notifSub = (TextView) ((LinearLayout) notes.getTag()).getChildAt(1);
        g1.addView(notes);
        g1.addView(Ui.divider(a, 0));
        g1.addView(Ui.menuRow(a, R.drawable.ic_star, "تقييماتي", "ما يقوله الجيران عنك", 0xFF9A6A00, Ui.AMBER_SOFT, v -> a.go(ReviewsActivity.class)));
        body.addView(g1, Ui.stack(a));
        counts();

        LinearLayout g2 = Ui.group(a);
        g2.addView(Ui.menuRow(a, R.drawable.ic_support_agent, "المساعدة والدعم", "الأسئلة الشائعة وتواصل معنا", Ui.GREEN, Ui.MINT, v -> a.go(SupportActivity.class)));
        g2.addView(Ui.divider(a, 0));
        g2.addView(Ui.menuRow(a, R.drawable.ic_description, "الشروط والأحكام", null, Ui.GREEN, Ui.MINT, v -> InfoActivity.open(a, "terms")));
        g2.addView(Ui.divider(a, 0));
        g2.addView(Ui.menuRow(a, R.drawable.ic_privacy_tip, "سياسة الخصوصية", null, Ui.GREEN, Ui.MINT, v -> InfoActivity.open(a, "privacy")));
        g2.addView(Ui.divider(a, 0));
        g2.addView(Ui.menuRow(a, R.drawable.ic_info, "عن نفع", null, Ui.GREEN, Ui.MINT, v -> InfoActivity.open(a, "about")));
        body.addView(g2, Ui.stack(a));

        LinearLayout g2b = Ui.group(a);
        g2b.addView(Ui.menuRow(a, R.drawable.ic_share, "شارك التطبيق", "ادع جيرانك للانضمام إلى نفع", Ui.GREEN, Ui.MINT, v -> Data.shareApp(a)));
        g2b.addView(Ui.divider(a, 0));
        g2b.addView(Ui.menuRow(a, R.drawable.ic_thumb_up, "قيم نفع", "رأيك يساعدنا على التحسين", 0xFF9A6A00, Ui.AMBER_SOFT, v -> Data.rateApp(a)));
        g2 = g2b;
        body.addView(g2, Ui.stack(a));

        LinearLayout g3 = Ui.group(a);
        g3.addView(Ui.menuRow(a, R.drawable.ic_logout, "تسجيل الخروج", null, Ui.WARN, Ui.WARN_SOFT, v -> logout()));
        body.addView(g3, Ui.stack(a));

        Data.user(a.db, a.uid, d -> {
            a.me = d;
            adminSlot.removeAllViews();
            if (Data.isAdmin(d)) {
                LinearLayout adm = Ui.row(a);
                Ui.pad(adm, 16, 14, 14, 14);
                LinearLayout at = Ui.col(a);
                at.addView(Ui.text(a, "لوحة الإدارة", 16, Ui.WHITE, true));
                at.addView(Ui.text(a, "المستخدمون والأدوات والبلاغات والدعم والإعدادات", 12, 0xDDFFFFFF, false));
                adm.addView(at, Ui.lp(a, 0, Ui.WRAP, 1));
                adm.addView(Ui.iv(a, R.drawable.ic_chevron_left, Ui.WHITE, 22));
                Ui.tap(adm, Ui.shape(a, Ui.GREEN, 4), 4, v -> a.go(AdminActivity.class));
                adminSlot.addView(adm, Ui.stack(a));
            }
            avHolder.removeAllViews();
            FrameLayout ring = new FrameLayout(a);
            ring.addView(Ui.avatar(a, Data.str(d, "fullName"), d == null ? null : d.getString("profileImageBase64"), 72));
            avHolder.addView(ring, new FrameLayout.LayoutParams(-1, -1));
            name.setText(Data.str(d, "fullName").isEmpty() ? "مستخدم نفع" : Data.str(d, "fullName"));
            email.setText(Data.str(d, "email"));
            chips.removeAllViews();
            chips.addView(Ui.chip(a, "حي " + Data.hoodName(d == null ? null : d.getString("neighborhoodId")), Ui.GREEN_DARK, Ui.MINT, R.drawable.ic_location_on));
            FirebaseUser fu = FirebaseAuth.getInstance().getCurrentUser();
            if ((d != null && Boolean.TRUE.equals(d.getBoolean("isVerified"))) || (fu != null && fu.isEmailVerified())) {
                TextView v = Ui.chip(a, "حساب موثق", Ui.GREEN_DARK, Ui.MINT, R.drawable.ic_verified);
                chips.addView(v);
                ((LinearLayout.LayoutParams) v.getLayoutParams()).setMarginStart(Ui.dp(a, 6));
            }
            Data.forgetRating(a.uid);
            Data.ratingOf(a.db, a.uid, rt -> rateNum.setText(rt[1] == 0 ? "—" : Data.rating(rt[0]) + ""));
        });
        a.db.collection("tools").whereEqualTo("ownerId", a.uid).get().addOnSuccessListener(s -> toolsNum.setText(String.valueOf(s.size())));
        a.db.collection("borrow_requests").whereEqualTo("ownerId", a.uid).get().addOnSuccessListener(s -> {
            int n = 0;
            for (DocumentSnapshot d : s.getDocuments()) {
                String st = d.getString("status");
                if ("accepted".equals(st) || "returned".equals(st)) n++;
            }
            lendNum.setText(String.valueOf(n));
        });
        return sc;
    }

    private TextView stat(LinearLayout into, String label, View.OnClickListener l) {
        LinearLayout c = Ui.col(a);
        c.setGravity(Gravity.CENTER);
        Ui.pad(c, 0, 4, 0, 4);
        Ui.tap(c, null, 12, l);
        TextView n = Ui.center(Ui.text(a, "—", 19, Ui.GREEN_DARK, true));
        c.addView(n, Ui.lp(a, Ui.MATCH, Ui.WRAP));
        c.addView(Ui.center(Ui.text(a, label, 12, Ui.MUTED, false)), Ui.lp(a, Ui.MATCH, Ui.WRAP));
        into.addView(c, Ui.lp(a, 0, Ui.WRAP, 1));
        return n;
    }


    private void logout() {
        Ui.confirm(a, "تسجيل الخروج", "هل تريد تسجيل الخروج من حسابك؟", "تسجيل الخروج", true, () -> {
            FirebaseAuth.getInstance().signOut();
            Data.clearCache();
            Intent i = new Intent(a, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            a.startActivity(i);
            a.finish();
        });
    }
}
