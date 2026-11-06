package com.example.nafa;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyToolsActivity extends BaseActivity {
    private static final String[] FILTERS = {"الكل", "متاحة", "معارة", "موقوفة"};
    private static final String[] FILTER_IDS = {null, "available", "borrowed", "paused"};

    private LinearLayout list, summary;
    private final List<TextView> chips = new ArrayList<>();
    private final List<DocumentSnapshot> all = new ArrayList<>();
    private int filter;

    @Override
    protected void onReady(Bundle saved) {
        Ui.Screen s = Ui.screen(this, "أدواتي", true);
        s.actions.addView(Ui.iconButton(this, R.drawable.ic_add, Ui.WHITE, 0, 42, v -> go(AddToolActivity.class)));

        summary = Ui.row(this);
        s.body.addView(summary, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 4, 0, 12));

        LinearLayout fr = Ui.row(this);
        for (int i = 0; i < FILTERS.length; i++) {
            TextView c = Ui.choice(this, FILTERS[i], 0);
            final int k = i;
            c.setOnClickListener(v -> { filter = k; render(); });
            chips.add(c);
            fr.addView(c, Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 0, 0, 8, 0));
        }
        s.body.addView(Ui.hscroll(this, fr), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 0, 0, 14));

        list = Ui.col(this);
        s.body.addView(list, Ui.lp(this, Ui.MATCH, Ui.WRAP));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ready) load();
    }

    private void load() {
        list.removeAllViews();
        list.addView(Ui.loading(this));
        db.collection("tools").whereEqualTo("ownerId", uid).get().addOnSuccessListener(q -> {
            all.clear();
            all.addAll(q.getDocuments());
            Collections.sort(all, (x, y) -> Long.compare(Data.millis(y, "createdAt"), Data.millis(x, "createdAt")));
            stats();
            render();
        }).addOnFailureListener(e -> {
            list.removeAllViews();
            list.addView(Ui.empty(this, R.drawable.ic_error_outline, "تعذر تحميل أدواتك", "تحقق من الاتصال بالإنترنت ثم حاول مجددا"));
        });
    }

    private void stats() {
        summary.removeAllViews();
        int av = 0, br = 0;
        for (DocumentSnapshot d : all) {
            String st = d.getString("status");
            if ("borrowed".equals(st)) br++;
            else if (!"paused".equals(st)) av++;
        }
        stat(String.valueOf(all.size()), "إجمالي الأدوات", Ui.GREEN_DARK, Ui.MINT, R.drawable.ic_inventory_2, 0, 0);
        stat(String.valueOf(av), "متاحة", Ui.GREEN_DARK, Ui.WHITE, R.drawable.ic_check_circle, 8, 1);
        stat(String.valueOf(br), "معارة", 0xFF9A6A00, Ui.AMBER_SOFT, R.drawable.ic_schedule, 8, 2);
    }

    private void stat(String num, String label, int fg, int bg, int icon, int gap, int filterIdx) {
        LinearLayout c = Ui.col(this);
        Ui.tap(c, Ui.shape(this, bg, 10, Ui.LINE, 1), 10, v -> { filter = filterIdx; render(); });
        Ui.pad(c, 12, 12, 12, 12);
        c.addView(Ui.iv(this, icon, fg, 20));
        c.addView(Ui.text(this, num, 22, fg, true), Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 0, 6, 0, 0));
        c.addView(Ui.lines(Ui.text(this, label, 12, Ui.MUTED, false), 1));
        summary.addView(c, Ui.m(this, Ui.lp(this, 0, Ui.WRAP, 1), gap, 0, 0, 0));
    }

    private void render() {
        for (int i = 0; i < chips.size(); i++) Ui.setChoice(chips.get(i), i == filter);
        list.removeAllViews();
        if (all.isEmpty()) {
            list.addView(Ui.empty(this, R.drawable.ic_handyman, "لم تضف أي أداة بعد",
                    "شارك أدواتك غير المستخدمة مع جيرانك وكن سببا في النفع"));
            TextView add = Ui.button(this, "أضف أول أداة", Ui.PRIMARY, R.drawable.ic_add);
            add.setOnClickListener(v -> go(AddToolActivity.class));
            list.addView(add, Ui.m(this, Ui.lp(this, Ui.MATCH, 52), 24, 0, 24, 0));
            return;
        }
        int shown = 0;
        for (DocumentSnapshot d : all) {
            String st = d.getString("status");
            if (st == null || st.isEmpty()) st = "available";
            if (FILTER_IDS[filter] != null && !FILTER_IDS[filter].equals(st)) continue;
            list.addView(item(d, st), Ui.stack(this));
            shown++;
        }
        if (shown == 0) list.addView(Ui.empty(this, R.drawable.ic_inbox, "لا توجد أدوات في هذا التصنيف", null));
    }

    private LinearLayout item(DocumentSnapshot d, String st) {
        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.shape(this, Ui.WHITE, 10, Ui.LINE, 1));
        LinearLayout top = Cards.row(this, db, d, true);
        Ui.tap(top, null, 20, v -> Cards.openTool(this, d.getId()));
        card.addView(top, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        card.addView(Ui.divider(this, 0));

        LinearLayout acts = Ui.row(this);
        acts.setGravity(Gravity.CENTER_VERTICAL);
        Ui.pad(acts, 8, 6, 8, 6);
        acts.addView(action(R.drawable.ic_edit, "تعديل", Ui.GREEN_DARK, v -> {
            Intent i = new Intent(this, AddToolActivity.class);
            i.putExtra("toolId", d.getId());
            startActivity(i);
        }), Ui.lp(this, 0, Ui.WRAP, 1));
        if (!"borrowed".equals(st)) {
            boolean paused = "paused".equals(st);
            acts.addView(action(paused ? R.drawable.ic_play_circle : R.drawable.ic_pause_circle,
                    paused ? "تفعيل" : "إيقاف", Ui.GREEN_DARK, v -> toggle(this, db, d, this::load)), Ui.lp(this, 0, Ui.WRAP, 1));
        } else {
            acts.addView(action(R.drawable.ic_swap_horiz, "الإعارة الحالية", 0xFF9A6A00, v -> Data.openRequests(this)), Ui.lp(this, 0, Ui.WRAP, 1));
        }
        acts.addView(action(R.drawable.ic_delete, "حذف", Ui.WARN, v -> delete(this, db, d, this::load)), Ui.lp(this, 0, Ui.WRAP, 1));
        card.addView(acts, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        return card;
    }

    private TextView action(int icon, String label, int color, android.view.View.OnClickListener l) {
        TextView t = Ui.medium(this, label, 13, color);
        Ui.center(t);
        t.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        t.setCompoundDrawablesRelative(Ui.icon(this, icon, color, 18), null, null, null);
        t.setCompoundDrawablePadding(Ui.dp(this, 6));
        if (l != null) Ui.tap(t, null, 14, l);
        return t;
    }

    public static void toggle(Activity a, FirebaseFirestore db, DocumentSnapshot d, Runnable done) {
        String st = d.getString("status");
        if ("borrowed".equals(st)) { Ui.toast(a, "لا يمكن تغيير حالة أداة معارة حاليا"); return; }
        boolean pause = !"paused".equals(st);
        String ns = pause ? "paused" : "available";
        db.collection("tools").document(d.getId())
                .update(Data.map("status", ns, "isAvailable", !pause, "updatedAt", FieldValue.serverTimestamp()))
                .addOnSuccessListener(v -> {
                    Ui.toast(a, pause ? "تم إيقاف عرض الأداة مؤقتا" : "أصبحت الأداة متاحة للاستعارة");
                    if (done != null) done.run();
                })
                .addOnFailureListener(e -> Ui.toast(a, "تعذر تحديث الحالة، حاول مرة أخرى"));
    }

    public static void delete(Activity a, FirebaseFirestore db, DocumentSnapshot d, Runnable done) {
        if ("borrowed".equals(d.getString("status"))) {
            Ui.toast(a, "لا يمكن حذف أداة معارة حاليا، انتظر حتى تعاد");
            return;
        }
        Ui.confirm(a, "حذف الأداة", "سيتم حذف «" + Data.str(d, "toolName") + "» نهائيا مع صورها، ولن تظهر لجيرانك بعد الآن.",
                "حذف", true, () -> db.collection("tool_images").whereEqualTo("toolId", d.getId()).get()
                        .continueWithTask(t -> {
                            WriteBatch b = db.batch();
                            if (t.isSuccessful() && t.getResult() != null)
                                for (DocumentSnapshot im : t.getResult().getDocuments()) b.delete(im.getReference());
                            b.delete(db.collection("tools").document(d.getId()));
                            return b.commit();
                        })
                        .addOnSuccessListener(v -> {
                            Ui.toast(a, "تم حذف الأداة");
                            if (done != null) done.run();
                        })
                        .addOnFailureListener(e -> Ui.toast(a, "تعذر حذف الأداة، حاول مرة أخرى")));
    }
}
