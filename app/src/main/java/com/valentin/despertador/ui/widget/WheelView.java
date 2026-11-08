package com.valentin.despertador.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.OverScroller;

import com.valentin.despertador.ui.Palette;
import com.valentin.despertador.ui.Ui;

import java.util.Locale;

/**
 * Rueda de números que gira sin fin (después del 59 viene el 00).
 * Se ven tres filas; la del centro es la elegida.
 */
public class WheelView extends View {
    public interface OnChange {
        void changed(int value);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final OverScroller scroller;
    private final GestureDetector gestures;

    private int count = 60;
    private boolean twoDigits = true;
    private float itemHeight;
    private float centerSize;
    private float sideSize;
    /** Posición en píxeles: el número del centro es round(scrollPx / itemHeight). */
    private float scrollPx;
    private int lastValue = -1;
    /** El dedo está sobre la rueda: no colocarla sola todavía. */
    private boolean touching;
    private OnChange listener;

    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setTextAlign(Paint.Align.CENTER);
        scroller = new OverScroller(context);
        setStyle(55, 20, 18);
        gestures = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                scroller.forceFinished(true);
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                scrollPx += dy;
                changedPosition();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                scroller.fling(0, Math.round(scrollPx), 0, Math.round(-vy), 0, 0,
                        Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2);
                postInvalidateOnAnimation();
                return true;
            }
        });
    }

    /** Alto de cada fila (dp) y tamaño de letra del centro y de los lados (sp). */
    public void setStyle(float itemHeightDp, float centerSp, float sideSp) {
        itemHeight = Ui.dp(getContext(), itemHeightDp);
        centerSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, centerSp, getResources().getDisplayMetrics());
        sideSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sideSp, getResources().getDisplayMetrics());
        requestLayout();
    }

    public void setRange(int count, boolean twoDigits) {
        this.count = count;
        this.twoDigits = twoDigits;
        invalidate();
    }

    public void setOnChange(OnChange listener) {
        this.listener = listener;
    }

    public void setValue(int value) {
        scroller.forceFinished(true);
        scrollPx = value * itemHeight;
        lastValue = value;
        invalidate();
    }

    public int getValue() {
        return Math.floorMod(Math.round(scrollPx / itemHeight), count);
    }

    private void changedPosition() {
        int v = getValue();
        if (v != lastValue) {
            lastValue = v;
            if (listener != null) listener.changed(v);
        }
        invalidate();
    }

    /** Al soltar, la rueda se coloca justo en el número más cercano. */
    private void snap() {
        float target = Math.round(scrollPx / itemHeight) * itemHeight;
        if (Math.abs(target - scrollPx) < 0.5f) {
            scrollPx = target;
            invalidate();
            return;
        }
        scroller.startScroll(0, Math.round(scrollPx), 0, Math.round(target - scrollPx), 200);
        postInvalidateOnAnimation();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            touching = true;
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        gestures.onTouchEvent(e);
        if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
            touching = false;
            if (scroller.isFinished()) snap();
        }
        return true;
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollPx = scroller.getCurrY();
            changedPosition();
            if (scroller.isFinished()) {
                if (!touching) snap();
            } else {
                postInvalidateOnAnimation();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED
                ? Ui.dp(getContext(), 64) : MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, Math.round(itemHeight * 3));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float centerY = getHeight() / 2f;
        float position = scrollPx / itemHeight;
        int first = (int) Math.floor(position) - 2;
        for (int i = first; i <= first + 4; i++) {
            float distance = Math.abs(i - position);
            if (distance > 1.6f) continue;
            float y = centerY + (i - position) * itemHeight;
            boolean selected = distance < 0.5f;
            paint.setTextSize(selected ? centerSize : sideSize);
            paint.setColor(selected ? Palette.TEXT_MAIN : Palette.TEXT_FAINT);
            int value = Math.floorMod(i, count);
            String text = twoDigits ? String.format(Locale.ROOT, "%02d", value) : String.valueOf(value);
            Paint.FontMetrics fm = paint.getFontMetrics();
            canvas.drawText(text, getWidth() / 2f, y - (fm.ascent + fm.descent) / 2, paint);
        }
    }
}
