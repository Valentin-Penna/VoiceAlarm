package com.valentin.despertador.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * Música en bucle + mensaje de voz repetido cada X segundos.
 * El primer mensaje sale siempre 1 segundo después de que empiece la música.
 * Todo el audio va en un hilo propio para que no lo retrase la pantalla.
 * Se usa al sonar la alarma y para "Probar sonido" en Configuración.
 */
public class AlarmSound {
    private final Context context;
    private final HandlerThread thread = new HandlerThread("alarma-audio");
    private final Handler handler;

    private MediaPlayer player;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private volatile boolean released = false;

    private float musicVolume = 1f;
    private float voiceVolume = 1f;
    private String message = "";
    private List<String> voiceIds;
    private long intervalMs = 15_000;
    private int voiceIndex = 0;

    /** Grabación del siguiente mensaje, ya preparada para sonar al instante. */
    private MediaPlayer nextVoice;
    private MediaPlayer playingVoice;
    /** Voz que espera a que arranque el motor de voz (cuando no hay grabación). */
    private String waitingVoice;

    /** Nivel de la música respecto a su volumen: baja poco a poco mientras habla la voz. */
    private float level = 1f;
    private float targetLevel = 1f;

    private final Runnable fade = new Runnable() {
        @Override
        public void run() {
            // Baja en ~0,4 s y vuelve a subir más despacio, en ~1 s
            level = targetLevel < level ? Math.max(targetLevel, level - 0.05f) : Math.min(targetLevel, level + 0.025f);
            applyMusicVolume();
            if (level != targetLevel) handler.postDelayed(this, 30);
        }
    };

    private final Runnable speakNext = this::speak;

    public AlarmSound(Context context) {
        this.context = context.getApplicationContext();
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    private static AudioAttributes attrs(int contentType) {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(contentType)
                .build();
    }

    public void start(String musicId, float musicVolume, String message, List<String> voiceIds,
                      float voiceVolume, int intervalSec) {
        handler.post(() -> {
            this.musicVolume = musicVolume;
            this.voiceVolume = voiceVolume;
            this.message = message.trim();
            this.voiceIds = VoiceCache.ids(voiceIds);
            this.intervalMs = Math.max(1, intervalSec) * 1000L;
            startMusic(musicId);
            long musicStarted = SystemClock.uptimeMillis();
            if (this.message.isEmpty()) return;

            // Si falta alguna grabación, se arranca ya el motor de voz por si acaso
            if (!VoiceCache.isReady(context, voiceIds, this.message)) startTts();
            prepareNextVoice();
            handler.postAtTime(speakNext, musicStarted + 1000);
        });
    }

    public void setVolumes(float music, float voice) {
        handler.post(() -> {
            musicVolume = music;
            voiceVolume = voice;
            applyMusicVolume();
            try {
                if (playingVoice != null) playingVoice.setVolume(voice, voice);
            } catch (Exception ignored) {
            }
        });
    }

    // --- Música ---

    private void startMusic(String musicId) {
        try {
            player = createMusic(Tracks.uri(musicId));
        } catch (Exception e) {
            Log.w("Despertador", "No se pudo reproducir " + musicId + ", uso el tono del móvil", e);
            try {
                player = createMusic(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
            } catch (Exception ignored) {
                player = null;
            }
        }
    }

    private MediaPlayer createMusic(Uri uri) throws Exception {
        MediaPlayer p = new MediaPlayer();
        p.setAudioAttributes(attrs(AudioAttributes.CONTENT_TYPE_MUSIC));
        p.setDataSource(context, uri);
        p.setLooping(true);
        p.prepare();
        p.setVolume(musicVolume, musicVolume);
        p.start();
        return p;
    }

    private void fadeTo(float target) {
        targetLevel = target;
        handler.removeCallbacks(fade);
        handler.post(fade);
    }

    private void applyMusicVolume() {
        float v = musicVolume * level;
        try {
            if (player != null) player.setVolume(v, v);
        } catch (Exception ignored) {
        }
    }

    // --- Voz ---

    private String currentVoiceId() {
        return voiceIds.get(voiceIndex % voiceIds.size());
    }

    /** Deja preparada la grabación del próximo mensaje (si existe). */
    private void prepareNextVoice() {
        if (nextVoice != null) nextVoice.release();
        nextVoice = null;
        File file = VoiceCache.file(context, currentVoiceId(), message);
        if (file.length() == 0) return;
        try {
            MediaPlayer p = new MediaPlayer();
            p.setAudioAttributes(attrs(AudioAttributes.CONTENT_TYPE_SPEECH));
            p.setDataSource(file.getPath());
            p.prepare();
            nextVoice = p;
        } catch (Exception ignored) {
            nextVoice = null;
        }
    }

    private void speak() {
        if (released) return;
        String voiceId = currentVoiceId();
        // Con varias voces elegidas, cada mensaje lo dice una distinta
        voiceIndex++;
        fadeTo(0.25f);
        MediaPlayer ready = nextVoice;
        nextVoice = null;
        if (ready != null) {
            try {
                if (playingVoice != null) playingVoice.release();
                ready.setVolume(voiceVolume, voiceVolume);
                ready.setOnCompletionListener(mp -> afterMessage());
                ready.setOnErrorListener((mp, what, extra) -> {
                    afterMessage();
                    return true;
                });
                ready.start();
                playingVoice = ready;
                return;
            } catch (Exception e) {
                ready.release();
                playingVoice = null;
            }
        }
        speakLive(voiceId);
    }

    private void startTts() {
        if (tts != null) return;
        tts = new TextToSpeech(context, status -> handler.post(() -> {
            TextToSpeech t = tts;
            if (status != TextToSpeech.SUCCESS || released || t == null) {
                // Sin motor de voz: que al menos siga el ciclo de mensajes
                if (waitingVoice != null) afterMessage();
                return;
            }
            t.setAudioAttributes(attrs(AudioAttributes.CONTENT_TYPE_SPEECH));
            t.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                }

                @Override
                public void onDone(String utteranceId) {
                    afterMessage();
                }

                @Override
                @SuppressWarnings("deprecation")
                public void onError(String utteranceId) {
                    afterMessage();
                }
            });
            ttsReady = true;
            if (waitingVoice != null) speakLive(waitingVoice);
        }));
    }

    /** Sin grabación: lo dice el motor de voz en el momento (en cuanto esté listo). */
    private void speakLive(String voiceId) {
        TextToSpeech t = tts;
        if (t == null || !ttsReady) {
            waitingVoice = voiceId;
            startTts();
            return;
        }
        waitingVoice = null;
        Voice voice = Voices.find(t, voiceId);
        if (voice != null) t.setVoice(voice);
        else t.setLanguage(Locale.forLanguageTag("es-ES"));
        Bundle params = new Bundle();
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, voiceVolume);
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM);
        if (t.speak(message, TextToSpeech.QUEUE_FLUSH, params, "mensaje") != TextToSpeech.SUCCESS) afterMessage();
    }

    private void afterMessage() {
        handler.post(() -> {
            if (released) return;
            handler.removeCallbacks(speakNext);
            fadeTo(1f);
            prepareNextVoice();
            handler.postDelayed(speakNext, intervalMs);
        });
    }

    public void release() {
        released = true;
        handler.removeCallbacksAndMessages(null);
        handler.post(() -> {
            for (MediaPlayer p : new MediaPlayer[]{player, playingVoice, nextVoice}) {
                if (p == null) continue;
                try {
                    p.stop();
                } catch (Exception ignored) {
                }
                p.release();
            }
            player = null;
            playingVoice = null;
            nextVoice = null;
            if (tts != null) {
                tts.stop();
                tts.shutdown();
            }
            tts = null;
            thread.quitSafely();
        });
    }
}
