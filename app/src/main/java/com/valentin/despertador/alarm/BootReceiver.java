package com.valentin.despertador.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.valentin.despertador.data.AlarmStore;

/** Al encender el móvil o cambiar la hora, vuelve a programar todas las alarmas. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmStore.init(context);
        Scheduler.scheduleAll(context);
    }
}
