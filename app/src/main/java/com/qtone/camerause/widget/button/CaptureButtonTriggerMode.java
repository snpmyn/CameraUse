package com.qtone.camerause.widget.button;

/**
 * Created on 2026/9/1.
 *
 * @author 郑少鹏
 * @desc 拍照按钮触发模式
 */
public enum CaptureButtonTriggerMode {
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
    CaptureButtonTriggerMode(int value) {
        this.value = value;
    }

    /**
     * 从值
     *
     * @param val 变量
     * @return 拍照按钮触发模式
     */
    static CaptureButtonTriggerMode fromValue(int val) {
        for (CaptureButtonTriggerMode captureButtonTriggerMode : values()) {
            if (captureButtonTriggerMode.value == val) {
                return captureButtonTriggerMode;
            }
        }
        return CLICK;
    }
}