package com.qtone.camerause.kit;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.Nullable;

import com.jiangdg.ausbc.widget.AspectRatioTextureView;

/**
 * Created on 2026/8/4.
 *
 * @author 郑少鹏
 * @desc 相机宽高比配套原件
 */
public class CameraAspectRatioKit {
    private static final String TAG = CameraAspectRatioKit.class.getSimpleName();
    /**
     * AspectRatioTextureView
     */
    private AspectRatioTextureView aspectRatioTextureView;
    /**
     * 当前物理帧宽度
     */
    private int currentWidth = -1;
    /**
     * 当前物理帧高度
     */
    private int currentHeight = -1;

    /**
     * 构造函数
     *
     * @param aspectRatioTextureView AspectRatioTextureView
     */
    public CameraAspectRatioKit(AspectRatioTextureView aspectRatioTextureView) {
        this.aspectRatioTextureView = aspectRatioTextureView;
    }

    /**
     * 更新宽高比
     * <p>
     * 根据相机抛出的实际帧宽高动态更新 AspectRatioTextureView 显示比例
     *
     * @param activity 用于切回主线程更新 UI
     * @param width    物理帧宽度
     * @param height   物理帧高度
     */
    public void updateAspectRatio(@Nullable Activity activity, int width, int height) {
        if ((width <= 0) || (height <= 0) || (aspectRatioTextureView == null) || (activity == null)) {
            return;
        }
        // 状态检查
        // 只有当分辨率确实发生变化时才更新布局，避免高频触发 requestLayout() 造成卡顿。
        if ((width != currentWidth) || (height != currentHeight)) {
            currentWidth = width;
            currentHeight = height;
            float ratio = (float) width / (float) height;
            Log.d(TAG, String.format("动态更新预览比例: %d:%d (宽高比: %.2f)", width, height, ratio));
            activity.runOnUiThread(() -> {
                // 确保 Activity 和 View 依然处于正常生命周期内
                if (!activity.isFinishing() && !activity.isDestroyed() && (aspectRatioTextureView != null)) {
                    aspectRatioTextureView.setAspectRatio(width, height);
                }
            });
        }
    }

    /**
     * 重置
     * <p>
     * 重置缓存的分辨率记录
     * 如在相机重启、切换设备时调用
     */
    public void reset() {
        this.currentWidth = -1;
        this.currentHeight = -1;
    }

    /**
     * 释放
     * <p>
     * 释放 View 引用
     * 防止 Fragment 销毁后子线程异步回调导致的内存泄漏
     */
    public void release() {
        this.aspectRatioTextureView = null;
        this.currentWidth = -1;
        this.currentHeight = -1;
    }
}