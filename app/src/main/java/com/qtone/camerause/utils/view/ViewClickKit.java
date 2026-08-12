package com.qtone.camerause.utils.view;

import android.os.SystemClock;
import android.view.View;

import androidx.annotation.NonNull;

import com.qtone.camerause.R;

/**
 * @decs: 视图点击配套原件
 * @author: 郑少鹏
 * @date: 2025/9/16 11:42
 * @version: v 1.0
 */
public class ViewClickKit {
    /**
     * 是否是快速点击
     * <p>
     * 间隔 1200 毫秒
     *
     * @param view 视图
     * @return 是否是快速点击
     */
    public static boolean isFastClick(@NonNull View view) {
        // 当前时间
        // Android 官方用于计算时间间隔的标准方案
        long currentTime = SystemClock.elapsedRealtime();
        Object tag = view.getTag(R.id.view_click_time);
        long lastClickTime = (tag instanceof Long) ? (Long) tag : 0L;
        if ((currentTime - lastClickTime) < 1200) {
            return true;
        }
        view.setTag(R.id.view_click_time, currentTime);
        return false;
    }

    /**
     * 单击
     *
     * @param view            视图
     * @param intervalMs      间隔毫秒
     * @param onClickListener 点击监听
     */
    public static void singleClick(@NonNull final View view, final long intervalMs, final View.OnClickListener onClickListener) {
        view.setOnClickListener(new View.OnClickListener() {
            private long lastClickTime = 0;

            @Override
            public void onClick(View v) {
                // 当前时间
                // Android 官方用于计算时间间隔的标准方案
                long currentTime = SystemClock.elapsedRealtime();
                if (currentTime - lastClickTime < intervalMs) {
                    return;
                }
                lastClickTime = currentTime;
                if (onClickListener != null) {
                    onClickListener.onClick(v);
                }
            }
        });
    }

    /**
     * 双击
     *
     * @param view                  视图
     * @param onDoubleClickListener 双击监听
     */
    public static void doubleClick(@NonNull View view, @NonNull OnDoubleClickListener onDoubleClickListener) {
        view.setOnClickListener(new View.OnClickListener() {
            private static final long DOUBLE_CLICK_INTERVAL = 300;
            private long lastClickTime;

            @Override
            public void onClick(View v) {
                // 当前时间
                // Android 官方用于计算时间间隔的标准方案
                long currentTime = SystemClock.elapsedRealtime();
                if ((currentTime - lastClickTime) < DOUBLE_CLICK_INTERVAL) {
                    onDoubleClickListener.onDoubleClick();
                }
                lastClickTime = currentTime;
            }
        });
    }

    /**
     * 双击监听
     */
    public interface OnDoubleClickListener {
        /**
         * 双击
         */
        void onDoubleClick();
    }
}