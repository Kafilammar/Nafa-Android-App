package com.example.nafa;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RequestsTab {
    private final MainActivity a;
    private final FirebaseFirestore db;
    private boolean incoming = true;
    private LinearLayout list;
    private TextView segIn, segOut;
    private final Map<String, DocumentSnapshot> toolCache = new HashMap<>();
    private final List<DocumentSnapshot> docs = new ArrayList<>();
    private LinearLayout filterRow;
    private int filter;
    private static final String[] FILTERS = {"الكل", "بانتظار الرد", "جارية", "مكتملة", "مغلقة"};

    RequestsTab(MainActivity a) { this.a = a; this.db = a.db; }

    void mode(int in, int f) {
        if (in == 1) incoming = true;
        else if (in == 0) incoming = false;
        if (f >= 0 && f < FILTERS.length) filter = f;
    }

    View view() {
        LinearLayout root = Ui.col(a);
        root.setBackgroundColor(Ui.CANVAS);
        LinearLayout head = Ui.col(a);
        Ui.pad(head, 16, 12, 16, 8);
        TextView bt = Ui.text(a, "طلباتي", 18, Ui.WHITE, true);
        bt.setBackgroundColor(Ui.GREEN);
        Ui.pad(bt, 16, 0, 16, 0);
        root.addView(bt, Ui.lp(a, Ui.MATCH, 56));

        LinearLayout seg = Ui.row(a);
        seg.setBackground(Ui.shape(a, Ui.FIELD, 10));
        Ui.pad(seg, 4);
        segIn = segment("الطلبات الواردة");
        segOut = segment("طلباتي المرسلة");
        segIn.setOnClickListener(v -> { if (!incoming) { incoming = true; filter = 0; paintSeg(); load(); } });
        segOut.setOnClickListener(v -> { if (incoming) { incoming = false; filter = 0; paintSeg(); load(); } });
        seg.addView(segIn, Ui.lp(a, 0, 44, 1));
        seg.addView(segOut, Ui.lp(a, 0, 44, 1));
        head.addView(seg, Ui.lp(a, Ui.MATCH, Ui.WRAP));
        filterRow = Ui.row(a);
        head.addView(Ui.hscroll(a, filterRow), Ui.m(a, Ui.lp(a, Ui.MATCH, Ui.WRAP), 0, 12, 0, 0));
        root.addView(head);

        ScrollView sc = Ui.scroll(a);
        list = Ui.col(a);
        Ui.pad(list, 16, 8, 16, 24);
        sc.addView(list);
        root.addView(sc, Ui.lp(a, Ui.MATCH, 0, 1));
        paintSeg();
        load();
        return root;
    }

    private TextView segment(String s) {
        TextView t = Ui.center(Ui.medium(a, s, 14, Ui.MUTED));
        t.setClickable(true);
        return t;
    }

    private void paintSeg() {
        segIn.setBackground(incoming ? Ui.shape(a, Ui.GREEN, 4) : null);
        segOut.setBackground(!incoming ? Ui.shape(a, Ui.GREEN, 4) : null);
        segIn.setTextColor(incoming ? Ui.WHITE : Ui.INK);
        segOut.setTextColor(!incoming ? Ui.WHITE : Ui.INK);
    }

    void load() {
        list.removeAllViews();
        list.addView(Ui.loading(a));
        final boolean in = incoming;
        db.collection("borrow_requests").whereEqualTo(in ? "ownerId" : "borrowerId", a.uid).get().addOnSuccessListener(s -> {
            if (in != incoming) return;
            docs.clear();
            docs.addAll(s.getDocuments());
            Collections.sort(docs, (x, y) -> {
                int px = rank(x.getString("status")), py = rank(y.getString("status"));
                if (px != py) return Integer.compare(px, py);
                return Long.compare(Data.millis(y, "requestedAt"), Data.millis(x, "requestedAt"));
            });
            render();
        }).addOnFailureListener(e -> {
            list.removeAllViews();
            list.addView(Ui.empty(a, R.drawable.ic_error_outline, "تعذر تحميل الطلبات", "تحقق من اتصالك بالإنترنت ثم حاول مجددا"));
            TextView retry = Ui.button(a, "إعادة المحاولة", Ui.SOFT, R.drawable.ic_refresh);
            retry.setOnClickListener(v -> load());
            list.addView(retry, Ui.lp(a, Ui.MATCH, 50));
        });
    }

    private static int rank(String st) {
        if ("pending".equals(st)) return 0;
        if ("accepted".equals(st)) return 1;
        return 2;
    }

    private static boolean matches(int f, String st) {
        switch (f) {
            case 1: return "pending".equals(st);
            case 2: return "accepted".equals(st);
            case 3: return "returned".equals(st);
            case 4: return "rejected".equals(st) || "cancelled".equals(st);
            default: return true;
        }
    }

    private void render() {
        boolean in = incoming;
        filterRow.removeAllViews();
        for (int i = 0; i < FILTERS.length; i++) {
            int n = 0;
            for (DocumentSnapshot d : docs) if (matches(i, d.getString("status"))) n++;
            if (i != 0 && n == 0 && i != filter) continue;
            final int k = i;
            TextView c = Ui.choice(a, FILTERS[i] + (n > 0 ? "  " + n : ""), 0);
            Ui.setChoice(c, i == filter);
            c.setOnClickListener(v -> { filter = k; render(); });
            filterRow.addView(c, Ui.m(a, Ui.lp(a, Ui.WRAP, Ui.WRAP), 0, 0, 8, 0));
        }
        ((View) filterRow.getParent()).setVisibility(docs.isEmpty() ? View.GONE : View.VISIBLE);

        list.removeAllViews();
        if (docs.isEmpty()) {
            list.addView(in
                    ? Ui.empty(a, R.drawable.ic_inbox, "لا توجد طلبات واردة", "عندما يطلب أحد الجيران استعارة أداتك سيظهر الطلب هنا")
                    : Ui.empty(a, R.drawable.ic_swap_horiz, "لم ترسل أي طلب بعد", "ابحث عن الأداة التي تحتاجها وأرسل طلب استعارة لصاحبها"));
            TextView b = in ? Ui.button(a, "أضف أداة للإعارة", Ui.SOFT, R.drawable.ic_add) : Ui.button(a, "تصفح الأدوات", Ui.SOFT, R.drawable.ic_search);
            b.setOnClickListener(v -> { if (in) a.go(AddToolActivity.class); else a.show(MainActivity.SEARCH); });
            list.addView(b, Ui.lp(a, Ui.MATCH, 50));
            return;
        }
        int shown = 0;
        for (DocumentSnapshot d : docs) {
            if (!matches(filter, d.getString("status"))) continue;
            list.addView(card(d, in), Ui.stack(a));
            shown++;
        }
        if (shown == 0) list.addView(Ui.empty(a, R.drawable.ic_inbox, "لا توجد طلبات هنا", "جرب تصنيفا آخر"));
    }

    private View card(DocumentSnapshot r, boolean in) {
        String status = r.getString("status");
        LinearLayout c = Ui.card(a);

        LinearLayout top = Ui.row(a);
        FrameLayout thumb = new FrameLayout(a);
        thumb.setBackground(Ui.shape(a, Ui.MINT, 10));
        top.addView(thumb, Ui.lp(a, 56, 56));
        LinearLayout t = Ui.col(a);
        TextView name = Ui.lines(Ui.text(a, Data.str(r, "toolName").isEmpty() ? "..." : Data.str(r, "toolName"), 16, Ui.INK, true), 1);
        t.addView(name);
        String otherField = in ? "borrowerName" : "ownerName";
        String otherId = in ? r.getString("borrowerId") : r.getString("ownerId");
        TextView who = Ui.lines(Ui.text(a, (in ? "من: " : "إلى: ") + Data.str(r, otherField), 13, Ui.GREEN_DARK, false), 1);
        t.addView(who);
        top.addView(t, Ui.m(a, Ui.lp(a, 0, Ui.WRAP, 1), 12, 0, 8, 0));
        int[] sc = Data.requestColors(status);
        top.addView(Ui.chip(a, Data.requestLabel(status), sc[0], sc[1], Data.requestIcon(status)));
        c.addView(top);

        final String[] otherName = {Data.str(r, otherField)};
        Ui.tap(who, null, 8, v -> Data.openProfile(a, otherId, otherName[0]));
        if (otherName[0].isEmpty()) Data.user(db, otherId, u -> { otherName[0] = Data.str(u, "fullName"); who.setText((in ? "من: " : "إلى: ") + otherName[0]); });
        String toolId = r.getString("toolId");
        if (toolId != null) {
            DocumentSnapshot cached = toolCache.get(toolId);
            if (cached != null) fillThumb(thumb, cached, name, r);
            else db.collection("tools").document(toolId).get().addOnSuccessListener(td -> {
                if (!td.exists()) return;
                toolCache.put(toolId, td);
                fillThumb(thumb, td, name, r);
            });
        }

        LinearLayout meta = Ui.row(a);
        meta.addView(Ui.iv(a, R.drawable.ic_schedule, Ui.MUTED, 16));
        meta.addView(Ui.text(a, "المدة: " + Data.duration(Math.max(1, Data.num(r.getLong("durationValue"))), r.getString("durationUnit")), 13, Ui.INK, false),
                Ui.m(a, Ui.lp(a, 0, Ui.WRAP, 1), 6, 0, 0, 0));
        meta.addView(Ui.text(a, Data.ago(Data.millis(r, "requestedAt")), 12, Ui.MUTED, false));
        c.addView(meta, Ui.m(a, Ui.lp(a, Ui.MATCH, Ui.WRAP), 0, 12, 0, 0));

        long startMs = Data.millis(r, "startDate");
        if (startMs > 0 && ("pending".equals(status) || "accepted".equals(status))) {
            LinearLayout sr = Ui.row(a);
            sr.addView(Ui.iv(a, R.drawable.ic_event, Ui.MUTED, 16));
            sr.addView(Ui.text(a, "تاريخ البدء: " + Data.day(startMs), 13, Ui.INK, false), Ui.m(a, Ui.lp(a, 0, Ui.WRAP, 1), 6, 0, 0, 0));
            c.addView(sr, Ui.m(a, Ui.lp(a, Ui.MATCH, Ui.WRAP), 0, 6, 0, 0));
        }
        long due = Data.millis(r, "dueAt");
        if ("accepted".equals(status) && due > 0) {
            long left = Data.daysLeft(due);
            int fg, bg, ic;
            String txt;
            if (left < 0) { fg = Ui.WARN; bg = Ui.WARN_SOFT; ic = R.drawable.ic_warning; txt = "متأخرة " + Data.count(-left, "يوما واحدا", "يومين", "أيام", "يوما") + " · كان موعد الإرجاع " + Data.day(due); }
            else if (left == 0) { fg = 0xFF9A6A00; bg = Ui.AMBER_SOFT; ic = R.drawable.ic_schedule; txt = "موعد الإرجاع اليوم"; }
            else { fg = Ui.GREEN_DARK; bg = Ui.MINT; ic = R.drawable.ic_schedule; txt = "موعد الإرجاع " + Data.day(due) + " · متبق " + Data.count(left, "يوم واحد", "يومان", "أيام", "يوما"); }
            LinearLayout dr = Ui.row(a);
            dr.setBackground(Ui.shape(a, bg, 12));
            Ui.pad(dr, 12, 9, 12, 9);
            dr.addView(Ui.iv(a, ic, fg, 18));
            dr.addView(Ui.medium(a, txt, 13, fg), Ui.m(a, Ui.lp(a, 0, Ui.WRAP, 1), 8, 0, 0, 0));
            c.addView(dr, Ui.m(a, Ui.lp(a, Ui.MATCH, Ui.WRAP), 0, 10, 0, 0));
        }

        String msg = Data.str(r, "message");
        if (!msg.isEmpty()) {
            TextView m = Ui.text(a, "«" + msg + "»", 14, Ui.INK, false);
            m.setBackground(Ui.shape(a, Ui.CANVAS, 12));
            Ui.pad(m, 12, 10, 12, 10);
            c.addView(m, Ui.m(a, Ui.lp(a, Ui.MATCH, Ui.WRAP), 0, 10, 0, 0));
        }

        LinearLayout act = Ui.row(a);
        Runnable refresh = this::load;
        if (in && "pending".equals(status)) {
            act.addView(btn("قبول", Ui.PRIMARY, R.drawable.ic_check, v -> Actions.accept(a, db, a.uid, r, refresh)), Ui.m(a, Ui.lp(a, 0, 46, 1), 0, 0, 4, 0));
            act.addView(btn("رفض", Ui.DANGER, R.drawable.ic_close, v -> Actions.reject(a, db, a.uid, r, refresh)), Ui.m(a, Ui.lp(a, 0, 46, 1), 4, 0, 0, 0));
        } else if (in && "accepted".equals(status)) {
            if (Data.millis(r, "returnClaimedAt") > 0) {
                LinearLayout cl = Ui.row(a);
                cl.setBackground(Ui.shape(a, Ui.MINT, 12));
                Ui.pad(cl, 12, 9, 12, 9);
                cl.addView(Ui.iv(a, R.drawable.ic_assignment_return, Ui.GREEN_DARK, 18));
                cl.addView(Ui.medium(a, "يقول المستعير إنه أعاد الأداة، أكد الاستلام", 13, Ui.GREEN_DARK), Ui.m(a, Ui.lp(a, 0, Ui.WRAP, 1), 8, 0, 0, 0));
                c.addView(cl, Ui.m(a, Ui.lp(a, Ui.MATCH, Ui.WRAP), 0, 10, 0, 0));
            }
            act.addView(btn("تم الإرجاع", Ui.PRIMARY, R.drawable.ic_assignment_return, v -> Actions.returned(a, db, a.uid, r, refresh)), Ui.m(a, Ui.lp(a, 0, 46, 1), 0, 0, 4, 0));
            act.addView(btn("تذكير", Ui.SOFT, R.drawable.ic_notifications_active, v -> Actions.remind(a, db, a.uid, r, refresh)), Ui.m(a, Ui.lp(a, 0, 46, 1), 4, 0, 4, 0));
            act.addView(chatIcon(otherId, otherName), Ui.m(a, Ui.lp(a, 46, 46), 4, 0, 0, 0));
        } else if (!in && "pending".equals(status)) {
            act.addView(btn("إلغاء الطلب", Ui.DANGER, R.drawable.ic_close, v -> Actions.cancel(a, db, a.uid, r, refresh)), Ui.m(a, Ui.lp(a, 0, 46, 1), 0, 0, 4, 0));
            act.addView(btn("مراسلة", Ui.SOFT, R.drawable.ic_chat, v -> Data.openChat(a, db, a.uid, otherId, otherName[0])), Ui.m(a, Ui.lp(a, 0, 46, 1), 4, 0, 0, 0));
        } else if (!in && "accepted".equals(status)) {
            boolean claimed = Data.millis(r, "returnClaimedAt") > 0;
            if (claimed) act.addView(btn("بانتظار تأكيد المالك", Ui.SOFT, R.drawable.ic_hourglass_top, v -> Ui.toast(a, "أبلغنا المالك، سيؤكد الاستلام قريبا")), Ui.m(a, Ui.lp(a, 0, 46, 1), 0, 0, 4, 0));
            else act.addView(btn("أعدت الأداة", Ui.PRIMARY, R.drawable.ic_assignment_return, v -> Actions.claimReturn(a, db, a.uid, r, this::load)), Ui.m(a, Ui.lp(a, 0, 46, 1), 0, 0, 4, 0));
            act.addView(chatIcon(otherId, otherName), Ui.m(a, Ui.lp(a, 46, 46), 4, 0, 4, 0));
            if (toolId != null) act.addView(Ui.iconButton(a, R.drawable.ic_handyman, Ui.GREEN, Ui.MINT, 46, v -> Cards.openTool(a, toolId)), Ui.m(a, Ui.lp(a, 46, 46), 4, 0, 0, 0));
        } else if ("returned".equals(status)) {
            boolean done = Boolean.TRUE.equals(r.getBoolean(in ? "ownerReviewed" : "borrowerReviewed"));
            if (!done) act.addView(btn(in ? "قيم المستعير" : "قيم التجربة", Ui.SOFT, R.drawable.ic_star,
                    v -> Actions.review(a, db, a.uid, r, in, otherName[0], refresh)), Ui.lp(a, 0, 46, 1));
            else {
                TextView ok = Ui.chip(a, "تم التقييم، شكرا لك", Ui.GREEN_DARK, Ui.MINT, R.drawable.ic_check_circle);
                act.addView(ok);
            }
        }
        if (act.getChildCount() > 0) c.addView(act, Ui.m(a, Ui.lp(a, Ui.MATCH, Ui.WRAP), 0, 14, 0, 0));
        return c;
    }

    private void fillThumb(FrameLayout thumb, DocumentSnapshot td, TextView name, DocumentSnapshot r) {
        thumb.removeAllViews();
        thumb.addView(Cards.toolImage(a, db, td, 14, 26), new FrameLayout.LayoutParams(-1, -1));
        if (Data.str(r, "toolName").isEmpty()) name.setText(Data.str(td, "toolName"));
        thumb.setOnClickListener(v -> Cards.openTool(a, td.getId()));
    }

    private View chatIcon(String otherId, String[] otherName) {
        android.widget.FrameLayout f = Ui.iconButton(a, R.drawable.ic_chat, Ui.GREEN, Ui.MINT, 46, v -> Data.openChat(a, db, a.uid, otherId, otherName[0]));
        f.setContentDescription("مراسلة");
        return f;
    }

    private TextView btn(String s, int style, int icon, View.OnClickListener l) {
        TextView b = Ui.button(a, s, style, icon);
        b.setMinHeight(Ui.dp(a, 46));
        b.setTextSize(14);
        b.setOnClickListener(l);
        b.setGravity(Gravity.CENTER);
        return b;
    }
}
