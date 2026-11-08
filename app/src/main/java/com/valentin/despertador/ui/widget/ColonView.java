package com.valentin.despertador.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.valentin.despertador.ui.Palette;

/** Los dos puntos entre la rueda de las horas y la de los minutos. */
public class ColonView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ColonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Palette.TEXT_MAIN);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float r = getWidth() / 2f;
        float gap = r * 1.8f;
        float cy = getHeight() / 2f;
        canvas.drawCircle(r, cy - gap - r, r, paint);
        canvas.drawCircle(r, cy + gap + r, r, paint);
    }
}
