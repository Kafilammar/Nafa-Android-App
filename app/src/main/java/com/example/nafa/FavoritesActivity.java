package com.example.nafa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends BaseActivity {
    private LinearLayout list;
    private TextView sub;

    @Override
    protected void onReady(Bundle saved) {
        Ui.Screen s = Ui.screen(this, "المفضلة", true);
        sub = Ui.text(this, "", 13, Ui.MUTED, false);
        s.body.addView(sub, Ui.m(this, Ui.lp(this, Ui.MATCH, Ui.WRAP), 4, 0, 4, 12));
        list = Ui.col(this);
        s.body.addView(list, Ui.lp(this, Ui.MATCH, Ui.WRAP));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ready) {
            list.removeAllViews();
            list.addView(Ui.loading(this));
            Data.loadFavs(db, uid, this::load);
        }
    }

    private void load() {
        List<String> ids = Data.favIds();
        if (ids.size() > 60) ids = ids.subList(0, 60);
        List<Task<DocumentSnapshot>> gets = new ArrayList<>();
        for (String id : ids) gets.add(db.collection("tools").document(id).get());
        final List<String> idList = ids;
        Tasks.whenAllComplete(gets).addOnCompleteListener(t -> {
            List<DocumentSnapshot> tools = new ArrayList<>();
            for (int i = 0; i < gets.size(); i++) {
                Task<DocumentSnapshot> g = gets.get(i);
                if (!g.isSuccessful() || g.getResult() == null) continue;
                DocumentSnapshot d = g.getResult();
                if (d.exists()) tools.add(d);
                else Data.forgetFav(db, uid, idList.get(i));
            }
            render(tools);
        });
    }

    private void render(List<DocumentSnapshot> tools) {
        list.removeAllViews();
        sub.setText(tools.isEmpty() ? "" : Data.count(tools.size(), "أداة واحدة محفوظة", "أداتان محفوظتان", "أدوات محفوظة", "أداة محفوظة"));
        if (tools.isEmpty()) {
            list.addView(Ui.empty(this, R.drawable.ic_favorite_border, "لا توجد أدوات في المفضلة",
                    "اضغط على في أي أداة لحفظها هنا والرجوع إليها بسرعة"));
            TextView b = Ui.button(this, "تصفح الأدوات", Ui.SOFT, R.drawable.ic_search);
            b.setOnClickListener(v -> {
                Intent i = new Intent(this, MainActivity.class);
                i.putExtra("tab", MainActivity.SEARCH);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
            });
            list.addView(b, Ui.m(this, Ui.lp(this, Ui.MATCH, 50), 24, 0, 24, 0));
            return;
        }
        Cards.fillGrid(this, db, list, tools);
    }
}
