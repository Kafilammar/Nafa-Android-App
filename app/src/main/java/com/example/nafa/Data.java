package com.example.nafa;

import android.app.Activity;
import android.content.Intent;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Data {
    private Data() {}

    public interface Cb<T> { void on(T t); }

    public static final String[] CAT_IDS = {"electric_tools", "hand_tools", "gardening", "cleaning", "construction", "other"};
    public static final String[] CAT_NAMES = {"أدوات كهربائية", "أدوات يدوية", "أدوات الزراعة", "أدوات التنظيف", "أدوات البناء", "أخرى"};
    public static final String[] CAT_SHORT = {"كهربائية", "يدوية", "الزراعة", "التنظيف", "البناء", "أخرى"};
    public static final int[] CAT_ICONS = {R.drawable.ic_flash_on, R.drawable.ic_handyman, R.drawable.ic_grass,
            R.drawable.ic_cleaning_services, R.drawable.ic_construction, R.drawable.ic_category};

    public static int catIndex(String id) {
        for (int i = 0; i < CAT_IDS.length; i++) if (CAT_IDS[i].equals(id)) return i;
        return CAT_IDS.length - 1;
    }

    public static String catName(String id) { return CAT_NAMES[catIndex(id)]; }

    public static int catIcon(String id) { return CAT_ICONS[catIndex(id)]; }

    public static final String[] CONDITION_IDS = {"new", "excellent", "good", "fair"};
    public static final String[] CONDITION_NAMES = {"جديدة", "ممتازة", "جيدة", "مقبولة"};

    public static String conditionName(String id) {
        for (int i = 0; i < CONDITION_IDS.length; i++) if (CONDITION_IDS[i].equals(id)) return CONDITION_NAMES[i];
        return null;
    }

    public static final String[] UNIT_IDS = {"hour", "day", "week"};
    public static final String[] UNIT_NAMES = {"ساعة", "يوم", "أسبوع"};

    public static int unitIndex(String id) {
        for (int i = 0; i < UNIT_IDS.length; i++) if (UNIT_IDS[i].equals(id)) return i;
        return 1;
    }

    public static String duration(long n, String unit) {
        switch (unit == null ? "day" : unit) {
            case "hour": return count(n, "ساعة واحدة", "ساعتان", "ساعات", "ساعة");
            case "week": return count(n, "أسبوع واحد", "أسبوعان", "أسابيع", "أسبوعا");
            default: return count(n, "يوم واحد", "يومان", "أيام", "يوما");
        }
    }

    public static String count(long n, String one, String two, String few, String many) {
        if (n == 1) return one;
        if (n == 2) return two;
        if (n >= 3 && n <= 10) return n + " " + few;
        return n + " " + many;
    }

    public static String toolStatusLabel(String s) {
        if ("borrowed".equals(s)) return "معارة حاليا";
        if ("paused".equals(s)) return "موقوفة مؤقتا";
        return "متاحة الآن";
    }

    public static int[] toolStatusColors(String s) {
        if ("borrowed".equals(s)) return new int[]{0xFF9A6A00, Ui.AMBER_SOFT};
        if ("paused".equals(s)) return new int[]{Ui.MUTED, Ui.FIELD};
        return new int[]{Ui.GREEN_DARK, Ui.MINT};
    }

    public static String requestLabel(String s) {
        if (s == null) return "";
        switch (s) {
            case "pending": return "بانتظار الرد";
            case "accepted": return "مقبول";
            case "rejected": return "مرفوض";
            case "cancelled": return "ملغي";
            case "returned": return "تم الإرجاع";
            default: return s;
        }
    }

    public static int[] requestColors(String s) {
        if ("pending".equals(s)) return new int[]{0xFF9A6A00, Ui.AMBER_SOFT};
        if ("accepted".equals(s)) return new int[]{Ui.GREEN_DARK, Ui.MINT};
        if ("rejected".equals(s)) return new int[]{Ui.WARN, Ui.WARN_SOFT};
        return new int[]{0xFF5F6663, Ui.FIELD};
    }

    public static int requestIcon(String s) {
        if ("pending".equals(s)) return R.drawable.ic_hourglass_top;
        if ("accepted".equals(s)) return R.drawable.ic_check_circle;
        if ("rejected".equals(s)) return R.drawable.ic_cancel;
        if ("returned".equals(s)) return R.drawable.ic_assignment_return;
        return R.drawable.ic_close;
    }

    private static final String[] HOOD_IDS = {"al_malqa", "al_narjis", "al_yasmin", "al_sahafa", "al_aqiq", "hittin",
            "al_nakheel", "al_rabie", "al_nada", "al_wadi", "al_ghadir", "al_mursalat", "al_qirawan", "al_arid"};
    private static final String[] HOOD_NAMES = {"الملقا", "النرجس", "الياسمين", "الصحافة", "العقيق", "حطين",
            "النخيل", "الربيع", "الندى", "الوادي", "الغدير", "المرسلات", "القيروان", "العارض"};
    private static final LinkedHashMap<String, String> HOODS = new LinkedHashMap<>();

    static {
        for (int i = 0; i < HOOD_IDS.length; i++) HOODS.put(HOOD_IDS[i], HOOD_NAMES[i]);
    }

    public static String hoodName(String id) {
        if (id == null || id.isEmpty()) return "حي غير محدد";
        String n = HOODS.get(id);
        return n != null ? n : id;
    }

    public static List<String> hoodIds() { return new ArrayList<>(HOODS.keySet()); }

    public static String[][] defaultHoods() {
        String[][] r = new String[HOOD_IDS.length][2];
        for (int i = 0; i < HOOD_IDS.length; i++) { r[i][0] = HOOD_IDS[i]; r[i][1] = HOOD_NAMES[i]; }
        return r;
    }

    public static List<String> hoodNames() { return new ArrayList<>(HOODS.values()); }

    public static void loadHoods(FirebaseFirestore db, Runnable done) {
        db.collection("neighborhoods").get().addOnSuccessListener(s -> {
            List<DocumentSnapshot> docs = new ArrayList<>(s.getDocuments());
            List<DocumentSnapshot> active = new ArrayList<>();
            for (DocumentSnapshot d : docs)
                if (!Boolean.FALSE.equals(d.getBoolean("isActive")) && d.getString("name") != null) active.add(d);
            if (!active.isEmpty()) {
                Collections.sort(active, (a, b) -> Long.compare(num(a.getLong("sortOrder")), num(b.getLong("sortOrder"))));
                HOODS.clear();
                for (DocumentSnapshot d : active) HOODS.put(d.getId(), d.getString("name"));
            }
            done.run();
        }).addOnFailureListener(e -> done.run());
    }

    public static String str(DocumentSnapshot d, String f) {
        String s = d == null ? null : d.getString(f);
        return s == null ? "" : s;
    }

    public static long num(Long l) { return l == null ? 0 : l; }

    public static double dbl(Double d) { return d == null ? 0 : d; }

    public static String firstName(String full) {
        if (full == null || full.trim().isEmpty()) return "";
        return full.trim().split("\\s+")[0];
    }

    public static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "ن";
        String[] p = name.trim().split("\\s+");
        String a = p[0].substring(0, 1);
        return p.length > 1 ? a + " " + p[1].substring(0, 1) : a;
    }

    public static String rating(double r) {
        return String.format(Locale.US, "%.1f", r);
    }

    public static String norm(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("[أإآ]", "ا").replace('ة', 'ه').replace('ى', 'ي')
                .replaceAll("[\\u064B-\\u0652\\u0640]", "").trim();
    }

    public static long millis(DocumentSnapshot d, String f) {
        Timestamp t = d.getTimestamp(f, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
        return t == null ? 0 : t.toDate().getTime();
    }

    public static String ago(long ms) {
        if (ms <= 0) return "الآن";
        long diff = Math.max(0, System.currentTimeMillis() - ms) / 1000;
        if (diff < 60) return "الآن";
        long m = diff / 60;
        if (m < 60) return "منذ " + count(m, "دقيقة", "دقيقتين", "دقائق", "دقيقة");
        long h = m / 60;
        if (h < 24) return "منذ " + count(h, "ساعة", "ساعتين", "ساعات", "ساعة");
        long d = h / 24;
        if (d < 7) return d == 1 ? "أمس" : "منذ " + count(d, "يوم", "يومين", "أيام", "يوما");
        return new SimpleDateFormat("yyyy/MM/dd", Locale.US).format(new Date(ms));
    }

    public static String clock(long ms) {
        if (ms <= 0) return "";
        return new SimpleDateFormat("h:mm a", new Locale("ar")).format(new Date(ms));
    }

    public static String authError(Exception e) {
        if (e instanceof FirebaseNetworkException) return "لا يوجد اتصال بالإنترنت، تحقق من الشبكة وحاول مجددا";
        if (e instanceof FirebaseTooManyRequestsException) return "محاولات كثيرة، انتظر قليلا ثم حاول مجددا";
        if (e instanceof FirebaseAuthException) {
            String c = ((FirebaseAuthException) e).getErrorCode();
            if (c == null) c = "";
            switch (c) {
                case "ERROR_INVALID_EMAIL": return "صيغة البريد الإلكتروني غير صحيحة";
                case "ERROR_WRONG_PASSWORD":
                case "ERROR_INVALID_CREDENTIAL":
                case "ERROR_USER_NOT_FOUND": return "البريد الإلكتروني أو كلمة المرور غير صحيحة";
                case "ERROR_USER_DISABLED": return "تم إيقاف هذا الحساب، تواصل مع الدعم";
                case "ERROR_EMAIL_ALREADY_IN_USE": return "هذا البريد مسجل مسبقا، جرب تسجيل الدخول";
                case "ERROR_WEAK_PASSWORD": return "كلمة المرور ضعيفة، استخدم 6 أحرف على الأقل";
                case "ERROR_REQUIRES_RECENT_LOGIN": return "لأمان حسابك، سجل الدخول من جديد ثم أعد المحاولة";
            }
        }
        return "تعذر إتمام العملية، حاول مرة أخرى";
    }

    public static long unitMillis(String unit) {
        if ("hour".equals(unit)) return 3_600_000L;
        if ("week".equals(unit)) return 7L * 86_400_000L;
        return 86_400_000L;
    }

    public static long startOfDay(long ms) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static String day(long ms) {
        if (ms <= 0) return "";
        long d = (startOfDay(ms) - startOfDay(System.currentTimeMillis())) / 86_400_000L;
        if (d == 0) return "اليوم";
        if (d == 1) return "غدا";
        if (d == -1) return "أمس";
        return new SimpleDateFormat("EEEE d MMMM", new Locale("ar")).format(new Date(ms));
    }

    public static String monthYear(long ms) {
        if (ms <= 0) return "";
        return new SimpleDateFormat("MMMM yyyy", new Locale("ar")).format(new Date(ms));
    }

    public static long daysLeft(long dueMs) {
        return (startOfDay(dueMs) - startOfDay(System.currentTimeMillis())) / 86_400_000L;
    }

    private static final java.util.Set<String> FAVS = new java.util.LinkedHashSet<>();
    private static boolean favsLoaded;

    public static void loadFavs(FirebaseFirestore db, String uid, Runnable done) {
        if (uid == null) return;
        db.collection("users").document(uid).get().addOnSuccessListener(d -> {
            FAVS.clear();
            Object f = d.get("favoriteIds");
            if (f instanceof List) for (Object o : (List<?>) f) if (o != null) FAVS.add(o.toString());
            favsLoaded = true;
            if (done != null) done.run();
        }).addOnFailureListener(e -> { favsLoaded = true; if (done != null) done.run(); });
    }

    public static boolean isFav(String toolId) { return toolId != null && FAVS.contains(toolId); }

    public static boolean favsLoaded() { return favsLoaded; }

    public static List<String> favIds() {
        List<String> l = new ArrayList<>(FAVS);
        Collections.reverse(l);
        return l;
    }

    public static void toggleFav(Activity a, FirebaseFirestore db, String uid, String toolId, String toolName, Cb<Boolean> ui) {
        if (uid == null || toolId == null) return;
        boolean add = !FAVS.contains(toolId);
        if (add) FAVS.add(toolId); else FAVS.remove(toolId);
        if (ui != null) ui.on(add);
        db.collection("users").document(uid)
                .update("favoriteIds", add ? FieldValue.arrayUnion(toolId) : FieldValue.arrayRemove(toolId))
                .addOnSuccessListener(v -> { forgetUser(uid); Ui.toast(a, add ? "أضيفت إلى المفضلة" : "أزيلت من المفضلة"); })
                .addOnFailureListener(e -> {
                    if (add) FAVS.remove(toolId); else FAVS.add(toolId);
                    if (ui != null) ui.on(!add);
                    Ui.toast(a, err(e, "تعذر تحديث المفضلة"));
                });
    }

    public static void forgetFav(FirebaseFirestore db, String uid, String toolId) {
        if (FAVS.remove(toolId) && db != null && uid != null)
            db.collection("users").document(uid).update("favoriteIds", FieldValue.arrayRemove(toolId));
    }

    private static final Map<String, double[]> RATINGS = new HashMap<>();

    public static void ratingOf(FirebaseFirestore db, String userId, Cb<double[]> cb) {
        if (userId == null || userId.isEmpty()) { cb.on(new double[]{0, 0}); return; }
        double[] c = RATINGS.get(userId);
        if (c != null) { cb.on(c); return; }
        db.collection("reviews").whereEqualTo("reviewedUserId", userId).get().addOnSuccessListener(q -> {
            double sum = 0;
            int n = 0;
            for (DocumentSnapshot d : q.getDocuments()) {
                double r = dbl(d.getDouble("rating"));
                if (r > 0) { sum += r; n++; }
            }
            double[] res = {n == 0 ? 0 : sum / n, n};
            RATINGS.put(userId, res);
            cb.on(res);
        }).addOnFailureListener(e -> cb.on(new double[]{0, 0}));
    }

    public static void forgetRating(String userId) { RATINGS.remove(userId); }

    public static void syncRating(FirebaseFirestore db, String userId) {
        forgetRating(userId);
        ratingOf(db, userId, r -> db.collection("users").document(userId)
                .update("ratingAverage", Math.round(r[0] * 10) / 10.0, "ratingCount", (long) r[1]));
    }

    public static boolean isAdmin(DocumentSnapshot u) {
        return u != null && ("admin".equals(u.getString("role")) || Boolean.TRUE.equals(u.getBoolean("isAdmin")));
    }

    public static boolean isSuspended(DocumentSnapshot u) {
        return u != null && Boolean.FALSE.equals(u.getBoolean("isActive")) && !isAdmin(u);
    }

    public static String err(Exception e, String fallback) {
        if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
            com.google.firebase.firestore.FirebaseFirestoreException.Code c = ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode();
            if (c == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED)
                return fallback + ": قاعدة البيانات رفضت العملية (تأكد من نشر قواعد الأمان المرفقة)";
            if (c == com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE)
                return fallback + ": لا يوجد اتصال بالإنترنت";
        }
        if (e instanceof FirebaseNetworkException) return fallback + ": لا يوجد اتصال بالإنترنت";
        return fallback;
    }

    public static void openProfile(Activity a, String userId, String name) {
        if (userId == null || userId.isEmpty()) return;
        Intent i = new Intent(a, UserProfileActivity.class);
        i.putExtra("userId", userId);
        i.putExtra("userName", name);
        a.startActivity(i);
    }

    public static void openRequests(Activity a) { openRequests(a, -1, -1); }

    public static void openRequests(Activity a, int in, int filter) {
        Intent i = new Intent(a, MainActivity.class);
        i.putExtra("tab", MainActivity.REQUESTS);
        i.putExtra("reqIn", in);
        i.putExtra("reqFilter", filter);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        a.startActivity(i);
    }

    public static void shareText(Activity a, String subject, String text) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, subject);
        i.putExtra(Intent.EXTRA_TEXT, text);
        try { a.startActivity(Intent.createChooser(i, "مشاركة عبر")); } catch (Exception e) { Ui.toast(a, "لا يوجد تطبيق للمشاركة"); }
    }

    public static String storeLink(Activity a) {
        return "https://play.google.com/store/apps/details?id=" + a.getPackageName();
    }

    public static void shareTool(Activity a, DocumentSnapshot t) {
        String n = str(t, "toolName");
        String txt = "وجدت «" + n + "» متاحة للإعارة مجانا في حي " + hoodName(t.getString("neighborhoodId"))
                + " على تطبيق نفع\n" + (str(t, "description").isEmpty() ? "" : str(t, "description") + "\n")
                + "حمل التطبيق: " + storeLink(a);
        shareText(a, n, txt);
    }

    public static void shareApp(Activity a) {
        shareText(a, "تطبيق نفع", "جرب تطبيق «نفع» لإعارة واستعارة الأدوات بين الجيران مجانا\n" + storeLink(a));
    }

    public static void rateApp(Activity a) {
        try {
            a.startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=" + a.getPackageName())));
        } catch (Exception e) {
            try { a.startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(storeLink(a)))); }
            catch (Exception x) { Ui.toast(a, "تعذر فتح المتجر"); }
        }
    }

    private static final Map<String, DocumentSnapshot> USERS = new HashMap<>();

    public static void user(FirebaseFirestore db, String id, Cb<DocumentSnapshot> cb) {
        if (id == null || id.isEmpty()) { cb.on(null); return; }
        DocumentSnapshot c = USERS.get(id);
        if (c != null) { cb.on(c); return; }
        String me = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (id.equals(me)) {
            db.collection("users").document(id).get()
                    .addOnSuccessListener(d -> { if (d.exists()) USERS.put(id, d); cb.on(d.exists() ? d : null); })
                    .addOnFailureListener(e -> cb.on(null));
        } else {
            db.collection("public_profiles").document(id).get()
                    .addOnSuccessListener(d -> { if (d.exists()) USERS.put(id, d); cb.on(d.exists() ? d : null); })
                    .addOnFailureListener(e -> cb.on(null));
        }
    }

    public static void ensurePublicProfile(FirebaseFirestore db, String uid) {
        if (uid == null || uid.isEmpty()) return;
        db.collection("users").document(uid).get().addOnSuccessListener(d -> {
            if (!d.exists()) return;
            Map<String, Object> p = publicProfileMap(d);
            db.collection("public_profiles").document(uid).set(p, SetOptions.merge());
        });
    }

    private static Map<String, Object> publicProfileMap(DocumentSnapshot d) {
        Map<String, Object> p = new HashMap<>();
        p.put("fullName", str(d, "fullName"));
        p.put("profileImageBase64", str(d, "profileImageBase64"));
        p.put("neighborhoodId", str(d, "neighborhoodId"));
        p.put("ratingAverage", dbl(d.getDouble("ratingAverage")));
        p.put("ratingCount", d.getLong("ratingCount") == null ? 0L : d.getLong("ratingCount"));
        p.put("isVerified", Boolean.TRUE.equals(d.getBoolean("isVerified")));
        Object created = d.get("createdAt");
        if (created != null) p.put("createdAt", created);
        p.put("updatedAt", FieldValue.serverTimestamp());
        return p;
    }

    public static void syncAllPublicProfiles(FirebaseFirestore db, Cb<String> cb) {
        db.collection("users").limit(1500).get().addOnSuccessListener(q -> {
            List<DocumentSnapshot> all = q.getDocuments();
            if (all.isEmpty()) { cb.on("تمت المزامنة، ولا يوجد مستخدمون"); return; }
            syncPublicProfileBatches(db, all, 0, 0, cb);
        }).addOnFailureListener(e -> cb.on("تعذر قراءة المستخدمين: " + err(e, "تحقق من الصلاحيات والاتصال")));
    }

    private static void syncPublicProfileBatches(FirebaseFirestore db, List<DocumentSnapshot> users, int index, int synced, Cb<String> cb) {
        if (index >= users.size()) {
            cb.on("تمت مزامنة " + synced + " ملفا عاما");
            return;
        }
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        int end = Math.min(index + 450, users.size());
        for (int i = index; i < end; i++) {
            DocumentSnapshot u = users.get(i);
            batch.set(db.collection("public_profiles").document(u.getId()), publicProfileMap(u), SetOptions.merge());
        }
        final int next = end;
        final int nextSynced = synced + (end - index);
        batch.commit()
                .addOnSuccessListener(v -> syncPublicProfileBatches(db, users, next, nextSynced, cb))
                .addOnFailureListener(e -> cb.on("تعذرت مزامنة الملفات العامة عند المستخدم رقم " + (index + 1) + ": " + err(e, "تحقق من الصلاحيات")));
    }

    public static void forgetUser(String id) { USERS.remove(id); }

    public static void clearCache() { USERS.clear(); FAVS.clear(); RATINGS.clear(); favsLoaded = false; }

    public static void notify(FirebaseFirestore db, String me, String to, String type, String title, String body, String refId) {
        if (to == null || to.isEmpty() || (to.equals(me) && !"system".equals(type))) return;
        Map<String, Object> n = new HashMap<>();
        n.put("userId", to);
        n.put("senderId", me);
        n.put("type", type);
        n.put("title", title);
        n.put("body", body);
        n.put("refId", refId == null ? "" : refId);
        n.put("isRead", false);
        n.put("createdAt", FieldValue.serverTimestamp());
        db.collection("notifications").add(n);
    }

    public static int notifIcon(String type) {
        if (type == null) return R.drawable.ic_notifications;
        switch (type) {
            case "request": return R.drawable.ic_swap_horiz;
            case "accepted": return R.drawable.ic_check_circle;
            case "rejected": return R.drawable.ic_cancel;
            case "cancelled": return R.drawable.ic_close;
            case "returned": return R.drawable.ic_assignment_return;
            case "review": return R.drawable.ic_star;
            case "message": return R.drawable.ic_chat;
            case "reminder": return R.drawable.ic_notifications_active;
            case "support": return R.drawable.ic_support_agent;
            case "announcement": return R.drawable.ic_notifications_active;
            case "system": return R.drawable.ic_eco;
            default: return R.drawable.ic_info;
        }
    }

    public static String chatId(String a, String b) {
        return a.compareTo(b) < 0 ? a + "_" + b : b + "_" + a;
    }

    public static void openChat(Activity a, FirebaseFirestore db, String me, String other, String otherName) {
        if (me == null || other == null || me.equals(other)) return;
        user(db, me, meDoc -> {
            String myName = str(meDoc, "fullName");
            String id = chatId(me, other);
            Map<String, Object> names = new HashMap<>();
            names.put(me, myName);
            names.put(other, otherName == null ? "" : otherName);
            Map<String, Object> c = new HashMap<>();
            c.put("participantIds", Arrays.asList(me.compareTo(other) < 0 ? me : other, me.compareTo(other) < 0 ? other : me));
            c.put("participantNames", names);
            c.put("updatedAt", FieldValue.serverTimestamp());
            db.collection("conversations").document(id).set(c, SetOptions.merge())
                    .addOnSuccessListener(v -> {
                        Intent i = new Intent(a, ChatActivity.class);
                        i.putExtra("conversationId", id);
                        i.putExtra("otherId", other);
                        i.putExtra("otherName", otherName);
                        a.startActivity(i);
                    })
                    .addOnFailureListener(e -> Ui.toast(a, "تعذر فتح المحادثة"));
        });
    }

    public static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }
}
