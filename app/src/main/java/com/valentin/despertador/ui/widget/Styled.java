package com.valentin.despertador.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatTextView;

import com.valentin.despertador.ui.Palette;
import com.valentin.despertador.ui.Ui;

/** Piezas con los colores del diseño: se usan directamente en los XML. */
public final class Styled {
    private Styled() {
    }

    /** Tarjeta redondeada con borde. */
    public static class Card extends LinearLayout {
        public Card(Context context, AttributeSet attrs) {
            super(context, attrs);
            Palette p = Palette.current();
            setBackground(Ui.rounded(context, p.card, 28, p.border));
        }
    }

    /** Botón principal: píldora del color del diseño. */
    public static class AccentButton extends AppCompatTextView {
        public AccentButton(Context context, AttributeSet attrs) {
            super(context, attrs);
            Palette p = Palette.current();
            setBackground(Ui.pill(context, p.accent, 0));
            setTextColor(p.onAccent());
            setGravity(Gravity.CENTER);
            setClickable(true);
            setFocusable(true);
        }
    }

    /** Botón secundario: píldora oscura con borde. */
    public static class GhostButton extends AppCompatTextView {
        public GhostButton(Context context, AttributeSet attrs) {
            super(context, attrs);
            Palette p = Palette.current();
            setBackground(Ui.pill(context, p.card, p.border));
            setTextColor(Palette.TEXT_MAIN);
            setGravity(Gravity.CENTER);
            setClickable(true);
            setFocusable(true);
        }
    }

    /** Texto en el color del diseño (títulos de las tarjetas). */
    public static class AccentText extends AppCompatTextView {
        public AccentText(Context context, AttributeSet attrs) {
            super(context, attrs);
            setTextColor(Palette.current().accent);
        }
    }
}
