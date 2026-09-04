package com.qtone.camerause.util.layoutparams;

import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

/**
 * Created on 2025/9/11.
 *
 * @author 郑少鹏
 * @desc 布局参数工具类
 */
public class LayoutParamsUtils {
    /**
     * 设置视图布局参数
     * <p>
     * {@link ViewGroup.LayoutParams#MATCH_PARENT}
     * {@link ViewGroup.LayoutParams#FILL_PARENT}
     * {@link ViewGroup.LayoutParams#WRAP_CONTENT}
     *
     * @param view      视图
     * @param setWidth  设置宽否
     * @param width     宽
     * @param setHeight 设置高否
     * @param height    高
     */
    public static void setViewLayoutParams(@NonNull View view, boolean setWidth, int width, boolean setHeight, int height) {
        if (!setWidth && !setHeight) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (null == layoutParams) {
            // 为空先创默认 ViewGroup.LayoutParams
            layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        if (setWidth) {
            layoutParams.width = width;
        }
        if (setHeight) {
            layoutParams.height = height;
        }
        view.setLayoutParams(layoutParams);
    }

    /**
     * 设置窗口管理器布局参数
     * <p>
     * {@link WindowManager.LayoutParams#MATCH_PARENT}
     * {@link WindowManager.LayoutParams#FILL_PARENT}
     * {@link WindowManager.LayoutParams#WRAP_CONTENT}
     *
     * @param window    窗口
     * @param setWidth  设置宽否
     * @param width     宽
     * @param setHeight 设置高否
     * @param height    高
     */
    public static void setWindowManagerLayoutParams(Window window, boolean setWidth, int width, boolean setHeight, int height) {
        if (null == window) {
            return;
        }
        if (!setWidth && !setHeight) {
            return;
        }
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        if (setWidth) {
            layoutParams.width = width;
        }
        if (setHeight) {
            layoutParams.height = height;
        }
        window.setAttributes(layoutParams);
    }
}