package com.valentin.despertador.audio;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import com.valentin.despertador.data.Alarm;
import com.valentin.despertador.data.AlarmStore;
import com.valentin.despertador.data.AppSettings;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Graba de antemano el mensaje de cada alarma con cada voz elegida.
 * Así, al sonar, la voz sale justo al segundo sin esperar a que arranque el motor de voz.
 */
public final class VoiceCache {
    public static final String DEFAULT = "default";
    /** Frase de "Probar sonido" en Configuración */
    public static final String PREVIEW = "Probando sonido. Así sonará tu alarma.";

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static boolean busy = false;
    private static boolean again = false;

    private VoiceCache() {
    }

    public static List<String> ids(List<String> voiceIds) {
        return voiceIds == null || voiceIds.isEmpty() ? Collections.singletonList(DEFAULT) : voiceIds;
    }

    public static File file(Context context, String voiceId, String message) {
        String name;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1")
                    .digest((voiceId + "|" + message.trim()).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            name = sb.toString();
        } catch (Exception e) {
            name = Integer.toHexString((voiceId + "|" + message.trim()).hashCode());
        }
        return new File(dir(context), name + ".wav");
    }

    public static boolean isReady(Context context, List<String> voiceIds, String message) {
        for (String id : ids(voiceIds)) if (file(context, id, message).length() == 0) return false;
        return true;
    }

    private static File dir(Context context) {
        File d = new File(context.getFilesDir(), "voz");
        d.mkdirs();
        return d;
    }

    /** Graba lo que falte y borra las grabaciones que ya no usa ninguna alarma. */
    public static void prepareAll(Context context) {
        Context app = context.getApplicationContext();
        handler.post(() -> {
            if (busy) {
                again = true;
                return;
            }
            AppSettings settings = AlarmStore.settings();
            Set<String[]> needed = new LinkedHashSet<>();
            Set<String> seen = new HashSet<>();
            for (Alarm a : AlarmStore.alarms()) {
                if (a.message.trim().isEmpty()) continue;
                for (String id : ids(a.voices != null ? a.voices : settings.voices)) {
                    if (seen.add(id + "|" + a.message.trim())) needed.add(new String[]{id, a.message.trim()});
                }
            }
            // La frase de prueba, con todas las voces (para probar sin guardar antes)
            List<String> previewVoices = new ArrayList<>();
            for (Voices.VoiceOption o : Voices.list()) previewVoices.add(o.id);
            previewVoices.add(DEFAULT);
            for (String id : previewVoices) {
                if (seen.add(id + "|" + PREVIEW)) needed.add(new String[]{id, PREVIEW});
            }

            Set<String> keep = new HashSet<>();
            for (String[] n : needed) keep.add(file(app, n[0], n[1]).getName());
            File[] files = dir(app).listFiles();
            if (files != null) for (File f : files) if (!keep.contains(f.getName())) f.delete();

            ArrayDeque<String[]> missing = new ArrayDeque<>();
            for (String[] n : needed) if (file(app, n[0], n[1]).length() == 0) missing.add(n);
            if (!missing.isEmpty()) {
                busy = true;
                new Synthesizer(app, missing).start();
            }
        });
    }

    /** Graba los mensajes uno detrás de otro con el motor de voz. */
    private static final class Synthesizer {
        private final Context app;
        private final ArrayDeque<String[]> queue;
        private TextToSpeech tts;
        private File target;
        private File tmp;

        Synthesizer(Context app, ArrayDeque<String[]> queue) {
            this.app = app;
            this.queue = queue;
        }

        void start() {
            tts = new TextToSpeech(app, status -> handler.post(() -> {
                if (status == TextToSpeech.SUCCESS) next();
                else finish();
            }));
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                }

                @Override
                public void onDone(String utteranceId) {
                    handler.post(() -> {
                        if (tmp != null) tmp.renameTo(target);
                        next();
                    });
                }

                @Override
                @SuppressWarnings("deprecation")
                public void onError(String utteranceId) {
                    handler.post(() -> {
                        if (tmp != null) tmp.delete();
                        next();
                    });
                }
            });
        }

        private void next() {
            while (true) {
                String[] job = queue.poll();
                if (job == null) {
                    finish();
                    return;
                }
                String voiceId = job[0];
                String message = job[1];
                if (DEFAULT.equals(voiceId)) {
                    tts.setLanguage(Locale.forLanguageTag("es-ES"));
                } else {
                    Voice voice = Voices.find(tts, voiceId);
                    if (voice == null) continue;
                    tts.setVoice(voice);
                }
                target = file(app, voiceId, message);
                tmp = new File(target.getPath() + ".tmp");
                if (tts.synthesizeToFile(message, new Bundle(), tmp, "cache") == TextToSpeech.SUCCESS) return;
            }
        }

        private void finish() {
            if (tts != null) tts.shutdown();
            tts = null;
            busy = false;
            if (again) {
                again = false;
                prepareAll(app);
            }
        }
    }
}
