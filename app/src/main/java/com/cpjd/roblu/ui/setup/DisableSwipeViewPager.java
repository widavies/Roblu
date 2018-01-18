package com.cpjd.roblu.ui.setup;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class DisableSwipeViewPager extends ViewPager {

    public DisableSwipeViewPager(Context context) {
        super(context);
    }

    public DisableSwipeViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean swipeEnabled = false;
        boolean result = swipeEnabled && super.onTouchEvent(event);
        if(result) performClick();
        return result;
    }
    
    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public void goToNextPage() {
        if (getCurrentItem() < getAdapter().getCount() - 1) {
            setCurrentItem(getCurrentItem() + 1);
        }
    }

    public void goToPreviousPage() {
        if (getCurrentItem() > 0) {
            setCurrentItem(getCurrentItem() - 1);
        }
    }
}