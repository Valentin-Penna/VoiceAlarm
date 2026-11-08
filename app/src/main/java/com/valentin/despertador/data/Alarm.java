package com.valentin.despertador.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Una alarma. Se copia antes de cambiarla para no tocar la que está guardada. */
public class Alarm {
    public int id;
    public String name = "";
    public int hour = 7;
    public int minute = 0;
    /** Días de la semana: 1 = lunes … 7 = domingo. Vacío = suena una vez. */
    public TreeSet<Integer> days = new TreeSet<>();
    public boolean enabled = true;
    public String message = "";
    public boolean shakeToSnooze = false;
    /** null = la música de Ajustes */
    public String music = null;
    /** null = las voces de Ajustes */
    public List<String> voices = null;
    /** Alarma rápida: hora exacta en la que sonará solo la próxima vez */
    public Long quickAt = null;
    /** Sonido normal que la alarma rápida sustituye */
    public Long skipUntil = null;
    /** Hora a la que vuelve a sonar después de retrasarla */
    public Long snoozeAt = null;
    /** Veces que se ha retrasado en este despertar */
    public int snoozeCount = 0;
    /** Hasta cuándo se puede retrasar; después suena sin parar hasta apagarla */
    public Long ringUntil = null;

    public Alarm(int id) {
        this.id = id;
    }

    public Alarm copy() {
        try {
            return fromJson(toJson());
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Quita la alarma rápida y el retraso pendientes. */
    public Alarm withoutOverrides() {
        Alarm a = copy();
        a.quickAt = null;
        a.skipUntil = null;
        a.snoozeAt = null;
        a.snoozeCount = 0;
        a.ringUntil = null;
        return a;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject()
                .put("id", id)
                .put("name", name)
                .put("hour", hour)
                .put("minute", minute)
                .put("days", new JSONArray(days))
                .put("enabled", enabled)
                .put("message", message)
                .put("shake", shakeToSnooze)
                .put("music", music == null ? JSONObject.NULL : music)
                .put("voices", voices == null ? JSONObject.NULL : new JSONArray(voices))
                .put("quickAt", quickAt == null ? JSONObject.NULL : quickAt)
                .put("skipUntil", skipUntil == null ? JSONObject.NULL : skipUntil)
                .put("snoozeAt", snoozeAt == null ? JSONObject.NULL : snoozeAt)
                .put("snoozeCount", snoozeCount)
                .put("ringUntil", ringUntil == null ? JSONObject.NULL : ringUntil);
        return o;
    }

    public static Alarm fromJson(JSONObject o) throws JSONException {
        Alarm a = new Alarm(o.getInt("id"));
        a.name = o.optString("name");
        a.hour = o.optInt("hour", 7);
        a.minute = o.optInt("minute", 0);
        JSONArray d = o.optJSONArray("days");
        if (d != null) for (int i = 0; i < d.length(); i++) a.days.add(d.getInt(i));
        a.enabled = o.optBoolean("enabled", true);
        a.message = o.optString("message");
        a.shakeToSnooze = o.optBoolean("shake", false);
        a.music = o.isNull("music") ? null : o.optString("music", null);
        a.voices = o.isNull("voices") ? null : strings(o.optJSONArray("voices"));
        a.quickAt = optLong(o, "quickAt");
        a.skipUntil = optLong(o, "skipUntil");
        a.snoozeAt = optLong(o, "snoozeAt");
        a.snoozeCount = o.optInt("snoozeCount", 0);
        a.ringUntil = optLong(o, "ringUntil");
        return a;
    }

    static List<String> strings(JSONArray arr) throws JSONException {
        List<String> list = new ArrayList<>();
        if (arr != null) for (int i = 0; i < arr.length(); i++) list.add(arr.getString(i));
        return list;
    }

    private static Long optLong(JSONObject o, String key) {
        return !o.has(key) || o.isNull(key) ? null : o.optLong(key);
    }
}
