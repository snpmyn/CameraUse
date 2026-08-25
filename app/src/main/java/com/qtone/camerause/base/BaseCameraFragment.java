package com.qtone.camerause.base;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.base.CameraFragment;
import com.jiangdg.ausbc.callback.ICameraStateCallBack;
import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.jiangdg.ausbc.camera.bean.CameraRequest;
import com.jiangdg.ausbc.render.env.RotateType;
import com.jiangdg.ausbc.widget.AspectRatioTextureView;
import com.jiangdg.ausbc.widget.IAspectRatio;
import com.qtone.camerause.util.log.LogKit;
import com.qtone.camerause.value.CameraResolution;
import com.qtone.camerause.widget.camera.CameraAspectRatioKit;
import com.qtone.camerause.widget.camera.CameraController;
import com.qtone.camerause.widget.roi.MultiRoiOverlayView;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Created on 2026/8/11.
 *
 * @author 郑少鹏
 * @desc 相机碎片基类
 */
public abstract class BaseCameraFragment extends CameraFragment {
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
            safeRun(appCompatActivity -> appCompatActivity.runOnUiThread(() -> cameraAspectRatioKit.updateAspectRatio(appCompatActivity, width, height)));
            // 实时分发原始数据
            if (data != null) {
                onPreviewFrame(data, width, height, format);
            }
        }
    };

    /**
     * 获取布局 ID
     *
     * @return 布局 ID
     */
    protected abstract int getLayoutId();

    /**
     * 获取根视图
     *
     * @param inflater  布局加载器
     * @param container 容器
     * @return 根视图
     */
    @org.jetbrains.annotations.Nullable
    @Override
    protected View getRootView(@NotNull LayoutInflater inflater, @org.jetbrains.annotations.Nullable ViewGroup container) {
        return inflater.inflate(getLayoutId(), container, false);
    }

    /**
     * 获取 TextureView 容器
     *
     * @return TextureView 容器
     */
    protected abstract ViewGroup getTextureViewContainer();

    /**
     * 获取 TextureView
     *
     * @return TextureView
     */
    protected abstract AspectRatioTextureView getTextureView();

    /**
     * 获取 MultiRoiOverlayView
     *
     * @return MultiRoiOverlayView
     */
    protected abstract MultiRoiOverlayView getMultiRoiOverlayView();

    /**
     * 初始化组件
     *
     * @param rootView 根视图
     */
    protected abstract void initWidget(View rootView);

    /**
     * 初始化数据
     * <p>
     * 子类重写须调 super.initData()
     */
    @CallSuper
    @Override
    protected void initData() {
        super.initData();
        // 相机宽高比配套原件
        cameraAspectRatioKit = new CameraAspectRatioKit(getTextureView(), getMultiRoiOverlayView());
    }

    /**
     * 开始逻辑
     */
    protected abstract void startLogic();

    /**
     * 获取相机分辨率
     *
     * @return 相机分辨率
     */
    @NonNull
    protected abstract CameraResolution getCameraResolution();

    /**
     * 预览帧
     *
     * @param data       图像帧字节数组
     * @param width      帧物理宽
     * @param height     帧物理高
     * @param dataFormat 数据格式
     */
    protected void onPreviewFrame(byte[] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat) {

    }

    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, @org.jetbrains.annotations.Nullable ViewGroup container, @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        return getRootView(inflater, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initWidget(view);
        // 调用父类 CameraFragment 初始逻辑
        // 动态添加 TextureView 进容器并注册 UVC 监听
        initView();
        initData();
        startLogic();
    }

    @org.jetbrains.annotations.Nullable
    @Override
    protected IAspectRatio getCameraView() {
        // 提供相机渲染组件
        return getTextureView();
    }

    @org.jetbrains.annotations.Nullable
    @Override
    protected ViewGroup getCameraViewContainer() {
        // 提供相机渲染容器
        return getTextureViewContainer();
    }

    /**
     * 获取相机请求参数
     *
     * @return 相机请求参数
     */
    @NotNull
    @Override
    protected CameraRequest getCameraRequest() {
        /*return new CameraRequest.Builder()
                .setPreviewWidth(1280)
                .setPreviewHeight(720)
                .setRenderMode(CameraRequest.RenderMode.OPENGL)
                .setDefaultRotateType(RotateType.ANGLE_0)
                .setAudioSource(CameraRequest.AudioSource.SOURCE_SYS_MIC)
                .setAspectRatioShow(true)
                .setCaptureRawImage(false)
                .setRawPreviewData(false)
                .create();*/
        return new CameraRequest.Builder()
                .setPreviewWidth(getCameraResolution().getWidth())
                .setPreviewHeight(getCameraResolution().getHeight())
                // OPENGL - RGBA
                // RGBA: width * height * 4 Byte
                // NORMAL - NV21
                // NV21: width * height * 1.5 Byte
                .setRenderMode(CameraRequest.RenderMode.NORMAL)
                .setDefaultRotateType(RotateType.ANGLE_0)
                .setAspectRatioShow(true)
                // 硬件开启预览帧数据输出
                .setRawPreviewData(true)
                .create();
    }

    @Override
    public void onCameraState(@NotNull MultiCameraClient.ICamera self, @NotNull ICameraStateCallBack.State code, @org.jetbrains.annotations.Nullable String msg) {
        if (code == ICameraStateCallBack.State.OPENED) {
            Log.d(LogKit.TAG, "相机打开成功");
            // 自动对焦
            CameraController.getInstance().setAutoFocus(getCurrentCamera(), true);
            // 预览区域动态适配
            safeRun(appCompatActivity -> appCompatActivity.runOnUiThread(() -> cameraAspectRatioKit.updateAspectRatio(appCompatActivity, getCameraResolution().getWidth(), getCameraResolution().getHeight())));
            // 清除已有预览帧回调
            CameraController.getInstance().removePreviewDataCallBack(self, previewDataCallBack);
            // 重新注册预览帧回调
            CameraController.getInstance().addPreviewDataCallBack(self, previewDataCallBack);
        } else if (code == ICameraStateCallBack.State.CLOSED) {
            Log.d(LogKit.TAG, "相机关闭成功");
            if (cameraAspectRatioKit != null) {
                // 重置缓存的分辨率记录
                cameraAspectRatioKit.reset();
            }
            // 清除已有预览帧回调
            CameraController.getInstance().removePreviewDataCallBack(self, previewDataCallBack);
        } else if (code == ICameraStateCallBack.State.ERROR) {
            Log.e(LogKit.TAG, "相机启动错误 || " + msg);
            // 清除已有预览帧回调
            CameraController.getInstance().removePreviewDataCallBack(self, previewDataCallBack);
        }
    }

    /**
     * 安全运行
     *
     * @param consumer Consumer<AppCompatActivity>
     */
    public void safeRun(@NonNull Consumer<AppCompatActivity> consumer) {
        if (needReturn()) {
            Log.w(LogKit.TAG, "安全运行终止 - Fragment 已解绑或宿主 Activity 状态异常");
            return;
        }
        consumer.accept((AppCompatActivity) requireActivity());
    }

    /**
     * 是否需要返回
     *
     * @return 是否需要返回
     */
    private boolean needReturn() {
        Activity activity = getActivity();
        return (!isAdded() || (activity == null) || activity.isFinishing() || activity.isDestroyed());
    }

    @Override
    public void onDestroyView() {
        MultiCameraClient.ICamera iCamera = getCurrentCamera();
        if (iCamera != null) {
            CameraController.getInstance().removePreviewDataCallBack(iCamera, previewDataCallBack);
            CameraController.getInstance().closeCamera(iCamera);
        }
        if (cameraAspectRatioKit != null) {
            cameraAspectRatioKit.release();
            cameraAspectRatioKit = null;
        }
        super.onDestroyView();
    }
}