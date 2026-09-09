package com.qtone.camerause.widget.button;

/**
 * Created on 2026/9/1.
 *
 * @author 郑少鹏
 * @desc 流光按钮形状
 */
public enum ShimmerButtonShape {
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
    ShimmerButtonShape(int value) {
        this.value = value;
    }

    /**
     * 从值
     *
     * @param val 变量
     * @return 流光按钮形状
     */
    static ShimmerButtonShape fromValue(int val) {
        for (ShimmerButtonShape shimmerButtonShape : values()) {
            if (shimmerButtonShape.value == val) {
                return shimmerButtonShape;
            }
        }
        return CIRCLE;
    }
}