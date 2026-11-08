package com.valentin.despertador.data;

import com.valentin.despertador.audio.Tracks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Ajustes generales de la app (pantalla Configuración). */
public class AppSettings {
    public int durationMin = 30;
    public int voiceIntervalSec = 15;
    public int snoozeMin = 3;
    public int quickMinutes = 8 * 60 + 30;
    public String music = Tracks.DEFAULT;
    /** Vacío = la voz por defecto del móvil */
    public List<String> voices = new ArrayList<>();
    public float musicVolume = 1f;
    public float voiceVolume = 1f;
    public String theme = "cian";

    public AppSettings copy() {
        try {
            return fromJson(toJson());
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("durationMin", durationMin)
                .put("voiceIntervalSec", voiceIntervalSec)
                .put("snoozeMin", snoozeMin)
                .put("quickMinutes", quickMinutes)
                .put("music", music)
                .put("voices", new JSONArray(voices))
                .put("musicVolume", (double) musicVolume)
                .put("voiceVolume", (double) voiceVolume)
                .put("theme", theme);
    }

    public static AppSettings fromJson(JSONObject o) throws JSONException {
        AppSettings s = new AppSettings();
        s.durationMin = o.optInt("durationMin", s.durationMin);
        s.voiceIntervalSec = o.optInt("voiceIntervalSec", s.voiceIntervalSec);
        s.snoozeMin = o.optInt("snoozeMin", s.snoozeMin);
        s.quickMinutes = o.optInt("quickMinutes", s.quickMinutes);
        s.music = o.optString("music", s.music);
        JSONArray v = o.optJSONArray("voices");
        if (v != null) s.voices = Alarm.strings(v);
        s.musicVolume = (float) o.optDouble("musicVolume", 1.0);
        s.voiceVolume = (float) o.optDouble("voiceVolume", 1.0);
        s.theme = o.optString("theme", s.theme);
        return s;
    }
}
