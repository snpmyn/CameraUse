package com.qtone.camerause.util.systembar;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Created on 2026/6/15.
 *
 * @author 郑少鹏
 * @desc 系统栏配套原件
 */
public class SystemBarKit {
    /**
     * 隐藏导航栏
     *
     * @param activity 活动
     */
    public static void hideNavigationBar(Activity activity) {
        if ((activity == null) || (activity.getWindow() == null)) {
            return;
        }
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            // 新版 API 隐藏
            WindowCompat.setDecorFitsSystemWindows(window, false);
            WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
            controller.hide(WindowInsetsCompat.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } else {
            // Android 10-
            // 经典 Flag 方案隐藏
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            window.getDecorView().setSystemUiVisibility(uiOptions);
        }
    }

    /**
     * 显示导航栏
     *
     * @param activity 活动
     */
    public static void showNavigationBar(Activity activity) {
        if ((activity == null) || (activity.getWindow() == null)) {
            return;
        }
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            // 核心修复点，让布局恢复服从系统窗口限制，缩回排版。
            WindowCompat.setDecorFitsSystemWindows(window, true);
            WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
            controller.show(WindowInsetsCompat.Type.navigationBars());
        } else {
            // Android 10-
            // 清除隐藏和全屏延伸标志
            int uiOptions = window.getDecorView().getSystemUiVisibility();
            uiOptions &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            uiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            // 确保老版本也清理掉布局延伸标志
            uiOptions &= ~View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            window.getDecorView().setSystemUiVisibility(uiOptions);
        }
    }

    /**
     * 隐藏状态栏 + 导航栏
     * <p>
     * 全屏沉浸
     *
     * @param activity 活动
     */
    public static void hideSystemBars(Activity activity) {
        if ((activity == null) || (activity.getWindow() == null)) {
            return;
        }
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            WindowCompat.setDecorFitsSystemWindows(window, false);
            WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } else {
            // Android 10-
            // 经典全屏 + 隐藏导航栏 + 粘性沉浸
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            window.getDecorView().setSystemUiVisibility(uiOptions);
        }
    }

    /**
     * 显示状态栏 + 导航栏
     * <p>
     * 退出全屏恢复
     *
     * @param activity 活动
     */
    public static void showSystemBars(Activity activity) {
        if ((activity == null) || (activity.getWindow() == null)) {
            return;
        }
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            // 核心修复点，让布局恢复服从系统窗口限制，避免重叠遮挡。
            WindowCompat.setDecorFitsSystemWindows(window, true);
            WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
            controller.show(WindowInsetsCompat.Type.systemBars());
        } else {
            // Android 10-
            // 恢复全量可见状态 (会自动清空所有 LAYOUT_FULLSCREEN 等沉浸式配置)
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }
}