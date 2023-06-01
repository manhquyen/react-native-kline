package com.byron.kline.callback;

import android.animation.ValueAnimator;

import com.byron.kline.base.BaseKChartView;

public class PriceLabelInLineClickListener {

    public boolean onPriceLabelClick(BaseKChartView view) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(view.getTranslateX(),
                view.getMinTranslate());
        valueAnimator.setDuration(view.getDuration()).addUpdateListener(animation -> {
           view.changeTranslated((Float) animation.getAnimatedValue());
            view.animInvalidate();
        });
        valueAnimator.setRepeatCount(0);
        valueAnimator.start();
        view.setSelectedIndex(-1);
        return true;
    }
}
