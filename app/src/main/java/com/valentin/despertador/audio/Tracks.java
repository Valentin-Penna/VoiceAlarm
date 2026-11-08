package com.valentin.despertador.audio;

import android.content.Context;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;

import com.valentin.despertador.R;
import com.valentin.despertador.util.Signal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Músicas de la alarma:
 * - Arriba, canciones tranquilas sin letra incluidas en la app (CC0 de FreePD).
 * - Debajo, los despertadores (tonos de alarma) del propio móvil.
 */
public final class Tracks {
    /** Un sonido que se puede elegir. */
    public static final class Track {
        public final String id;
        public final String title;

        Track(String id, String title) {
            this.id = id;
            this.title = title;
        }
    }

    /** El tono de alarma que el móvil tiene puesto por defecto. */
    public static final String SYSTEM = "sistema";
    /** Música que viene de serie. */
    public static final String DEFAULT = "raw:nostalgic_piano";

    /** Canciones incluidas. El id es "raw:<nombre del archivo>" (no cambia entre versiones). */
    public static final List<Track> SONGS = Collections.unmodifiableList(new ArrayList<Track>() {{
        // La referencia a R.raw evita que el APK final quite el archivo al optimizar
        add(song("A Waltz For Naseem", "waltz_for_naseem", R.raw.waltz_for_naseem));
        add(song("Amazing Grace", "amazing_grace", R.raw.amazing_grace));
        add(song("Aquarium", "aquarium", R.raw.aquarium));
        add(song("Horizon Flare", "horizon_flare", R.raw.horizon_flare));
        add(song("Infinite Peace", "infinite_peace", R.raw.infinite_peace));
        add(song("Infinite Wonder", "infinite_wonder", R.raw.infinite_wonder));
        add(song("Isolation Waltz", "isolation_waltz", R.raw.isolation_waltz));
        add(song("Kalimba Relaxation", "kalimba_relaxation", R.raw.kalimba_relaxation));
        add(song("Mana Two", "mana_two", R.raw.mana_two));
        add(song("Nostalgic Piano", "nostalgic_piano", R.raw.nostalgic_piano));
        add(song("Painting Room", "painting_room", R.raw.painting_room));
        add(song("Parhelion", "parhelion", R.raw.parhelion));
        add(song("Piano Magic Motive", "piano_magic_motive", R.raw.piano_magic_motive));
        add(song("Travelers Notebook", "travelers_notebook", R.raw.travelers_notebook));
        add(song("Winter", "winter", R.raw.winter));
    }});

    /** Avisa cuando ya se han leído los despertadores del móvil. */
    public static final Signal changed = new Signal();

    private static volatile List<Track> phoneTones = new ArrayList<>();
    private static volatile Uri systemDefault;
    private static String packageName = "com.valentin.despertador";

    private Tracks() {
    }

    private static Track song(String title, String file, int res) {
        return new Track("raw:" + file, title);
    }

    /** Despertadores del móvil, en orden alfabético. El id es la dirección (URI) del tono. */
    public static List<Track> phoneTones() {
        return phoneTones;
    }

    public static void load(Context context) {
        Context app = context.getApplicationContext();
        packageName = app.getPackageName();
        new Thread(() -> {
            Uri def = RingtoneManager.getActualDefaultRingtoneUri(app, RingtoneManager.TYPE_ALARM);
            systemDefault = def != null ? def : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            List<Track> items = new ArrayList<>();
            Set<String> titles = new HashSet<>();
            try {
                RingtoneManager rm = new RingtoneManager(app);
                rm.setType(RingtoneManager.TYPE_ALARM);
                try (Cursor cursor = rm.getCursor()) {
                    while (cursor.moveToNext()) {
                        String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
                        if (titles.add(title)) {
                            items.add(new Track(rm.getRingtoneUri(cursor.getPosition()).toString(), title));
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            items.sort((a, b) -> a.title.compareToIgnoreCase(b.title));
            phoneTones = items;
            changed.emit();
        }).start();
    }

    /** Dirección del sonido que hay que reproducir. */
    public static Uri uri(String id) {
        if (id != null && id.startsWith("raw:")) {
            return Uri.parse("android.resource://" + packageName + "/raw/" + id.substring(4));
        }
        if (id != null && id.startsWith("content:")) return Uri.parse(id);
        if (SYSTEM.equals(id)) {
            return systemDefault != null ? systemDefault : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }
        // Algo que ya no existe (p. ej. una canción quitada): la de serie
        return uri(DEFAULT);
    }

    /** Nombre que se enseña en la app. */
    public static String title(String id) {
        for (Track t : SONGS) if (same(t.id, id)) return t.title;
        for (Track t : phoneTones) if (same(t.id, id)) return t.title;
        return "Tono del móvil";
    }

    /** ¿Son el mismo sonido? (las direcciones de los tonos pueden llevar datos extra detrás del "?") */
    public static boolean same(String a, String b) {
        Uri ua = uri(a);
        Uri ub = uri(b);
        if (ua == null || ub == null) return false;
        return ua.buildUpon().clearQuery().build().equals(ub.buildUpon().clearQuery().build());
    }
}
