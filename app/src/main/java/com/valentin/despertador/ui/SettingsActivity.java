package com.valentin.despertador.ui;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.valentin.despertador.R;
import com.valentin.despertador.audio.AlarmSound;
import com.valentin.despertador.audio.Tracks;
import com.valentin.despertador.audio.VoiceCache;
import com.valentin.despertador.audio.Voices;
import com.valentin.despertador.data.AlarmStore;
import com.valentin.despertador.data.AppSettings;
import com.valentin.despertador.ui.widget.LineSlider;
import com.valentin.despertador.ui.widget.WheelView;

import java.util.ArrayList;
import java.util.List;

/** Configuración: tiempos, alarma rápida, música y voces de serie, volúmenes y "Probar sonido". */
public class SettingsActivity extends BaseActivity {
    private AppSettings saved;
    private int quick;
    private String music;
    private List<String> voices;
    private float musicVolume;
    private float voiceVolume;

    private EditText duration;
    private EditText interval;
    private EditText snooze;
    private TextView quickText;
    private TextView musicText;
    private TextView voiceText;
    private TextView test;
    private AlarmSound preview;
    private final Runnable refreshTexts = this::refreshTexts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);
        Ui.padForSystemBars(findViewById(R.id.raiz));
        saved = AlarmStore.settings();
        quick = saved.quickMinutes;
        music = saved.music;
        voices = new ArrayList<>(saved.voices);
        musicVolume = saved.musicVolume;
        voiceVolume = saved.voiceVolume;

        findViewById(R.id.barra).findViewById(R.id.volver).setOnClickListener(v -> finish());
        findViewById(R.id.disenos).setOnClickListener(v -> {
            stopPreview();
            startActivity(new Intent(this, DesignsActivity.class));
        });

        duration = field(R.id.duracion, saved.durationMin);
        interval = field(R.id.intervalo, saved.voiceIntervalSec);
        snooze = field(R.id.posponer, saved.snoozeMin);

        quickText = findViewById(R.id.rapida);
        quickText.setBackground(Ui.rounded(this, 0, 18, palette.border));
        quickText.setOnClickListener(v -> editQuick());

        View musicField = findViewById(R.id.musica);
        musicText = musicField.findViewById(R.id.texto);
        musicField.setOnClickListener(v -> pickMusic(musicField));
        View voiceField = findViewById(R.id.voz);
        voiceText = voiceField.findViewById(R.id.texto);
        voiceField.setOnClickListener(v -> pickVoices(voiceField));

        LineSlider musicSlider = findViewById(R.id.volumen_musica);
        musicSlider.setValue(musicVolume);
        musicSlider.setOnChange(value -> {
            musicVolume = value;
            if (preview != null) preview.setVolumes(musicVolume, voiceVolume);
        });
        LineSlider voiceSlider = findViewById(R.id.volumen_voz);
        voiceSlider.setValue(voiceVolume);
        voiceSlider.setOnChange(value -> {
            voiceVolume = value;
            if (preview != null) preview.setVolumes(musicVolume, voiceVolume);
        });

        test = findViewById(R.id.probar);
        test.setOnClickListener(v -> {
            if (preview != null) {
                stopPreview();
                return;
            }
            // Como la alarma de verdad: la música elegida y la frase con las voces
            // elegidas cada X segundos, hasta pulsar "Detener" o salir
            preview = new AlarmSound(this);
            preview.start(music, musicVolume, VoiceCache.PREVIEW, voices, voiceVolume,
                    number(interval, saved.voiceIntervalSec, 1, 600));
            test.setText("DETENER");
        });

        findViewById(R.id.guardar).setOnClickListener(v -> {
            stopPreview();
            AppSettings s = AlarmStore.settings();
            s.durationMin = number(duration, saved.durationMin, 1, 180);
            s.voiceIntervalSec = number(interval, saved.voiceIntervalSec, 1, 600);
            s.snoozeMin = number(snooze, saved.snoozeMin, 1, 120);
            s.quickMinutes = quick;
            s.music = music;
            s.voices = new ArrayList<>(voices);
            s.musicVolume = musicVolume;
            s.voiceVolume = voiceVolume;
            AlarmStore.saveSettings(s);
            VoiceCache.prepareAll(this);
            finish();
        });

        Voices.changed.add(refreshTexts);
        Tracks.changed.add(refreshTexts);
        refreshTexts();
    }

    @Override
    protected void onStop() {
        stopPreview();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Voices.changed.remove(refreshTexts);
        Tracks.changed.remove(refreshTexts);
        super.onDestroy();
    }

    private EditText field(int id, int value) {
        EditText e = findViewById(id);
        e.setText(String.valueOf(value));
        Ui.tintCursor(e, palette.accent);
        return e;
    }

    private static int number(EditText e, int fallback, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(e.getText().toString().trim())));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private void stopPreview() {
        if (preview != null) preview.release();
        preview = null;
        if (test != null) test.setText("PROBAR SONIDO");
    }

    private void refreshTexts() {
        quickText.setText(Format.duration(quick));
        musicText.setText(Tracks.title(music));
        voiceText.setText(Voices.labels(voices));
    }

    private void pickMusic(View anchor) {
        List<Dropdown.Item> items = new ArrayList<>();
        items.add(Dropdown.Item.header("Canciones"));
        for (Tracks.Track t : Tracks.SONGS) items.add(Dropdown.Item.option(t.id, t.title, null, Tracks.same(music, t.id)));
        items.add(Dropdown.Item.header("Despertadores del móvil"));
        for (Tracks.Track t : Tracks.phoneTones()) items.add(Dropdown.Item.option(t.id, t.title, null, Tracks.same(music, t.id)));
        Dropdown.show(anchor, items, false, (item, all) -> {
            music = item.key;
            refreshTexts();
        });
    }

    private void pickVoices(View anchor) {
        List<Dropdown.Item> items = new ArrayList<>();
        if (Voices.list().isEmpty()) {
            items.add(Dropdown.Item.option("", "Voz del sistema", "No hay voces en español instaladas", true));
        }
        for (Voices.VoiceOption v : Voices.list()) {
            items.add(Dropdown.Item.option(v.id, v.label, v.region, voices.contains(v.id)));
        }
        Dropdown.show(anchor, items, true, (item, all) -> {
            if (item.key.isEmpty()) return;
            if (!voices.remove(item.key)) voices.add(item.key);
            item.checked = voices.contains(item.key);
            refreshTexts();
        });
    }

    private void editQuick() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_rapida, null, false);
        Dialog dialog = Dialogs.panel(this, view);
        WheelView hours = view.findViewById(R.id.horas);
        WheelView minutes = view.findViewById(R.id.minutos);
        hours.setStyle(52, 36, 18);
        minutes.setStyle(52, 36, 18);
        hours.setRange(24, false);
        minutes.setRange(60, true);
        hours.setValue(quick / 60);
        minutes.setValue(quick % 60);
        view.findViewById(R.id.cancelar).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.aceptar).setOnClickListener(v -> {
            quick = Math.max(1, hours.getValue() * 60 + minutes.getValue());
            refreshTexts();
            dialog.dismiss();
        });
        dialog.show();
    }
}
