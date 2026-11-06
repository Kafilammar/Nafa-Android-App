package com.example.nafa;

import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SupportActivity extends BaseActivity {
    private static final String[] TYPES = {"مشكلة تقنية", "مشكلة في إعارة", "بلاغ عن مستخدم أو أداة", "اقتراح", "أخرى"};
    private static final String[] TYPE_IDS = {"technical", "borrowing", "report", "suggestion", "other"};

    private static final String[][] FAQ = {
            {"كيف أستعير أداة؟", "افتح الأداة من الرئيسية أو البحث، ثم اضغط «طلب استعارة» وحدد المدة المطلوبة. سيصلك إشعار عند قبول المالك، وبعدها تتفقان على وقت الاستلام عبر المحادثة."},
            {"كيف أضيف أداتي للإعارة؟", "اضغط زر (+) في الشريط السفلي، أضف صورة واضحة واسما ووصفا والمدة القصوى للإعارة ثم احفظ. ستظهر أداتك مباشرة لجيرانك."},
            {"ماذا لو تأخر المستعير في الإرجاع؟", "تواصل معه أولا عبر المحادثة داخل التطبيق. وإن لم يتجاوب، أرسل بلاغا من هذه الصفحة وسيتابع فريق نفع الحالة."},
            {"هل الاستعارة مجانية؟", "نعم، نفع منصة تكافل بين الجيران ولا تتقاضى أي رسوم على الإعارة."},
            {"كيف أوقف عرض أداتي مؤقتا؟", "من «حسابي ← إدارة أدواتي» اضغط «إيقاف» على الأداة، ويمكنك إعادة تفعيلها في أي وقت."},
            {"كيف أحذف حسابي؟", "من «الإعدادات ← حذف الحساب نهائيا» مع إدخال كلمة المرور للتأكيد. سيتم حذف بياناتك وأدواتك."},
    };

    private Ui.Select type;
    private Ui.Field subject, message;
    private TextView send;
    private LinearLayout tickets;

    @Override
    protected void onReady(Bundle saved) {
        Ui.Screen s = Ui.screen(this, "المساعدة والدعم", true);
        LinearLayout b = s.body;


        b.addView(Ui.section(this, "الأسئلة الشائعة", null, null));
        LinearLayout faq = Ui.group(this);
        for (int i = 0; i < FAQ.length; i++) {
            if (i > 0) faq.addView(Ui.divider(this, 0));
            faq.addView(faqItem(FAQ[i][0], FAQ[i][1]));
        }
        b.addView(faq, Ui.stack(this));

        b.addView(Ui.section(this, "راسل فريق الدعم", null, null));
        LinearLayout form = Ui.card(this);
        type = Ui.select(this, "نوع الطلب", R.drawable.ic_category, TYPES);
        form.addView(type.box, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 0, 0, 10));
        subject = Ui.field(this, "عنوان مختصر", R.drawable.ic_edit, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        form.addView(subject.box, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 0, 0, 10));
        message = Ui.area(this, "اشرح المشكلة أو الاقتراح بالتفصيل...", 4);
        form.addView(message.box, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 0, 0, 14));
        send = Ui.button(this, "إرسال الطلب", Ui.PRIMARY, R.drawable.ic_send);
        send.setOnClickListener(v -> submit());
        form.addView(send, Ui.lp(this, Ui.MATCH, 52));
        b.addView(form, Ui.stack(this));

        b.addView(Ui.section(this, "طلباتي السابقة", null, null));
        tickets = Ui.col(this);
        b.addView(tickets, Ui.lp(this, Ui.MATCH, Ui.WRAP));
        loadTickets();
    }

    private View faqItem(String q, String a) {
        LinearLayout c = Ui.col(this);
        Ui.pad(c, 16, 14, 16, 14);
        LinearLayout r = Ui.row(this);
        r.addView(Ui.medium(this, q, 15, Ui.INK), Ui.lp(this, 0, Ui.WRAP, 1));
        ImageView arrow = Ui.iv(this, R.drawable.ic_chevron_left, Ui.MUTED, 20);
        arrow.setRotation(-90);
        r.addView(arrow);
        c.addView(r);
        TextView ans = Ui.text(this, a, 14, 0xFF5F6663, false);
        ans.setLineSpacing(0, 1.25f);
        ans.setVisibility(View.GONE);
        c.addView(ans, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 8, 0, 0));
        Ui.tap(c, null, 0, v -> {
            boolean open = ans.getVisibility() != View.VISIBLE;
            ans.setVisibility(open ? View.VISIBLE : View.GONE);
            arrow.setRotation(open ? 90 : -90);
        });
        return c;
    }

    private void submit() {
        if (type.index < 0) { Ui.toast(this, "اختر نوع الطلب"); return; }
        if (subject.value().length() < 3) { subject.error("اكتب عنوانا واضحا"); return; }
        if (message.value().length() < 10) { message.error("اكتب تفاصيل أكثر (10 أحرف على الأقل)"); return; }
        Ui.busy(send, true, "جار الإرسال...");
        Data.user(db, uid, me -> {
            Map<String, Object> m = Data.map(
                    "userId", uid,
                    "userName", Data.str(me, "fullName"),
                    "userEmail", Data.str(me, "email"),
                    "type", TYPE_IDS[type.index],
                    "subject", subject.value(),
                    "message", message.value(),
                    "status", "open",
                    "adminReply", "",
                    "platform", "android " + android.os.Build.VERSION.RELEASE + " · " + android.os.Build.MODEL,
                    "appVersion", InfoActivity.version(this),
                    "createdAt", FieldValue.serverTimestamp(),
                    "updatedAt", FieldValue.serverTimestamp());
            db.collection("support_requests").add(m).addOnSuccessListener(r -> {
                Ui.busy(send, false, null);
                subject.set("");
                message.set("");
                type.set(-1);
                Ui.toast(this, "تم إرسال طلبك، سنرد عليك قريبا");
                loadTickets();
            }).addOnFailureListener(e -> {
                Ui.busy(send, false, null);
                Ui.toast(this, "تعذر الإرسال، تحقق من الاتصال وحاول مجددا");
            });
        });
    }

    private void loadTickets() {
        tickets.removeAllViews();
        tickets.addView(Ui.loading(this));
        db.collection("support_requests").whereEqualTo("userId", uid).get().addOnSuccessListener(q -> {
            tickets.removeAllViews();
            List<DocumentSnapshot> l = new ArrayList<>(q.getDocuments());
            Collections.sort(l, (x, y) -> Long.compare(Data.millis(y, "createdAt"), Data.millis(x, "createdAt")));
            if (l.isEmpty()) {
                TextView t = Ui.center(Ui.text(this, "لم ترسل أي طلب دعم حتى الآن", 14, Ui.MUTED, false));
                Ui.pad(t, 16);
                tickets.addView(t, Ui.lp(this, Ui.MATCH, Ui.WRAP));
                return;
            }
            for (DocumentSnapshot d : l) tickets.addView(ticket(d), Ui.stack(this));
        }).addOnFailureListener(e -> {
            tickets.removeAllViews();
            tickets.addView(Ui.center(Ui.text(this, "تعذر تحميل الطلبات السابقة", 14, Ui.MUTED, false)), Ui.lp(this, Ui.MATCH, Ui.WRAP));
        });
    }

    private View ticket(DocumentSnapshot d) {
        LinearLayout c = Ui.card(this);
        LinearLayout top = Ui.row(this);
        top.addView(Ui.lines(Ui.text(this, Data.str(d, "subject"), 15, Ui.INK, true), 1), Ui.lp(this, 0, Ui.WRAP, 1));
        String st = Data.str(d, "status");
        int fg, bg;
        String lbl;
        switch (st) {
            case "closed":
            case "resolved": lbl = "تم الحل"; fg = Ui.GREEN_DARK; bg = Ui.MINT; break;
            case "in_progress": lbl = "قيد المعالجة"; fg = 0xFF9A6A00; bg = Ui.AMBER_SOFT; break;
            default: lbl = "مفتوح"; fg = 0xFF5F6663; bg = Ui.FIELD;
        }
        top.addView(Ui.chip(this, lbl, fg, bg, 0), Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 8, 0, 0, 0));
        c.addView(top);
        int ti = Math.max(0, java.util.Arrays.asList(TYPE_IDS).indexOf(Data.str(d, "type")));
        c.addView(Ui.text(this, TYPES[ti] + " · " + Data.ago(Data.millis(d, "createdAt")), 12, Ui.MUTED, false),
                Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 2, 0, 6));
        c.addView(Ui.lines(Ui.text(this, Data.str(d, "message"), 14, 0xFF4A4F4D, false), 3));
        String reply = Data.str(d, "adminReply");
        if (!reply.isEmpty()) {
            LinearLayout rp = Ui.col(this);
            rp.setBackground(Ui.shape(this, Ui.MINT, 10));
            Ui.pad(rp, 12, 10, 12, 10);
            TextView h = Ui.medium(this, "رد فريق نفع", 13, Ui.GREEN_DARK);
            h.setCompoundDrawablesRelative(Ui.icon(this, R.drawable.ic_verified, Ui.GREEN, 16), null, null, null);
            h.setCompoundDrawablePadding(Ui.dp(this, 6));
            h.setGravity(Gravity.CENTER_VERTICAL);
            rp.addView(h);
            rp.addView(Ui.text(this, reply, 14, Ui.INK, false), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 4, 0, 0));
            c.addView(rp, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 10, 0, 0));
        }
        return c;
    }
}
