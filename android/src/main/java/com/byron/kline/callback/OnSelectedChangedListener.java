package com.byron.kline.callback;

import com.byron.kline.base.BaseKChartView;

/**
 * 选中点变化时的监听
 */
public interface OnSelectedChangedListener {
    /**
     * 当选点中变化时
     *
     * @param view  当前view
     * @param index 选中点的索引
     */
    void onSelectedChanged(BaseKChartView view, int index, float... values);
}