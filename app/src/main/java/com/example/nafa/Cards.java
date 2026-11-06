package com.example.nafa;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public final class Cards {
    private Cards() {}

    public static void openTool(Activity a, String id) {
        Intent i = new Intent(a, ToolDetailsActivity.class);
        i.putExtra("toolId", id);
        a.startActivity(i);
    }

    public static FrameLayout toolImage(Activity a, FirebaseFirestore db, DocumentSnapshot d, float radius, int iconSize) {
        FrameLayout f = new FrameLayout(a);
        f.setBackground(Ui.shape(a, Ui.MINT, radius));
        f.setClipToOutline(true);
        ImageView ph = Ui.iv(a, Data.catIcon(d.getString("categoryId")), 0x6600852B, iconSize);
        f.addView(ph, new FrameLayout.LayoutParams(Ui.dp(a, iconSize), Ui.dp(a, iconSize), Gravity.CENTER));
        ImageView img = new ImageView(a);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        f.addView(img, new FrameLayout.LayoutParams(-1, -1));
        String thumb = d.getString("thumbBase64");
        if (thumb != null && !thumb.isEmpty()) {
            Img.load(img, thumb, Img.THUMB_PX);
        } else if (db != null) {
            db.collection("tool_images").whereEqualTo("toolId", d.getId()).limit(1).get()
                    .addOnSuccessListener(q -> { if (!q.isEmpty()) Img.load(img, q.getDocuments().get(0).getString("imageBase64"), Img.THUMB_PX); });
        }
        return f;
    }

    public static TextView statusChip(Activity a, String status) {
        int[] c = Data.toolStatusColors(status);
        return Ui.chip(a, Data.toolStatusLabel(status), c[0], c[1], 0);
    }

    public static FrameLayout heart(Activity a, FirebaseFirestore db, DocumentSnapshot d, int size) {
        String uid = FirebaseAuth.getInstance().getUid();
        boolean on = Data.isFav(d.getId());
        final FrameLayout[] h = new FrameLayout[1];
        h[0] = Ui.iconButton(a, on ? R.drawable.ic_favorite : R.drawable.ic_favorite_border, on ? Ui.WARN : Ui.INK, Ui.WHITE, size,
                v -> Data.toggleFav(a, db, uid, d.getId(), Data.str(d, "toolName"),
                        added -> Ui.swapIcon(h[0], added ? R.drawable.ic_favorite : R.drawable.ic_favorite_border, added ? Ui.WARN : Ui.INK)));
        h[0].setContentDescription("المفضلة");
        return h[0];
    }

    public static int gridCardDp(Activity a) {
        return (Ui.screenDp(a) - 32 - 12) / 2;
    }

    public static LinearLayout grid(Activity a, FirebaseFirestore db, DocumentSnapshot d) {
        LinearLayout card = Ui.col(a);
        card.setClipToOutline(true);
        int imgH = Ui.clamp(Math.round(gridCardDp(a) * 0.78f), 110, 210);
        FrameLayout img = toolImage(a, db, d, 0, Ui.clamp(imgH / 3, 36, 60));
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(Ui.dp(a, 34), Ui.dp(a, 34), Gravity.TOP | Gravity.END);
        hp.setMargins(Ui.dp(a, 6), Ui.dp(a, 6), Ui.dp(a, 6), 0);
        img.addView(heart(a, db, d, 34), hp);
        card.addView(img, Ui.lp(a, Ui.MATCH, imgH));

        LinearLayout info = Ui.col(a);
        Ui.pad(info, 8, 6, 8, 8);
        info.addView(Ui.lines(Ui.text(a, Data.str(d, "toolName"), 15, Ui.INK, true), 1));
        info.addView(Ui.lines(Ui.text(a, Data.catName(d.getString("categoryId")) + " - " + Data.hoodName(d.getString("neighborhoodId")), 12, Ui.MUTED, false), 1));
        info.addView(Ui.text(a, Data.toolStatusLabel(d.getString("status")), 12, Data.toolStatusColors(d.getString("status"))[0], false));
        card.addView(info);
        Ui.tap(card, Ui.shape(a, Ui.WHITE, 10, Ui.LINE, 1), 10, v -> openTool(a, d.getId()));
        return card;
    }

    public static LinearLayout row(Activity a, FirebaseFirestore db, DocumentSnapshot d, boolean showStatus) {
        LinearLayout card = Ui.row(a);
        Ui.pad(card, 10);
        int img = Ui.clamp(Math.round(Ui.screenDp(a) * 0.22f), 76, 104);
        card.addView(toolImage(a, db, d, 4, 30), Ui.lp(a, img, img));
        LinearLayout info = Ui.col(a);
        LinearLayout top = Ui.row(a);
        top.addView(Ui.lines(Ui.text(a, Data.str(d, "toolName"), 16, Ui.INK, true), 1), Ui.lp(a, 0, Ui.WRAP, 1));
        if (Data.isFav(d.getId())) top.addView(Ui.iv(a, R.drawable.ic_favorite, Ui.WARN, 16), Ui.m(a, Ui.lp(a, 16, 16), 6, 0, 0, 0));
        info.addView(top);
        String desc = Data.str(d, "description");
        if (!desc.isEmpty())
            info.addView(Ui.lines(Ui.text(a, desc, 13, Ui.MUTED, false), 2), Ui.m(a, Ui.lp(a, Ui.MATCH, Ui.WRAP), 0, 2, 0, 0));
        LinearLayout meta = Ui.row(a);
        meta.addView(Ui.text(a, Data.catName(d.getString("categoryId")), 12, Ui.GREEN_DARK, false));
        if (showStatus) {
            TextView st = statusChip(a, d.getString("status"));
            meta.addView(st);
            ((LinearLayout.LayoutParams) st.getLayoutParams()).setMarginStart(Ui.dp(a, 6));
        } else {
            LinearLayout loc = Ui.row(a);
            loc.addView(Ui.lines(Ui.text(a, Data.hoodName(d.getString("neighborhoodId")), 12, Ui.MUTED, false), 1));
            meta.addView(loc, Ui.m(a, Ui.lp(a, 0, Ui.WRAP, 1), 8, 0, 0, 0));
        }
        info.addView(meta, Ui.m(a, Ui.lp(a, Ui.MATCH, Ui.WRAP), 0, 8, 0, 0));
        card.addView(info, Ui.m(a, Ui.lp(a, 0, Ui.WRAP, 1), 12, 0, 4, 0));
        Ui.tap(card, Ui.shape(a, Ui.WHITE, 10, Ui.LINE, 1), 10, v -> openTool(a, d.getId()));
        return card;
    }

    public static void fillGrid(Activity a, FirebaseFirestore db, LinearLayout into, List<DocumentSnapshot> tools) {
        for (int i = 0; i < tools.size(); i += 2) {
            LinearLayout r = Ui.row(a);
            r.setGravity(Gravity.TOP);
            r.addView(grid(a, db, tools.get(i)), Ui.m(a, Ui.lp(a, 0, Ui.WRAP, 1), 0, 0, 6, 0));
            if (i + 1 < tools.size()) r.addView(grid(a, db, tools.get(i + 1)), Ui.m(a, Ui.lp(a, 0, Ui.WRAP, 1), 6, 0, 0, 0));
            else { View sp = new View(a); r.addView(sp, Ui.m(a, Ui.lp(a, 0, 1, 1), 6, 0, 0, 0)); }
            into.addView(r, Ui.m(a, Ui.lp(a, Ui.MATCH, Ui.WRAP), 0, 0, 0, 12));
        }
    }

    public static List<String> sortedImages(List<DocumentSnapshot> docs) {
        List<DocumentSnapshot> l = new ArrayList<>(docs);
        java.util.Collections.sort(l, (x, y) -> Long.compare(Data.num(x.getLong("imageOrder")), Data.num(y.getLong("imageOrder"))));
        List<String> out = new ArrayList<>();
        for (DocumentSnapshot d : l) {
            String b = d.getString("imageBase64");
            if (b != null && !b.isEmpty()) out.add(b);
        }
        return out;
    }

    public static final class Gallery {
        public FrameLayout root;
        ImageView img;
        LinearLayout dots;
        TextView counter;
        List<String> images = new ArrayList<>();
        int index, px;
        boolean full;
        Runnable onTap;

        public void set(List<String> list) {
            images = list == null ? new ArrayList<>() : list;
            index = Math.min(index, Math.max(0, images.size() - 1));
            if (!images.isEmpty()) Img.load(img, images.get(index), px);
            paint();
        }

        void go(int dir) {
            if (images.size() < 2) return;
            int n = (index + dir + images.size()) % images.size();
            index = n;
            Activity a = (Activity) root.getContext();
            float off = Ui.dp(a, 40) * (dir > 0 ? 1 : -1);
            Img.load(img, images.get(index), px);
            paint();
        }

        void paint() {
            Activity a = (Activity) root.getContext();
            dots.removeAllViews();
            boolean many = images.size() > 1;
            dots.setVisibility(many ? View.VISIBLE : View.GONE);
            counter.setVisibility(many ? View.VISIBLE : View.GONE);
            counter.setText((index + 1) + " / " + images.size());
            if (!many) return;
            for (int i = 0; i < images.size(); i++) {
                View dot = new View(a);
                boolean on = i == index;
                dot.setBackground(Ui.shape(a, on ? Ui.WHITE : 0x88FFFFFF, 4));
                dots.addView(dot, Ui.m(a, Ui.lp(a, on ? 18 : 7, 7), 3, 0, 3, 0));
            }
        }
    }

    public static Gallery gallery(Activity a, boolean full) {
        Gallery g = new Gallery();
        g.full = full;
        g.px = full ? Img.FULL_PX : Ui.dp(a, 420);
        g.root = new FrameLayout(a);
        g.img = new ImageView(a);
        g.img.setScaleType(full ? ImageView.ScaleType.FIT_CENTER : ImageView.ScaleType.CENTER_CROP);
        g.root.addView(g.img, new FrameLayout.LayoutParams(-1, -1));

        g.dots = Ui.row(a);
        g.dots.setGravity(Gravity.CENTER);
        Ui.pad(g.dots, 8, 5, 8, 5);
        g.dots.setBackground(Ui.shape(a, 0x55000000, 12));
        FrameLayout.LayoutParams dp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dp.bottomMargin = Ui.dp(a, full ? 36 : 40);
        g.root.addView(g.dots, dp);

        g.counter = Ui.chip(a, "", Ui.WHITE, 0x77000000, R.drawable.ic_photo_library);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
        cp.setMargins(Ui.dp(a, 14), 0, Ui.dp(a, 14), Ui.dp(a, full ? 36 : 40));
        if (!full) g.root.addView(g.counter, cp);

        GestureDetector gd = new GestureDetector(a, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }

            @Override public boolean onSingleTapUp(MotionEvent e) {
                if (g.onTap != null) g.onTap.run();
                return true;
            }

            @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                if (Math.abs(dx) > Math.abs(dy) && g.root.getParent() != null)
                    g.root.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }

            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (Math.abs(vx) < Math.abs(vy) || Math.abs(vx) < 500) return false;
                g.go(vx > 0 ? 1 : -1);
                return true;
            }
        });
        g.root.setOnTouchListener((v, e) -> gd.onTouchEvent(e));
        g.paint();
        return g;
    }

    public static void viewer(Activity a, List<String> images, int start) {
        if (images == null || images.isEmpty()) return;
        Dialog d = new Dialog(a, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout root = new FrameLayout(a);
        root.setBackgroundColor(Color.BLACK);
        Gallery g = gallery(a, true);
        g.index = Math.max(0, Math.min(start, images.size() - 1));
        root.addView(g.root, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout close = Ui.iconButton(a, R.drawable.ic_close, Ui.WHITE, 0x44FFFFFF, 44, v -> d.dismiss());
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(Ui.dp(a, 44), Ui.dp(a, 44), Gravity.TOP | Gravity.START);
        clp.setMargins(Ui.dp(a, 16), Ui.dp(a, 40), Ui.dp(a, 16), 0);
        root.addView(close, clp);
        TextView hint = Ui.center(Ui.text(a, images.size() > 1 ? "اسحب للتنقل بين الصور" : "", 12, 0x99FFFFFF, false));
        FrameLayout.LayoutParams hl = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        hl.bottomMargin = Ui.dp(a, 12);
        root.addView(hint, hl);
        g.onTap = d::dismiss;
        d.setContentView(root);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            w.getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }
        d.show();
        g.set(images);
    }
}
