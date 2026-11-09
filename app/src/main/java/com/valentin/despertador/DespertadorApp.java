package com.valentin.despertador;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import com.valentin.despertador.alarm.AlarmService;
import com.valentin.despertador.alarm.Scheduler;
import com.valentin.despertador.audio.LoudVolume;
import com.valentin.despertador.audio.Tracks;
import com.valentin.despertador.audio.VoiceCache;
import com.valentin.despertador.audio.Voices;
import com.valentin.despertador.data.AlarmStore;

public class DespertadorApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AlarmStore.init(this);
        // Si la app se cerró de golpe mientras sonaba, devuelve el volumen de alarma de antes
        LoudVolume.restore(this);

        // Canal de la notificación de la alarma (sin sonido: la música la pone el servicio)
        NotificationChannel channel = new NotificationChannel(AlarmService.CHANNEL, "Alarmas", NotificationManager.IMPORTANCE_HIGH);
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        Voices.load(this);
        Tracks.load(this);
        Scheduler.scheduleAll(this);
        VoiceCache.prepareAll(this);
    }
}
