package com.valentin.despertador.util;

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Aviso sencillo: las pantallas se apuntan y se les llama (en el hilo principal) cuando algo cambia. */
public class Signal {
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final Handler main = new Handler(Looper.getMainLooper());

    public void add(Runnable r) {
        listeners.add(r);
    }

    public void remove(Runnable r) {
        listeners.remove(r);
    }

    public void emit() {
        main.post(() -> {
            for (Runnable r : listeners) r.run();
        });
    }
}
