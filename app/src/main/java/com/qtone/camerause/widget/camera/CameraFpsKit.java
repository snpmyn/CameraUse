package com.qtone.camerause.widget.camera;

import android.os.SystemClock;

/**
 * Created on 2026/9/5.
 *
 * @author 郑少鹏
 * @desc 相机帧率配套原件
 */
@SuppressWarnings("unused")
public class CameraFpsKit {
    /**
     * 间隔毫秒
     */
    private final long intervalMs;
    /**
     * 帧数量
     */
    private int frameCount = 0;
    /**
     * 上次计算时间戳毫秒
     */
    private long lastCalculateTimestampMs = 0L;
    /**
     * 当前相机帧率
     * <p>
     * 使用 volatile 保证多线程读写可见性
     */
    private volatile float currentCameraFps = 0.0f;
    /**
     * 相机帧率回调
     */
    private OnCameraFpsCallback onCameraFpsCallback;

    /**
     * constructor
     *
     * @param onCameraFpsCallback 相机帧率回调
     */
    public CameraFpsKit(OnCameraFpsCallback onCameraFpsCallback) {
        this(1000, onCameraFpsCallback);
    }

    /**
     * constructor
     *
     * @param intervalMs          间隔毫秒
     *                            建议 500ms ~ 1000ms
     * @param onCameraFpsCallback 相机帧率回调
     */
    public CameraFpsKit(long intervalMs, OnCameraFpsCallback onCameraFpsCallback) {
        // 间隔毫秒
        this.intervalMs = intervalMs;
        // 相机帧率回调
        this.onCameraFpsCallback = onCameraFpsCallback;
    }

    /**
     * 统计帧数
     */
    public void countFrame() {
        frameCount++;
        long now = SystemClock.elapsedRealtime();
        if (lastCalculateTimestampMs == 0L) {
            lastCalculateTimestampMs = now;
            return;
        }
        long diff = (now - lastCalculateTimestampMs);
        if (diff >= intervalMs) {
            currentCameraFps = (frameCount * 1000.0f) / diff;
            frameCount = 0;
            lastCalculateTimestampMs = now;
            // 当帧率更新时自动触发回调
            if (onCameraFpsCallback != null) {
                onCameraFpsCallback.onCameraFpsChange(currentCameraFps);
            }
        }
    }

    /**
     * 获取当前相机帧率
     *
     * @return 当前相机帧率
     */
    public float getCurrentCameraFps() {
        return currentCameraFps;
    }

    /**
     * 重置
     */
    public void reset() {
        frameCount = 0;
        lastCalculateTimestampMs = 0L;
        currentCameraFps = 0.0f;
    }

    /**
     * 设置相机帧率回调
     *
     * @param onCameraFpsCallback 相机帧率回调
     */
    public void setOnCameraFpsCallback(OnCameraFpsCallback onCameraFpsCallback) {
        this.onCameraFpsCallback = onCameraFpsCallback;
    }

    /**
     * 相机帧率回调
     */
    public interface OnCameraFpsCallback {
        /**
         * 相机帧率变化
         *
         * @param fps 帧率
         */
        void onCameraFpsChange(float fps);
    }
}