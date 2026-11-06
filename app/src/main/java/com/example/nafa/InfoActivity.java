package com.example.nafa;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class InfoActivity extends BaseActivity {
    public static final String SUPPORT_EMAIL = "support@nafa.app";
    private static final String UPDATED = "آخر تحديث: سبتمبر 2026";

    public static void open(Activity a, String type) {
        Intent i = new Intent(a, InfoActivity.class);
        i.putExtra("type", type);
        a.startActivity(i);
    }

    public static String version(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    @Override
    protected boolean needsAuth() { return false; }

    @Override
    protected void onReady(Bundle saved) {
        String type = getIntent().getStringExtra("type");
        if ("privacy".equals(type)) privacy();
        else if ("about".equals(type)) about();
        else terms();
    }

    private LinearLayout page(String title, int icon, String intro) {
        Ui.Screen s = Ui.screen(this, title, true);
        s.body.addView(Ui.text(this, intro, 14, Ui.INK, false));
        s.body.addView(Ui.text(this, UPDATED, 12, Ui.MUTED, false), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 4, 0, 12));
        return s.body;
    }

    private void clause(LinearLayout b, int n, String title, String text) {
        b.addView(Ui.text(this, n + ". " + title, 16, Ui.INK, true), Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 10, 0, 4));
        b.addView(Ui.text(this, text, 14, 0xFF555555, false), Ui.lp(this, Ui.MATCH, Ui.WRAP));
    }

    private void contact(LinearLayout b) {
        TextView mail = Ui.button(this, "تواصل معنا: " + SUPPORT_EMAIL, Ui.SOFT, R.drawable.ic_email);
        mail.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + SUPPORT_EMAIL));
            i.putExtra(Intent.EXTRA_SUBJECT, "استفسار من تطبيق نفع");
            try { startActivity(i); } catch (Exception e) { Ui.toast(this, SUPPORT_EMAIL); }
        });
        b.addView(mail, Ui.m(this, Ui.lp(this, Ui.MATCH, 52), 0, 8, 0, 0));
    }

    private void terms() {
        LinearLayout b = page("الشروط والأحكام", R.drawable.ic_description,
                "باستخدامك تطبيق «نفع» فإنك توافق على الشروط التالية. نرجو قراءتها بعناية.");
        int i = 1;
        clause(b, i++, "التعريف بالخدمة", "«نفع» منصة مجتمعية مجانية تتيح للجيران إعارة الأدوات واستعارتها فيما بينهم. يقتصر دور المنصة على الربط بين المستخدمين، ولا تعد طرفا في اتفاق الإعارة ولا مالكة لأي أداة معروضة.");
        clause(b, i++, "الحساب والأهلية", "يجب أن يكون عمرك 18 عاما فأكثر، وأن تقدم بيانات صحيحة ومحدثة. أنت مسؤول عن سرية كلمة المرور وعن كل نشاط يتم عبر حسابك.");
        clause(b, i++, "عرض الأدوات", "يلتزم المعير بعرض أدوات يملكها فعليا وتكون سليمة وآمنة للاستخدام، مع وصف دقيق لحالتها وصور حقيقية. يمنع عرض أي أداة محظورة نظاما أو خطرة أو مخالفة للآداب العامة.");
        clause(b, i++, "الاستعارة والإرجاع", "يلتزم المستعير باستخدام الأداة فيما صممت له، والمحافظة عليها، وإرجاعها في الموعد المتفق عليه وبالحالة التي استلمها بها. أي تلف أو فقد يتم الاتفاق على معالجته وديا بين الطرفين.");
        clause(b, i++, "السلامة", "يتحمل كل مستخدم مسؤولية سلامته عند استخدام الأدوات. ننصح بالاستلام والتسليم في أماكن عامة أو آمنة، وبعدم مشاركة معلومات حساسة عبر المحادثات.");
        clause(b, i++, "البلاغات", "يمكنك الإبلاغ عن أي أداة أو مستخدم يخالف الشروط. البلاغات سرية ويراجعها فريق نفع، وقد يترتب عليها إيقاف الأداة أو الحساب المخالف.");
        clause(b, i++, "التقييمات والسلوك", "التقييمات يجب أن تكون صادقة وتعكس تجربة حقيقية. يمنع الإساءة أو التحرش أو الاحتيال أو استخدام المنصة لأغراض تجارية دون إذن.");
        clause(b, i++, "إيقاف الحسابات", "يحق لإدارة «نفع» إيقاف أو حذف أي حساب أو أداة تخالف هذه الشروط أو الأنظمة المعمول بها في المملكة العربية السعودية.");
        clause(b, i++, "إخلاء المسؤولية", "تقدم الخدمة «كما هي». لا تتحمل المنصة مسؤولية أي ضرر أو خلاف ينشأ بين المستخدمين، مع التزامها بالمساعدة في حل البلاغات قدر الإمكان.");
        clause(b, i++, "تعديل الشروط", "قد نحدث هذه الشروط من وقت لآخر، وسيتم إشعارك بالتغييرات الجوهرية داخل التطبيق. استمرارك في الاستخدام يعني موافقتك على الشروط المحدثة.");
        clause(b, i, "النظام الواجب التطبيق", "تخضع هذه الشروط لأنظمة المملكة العربية السعودية.");
        contact(b);
    }

    private void privacy() {
        LinearLayout b = page("سياسة الخصوصية", R.drawable.ic_privacy_tip,
                "خصوصيتك أولوية لدينا. توضح هذه السياسة ما نجمعه من بيانات وكيف نستخدمها ونحميها.");
        int i = 1;
        clause(b, i++, "البيانات التي نجمعها", "الاسم، البريد الإلكتروني، رقم الجوال، الحي السكني، صورة الملف الشخصي (اختيارية)، بيانات الأدوات وصورها، طلبات الاستعارة وتواريخها، الرسائل المتبادلة، التقييمات، قائمة المفضلة، والبلاغات وطلبات الدعم التي ترسلها.");
        clause(b, i++, "كيف نستخدم بياناتك", "لتشغيل الخدمة وربطك بجيرانك، عرض أدواتك للمستخدمين في المنطقة، إرسال الإشعارات المتعلقة بطلباتك، تحسين التطبيق، والتعامل مع البلاغات وطلبات الدعم.");
        clause(b, i++, "ما يراه الآخرون", "يظهر للمستخدمين الآخرين اسمك وصورتك وحيك وتقييمك وأدواتك المعروضة. لا نعرض بريدك الإلكتروني أو رقم جوالك بشكل علني.");
        clause(b, i++, "مشاركة البيانات", "لا نبيع بياناتك ولا نشاركها مع جهات تسويقية. نستخدم خدمات Google Firebase لاستضافة البيانات والمصادقة، وقد نفصح عن البيانات إذا طلبت ذلك جهة رسمية مختصة وفق الأنظمة.");
        clause(b, i++, "حماية البيانات", "تنقل البيانات مشفرة، ويتم تقييد الوصول إليها بقواعد أمان تضمن أن كل مستخدم لا يعدل إلا بياناته.");
        clause(b, i++, "حقوقك", "يمكنك تعديل بياناتك في أي وقت من «تعديل الملف الشخصي»، ويمكنك حذف حسابك وجميع بياناتك نهائيا من «الإعدادات ← حذف الحساب».");
        clause(b, i++, "الاحتفاظ بالبيانات", "نحتفظ ببياناتك طوال فترة نشاط حسابك، وتحذف عند حذف الحساب، باستثناء ما يلزم الاحتفاظ به نظاما.");
        clause(b, i, "التواصل", "لأي استفسار حول الخصوصية يمكنك مراسلتنا عبر صفحة المساعدة والدعم أو البريد الإلكتروني أدناه.");
        contact(b);
    }

    private void about() {
        Ui.Screen s = Ui.screen(this, "عن نفع", true);
        LinearLayout b = s.body;
        LinearLayout hero = Ui.col(this);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        Ui.pad(hero, 10, 10, 10, 10);
        android.widget.ImageView logo = new android.widget.ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher_round);
        hero.addView(logo, Ui.lp(this, 64, 64));
        hero.addView(Ui.text(this, "نفع", 22, Ui.INK, true), Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 0, 10, 0, 0));
        hero.addView(Ui.center(Ui.text(this, "أدواتك تنفع جيرانك", 14, Ui.MUTED, false)));
        TextView v = Ui.text(this, "الإصدار " + version(this), 12, Ui.MUTED, false);
        LinearLayout.LayoutParams vp = Ui.m(this, Ui.lp(this, Ui.WRAP, Ui.WRAP), 0, 12, 0, 0);
        hero.addView(v, vp);
        b.addView(hero, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 4, 0, 12));

        LinearLayout c = Ui.card(this);
        c.addView(Ui.text(this, "فكرتنا", 17, Ui.INK, true));
        TextView d = Ui.text(this, "كم أداة في بيتك تستخدمها مرة واحدة في السنة؟ «نفع» يحول هذه الأدوات إلى قيمة مشتركة بين الجيران: تستعير ما تحتاجه بدل شرائه، وتعير ما لا تستخدمه ليستفيد منه غيرك. نوفر المال، ونقلل الهدر، ونقوي روابط الحي.", 14, 0xFF4A4F4D, false);
                c.addView(d, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 0, 8, 0, 0));
        b.addView(c, Ui.stack(this));


        LinearLayout links = Ui.group(this);
        links.addView(Ui.menuRow(this, R.drawable.ic_description, "الشروط والأحكام", null, Ui.GREEN, Ui.MINT, x -> open(this, "terms")));
        links.addView(Ui.divider(this, 0));
        links.addView(Ui.menuRow(this, R.drawable.ic_privacy_tip, "سياسة الخصوصية", null, Ui.GREEN, Ui.MINT, x -> open(this, "privacy")));
        b.addView(links, Ui.stack(this));
        contact(b);
    }
}
