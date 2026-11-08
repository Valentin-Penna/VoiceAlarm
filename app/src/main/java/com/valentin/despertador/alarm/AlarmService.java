package com.valentin.despertador.alarm;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.valentin.despertador.R;
import com.valentin.despertador.audio.AlarmSound;
import com.valentin.despertador.data.Alarm;
import com.valentin.despertador.data.AlarmStore;
import com.valentin.despertador.data.AppSettings;
import com.valentin.despertador.ui.Format;
import com.valentin.despertador.util.Signal;

/**
 * Hace sonar la alarma: música, voz y posponer al agitar.
 * Se puede retrasar hasta el límite (hora + duración); después suena sin parar hasta apagarla.
 */
public class AlarmService extends Service implements SensorEventListener {
    public static final String CHANNEL = "alarma";
    public static final String EXTRA_ID = "id";
    public static final String ACTION_START = "com.valentin.despertador.START";
    public static final String ACTION_STOP = "com.valentin.despertador.STOP";
    public static final String ACTION_SNOOZE = "com.valentin.despertador.SNOOZE";
    private static final int NOTIF_ID = 1;

    /** Avisa cuando empieza o deja de sonar una alarma. */
    public static final Signal ringingChanged = new Signal();
    private static volatile Integer ringingId = null;

    /** Id de la alarma que está sonando ahora (o null). */
    public static Integer ringing() {
        return ringingId;
    }

    public static Intent intent(Context context, String action, int id) {
        return new Intent(context, AlarmService.class).setAction(action).putExtra(EXTRA_ID, id);
    }

    public static boolean canSnooze(Alarm a) {
        return a.ringUntil == null || System.currentTimeMillis() < a.ringUntil;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Alarm alarm;
    private AlarmSound sound;
    private PowerManager.WakeLock wakeLock;
    private SensorManager sensors;
    private long startedAt;
    private int shakes;
    private long lastShake;

    /** Al llegar al límite, la notificación pierde el botón de retrasar. */
    private final Runnable limitReached = () -> {
        if (alarm != null) goForeground(alarm);
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AlarmStore.init(this);
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_START.equals(action)) {
            Alarm a = AlarmStore.get(intent.getIntExtra(EXTRA_ID, -1));
            // Hay que pasar a primer plano enseguida aunque luego no suene nada
            goForeground(a);
            if (a == null) {
                if (alarm == null) stopService();
            } else {
                if (alarm != null) stopRinging(false);
                start(a);
            }
        } else if (ACTION_SNOOZE.equals(action)) {
            snooze();
        } else if (ACTION_STOP.equals(action)) {
            stopRinging(true);
        } else if (alarm == null) {
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void start(Alarm stored) {
        long now = System.currentTimeMillis();
        startedAt = now;
        shakes = 0;
        AppSettings settings = AlarmStore.settings();

        // ¿Es la misma alarma que vuelve después de retrasarla, o un despertar nuevo?
        boolean afterSnooze = stored.ringUntil != null && stored.snoozeAt != null && stored.snoozeAt <= now + 60_000;
        Alarm a = stored.copy();
        a.snoozeAt = null;
        if (a.quickAt != null && a.quickAt <= now + 60_000) a.quickAt = null;
        if (!afterSnooze) {
            a.ringUntil = now + Math.max(1, settings.durationMin) * 60_000L;
            a.snoozeCount = 0;
        }
        AlarmStore.save(a);
        alarm = a;
        ringingId = a.id;
        ringingChanged.emit();
        goForeground(a);

        // Suena hasta que se apague, así que el móvil no debe dormirse
        wakeLock = getSystemService(PowerManager.class)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "despertador:alarma");
        wakeLock.acquire(12 * 3_600_000L);

        sound = new AlarmSound(this);
        sound.start(a.music != null ? a.music : settings.music, settings.musicVolume, a.message,
                a.voices != null ? a.voices : settings.voices, settings.voiceVolume, settings.voiceIntervalSec);

        if (a.shakeToSnooze && canSnooze(a)) {
            sensors = getSystemService(SensorManager.class);
            Sensor acc = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (acc != null) sensors.registerListener(this, acc, SensorManager.SENSOR_DELAY_GAME);
        }

        if (a.ringUntil != null && a.ringUntil > now) handler.postDelayed(limitReached, a.ringUntil - now + 500);

        // Si Android lo permite, abre la pantalla de la alarma directamente
        try {
            startActivity(ringIntent(a.id));
        } catch (Exception ignored) {
        }
    }

    private void snooze() {
        Alarm a = alarm;
        if (a == null) return;
        long now = System.currentTimeMillis();
        // Pasado el límite ya no se puede retrasar: sigue sonando
        if (!canSnooze(a)) return;
        Alarm current = AlarmStore.get(a.id);
        Alarm updated = current != null ? current : a.copy();
        long limit = a.ringUntil != null ? a.ringUntil : Long.MAX_VALUE;
        long snoozeMs = Math.max(1, AlarmStore.settings().snoozeMin) * 60_000L;
        updated.enabled = true;
        // Si el retraso pasa del límite, vuelve a sonar justo en el límite
        updated.snoozeAt = Math.min(now + snoozeMs, limit);
        updated.snoozeCount = a.snoozeCount + 1;
        updated.ringUntil = a.ringUntil;
        release();
        Scheduler.saveAndSchedule(this, updated);
        ringingId = null;
        ringingChanged.emit();
        stopService();
    }

    private void stopRinging(boolean stopService) {
        Alarm a = alarm;
        release();
        if (a != null) Scheduler.dismiss(this, a.id);
        ringingId = null;
        ringingChanged.emit();
        if (stopService) stopService();
    }

    private void release() {
        handler.removeCallbacksAndMessages(null);
        if (sound != null) sound.release();
        sound = null;
        if (sensors != null) sensors.unregisterListener(this);
        sensors = null;
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        wakeLock = null;
        alarm = null;
    }

    private void stopService() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (alarm != null) {
            release();
            ringingId = null;
            ringingChanged.emit();
        }
        super.onDestroy();
    }

    // --- Posponer agitando el móvil ---

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0], y = event.values[1], z = event.values[2];
        double g = Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;
        long now = System.currentTimeMillis();
        if (g < 2.2 || now - startedAt < 2000 || now - lastShake < 250) return;
        // Dos sacudidas seguidas para que no se posponga por un golpe sin querer
        shakes = now - lastShake < 1500 ? shakes + 1 : 1;
        lastShake = now;
        if (shakes >= 2) snooze();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // --- Notificación ---

    private Intent ringIntent(int id) {
        return new Intent(this, RingActivity.class)
                .putExtra(EXTRA_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
    }

    private void goForeground(Alarm a) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent ring = PendingIntent.getActivity(this, 1, ringIntent(a != null ? a.id : -1), flags);
        PendingIntent stop = PendingIntent.getService(this, 3, intent(this, ACTION_STOP, -1), flags);
        String title = a != null && !a.name.trim().isEmpty() ? a.name : "Alarma";
        String text = null;
        if (a != null) {
            text = Format.time(a.hour, a.minute);
            if (!a.message.trim().isEmpty()) text += " · " + a.message;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle(title)
                .setContentText(text)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setFullScreenIntent(ring, true)
                .setContentIntent(ring);
        if (a != null && canSnooze(a)) {
            PendingIntent snooze = PendingIntent.getService(this, 2, intent(this, ACTION_SNOOZE, -1), flags);
            builder.addAction(0, "Retrasar " + AlarmStore.settings().snoozeMin + " min", snooze);
        }
        builder.addAction(0, "Apagar alarma", stop);
        Notification notification = builder.build();
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK : 0;
        ServiceCompat.startForeground(this, NOTIF_ID, notification, type);
    }
}
