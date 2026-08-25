package com.qtone.camerause.util.density;

import android.content.Context;
import android.content.res.Resources;

import org.jetbrains.annotations.NotNull;

/**
 * Created on 2017/11/17.
 *
 * @author 郑少鹏
 * @desc DensityUtils
 */
@SuppressWarnings("unused")
public class DensityUtils {
    /**
     * 设备独立像素转像素
     *
     * @param context 上下文
     * @param dip     设备独立像素
     * @return 像素
     */
    public static int dipToPxByFloat(@NotNull Context context, float dip) {
        return (int) ((dip * context.getResources().getDisplayMetrics().density) + 0.5F);
    }

    /**
     * 设备独立像素转像素
     *
     * @param dip 设备独立像素
     * @return 像素
     */
    public static int dipToPxByInt(int dip) {
        return (int) (dip * Resources.getSystem().getDisplayMetrics().density);
    }

    /**
     * 像素转设备独立像素
     *
     * @param context 上下文
     * @param px      像素
     * @return 设备独立像素
     */
    public static int pxToDip(@NotNull Context context, float px) {
        return (int) ((px / context.getResources().getDisplayMetrics().density) + 0.5F);
    }

    /**
     * px 转 sp
     *
     * @param context 上下文
     * @param px      px
     * @return sp
     */
    public static int pxToSp(@NotNull Context context, float px) {
        return (int) ((px / context.getResources().getDisplayMetrics().scaledDensity) + 0.5F);
    }

    /**
     * sp 转 px
     *
     * @param context 上下文
     * @param sp      sp
     * @return px
     */
    public static int spToPx(@NotNull Context context, float sp) {
        return (int) ((sp * context.getResources().getDisplayMetrics().scaledDensity) + 0.5F);
    }

    /**
     * 获取逻辑 DPI
     * <p>
     * 系统归一化 DPI
     * 如 120, 160, 240, 320, 480, 640 等
     *
     * @param context 上下文
     * @return 逻辑 DPI
     */
    public static int getDensityDpi(@NotNull Context context) {
        return context.getResources().getDisplayMetrics().densityDpi;
    }

    /**
     * 获取系统逻辑 DPI
     *
     * @return 系统逻辑 DPI
     */
    public static int getSystemDensityDpi() {
        return Resources.getSystem().getDisplayMetrics().densityDpi;
    }

    /**
     * 获取 X 轴 DPI
     *
     * @param context 上下文
     * @return X 轴 DPI
     */
    public static float getDpiOnX(@NotNull Context context) {
        return context.getResources().getDisplayMetrics().xdpi;
    }

    /**
     * 获取 Y 轴 DPI
     *
     * @param context 上下文
     * @return Y 轴 DPI
     */
    public static float getDpiOnY(@NotNull Context context) {
        return context.getResources().getDisplayMetrics().ydpi;
    }
}