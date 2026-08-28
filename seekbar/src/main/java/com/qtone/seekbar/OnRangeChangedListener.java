package com.qtone.seekbar;

/**
 * ================================================
 * 作    者：JayGoo
 * 版    本：
 * 创建日期：2018/5/8
 * 描    述:
 * ================================================
 */
public interface OnRangeChangedListener {
    default void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {

    }

    default void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {

    }

    default void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {

    }
}