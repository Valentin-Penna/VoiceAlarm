package com.valentin.despertador.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import androidx.core.graphics.ColorUtils;

import com.valentin.despertador.data.AlarmStore;

import java.util.Arrays;
import java.util.List;

/** Un diseño (tema de color) de la app. */
public final class Palette {
    public static final int TEXT_MAIN = 0xFFF2F4F8;
    public static final int TEXT_DIM = 0xFFB2B9C8;
    public static final int TEXT_FAINT = 0xFF6F778A;
    /** Línea de debajo de los campos */
    public static final int LINE = 0x996F778A;

    public static final List<Palette> ALL = Arrays.asList(
            new Palette("cian", "Cian", 0xFF00D4FF, 0xFF151B28, 0xFF0A0D14, 0xFF1E2638, 0xFF3A4357),
            new Palette("violeta", "Violeta", 0xFFB388FF, 0xFF1A1526, 0xFF0C0A12, 0xFF261F38, 0xFF463C5E),
            new Palette("esmeralda", "Esmeralda", 0xFF2EE6A6, 0xFF121E1B, 0xFF08100E, 0xFF1B2C28, 0xFF36524B),
            new Palette("ambar", "Ámbar", 0xFFFFB547, 0xFF1E1810, 0xFF0F0C08, 0xFF2C251A, 0xFF544734),
            new Palette("rosa", "Rosa", 0xFFFF6FA8, 0xFF20131C, 0xFF10090E, 0xFF2F1E29, 0xFF583B4D),
            new Palette("azul", "Azul", 0xFF5B8CFF, 0xFF131829, 0xFF090B16, 0xFF1D2442, 0xFF3B456B),
            new Palette("rojo", "Rojo", 0xFFFF5A5F, 0xFF201314, 0xFF100909, 0xFF2F1E20, 0xFF583A3D),
            new Palette("grafito", "Grafito", 0xFFE6E6E6, 0xFF1A1A1A, 0xFF0B0B0B, 0xFF252525, 0xFF444444)
    );

    public final String id;
    public final String name;
    public final int accent;
    public final int bgTop;
    public final int bgBottom;
    public final int card;
    public final int border;

    private Palette(String id, String name, int accent, int bgTop, int bgBottom, int card, int border) {
        this.id = id;
        this.name = name;
        this.accent = accent;
        this.bgTop = bgTop;
        this.bgBottom = bgBottom;
        this.card = card;
        this.border = border;
    }

    /** Texto sobre el color principal: blanco, o oscuro si el color es muy claro. */
    public int onAccent() {
        return ColorUtils.calculateLuminance(accent) > 0.6 ? 0xFF10141C : Color.WHITE;
    }

    public GradientDrawable background() {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{bgTop, bgBottom});
    }

    public static Palette of(String id) {
        for (Palette p : ALL) if (p.id.equals(id)) return p;
        return ALL.get(0);
    }

    public static Palette current() {
        return of(AlarmStore.settings().theme);
    }
}
