package com.qtone.camerause.widget.button;

/**
 * Created on 2026/9/1.
 *
 * @author 郑少鹏
 * @desc 拍照按钮形状
 */
public enum CaptureButtonShape {
    /**
     * 圆形
     */
    CIRCLE(0),
    /**
     * 方形
     */
    SQUARE(1);
    /**
     * 值
     */
    final int value;

    /**
     * constructor
     *
     * @param value 值
     */
    CaptureButtonShape(int value) {
        this.value = value;
    }

    /**
     * 从值
     *
     * @param val 变量
     * @return 拍照按钮形状
     */
    static CaptureButtonShape fromValue(int val) {
        for (CaptureButtonShape captureButtonShape : values()) {
            if (captureButtonShape.value == val) {
                return captureButtonShape;
            }
        }
        return CIRCLE;
    }
}