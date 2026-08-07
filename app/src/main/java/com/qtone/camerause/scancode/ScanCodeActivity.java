package com.qtone.camerause.scancode;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.qtone.camerause.R;
import com.qtone.camerause.base.BaseCameraActivity;
import com.qtone.camerause.value.CameraResolution;

/**
 * Created on 2026/8/5.
 *
 * @author 郑少鹏
 * @desc 扫码页
 */
public class ScanCodeActivity extends BaseCameraActivity {
    /**
     * 默认分辨率
     */
    private static final CameraResolution DEFAULT_RESOLUTION = CameraResolution.RES_1280_720;
    /**
     * 扫码页配套原件
     */
    private ScanCodeActivityKit scanCodeActivityKit;

    /**
     * 获取布局 ID
     */
    @Override
    protected int getLayoutId() {
        return R.layout.activity_scan_code;
    }

    /**
     * 获取容器 ID
     */
    @Override
    protected int getContainerId() {
        return R.id.scanCodeActivityFl;
    }

    /**
     * 获取 TextureView ID
     */
    @Override
    protected int getTextureViewId() {
        return R.id.scanCodeActivityArtv;
    }

    /**
     * 获取相机分辨率
     */
    @NonNull
    @Override
    protected CameraResolution getCustomResolution() {
        return DEFAULT_RESOLUTION;
    }

    /**
     * 开始逻辑
     *
     * @param savedInstanceState Bundle
     */
    @Override
    protected void startLogic(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.startLogic(savedInstanceState);
        scanCodeActivityKit = new ScanCodeActivityKit();
    }

    /**
     * 预览帧
     *
     * @param data       图像帧字节数组
     *                   相机底层输出的 NV21 或经转码后的 RGBA
     * @param width      帧物理宽
     * @param height     帧物理高
     * @param dataFormat 数据格式
     */
    @Override
    protected void onPreviewFrame(byte[] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat) {
        if ((data != null) && (scanCodeActivityKit.getScanCodeProcessor() != null)) {
            // 处理帧
            scanCodeActivityKit.getScanCodeProcessor().processFrame(data, width, height, dataFormat, 0);
        }
    }

    @Override
    protected void onDestroy() {
        scanCodeActivityKit.release();
        super.onDestroy();
    }
}