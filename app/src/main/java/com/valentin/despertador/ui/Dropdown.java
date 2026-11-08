package com.valentin.despertador.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.widget.CompoundButtonCompat;

import com.valentin.despertador.R;

import java.util.List;

/** Menú que se abre debajo de un campo (música y voces). Con casillas si se pueden elegir varias. */
public final class Dropdown {
    public static final class Item {
        public final String key;
        public final String text;
        public final String sub;
        public final boolean header;
        public boolean checked;

        private Item(String key, String text, String sub, boolean header, boolean checked) {
            this.key = key;
            this.text = text;
            this.sub = sub;
            this.header = header;
            this.checked = checked;
        }

        public static Item header(String text) {
            return new Item(null, text, null, true, false);
        }

        public static Item option(String key, String text, String sub, boolean checked) {
            return new Item(key, text, sub, false, checked);
        }
    }

    public interface OnPick {
        /** Con varias a la vez, aquí se cambian los "checked" de la lista y el menú se redibuja. */
        void picked(Item item, List<Item> all);
    }

    private Dropdown() {
    }

    public static void show(View anchor, List<Item> items, boolean multi, OnPick onPick) {
        Context c = anchor.getContext();
        Palette p = Palette.current();
        LinearLayout column = new LinearLayout(c);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(0, Ui.dp(c, 6), 0, Ui.dp(c, 6));
        ScrollView scroll = new ScrollView(c);
        scroll.addView(column);
        scroll.setBackground(Ui.rounded(c, p.card, 16, p.border));
        // Que las filas no se salgan de las esquinas redondeadas
        scroll.setClipToOutline(true);

        int width = Ui.dp(c, 270);
        PopupWindow popup = new PopupWindow(scroll, width, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(Ui.dp(c, 8));
        Runnable[] render = new Runnable[1];
        render[0] = () -> {
            column.removeAllViews();
            for (Item item : items) column.addView(row(c, p, item, multi, v -> {
                onPick.picked(item, items);
                if (multi) render[0].run();
                else popup.dismiss();
            }));
        };
        render[0].run();

        // Como mucho 400 dp de alto: si hay más, se desplaza
        column.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED);
        popup.setHeight(Math.min(column.getMeasuredHeight(), Ui.dp(c, 400)));
        popup.showAsDropDown(anchor, Ui.dp(c, 0), 0, Gravity.START);
    }

    private static View row(Context c, Palette p, Item item, boolean multi, View.OnClickListener click) {
        if (item.header) {
            TextView t = new TextView(c);
            t.setText(item.text.toUpperCase());
            t.setTextColor(p.accent);
            t.setTextSize(12);
            t.setLetterSpacing(0.12f);
            t.setPadding(Ui.dp(c, 16), Ui.dp(c, 14), Ui.dp(c, 16), Ui.dp(c, 6));
            return t;
        }
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(c, 16), Ui.dp(c, 10), Ui.dp(c, 16), Ui.dp(c, 10));
        row.setMinimumHeight(Ui.dp(c, 48));
        row.setOnClickListener(click);
        row.setBackgroundResource(android.R.drawable.list_selector_background);
        if (multi) {
            AppCompatCheckBox box = new AppCompatCheckBox(c);
            box.setChecked(item.checked);
            box.setClickable(false);
            box.setFocusable(false);
            CompoundButtonCompat.setButtonTintList(box, Ui.tint(item.checked ? p.accent : Palette.TEXT_DIM));
            row.addView(box);
        }
        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(multi ? Ui.dp(c, 8) : 0, 0, 0, 0);
        TextView t = new TextView(c);
        t.setText(item.text);
        t.setTextSize(16);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(item.checked && !multi ? p.accent : Palette.TEXT_MAIN);
        texts.addView(t);
        if (item.sub != null) {
            TextView s = new TextView(c);
            s.setText(item.sub);
            s.setTextSize(12);
            s.setTextColor(Palette.TEXT_FAINT);
            texts.addView(s);
        }
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (!multi && item.checked) {
            ImageView check = new ImageView(c);
            check.setImageResource(R.drawable.ic_check);
            check.setImageTintList(Ui.tint(p.accent));
            row.addView(check, new LinearLayout.LayoutParams(Ui.dp(c, 24), Ui.dp(c, 24)));
        }
        return row;
    }
}
