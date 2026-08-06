package com.qtone.camerause.base;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.base.CameraActivity;
import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.jiangdg.ausbc.camera.bean.CameraRequest;
import com.jiangdg.ausbc.render.env.RotateType;
import com.jiangdg.ausbc.widget.AspectRatioTextureView;
import com.jiangdg.ausbc.widget.IAspectRatio;
import com.qtone.camerause.kit.CameraAspectRatioKit;
import com.qtone.camerause.value.CameraResolution;

import org.jetbrains.annotations.NotNull;

/**
 * Created on 2026/8/5.
 *
 * @author 郑少鹏
 * @desc 相机基类
 */
public abstract class BaseCameraActivity extends CameraActivity {
    private static final String TAG = BaseCameraActivity.class.getSimpleName();
    /**
     * 容器
     */
    protected ViewGroup container;
    /**
     * AspectRatioTextureView
     */
    protected AspectRatioTextureView aspectRatioTextureView;
    /**
     * 相机宽高比配套原件
     */
    protected CameraAspectRatioKit cameraAspectRatioKit;
    /**
     * 预览数据回调
     */
    private final IPreviewDataCallBack previewDataCallBack = new IPreviewDataCallBack() {
        @Override
        public void onPreviewData(@org.jetbrains.annotations.Nullable byte[] data, int width, int height, @NotNull DataFormat format) {
            // 预览区域动态适配
            if (cameraAspectRatioKit != null) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed() && (cameraAspectRatioKit != null)) {
                        cameraAspectRatioKit.updateAspectRatio(BaseCameraActivity.this, width, height);
                    }
                });
            }
            // 实时分发原始数据
            if (data != null) {
                onPreviewFrame(data, width, height, format);
            }
        }
    };

    @Override
    protected void onCreate(@org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        // 触发 ausbc 底层相机生命周期绑定
        super.onCreate(savedInstanceState);
        // 开始逻辑
        startLogic(savedInstanceState);
    }

    @Override
    protected void initView() {
        super.initView();
        // 控件
        container = findViewById(getContainerId());
        aspectRatioTextureView = findViewById(getTextureViewId());
        if (aspectRatioTextureView != null) {
            // 相机宽高比配套原件
            cameraAspectRatioKit = new CameraAspectRatioKit(aspectRatioTextureView);
        }
    }

    @org.jetbrains.annotations.Nullable
    @Override
    protected View getRootView(@NotNull LayoutInflater layoutInflater) {
        int layoutId = getLayoutId();
        if (layoutId != 0) {
            return layoutInflater.inflate(layoutId, null);
        }
        return null;
    }

    /**
     * 获取布局 ID
     *
     * @return 布局 ID
     */
    @LayoutRes
    protected abstract int getLayoutId();

    /**
     * 获取容器 ID
     *
     * @return 容器 ID
     */
    protected abstract int getContainerId();

    /**
     * 获取 TextureView ID
     *
     * @return TextureView ID
     */
    protected abstract int getTextureViewId();

    /**
     * 获取相机分辨率
     *
     * @return 相机分辨率
     */
    @NonNull
    protected abstract CameraResolution getCustomResolution();

    /**
     * 开始逻辑
     *
     * @param savedInstanceState Bundle
     */
    protected void startLogic(@Nullable Bundle savedInstanceState) {

    }

    /**
     * 预览帧
     *
     * @param data       相机底层输出的原始 NV21 / RGBA 字节数组
     * @param width      帧物理宽
     * @param height     帧物理高
     * @param dataFormat 数据格式
     */
    protected void onPreviewFrame(byte[] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat) {

    }

    @Nullable
    @Override
    protected IAspectRatio getCameraView() {
        // 提供相机渲染组件
        return aspectRatioTextureView;
    }

    @Nullable
    @Override
    protected ViewGroup getCameraViewContainer() {
        // 提供相机渲染容器
        return container;
    }

    /**
     * 获取相机请求参数
     *
     * @return 相机请求参数
     */
    @NonNull
    @Override
    protected CameraRequest getCameraRequest() {
        CameraResolution resolution = getCustomResolution();
        return new CameraRequest.Builder()
                .setPreviewWidth(resolution.getWidth())
                .setPreviewHeight(resolution.getHeight())
                // NORMAL - NV21
                // 效率较 OPENGL - RGBA 更高
                .setRenderMode(CameraRequest.RenderMode.NORMAL)
                .setDefaultRotateType(RotateType.ANGLE_0)
                .setAspectRatioShow(true)
                // 硬件开启预览帧数据输出
                .setRawPreviewData(true)
                .create();
    }

    @CallSuper
    @Override
    public void onCameraState(@NotNull MultiCameraClient.ICamera self, @NotNull State code, @Nullable String msg) {
        if (code == State.OPENED) {
            Log.d(TAG, "相机打开成功");
            CameraResolution customResolution = getCustomResolution();
            if (cameraAspectRatioKit != null) {
                // 相机打开成功 -> 按默认分辨率设置预览区域比例
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed() && cameraAspectRatioKit != null) {
                        cameraAspectRatioKit.updateAspectRatio(this, customResolution.getWidth(), customResolution.getHeight());
                    }
                });
            }
            // 清除已有预览帧回调
            self.removePreviewDataCallBack(previewDataCallBack);
            // 重新注册预览帧回调
            self.addPreviewDataCallBack(previewDataCallBack);
        } else if (code == State.CLOSED) {
            Log.d(TAG, "相机关闭成功");
            // 清除已有预览帧回调
            self.removePreviewDataCallBack(previewDataCallBack);
            if (cameraAspectRatioKit != null) {
                // 重置缓存的分辨率记录
                cameraAspectRatioKit.reset();
            }
        } else if (code == State.ERROR) {
            Log.e(TAG, "相机打开错误 || " + msg);
            self.removePreviewDataCallBack(previewDataCallBack);
        }
    }

    @Override
    protected void onDestroy() {
        MultiCameraClient.ICamera iCamera = getCurrentCamera();
        if (iCamera != null) {
            iCamera.removePreviewDataCallBack(previewDataCallBack);
        }
        if (cameraAspectRatioKit != null) {
            cameraAspectRatioKit.release();
            cameraAspectRatioKit = null;
        }
        super.onDestroy();
    }
}