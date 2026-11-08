package com.valentin.despertador.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.valentin.despertador.R;
import com.valentin.despertador.alarm.Scheduler;
import com.valentin.despertador.audio.Tracks;
import com.valentin.despertador.audio.Voices;
import com.valentin.despertador.data.Alarm;
import com.valentin.despertador.data.AlarmStore;
import com.valentin.despertador.ui.widget.ToggleView;
import com.valentin.despertador.ui.widget.WheelView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Editar una alarma ("Ajustes adicionales") o crear una nueva ("Añadir alarma"). */
public class EditActivity extends BaseActivity {
    private static final String DEFAULT = "Predeterminado (Ajustes)";
    private static final String DEFAULT_KEY = "__ajustes__";

    private Alarm initial;
    private boolean isNew;
    private int hour;
    private int minute;
    private TreeSet<Integer> days;
    private boolean shake;
    private String music;
    private List<String> voices;

    private EditText name;
    private EditText message;
    private TextView next;
    private TextView daysText;
    private TextView musicText;
    private TextView voiceText;
    private final Runnable refreshTexts = this::refreshTexts;

    /** Abre la pantalla con una alarma (la guardada o la que viene del panel con cambios). */
    public static void open(Activity from, Alarm alarm, boolean isNew) {
        try {
            from.startActivity(new Intent(from, EditActivity.class)
                    .putExtra("alarma", alarm.toJson().toString())
                    .putExtra("nueva", isNew));
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar);
        Ui.padForSystemBars(findViewById(R.id.raiz));
        try {
            initial = Alarm.fromJson(new JSONObject(getIntent().getStringExtra("alarma")));
        } catch (Exception e) {
            finish();
            return;
        }
        isNew = getIntent().getBooleanExtra("nueva", false);
        hour = initial.hour;
        minute = initial.minute;
        days = new TreeSet<>(initial.days);
        shake = initial.shakeToSnooze;
        music = initial.music;
        voices = initial.voices != null ? new ArrayList<>(initial.voices) : null;

        View bar = findViewById(R.id.barra);
        bar.findViewById(R.id.volver).setOnClickListener(v -> finish());
        View delete = bar.findViewById(R.id.eliminar);
        if (!isNew) {
            delete.setVisibility(View.VISIBLE);
            delete.setOnClickListener(v -> Dialogs.confirm(this, "Eliminar alarma",
                    "¿Seguro que quieres eliminar esta alarma?", "Eliminar", () -> {
                        Scheduler.cancel(this, initial.id);
                        AlarmStore.delete(initial.id);
                        finish();
                    }));
        }

        name = findViewById(R.id.nombre);
        name.setText(initial.name);
        Ui.tintCursor(name, palette.accent);
        message = findViewById(R.id.mensaje);
        message.setText(initial.message);
        Ui.tintCursor(message, palette.accent);

        findViewById(R.id.caja_rueda).setBackground(Ui.rounded(this, palette.card, 30, palette.border));
        WheelView hours = findViewById(R.id.horas);
        WheelView minutes = findViewById(R.id.minutos);
        hours.setRange(24, true);
        minutes.setRange(60, true);
        hours.setValue(hour);
        minutes.setValue(minute);
        hours.setOnChange(h -> {
            hour = h;
            refreshTexts();
        });
        minutes.setOnChange(m -> {
            minute = m;
            refreshTexts();
        });
        next = findViewById(R.id.proxima);
        daysText = findViewById(R.id.repetir_texto);
        findViewById(R.id.repetir).setOnClickListener(v -> editDays());

        ToggleView shakeToggle = findViewById(R.id.movimiento_interruptor);
        shakeToggle.setChecked(shake);
        shakeToggle.setOnChange(on -> shake = on);
        findViewById(R.id.movimiento).setOnClickListener(v -> shakeToggle.performClick());

        View musicField = findViewById(R.id.musica);
        musicText = musicField.findViewById(R.id.texto);
        musicField.setOnClickListener(v -> pickMusic(musicField));
        View voiceField = findViewById(R.id.voz);
        voiceText = voiceField.findViewById(R.id.texto);
        voiceField.setOnClickListener(v -> pickVoices(voiceField));

        findViewById(R.id.guardar).setOnClickListener(v -> {
            Scheduler.saveAndSchedule(this, build());
            finish();
        });

        Voices.changed.add(refreshTexts);
        Tracks.changed.add(refreshTexts);
        refreshTexts();
    }

    @Override
    protected void onDestroy() {
        Voices.changed.remove(refreshTexts);
        Tracks.changed.remove(refreshTexts);
        super.onDestroy();
    }

    private Alarm build() {
        Alarm a = initial.copy();
        a.name = name.getText().toString().trim();
        a.hour = hour;
        a.minute = minute;
        a.message = message.getText().toString().trim();
        a.days = new TreeSet<>(days);
        a.shakeToSnooze = shake;
        a.music = music;
        a.voices = voices != null ? new ArrayList<>(voices) : null;
        a.enabled = true;
        boolean timeChanged = hour != initial.hour || minute != initial.minute || !days.equals(initial.days);
        if (timeChanged || isNew) {
            a = a.withoutOverrides();
            a.enabled = true;
        }
        return a;
    }

    private void refreshTexts() {
        Long trigger = Scheduler.nextTrigger(build());
        if (trigger != null) {
            long end = trigger + AlarmStore.settings().durationMin * 60_000L;
            next.setText("Próxima alarma: " + Format.dayOf(trigger) + " " + Format.timeOf(trigger) + " a " + Format.timeOf(end));
        }
        daysText.setText(Format.days(days));
        musicText.setText(music != null ? Tracks.title(music) : DEFAULT);
        voiceText.setText(voices != null ? Voices.labels(voices) : DEFAULT);
    }

    private void pickMusic(View anchor) {
        List<Dropdown.Item> items = new ArrayList<>();
        items.add(Dropdown.Item.option(DEFAULT_KEY, DEFAULT, null, music == null));
        items.add(Dropdown.Item.header("Canciones"));
        for (Tracks.Track t : Tracks.SONGS) {
            items.add(Dropdown.Item.option(t.id, t.title, null, music != null && Tracks.same(music, t.id)));
        }
        items.add(Dropdown.Item.header("Despertadores del móvil"));
        for (Tracks.Track t : Tracks.phoneTones()) {
            items.add(Dropdown.Item.option(t.id, t.title, null, music != null && Tracks.same(music, t.id)));
        }
        Dropdown.show(anchor, items, false, (item, all) -> {
            music = DEFAULT_KEY.equals(item.key) ? null : item.key;
            refreshTexts();
        });
    }

    private void pickVoices(View anchor) {
        List<Dropdown.Item> items = new ArrayList<>();
        items.add(Dropdown.Item.option(DEFAULT_KEY, DEFAULT, null, voices == null));
        for (Voices.VoiceOption v : Voices.list()) {
            items.add(Dropdown.Item.option(v.id, v.label, v.region, voices != null && voices.contains(v.id)));
        }
        Dropdown.show(anchor, items, true, (item, all) -> {
            if (DEFAULT_KEY.equals(item.key)) {
                voices = null;
            } else {
                List<String> now = voices != null ? voices : new ArrayList<>();
                if (now.contains(item.key)) now.remove(item.key);
                else now.add(item.key);
                voices = now.isEmpty() ? null : now;
            }
            for (Dropdown.Item i : all) {
                if (i.header) continue;
                i.checked = DEFAULT_KEY.equals(i.key) ? voices == null : voices != null && voices.contains(i.key);
            }
            refreshTexts();
        });
    }

    private void editDays() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_repetir, null, false);
        Dialog dialog = Dialogs.panel(this, view);
        TreeSet<Integer> chosen = new TreeSet<>(days);
        TextView summary = view.findViewById(R.id.resumen);
        LinearLayout row = view.findViewById(R.id.dias);
        String[] letters = {"L", "M", "X", "J", "V", "S", "D"};
        Runnable[] draw = new Runnable[1];
        draw[0] = () -> {
            summary.setText(Format.days(chosen));
            row.removeAllViews();
            for (int i = 0; i < 7; i++) {
                int day = i + 1;
                boolean on = chosen.contains(day);
                TextView circle = new TextView(this);
                circle.setText(letters[i]);
                circle.setTextSize(16);
                circle.setGravity(Gravity.CENTER);
                circle.setTextColor(on ? palette.onAccent() : Palette.TEXT_MAIN);
                circle.setBackground(Ui.pill(this, on ? palette.accent : palette.card, on ? palette.accent : palette.border));
                circle.setOnClickListener(v -> {
                    if (!chosen.remove(day)) chosen.add(day);
                    draw[0].run();
                });
                // Los siete círculos repartidos a lo ancho
                LinearLayout cell = new LinearLayout(this);
                cell.setGravity(Gravity.CENTER);
                cell.addView(circle, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)));
                row.addView(cell, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            }
        };
        draw[0].run();
        view.findViewById(R.id.aceptar).setOnClickListener(v -> {
            days = chosen;
            refreshTexts();
            dialog.dismiss();
        });
        dialog.show();
    }
}
