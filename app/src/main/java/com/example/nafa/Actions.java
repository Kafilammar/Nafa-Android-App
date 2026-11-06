package com.example.nafa;

import android.app.Activity;
import android.app.Dialog;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public final class Actions {
    private Actions() {}

    private static String toolName(DocumentSnapshot r) {
        String n = r.getString("toolName");
        return n == null || n.isEmpty() ? "الأداة" : n;
    }

    public static void accept(Activity a, FirebaseFirestore db, String me, DocumentSnapshot r, Runnable done) {
        String toolId = r.getString("toolId");
        if (toolId == null) return;
        DocumentReference tool = db.collection("tools").document(toolId);
        tool.get().addOnSuccessListener(t -> {
            if (t.exists() && "borrowed".equals(t.getString("status"))) {
                Ui.toast(a, "الأداة معارة حاليا، أنه الإعارة الحالية أولا");
                return;
            }
            WriteBatch b = db.batch();
            long start = Math.max(System.currentTimeMillis(), Data.millis(r, "startDate"));
            long due = start + Math.max(1, Data.num(r.getLong("durationValue"))) * Data.unitMillis(r.getString("durationUnit"));
            b.update(r.getReference(), Data.map("status", "accepted", "respondedAt", FieldValue.serverTimestamp(),
                    "dueAt", new Timestamp(new java.util.Date(due)), "updatedAt", FieldValue.serverTimestamp()));
            if (t.exists()) b.update(tool, Data.map("status", "borrowed", "isAvailable", false,
                    "currentBorrowerId", r.getString("borrowerId"), "updatedAt", FieldValue.serverTimestamp()));
            b.commit().addOnSuccessListener(v -> {
                Data.notify(db, me, r.getString("borrowerId"), "accepted", "تم قبول طلبك",
                        "وافق المالك على إعارتك «" + toolName(r) + "». موعد الإرجاع: " + Data.day(due) + ". تواصل معه لترتيب الاستلام.", r.getId());
                rejectOthers(db, me, toolId, r.getId());
                Ui.toast(a, "تم قبول الطلب");
                done.run();
            }).addOnFailureListener(e -> Ui.toast(a, "تعذر قبول الطلب، حاول مجددا"));
        }).addOnFailureListener(e -> Ui.toast(a, "تعذر قبول الطلب، حاول مجددا"));
    }

    private static void rejectOthers(FirebaseFirestore db, String me, String toolId, String keep) {
        db.collection("borrow_requests").whereEqualTo("ownerId", me).whereEqualTo("toolId", toolId).whereEqualTo("status", "pending").get()
                .addOnSuccessListener(s -> {
                    for (DocumentSnapshot d : s.getDocuments()) {
                        if (d.getId().equals(keep) || !me.equals(d.getString("ownerId"))) continue;
                        d.getReference().update(Data.map("status", "rejected", "respondedAt", FieldValue.serverTimestamp(), "updatedAt", FieldValue.serverTimestamp()));
                        Data.notify(db, me, d.getString("borrowerId"), "rejected", "تعذر قبول طلبك",
                                "«" + toolName(d) + "» أصبحت معارة لشخص آخر. يمكنك الطلب لاحقا.", d.getId());
                    }
                });
    }

    public static void reject(Activity a, FirebaseFirestore db, String me, DocumentSnapshot r, Runnable done) {
        Ui.confirm(a, "رفض الطلب", "هل تريد رفض طلب استعارة «" + toolName(r) + "»؟", "رفض", true, () ->
                r.getReference().update(Data.map("status", "rejected", "respondedAt", FieldValue.serverTimestamp(), "updatedAt", FieldValue.serverTimestamp()))
                        .addOnSuccessListener(v -> {
                            Data.notify(db, me, r.getString("borrowerId"), "rejected", "تم رفض طلبك",
                                    "اعتذر المالك عن إعارة «" + toolName(r) + "» هذه المرة.", r.getId());
                            Ui.toast(a, "تم رفض الطلب");
                            done.run();
                        }).addOnFailureListener(e -> Ui.toast(a, Data.err(e, "تعذر تنفيذ العملية"))));
    }

    public static void cancel(Activity a, FirebaseFirestore db, String me, DocumentSnapshot r, Runnable done) {
        Ui.confirm(a, "إلغاء الطلب", "هل تريد إلغاء طلب استعارة «" + toolName(r) + "»؟", "إلغاء الطلب", true, () ->
                r.getReference().update(Data.map("status", "cancelled", "updatedAt", FieldValue.serverTimestamp()))
                        .addOnSuccessListener(v -> {
                            Data.notify(db, me, r.getString("ownerId"), "cancelled", "ألغي طلب استعارة",
                                    "ألغى المستعير طلبه على «" + toolName(r) + "».", r.getId());
                            Ui.toast(a, "تم إلغاء الطلب");
                            done.run();
                        }).addOnFailureListener(e -> Ui.toast(a, Data.err(e, "تعذر تنفيذ العملية"))));
    }

    public static void returned(Activity a, FirebaseFirestore db, String me, DocumentSnapshot r, Runnable done) {
        Ui.confirm(a, "تأكيد الإرجاع", "هل استلمت «" + toolName(r) + "» من المستعير؟ ستعود الأداة متاحة للإعارة.", "نعم، تم الاستلام", false, () -> {
            WriteBatch b = db.batch();
            b.update(r.getReference(), Data.map("status", "returned", "returnedAt", FieldValue.serverTimestamp(), "updatedAt", FieldValue.serverTimestamp()));
            String toolId = r.getString("toolId");
            if (toolId != null) b.update(db.collection("tools").document(toolId), Data.map("status", "available", "isAvailable", true,
                    "currentBorrowerId", FieldValue.delete(), "borrowCount", FieldValue.increment(1), "updatedAt", FieldValue.serverTimestamp()));
            b.commit().addOnSuccessListener(v -> {
                Data.notify(db, me, r.getString("borrowerId"), "returned", "اكتملت الإعارة",
                        "شكرا لإرجاع «" + toolName(r) + "». شاركنا تقييمك لتجربتك.", r.getId());
                Ui.toast(a, "تم تسجيل الإرجاع");
                done.run();
            }).addOnFailureListener(e -> Ui.toast(a, Data.err(e, "تعذر تنفيذ العملية")));
        });
    }

    public static void review(Activity a, FirebaseFirestore db, String me, DocumentSnapshot r, boolean iAmOwner, String otherName0, Runnable done) {
        String reviewed = iAmOwner ? r.getString("borrowerId") : r.getString("ownerId");
        if (reviewed == null || reviewed.isEmpty()) { Ui.toast(a, "تعذر تحديد الطرف الآخر"); return; }
        String nameField = iAmOwner ? "borrowerName" : "ownerName";
        String otherName = otherName0 != null && !otherName0.isEmpty() ? otherName0
                : (Data.str(r, nameField).isEmpty() ? "جارك" : Data.str(r, nameField));

        LinearLayout c = Ui.col(a);
        c.addView(Ui.center(Ui.text(a, iAmOwner ? "كيف كان تعامل " + otherName + " مع أداتك؟"
                : "كيف كانت تجربتك مع " + otherName + " و«" + toolName(r) + "»؟", 15, Ui.MUTED, false)), Ui.lp(a, Ui.MATCH, Ui.WRAP));
        int[] rating = {0};
        c.addView(Ui.ratingPicker(a, rating, 40), Ui.m(a, Ui.lp(a, Ui.MATCH, Ui.WRAP), 0, 14, 0, 4));
        TextView hint = Ui.center(Ui.medium(a, "اضغط على النجوم للتقييم", 13, Ui.MUTED));
        c.addView(hint, Ui.m(a, Ui.lp(a, Ui.MATCH, Ui.WRAP), 0, 0, 0, 14));
        String[] words = {"", "سيئة", "مقبولة", "جيدة", "جيدة جدا", "ممتازة"};
        Ui.onRating = v -> { hint.setText("تجربة " + words[v]); hint.setTextColor(v >= 4 ? Ui.GREEN_DARK : v == 3 ? 0xFF9A6A00 : Ui.WARN); };
        Ui.Field comment = Ui.area(a, "اكتب تعليقا يساعد الجيران (اختياري)", 3);
        comment.edit.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(400)});
        c.addView(comment.box, Ui.lp(a, Ui.MATCH, Ui.WRAP));
        TextView send = Ui.button(a, "إرسال التقييم", Ui.PRIMARY, R.drawable.ic_star);
        c.addView(send, Ui.m(a, Ui.lp(a, Ui.MATCH, 54), 0, 16, 0, 0));
        Dialog d = Ui.sheet(a, "قيم التجربة", c);
        d.setOnDismissListener(x -> Ui.onRating = null);
        send.setOnClickListener(v -> {
            if (rating[0] == 0) { Ui.toast(a, "اختر عدد النجوم أولا"); return; }
            Ui.busy(send, true, "جار الإرسال...");
            Data.user(db, me, meDoc -> {
                String myName = Data.str(meDoc, "fullName");
                Map<String, Object> rv = new HashMap<>();
                rv.put("reviewerId", me);
                rv.put("reviewerName", myName);
                rv.put("reviewedUserId", reviewed);
                rv.put("reviewedName", otherName);
                rv.put("requestId", r.getId());
                rv.put("toolId", r.getString("toolId"));
                rv.put("toolName", toolName(r));
                rv.put("role", iAmOwner ? "lender" : "borrower");
                rv.put("rating", (double) rating[0]);
                rv.put("comment", comment.value());
                rv.put("createdAt", FieldValue.serverTimestamp());
                WriteBatch b = db.batch();
                b.set(db.collection("reviews").document(r.getId() + "_" + me), rv);
                b.update(r.getReference(), Data.map(iAmOwner ? "ownerReviewed" : "borrowerReviewed", true, "updatedAt", FieldValue.serverTimestamp()));
                b.commit().addOnSuccessListener(x -> {
                    Data.syncRating(db, reviewed);
                    Data.notify(db, me, reviewed, "review", "حصلت على تقييم جديد ⭐",
                            (myName.isEmpty() ? "أحد جيرانك" : myName) + " قيمك بـ " + rating[0] + " من 5"
                                    + (comment.value().isEmpty() ? "" : ": «" + comment.value() + "»"), r.getId());
                    Ui.onRating = null;
                    d.dismiss();
                    Ui.toast(a, "شكرا، تم إرسال تقييمك");
                    done.run();
                }).addOnFailureListener(e -> { Ui.busy(send, false, null); Ui.toast(a, Data.err(e, "تعذر إرسال التقييم")); });
            });
        });
    }

    public static void claimReturn(Activity a, FirebaseFirestore db, String me, DocumentSnapshot r, Runnable done) {
        Ui.confirm(a, "أعدت الأداة؟", "سنرسل إشعارا لصاحب «" + toolName(r) + "» ليؤكد الاستلام، وبعدها يمكنكما تقييم التجربة.", "نعم، أعدتها", false, () ->
                r.getReference().update(Data.map("returnClaimedAt", FieldValue.serverTimestamp(), "updatedAt", FieldValue.serverTimestamp()))
                        .addOnSuccessListener(v -> {
                            Data.notify(db, me, r.getString("ownerId"), "returned", "المستعير أعاد الأداة",
                                    "يقول " + Data.str(r, "borrowerName") + " إنه أعاد «" + toolName(r) + "». أكد الاستلام من صفحة الطلبات.", r.getId());
                            Ui.toast(a, "تم إبلاغ المالك، بانتظار تأكيده");
                            if (done != null) done.run();
                        }).addOnFailureListener(e -> Ui.toast(a, Data.err(e, "تعذر إرسال الإشعار"))));
    }

    public static void remind(Activity a, FirebaseFirestore db, String me, DocumentSnapshot r, Runnable done) {
        long last = Data.millis(r, "lastReminderAt");
        if (last > 0 && System.currentTimeMillis() - last < 12 * 3_600_000L) {
            Ui.toast(a, "أرسلت تذكيرا مؤخرا، يمكنك التذكير مجددا بعد قليل");
            return;
        }
        long due = Data.millis(r, "dueAt");
        long left = due > 0 ? Data.daysLeft(due) : 0;
        String when = due == 0 ? "" : left < 0 ? " (متأخرة " + Data.count(-left, "يوما واحدا", "يومين", "أيام", "يوما") + ")"
                : left == 0 ? " (موعدها اليوم)" : " (موعدها " + Data.day(due) + ")";
        Ui.confirm(a, "تذكير بالإرجاع", "سنرسل إشعارا للمستعير يذكره بإرجاع «" + toolName(r) + "»" + when + ".", "إرسال التذكير", false, () ->
                r.getReference().update(Data.map("lastReminderAt", FieldValue.serverTimestamp(), "updatedAt", FieldValue.serverTimestamp()))
                        .addOnSuccessListener(v -> {
                            Data.notify(db, me, r.getString("borrowerId"), "reminder", "تذكير بإرجاع أداة",
                                    "يذكرك المالك بإرجاع «" + toolName(r) + "»" + when + ". شكرا لالتزامك", r.getId());
                            Ui.toast(a, "تم إرسال التذكير");
                            if (done != null) done.run();
                        }).addOnFailureListener(e -> Ui.toast(a, "تعذر إرسال التذكير")));
    }

    public static void report(Activity a, FirebaseFirestore db, String me, String targetType, String targetId, String targetName) {
        String[] reasons = "tool".equals(targetType)
                ? new String[]{"معلومات أو صور مضللة", "أداة خطرة أو محظورة", "عرض تجاري أو إعلان", "محتوى غير لائق", "سبب آخر"}
                : new String[]{"سلوك مسيء أو غير لائق", "لم يرجع أداة", "احتيال أو انتحال", "رسائل مزعجة", "سبب آخر"};
        LinearLayout c = Ui.col(a);
        c.addView(Ui.text(a, "بلاغك سري ولن يطلع عليه الطرف الآخر. سيراجعه فريق نفع خلال 24 ساعة.", 13, Ui.MUTED, false),
                Ui.m(a, Ui.lp(a, Ui.MATCH, Ui.WRAP), 0, 0, 0, 12));
        int[] pick = {-1};
        java.util.List<TextView> chips = new java.util.ArrayList<>();
        for (int i = 0; i < reasons.length; i++) {
            final int k = i;
            TextView t = Ui.choice(a, reasons[i], 0);
            t.setOnClickListener(v -> { pick[0] = k; for (int j = 0; j < chips.size(); j++) Ui.setChoice(chips.get(j), j == k); });
            chips.add(t);
            c.addView(t, Ui.m(a, Ui.lp(a, Ui.MATCH, Ui.WRAP), 0, 0, 0, 8));
        }
        Ui.Field details = Ui.area(a, "تفاصيل إضافية تساعدنا (اختياري)", 3);
        c.addView(details.box, Ui.m(a, Ui.lp(a, Ui.MATCH, Ui.WRAP), 0, 6, 0, 0));
        TextView send = Ui.button(a, "إرسال البلاغ", Ui.DANGER, R.drawable.ic_flag);
        c.addView(send, Ui.m(a, Ui.lp(a, Ui.MATCH, 52), 0, 16, 0, 0));
        Dialog d = Ui.sheet(a, "tool".equals(targetType) ? "الإبلاغ عن الأداة" : "الإبلاغ عن المستخدم", c);
        send.setOnClickListener(v -> {
            if (pick[0] < 0) { Ui.toast(a, "اختر سبب البلاغ"); return; }
            Ui.busy(send, true, "جار الإرسال...");
            db.collection("reports").add(Data.map(
                    "reporterId", me,
                    "targetType", targetType,
                    "targetId", targetId,
                    "targetName", targetName == null ? "" : targetName,
                    "reason", reasons[pick[0]],
                    "details", details.value(),
                    "status", "open",
                    "createdAt", FieldValue.serverTimestamp()))
                    .addOnSuccessListener(x -> { d.dismiss(); Ui.toast(a, "شكرا لك، تم استلام البلاغ"); })
                    .addOnFailureListener(e -> { Ui.busy(send, false, null); Ui.toast(a, "تعذر إرسال البلاغ، حاول مجددا"); });
        });
    }
}
