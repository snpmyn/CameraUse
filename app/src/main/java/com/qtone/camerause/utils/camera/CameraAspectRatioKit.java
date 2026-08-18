package com.qtone.camerause.utils.camera;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.Nullable;

import com.jiangdg.ausbc.widget.AspectRatioTextureView;
import com.qtone.camerause.utils.log.LogKit;
import com.qtone.camerause.utils.mmkv.MmkvKit;
import com.qtone.camerause.value.MmkvConstant;
import com.qtone.camerause.widget.MultiRoiOverlayView;

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
     * MultiRoiOverlayView
     */
    private MultiRoiOverlayView multiRoiOverlayView;

    /**
     * constructor
     *
     * @param aspectRatioTextureView AspectRatioTextureView
     * @param multiRoiOverlayView    MultiRoiOverlayView
     */
    public CameraAspectRatioKit(AspectRatioTextureView aspectRatioTextureView, MultiRoiOverlayView multiRoiOverlayView) {
        this.aspectRatioTextureView = aspectRatioTextureView;
        this.multiRoiOverlayView = multiRoiOverlayView;
    }

    /**
     * 更新宽高比
     *
     * @param activity 活动
     * @param width    物理帧宽
     * @param height   物理帧高
     */
    public void updateAspectRatio(@Nullable Activity activity, int width, int height) {
        if ((width <= 0) || (height <= 0)) {
            return;
        }
        // 分辨率变化时更新 -> 规避高频触发 requestLayout() 导致卡顿
        if ((currentWidth != width) || (currentHeight != height)) {
            // 当前物理帧宽
            currentWidth = width;
            // 当前物理帧高
            currentHeight = height;
            // 宽高比
            float aspectRatio = (float) width / (float) height;
            // 存储
            MmkvKit.INSTANCE.set(MmkvConstant.CAMERA_WIDTH, width);
            MmkvKit.INSTANCE.set(MmkvConstant.CAMERA_HEIGHT, height);
            MmkvKit.INSTANCE.set(MmkvConstant.CAMERA_ASPECT_RATIO, aspectRatio);
            assert activity != null;
            activity.runOnUiThread(() -> {
                if (aspectRatioTextureView != null) {
                    aspectRatioTextureView.setAspectRatio(width, height);
                    Log.d(LogKit.TAG, String.format("更新宽高比 - 预览区域 || %d:%d (宽高比 %.2f)", width, height, aspectRatio));
                }
                if (multiRoiOverlayView != null) {
                    multiRoiOverlayView.updateAspectRatio(width, height);
                    Log.d(LogKit.TAG, String.format("更新宽高比 - 多 ROI 覆盖视图 || %d:%d (宽高比 %.2f)", width, height, aspectRatio));
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
        this.multiRoiOverlayView = null;
    }
}