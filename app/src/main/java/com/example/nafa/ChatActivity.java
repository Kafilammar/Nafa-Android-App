package com.example.nafa;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends BaseActivity {
    private static final String[] QUICK = {"السلام عليكم", "هل الأداة متاحة؟", "متى يناسبك الاستلام؟", "شكرا جزيلا"};

    private String convId, otherId, otherName;
    private LinearLayout msgs;
    private ScrollView scroll;
    private EditText input;
    private FrameLayout sendBtn;
    private List<DocumentSnapshot> messages = new ArrayList<>();
    private int otherUnread = -1;
    private boolean firstLoad = true;

    @Override
    protected void onReady(Bundle saved) {
        convId = getIntent().getStringExtra("conversationId");
        otherId = getIntent().getStringExtra("otherId");
        otherName = getIntent().getStringExtra("otherName");
        if (otherId == null || otherId.isEmpty()) { finish(); return; }
        if (convId == null || convId.isEmpty()) convId = Data.chatId(uid, otherId);
        if (otherName == null || otherName.isEmpty()) otherName = "جار من نفع";
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        bars(Ui.WHITE, Ui.WHITE, true);
        build();
        listen();
    }

    private void build() {
        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(Ui.CANVAS);

        LinearLayout bar = Ui.row(this);
        bar.setBackgroundColor(Ui.WHITE);
        Ui.pad(bar, 10, 8, 12, 8);
        bar.addView(Ui.iconButton(this, R.drawable.ic_arrow_forward, Ui.INK, Ui.WHITE, 42, v -> finish()));
        FrameLayout av = new FrameLayout(this);
        av.addView(Ui.avatar(this, otherName, null, 42));
        bar.addView(av, Ui.m(this, Ui.lp(this, 42, 42), 6, 0, 10, 0));
        LinearLayout who = Ui.col(this);
        TextView nameView = Ui.lines(Ui.text(this, otherName, 16, Ui.INK, true), 1);
        who.addView(nameView);
        TextView sub = Ui.lines(Ui.text(this, "جار في نفع", 12, Ui.MUTED, false), 1);
        who.addView(sub);
        bar.addView(who, Ui.lp(this, 0, Ui.WRAP, 1));
        View.OnClickListener profile = v -> Data.openProfile(this, otherId, otherName);
        Ui.tap(who, null, 12, profile);
        av.setOnClickListener(profile);
        bar.addView(Ui.iconButton(this, R.drawable.ic_more_vert, Ui.INK, Ui.CANVAS, 40, v -> Ui.options(this, null,
                new String[]{"عرض الملف الشخصي", "الإبلاغ عن المستخدم"},
                new int[]{R.drawable.ic_person, R.drawable.ic_flag}, new int[]{Ui.GREEN, Ui.WARN}, k -> {
                    if (k == 0) Data.openProfile(this, otherId, otherName);
                    else Actions.report(this, db, uid, "user", otherId, otherName);
                })));
        root.addView(bar, Ui.lp(this, Ui.MATCH, Ui.WRAP));

        Data.user(db, otherId, u -> {
            if (u == null) return;
            String real = Data.str(u, "fullName");
            if (!real.isEmpty() && !real.equals(otherName)) { otherName = real; nameView.setText(real); }
            String img = u.getString("profileImageBase64");
            av.removeAllViews();
            av.addView(Ui.avatar(this, otherName, img, 42));
            String hood = Data.hoodName(u.getString("neighborhoodId"));
            sub.setText(hood.isEmpty() ? "جار في نفع" : "حي " + hood);
            Data.ratingOf(db, otherId, rt -> { if (rt[1] > 0) sub.setText((hood.isEmpty() ? "جار في نفع" : "حي " + hood) + "  ·  " + Data.rating(rt[0])); });
        });

        scroll = Ui.scroll(this);
        scroll.setFillViewport(true);
        msgs = Ui.col(this);
        Ui.pad(msgs, 12, 12, 12, 12);
        scroll.addView(msgs, new FrameLayout.LayoutParams(-1, -2));
        msgs.addView(Ui.loading(this));
        root.addView(scroll, Ui.lp(this, Ui.MATCH, 0, 1));

        LinearLayout inputBar = Ui.row(this);
        inputBar.setBackgroundColor(Ui.WHITE);
        inputBar.setGravity(Gravity.BOTTOM);
        Ui.pad(inputBar, 12, 10, 12, 10);
        input = new EditText(this);
        input.setHint("اكتب رسالتك...");
        input.setHintTextColor(Ui.MUTED);
        input.setTextColor(Ui.INK);
        input.setTextSize(15);
        input.setMaxLines(5);
        input.setMinHeight(Ui.dp(this, 48));
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setBackground(Ui.shape(this, Ui.FIELD, 10));
        input.setPadding(Ui.dp(this, 18), Ui.dp(this, 12), Ui.dp(this, 18), Ui.dp(this, 12));
        input.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        inputBar.addView(input, Ui.lp(this, 0, Ui.WRAP, 1));
        sendBtn = Ui.iconButton(this, R.drawable.ic_send, Ui.WHITE, Ui.GREEN, 48, v -> send(input.getText().toString()));
        sendBtn.getChildAt(0).setScaleX(-1f);
        inputBar.addView(sendBtn, Ui.m(this, Ui.lp(this, 48, 48), 8, 0, 0, 0));
        root.addView(inputBar, Ui.lp(this, Ui.MATCH, Ui.WRAP));

        input.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(android.text.Editable e) { sendBtn.setAlpha(e.toString().trim().isEmpty() ? 0.5f : 1f); }
        });
        sendBtn.setAlpha(0.5f);
        scroll.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> { if (b < ob) toBottom(false); });
        setContentView(root);
    }

    private void listen() {
        listeners.add(db.collection("conversations").document(convId).addSnapshotListener((d, e) -> {
            if (d == null || !d.exists()) return;
            Object u = d.get("unread");
            int mine = 0;
            otherUnread = 0;
            if (u instanceof Map) {
                Object a = ((Map<?, ?>) u).get(uid), b = ((Map<?, ?>) u).get(otherId);
                if (a instanceof Number) mine = ((Number) a).intValue();
                if (b instanceof Number) otherUnread = ((Number) b).intValue();
            }
            if (mine > 0) resetUnread();
            if (!firstLoad) render();
        }));
        listeners.add(db.collection("conversations").document(convId).collection("messages")
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(300)
                .addSnapshotListener((q, e) -> {
                    if (q == null) {
                        if (firstLoad) {
                            msgs.removeAllViews();
                            msgs.addView(Ui.empty(this, R.drawable.ic_error_outline, "تعذر تحميل الرسائل", "تحقق من الاتصال بالإنترنت"));
                        }
                        return;
                    }
                    messages = new ArrayList<>(q.getDocuments());
                    Collections.reverse(messages);
                    firstLoad = false;
                    render();
                }));
    }

    private void resetUnread() {
        Map<String, Object> un = Data.map(uid, 0L);
        db.collection("conversations").document(convId).set(Data.map("unread", un), SetOptions.merge());
    }

    private void render() {
        boolean atBottom = !scroll.canScrollVertically(1);
        msgs.removeAllViews();
        if (messages.isEmpty()) { emptyState(); return; }
        int unreadMine = Math.max(0, otherUnread);
        String lastDay = null;
        String prevSender = null;
        for (int i = 0; i < messages.size(); i++) {
            DocumentSnapshot m = messages.get(i);
            long t = Data.millis(m, "createdAt");
            String day = dayLabel(t);
            if (!day.equals(lastDay)) {
                lastDay = day;
                prevSender = null;
                TextView sep = Ui.chip(this, day, 0xFF5F6663, Ui.WHITE, 0);
                LinearLayout.LayoutParams sp = Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 0, 10, 0, 10);
                sp.gravity = Gravity.CENTER_HORIZONTAL;
                msgs.addView(sep, sp);
            }
            String sender = m.getString("senderId");
            boolean mine = uid.equals(sender);
            int mineAfter = 0;
            if (mine) for (int k = i + 1; k < messages.size(); k++) if (uid.equals(messages.get(k).getString("senderId"))) mineAfter++;
            boolean read = mine && mineAfter >= unreadMine;
            boolean pending = m.getMetadata() != null && m.getMetadata().hasPendingWrites();
            msgs.addView(bubble(m, mine, read, pending, sender != null && sender.equals(prevSender)));
            prevSender = sender;
        }
        if (atBottom || firstShown) toBottom(!firstShown);
        firstShown = false;
    }

    private boolean firstShown = true;

    private void emptyState() {
        LinearLayout c = Ui.col(this);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.addView(Ui.empty(this, R.drawable.ic_forum, "ابدأ المحادثة مع " + Data.firstName(otherName),
                "اتفقا على موعد ومكان الاستلام بكل وضوح، واحرص على بقاء التواصل داخل التطبيق"));
        LinearLayout chips = Ui.col(this);
        chips.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String q : QUICK) {
            TextView t = Ui.choice(this, q, 0);
            t.setOnClickListener(v -> send(q));
            chips.addView(t, Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 0, 0, 0, 8));
        }
        c.addView(chips);
        msgs.addView(c, Ui.lp(this, Ui.MATCH, Ui.WRAP));
    }

    private View bubble(DocumentSnapshot m, boolean mine, boolean read, boolean pending, boolean grouped) {
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int maxText = Math.round(screenW * 0.72f) - Ui.dp(this, 28);

        TextView t = new TextView(this);
        t.setText(Data.str(m, "text"));
        t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
        t.setTextColor(mine ? Ui.WHITE : Ui.INK);
        t.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
        t.setSingleLine(false);
        t.setMaxLines(Integer.MAX_VALUE);
        t.setHorizontallyScrolling(false);
        t.setLineSpacing(0, 1.2f);
        t.setMaxWidth(maxText);
        t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        t.setGravity(Gravity.START);
        t.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);
        t.setLinkTextColor(mine ? Ui.LIME : Ui.GREEN);
        android.text.util.Linkify.addLinks(t, android.text.util.Linkify.WEB_URLS | android.text.util.Linkify.PHONE_NUMBERS);
        final String copy = Data.str(m, "text");

        LinearLayout meta = Ui.row(this);
        meta.addView(Ui.text(this, Data.clock(Data.millis(m, "createdAt")), 11, mine ? 0xCCFFFFFF : Ui.MUTED, false));
        if (mine) {
            int ic = pending ? R.drawable.ic_schedule : read ? R.drawable.ic_done_all : R.drawable.ic_check;
            meta.addView(Ui.iv(this, ic, read ? Ui.LIME : 0xCCFFFFFF, 15), Ui.m(this, Ui.lp(this, 15, 15), 4, 0, 0, 0));
        }

        LinearLayout b = Ui.col(this);
        float r = Ui.dp(this, 18), s = Ui.dp(this, 5);
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(mine ? Ui.GREEN : Ui.WHITE);
        if (!mine) g.setStroke(Ui.dp(this, 1), Ui.LINE);
        g.setCornerRadii(mine ? new float[]{r, r, s, s, r, r, r, r} : new float[]{s, s, r, r, r, r, r, r});
        b.setBackground(g);
        Ui.pad(b, 14, 9, 14, 7);
        b.addView(t, new LinearLayout.LayoutParams(ViewGroupLp.WRAP, ViewGroupLp.WRAP));
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(ViewGroupLp.WRAP, ViewGroupLp.WRAP);
        mp.gravity = Gravity.END;
        mp.topMargin = Ui.dp(this, 2);
        b.addView(meta, mp);

        View.OnLongClickListener copier = v -> {
            android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("رسالة", copy));
            Ui.toast(this, "تم نسخ الرسالة");
            return true;
        };
        b.setOnLongClickListener(copier);
        t.setOnLongClickListener(copier);

        LinearLayout wrap = Ui.row(this);
        wrap.setBaselineAligned(false);
        wrap.setGravity(mine ? Gravity.START : Gravity.END);
        wrap.addView(b, new LinearLayout.LayoutParams(ViewGroupLp.WRAP, ViewGroupLp.WRAP));
        wrap.setLayoutParams(Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, grouped ? 2 : 8, 0, 0));
        return wrap;
    }

    private static final class ViewGroupLp {
        static final int WRAP = LinearLayout.LayoutParams.WRAP_CONTENT;
    }

    private String dayLabel(long ms) {
        if (ms == 0) return "اليوم";
        Calendar now = Calendar.getInstance(), c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        if (now.get(Calendar.YEAR) == c.get(Calendar.YEAR)) {
            int d = now.get(Calendar.DAY_OF_YEAR) - c.get(Calendar.DAY_OF_YEAR);
            if (d == 0) return "اليوم";
            if (d == 1) return "أمس";
        }
        return new SimpleDateFormat("EEEE d MMMM", new Locale("ar")).format(new Date(ms));
    }

    private void toBottom(boolean smooth) {
        scroll.post(() -> {
            if (smooth) scroll.smoothScrollTo(0, msgs.getHeight());
            else scroll.scrollTo(0, msgs.getHeight());
        });
    }

    private void send(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) return;
        if (text.length() > 1000) { Ui.toast(this, "الرسالة طويلة جدا (الحد 1000 حرف)"); return; }
        input.setText("");
        boolean notifyOther = otherUnread == 0 || otherUnread == -1;
        Map<String, Object> msg = Data.map(
                "senderId", uid,
                "receiverId", otherId,
                "text", text,
                "createdAt", FieldValue.serverTimestamp());
        db.collection("conversations").document(convId).collection("messages").add(msg)
                .addOnFailureListener(e -> { Ui.toast(this, "تعذر إرسال الرسالة"); if (input.getText().length() == 0) input.setText(text); });
        Data.user(db, uid, me -> {
            String myName = Data.str(me, "fullName");
            Map<String, Object> names = Data.map(uid, myName, otherId, otherName);
            Map<String, Object> c = Data.map(
                    "participantIds", java.util.Arrays.asList(uid.compareTo(otherId) < 0 ? uid : otherId, uid.compareTo(otherId) < 0 ? otherId : uid),
                    "participantNames", names,
                    "lastMessage", text.length() > 120 ? text.substring(0, 120) + "…" : text,
                    "lastMessageAt", FieldValue.serverTimestamp(),
                    "lastSenderId", uid,
                    "unread", Data.map(otherId, FieldValue.increment(1), uid, 0L),
                    "updatedAt", FieldValue.serverTimestamp());
            db.collection("conversations").document(convId).set(c, SetOptions.merge());
            if (notifyOther) Data.notify(db, uid, otherId, "message", "رسالة جديدة من " + Data.firstName(myName),
                    text.length() > 80 ? text.substring(0, 80) + "…" : text, convId);
        });
        toBottom(true);
    }
}
