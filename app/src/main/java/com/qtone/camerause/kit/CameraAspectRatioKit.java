package com.qtone.camerause.kit;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.Nullable;

import com.jiangdg.ausbc.widget.AspectRatioTextureView;
import com.qtone.camerause.kit.log.LogKit;

/**
 * Created on 2026/8/4.
 *
 * @author 郑少鹏
 * @desc 相机宽高比配套原件
 */
public class CameraAspectRatioKit {
    /**
     * 当前物理帧宽
     */
    private int currentWidth = -1;
    /**
     * 当前物理帧高
     */
    private int currentHeight = -1;
    /**
     * AspectRatioTextureView
     */
    private AspectRatioTextureView aspectRatioTextureView;

    /**
     * constructor
     *
     * @param aspectRatioTextureView AspectRatioTextureView
     */
    public CameraAspectRatioKit(AspectRatioTextureView aspectRatioTextureView) {
        this.aspectRatioTextureView = aspectRatioTextureView;
    }

    /**
     * 更新宽高比
     *
     * @param activity 活动
     * @param width    物理帧宽
     * @param height   物理帧高
     */
    public void updateAspectRatio(@Nullable Activity activity, int width, int height) {
        if ((activity == null) || (aspectRatioTextureView == null) || (width <= 0) || (height <= 0)) {
            return;
        }
        // 分辨率变化时更新 -> 规避高频触发 requestLayout() 导致卡顿
        if ((currentWidth != width) || (currentHeight != height)) {
            currentWidth = width;
            currentHeight = height;
            float ratio = (float) width / (float) height;
            Log.d(LogKit.TAG, String.format("预览区更新宽高比 || %d:%d (宽高比 %.2f)", width, height, ratio));
            activity.runOnUiThread(() -> {
                if (!activity.isFinishing() && !activity.isDestroyed() && (aspectRatioTextureView != null)) {
                    aspectRatioTextureView.setAspectRatio(width, height);
                }
            });
        }
    }

    /**
     * 重置
     * <p>
     * 相机重启时调
     * 切换设备时调
     * <p>
     * 重置缓存的分辨率记录
     */
    public void reset() {
        this.currentWidth = -1;
        this.currentHeight = -1;
    }

    /**
     * 释放
     */
    public void release() {
        this.currentWidth = -1;
        this.currentHeight = -1;
        this.aspectRatioTextureView = null;
    }
}