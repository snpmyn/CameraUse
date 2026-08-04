package com.qtone.camerause.scancode;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.base.CameraFragment;
import com.jiangdg.ausbc.camera.bean.CameraRequest;
import com.jiangdg.ausbc.render.env.RotateType;
import com.jiangdg.ausbc.utils.ToastUtils;
import com.jiangdg.ausbc.widget.AspectRatioTextureView;
import com.jiangdg.ausbc.widget.IAspectRatio;
import com.qtone.camerause.R;
import com.qtone.camerause.kit.CameraAspectRatioKit;

import org.jetbrains.annotations.NotNull;

/**
 * @decs: 扫码碎片
 * @author: 郑少鹏
 * @date: 2026/7/28 16:18
 * @version: v 1.0
 */
public class ScanCodeFragment extends CameraFragment {
    private static final String TAG = ScanCodeFragment.class.getSimpleName();
    /**
     * 默认请求的相机物理分辨率
     * <p>
     * 宽
     */
    private static final int PREVIEW_WIDTH = 2592;
    /**
     * 默认请求的相机物理分辨率
     * <p>
     * 高
     */
    private static final int PREVIEW_HEIGHT = 1944;
    /**
     * 渲染控件与容器
     */
    private AspectRatioTextureView aspectRatioTextureView;
    /**
     * 相机宽高比配套原件
     */
    private CameraAspectRatioKit mCameraAspectRatioKit;
    /**
     * 容器
     */
    private ViewGroup container;
    /**
     * 扫码处理器
     */
    private ScanCodeProcessor scanCodeProcessor;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (aspectRatioTextureView != null) {
            // 相机宽高比配套原件
            mCameraAspectRatioKit = new CameraAspectRatioKit(aspectRatioTextureView);
        }
        // 初始化 MLKit 扫码分析器
        scanCodeProcessor = new ScanCodeProcessor(new ScanCodeProcessor.OnScanResultListener() {
            @Override
            public void onSuccess(String result, Barcode barcode) {
                Log.d(TAG, "扫码成功 || " + result);
                ToastUtils.show("扫码成功 || " + result);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "扫码错误 || ", e);
            }
        });
    }

    @Nullable
    @Override
    protected View getRootView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container) {
        View root = inflater.inflate(R.layout.fragment_scan_code, container, false);
        aspectRatioTextureView = root.findViewById(R.id.scanCodeFragmentArtv);
        this.container = root.findViewById(R.id.camera_container);
        return root;
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
     * 构建 CameraRequest 相机配置参数
     *
     * @return CameraRequest 实体
     */
    @NonNull
    @Override
    protected CameraRequest getCameraRequest() {
        return new CameraRequest.Builder()
                .setPreviewWidth(PREVIEW_WIDTH)
                .setPreviewHeight(PREVIEW_HEIGHT)
                // 若仅需扫码且无滤镜需求则设为 CameraRequest.RenderMode.NORMAL
                // 可直接输出 NV21 数据，效率比 OPENGL (RGBA) 更高。
                .setRenderMode(CameraRequest.RenderMode.OPENGL)
                .setDefaultRotateType(RotateType.ANGLE_0)
                .setAspectRatioShow(true)
                // 开启硬件 RawPreviewData 数据输出 (以便在回调中抓取 NV21 原始点阵数据)
                .setRawPreviewData(true)
                .create();
    }

    /**
     * 相机状态变更监听回调
     *
     * @param self 相机客户端对象
     * @param code 相机当前状态枚举
     * @param msg  异常或状态描述信息
     */
    @Override
    public void onCameraState(@NotNull MultiCameraClient.ICamera self, @NotNull State code, @Nullable String msg) {
        if (code == State.OPENED) {
            Log.d(TAG, "扫码相机打开成功");
            // 初始时按默认分辨率配置初始化预览控件展示比例
            if (mCameraAspectRatioKit != null) {
                mCameraAspectRatioKit.updateAspectRatio(getActivity(), PREVIEW_WIDTH, PREVIEW_HEIGHT);
            }
            // 注册预览帧回调
            self.addPreviewDataCallBack((data, width, height, format) -> {
                // 1. UI 视角动态适配逻辑
                // 直接交由 CameraAspectRatioKit 处理 (内置分辨率去重与线程安全切换，防止界面拉伸或频繁 re-layout)
                if (mCameraAspectRatioKit != null) {
                    mCameraAspectRatioKit.updateAspectRatio(getActivity(), width, height);
                }
                // 2. 实时扫码分析处理
                if ((data != null) && (scanCodeProcessor != null)) {
                    // 将回调中的 format 准确透传给 UsbScanManager / ScanCodeProcessor
                    scanCodeProcessor.processFrame(data, width, height, format, 0);
                }
            });
        } else if (code == State.CLOSED) {
            // 相机关闭或断开连接时
            // 重置 CameraAspectRatioKit 内缓存的分辨率记录
            if (mCameraAspectRatioKit != null) {
                mCameraAspectRatioKit.reset();
            }
        } else if (code == State.ERROR) {
            Log.e(TAG, "扫码相机打开错误 || " + msg);
        }
    }

    @Override
    public void onDestroyView() {
        if (scanCodeProcessor != null) {
            scanCodeProcessor.release();
            scanCodeProcessor = null;
        }
        if (mCameraAspectRatioKit != null) {
            mCameraAspectRatioKit.release();
            mCameraAspectRatioKit = null;
        }
        super.onDestroyView();
    }
}