package com.valentin.despertador.ui.widget;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.core.content.ContextCompat;

import com.valentin.despertador.R;
import com.valentin.despertador.ui.Palette;
import com.valentin.despertador.ui.Ui;

/** Hay que arrastrar la bola hasta el final para apagar (así no se apaga sin querer). */
public class SlideToConfirmView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Palette p = Palette.current();
    private final Drawable arrow;
    private String text = "DESLIZA PARA APAGAR";
    private float offset;
    private float touchStart;
    private boolean dragging;
    private boolean done;
    private Runnable onConfirm;

    public SlideToConfirmView(Context context, AttributeSet attrs) {
        super(context, attrs);
        arrow = ContextCompat.getDrawable(context, R.drawable.ic_arrow).mutate();
        arrow.setTint(p.onAccent());
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 15, getResources().getDisplayMetrics()));
        paint.setLetterSpacing(0.16f);
    }

    public void setText(String text) {
        this.text = text.toUpperCase();
        invalidate();
    }

    public void setOnConfirm(Runnable onConfirm) {
        this.onConfirm = onConfirm;
    }

    private float knob() {
        return Ui.dp(getContext(), 58);
    }

    private float pad() {
        return Ui.dp(getContext(), 5);
    }

    private float maxOffset() {
        return Math.max(1f, getWidth() - knob() - 2 * pad());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Math.round(knob() + 2 * pad()));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (done) return true;
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Solo se arrastra cogiendo la bola
                dragging = e.getX() <= offset + knob() + 2 * pad();
                touchStart = e.getX() - offset;
                if (dragging) getParent().requestDisallowInterceptTouchEvent(true);
                return dragging;
            case MotionEvent.ACTION_MOVE:
                offset = Math.max(0, Math.min(maxOffset(), e.getX() - touchStart));
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                if (offset > maxOffset() * 0.85f) {
                    done = true;
                    animateTo(maxOffset(), null);
                    if (onConfirm != null) onConfirm.run();
                } else {
                    animateTo(0, new OvershootInterpolator(1.5f));
                }
                return true;
        }
        return false;
    }

    private void animateTo(float target, OvershootInterpolator interpolator) {
        ValueAnimator anim = ValueAnimator.ofFloat(offset, target).setDuration(250);
        if (interpolator != null) anim.setInterpolator(interpolator);
        anim.addUpdateListener(a -> {
            offset = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float h = getHeight();
        float r = h / 2;
        // Carril
        rect.set(0, 0, getWidth(), h);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(p.card);
        canvas.drawRoundRect(rect, r, r, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Ui.dp(getContext(), 1));
        paint.setColor(p.border);
        rect.inset(0.5f, 0.5f);
        canvas.drawRoundRect(rect, r, r, paint);
        // Rastro de color que deja la bola
        paint.setStyle(Paint.Style.FILL);
        paint.setColor((p.accent & 0x00FFFFFF) | 0x2E000000);
        rect.set(0, 0, offset + knob() + 2 * pad(), h);
        canvas.drawRoundRect(rect, r, r, paint);
        // Texto, que se desvanece al arrastrar
        float progress = Math.min(1f, offset / maxOffset());
        paint.setColor(Palette.TEXT_MAIN);
        paint.setAlpha(Math.round(255 * (1 - progress)));
        Paint.FontMetrics fm = paint.getFontMetrics();
        float textCenter = knob() + (getWidth() - knob()) / 2f;
        canvas.drawText(text, textCenter, h / 2 - (fm.ascent + fm.descent) / 2, paint);
        paint.setAlpha(255);
        // Bola con la flecha
        float cx = pad() + offset + knob() / 2;
        paint.setColor(p.accent);
        canvas.drawCircle(cx, h / 2, knob() / 2, paint);
        int s = Ui.dp(getContext(), 26);
        arrow.setBounds(Math.round(cx - s / 2f), Math.round(h / 2 - s / 2f), Math.round(cx + s / 2f), Math.round(h / 2 + s / 2f));
        arrow.draw(canvas);
    }
}
