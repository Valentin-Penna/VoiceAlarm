package com.valentin.despertador.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.valentin.despertador.ui.Palette;
import com.valentin.despertador.ui.Ui;

/** Deslizador de línea fina con bolita (volúmenes). Valor de 0 a 1. */
public class LineSlider extends View {
    public interface OnChange {
        void changed(float value);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int accent = Palette.current().accent;
    private float value = 1f;
    private OnChange listener;

    public LineSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setValue(float v) {
        value = v;
        invalidate();
    }

    public void setOnChange(OnChange listener) {
        this.listener = listener;
    }

    private float thumb() {
        return Ui.dp(getContext(), 10);
    }

    private float start() {
        return thumb() + Ui.dp(getContext(), 12);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Ui.dp(getContext(), 40));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) getParent().requestDisallowInterceptTouchEvent(true);
        float end = getWidth() - thumb();
        value = Math.max(0f, Math.min(1f, (e.getX() - start()) / (end - start())));
        if (listener != null) listener.changed(value);
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float y = getHeight() / 2f;
        float start = start();
        float end = getWidth() - thumb();
        float x = start + (end - start) * value;
        paint.setStrokeWidth(Ui.dp(getContext(), 3));
        paint.setColor(Palette.TEXT_FAINT);
        canvas.drawLine(start, y, end, y, paint);
        paint.setColor(accent);
        canvas.drawLine(start, y, x, y, paint);
        canvas.drawCircle(x, y, thumb(), paint);
    }
}
