package com.valentin.despertador.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.valentin.despertador.util.Signal;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Guarda las alarmas y los ajustes en el móvil (SharedPreferences en JSON). */
public final class AlarmStore {
    /** Avisa a las pantallas cuando cambian las alarmas o los ajustes. */
    public static final Signal changed = new Signal();

    private static SharedPreferences prefs;
    private static List<Alarm> alarms = new ArrayList<>();
    private static AppSettings settings = new AppSettings();

    private AlarmStore() {
    }

    public static synchronized void init(Context context) {
        if (prefs != null) return;
        prefs = context.getApplicationContext().getSharedPreferences("despertador", Context.MODE_PRIVATE);
        alarms = readAlarms();
        settings = readSettings();
    }

    /** Copia de la lista (en el orden en que se crearon). */
    public static synchronized List<Alarm> alarms() {
        List<Alarm> list = new ArrayList<>();
        for (Alarm a : alarms) list.add(a.copy());
        return list;
    }

    public static synchronized Alarm get(int id) {
        for (Alarm a : alarms) if (a.id == id) return a.copy();
        return null;
    }

    public static synchronized int newId() {
        int max = 0;
        for (Alarm a : alarms) max = Math.max(max, a.id);
        return max + 1;
    }

    public static void save(Alarm alarm) {
        synchronized (AlarmStore.class) {
            Alarm copy = alarm.copy();
            boolean found = false;
            for (int i = 0; i < alarms.size(); i++) {
                if (alarms.get(i).id == copy.id) {
                    alarms.set(i, copy);
                    found = true;
                }
            }
            if (!found) alarms.add(copy);
            writeAlarms();
        }
        changed.emit();
    }

    public static void delete(int id) {
        synchronized (AlarmStore.class) {
            alarms.removeIf(a -> a.id == id);
            writeAlarms();
        }
        changed.emit();
    }

    public static synchronized AppSettings settings() {
        return settings.copy();
    }

    public static void saveSettings(AppSettings s) {
        synchronized (AlarmStore.class) {
            settings = s.copy();
            try {
                prefs.edit().putString("settings", settings.toJson().toString()).apply();
            } catch (Exception ignored) {
            }
        }
        changed.emit();
    }

    private static void writeAlarms() {
        JSONArray arr = new JSONArray();
        try {
            for (Alarm a : alarms) arr.put(a.toJson());
        } catch (Exception ignored) {
        }
        prefs.edit().putString("alarms", arr.toString()).apply();
    }

    private static List<Alarm> readAlarms() {
        List<Alarm> list = new ArrayList<>();
        String text = prefs.getString("alarms", null);
        if (text == null) return list;
        try {
            JSONArray arr = new JSONArray(text);
            for (int i = 0; i < arr.length(); i++) list.add(Alarm.fromJson(arr.getJSONObject(i)));
        } catch (Exception ignored) {
        }
        return list;
    }

    private static AppSettings readSettings() {
        String text = prefs.getString("settings", null);
        if (text == null) return new AppSettings();
        try {
            return AppSettings.fromJson(new JSONObject(text));
        } catch (Exception e) {
            return new AppSettings();
        }
    }
}
