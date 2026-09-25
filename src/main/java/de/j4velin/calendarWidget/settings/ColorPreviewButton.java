package de.j4velin.calendarWidget.settings;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Small square showing a color, replaces the former external colorpicker library
 */
public class ColorPreviewButton extends View {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int color = Color.BLACK;

    public ColorPreviewButton(final Context context) {
        this(context, null);
    }

    public ColorPreviewButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(2);
        border.setColor(Color.GRAY);
        setClickable(true);
    }

    public void setColor(final int color) {
        this.color = color;
        invalidate();
    }

    public int getColor() {
        return color;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        fill.setColor(color);
        canvas.drawRect(0, 0, getWidth(), getHeight(), fill);
        canvas.drawRect(1, 1, getWidth() - 1, getHeight() - 1, border);
    }
}
