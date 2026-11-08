package com.valentin.despertador.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Textos de horas, días y duraciones. */
public final class Format {
    private static final String[] SHORT_DAYS = {"", "lun", "mar", "mié", "jue", "vie", "sáb", "dom"};
    private static final Set<Integer> WEEKDAYS = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-ES"));

    private Format() {
    }

    public static String time(int hour, int minute) {
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    public static String days(Set<Integer> days) {
        if (days.isEmpty()) return "Una vez";
        if (days.size() == 7) return "Diariamente";
        if (days.equals(WEEKDAYS)) return "Lun a Vie";
        StringBuilder sb = new StringBuilder();
        for (int d : days) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(SHORT_DAYS[d]);
        }
        return sb.toString();
    }

    /** 510 → "8h 30min" */
    public static String duration(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        if (h == 0) return m + "min";
        if (m == 0) return h + "h";
        return h + "h " + m + "min";
    }

    private static ZonedDateTime at(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault());
    }

    public static String timeOf(long millis) {
        ZonedDateTime t = at(millis);
        return time(t.getHour(), t.getMinute());
    }

    public static String dayOf(long millis) {
        return SHORT_DAYS[at(millis).getDayOfWeek().getValue()];
    }

    /** "sábado, 7 de noviembre" */
    public static String dateOf(long millis) {
        return at(millis).format(DATE);
    }

    public static String times(int n) {
        return n == 1 ? "1 vez" : n + " veces";
    }
}
