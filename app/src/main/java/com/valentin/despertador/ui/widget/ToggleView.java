package com.valentin.despertador.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.valentin.despertador.ui.Palette;
import com.valentin.despertador.ui.Ui;

/** Interruptor con la bola grande que sobresale del carril, como en la app original. */
public class ToggleView extends View {
    public interface OnChange {
        void changed(boolean checked);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int accent = Palette.current().accent;
    private boolean checked;
    /** 0 = apagado, 1 = encendido (para la animación) */
    private float position;
    private OnChange listener;

    public ToggleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(v -> setChecked(!checked, true));
    }

    public void setOnChange(OnChange listener) {
        this.listener = listener;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean value) {
        checked = value;
        position = value ? 1 : 0;
        invalidate();
    }

    private void setChecked(boolean value, boolean fromUser) {
        checked = value;
        ValueAnimator anim = ValueAnimator.ofFloat(position, value ? 1 : 0).setDuration(150);
        anim.addUpdateListener(a -> {
            position = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
        if (fromUser && listener != null) listener.changed(value);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(Ui.dp(getContext(), 48), Ui.dp(getContext(), 32));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Context c = getContext();
        float cy = getHeight() / 2f;
        float trackH = Ui.dp(c, 18);
        rect.set(Ui.dp(c, 5), cy - trackH / 2, Ui.dp(c, 43), cy + trackH / 2);
        paint.setColor(checked ? (accent & 0x00FFFFFF) | 0x8C000000 : 0xFF7A8090);
        canvas.drawRoundRect(rect, trackH / 2, trackH / 2, paint);
        float r = Ui.dp(c, 13);
        float x = r + position * Ui.dp(c, 22);
        paint.setColor(checked ? accent : 0xFFB4B8C4);
        canvas.drawCircle(x, cy, r, paint);
    }
}
