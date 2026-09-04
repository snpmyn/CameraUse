package com.qtone.camerause.util.data;

/**
 * Created on 2025/10/3.
 *
 * @author 郑少鹏
 * @desc DoubleUtils
 */
public class DoubleUtils {
    /**
     * double 转 float
     * <p>
     * 超 float 范围返 Float.MAX_VALUE 或 Float.MIN_VALUE
     *
     * @param value double
     * @return float
     */
    public static float doubleToFloat(double value) {
        if (value > Float.MAX_VALUE) {
            return Float.MAX_VALUE;
        } else if (value < -Float.MAX_VALUE) {
            return -Float.MAX_VALUE;
        } else if ((value > 0) && (value < Float.MIN_VALUE)) {
            // 非零最小正数
            return Float.MIN_VALUE;
        } else if ((value < 0) && (value > -Float.MIN_VALUE)) {
            // 非零最小负数
            return -Float.MIN_VALUE;
        } else {
            // 正常范围内直接强制转换
            return (float) value;
        }
    }
}