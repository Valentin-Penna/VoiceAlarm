package com.valentin.despertador.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.valentin.despertador.MainActivity;
import com.valentin.despertador.audio.VoiceCache;
import com.valentin.despertador.data.Alarm;
import com.valentin.despertador.data.AlarmStore;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Calcula cuándo toca cada alarma y se lo dice a Android. */
public final class Scheduler {
    private Scheduler() {
    }

    /** Próxima vez que toca según la hora y los días (sin alarma rápida ni retraso). */
    public static long nextNormal(Alarm a, long fromMillis) {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime from = Instant.ofEpochMilli(fromMillis).atZone(zone);
        LocalDate today = from.toLocalDate();
        for (int i = 0; i <= 7; i++) {
            ZonedDateTime candidate = today.plusDays(i).atTime(a.hour, a.minute).atZone(zone);
            if (candidate.isAfter(from) && (a.days.isEmpty() || a.days.contains(candidate.getDayOfWeek().getValue()))) {
                return candidate.toInstant().toEpochMilli();
            }
        }
        return today.plusDays(1).atTime(a.hour, a.minute).atZone(zone).toInstant().toEpochMilli();
    }

    /** Cuándo va a sonar de verdad, o null si está apagada. */
    public static Long nextTrigger(Alarm a) {
        long now = System.currentTimeMillis();
        if (!a.enabled) return null;
        if (a.snoozeAt != null && a.snoozeAt > now) return a.snoozeAt;
        if (a.quickAt != null && a.quickAt > now) return a.quickAt;
        Long skip = a.skipUntil != null && a.skipUntil >= now ? a.skipUntil : null;
        return nextNormal(a, skip != null ? skip + 1000 : now);
    }

    /** Alarma rápida: suena dentro de [minutes] solo esta vez y sustituye al siguiente sonido normal. */
    public static Alarm withQuick(Alarm base, int minutes) {
        long now = System.currentTimeMillis();
        long quickAt = now + minutes * 60_000L;
        long normal = nextNormal(base, now);
        Alarm a = base.copy();
        a.enabled = true;
        a.quickAt = quickAt;
        // Solo se salta el sonido normal si es el mismo despertar (no uno de dentro de varios días)
        a.skipUntil = normal <= quickAt + 12 * 3_600_000L ? normal : null;
        a.snoozeAt = null;
        return a;
    }

    public static void saveAndSchedule(Context context, Alarm a) {
        AlarmStore.save(a);
        schedule(context, a);
        VoiceCache.prepareAll(context);
    }

    /** Apagar la alarma: quita el retraso y, si no se repite, la desactiva. */
    public static void dismiss(Context context, int id) {
        Alarm a = AlarmStore.get(id);
        if (a == null) return;
        long now = System.currentTimeMillis();
        Alarm updated;
        if (a.days.isEmpty() && (a.quickAt == null || a.quickAt <= now)) {
            updated = a.withoutOverrides();
            updated.enabled = false;
        } else {
            updated = a.copy();
            updated.snoozeAt = null;
            updated.snoozeCount = 0;
            updated.ringUntil = null;
        }
        saveAndSchedule(context, updated);
    }

    public static void scheduleAll(Context context) {
        for (Alarm a : AlarmStore.alarms()) schedule(context, a);
    }

    public static void schedule(Context context, Alarm a) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        PendingIntent operation = operation(context, a.id);
        am.cancel(operation);
        Long trigger = nextTrigger(a);
        if (trigger == null) return;
        PendingIntent show = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(trigger, show), operation);
        } catch (SecurityException e) {
            // Sin permiso de alarma exacta: al menos que suene aproximadamente a su hora
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, operation);
        }
    }

    public static void cancel(Context context, int id) {
        context.getSystemService(AlarmManager.class).cancel(operation(context, id));
    }

    private static PendingIntent operation(Context context, int id) {
        Intent intent = new Intent(context, AlarmReceiver.class).putExtra(AlarmService.EXTRA_ID, id);
        return PendingIntent.getBroadcast(context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
