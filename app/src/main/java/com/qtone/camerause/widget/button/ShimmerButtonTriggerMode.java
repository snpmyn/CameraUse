package com.qtone.camerause.widget.button;

/**
 * Created on 2026/9/1.
 *
 * @author 郑少鹏
 * @desc 流光按钮触发模式
 */
public enum ShimmerButtonTriggerMode {
    /**
     * 普通点击触发
     * <p>
     * 点击立刻触发
     */
    CLICK(0),
    /**
     * 长按蓄力触发
     * <p>
     * 按住满圈触发 + 中途松手取消
     */
    LONG_PRESS_CHARGE(1);
    /**
     * 值
     */
    final int value;

    /**
     * constructor
     *
     * @param value 值
     */
    ShimmerButtonTriggerMode(int value) {
        this.value = value;
    }

    /**
     * 从值
     *
     * @param val 变量
     * @return 流光按钮触发模式
     */
    static ShimmerButtonTriggerMode fromValue(int val) {
        for (ShimmerButtonTriggerMode shimmerButtonTriggerMode : values()) {
            if (shimmerButtonTriggerMode.value == val) {
                return shimmerButtonTriggerMode;
            }
        }
        return CLICK;
    }
}