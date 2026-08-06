package com.qtone.camerause.scancode;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.jiangdg.ausbc.utils.ToastUtils;
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
    private static final String TAG = ScanCodeActivity.class.getSimpleName();
    /**
     * 默认分辨率
     */
    private static final CameraResolution DEFAULT_RESOLUTION = CameraResolution.RES_1280_720;
    /**
     * 扫码处理器
     */
    private ScanCodeProcessor scanCodeProcessor;

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
        scanCodeProcessor = new ScanCodeProcessor(new ScanCodeProcessor.OnScanCodeListener() {
            @Override
            public void onScanCodeSuccess(String result, Barcode barcode) {
                Log.d(TAG, "扫码成功 || " + result);
                ToastUtils.show("扫码成功 || " + result);
            }

            @Override
            public void onScanCodeFailure(Exception e) {
                Log.e(TAG, "扫码失败 || ", e);
                ToastUtils.show("扫码成功 || " + e.getMessage());
            }
        });
        scanCodeProcessor.setScanInterval(1);
    }

    /**
     * 预览帧
     *
     * @param data       相机底层输出的原始 NV21 / RGBA 字节数组
     * @param width      帧物理宽
     * @param height     帧物理高
     * @param dataFormat 数据格式
     */
    @Override
    protected void onPreviewFrame(byte[] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat) {
        if ((data != null) && (scanCodeProcessor != null)) {
            // 处理帧
            scanCodeProcessor.processFrame(data, width, height, dataFormat, 0);
        }
    }

    @Override
    protected void onDestroy() {
        if (scanCodeProcessor != null) {
            scanCodeProcessor.release();
            scanCodeProcessor = null;
        }
        super.onDestroy();
    }
}