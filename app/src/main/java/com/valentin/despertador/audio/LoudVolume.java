package com.valentin.despertador.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.util.Log;

/**
 * Pone el volumen de alarma del móvil al máximo (y sin silenciar) mientras suena,
 * para que la alarma se oiga aunque el usuario lo tenga bajado o en silencio.
 * Lo fuerte o flojo que suena lo decide el volumen de la propia app.
 * Al terminar deja el volumen como estaba. El valor anterior se guarda en disco
 * por si la app se cierra de golpe mientras suena.
 */
public final class LoudVolume {
    private static final String PREFS = "volumen";
    private static final String KEY = "alarma_antes";

    /** Cuántos sonidos de alarma lo están usando (alarma y "Probar sonido" a la vez). */
    private static int users = 0;

    private LoudVolume() {
    }

    /** Guarda el volumen del usuario (si no estaba guardado ya) y lo sube al máximo. */
    public static synchronized void raise(Context context) {
        AudioManager am = context.getSystemService(AudioManager.class);
        if (am == null) return;
        users++;
        SharedPreferences prefs = prefs(context);
        if (!prefs.contains(KEY)) {
            prefs.edit().putInt(KEY, am.getStreamVolume(AudioManager.STREAM_ALARM)).commit();
        }
        keepUp(context);
    }

    /** Si el volumen de alarma se ha bajado o silenciado, lo vuelve a subir. */
    public static void keepUp(Context context) {
        AudioManager am = context.getSystemService(AudioManager.class);
        if (am == null) return;
        try {
            if (am.isStreamMute(AudioManager.STREAM_ALARM)) {
                am.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_UNMUTE, 0);
            }
            int max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            if (am.getStreamVolume(AudioManager.STREAM_ALARM) < max) {
                am.setStreamVolume(AudioManager.STREAM_ALARM, max, 0);
            }
        } catch (SecurityException e) {
            Log.w("Despertador", "No se pudo subir el volumen de alarma", e);
        }
    }

    /** Devuelve el volumen de alarma al que tenía el usuario antes de sonar. */
    public static synchronized void restore(Context context) {
        if (users > 0) users--;
        if (users > 0) return;
        SharedPreferences prefs = prefs(context);
        int before = prefs.getInt(KEY, -1);
        if (before < 0) return;
        AudioManager am = context.getSystemService(AudioManager.class);
        if (am != null) {
            try {
                am.setStreamVolume(AudioManager.STREAM_ALARM, before, 0);
            } catch (SecurityException e) {
                Log.w("Despertador", "No se pudo devolver el volumen de alarma", e);
            }
        }
        prefs.edit().remove(KEY).commit();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
