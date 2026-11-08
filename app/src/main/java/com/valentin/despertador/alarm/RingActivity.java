package com.valentin.despertador.alarm;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;

import com.valentin.despertador.R;
import com.valentin.despertador.data.Alarm;
import com.valentin.despertador.data.AlarmStore;
import com.valentin.despertador.ui.BaseActivity;
import com.valentin.despertador.ui.Format;
import com.valentin.despertador.ui.Ui;
import com.valentin.despertador.ui.widget.SlideToConfirmView;

import java.util.Locale;

/**
 * Pantalla que aparece cuando suena la alarma (también con el móvil bloqueado).
 * Al retrasarla se queda abierta diciendo cuántas veces se ha retrasado y cuándo vuelve a sonar.
 */
public class RingActivity extends BaseActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int alarmId;
    private boolean ringing;

    private TextView name, clock, date, message, times, snoozedTimes, back, countdown, limit, retrasar;
    private View ringingGroup, snoozedGroup, snoozeArea, noSnooze;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            update();
            handler.postDelayed(this, 1000);
        }
    };
    private final Runnable changed = this::update;
    private final Runnable finishIfIdle = () -> {
        if (AlarmService.ringing() == null && !isSnoozed()) finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        setContentView(R.layout.activity_alarma);
        Ui.padForSystemBars(findViewById(R.id.raiz));
        alarmId = getIntent().getIntExtra(AlarmService.EXTRA_ID, -1);

        name = findViewById(R.id.nombre);
        clock = findViewById(R.id.hora);
        date = findViewById(R.id.fecha);
        message = findViewById(R.id.mensaje);
        times = findViewById(R.id.veces);
        snoozedTimes = findViewById(R.id.retrasada_veces);
        back = findViewById(R.id.vuelve);
        countdown = findViewById(R.id.cuenta);
        limit = findViewById(R.id.limite);
        retrasar = findViewById(R.id.retrasar);
        ringingGroup = findViewById(R.id.sonando);
        snoozedGroup = findViewById(R.id.retrasada);
        snoozeArea = findViewById(R.id.zona_retrasar);
        noSnooze = findViewById(R.id.sin_retrasar);

        retrasar.setOnClickListener(v -> startService(AlarmService.intent(this, AlarmService.ACTION_SNOOZE, -1)));
        SlideToConfirmView slide = findViewById(R.id.apagar);
        slide.setText("Desliza para apagar");
        slide.setOnConfirm(() -> {
            if (ringing) startService(AlarmService.intent(this, AlarmService.ACTION_STOP, -1));
            else Scheduler.dismiss(this, alarmId);
            finish();
        });

        // Atrás no la cierra mientras suena: hay que retrasarla o apagarla
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!ringing) finish();
            }
        });

        AlarmService.ringingChanged.add(changed);
        AlarmStore.changed.add(changed);
        handler.post(tick);
        // Puede abrirse un instante antes de que empiece a sonar
        handler.postDelayed(finishIfIdle, 3000);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        AlarmService.ringingChanged.remove(changed);
        AlarmStore.changed.remove(changed);
        super.onDestroy();
    }

    private boolean isSnoozed() {
        Alarm a = AlarmStore.get(alarmId);
        return a != null && a.snoozeAt != null && a.snoozeAt > System.currentTimeMillis();
    }

    private void update() {
        Integer id = AlarmService.ringing();
        boolean wasRinging = ringing;
        ringing = id != null;
        if (ringing) alarmId = id;
        if (wasRinging && !ringing && !isSnoozed()) {
            finish();
            return;
        }
        // La pantalla solo se mantiene encendida mientras suena
        if (ringing) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Alarm a = AlarmStore.get(alarmId);
        long now = System.currentTimeMillis();
        boolean showRinging = ringing || !isSnoozed();
        int count = a != null ? a.snoozeCount : 0;

        name.setText(a != null && !a.name.trim().isEmpty() ? a.name : "Alarma");
        clock.setText(Format.timeOf(now));
        clock.setTextColor(showRinging ? 0xFFF2F4F8 : 0xFFB2B9C8);
        // Hora con brillo del color del diseño
        if (showRinging) clock.setShadowLayer(40f, 0, 0, palette.accent);
        else clock.setShadowLayer(0, 0, 0, 0);
        date.setText(Format.dateOf(now));

        ringingGroup.setVisibility(showRinging ? View.VISIBLE : View.GONE);
        snoozedGroup.setVisibility(showRinging ? View.GONE : View.VISIBLE);
        snoozeArea.setVisibility(showRinging ? View.VISIBLE : View.GONE);
        if (showRinging) {
            String msg = a != null ? a.message.trim() : "";
            message.setText(msg);
            message.setVisibility(msg.isEmpty() ? View.GONE : View.VISIBLE);
            times.setText("Retrasada " + Format.times(count));
            times.setVisibility(count > 0 ? View.VISIBLE : View.GONE);

            boolean canSnooze = a == null || AlarmService.canSnooze(a);
            retrasar.setText("Retrasar " + AlarmStore.settings().snoozeMin + " min");
            retrasar.setVisibility(canSnooze ? View.VISIBLE : View.GONE);
            limit.setVisibility(canSnooze && a != null && a.ringUntil != null ? View.VISIBLE : View.GONE);
            if (a != null && a.ringUntil != null) limit.setText("Se puede retrasar hasta las " + Format.timeOf(a.ringUntil));
            noSnooze.setVisibility(canSnooze ? View.GONE : View.VISIBLE);
        } else {
            long next = a.snoozeAt;
            long left = Math.max(0, (next - now) / 1000);
            snoozedTimes.setText("Retrasada " + Format.times(count));
            back.setText(Format.timeOf(next));
            countdown.setText(String.format(Locale.ROOT, "dentro de %d:%02d", left / 60, left % 60));
        }
    }
}
