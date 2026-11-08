package com.valentin.despertador.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/** Ayudas para dibujar fondos redondeados y ajustar la pantalla. */
public final class Ui {
    private Ui() {
    }

    public static int dp(Context c, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, c.getResources().getDisplayMetrics()));
    }

    /** Rectángulo redondeado con borde opcional (border = 0 sin borde). */
    public static GradientDrawable rounded(Context c, int color, float radiusDp, int border) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        if (border != 0) d.setStroke(dp(c, 1), border);
        return d;
    }

    /** Forma de píldora (bordes del todo redondos). */
    public static GradientDrawable pill(Context c, int color, int border) {
        return rounded(c, color, 999, border);
    }

    /** Pantalla de borde a borde con iconos claros arriba y abajo. */
    @SuppressWarnings("deprecation")
    public static void edgeToEdge(Window window) {
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.setNavigationBarContrastEnforced(false);
        WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightStatusBars(false);
        WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightNavigationBars(false);
    }

    /** Deja hueco para la barra de estado, la de navegación y el teclado. */
    public static void padForSystemBars(View root) {
        int l = root.getPaddingLeft(), t = root.getPaddingTop(), r = root.getPaddingRight(), b = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(l + bars.left, t + bars.top, r + bars.right, b + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /** Cursor del color del tema en los campos de texto. */
    public static void tintCursor(EditText field, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            GradientDrawable cursor = new GradientDrawable();
            cursor.setColor(color);
            cursor.setSize(dp(field.getContext(), 2), 1);
            field.setTextCursorDrawable(cursor);
        }
        field.setHighlightColor((color & 0x00FFFFFF) | 0x55000000);
    }

    public static ColorStateList tint(int color) {
        return ColorStateList.valueOf(color);
    }

    public static void setTextColor(TextView v, int color) {
        v.setTextColor(color);
    }
}
