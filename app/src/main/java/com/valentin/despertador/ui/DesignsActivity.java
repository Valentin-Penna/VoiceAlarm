package com.valentin.despertador.ui;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.valentin.despertador.R;
import com.valentin.despertador.data.AlarmStore;
import com.valentin.despertador.data.AppSettings;

/** Elegir el diseño (tema de color). Cada fila se pinta con los colores de su propio diseño. */
public class DesignsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disenos);
        Ui.padForSystemBars(findViewById(R.id.raiz));
        findViewById(R.id.barra).findViewById(R.id.volver).setOnClickListener(v -> finish());
        LinearLayout list = findViewById(R.id.lista);
        String current = AlarmStore.settings().theme;
        for (Palette p : Palette.ALL) list.addView(row(p, p.id.equals(current)));
    }

    private View row(Palette p, boolean selected) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(this, 20), Ui.dp(this, 18), Ui.dp(this, 20), Ui.dp(this, 18));
        GradientDrawable bg = p.background();
        bg.setCornerRadius(Ui.dp(this, 28));
        bg.setStroke(Ui.dp(this, selected ? 2 : 1), selected ? p.accent : p.border);
        row.setBackground(bg);
        row.setOnClickListener(v -> {
            AppSettings s = AlarmStore.settings();
            s.theme = p.id;
            AlarmStore.saveSettings(s);
            recreate();
        });

        View dot = new View(this);
        dot.setBackground(Ui.pill(this, p.accent, 0));
        row.addView(dot, new LinearLayout.LayoutParams(Ui.dp(this, 34), Ui.dp(this, 34)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = Ui.dp(this, 16);
        TextView name = new TextView(this);
        name.setText(p.name);
        name.setTextSize(19);
        name.setTextColor(Palette.TEXT_MAIN);
        texts.addView(name);
        // Barrita de muestra con el color del diseño
        FrameLayout bar = new FrameLayout(this);
        bar.setBackground(Ui.pill(this, p.card, 0));
        View fill = new View(this);
        fill.setBackground(Ui.pill(this, p.accent, 0));
        bar.addView(fill, new FrameLayout.LayoutParams(Ui.dp(this, 60), Ui.dp(this, 8)));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(Ui.dp(this, 110), Ui.dp(this, 8));
        blp.topMargin = Ui.dp(this, 8);
        texts.addView(bar, blp);
        row.addView(texts, tlp);

        if (selected) {
            ImageView check = new ImageView(this);
            check.setImageResource(R.drawable.ic_check);
            check.setImageTintList(Ui.tint(p.accent));
            row.addView(check, new LinearLayout.LayoutParams(Ui.dp(this, 24), Ui.dp(this, 24)));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(this, 14);
        row.setLayoutParams(lp);
        return row;
    }
}
