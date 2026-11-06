package com.example.nafa;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolDetailsActivity extends BaseActivity {
    private String id;
    private DocumentSnapshot tool, owner;
    private LinearLayout root;
    private final List<String> images = new ArrayList<>();

    @Override
    protected void onReady(Bundle saved) {
        id = getIntent().getStringExtra("toolId");
        if (id == null) { finish(); return; }
        root = Ui.col(this);
        root.setBackgroundColor(Ui.WHITE);
        root.addView(Ui.loading(this), Ui.lp(this, Ui.MATCH, Ui.MATCH));
        setContentView(root);
        bars(Ui.WHITE, Ui.WHITE, true);
        if (!Data.favsLoaded()) Data.loadFavs(db, uid, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ready && id != null) load();
    }

    private void load() {
        db.collection("tools").document(id).get().addOnSuccessListener(d -> {
            if (!d.exists()) { Data.forgetFav(db, uid, id); Ui.toast(this, "هذه الأداة لم تعد متوفرة"); finish(); return; }
            tool = d;
            Data.user(db, d.getString("ownerId"), u -> { owner = u; render(); });
        }).addOnFailureListener(e -> { Ui.toast(this, "تعذر تحميل الأداة، تحقق من الاتصال"); finish(); });
    }

    private void render() {
        root.removeAllViews();
        boolean mine = uid.equals(tool.getString("ownerId"));
        String status = tool.getString("status");
        String on = Data.str(owner, "fullName");
        if (on.isEmpty()) on = Data.str(tool, "ownerName");
        final String ownerName = on.isEmpty() ? "مستخدم نفع" : on;

        ScrollView sc = Ui.scroll(this);
        LinearLayout col = Ui.col(this);
        sc.addView(col);
        root.addView(sc, Ui.lp(this, Ui.MATCH, 0, 1));

        int heroH = Ui.clamp(Math.round(Ui.screenDp(this) * 0.85f), 260, 400);
        FrameLayout hero = Cards.toolImage(this, null, tool, 0, 96);
        Cards.Gallery g = Cards.gallery(this, false);
        hero.addView(g.root, new FrameLayout.LayoutParams(-1, -1));
        g.onTap = () -> Cards.viewer(this, images, g.index);
        if (!images.isEmpty()) g.set(images);
        db.collection("tool_images").whereEqualTo("toolId", id).get().addOnSuccessListener(q -> {
            images.clear();
            images.addAll(Cards.sortedImages(q.getDocuments()));
            g.set(images);
        });

        FrameLayout back = Ui.iconButton(this, R.drawable.ic_arrow_forward, Ui.INK, Ui.WHITE, 42, v -> finish());
        hero.addView(back, corner(Gravity.TOP | Gravity.START, 0));
        LinearLayout tools = Ui.row(this);
        FrameLayout share = Ui.iconButton(this, R.drawable.ic_share, Ui.INK, Ui.WHITE, 42, v -> Data.shareTool(this, tool));
        tools.addView(share, Ui.lp(this, 42, 42));
        FrameLayout heart = Cards.heart(this, db, tool, 42);
        tools.addView(heart, Ui.m(this, Ui.lp(this, 42, 42), 8, 0, 0, 0));
        if (!mine) {
            FrameLayout more = Ui.iconButton(this, R.drawable.ic_flag, Ui.WARN, Ui.WHITE, 42,
                    v -> Actions.report(this, db, uid, "tool", id, Data.str(tool, "toolName")));
            more.setContentDescription("إبلاغ");
            tools.addView(more, Ui.m(this, Ui.lp(this, 42, 42), 8, 0, 0, 0));
        }
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-2, Ui.dp(this, 42), Gravity.TOP | Gravity.END);
        tp.setMargins(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), 0);
        hero.addView(tools, tp);
        col.addView(hero, Ui.lp(this, Ui.MATCH, heroH));

        LinearLayout body = Ui.col(this);
        body.setBackgroundColor(Ui.WHITE);
        Ui.pad(body, 20, 22, 20, 24);
        LinearLayout.LayoutParams bl = Ui.lp(this, Ui.MATCH, Ui.WRAP);
                col.addView(body, bl);

        body.addView(Ui.text(this, Data.str(tool, "toolName"), 24, Ui.INK, true));
        LinearLayout chips = Ui.row(this);
        chips.addView(Cards.statusChip(this, status));
        addChip(chips, Ui.chip(this, Data.catName(tool.getString("categoryId")), Ui.GREEN_DARK, Ui.MINT, Data.catIcon(tool.getString("categoryId"))));
        String cond = Data.conditionName(tool.getString("condition"));
        if (cond != null) addChip(chips, Ui.chip(this, "الحالة: " + cond, Ui.INK, Ui.FIELD, R.drawable.ic_verified));
        long lent = Data.num(tool.getLong("borrowCount"));
        if (lent > 0) addChip(chips, Ui.chip(this, "أعيرت " + Data.count(lent, "مرة", "مرتين", "مرات", "مرة"), 0xFF9A6A00, Ui.AMBER_SOFT, R.drawable.ic_history));
        body.addView(Ui.hscroll(this, chips), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 10, 0, 0));

        LinearLayout facts = Ui.row(this);
        facts.setGravity(Gravity.TOP);
        facts.setBackground(Ui.shape(this, Ui.CANVAS, 10));
        Ui.pad(facts, 8, 14, 8, 14);
        long max = Data.num(tool.getLong("maxDurationValue"));
        fact(facts, R.drawable.ic_schedule, "أقصى مدة", max > 0 ? Data.duration(max, tool.getString("maxDurationUnit")) : "حسب الاتفاق");
        fact(facts, R.drawable.ic_location_on, "الحي", Data.hoodName(tool.getString("neighborhoodId")));
        fact(facts, R.drawable.ic_event, "أضيفت", Data.ago(Data.millis(tool, "createdAt")));
        body.addView(facts, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 16, 0, 0));

        section(body, "الوصف", Data.str(tool, "description"));
        section(body, "شروط الإعارة", Data.str(tool, "details"));
        section(body, "مكان الاستلام", Data.str(tool, "locationText"));

        body.addView(Ui.section(this, "صاحب الأداة", null, null));
        LinearLayout oc = Ui.row(this);
        Ui.pad(oc, 14);
        oc.addView(Ui.avatar(this, ownerName, owner == null ? null : owner.getString("profileImageBase64"), 52));
        LinearLayout ot = Ui.col(this);
        LinearLayout nameRow = Ui.row(this);
        nameRow.addView(Ui.lines(Ui.text(this, mine ? ownerName + " (أنت)" : ownerName, 16, Ui.INK, true), 1));
        if (owner != null && Boolean.TRUE.equals(owner.getBoolean("isVerified")))
            nameRow.addView(Ui.iv(this, R.drawable.ic_verified, Ui.GREEN, 16), Ui.m(this, Ui.lp(this, 16, 16), 6, 0, 0, 0));
        ot.addView(nameRow);
        LinearLayout rr = Ui.row(this);
        TextView rtxt = Ui.lines(Ui.text(this, "...", 12, Ui.MUTED, false), 1);
        LinearLayout starsBox = Ui.row(this);
        starsBox.addView(Ui.stars(this, 0, 14));
        rr.addView(starsBox);
        rr.addView(rtxt, Ui.m(this, Ui.lp(this, 0, Ui.WRAP, 1), 6, 0, 0, 0));
        Data.ratingOf(db, tool.getString("ownerId"), rt -> {
            starsBox.removeAllViews();
            starsBox.addView(Ui.stars(this, rt[0], 14));
            long cnt = (long) rt[1];
            rtxt.setText(cnt == 0 ? "لا توجد تقييمات بعد" : Data.rating(rt[0]) + " (" + Data.count(cnt, "تقييم", "تقييمان", "تقييمات", "تقييما") + ")");
        });
        ot.addView(rr, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 4, 0, 0));
        oc.addView(ot, Ui.m(this, Ui.lp(this, 0, Ui.WRAP, 1), 12, 0, 8, 0));
        oc.addView(Ui.iv(this, R.drawable.ic_chevron_left, Ui.MUTED, 20));
        Ui.tap(oc, Ui.shape(this, Ui.WHITE, 10, Ui.LINE, 1), 10, v -> Data.openProfile(this, tool.getString("ownerId"), ownerName));
        body.addView(oc, Ui.lp(this, Ui.MATCH, Ui.WRAP));

        LinearLayout reviews = Ui.col(this);
        body.addView(reviews, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        loadToolReviews(reviews, ownerName);

        if (!mine) {
            LinearLayout tip = Ui.row(this);
            tip.setGravity(Gravity.TOP);
            tip.setBackground(Ui.shape(this, Ui.AMBER_SOFT, 10));
            Ui.pad(tip, 14, 12, 14, 12);
            tip.addView(Ui.iv(this, R.drawable.ic_privacy_tip, 0xFF9A6A00, 20));
            tip.addView(Ui.text(this, "افحص الأداة عند الاستلام، واتفق على موعد الإرجاع عبر المحادثة داخل التطبيق.", 13, 0xFF7A5500, false),
                    Ui.m(this, Ui.lp(this, 0, Ui.WRAP, 1), 10, 0, 0, 0));
            body.addView(tip, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 18, 0, 0));
        }

        LinearLayout foot = Ui.row(this);
        foot.setBackgroundColor(Ui.WHITE);
        Ui.pad(foot, 16, 12, 16, 12);
        root.addView(foot, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        if (mine) ownerFooter(foot, status);
        else borrowerFooter(foot, status, ownerName);
    }

    private FrameLayout.LayoutParams corner(int gravity, int extra) {
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42), gravity);
        bp.setMargins(Ui.dp(this, 14 + extra), Ui.dp(this, 12), Ui.dp(this, 14 + extra), 0);
        return bp;
    }

    private void loadToolReviews(LinearLayout into, String ownerName) {
        db.collection("reviews").whereEqualTo("toolId", id).get().addOnSuccessListener(q -> {
            List<DocumentSnapshot> l = new ArrayList<>();
            for (DocumentSnapshot d : q.getDocuments()) if (!"lender".equals(d.getString("role"))) l.add(d);
            if (l.isEmpty()) return;
            Collections.sort(l, (x, y) -> Long.compare(Data.millis(y, "createdAt"), Data.millis(x, "createdAt")));
            double sum = 0;
            for (DocumentSnapshot d : l) sum += Data.dbl(d.getDouble("rating"));
            double avg = sum / l.size();
            LinearLayout head = Ui.row(this);
            head.setPadding(0, Ui.dp(this, 20), 0, Ui.dp(this, 10));
            head.addView(Ui.text(this, "تقييمات المستعيرين", 18, Ui.INK, true), Ui.lp(this, 0, Ui.WRAP, 1));
            head.addView(Ui.chip(this, Data.rating(avg) + " · " + Data.count(l.size(), "تقييم", "تقييمان", "تقييمات", "تقييما"), 0xFF9A6A00, Ui.AMBER_SOFT, R.drawable.ic_star));
            into.addView(head);
            for (int i = 0; i < Math.min(2, l.size()); i++) into.addView(ReviewsActivity.reviewCard(this, db, l.get(i)), Ui.stack(this));
            if (l.size() > 2) {
                TextView all = Ui.link(this, "عرض كل تقييمات " + Data.firstName(ownerName), v -> {
                    Intent i = new Intent(this, ReviewsActivity.class);
                    i.putExtra("userId", tool.getString("ownerId"));
                    i.putExtra("userName", ownerName);
                    startActivity(i);
                });
                into.addView(all);
            }
        });
    }

    private void addChip(LinearLayout into, TextView c) {
        into.addView(c);
        ((LinearLayout.LayoutParams) c.getLayoutParams()).setMarginStart(Ui.dp(this, 6));
    }

    private void fact(LinearLayout into, int icon, String label, String value) {
        LinearLayout c = Ui.col(this);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.addView(Ui.badgeIcon(this, icon, Ui.GREEN, Ui.WHITE, 38, 20));
        c.addView(Ui.center(Ui.text(this, label, 12, Ui.MUTED, false)), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 6, 0, 0));
        c.addView(Ui.center(Ui.lines(Ui.text(this, value, 13, Ui.INK, true), 2)), Ui.lp(this, Ui.MATCH, Ui.WRAP));
        into.addView(c, Ui.lp(this, 0, Ui.WRAP, 1));
    }

    private void section(LinearLayout body, String title, String text) {
        if (text == null || text.isEmpty()) return;
        body.addView(Ui.text(this, title, 17, Ui.INK, true), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 20, 0, 6));
        TextView t = Ui.text(this, text, 15, 0xFF4A4F4D, false);
        t.setLineSpacing(0, 1.3f);
        t.setTextIsSelectable(true);
        body.addView(t, Ui.lp(this, Ui.MATCH, Ui.WRAP));
    }

    private void ownerFooter(LinearLayout foot, String status) {
        TextView edit = Ui.button(this, "تعديل", Ui.PRIMARY, R.drawable.ic_edit);
        edit.setOnClickListener(v -> {
            Intent i = new Intent(this, AddToolActivity.class);
            i.putExtra("toolId", id);
            startActivity(i);
        });
        foot.addView(edit, Ui.lp(this, 0, 52, 1));
        if ("borrowed".equals(status)) {
            TextView req = Ui.button(this, "الإعارة الحالية", Ui.SOFT, R.drawable.ic_swap_horiz);
            req.setOnClickListener(v -> Data.openRequests(this));
            foot.addView(req, Ui.m(this, Ui.lp(this, 0, 52, 1), 8, 0, 0, 0));
        } else {
            boolean paused = "paused".equals(status);
            TextView tg = Ui.button(this, paused ? "تفعيل" : "إيقاف مؤقت", Ui.SOFT, paused ? R.drawable.ic_play_circle : R.drawable.ic_pause_circle);
            tg.setOnClickListener(v -> MyToolsActivity.toggle(this, db, tool, this::load));
            foot.addView(tg, Ui.m(this, Ui.lp(this, 0, 52, 1), 8, 0, 0, 0));
        }
        FrameLayout del = Ui.iconButton(this, R.drawable.ic_delete, Ui.WARN, Ui.WARN_SOFT, 52, v -> MyToolsActivity.delete(this, db, tool, this::finish));
        del.setContentDescription("حذف");
        foot.addView(del, Ui.m(this, Ui.lp(this, 52, 52), 8, 0, 0, 0));
    }

    private void borrowerFooter(LinearLayout foot, String status, String ownerName) {
        FrameLayout chat = Ui.iconButton(this, R.drawable.ic_chat, Ui.GREEN, Ui.MINT, 52,
                v -> Data.openChat(this, db, uid, tool.getString("ownerId"), ownerName));
        chat.setContentDescription("مراسلة المالك");
        TextView req = Ui.button(this, "طلب استعارة", Ui.PRIMARY, R.drawable.ic_swap_horiz);
        foot.addView(req, Ui.lp(this, 0, 52, 1));
        foot.addView(chat, Ui.m(this, Ui.lp(this, 52, 52), 8, 0, 0, 0));
        if (!"available".equals(status)) {
            req.setText("borrowed".equals(status) ? "معارة حاليا" : "غير متاحة حاليا");
            Ui.enable(req, false);
            return;
        }
        Ui.enable(req, false);
        db.collection("borrow_requests").whereEqualTo("toolId", id).whereEqualTo("borrowerId", uid).get().addOnSuccessListener(q -> {
            for (DocumentSnapshot d : q.getDocuments()) {
                String st = d.getString("status");
                if ("pending".equals(st) || "accepted".equals(st)) {
                    req.setText("pending".equals(st) ? "طلبك بانتظار الرد" : "طلبك مقبول");
                    req.setOnClickListener(v -> Data.openRequests(this));
                    Ui.enable(req, true);
                    req.setAlpha(0.85f);
                    return;
                }
            }
            Ui.enable(req, true);
            req.setOnClickListener(v -> requestSheet(ownerName));
        }).addOnFailureListener(e -> { Ui.enable(req, true); req.setOnClickListener(v -> requestSheet(ownerName)); });
    }

    private void requestSheet(String ownerName) {
        LinearLayout c = Ui.col(this);
        long maxRaw = Data.num(tool.getLong("maxDurationValue"));
        long max = Math.max(1, maxRaw == 0 ? 30 : maxRaw);
        String unit = tool.getString("maxDurationUnit") == null ? "day" : tool.getString("maxDurationUnit");

        c.addView(Ui.medium(this, "متى تحتاجها؟", 14, Ui.INK));
        long[] start = {Data.startOfDay(System.currentTimeMillis())};
        LinearLayout dates = Ui.row(this);
        TextView today = Ui.choice(this, "اليوم", 0), tomorrow = Ui.choice(this, "غدا", 0), other = Ui.choice(this, "تاريخ آخر", R.drawable.ic_event);
        TextView[] all = {today, tomorrow, other};
        Runnable paint = () -> {
            long t0 = Data.startOfDay(System.currentTimeMillis());
            Ui.setChoice(today, start[0] == t0);
            Ui.setChoice(tomorrow, start[0] == t0 + 86_400_000L);
            boolean custom = start[0] != t0 && start[0] != t0 + 86_400_000L;
            Ui.setChoice(other, custom);
            other.setText(custom ? Data.day(start[0]) : "تاريخ آخر");
        };
        today.setOnClickListener(v -> { start[0] = Data.startOfDay(System.currentTimeMillis()); paint.run(); });
        tomorrow.setOnClickListener(v -> { start[0] = Data.startOfDay(System.currentTimeMillis()) + 86_400_000L; paint.run(); });
        other.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(start[0]);
            DatePickerDialog dp = new DatePickerDialog(this, (dv, y, m, dd) -> {
                Calendar p = Calendar.getInstance();
                p.set(y, m, dd, 0, 0, 0);
                start[0] = Data.startOfDay(p.getTimeInMillis());
                paint.run();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dp.getDatePicker().setMinDate(Data.startOfDay(System.currentTimeMillis()));
            dp.getDatePicker().setMaxDate(System.currentTimeMillis() + 60L * 86_400_000L);
            dp.show();
        });
        for (TextView t : all) dates.addView(t, Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 0, 0, 8, 0));
        c.addView(Ui.hscroll(this, dates), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 8, 0, 14));
        paint.run();

        c.addView(Ui.medium(this, "المدة المطلوبة", 14, Ui.INK));
        long[] val = {1};
        LinearLayout step = Ui.row(this);
        step.setBackground(Ui.shape(this, Ui.FIELD, 10));
        Ui.pad(step, 8);
        TextView num = Ui.center(Ui.text(this, Data.duration(1, unit), 18, Ui.INK, true));
        step.addView(Ui.iconButton(this, R.drawable.ic_add, Ui.GREEN, Ui.WHITE, 44, v -> {
            if (val[0] < max) { val[0]++; num.setText(Data.duration(val[0], unit)); }
            else Ui.toast(this, "هذه أقصى مدة يسمح بها المالك");
        }));
        step.addView(num, Ui.lp(this, 0, Ui.WRAP, 1));
        FrameLayout minus = new FrameLayout(this);
        minus.addView(Ui.center(Ui.text(this, "−", 24, Ui.GREEN, true)), new FrameLayout.LayoutParams(-1, -1));
        Ui.tap(minus, Ui.oval(Ui.WHITE), 10, v -> { if (val[0] > 1) { val[0]--; num.setText(Data.duration(val[0], unit)); } });
        step.addView(minus, Ui.lp(this, 44, 44));
        c.addView(step, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 8, 0, 4));
        c.addView(Ui.text(this, "أقصى مدة يسمح بها المالك: " + Data.duration(max, unit), 12, Ui.MUTED, false), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 4, 0, 4, 12));

        Ui.Field msg = Ui.area(this, "رسالة لصاحب الأداة: لماذا تحتاجها؟ (اختياري)", 3);
        c.addView(msg.box, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        TextView send = Ui.button(this, "إرسال الطلب", Ui.PRIMARY, R.drawable.ic_check);
        c.addView(send, Ui.m(this, Ui.lp(this, Ui.MATCH, 54), 0, 16, 0, 0));
        Dialog d = Ui.sheet(this, "طلب استعارة «" + Data.str(tool, "toolName") + "»", c);

        send.setOnClickListener(v -> {
            Ui.busy(send, true, "جار الإرسال...");
            Data.user(db, uid, me -> {
                Map<String, Object> x = new HashMap<>();
                x.put("toolId", id);
                x.put("toolName", Data.str(tool, "toolName"));
                x.put("ownerId", tool.getString("ownerId"));
                x.put("ownerName", ownerName);
                x.put("borrowerId", uid);
                x.put("borrowerName", Data.str(me, "fullName"));
                x.put("durationValue", val[0]);
                x.put("durationUnit", unit);
                x.put("startDate", new Timestamp(new Date(start[0])));
                x.put("message", msg.value());
                x.put("status", "pending");
                x.put("ownerReviewed", false);
                x.put("borrowerReviewed", false);
                x.put("requestedAt", FieldValue.serverTimestamp());
                x.put("updatedAt", FieldValue.serverTimestamp());
                db.collection("borrow_requests").add(x).addOnSuccessListener(r -> {
                    Data.notify(db, uid, tool.getString("ownerId"), "request", "طلب استعارة جديد",
                            Data.str(me, "fullName") + " يطلب استعارة «" + Data.str(tool, "toolName") + "» لمدة "
                                    + Data.duration(val[0], unit) + " ابتداء من " + Data.day(start[0]), r.getId());
                    d.dismiss();
                    Ui.toast(this, "تم إرسال طلبك، سنخبرك عند رد المالك");
                    load();
                }).addOnFailureListener(e -> { Ui.busy(send, false, null); Ui.toast(this, "تعذر إرسال الطلب، حاول مجددا"); });
            });
        });
    }
}
