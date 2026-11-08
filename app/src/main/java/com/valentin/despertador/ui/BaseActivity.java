package com.valentin.despertador.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/** Pantalla con el fondo y los colores del diseño elegido. Si se cambia de diseño, se vuelve a dibujar. */
public abstract class BaseActivity extends AppCompatActivity {
    protected Palette palette;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        palette = Palette.current();
        Ui.edgeToEdge(getWindow());
        getWindow().setBackgroundDrawable(palette.background());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Palette.current().id.equals(palette.id)) recreate();
    }
}
