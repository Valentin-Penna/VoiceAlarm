package com.valentin.despertador.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import com.valentin.despertador.data.Alarm;
import com.valentin.despertador.data.AlarmStore;

/** Lo llama Android a la hora de la alarma: arranca el servicio que la hace sonar. */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmStore.init(context);
        int id = intent.getIntExtra(AlarmService.EXTRA_ID, -1);
        Alarm alarm = AlarmStore.get(id);
        if (alarm == null || !alarm.enabled) return;
        ContextCompat.startForegroundService(context, AlarmService.intent(context, AlarmService.ACTION_START, id));
    }
}
