package de.j4velin.calendarWidget.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

public class SwipeableViewPager extends ViewPager {
    private boolean swipeable = true;

    public SwipeableViewPager(final Context context) {
        super(context);
    }

    public SwipeableViewPager(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSwipeable(boolean swipeable) {
        this.swipeable = swipeable;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent arg0) {
        try {
            return (this.swipeable) && super.onTouchEvent(arg0);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(final MotionEvent arg0) {
        try {
            return (this.swipeable) && super.onInterceptTouchEvent(arg0);
        } catch (Exception e) {
            return false;
        }
    }
}
