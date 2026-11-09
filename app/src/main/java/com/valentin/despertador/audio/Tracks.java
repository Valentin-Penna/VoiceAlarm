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
 * - Arriba, música clásica tranquila de despertar (grabaciones de dominio público o CC0 de Wikimedia Commons).
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
    public static final String DEFAULT = "raw:grieg_la_manana";

    /** Canciones incluidas. El id es "raw:<nombre del archivo>" (no cambia entre versiones). */
    public static final List<Track> SONGS = Collections.unmodifiableList(new ArrayList<Track>() {{
        // La referencia a R.raw evita que el APK final quite el archivo al optimizar
        add(song("La mañana (Grieg)", "grieg_la_manana", R.raw.grieg_la_manana));
        add(song("Aire en sol (Bach)", "bach_aire", R.raw.bach_aire));
        add(song("Clair de lune (Debussy)", "debussy_clair_de_lune", R.raw.debussy_clair_de_lune));
        add(song("El Danubio azul (Strauss)", "strauss_danubio_azul", R.raw.strauss_danubio_azul));
        add(song("Gymnopédie nº 1 (Satie)", "satie_gymnopedie", R.raw.satie_gymnopedie));
        add(song("Nocturno op. 9 nº 2 (Chopin)", "chopin_nocturno", R.raw.chopin_nocturno));
        add(song("Pequeña serenata nocturna (Mozart)", "mozart_serenata", R.raw.mozart_serenata));
        add(song("Vals del Lago de los cisnes (Chaikovski)", "chaikovski_lago_cisnes", R.raw.chaikovski_lago_cisnes));
        // Música de meditación: FreePD (CC0), Kevin MacLeod (CC BY 3.0) y Joey Bond (CC0)
        add(song("Deep Relaxation", "deep_relaxation", R.raw.deep_relaxation));
        add(song("Elf Meditation", "elf_meditation", R.raw.elf_meditation));
        add(song("Ancient Winds", "ancient_winds", R.raw.ancient_winds));
        add(song("Wind of the Rainforest", "wind_of_the_rainforest", R.raw.wind_of_the_rainforest));
        add(song("Ebbs and Flows", "ebbs_and_flows", R.raw.ebbs_and_flows));
        add(song("River Meditation", "river_meditation", R.raw.river_meditation));
        add(song("Meditation Impromptu 1", "meditation_impromptu_1", R.raw.meditation_impromptu_1));
        add(song("Meditation Impromptu 2", "meditation_impromptu_2", R.raw.meditation_impromptu_2));
        add(song("Meditation Impromptu 3", "meditation_impromptu_3", R.raw.meditation_impromptu_3));
        add(song("Organic Meditations 1", "organic_meditations_1", R.raw.organic_meditations_1));
        add(song("Organic Meditations 3", "organic_meditations_3", R.raw.organic_meditations_3));
        add(song("Relaxing Piano Music", "relaxing_piano_music", R.raw.relaxing_piano_music));
        add(song("Jade Pillow", "jade_pillow", R.raw.jade_pillow));
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
