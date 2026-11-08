package com.valentin.despertador.audio;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import com.valentin.despertador.util.Signal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Voces en español del motor de voz del móvil, numeradas como "Voz I", "Voz II"…
 * Solo las que funcionan sin internet (la alarma tiene que hablar aunque no haya red).
 */
public final class Voices {
    public static final class VoiceOption {
        public final String id;
        public final String label;
        public final String region;

        VoiceOption(String id, String label, String region) {
            this.id = id;
            this.label = label;
            this.region = region;
        }
    }

    /** Avisa cuando ya está la lista de voces. */
    public static final Signal changed = new Signal();

    private static volatile List<VoiceOption> list = new ArrayList<>();
    private static TextToSpeech loader;

    private Voices() {
    }

    public static List<VoiceOption> list() {
        return list;
    }

    public static synchronized void load(Context context) {
        if (loader != null || !list.isEmpty()) return;
        Context app = context.getApplicationContext();
        final TextToSpeech[] holder = new TextToSpeech[1];
        holder[0] = new TextToSpeech(app, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale spanish = Locale.forLanguageTag("es-ES");
                List<VoiceOption> options = new ArrayList<>();
                List<Voice> voices = sorted(holder[0]);
                for (int i = 0; i < voices.size(); i++) {
                    Voice v = voices.get(i);
                    options.add(new VoiceOption(v.getName(), "Voz " + roman(i + 1), v.getLocale().getDisplayCountry(spanish)));
                }
                list = options;
                changed.emit();
                VoiceCache.prepareAll(app);
            }
            holder[0].shutdown();
            synchronized (Voices.class) {
                loader = null;
            }
        });
        loader = holder[0];
    }

    /** Las voces de este motor en el mismo orden que la lista (para que "Voz II" sea siempre la misma). */
    public static List<Voice> sorted(TextToSpeech tts) {
        List<Voice> spanish = new ArrayList<>();
        try {
            Set<Voice> all = tts.getVoices();
            if (all != null) for (Voice v : all) if ("es".equals(v.getLocale().getLanguage())) spanish.add(v);
        } catch (Exception ignored) {
        }
        List<Voice> offline = new ArrayList<>();
        for (Voice v : spanish) {
            if (!v.isNetworkConnectionRequired() && !v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) {
                offline.add(v);
            }
        }
        List<Voice> result = offline.isEmpty() ? spanish : offline;
        result.sort(Comparator
                .comparing((Voice v) -> !"ES".equals(v.getLocale().getCountry()))
                .thenComparing(v -> v.getLocale().getCountry())
                .thenComparing(Voice::getName));
        return result;
    }

    /** Busca una voz guardada entre todas las del motor (aunque ya no salga en la lista). */
    public static Voice find(TextToSpeech tts, String id) {
        try {
            Set<Voice> all = tts.getVoices();
            if (all != null) for (Voice v : all) if (v.getName().equals(id)) return v;
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String label(String id) {
        for (VoiceOption o : list) if (o.id.equals(id)) return o.label;
        return "Voz";
    }

    public static String labels(List<String> ids) {
        if (ids.isEmpty()) return "Voz del sistema";
        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(label(id));
        }
        return sb.toString();
    }

    private static String roman(int n) {
        int[] values = {10, 9, 5, 4, 1};
        String[] symbols = {"X", "IX", "V", "IV", "I"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (n >= values[i]) {
                sb.append(symbols[i]);
                n -= values[i];
            }
        }
        return sb.toString();
    }
}
