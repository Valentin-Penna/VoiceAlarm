package com.valentin.despertador.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.valentin.despertador.R;

/** Ventanas flotantes con el fondo oscurecido. */
public final class Dialogs {
    private Dialogs() {
    }

    /** Ventana con el contenido que se le pase, redondeada y con el degradado del diseño. */
    public static Dialog panel(Context c, View content) {
        Palette p = Palette.current();
        Dialog dialog = new Dialog(c);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{p.card, p.bgTop});
        bg.setCornerRadius(Ui.dp(c, 30));
        content.setBackground(bg);
        int pad = Ui.dp(c, 22);
        content.setPadding(pad, pad, pad, pad);
        FrameLayout wrapper = new FrameLayout(c);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = lp.rightMargin = Ui.dp(c, 10);
        wrapper.addView(content, lp);
        dialog.setContentView(wrapper);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }

    /** Pregunta con "Cancelar" y un botón para confirmar. */
    public static Dialog confirm(Context c, String title, String text, String confirm, Runnable onConfirm) {
        View view = LayoutInflater.from(c).inflate(R.layout.dialog_confirmar, null, false);
        ((TextView) view.findViewById(R.id.titulo)).setText(title);
        ((TextView) view.findViewById(R.id.texto)).setText(text);
        TextView ok = view.findViewById(R.id.confirmar);
        ok.setText(confirm);
        Dialog dialog = panel(c, view);
        view.findViewById(R.id.cancelar).setOnClickListener(v -> dialog.dismiss());
        ok.setOnClickListener(v -> {
            dialog.dismiss();
            onConfirm.run();
        });
        dialog.show();
        return dialog;
    }
}
