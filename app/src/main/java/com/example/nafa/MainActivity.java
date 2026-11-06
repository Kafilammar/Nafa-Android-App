package com.example.nafa;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MainActivity extends BaseActivity {
    public static final int HOME = 0, SEARCH = 1, REQUESTS = 2, PROFILE = 3;

    private FrameLayout host;
    private final LinearLayout[] navItems = new LinearLayout[4];
    private TextView reqCounter, notifCounter, chatCounter;
    private int tab = HOME, pendingCount, unreadNotifs, unreadChats;

    DocumentSnapshot me;
    private final List<DocumentSnapshot> tools = new ArrayList<>();
    private boolean toolsLoaded;
    private String searchCat, searchQuery = "";
    private boolean myHoodOnly, focusSearch;
    private int sortMode;
    private LinearLayout nav;
    private TextView hoodInfo;
    private static final String[] SORTS = {"الأحدث", "حيي أولا", "الاسم (أ - ي)"};

    private RequestsTab requestsTab;
    private ProfileTab profileTab;
    private Object backCallback;

    @Override
    protected void onReady(Bundle saved) {
        requestsTab = new RequestsTab(this);
        profileTab = new ProfileTab(this);
        tab = getIntent().getIntExtra("tab", HOME);
        requestsTab.mode(getIntent().getIntExtra("reqIn", -1), getIntent().getIntExtra("reqFilter", -1));
        build();
        listenCounters();
        Data.loadHoods(db, () -> {});
        Data.user(db, uid, d -> { if (d != null) { me = d; Data.ensurePublicProfile(db, uid); checkGate(); } });
        loadConfig();
        Data.loadFavs(db, uid, () -> { if (tab == HOME && toolsLoaded) show(HOME); });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra("tab")) tab = intent.getIntExtra("tab", HOME);
        requestsTab.mode(intent.getIntExtra("reqIn", -1), intent.getIntExtra("reqFilter", -1));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!ready) return;
        toolsLoaded = false;
        show(tab);
    }

    DocumentSnapshot config;
    private boolean gated, configShown;

    private void loadConfig() {
        db.collection("app_config").document("main").get().addOnSuccessListener(d -> {
            config = d.exists() ? d : null;
            checkGate();
            if (!gated && !configShown && tab == HOME && config != null && Boolean.TRUE.equals(config.getBoolean("announcementOn"))) {
                configShown = true;
                show(HOME);
            }
        });
    }

    private void checkGate() {
        if (gated || me == null) return;
        if (Data.isSuspended(me)) {
            gate(R.drawable.ic_error_outline, "تم إيقاف حسابك",
                    "أوقفت إدارة نفع هذا الحساب لمخالفة الشروط. إذا كنت ترى أن هذا خطأ تواصل مع فريق الدعم.", true);
        } else if (config != null && Boolean.TRUE.equals(config.getBoolean("maintenance")) && !Data.isAdmin(me)) {
            String msg = Data.str(config, "maintenanceMessage");
            gate(R.drawable.ic_construction, "نعمل على تحسين نفع",
                    msg.isEmpty() ? "التطبيق تحت الصيانة حاليا وسيعود قريبا. شكرا لصبرك" : msg, false);
        }
    }

    private void gate(int icon, String title, String msg, boolean support) {
        gated = true;
        bars(Ui.WHITE, Ui.WHITE, true);
        LinearLayout c = Ui.col(this);
        c.setGravity(Gravity.CENTER);
        c.setBackgroundColor(Ui.WHITE);
        Ui.pad(c, 28, 0, 28, 0);
        c.addView(Ui.badgeIcon(this, icon, support ? Ui.WARN : Ui.GREEN, support ? Ui.WARN_SOFT : Ui.MINT, 110, 54));
        c.addView(Ui.center(Ui.text(this, title, 23, Ui.INK, true)), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 22, 0, 8));
        TextView m = Ui.center(Ui.text(this, msg, 15, Ui.MUTED, false));
        m.setLineSpacing(0, 1.3f);
        c.addView(m, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        if (support) {
            TextView sup = Ui.button(this, "تواصل مع الدعم", Ui.PRIMARY, R.drawable.ic_support_agent);
            sup.setOnClickListener(v -> go(SupportActivity.class));
            c.addView(sup, Ui.m(this, Ui.lp(this, Ui.MATCH, 52), 0, 28, 0, 0));
        } else {
            TextView retry = Ui.button(this, "إعادة المحاولة", Ui.PRIMARY, R.drawable.ic_refresh);
            retry.setOnClickListener(v -> recreate());
            c.addView(retry, Ui.m(this, Ui.lp(this, Ui.MATCH, 52), 0, 28, 0, 0));
        }
        TextView out = Ui.button(this, "تسجيل الخروج", Ui.GHOST, R.drawable.ic_logout);
        out.setOnClickListener(v -> {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
            Data.clearCache();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
        c.addView(out, Ui.m(this, Ui.lp(this, Ui.MATCH, 50), 0, 8, 0, 0));
        setContentView(c);
    }

    private void build() {
        FrameLayout frame = new FrameLayout(this);
        LinearLayout root = Ui.col(this);
        host = new FrameLayout(this);
        root.addView(host, Ui.lp(this, Ui.MATCH, 0, 1));

        nav = Ui.row(this);
        nav.setBackgroundColor(Ui.WHITE);
        nav.setPadding(Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6));
        String[] labels = {"الرئيسية", "البحث", "طلباتي", "حسابي"};
        int[] icons = {R.drawable.ic_home, R.drawable.ic_search, R.drawable.ic_swap_horiz, R.drawable.ic_person};
        reqCounter = Ui.counter(this);
        for (int i = 0; i < 4; i++) {
            if (i == 2) {
                LinearLayout slot = Ui.col(this);
                slot.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
                TextView addLbl = Ui.center(Ui.text(this, "إضافة", 11, Ui.MUTED, false));
                slot.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 2));
                ImageView addIc = Ui.iv(this, R.drawable.ic_add, Ui.MUTED, 22);
                slot.addView(addIc, Ui.m(this, Ui.lp(this, 22, 22), 0, 5, 0, 5));
                slot.addView(addLbl, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 3, 0, 0));
                Ui.tap(slot, null, 16, v -> go(AddToolActivity.class));
                nav.addView(slot, Ui.lp(this, 0, Ui.WRAP, 1));
            }
            navItems[i] = navItem(icons[i], labels[i], i == 2 ? reqCounter : null);
            final int t = i;
            navItems[i].setOnClickListener(v -> { if (tab != t) show(t); });
            nav.addView(navItems[i], Ui.lp(this, 0, Ui.WRAP, 1));
        }
        root.addView(Ui.divider(this, 0));
        root.addView(nav, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        frame.addView(root, new FrameLayout.LayoutParams(-1, -1));

        setContentView(frame);
    }

    private LinearLayout navItem(int icon, String label, TextView counter) {
        LinearLayout it = Ui.col(this);
        it.setGravity(Gravity.CENTER);
        it.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 2));
        FrameLayout pill = new FrameLayout(this);
        ImageView iv = new ImageView(this);
        pill.addView(iv, new FrameLayout.LayoutParams(Ui.dp(this, 22), Ui.dp(this, 22), Gravity.CENTER));
        if (counter != null) {
            FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, Ui.dp(this, 18), Gravity.TOP | Gravity.END);
            pill.addView(counter, cp);
        }
        it.addView(pill, Ui.lp(this, 60, 32));
        TextView t = Ui.center(Ui.text(this, label, 11, Ui.MUTED, false));
        it.addView(t, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 3, 0, 0));
        it.setTag(new Object[]{pill, iv, t, icon});
        Ui.tap(it, null, 16, null);
        return it;
    }

    private void paintNav() {
        for (int i = 0; i < 4; i++) {
            Object[] p = (Object[]) navItems[i].getTag();
            boolean on = i == tab;
                        ((ImageView) p[1]).setImageDrawable(Ui.icon(this, (Integer) p[3], on ? Ui.GREEN : Ui.MUTED, 22));
            TextView t = (TextView) p[2];
            t.setTextColor(on ? Ui.GREEN_DARK : Ui.MUTED);
        }
    }

    void show(int t) {
        if (gated) return;
        tab = t;
        paintNav();
        host.removeAllViews();
        View v;
        switch (t) {
            case SEARCH: v = searchView(); bars(Ui.GREEN, Ui.WHITE, false); break;
            case REQUESTS: v = requestsTab.view(); bars(Ui.GREEN, Ui.WHITE, false); break;
            case PROFILE: v = profileTab.view(); bars(Ui.GREEN, Ui.WHITE, false); break;
            default: v = homeView(); bars(Ui.GREEN, Ui.WHITE, false);
        }
        host.addView(v, new FrameLayout.LayoutParams(-1, -1));
        updateBack();
    }

    @Override
    protected void onKeyboard(boolean shown) {
        if (nav != null) nav.setVisibility(shown ? View.GONE : View.VISIBLE);
    }

    private void updateBack() {
        if (Build.VERSION.SDK_INT < 33) return;
        OnBackInvokedDispatcher d = getOnBackInvokedDispatcher();
        if (tab != HOME && backCallback == null) {
            OnBackInvokedCallback cb = () -> show(HOME);
            backCallback = cb;
            d.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, cb);
        } else if (tab == HOME && backCallback != null) {
            d.unregisterOnBackInvokedCallback((OnBackInvokedCallback) backCallback);
            backCallback = null;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (tab != HOME) show(HOME);
        else super.onBackPressed();
    }

    private void listenCounters() {
        notifCounter = Ui.counter(this);
        chatCounter = Ui.counter(this);
        listeners.add(db.collection("notifications").whereEqualTo("userId", uid).whereEqualTo("isRead", false)
                .addSnapshotListener((s, e) -> { unreadNotifs = s == null ? 0 : s.size(); Ui.setCount(notifCounter, unreadNotifs); profileTab.counts(); }));
        listeners.add(db.collection("borrow_requests").whereEqualTo("ownerId", uid).whereEqualTo("status", "pending")
                .addSnapshotListener((s, e) -> { pendingCount = s == null ? 0 : s.size(); Ui.setCount(reqCounter, pendingCount); }));
        listeners.add(db.collection("conversations").whereArrayContains("participantIds", uid)
                .addSnapshotListener((s, e) -> {
                    int n = 0;
                    if (s != null) for (DocumentSnapshot d : s.getDocuments()) {
                        Object u = d.get("unread");
                        if (u instanceof Map) {
                            Object v = ((Map<?, ?>) u).get(uid);
                            if (v instanceof Number) n += ((Number) v).intValue();
                        }
                    }
                    unreadChats = n;
                    Ui.setCount(chatCounter, n);
                    profileTab.counts();
                }));
    }

    int unreadNotifs() { return unreadNotifs; }

    int unreadChats() { return unreadChats; }

    private TextView freshCounter(TextView old, int n) {
        if (old != null && old.getParent() != null) ((android.view.ViewGroup) old.getParent()).removeView(old);
        Ui.setCount(old, n);
        return old;
    }

    private View homeView() {
        ScrollView sc = Ui.scroll(this);
        sc.setBackgroundColor(Ui.CANVAS);
        LinearLayout col = Ui.col(this);
        sc.addView(col);

        LinearLayout head = Ui.col(this);
        android.graphics.drawable.GradientDrawable hb = Ui.gradient(this, Ui.GREEN, Ui.GREEN_DARK, 0);
        head.setBackground(hb);
        Ui.pad(head, 12, 8, 8, 12);

        LinearLayout top = Ui.row(this);
        String first = me == null ? "" : Data.firstName(Data.str(me, "fullName"));
        top.addView(Ui.lines(Ui.text(this, first.isEmpty() ? "أهلا بك" : "أهلا " + first, 18, Ui.WHITE, true), 1), Ui.lp(this, 0, Ui.WRAP, 1));
        top.addView(Ui.iconWithCounter(this, R.drawable.ic_forum, Ui.WHITE, 0x26FFFFFF, 42,
                freshCounter(chatCounter, unreadChats), v -> go(ConversationsActivity.class)));
        View bell = Ui.iconWithCounter(this, R.drawable.ic_notifications, Ui.WHITE, 0x26FFFFFF, 42,
                freshCounter(notifCounter, unreadNotifs), v -> go(NotificationsActivity.class));
        top.addView(bell, Ui.m(this, Ui.lp(this, 48, 48), 6, 0, 0, 0));
        head.addView(top);

        LinearLayout loc = Ui.row(this);
                loc.addView(Ui.text(this, me == null ? "..." : "حي " + Data.hoodName(me.getString("neighborhoodId")), 13, Ui.WHITE, false),
                Ui.lp(this, Ui.WRAP, Ui.WRAP));
        hoodInfo = Ui.text(this, "", 12, Ui.WHITE, false);
        hoodInfo.setVisibility(View.GONE);
        loc.addView(Ui.grow(this));
        loc.addView(hoodInfo);
        loc.setOnClickListener(v -> go(EditProfileActivity.class));
        head.addView(loc, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 2, 0, 0));
        col.addView(head, Ui.lp(this, Ui.MATCH, Ui.WRAP));

        LinearLayout sb = Ui.row(this);
        Ui.pad(sb, 16, 0, 16, 0);
        sb.setMinimumHeight(Ui.dp(this, 46));
        sb.addView(Ui.lines(Ui.text(this, "ابحث عن أداة...", 15, Ui.MUTED, false), 1), Ui.lp(this, 0, Ui.WRAP, 1));
        sb.addView(Ui.iv(this, R.drawable.ic_search, Ui.MUTED, 20));
        Ui.tap(sb, Ui.shape(this, Ui.WHITE, 10, Ui.LINE, 1), 10, v -> { searchCat = null; focusSearch = true; show(SEARCH); });
        LinearLayout.LayoutParams sp = Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 16, 0, 16, 0);
        sp.topMargin = Ui.dp(this, 12);
        col.addView(sb, sp);

        LinearLayout body = Ui.col(this);
        Ui.pad(body, 16, 4, 16, 24);
        col.addView(body);

        if (config != null && Boolean.TRUE.equals(config.getBoolean("announcementOn")) && !Data.str(config, "announcement").isEmpty()) {
            LinearLayout ann = Ui.row(this);
            ann.setGravity(Gravity.TOP);
            ann.setBackground(Ui.shape(this, Ui.AMBER_SOFT, 4));
            Ui.pad(ann, 12, 10, 12, 10);
            LinearLayout at = Ui.col(this);
            at.addView(Ui.text(this, "إعلان من إدارة نفع", 13, 0xFF9A6A00, true));
            TextView am = Ui.text(this, Data.str(config, "announcement"), 14, 0xFF5C4300, false);
            am.setLineSpacing(0, 1.25f);
            at.addView(am, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 2, 0, 0));
            ann.addView(at, Ui.lp(this, 0, Ui.WRAP, 1));
            body.addView(ann, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 14, 0, 0));
        }

        body.addView(Ui.section(this, "التصنيفات", null, null));
        LinearLayout catRow = null;
        for (int i = 0; i < Data.CAT_IDS.length; i++) {
            if (i % 3 == 0) {
                catRow = Ui.row(this);
                catRow.setGravity(Gravity.TOP);
                body.addView(catRow, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 0, 0, 10));
            }
            final String id = Data.CAT_IDS[i];
            LinearLayout c = Ui.col(this);
            c.setGravity(Gravity.CENTER_HORIZONTAL);
            Ui.pad(c, 4, 10, 4, 10);
            c.addView(Ui.iv(this, Data.CAT_ICONS[i], Ui.GREEN, 28));
            TextView tl = Ui.center(Ui.lines(Ui.text(this, Data.CAT_SHORT[i], 13, Ui.INK, false), 1));
            c.addView(tl, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 4, 0, 0));
            Ui.tap(c, Ui.shape(this, Ui.WHITE, 10, Ui.LINE, 1), 10, v -> { searchCat = id; focusSearch = false; show(SEARCH); });
            int pos = i % 3;
            catRow.addView(c, Ui.m(this, Ui.lp(this, 0, Ui.WRAP, 1), pos == 0 ? 0 : 5, 0, pos == 2 ? 0 : 5, 0));
        }

        TextView add = Ui.button(this, "أضف أداة", Ui.PRIMARY, 0);
        add.setOnClickListener(v -> go(AddToolActivity.class));
        body.addView(add, Ui.m(this, Ui.lp(this, Ui.MATCH, 46), 0, 6, 0, 0));

        body.addView(Ui.section(this, "متاحة بالقرب منك", "عرض الكل", v -> { searchCat = null; focusSearch = false; show(SEARCH); }));
        LinearLayout grid = Ui.col(this);
        grid.addView(Ui.loading(this));
        body.addView(grid);

        Data.forgetUser(uid);
        Data.user(db, uid, d -> {
            boolean first0 = me == null;
            me = d;
            checkGate();
            if (gated) return;
            if (first0 && d != null) { if (tab == HOME) show(HOME); return; }
            if (tab == HOME) loadTools(() -> renderHomeGrid(grid), err -> homeError(grid));
        });
        return sc;
    }

    private void homeError(LinearLayout grid) {
        {
            grid.removeAllViews();
            grid.addView(Ui.empty(this, R.drawable.ic_error_outline, "تعذر تحميل الأدوات", "تحقق من اتصالك بالإنترنت ثم حاول مجددا"));
            TextView retry = Ui.button(this, "إعادة المحاولة", Ui.SOFT, 0);
            retry.setOnClickListener(v -> show(HOME));
            grid.addView(retry, Ui.lp(this, Ui.MATCH, 50));
        }
    }

    private void loadTools(Runnable ok, Data.Cb<Exception> fail) {
        if (toolsLoaded) { ok.run(); return; }
        db.collection("tools").whereEqualTo("isAvailable", true).limit(200).get().addOnSuccessListener(s -> {
            tools.clear();
            String myHood = me == null ? "" : Data.str(me, "neighborhoodId");
            for (DocumentSnapshot d : s.getDocuments()) if (!uid.equals(d.getString("ownerId"))) tools.add(d);
            Collections.sort(tools, (a, b) -> {
                boolean ha = myHood.equals(a.getString("neighborhoodId")), hb = myHood.equals(b.getString("neighborhoodId"));
                if (ha != hb) return ha ? -1 : 1;
                return Long.compare(Data.millis(b, "createdAt"), Data.millis(a, "createdAt"));
            });
            toolsLoaded = true;
            ok.run();
        }).addOnFailureListener(fail::on);
    }

    private void renderHomeGrid(LinearLayout grid) {
        grid.removeAllViews();
        updateHoodInfo();
        if (tools.isEmpty()) {
            grid.addView(Ui.empty(this, R.drawable.ic_handyman, "لا توجد أدوات متاحة بعد", "كن أول من يشارك أداة في حيك ويساعد جيرانه"));
            return;
        }
        Cards.fillGrid(this, db, grid, tools.subList(0, Math.min(20, tools.size())));
        if (tools.size() > 20) {
            TextView more = Ui.button(this, "عرض كل الأدوات (" + tools.size() + ")", Ui.SOFT, R.drawable.ic_search);
            more.setOnClickListener(v -> { searchCat = null; focusSearch = false; show(SEARCH); });
            grid.addView(more, Ui.lp(this, Ui.MATCH, 50));
        }
    }

    private void updateHoodInfo() {
        if (hoodInfo == null || me == null) return;
        String myHood = Data.str(me, "neighborhoodId");
        int n = 0;
        for (DocumentSnapshot d : tools) if (myHood.equals(d.getString("neighborhoodId"))) n++;
        hoodInfo.setText(n == 0 ? "كن أول من يشارك" : Data.count(n, "أداة متاحة", "أداتان متاحتان", "أدوات متاحة", "أداة متاحة"));
        hoodInfo.setVisibility(View.VISIBLE);
    }

    private View searchView() {
        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(Ui.CANVAS);
        LinearLayout head = Ui.col(this);
        Ui.pad(head, 16, 12, 16, 8);
        TextView bt = Ui.text(this, "البحث", 18, Ui.WHITE, true);
        bt.setBackgroundColor(Ui.GREEN);
        Ui.pad(bt, 16, 0, 16, 0);
        root.addView(bt, Ui.lp(this, Ui.MATCH, 56));

        Ui.Field q = Ui.field(this, "اسم الأداة أو وصفها", R.drawable.ic_search, InputType.TYPE_CLASS_TEXT);
        q.box.setBackground(Ui.shape(this, Ui.WHITE, 10, Ui.LINE, 1));
        q.edit.setOnFocusChangeListener(null);
        ImageView clear = Ui.iv(this, R.drawable.ic_close, Ui.MUTED, 20);
        Ui.tap(clear, null, 20, v -> q.edit.setText(""));
        q.box.addView(clear);
        q.edit.setText(searchQuery);
        clear.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
        head.addView(q.box, Ui.lp(this, Ui.MATCH, Ui.WRAP));

        LinearLayout chips = Ui.row(this);
        List<TextView> all = new ArrayList<>();
        String[] ids = new String[Data.CAT_IDS.length + 1];
        String[] names = new String[ids.length];
        int[] icons = new int[ids.length];
        names[0] = "الكل"; icons[0] = R.drawable.ic_apps;
        for (int i = 0; i < Data.CAT_IDS.length; i++) { ids[i + 1] = Data.CAT_IDS[i]; names[i + 1] = Data.CAT_NAMES[i]; icons[i + 1] = Data.CAT_ICONS[i]; }
        LinearLayout results = Ui.col(this);
        TextView count = Ui.medium(this, "", 13, Ui.MUTED);
        Runnable render = () -> renderSearch(results, count);
        for (int i = 0; i < ids.length; i++) {
            TextView c = Ui.choice(this, names[i], icons[i]);
            final String id = ids[i];
            Ui.setChoice(c, id == null ? searchCat == null : id.equals(searchCat));
            c.setOnClickListener(v -> {
                searchCat = id;
                for (int k = 0; k < all.size(); k++) Ui.setChoice(all.get(k), k == 0 ? searchCat == null : ids[k].equals(searchCat));
                render.run();
            });
            all.add(c);
            chips.addView(c, Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 0, 0, 8, 0));
        }
        head.addView(Ui.hscroll(this, chips), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 12, 0, 0));

        LinearLayout meta = Ui.row(this);
        meta.addView(count, Ui.lp(this, 0, Ui.WRAP, 1));
        TextView sortChip = Ui.choice(this, SORTS[sortMode], R.drawable.ic_sort);
        sortChip.setOnClickListener(v -> Ui.options(this, "ترتيب النتائج", SORTS,
                new int[]{R.drawable.ic_history, R.drawable.ic_location_on, R.drawable.ic_sort}, null, k -> {
                    sortMode = k;
                    sortChip.setText(SORTS[k]);
                    render.run();
                }));
        meta.addView(sortChip, Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 0, 0, 8, 0));
        TextView hoodChip = Ui.choice(this, "حيي فقط", R.drawable.ic_location_on);
        Ui.setChoice(hoodChip, myHoodOnly);
        hoodChip.setOnClickListener(v -> { myHoodOnly = !myHoodOnly; Ui.setChoice(hoodChip, myHoodOnly); render.run(); });
        meta.addView(hoodChip);
        head.addView(meta, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 12, 0, 0));
        root.addView(head);

        ScrollView sc = Ui.scroll(this);
        Ui.pad(results, 16, 4, 16, 24);
        sc.addView(results);
        root.addView(sc, Ui.lp(this, Ui.MATCH, 0, 1));

        q.edit.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) {
                searchQuery = s.toString();
                clear.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                render.run();
            }
        });

        results.addView(Ui.loading(this));
        if (me == null) Data.user(db, uid, d -> me = d);
        loadTools(render, e -> {
            results.removeAllViews();
            results.addView(Ui.empty(this, R.drawable.ic_error_outline, "تعذر تحميل الأدوات", "تحقق من اتصالك بالإنترنت ثم حاول مجددا"));
        });
        if (focusSearch) {
            focusSearch = false;
            q.edit.requestFocus();
            q.edit.postDelayed(() -> {
                InputMethodManager im = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (im != null) im.showSoftInput(q.edit, InputMethodManager.SHOW_IMPLICIT);
            }, 200);
        }
        return root;
    }

    private void renderSearch(LinearLayout results, TextView count) {
        if (!toolsLoaded) return;
        results.removeAllViews();
        String nq = Data.norm(searchQuery);
        String myHood = me == null ? "" : Data.str(me, "neighborhoodId");
        int n = 0;
        List<DocumentSnapshot> sorted = new ArrayList<>(tools);
        if (sortMode == 0) Collections.sort(sorted, (a, b) -> Long.compare(Data.millis(b, "createdAt"), Data.millis(a, "createdAt")));
        else if (sortMode == 2) {
            java.text.Collator col = java.text.Collator.getInstance(new java.util.Locale("ar"));
            Collections.sort(sorted, (a, b) -> col.compare(Data.str(a, "toolName"), Data.str(b, "toolName")));
        }
        for (DocumentSnapshot d : sorted) {
            if (searchCat != null && !searchCat.equals(d.getString("categoryId"))) continue;
            if (myHoodOnly && !myHood.equals(d.getString("neighborhoodId"))) continue;
            if (!nq.isEmpty()) {
                String hay = Data.norm(Data.str(d, "toolName") + " " + Data.str(d, "description") + " " + Data.catName(d.getString("categoryId")));
                if (!hay.contains(nq)) continue;
            }
            results.addView(Cards.row(this, db, d, false), Ui.stack(this));
            n++;
        }
        count.setText(n == 0 ? "لا توجد نتائج" : Data.count(n, "نتيجة واحدة", "نتيجتان", "نتائج", "نتيجة"));
        if (n == 0) results.addView(Ui.empty(this, R.drawable.ic_search, "لم نجد ما تبحث عنه",
                "جرب كلمة أخرى أو اختر تصنيفا مختلفا"));
    }
}
