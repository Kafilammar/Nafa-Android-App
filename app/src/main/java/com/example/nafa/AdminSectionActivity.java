package com.example.nafa;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AdminSectionActivity extends BaseActivity {
    public static final String USERS = "users", TOOLS = "tools", REQUESTS = "requests", REPORTS = "reports",
            SUPPORT = "support", REVIEWS = "reviews", HOODS = "hoods", BROADCAST = "broadcast", CONFIG = "config";

    private String section;
    private int filter;
    private Ui.Screen s;
    private LinearLayout list, chips;
    private Ui.Field search;
    private TextView countText;
    private String[] filters = new String[0];
    private final List<DocumentSnapshot> docs = new ArrayList<>();

    @Override
    protected void onReady(Bundle saved) {
        section = getIntent().getStringExtra("section");
        filter = getIntent().getIntExtra("filter", 0);
        if (section == null) { finish(); return; }
        s = Ui.screen(this, title(), true);
        s.body.addView(Ui.loading(this));
        Data.user(db, uid, me -> {
            if (!Data.isAdmin(me)) { Ui.toast(this, "هذه الصفحة متاحة للمدراء فقط"); finish(); return; }
            s.body.removeAllViews();
            setup();
        });
    }

    private String title() {
        switch (section) {
            case USERS: return "إدارة المستخدمين";
            case TOOLS: return "إدارة الأدوات";
            case REQUESTS: return "طلبات الاستعارة";
            case REPORTS: return "البلاغات";
            case SUPPORT: return "الدعم الفني";
            case REVIEWS: return "التقييمات";
            case HOODS: return "الأحياء";
            case BROADCAST: return "إشعار عام";
            default: return "إعدادات التطبيق";
        }
    }

    private void setup() {
        switch (section) {
            case USERS:
                listUi(new String[]{"الكل", "المدراء", "الموثقون", "الموقوفون"}, "ابحث بالاسم أو البريد أو الجوال");
                TextView sync = Ui.button(this, "مزامنة الملفات العامة", Ui.SOFT, R.drawable.ic_refresh);
                sync.setOnClickListener(v -> {
                    Ui.busy(sync, true, "جار المزامنة...");
                    Data.syncAllPublicProfiles(db, message -> {
                        Ui.busy(sync, false, null);
                        Ui.toast(this, message);
                    });
                });
                s.body.addView(sync, Ui.m(this, Ui.lp(this, Ui.MATCH, 50), 0, 0, 0, 10));
                break;
            case TOOLS: listUi(new String[]{"الكل", "متاحة", "معارة", "موقوفة"}, "ابحث باسم الأداة أو المالك"); break;
            case REQUESTS: listUi(new String[]{"الكل", "بانتظار الرد", "جارية", "مكتملة", "مغلقة", "متأخرة"}, "ابحث بالأداة أو المالك أو المستعير"); break;
            case REPORTS: listUi(new String[]{"مفتوحة", "قيد المراجعة", "تم الحل", "مرفوضة", "الكل"}, "ابحث في البلاغات"); break;
            case SUPPORT: listUi(new String[]{"مفتوحة", "قيد المعالجة", "تم الحل", "الكل"}, "ابحث في التذاكر"); break;
            case REVIEWS: listUi(new String[]{"الكل", "منخفضة (1-2)", "متوسطة (3)", "مرتفعة (4-5)"}, "ابحث بالاسم أو التعليق"); break;
            case HOODS:
                listUi(new String[]{"الكل", "مفعلة", "معطلة"}, null);
                s.actions.addView(Ui.iconButton(this, R.drawable.ic_add, Ui.WHITE, 0, 42, v -> hoodSheet(null)));
                break;
            case BROADCAST: broadcastUi(); return;
            default: configUi(); return;
        }
        if (section.equals(USERS) || section.equals(TOOLS) || section.equals(REQUESTS) || section.equals(REPORTS) || section.equals(SUPPORT) || section.equals(REVIEWS))
            s.actions.addView(Ui.iconButton(this, R.drawable.ic_refresh, Ui.WHITE, 0, 42, v -> load()));
        load();
    }

    private void listUi(String[] f, String hint) {
        filters = f;
        if (filter < 0 || filter >= f.length) filter = 0;
        if (hint != null) {
            search = Ui.field(this, hint, R.drawable.ic_search, InputType.TYPE_CLASS_TEXT);
            search.box.setBackground(Ui.shape(this, Ui.WHITE, 10, Ui.LINE, 1));
            search.edit.setOnFocusChangeListener(null);
            search.edit.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence a, int b, int c, int d) {}
                public void onTextChanged(CharSequence a, int b, int c, int d) {}
                public void afterTextChanged(Editable e) { render(); }
            });
            s.body.addView(search.box, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 4, 0, 10));
        }
        chips = Ui.row(this);
        s.body.addView(Ui.hscroll(this, chips), Ui.lp(this, Ui.MATCH, Ui.WRAP));
        countText = Ui.medium(this, "", 13, Ui.MUTED);
        s.body.addView(countText, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 4, 12, 4, 8));
        list = Ui.col(this);
        s.body.addView(list, Ui.lp(this, Ui.MATCH, Ui.WRAP));
    }

    private void load() {
        list.removeAllViews();
        list.addView(Ui.loading(this));
        Query q;
        final String timeField;
        switch (section) {
            case USERS: q = db.collection("users").limit(1500); timeField = "createdAt"; break;
            case TOOLS: q = db.collection("tools").limit(1500); timeField = "createdAt"; break;
            case REQUESTS: q = db.collection("borrow_requests").limit(1500); timeField = "requestedAt"; break;
            case REPORTS: q = db.collection("reports").limit(800); timeField = "createdAt"; break;
            case SUPPORT: q = db.collection("support_requests").limit(800); timeField = "createdAt"; break;
            case REVIEWS: q = db.collection("reviews").limit(1500); timeField = "createdAt"; break;
            default: q = db.collection("neighborhoods"); timeField = null;
        }
        q.get().addOnSuccessListener(r -> {
            docs.clear();
            docs.addAll(r.getDocuments());
            if (timeField != null) Collections.sort(docs, (x, y) -> Long.compare(Data.millis(y, timeField), Data.millis(x, timeField)));
            else Collections.sort(docs, (x, y) -> Long.compare(Data.num(x.getLong("sortOrder")), Data.num(y.getLong("sortOrder"))));
            render();
        }).addOnFailureListener(e -> {
            list.removeAllViews();
            list.addView(Ui.empty(this, R.drawable.ic_error_outline, "تعذر تحميل البيانات", Data.err(e, "تحقق من الاتصال")));
        });
    }

    private void render() {
        if (list == null) return;
        chips.removeAllViews();
        for (int i = 0; i < filters.length; i++) {
            int n = 0;
            for (DocumentSnapshot d : docs) if (match(d, i)) n++;
            final int k = i;
            TextView c = Ui.choice(this, filters[i] + "  " + n, 0);
            Ui.setChoice(c, i == filter);
            c.setOnClickListener(v -> { filter = k; render(); });
            chips.addView(c, Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 0, 0, 8, 0));
        }
        list.removeAllViews();
        String qn = search == null ? "" : Data.norm(search.value());
        int shown = 0;
        for (DocumentSnapshot d : docs) {
            if (!match(d, filter)) continue;
            if (!qn.isEmpty() && !Data.norm(hay(d)).contains(qn)) continue;
            if (shown >= 300) break;
            list.addView(row(d), Ui.stack(this));
            shown++;
        }
        countText.setText(shown == 0 ? "لا توجد نتائج" : "عدد النتائج: " + shown);
        if (shown == 0) {
            list.addView(Ui.empty(this, R.drawable.ic_inbox, "لا توجد عناصر هنا", qn.isEmpty() ? "جرب فلترا آخر" : "جرب كلمة بحث أخرى"));
            if (section.equals(HOODS) && docs.isEmpty()) {
                TextView imp = Ui.button(this, "استيراد الأحياء الافتراضية", Ui.PRIMARY, R.drawable.ic_location_on);
                imp.setOnClickListener(v -> importHoods());
                list.addView(imp, Ui.lp(this, Ui.MATCH, 52));
            }
        }
    }

    private boolean match(DocumentSnapshot d, int f) {
        String st = d.getString("status");
        switch (section) {
            case USERS:
                if (f == 1) return Data.isAdmin(d);
                if (f == 2) return Boolean.TRUE.equals(d.getBoolean("isVerified"));
                if (f == 3) return Boolean.FALSE.equals(d.getBoolean("isActive"));
                return true;
            case TOOLS:
                if (st == null || st.isEmpty()) st = "available";
                if (f == 1) return "available".equals(st);
                if (f == 2) return "borrowed".equals(st);
                if (f == 3) return "paused".equals(st);
                return true;
            case REQUESTS:
                if (f == 1) return "pending".equals(st);
                if (f == 2) return "accepted".equals(st);
                if (f == 3) return "returned".equals(st);
                if (f == 4) return "rejected".equals(st) || "cancelled".equals(st);
                if (f == 5) return "accepted".equals(st) && Data.millis(d, "dueAt") > 0 && Data.daysLeft(Data.millis(d, "dueAt")) < 0;
                return true;
            case REPORTS:
                if (st == null || st.isEmpty()) st = "open";
                if (f == 0) return "open".equals(st);
                if (f == 1) return "reviewing".equals(st);
                if (f == 2) return "resolved".equals(st);
                if (f == 3) return "dismissed".equals(st);
                return true;
            case SUPPORT:
                if (st == null || st.isEmpty()) st = "open";
                if (f == 0) return "open".equals(st);
                if (f == 1) return "in_progress".equals(st);
                if (f == 2) return "resolved".equals(st) || "closed".equals(st);
                return true;
            case REVIEWS: {
                double r = Data.dbl(d.getDouble("rating"));
                if (f == 1) return r <= 2;
                if (f == 2) return r > 2 && r < 4;
                if (f == 3) return r >= 4;
                return true;
            }
            default:
                boolean on = !Boolean.FALSE.equals(d.getBoolean("isActive"));
                if (f == 1) return on;
                if (f == 2) return !on;
                return true;
        }
    }

    private String hay(DocumentSnapshot d) {
        switch (section) {
            case USERS: return Data.str(d, "fullName") + " " + Data.str(d, "email") + " " + Data.str(d, "phone") + " " + Data.hoodName(d.getString("neighborhoodId"));
            case TOOLS: return Data.str(d, "toolName") + " " + Data.str(d, "ownerName") + " " + Data.str(d, "description");
            case REQUESTS: return Data.str(d, "toolName") + " " + Data.str(d, "ownerName") + " " + Data.str(d, "borrowerName");
            case REPORTS: return Data.str(d, "reason") + " " + Data.str(d, "targetName") + " " + Data.str(d, "details");
            case SUPPORT: return Data.str(d, "subject") + " " + Data.str(d, "message") + " " + Data.str(d, "userName") + " " + Data.str(d, "userEmail");
            case REVIEWS: return Data.str(d, "reviewerName") + " " + Data.str(d, "reviewedName") + " " + Data.str(d, "comment") + " " + Data.str(d, "toolName");
            default: return Data.str(d, "name");
        }
    }

    private View row(DocumentSnapshot d) {
        switch (section) {
            case USERS: return userRow(d);
            case TOOLS: return toolRow(d);
            case REQUESTS: return requestRow(d);
            case REPORTS: return reportRow(d);
            case SUPPORT: return supportRow(d);
            case REVIEWS: return reviewRow(d);
            default: return hoodRow(d);
        }
    }

    private final List<String> optLabels = new ArrayList<>();
    private final List<Integer> optIcons = new ArrayList<>(), optColors = new ArrayList<>();
    private final List<Runnable> optActions = new ArrayList<>();

    private void opt(String label, int icon, int color, Runnable r) {
        optLabels.add(label);
        optIcons.add(icon);
        optColors.add(color);
        optActions.add(r);
    }

    private void showOpts(String title) {
        String[] l = optLabels.toArray(new String[0]);
        int[] ic = new int[optIcons.size()], co = new int[optColors.size()];
        for (int i = 0; i < ic.length; i++) { ic[i] = optIcons.get(i); co[i] = optColors.get(i); }
        final List<Runnable> acts = new ArrayList<>(optActions);
        optLabels.clear(); optIcons.clear(); optColors.clear(); optActions.clear();
        Ui.options(this, title, l, ic, co, k -> acts.get(k).run());
    }

    private void update(DocumentReference ref, Map<String, Object> m, String ok, Runnable after) {
        ref.update(m).addOnSuccessListener(v -> {
            Ui.toast(this, ok);
            if (after != null) after.run();
            load();
        }).addOnFailureListener(e -> Ui.toast(this, Data.err(e, "تعذر تنفيذ العملية")));
    }

    private TextView statusChip(String label, int fg, int bg) {
        return Ui.chip(this, label, fg, bg, 0);
    }

    private LinearLayout card(View.OnClickListener l) {
        LinearLayout c = Ui.card(this);
        Ui.tap(c, Ui.shape(this, Ui.WHITE, 10, Ui.LINE, 1), 10, l);
        return c;
    }

    private void notifySheet(String to, String name) {
        LinearLayout c = Ui.col(this);
        Ui.Field t = Ui.field(this, "عنوان الإشعار", R.drawable.ic_notifications, InputType.TYPE_CLASS_TEXT);
        c.addView(t.box, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 0, 0, 10));
        Ui.Field b = Ui.area(this, "نص الإشعار", 3);
        c.addView(b.box, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        TextView send = Ui.button(this, "إرسال", Ui.PRIMARY, R.drawable.ic_send);
        c.addView(send, Ui.m(this, Ui.lp(this, Ui.MATCH, 52), 0, 16, 0, 0));
        Dialog dl = Ui.sheet(this, "إشعار إلى " + name, c);
        send.setOnClickListener(v -> {
            if (t.value().length() < 2) { t.error("اكتب عنوانا"); return; }
            if (b.value().length() < 2) { b.error("اكتب نص الإشعار"); return; }
            db.collection("notifications").add(Data.map("userId", to, "senderId", uid, "type", "announcement",
                    "title", t.value(), "body", b.value(), "refId", "", "isRead", false, "createdAt", FieldValue.serverTimestamp()))
                    .addOnSuccessListener(x -> { dl.dismiss(); Ui.toast(this, "تم إرسال الإشعار"); })
                    .addOnFailureListener(e -> Ui.toast(this, Data.err(e, "تعذر الإرسال")));
        });
    }

    private View userRow(DocumentSnapshot d) {
        String name = Data.str(d, "fullName").isEmpty() ? "مستخدم بدون اسم" : Data.str(d, "fullName");
        LinearLayout c = card(v -> userActions(d, name));
        LinearLayout r = Ui.row(this);
        r.addView(Ui.avatar(this, name, d.getString("profileImageBase64"), 48));
        LinearLayout t = Ui.col(this);
        LinearLayout top = Ui.row(this);
        top.addView(Ui.lines(Ui.text(this, name, 15, Ui.INK, true), 1), Ui.lp(this, 0, Ui.WRAP, 1));
        if (Data.isAdmin(d)) top.addView(statusChip("مدير", Ui.WHITE, Ui.GREEN_DARK), Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 4, 0, 0, 0));
        if (Boolean.TRUE.equals(d.getBoolean("isVerified"))) top.addView(Ui.iv(this, R.drawable.ic_verified, Ui.GREEN, 18), Ui.m(this, Ui.lp(this, 18, 18), 4, 0, 0, 0));
        if (Boolean.FALSE.equals(d.getBoolean("isActive"))) top.addView(statusChip("موقوف", Ui.WARN, Ui.WARN_SOFT), Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 4, 0, 0, 0));
        t.addView(top);
        t.addView(Ui.lines(Ui.text(this, Data.str(d, "email"), 12, Ui.MUTED, false), 1));
        t.addView(Ui.lines(Ui.text(this, "حي " + Data.hoodName(d.getString("neighborhoodId")) + " · " + Data.str(d, "phone")
                + " · انضم " + Data.ago(Data.millis(d, "createdAt")), 12, Ui.MUTED, false), 1));
        r.addView(t, Ui.m(this, Ui.lp(this, 0, Ui.WRAP, 1), 12, 0, 0, 0));
        c.addView(r);
        return c;
    }

    private void userActions(DocumentSnapshot d, String name) {
        boolean self = d.getId().equals(uid);
        boolean verified = Boolean.TRUE.equals(d.getBoolean("isVerified"));
        boolean suspended = Boolean.FALSE.equals(d.getBoolean("isActive"));
        boolean admin = Data.isAdmin(d);
        DocumentReference ref = d.getReference();
        opt("عرض الملف العام", R.drawable.ic_person, Ui.GREEN, () -> Data.openProfile(this, d.getId(), name));
        if (!self) opt("مراسلة", R.drawable.ic_chat, Ui.GREEN, () -> Data.openChat(this, db, uid, d.getId(), name));
        opt(verified ? "إلغاء التوثيق" : "توثيق الحساب", R.drawable.ic_verified, Ui.GREEN, () ->
                update(ref, Data.map("isVerified", !verified, "updatedAt", FieldValue.serverTimestamp()),
                        verified ? "تم إلغاء التوثيق" : "تم توثيق الحساب", () -> {
                            Data.forgetUser(d.getId());
                            if (!verified) Data.notify(db, uid, d.getId(), "announcement", "تم توثيق حسابك", "أصبح حسابك موثقا في نفع، شكرا لمساهمتك في مجتمع الحي.", "");
                        }));
        opt("إرسال إشعار خاص", R.drawable.ic_notifications_active, Ui.GREEN, () -> notifySheet(d.getId(), name));
        if (!self) {
            opt(admin ? "إزالة صلاحية المدير" : "تعيين مديرا", R.drawable.ic_settings, Ui.WARN, () ->
                    Ui.confirm(this, admin ? "إزالة صلاحية المدير" : "تعيين مدير",
                            admin ? "لن يتمكن " + name + " من دخول لوحة الإدارة." : "سيحصل " + name + " على صلاحيات كاملة للتحكم في التطبيق.",
                            "تأكيد", true, () -> update(ref, Data.map("role", admin ? "user" : "admin", "isAdmin", !admin, "updatedAt", FieldValue.serverTimestamp()),
                                    "تم تحديث الصلاحيات", () -> Data.forgetUser(d.getId()))));
            opt(suspended ? "تفعيل الحساب" : "إيقاف الحساب", suspended ? R.drawable.ic_play_circle : R.drawable.ic_pause_circle, Ui.WARN, () ->
                    Ui.confirm(this, suspended ? "تفعيل الحساب" : "إيقاف الحساب",
                            suspended ? "سيعود " + name + " لاستخدام التطبيق وتعاد أدواته التي أوقفتها الإدارة."
                                    : "سيمنع " + name + " من استخدام التطبيق وتوقف أدواته المتاحة مؤقتا.",
                            suspended ? "تفعيل" : "إيقاف", !suspended, () -> setSuspended(d.getId(), !suspended)));
        }
        showOpts(name);
    }

    private void setSuspended(String userId, boolean suspend) {
        db.collection("tools").whereEqualTo("ownerId", userId).get().addOnSuccessListener(q -> {
            WriteBatch b = db.batch();
            b.update(db.collection("users").document(userId), Data.map("isActive", !suspend,
                    "suspendedAt", suspend ? FieldValue.serverTimestamp() : FieldValue.delete(), "updatedAt", FieldValue.serverTimestamp()));
            int n = 0;
            for (DocumentSnapshot t : q.getDocuments()) {
                if (n > 400) break;
                String st = t.getString("status");
                if (suspend && (st == null || "available".equals(st))) {
                    b.update(t.getReference(), Data.map("status", "paused", "isAvailable", false, "pausedByAdmin", true));
                    n++;
                } else if (!suspend && Boolean.TRUE.equals(t.getBoolean("pausedByAdmin"))) {
                    b.update(t.getReference(), Data.map("status", "available", "isAvailable", true, "pausedByAdmin", FieldValue.delete()));
                    n++;
                }
            }
            b.commit().addOnSuccessListener(v -> {
                Data.forgetUser(userId);
                Ui.toast(this, suspend ? "تم إيقاف الحساب" : "تم تفعيل الحساب");
                if (!suspend) Data.notify(db, uid, userId, "announcement", "تم تفعيل حسابك", "يمكنك الآن استخدام نفع من جديد. نرجو الالتزام بالشروط.", "");
                load();
            }).addOnFailureListener(e -> Ui.toast(this, Data.err(e, "تعذر تحديث الحساب")));
        }).addOnFailureListener(e -> Ui.toast(this, Data.err(e, "تعذر تحديث الحساب")));
    }

    private View toolRow(DocumentSnapshot d) {
        LinearLayout wrap = Ui.col(this);
        LinearLayout card = Cards.row(this, db, d, true);
        card.setOnClickListener(v -> toolActions(d));
        wrap.addView(card, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        TextView meta = Ui.text(this, "المالك: " + (Data.str(d, "ownerName").isEmpty() ? "—" : Data.str(d, "ownerName"))
                + " · أضيفت " + Data.ago(Data.millis(d, "createdAt"))
                + (Boolean.TRUE.equals(d.getBoolean("pausedByAdmin")) ? " · أوقفتها الإدارة" : ""), 12, Ui.MUTED, false);
        wrap.addView(meta, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 8, 4, 8, 0));
        return wrap;
    }

    private void toolActions(DocumentSnapshot d) {
        String st = d.getString("status");
        String name = Data.str(d, "toolName");
        String owner = d.getString("ownerId");
        opt("عرض الأداة", R.drawable.ic_handyman, Ui.GREEN, () -> Cards.openTool(this, d.getId()));
        opt("ملف المالك", R.drawable.ic_person, Ui.GREEN, () -> Data.openProfile(this, owner, Data.str(d, "ownerName")));
        if (!"borrowed".equals(st)) {
            boolean paused = "paused".equals(st);
            opt(paused ? "تفعيل الأداة" : "إيقاف الأداة", paused ? R.drawable.ic_play_circle : R.drawable.ic_pause_circle, Ui.WARN, () ->
                    update(d.getReference(), Data.map("status", paused ? "available" : "paused", "isAvailable", paused,
                            "pausedByAdmin", paused ? FieldValue.delete() : true, "updatedAt", FieldValue.serverTimestamp()),
                            paused ? "تم تفعيل الأداة" : "تم إيقاف الأداة", () -> Data.notify(db, uid, owner, "announcement",
                                    paused ? "أعيد تفعيل أداتك" : "أوقفت الإدارة إحدى أدواتك",
                                    paused ? "«" + name + "» ظاهرة الآن للجيران من جديد." : "«" + name + "» لم تعد ظاهرة مؤقتا. تواصل مع الدعم للاستفسار.", d.getId())));
            opt("حذف الأداة نهائيا", R.drawable.ic_delete_forever, Ui.WARN, () -> MyToolsActivity.delete(this, db, d, () -> {
                Data.notify(db, uid, owner, "announcement", "حذفت الإدارة إحدى أدواتك", "تم حذف «" + name + "» لمخالفتها الشروط. تواصل مع الدعم للاستفسار.", "");
                load();
            }));
        }
        showOpts(name);
    }

    private View requestRow(DocumentSnapshot d) {
        String st = d.getString("status");
        LinearLayout c = card(v -> requestActions(d));
        LinearLayout top = Ui.row(this);
        top.addView(Ui.lines(Ui.text(this, Data.str(d, "toolName"), 15, Ui.INK, true), 1), Ui.lp(this, 0, Ui.WRAP, 1));
        int[] sc = Data.requestColors(st);
        top.addView(Ui.chip(this, Data.requestLabel(st), sc[0], sc[1], Data.requestIcon(st)));
        c.addView(top);
        c.addView(Ui.lines(Ui.text(this, "المالك: " + Data.str(d, "ownerName") + "  ←  المستعير: " + Data.str(d, "borrowerName"), 13, Ui.INK, false), 2),
                Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 6, 0, 0));
        c.addView(Ui.text(this, "المدة: " + Data.duration(Math.max(1, Data.num(d.getLong("durationValue"))), d.getString("durationUnit"))
                + " · طلب " + Data.ago(Data.millis(d, "requestedAt")), 12, Ui.MUTED, false), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 4, 0, 0));
        long due = Data.millis(d, "dueAt");
        if ("accepted".equals(st) && due > 0) {
            long left = Data.daysLeft(due);
            TextView dt = left < 0 ? Ui.chip(this, "متأخرة " + Data.count(-left, "يوما", "يومين", "أيام", "يوما"), Ui.WARN, Ui.WARN_SOFT, R.drawable.ic_warning)
                    : Ui.chip(this, "الإرجاع " + Data.day(due), Ui.GREEN_DARK, Ui.MINT, R.drawable.ic_schedule);
            c.addView(dt, Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 0, 8, 0, 0));
        }
        return c;
    }

    private void requestActions(DocumentSnapshot d) {
        String st = d.getString("status");
        opt("عرض الأداة", R.drawable.ic_handyman, Ui.GREEN, () -> Cards.openTool(this, d.getString("toolId")));
        opt("ملف المالك", R.drawable.ic_person, Ui.GREEN, () -> Data.openProfile(this, d.getString("ownerId"), Data.str(d, "ownerName")));
        opt("ملف المستعير", R.drawable.ic_person, Ui.GREEN, () -> Data.openProfile(this, d.getString("borrowerId"), Data.str(d, "borrowerName")));
        if ("accepted".equals(st)) {
            opt("تذكير المستعير بالإرجاع", R.drawable.ic_notifications_active, Ui.GREEN, () -> Actions.remind(this, db, uid, d, this::load));
            opt("تأكيد الإرجاع (إجراء إداري)", R.drawable.ic_assignment_return, Ui.GREEN, () -> Actions.returned(this, db, uid, d, this::load));
        }
        if ("pending".equals(st) || "accepted".equals(st))
            opt("إلغاء الطلب (إجراء إداري)", R.drawable.ic_cancel, Ui.WARN, () -> Ui.confirm(this, "إلغاء الطلب",
                    "سيلغى الطلب" + ("accepted".equals(st) ? " وتعود الأداة متاحة" : "") + " ويبلغ الطرفان.", "إلغاء الطلب", true, () -> adminCancel(d)));
        showOpts(Data.str(d, "toolName"));
    }

    private void adminCancel(DocumentSnapshot d) {
        WriteBatch b = db.batch();
        b.update(d.getReference(), Data.map("status", "cancelled", "cancelledBy", "admin", "updatedAt", FieldValue.serverTimestamp()));
        String toolId = d.getString("toolId");
        if ("accepted".equals(d.getString("status")) && toolId != null)
            b.update(db.collection("tools").document(toolId), Data.map("status", "available", "isAvailable", true,
                    "currentBorrowerId", FieldValue.delete(), "updatedAt", FieldValue.serverTimestamp()));
        b.commit().addOnSuccessListener(v -> {
            String body = "ألغت إدارة نفع طلب استعارة «" + Data.str(d, "toolName") + "». تواصل مع الدعم للاستفسار.";
            Data.notify(db, uid, d.getString("ownerId"), "cancelled", "إلغاء طلب من الإدارة", body, d.getId());
            Data.notify(db, uid, d.getString("borrowerId"), "cancelled", "إلغاء طلب من الإدارة", body, d.getId());
            Ui.toast(this, "تم إلغاء الطلب");
            load();
        }).addOnFailureListener(e -> Ui.toast(this, Data.err(e, "تعذر إلغاء الطلب")));
    }

    private static String reportLabel(String st) {
        if ("reviewing".equals(st)) return "قيد المراجعة";
        if ("resolved".equals(st)) return "تم الحل";
        if ("dismissed".equals(st)) return "مرفوض";
        return "مفتوح";
    }

    private View reportRow(DocumentSnapshot d) {
        String st = Data.str(d, "status");
        boolean tool = "tool".equals(d.getString("targetType"));
        LinearLayout c = card(v -> reportActions(d));
        LinearLayout top = Ui.row(this);
        top.addView(Ui.badgeIcon(this, R.drawable.ic_flag, Ui.WARN, Ui.WARN_SOFT, 38, 20));
        LinearLayout t = Ui.col(this);
        t.addView(Ui.lines(Ui.text(this, Data.str(d, "reason"), 15, Ui.INK, true), 1));
        t.addView(Ui.lines(Ui.text(this, (tool ? "أداة: " : "مستخدم: ") + Data.str(d, "targetName"), 13, Ui.GREEN_DARK, false), 1));
        top.addView(t, Ui.m(this, Ui.lp(this, 0, Ui.WRAP, 1), 10, 0, 8, 0));
        boolean open = st.isEmpty() || "open".equals(st);
        top.addView(statusChip(reportLabel(st), open ? Ui.WARN : "resolved".equals(st) ? Ui.GREEN_DARK : 0xFF5F6663,
                open ? Ui.WARN_SOFT : "resolved".equals(st) ? Ui.MINT : Ui.FIELD));
        c.addView(top);
        String det = Data.str(d, "details");
        if (!det.isEmpty()) {
            TextView dt = Ui.lines(Ui.text(this, det, 14, 0xFF4A4F4D, false), 4);
            dt.setBackground(Ui.shape(this, Ui.CANVAS, 12));
            Ui.pad(dt, 12, 8, 12, 8);
            c.addView(dt, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 10, 0, 0));
        }
        TextView who = Ui.text(this, "المبلغ: … · " + Data.ago(Data.millis(d, "createdAt")), 12, Ui.MUTED, false);
        c.addView(who, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 8, 0, 0));
        Data.user(db, d.getString("reporterId"), u -> who.setText("المبلغ: " + (u == null ? "—" : Data.str(u, "fullName")) + " · " + Data.ago(Data.millis(d, "createdAt"))));
        return c;
    }

    private void reportActions(DocumentSnapshot d) {
        boolean tool = "tool".equals(d.getString("targetType"));
        String target = d.getString("targetId"), name = Data.str(d, "targetName");
        opt(tool ? "عرض الأداة" : "عرض ملف المستخدم", tool ? R.drawable.ic_handyman : R.drawable.ic_person, Ui.GREEN, () -> {
            if (tool) Cards.openTool(this, target); else Data.openProfile(this, target, name);
        });
        opt("ملف المبلغ", R.drawable.ic_person, Ui.GREEN, () -> Data.openProfile(this, d.getString("reporterId"), ""));
        opt("تحديد: قيد المراجعة", R.drawable.ic_hourglass_top, Ui.GREEN, () -> reportStatus(d, "reviewing"));
        opt("تحديد: تم الحل", R.drawable.ic_check_circle, Ui.GREEN, () -> reportStatus(d, "resolved"));
        opt("رفض البلاغ", R.drawable.ic_cancel, Ui.WARN, () -> reportStatus(d, "dismissed"));
        if (tool) opt("إيقاف الأداة المبلغ عنها", R.drawable.ic_pause_circle, Ui.WARN, () ->
                db.collection("tools").document(target).update(Data.map("status", "paused", "isAvailable", false, "pausedByAdmin", true))
                        .addOnSuccessListener(v -> { Ui.toast(this, "تم إيقاف الأداة"); reportStatus(d, "resolved"); })
                        .addOnFailureListener(e -> Ui.toast(this, Data.err(e, "تعذر إيقاف الأداة (ربما حذفت أو معارة)"))));
        else opt("إيقاف الحساب المبلغ عنه", R.drawable.ic_pause_circle, Ui.WARN, () ->
                Ui.confirm(this, "إيقاف الحساب", "سيمنع " + name + " من استخدام التطبيق.", "إيقاف", true, () -> {
                    setSuspended(target, true);
                    reportStatus(d, "resolved");
                }));
        showOpts(Data.str(d, "reason"));
    }

    private void reportStatus(DocumentSnapshot d, String st) {
        update(d.getReference(), Data.map("status", st, "handledBy", uid, "handledAt", FieldValue.serverTimestamp()),
                "تم تحديث حالة البلاغ", () -> {
                    if ("resolved".equals(st) || "dismissed".equals(st))
                        Data.notify(db, uid, d.getString("reporterId"), "announcement", "تمت مراجعة بلاغك",
                                "resolved".equals(st) ? "شكرا لك، اتخذنا الإجراء المناسب بخصوص بلاغك." : "راجعنا بلاغك ولم نجد مخالفة للشروط. شكرا لحرصك.", d.getId());
                });
    }

    private static final String[] SUP_IDS = {"open", "in_progress", "resolved"};
    private static final String[] SUP_NAMES = {"مفتوح", "قيد المعالجة", "تم الحل"};

    private View supportRow(DocumentSnapshot d) {
        String st = Data.str(d, "status");
        LinearLayout c = card(v -> replySheet(d));
        LinearLayout top = Ui.row(this);
        top.addView(Ui.lines(Ui.text(this, Data.str(d, "subject"), 15, Ui.INK, true), 1), Ui.lp(this, 0, Ui.WRAP, 1));
        int si = Math.max(0, java.util.Arrays.asList(SUP_IDS).indexOf("closed".equals(st) ? "resolved" : st));
        top.addView(statusChip(SUP_NAMES[si], si == 2 ? Ui.GREEN_DARK : si == 1 ? 0xFF9A6A00 : Ui.WARN, si == 2 ? Ui.MINT : si == 1 ? Ui.AMBER_SOFT : Ui.WARN_SOFT));
        c.addView(top);
        c.addView(Ui.lines(Ui.text(this, Data.str(d, "userName") + " · " + Data.str(d, "userEmail"), 12, Ui.GREEN_DARK, false), 1),
                Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 2, 0, 6));
        c.addView(Ui.lines(Ui.text(this, Data.str(d, "message"), 14, 0xFF4A4F4D, false), 4));
        c.addView(Ui.text(this, Data.ago(Data.millis(d, "createdAt")) + " · " + Data.str(d, "platform") + " · v" + Data.str(d, "appVersion"), 11, Ui.MUTED, false),
                Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 6, 0, 0));
        if (!Data.str(d, "adminReply").isEmpty()) {
            TextView rp = Ui.lines(Ui.text(this, "الرد: " + Data.str(d, "adminReply"), 13, Ui.GREEN_DARK, false), 2);
            rp.setBackground(Ui.shape(this, Ui.MINT, 12));
            Ui.pad(rp, 10, 8, 10, 8);
            c.addView(rp, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 8, 0, 0));
        }
        return c;
    }

    private void replySheet(DocumentSnapshot d) {
        LinearLayout c = Ui.col(this);
        TextView msg = Ui.text(this, Data.str(d, "message"), 14, 0xFF4A4F4D, false);
        msg.setBackground(Ui.shape(this, Ui.CANVAS, 12));
        Ui.pad(msg, 12, 10, 12, 10);
        msg.setTextIsSelectable(true);
        c.addView(msg, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 0, 0, 12));
        c.addView(Ui.medium(this, "الحالة", 14, Ui.INK));
        LinearLayout sr = Ui.row(this);
        String cur = "closed".equals(Data.str(d, "status")) ? "resolved" : Data.str(d, "status");
        int[] pick = {Math.max(0, java.util.Arrays.asList(SUP_IDS).indexOf(cur))};
        List<TextView> sc = new ArrayList<>();
        for (int i = 0; i < SUP_IDS.length; i++) {
            final int k = i;
            TextView t = Ui.choice(this, SUP_NAMES[i], 0);
            t.setOnClickListener(v -> { pick[0] = k; for (int j = 0; j < sc.size(); j++) Ui.setChoice(sc.get(j), j == k); });
            sc.add(t);
            Ui.setChoice(t, i == pick[0]);
            sr.addView(t, Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 0, 0, 8, 0));
        }
        c.addView(Ui.hscroll(this, sr), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 8, 0, 12));
        Ui.Field reply = Ui.area(this, "اكتب ردك للمستخدم...", 4);
        reply.set(Data.str(d, "adminReply"));
        c.addView(reply.box, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        TextView save = Ui.button(this, "حفظ وإرسال الرد", Ui.PRIMARY, R.drawable.ic_send);
        c.addView(save, Ui.m(this, Ui.lp(this, Ui.MATCH, 52), 0, 16, 0, 0));
        LinearLayout extra = Ui.row(this);
        TextView prof = Ui.link(this, "ملف المستخدم", v -> Data.openProfile(this, d.getString("userId"), Data.str(d, "userName")));
        extra.addView(prof);
        extra.setGravity(Gravity.CENTER);
        c.addView(extra, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 6, 0, 0));
        Dialog dl = Ui.sheet(this, Data.str(d, "subject"), c);
        save.setOnClickListener(v -> {
            String rep = reply.value();
            String st = SUP_IDS[pick[0]];
            boolean newReply = !rep.isEmpty() && !rep.equals(Data.str(d, "adminReply"));
            Ui.busy(save, true, "جار الحفظ...");
            d.getReference().update(Data.map("adminReply", rep, "status", st, "repliedBy", uid,
                    "repliedAt", FieldValue.serverTimestamp(), "updatedAt", FieldValue.serverTimestamp()))
                    .addOnSuccessListener(x -> {
                        if (newReply || "resolved".equals(st))
                            Data.notify(db, uid, d.getString("userId"), "support", "رد فريق الدعم على طلبك",
                                    "«" + Data.str(d, "subject") + "»: " + (rep.isEmpty() ? "تم حل طلبك." : rep), d.getId());
                        dl.dismiss();
                        Ui.toast(this, "تم حفظ الرد");
                        load();
                    }).addOnFailureListener(e -> { Ui.busy(save, false, null); Ui.toast(this, Data.err(e, "تعذر الحفظ")); });
        });
    }

    private View reviewRow(DocumentSnapshot d) {
        LinearLayout wrap = Ui.col(this);
        String reviewed = d.getString("reviewedUserId");
        TextView about = Ui.medium(this, "تقييم لـ: " + (Data.str(d, "reviewedName").isEmpty() ? "…" : Data.str(d, "reviewedName")), 13, Ui.GREEN_DARK);
        wrap.addView(about, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 6, 0, 6, 6));
        if (Data.str(d, "reviewedName").isEmpty())
            Data.user(db, reviewed, u -> about.setText("تقييم لـ: " + (u == null ? "—" : Data.str(u, "fullName"))));
        wrap.addView(ReviewsActivity.reviewCard(this, db, d), Ui.lp(this, Ui.MATCH, Ui.WRAP));
        LinearLayout acts = Ui.row(this);
        TextView p = Ui.link(this, "ملف المقيم", v -> Data.openProfile(this, reviewed, ""));
        acts.addView(p);
        acts.addView(Ui.grow(this));
        TextView del = Ui.link(this, "حذف التقييم", v -> Ui.confirm(this, "حذف التقييم", "سيحذف هذا التقييم نهائيا ويعاد حساب متوسط التقييم.", "حذف", true, () ->
                d.getReference().delete().addOnSuccessListener(x -> {
                    Data.syncRating(db, reviewed);
                    Ui.toast(this, "تم حذف التقييم");
                    load();
                }).addOnFailureListener(e -> Ui.toast(this, Data.err(e, "تعذر الحذف")))));
        del.setTextColor(Ui.WARN);
        acts.addView(del);
        wrap.addView(acts, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        return wrap;
    }

    private View hoodRow(DocumentSnapshot d) {
        boolean on = !Boolean.FALSE.equals(d.getBoolean("isActive"));
        LinearLayout c = card(v -> hoodSheet(d));
        LinearLayout r = Ui.row(this);
        TextView num = Ui.center(Ui.text(this, String.valueOf(Data.num(d.getLong("sortOrder"))), 13, Ui.WHITE, true));
        num.setBackground(Ui.oval(on ? Ui.GREEN : Ui.MUTED));
        r.addView(num, Ui.lp(this, 30, 30));
        LinearLayout t = Ui.col(this);
        t.addView(Ui.text(this, Data.str(d, "name"), 16, Ui.INK, true));
        t.addView(Ui.text(this, "المعرف: " + d.getId(), 11, Ui.MUTED, false));
        r.addView(t, Ui.m(this, Ui.lp(this, 0, Ui.WRAP, 1), 12, 0, 8, 0));
        r.addView(statusChip(on ? "مفعل" : "معطل", on ? Ui.GREEN_DARK : 0xFF5F6663, on ? Ui.MINT : Ui.FIELD));
        c.addView(r);
        return c;
    }

    private void hoodSheet(DocumentSnapshot d) {
        LinearLayout c = Ui.col(this);
        Ui.Field name = Ui.field(this, "اسم الحي", R.drawable.ic_location_on, InputType.TYPE_CLASS_TEXT);
        Ui.Field order = Ui.field(this, "الترتيب (رقم)", R.drawable.ic_sort, InputType.TYPE_CLASS_NUMBER);
        c.addView(name.box, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 0, 0, 10));
        c.addView(order.box, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 0, 0, 10));
        LinearLayout ar = Ui.row(this);
        ar.setBackground(Ui.shape(this, Ui.WHITE, 10, Ui.LINE, 1));
        Ui.pad(ar, 16, 10, 12, 10);
        ar.addView(Ui.medium(this, "ظاهر في قائمة الأحياء", 15, Ui.INK), Ui.lp(this, 0, Ui.WRAP, 1));
        Switch act = Ui.toggle(this);
        act.setChecked(true);
        ar.addView(act);
        c.addView(ar, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        if (d != null) {
            name.set(Data.str(d, "name"));
            order.set(String.valueOf(Data.num(d.getLong("sortOrder"))));
            act.setChecked(!Boolean.FALSE.equals(d.getBoolean("isActive")));
        } else order.set(String.valueOf(docs.size() + 1));
        TextView save = Ui.button(this, d == null ? "إضافة الحي" : "حفظ", Ui.PRIMARY, R.drawable.ic_check);
        c.addView(save, Ui.m(this, Ui.lp(this, Ui.MATCH, 52), 0, 16, 0, 0));
        if (d != null) {
            TextView del = Ui.button(this, "حذف الحي", Ui.DANGER, R.drawable.ic_delete);
            c.addView(del, Ui.m(this, Ui.lp(this, Ui.MATCH, 50), 0, 8, 0, 0));
            del.setOnClickListener(v -> Ui.confirm(this, "حذف الحي", "المستخدمون المسجلون في هذا الحي سيبقى معرفه في ملفاتهم. الأفضل تعطيله بدل حذفه.", "حذف", true, () ->
                    d.getReference().delete().addOnSuccessListener(x -> { Ui.toast(this, "تم حذف الحي"); Data.loadHoods(db, () -> {}); load(); })
                            .addOnFailureListener(e -> Ui.toast(this, Data.err(e, "تعذر الحذف")))));
        }
        Dialog dl = Ui.sheet(this, d == null ? "حي جديد" : "تعديل الحي", c);
        save.setOnClickListener(v -> {
            if (name.value().length() < 2) { name.error("اكتب اسم الحي"); return; }
            long ord;
            try { ord = Long.parseLong(order.value().replaceAll("[^0-9]", "")); } catch (Exception e) { ord = docs.size() + 1; }
            Map<String, Object> m = Data.map("name", name.value(), "sortOrder", ord, "isActive", act.isChecked(), "updatedAt", FieldValue.serverTimestamp());
            DocumentReference ref = d == null ? db.collection("neighborhoods").document() : d.getReference();
            ref.set(m, SetOptions.merge()).addOnSuccessListener(x -> {
                dl.dismiss();
                Ui.toast(this, "تم الحفظ");
                Data.loadHoods(db, () -> {});
                load();
            }).addOnFailureListener(e -> Ui.toast(this, Data.err(e, "تعذر الحفظ")));
        });
    }

    private void importHoods() {
        WriteBatch b = db.batch();
        String[][] h = Data.defaultHoods();
        for (int i = 0; i < h.length; i++)
            b.set(db.collection("neighborhoods").document(h[i][0]), Data.map("name", h[i][1], "sortOrder", (long) (i + 1), "isActive", true));
        b.commit().addOnSuccessListener(v -> { Ui.toast(this, "تم استيراد " + h.length + " حيا"); Data.loadHoods(db, () -> {}); load(); })
                .addOnFailureListener(e -> Ui.toast(this, Data.err(e, "تعذر الاستيراد")));
    }

    private void broadcastUi() {
        LinearLayout b = s.body;
        LinearLayout info = Ui.row(this);
        info.setGravity(Gravity.TOP);
        info.setBackground(Ui.shape(this, Ui.MINT, 10));
        Ui.pad(info, 14, 12, 14, 12);
        info.addView(Ui.iv(this, R.drawable.ic_info, Ui.GREEN_DARK, 20));
        info.addView(Ui.text(this, "يصل الإشعار إلى صفحة الإشعارات لدى كل مستخدم نشط مستهدف، ويظهر عداد غير المقروء فورا.", 13, Ui.GREEN_DARK, false),
                Ui.m(this, Ui.lp(this, 0, Ui.WRAP, 1), 10, 0, 0, 0));
        b.addView(info, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 4, 0, 6));

        LinearLayout form = Ui.card(this);
        List<String> hoodIds = Data.hoodIds();
        String[] targets = new String[hoodIds.size() + 1];
        targets[0] = "جميع المستخدمين";
        for (int i = 0; i < hoodIds.size(); i++) targets[i + 1] = "سكان حي " + Data.hoodName(hoodIds.get(i));
        Ui.Select target = Ui.select(this, "الجمهور المستهدف", R.drawable.ic_group, targets);
        target.set(0);
        form.addView(target.box, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 0, 0, 10));
        Ui.Field t = Ui.field(this, "عنوان الإشعار", R.drawable.ic_notifications, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        form.addView(t.box, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 0, 0, 10));
        Ui.Field body = Ui.area(this, "نص الإشعار...", 4);
        form.addView(body.box, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        TextView send = Ui.button(this, "إرسال الإشعار", Ui.PRIMARY, R.drawable.ic_send);
        form.addView(send, Ui.m(this, Ui.lp(this, Ui.MATCH, 52), 0, 16, 0, 0));
        b.addView(form, Ui.stack(this));

        b.addView(Ui.section(this, "الإشعارات المرسلة سابقا", null, null));
        LinearLayout hist = Ui.col(this);
        b.addView(hist, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        Runnable loadHist = () -> db.collection("broadcasts").orderBy("createdAt", Query.Direction.DESCENDING).limit(20).get().addOnSuccessListener(q -> {
            hist.removeAllViews();
            if (q.isEmpty()) { hist.addView(Ui.center(Ui.text(this, "لم يرسل أي إشعار عام بعد", 14, Ui.MUTED, false)), Ui.lp(this, Ui.MATCH, Ui.WRAP)); return; }
            for (DocumentSnapshot d : q.getDocuments()) {
                LinearLayout c = Ui.card(this);
                c.addView(Ui.text(this, Data.str(d, "title"), 15, Ui.INK, true));
                c.addView(Ui.lines(Ui.text(this, Data.str(d, "body"), 13, 0xFF4A4F4D, false), 3), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 4, 0, 6));
                c.addView(Ui.text(this, Data.str(d, "target") + " · " + Data.num(d.getLong("count")) + " مستلم · " + Data.ago(Data.millis(d, "createdAt")), 12, Ui.MUTED, false));
                hist.addView(c, Ui.stack(this));
            }
        });
        loadHist.run();

        send.setOnClickListener(v -> {
            if (t.value().length() < 3) { t.error("اكتب عنوانا واضحا"); return; }
            if (body.value().length() < 5) { body.error("اكتب نص الإشعار"); return; }
            int ti = Math.max(0, target.index);
            String hood = ti == 0 ? null : hoodIds.get(ti - 1);
            Ui.confirm(this, "تأكيد الإرسال", "سيرسل الإشعار إلى " + targets[ti] + ". لا يمكن التراجع بعد الإرسال.", "إرسال", false, () -> {
                Ui.busy(send, true, "جار الإرسال...");
                db.collection("users").limit(10000).get().addOnSuccessListener(q -> {
                    List<Task<Void>> commits = new ArrayList<>();
                    WriteBatch wb = db.batch();
                    int n = 0;
                    for (DocumentSnapshot u : q.getDocuments()) {
                        if (Boolean.FALSE.equals(u.getBoolean("isActive"))) continue;
                        if (hood != null && !hood.equals(u.getString("neighborhoodId"))) continue;
                        wb.set(db.collection("notifications").document(), Data.map("userId", u.getId(), "senderId", uid, "type", "announcement",
                                "title", t.value(), "body", body.value(), "refId", "", "isRead", false, "createdAt", FieldValue.serverTimestamp()));
                        if (++n % 400 == 0) { commits.add(wb.commit()); wb = db.batch(); }
                    }
                    final int total = n;
                    wb.set(db.collection("broadcasts").document(), Data.map("title", t.value(), "body", body.value(), "target", targets[ti],
                            "count", (long) total, "sentBy", uid, "createdAt", FieldValue.serverTimestamp()));
                    commits.add(wb.commit());
                    Tasks.whenAll(commits).addOnSuccessListener(x -> {
                        Ui.busy(send, false, null);
                        t.set("");
                        body.set("");
                        Ui.toast(this, "تم إرسال الإشعار إلى " + total + " مستخدم");
                        loadHist.run();
                    }).addOnFailureListener(e -> { Ui.busy(send, false, null); Ui.toast(this, Data.err(e, "تعذر إكمال الإرسال")); });
                }).addOnFailureListener(e -> { Ui.busy(send, false, null); Ui.toast(this, Data.err(e, "تعذر جلب المستخدمين")); });
            });
        });
    }

    private void configUi() {
        LinearLayout b = s.body;
        b.addView(Ui.loading(this));
        DocumentReference ref = db.collection("app_config").document("main");
        ref.get().addOnSuccessListener(d -> {
            b.removeAllViews();

            b.addView(Ui.section(this, "إعلان الصفحة الرئيسية", null, null));
            LinearLayout c1 = Ui.card(this);
            Switch annOn = toggleRow(c1, "إظهار الإعلان", "يظهر كبطاقة مميزة أعلى الصفحة الرئيسية لكل المستخدمين");
            Ui.Field ann = Ui.area(this, "نص الإعلان، مثال: حملة تبادل أدوات الحديقة هذا الأسبوع", 3);
            c1.addView(ann.box, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 12, 0, 0));
            b.addView(c1, Ui.stack(this));

            b.addView(Ui.section(this, "وضع الصيانة", null, null));
            LinearLayout c2 = Ui.card(this);
            Switch mOn = toggleRow(c2, "تفعيل وضع الصيانة", "يمنع المستخدمين من الدخول ويعرض رسالتك (المدراء مستثنون)");
            Ui.Field mMsg = Ui.area(this, "رسالة الصيانة للمستخدمين", 3);
            c2.addView(mMsg.box, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 12, 0, 0));
            b.addView(c2, Ui.stack(this));

            if (d.exists()) {
                annOn.setChecked(Boolean.TRUE.equals(d.getBoolean("announcementOn")));
                ann.set(Data.str(d, "announcement"));
                mOn.setChecked(Boolean.TRUE.equals(d.getBoolean("maintenance")));
                mMsg.set(Data.str(d, "maintenanceMessage"));
                long up = Data.millis(d, "updatedAt");
                if (up > 0) b.addView(Ui.center(Ui.text(this, "آخر تحديث " + Data.ago(up), 12, Ui.MUTED, false)), Ui.lp(this, Ui.MATCH, Ui.WRAP));
            }

            LinearLayout foot = Ui.footer(this, s);
            TextView save = Ui.button(this, "حفظ الإعدادات", Ui.PRIMARY, R.drawable.ic_check);
            foot.addView(save, Ui.lp(this, 0, 54, 1));
            save.setOnClickListener(v -> {
                if (annOn.isChecked() && ann.value().isEmpty()) { ann.error("اكتب نص الإعلان"); return; }
                Runnable doSave = () -> {
                    Ui.busy(save, true, "جار الحفظ...");
                    ref.set(Data.map("announcementOn", annOn.isChecked(), "announcement", ann.value(),
                            "maintenance", mOn.isChecked(), "maintenanceMessage", mMsg.value(),
                            "updatedBy", uid, "updatedAt", FieldValue.serverTimestamp()), SetOptions.merge())
                            .addOnSuccessListener(x -> { Ui.busy(save, false, null); Ui.toast(this, "تم حفظ الإعدادات"); })
                            .addOnFailureListener(e -> { Ui.busy(save, false, null); Ui.toast(this, Data.err(e, "تعذر الحفظ")); });
                };
                if (mOn.isChecked()) Ui.confirm(this, "تفعيل الصيانة", "لن يتمكن المستخدمون من استخدام التطبيق حتى توقف وضع الصيانة. متأكد؟", "تفعيل", true, doSave);
                else doSave.run();
            });
        }).addOnFailureListener(e -> {
            b.removeAllViews();
            b.addView(Ui.empty(this, R.drawable.ic_error_outline, "تعذر تحميل الإعدادات", Data.err(e, "تحقق من الاتصال")));
        });
    }

    private Switch toggleRow(LinearLayout into, String title, String sub) {
        LinearLayout r = Ui.row(this);
        LinearLayout t = Ui.col(this);
        t.addView(Ui.medium(this, title, 15, Ui.INK));
        t.addView(Ui.text(this, sub, 12, Ui.MUTED, false));
        r.addView(t, Ui.lp(this, 0, Ui.WRAP, 1));
        Switch sw = Ui.toggle(this);
        r.addView(sw);
        r.setOnClickListener(v -> sw.toggle());
        into.addView(r, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        return sw;
    }
}
