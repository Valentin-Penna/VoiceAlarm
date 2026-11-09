package com.valentin.despertador;

import android.Manifest;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.valentin.despertador.alarm.Scheduler;
import com.valentin.despertador.audio.Voices;
import com.valentin.despertador.data.Alarm;
import com.valentin.despertador.data.AlarmStore;
import com.valentin.despertador.data.AppSettings;
import com.valentin.despertador.ui.BaseActivity;
import com.valentin.despertador.ui.Dialogs;
import com.valentin.despertador.ui.EditActivity;
import com.valentin.despertador.ui.Format;
import com.valentin.despertador.ui.SettingsActivity;
import com.valentin.despertador.ui.Ui;
import com.valentin.despertador.ui.widget.ToggleView;
import com.valentin.despertador.ui.widget.WheelView;

import java.util.List;

/** Lista de alarmas. Al tocar una se despliega con la hora, la alarma rápida y el interruptor. */
public class MainActivity extends BaseActivity {
    private LinearLayout list;
    private TextView empty;
    private final Runnable refresh = this::render;

    private final ActivityResultLauncher<String> askNotifications =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Ui.padForSystemBars(findViewById(R.id.raiz));
        list = findViewById(R.id.lista);
        empty = findViewById(R.id.vacia);

        View settings = findViewById(R.id.ajustes);
        settings.setBackground(Ui.pill(this, palette.card, 0));
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.anadir).setOnClickListener(v -> {
            // Las alarmas nuevas empiezan sonando todos los días
            Alarm alarm = new Alarm(AlarmStore.newId());
            for (int d = 1; d <= 7; d++) alarm.days.add(d);
            EditActivity.open(this, alarm, true);
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        Voices.load(this);
        askFullScreen();

        AlarmStore.changed.add(refresh);
        render();
    }

    @Override
    protected void onDestroy() {
        AlarmStore.changed.remove(refresh);
        super.onDestroy();
    }

    /** Desde Android 14 hay que permitir que la alarma salga a pantalla completa. */
    private void askFullScreen() {
        if (Build.VERSION.SDK_INT < 34) return;
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm.canUseFullScreenIntent()) return;
        Dialogs.confirm(this, "Permiso necesario",
                "Para que la alarma aparezca en pantalla aunque el móvil esté bloqueado, "
                        + "activa las notificaciones a pantalla completa para Despertador.",
                "Activar",
                () -> startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:" + getPackageName()))));
    }

    private void render() {
        list.removeAllViews();
        List<Alarm> alarms = AlarmStore.alarms();
        empty.setVisibility(alarms.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Alarm a : alarms) {
            View card = inflater.inflate(R.layout.item_alarma, list, false);
            ((TextView) card.findViewById(R.id.hora)).setText(Format.time(a.hour, a.minute));
            ((TextView) card.findViewById(R.id.nombre)).setText(a.name);
            ((TextView) card.findViewById(R.id.dias)).setText(Format.days(a.days));
            TextView quick = card.findViewById(R.id.rapida);
            if (a.enabled && a.quickAt != null && a.quickAt > System.currentTimeMillis()) {
                quick.setText("Alarma rápida: " + Format.timeOf(a.quickAt));
                quick.setVisibility(View.VISIBLE);
            }
            ToggleView toggle = card.findViewById(R.id.interruptor);
            toggle.setChecked(a.enabled);
            toggle.setOnChange(on -> {
                Alarm updated = on ? a.copy() : a.withoutOverrides();
                updated.enabled = on;
                Scheduler.saveAndSchedule(this, updated);
            });
            card.setOnClickListener(v -> openPanel(a));
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) card.getLayoutParams();
            lp.bottomMargin = Ui.dp(this, 15);
            list.addView(card, lp);
        }
    }

    /** La alarma desplegada al tocarla: hora, alarma rápida, interruptor y botones. */
    private void openPanel(Alarm alarm) {
        AppSettings settings = AlarmStore.settings();
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_panel, null, false);
        Dialog dialog = Dialogs.panel(this, view);
        int[] time = {alarm.hour, alarm.minute};
        boolean[] enabled = {alarm.enabled};

        TextView hour = view.findViewById(R.id.hora);
        hour.setText(Format.time(alarm.hour, alarm.minute));
        ((TextView) view.findViewById(R.id.nombre)).setText(alarm.name);
        ((TextView) view.findViewById(R.id.dias)).setText(Format.days(alarm.days));

        View quick = view.findViewById(R.id.rapida);
        quick.setBackground(Ui.pill(this, palette.card, palette.border));
        ((TextView) view.findViewById(R.id.rapida_texto)).setText("+" + Format.duration(settings.quickMinutes));

        ToggleView toggle = view.findViewById(R.id.interruptor);
        toggle.setChecked(alarm.enabled);
        toggle.setOnChange(on -> enabled[0] = on);

        WheelView hours = view.findViewById(R.id.horas);
        WheelView minutes = view.findViewById(R.id.minutos);
        hours.setStyle(56, 44, 18);
        minutes.setStyle(56, 44, 18);
        hours.setRange(24, true);
        minutes.setRange(60, true);
        hours.setValue(alarm.hour);
        minutes.setValue(alarm.minute);
        hours.setOnChange(h -> {
            time[0] = h;
            hour.setText(Format.time(time[0], time[1]));
        });
        minutes.setOnChange(m -> {
            time[1] = m;
            hour.setText(Format.time(time[0], time[1]));
        });

        view.findViewById(R.id.mas).setOnClickListener(v -> {
            dialog.dismiss();
            EditActivity.open(this, pending(alarm, time, enabled[0]), false);
        });
        view.findViewById(R.id.listo).setOnClickListener(v -> {
            Scheduler.saveAndSchedule(this, pending(alarm, time, enabled[0]));
            dialog.dismiss();
        });
        quick.setOnClickListener(v -> {
            long at = System.currentTimeMillis() + settings.quickMinutes * 60_000L;
            Dialogs.confirm(this, "Alarma rápida",
                    "Sonará en " + Format.duration(settings.quickMinutes) + ", a las " + Format.timeOf(at)
                            + ". ¿Programar esta alarma?",
                    "Programar",
                    () -> {
                        Alarm base = alarm.copy();
                        base.hour = time[0];
                        base.minute = time[1];
                        Scheduler.saveAndSchedule(this, Scheduler.withQuick(base, settings.quickMinutes));
                        dialog.dismiss();
                    });
        });
        dialog.show();
    }

    /** La alarma con los cambios del panel. Si cambia la hora o se apaga, se quita lo pendiente. */
    private static Alarm pending(Alarm alarm, int[] time, boolean enabled) {
        boolean changed = time[0] != alarm.hour || time[1] != alarm.minute;
        Alarm a = changed || !enabled ? alarm.withoutOverrides() : alarm.copy();
        a.hour = time[0];
        a.minute = time[1];
        a.enabled = enabled;
        return a;
    }
}
