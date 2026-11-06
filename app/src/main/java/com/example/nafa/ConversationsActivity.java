package com.example.nafa;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ConversationsActivity extends BaseActivity {
    private LinearLayout list;
    private Ui.Field search;
    private final List<DocumentSnapshot> items = new ArrayList<>();

    @Override
    protected void onReady(Bundle saved) {
        Ui.Screen s = Ui.screen(this, "المحادثات", true);
        search = Ui.field(this, "ابحث باسم الجار", R.drawable.ic_search, android.text.InputType.TYPE_CLASS_TEXT);
        search.box.setBackground(Ui.shape(this, Ui.WHITE, 10, Ui.LINE, 1));
        search.edit.setOnFocusChangeListener(null);
        search.edit.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence a, int b, int c, int d) {}
            public void onTextChanged(CharSequence a, int b, int c, int d) {}
            public void afterTextChanged(android.text.Editable e) { render(); }
        });
        s.body.addView(search.box, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 4, 0, 14));
        list = Ui.col(this);
        s.body.addView(list, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        list.addView(Ui.loading(this));

        listeners.add(db.collection("conversations").whereArrayContains("participantIds", uid)
                .addSnapshotListener((q, e) -> {
                    if (q == null) {
                        list.removeAllViews();
                        list.addView(Ui.empty(this, R.drawable.ic_error_outline, "تعذر تحميل المحادثات", "تحقق من الاتصال بالإنترنت"));
                        return;
                    }
                    items.clear();
                    items.addAll(q.getDocuments());
                    Collections.sort(items, (x, y) -> Long.compare(time(y), time(x)));
                    render();
                }));
    }

    private static long time(DocumentSnapshot d) {
        long t = Data.millis(d, "lastMessageAt");
        return t != 0 ? t : Data.millis(d, "updatedAt");
    }

    private String otherId(DocumentSnapshot d) {
        Object p = d.get("participantIds");
        if (p instanceof List) for (Object o : (List<?>) p) if (o != null && !uid.equals(o)) return o.toString();
        return "";
    }

    private String otherName(DocumentSnapshot d, String other) {
        Object n = d.get("participantNames");
        if (n instanceof Map) {
            Object v = ((Map<?, ?>) n).get(other);
            if (v != null && !v.toString().isEmpty()) return v.toString();
        }
        return "جار من نفع";
    }

    private int unread(DocumentSnapshot d) {
        Object u = d.get("unread");
        if (u instanceof Map) {
            Object v = ((Map<?, ?>) u).get(uid);
            if (v instanceof Number) return ((Number) v).intValue();
        }
        return 0;
    }

    private void render() {
        list.removeAllViews();
        String qn = Data.norm(search.value());
        LinearLayout g = null;
        int shown = 0;
        for (DocumentSnapshot d : items) {
            if (Data.str(d, "lastMessage").isEmpty()) continue;
            String other = otherId(d), name = otherName(d, other);
            if (!qn.isEmpty() && !Data.norm(name).contains(qn)) continue;
            if (g == null) { g = Ui.group(this); list.addView(g, Ui.stack(this)); }
            else g.addView(Ui.divider(this, 0));
            g.addView(item(d, other, name));
            shown++;
        }
        if (shown == 0) {
            if (!qn.isEmpty()) list.addView(Ui.empty(this, R.drawable.ic_search, "لا توجد نتائج", "جرب اسما آخر"));
            else list.addView(Ui.empty(this, R.drawable.ic_forum, "لا توجد محادثات بعد",
                    "ابدأ محادثة مع صاحب أي أداة من صفحة تفاصيلها للاتفاق على الاستلام"));
        }
    }

    private View item(DocumentSnapshot d, String other, String name) {
        int n = unread(d);
        LinearLayout r = Ui.row(this);
        Ui.pad(r, 14, 12, 14, 12);
        FrameLayout av = new FrameLayout(this);
        av.addView(Ui.avatar(this, name, null, 52));
        r.addView(av, Ui.lp(this, 52, 52));
        av.setOnClickListener(v -> Data.openProfile(this, other, name));
        Data.user(db, other, u -> {
            if (u == null) return;
            String img = u.getString("profileImageBase64");
            if (img != null && !img.isEmpty()) {
                av.removeAllViews();
                av.addView(Ui.avatar(this, name, img, 52));
            }
        });

        LinearLayout t = Ui.col(this);
        LinearLayout top = Ui.row(this);
        top.addView(Ui.lines(Ui.text(this, name, 16, Ui.INK, true), 1), Ui.lp(this, 0, Ui.WRAP, 1));
        top.addView(Ui.text(this, Data.ago(time(d)), 12, n > 0 ? Ui.GREEN : Ui.MUTED, n > 0));
        t.addView(top);
        LinearLayout bottom = Ui.row(this);
        String last = Data.str(d, "lastMessage");
        if (uid.equals(d.getString("lastSenderId"))) last = "أنت: " + last;
        bottom.addView(Ui.lines(Ui.text(this, last, 14, n > 0 ? Ui.INK : Ui.MUTED, false), 1), Ui.lp(this, 0, Ui.WRAP, 1));
        if (n > 0) {
            TextView c = Ui.counter(this);
            Ui.setCount(c, n);
            c.setTextSize(11);
            c.setMinWidth(Ui.dp(this, 22));
            c.setBackground(Ui.shape(this, Ui.GREEN, 11));
            bottom.addView(c, Ui.m(this, Ui.lp(this, Ui.WRAP, 22), 8, 0, 0, 0));
        }
        t.addView(bottom, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 4, 0, 0));
        r.addView(t, Ui.m(this, Ui.lp(this, 0, Ui.WRAP, 1), 12, 0, 0, 0));
        r.setGravity(Gravity.CENTER_VERTICAL);
        Ui.tap(r, null, 0, v -> {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("conversationId", d.getId());
            i.putExtra("otherId", other);
            i.putExtra("otherName", name);
            startActivity(i);
        });
        return r;
    }
}
