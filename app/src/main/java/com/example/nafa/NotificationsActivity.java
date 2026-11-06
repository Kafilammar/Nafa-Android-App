package com.example.nafa;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsActivity extends BaseActivity {
    private LinearLayout list;
    private TextView readAll;
    private android.widget.FrameLayout more;
    private final List<DocumentSnapshot> items = new ArrayList<>();

    @Override
    protected void onReady(Bundle saved) {
        Ui.Screen s = Ui.screen(this, "الإشعارات", true);
        readAll = Ui.link(this, "قراءة الكل", v -> markAll());
        readAll.setTextColor(Ui.WHITE);
        readAll.setVisibility(View.GONE);
        s.actions.addView(readAll);
        more = Ui.iconButton(this, R.drawable.ic_more_vert, Ui.WHITE, 0, 42, v -> Ui.options(this, "إدارة الإشعارات",
                new String[]{"تعليم الكل كمقروء", "حذف الإشعارات المقروءة"},
                new int[]{R.drawable.ic_done_all, R.drawable.ic_delete}, new int[]{Ui.GREEN, Ui.WARN}, k -> {
                    if (k == 0) markAll(); else clearRead();
                }));
        more.setVisibility(View.GONE);
        s.actions.addView(more, Ui.m(this, Ui.lp(this, 42, 42), 4, 0, 0, 0));
        list = Ui.col(this);
        s.body.addView(list, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        list.addView(Ui.loading(this));

        listeners.add(db.collection("notifications").whereEqualTo("userId", uid).limit(100)
                .addSnapshotListener((q, e) -> {
                    if (q == null) {
                        list.removeAllViews();
                        list.addView(Ui.empty(this, R.drawable.ic_error_outline, "تعذر تحميل الإشعارات", "تحقق من الاتصال بالإنترنت"));
                        return;
                    }
                    items.clear();
                    items.addAll(q.getDocuments());
                    Collections.sort(items, (x, y) -> Long.compare(Data.millis(y, "createdAt"), Data.millis(x, "createdAt")));
                    render();
                }));
    }

    private void render() {
        list.removeAllViews();
        int unread = 0;
        for (DocumentSnapshot d : items) if (!Boolean.TRUE.equals(d.getBoolean("isRead"))) unread++;
        readAll.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
        more.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        if (items.isEmpty()) {
            list.addView(Ui.empty(this, R.drawable.ic_notifications_none, "لا توجد إشعارات",
                    "ستصلك هنا تحديثات طلباتك وتقييماتك ورسائل جيرانك"));
            return;
        }
        String lastGroup = null;
        LinearLayout group = null;
        for (DocumentSnapshot d : items) {
            String g = groupOf(Data.millis(d, "createdAt"));
            if (!g.equals(lastGroup)) {
                lastGroup = g;
                list.addView(Ui.text(this, g, 14, Ui.MUTED, true), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 4, 12, 4, 8));
                group = Ui.group(this);
                list.addView(group, Ui.stack(this));
            } else if (group != null) {
                group.addView(Ui.divider(this, 0));
            }
            if (group != null) group.addView(item(d));
        }
    }

    private String groupOf(long ms) {
        if (ms == 0) return "الآن";
        java.util.Calendar now = java.util.Calendar.getInstance(), c = java.util.Calendar.getInstance();
        c.setTimeInMillis(ms);
        int days = (int) ((startOfDay(now) - startOfDay(c)) / 86_400_000L);
        if (days <= 0) return "اليوم";
        if (days == 1) return "أمس";
        if (days < 7) return "هذا الأسبوع";
        return "أقدم";
    }

    private static long startOfDay(java.util.Calendar c) {
        java.util.Calendar x = (java.util.Calendar) c.clone();
        x.set(java.util.Calendar.HOUR_OF_DAY, 0);
        x.set(java.util.Calendar.MINUTE, 0);
        x.set(java.util.Calendar.SECOND, 0);
        x.set(java.util.Calendar.MILLISECOND, 0);
        return x.getTimeInMillis();
    }

    private View item(DocumentSnapshot d) {
        boolean read = Boolean.TRUE.equals(d.getBoolean("isRead"));
        String type = d.getString("type");
        int[] col = colors(type);
        LinearLayout r = Ui.row(this);
        r.setGravity(Gravity.TOP);
        Ui.pad(r, 14, 14, 14, 14);

        FrameLayout ic = Ui.badgeIcon(this, Data.notifIcon(type), col[0], col[1], 42, 21);
        r.addView(ic);
        LinearLayout t = Ui.col(this);
        LinearLayout head = Ui.row(this);
        head.addView(Ui.lines(Ui.text(this, Data.str(d, "title"), 15, Ui.INK, !read), 2), Ui.lp(this, 0, Ui.WRAP, 1));
        if (!read) {
            View dot = new View(this);
            dot.setBackground(Ui.oval(Ui.GREEN));
            head.addView(dot, Ui.m(this, Ui.lp(this, 9, 9), 8, 0, 0, 0));
        }
        t.addView(head);
        String body = Data.str(d, "body");
        if (!body.isEmpty()) t.addView(Ui.lines(Ui.text(this, body, 13, 0xFF5F6663, false), 3), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 3, 0, 0));
        t.addView(Ui.text(this, Data.ago(Data.millis(d, "createdAt")), 12, Ui.MUTED, false), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 6, 0, 0));
        r.addView(t, Ui.m(this, Ui.lp(this, 0, Ui.WRAP, 1), 12, 0, 0, 0));
        Ui.tap(r, read ? null : new android.graphics.drawable.ColorDrawable(0x0F00852B), 0, v -> open(d));
        r.setOnLongClickListener(v -> {
            Ui.confirm(this, "حذف الإشعار", "هل تريد حذف هذا الإشعار؟", "حذف", true,
                    () -> d.getReference().delete());
            return true;
        });
        return r;
    }

    private int[] colors(String type) {
        if (type == null) return new int[]{Ui.GREEN, Ui.MINT};
        switch (type) {
            case "rejected":
            case "cancelled": return new int[]{Ui.WARN, Ui.WARN_SOFT};
            case "review":
            case "reminder":
            case "request": return new int[]{0xFF9A6A00, Ui.AMBER_SOFT};
            default: return new int[]{Ui.GREEN, Ui.MINT};
        }
    }

    private void open(DocumentSnapshot d) {
        if (!Boolean.TRUE.equals(d.getBoolean("isRead"))) d.getReference().update("isRead", true);
        String type = d.getString("type");
        if (type == null) return;
        switch (type) {
            case "request":
            case "cancelled":
                Data.openRequests(this, 1, 0);
                break;
            case "reminder":
            case "accepted":
            case "rejected":
                Data.openRequests(this, 0, 0);
                break;
            case "returned": {
                boolean forOwner = Data.str(d, "title").contains("أعاد");
                Data.openRequests(this, forOwner ? 1 : 0, forOwner ? 2 : 3);
                break;
            }
            case "support":
                go(SupportActivity.class);
                break;
            case "review":
                go(ReviewsActivity.class);
                break;
            case "message": {
                String conv = d.getString("refId"), sender = d.getString("senderId");
                if (conv != null && !conv.isEmpty() && sender != null && !sender.isEmpty() && !sender.equals(uid)) {
                    Intent i = new Intent(this, ChatActivity.class);
                    i.putExtra("conversationId", conv);
                    i.putExtra("otherId", sender);
                    String t = Data.str(d, "title");
                    i.putExtra("otherName", t.startsWith("رسالة جديدة من ") ? t.substring("رسالة جديدة من ".length()) : "");
                    startActivity(i);
                } else go(ConversationsActivity.class);
                break;
            }
            case "system":
                go(AddToolActivity.class);
                break;
        }
    }

    private void clearRead() {
        int n = 0;
        for (DocumentSnapshot d : items) if (Boolean.TRUE.equals(d.getBoolean("isRead"))) n++;
        if (n == 0) { Ui.toast(this, "لا توجد إشعارات مقروءة"); return; }
        final int total = n;
        Ui.confirm(this, "حذف الإشعارات المقروءة", "سيتم حذف " + Data.count(total, "إشعار واحد", "إشعارين", "إشعارات", "إشعارا") + " مقروءة.", "حذف", true, () -> {
            WriteBatch b = db.batch();
            int k = 0;
            for (DocumentSnapshot d : items)
                if (Boolean.TRUE.equals(d.getBoolean("isRead")) && k < 450) { b.delete(d.getReference()); k++; }
            b.commit().addOnSuccessListener(v -> Ui.toast(this, "تم حذف الإشعارات المقروءة"))
                    .addOnFailureListener(e -> Ui.toast(this, "تعذر الحذف، حاول مرة أخرى"));
        });
    }

    private void markAll() {
        WriteBatch b = db.batch();
        int n = 0;
        for (DocumentSnapshot d : items) {
            if (!Boolean.TRUE.equals(d.getBoolean("isRead")) && n < 450) {
                b.update(d.getReference(), "isRead", true);
                n++;
            }
        }
        if (n == 0) return;
        b.commit().addOnSuccessListener(v -> Ui.toast(this, "تم تعليم كل الإشعارات كمقروءة"))
                .addOnFailureListener(e -> Ui.toast(this, "تعذر التحديث، حاول مرة أخرى"));
    }
}
