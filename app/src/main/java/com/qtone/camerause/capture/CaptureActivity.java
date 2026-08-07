package com.qtone.camerause.capture;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.qtone.camerause.R;
import com.qtone.camerause.base.BaseCameraActivity;
import com.qtone.camerause.crop.ExamCropProcessor;
import com.qtone.camerause.kit.LogKit;
import com.qtone.camerause.value.CameraResolution;

import org.jetbrains.annotations.NotNull;

/**
 * Created on 2026/8/5.
 *
 * @author 郑少鹏
 * @desc 拍照页
 */
public class CaptureActivity extends BaseCameraActivity implements View.OnClickListener {
    /**
     * 默认分辨率
     */
    private static final CameraResolution DEFAULT_RESOLUTION = CameraResolution.RES_2592_1944;
    /**
     * 拍照页配套原件
     */
    private CaptureActivityKit captureActivityKit;

    /**
     * 获取布局 ID
     *
     * @return 布局 ID
     */
    @Override
    protected int getLayoutId() {
        return R.layout.activity_capture;
    }

    /**
     * 获取容器 ID
     *
     * @return 容器 ID
     */
    @Override
    protected int getContainerId() {
        return R.id.captureActivityFl;
    }

    /**
     * 获取 TextureView ID
     *
     * @return TextureView ID
     */
    @Override
    protected int getTextureViewId() {
        return R.id.captureActivityArtv;
    }

    /**
     * 获取相机分辨率
     *
     * @return 相机分辨率
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
        // 拍照页配套原件
        captureActivityKit = new CaptureActivityKit(this);
        // MaterialButton
        findViewById(R.id.captureActivityMbSingleCapture).setOnClickListener(this);
        findViewById(R.id.captureActivityMbBurstCapture).setOnClickListener(this);
        findViewById(R.id.captureActivityMbStopBurstCapture).setOnClickListener(this);

        String filePath = "/storage/emulated/0/Android/data/com.qtone.camerause/files/Pictures/IMG_1786010873667_0001.jpg";
        //String filePath = "/storage/emulated/0/Android/data/com.qtone.camerause/files/Pictures/IMG_1786086059174_0001.jpg";
        captureActivityKit.getExamCropProcessor().processAsync(this, filePath, new ExamCropProcessor.OnExamCropCallback() {
            @Override
            public void onExamCropSuccess(String croppedPath, Bitmap resultBitmap) {

            }

            @Override
            public void onExamCropError(String errorMsg) {

            }
        });
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
        if (captureActivityKit.getCaptureProcessor() != null) {
            captureActivityKit.getCaptureProcessor().onPreviewFrame(data, width, height, captureActivityKit);
        }
    }

    /**
     * 开始单拍
     */
    public void startSingleCapture() {
        if (captureActivityKit.getCaptureProcessor() != null) {
            Log.d(LogKit.TAG, "开始单拍");
            captureActivityKit.getCaptureProcessor().startSingleCapture(this, getCurrentCamera(), captureActivityKit);
        }
    }

    /**
     * 开始连拍
     *
     * @param intervalMs 连拍时间间隔
     *                   单位 毫秒
     */
    public void startBurstCapture(long intervalMs) {
        if (captureActivityKit.getCaptureProcessor() != null) {
            Log.d(LogKit.TAG, "开始连拍");
            captureActivityKit.getCaptureProcessor().startBurstCapture(this, getCurrentCamera(), intervalMs, captureActivityKit);
        }
    }

    /**
     * 停止连拍
     */
    public void stopBurstCapture() {
        if (captureActivityKit.getCaptureProcessor() != null) {
            Log.d(LogKit.TAG, "停止连拍");
            captureActivityKit.getCaptureProcessor().stopBurstCapture();
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(@NotNull View v) {
        int viewId = v.getId();
        switch (viewId) {
            case R.id.captureActivityMbSingleCapture:
                startSingleCapture();
                break;
            case R.id.captureActivityMbBurstCapture:
                startBurstCapture(3000);
                break;
            case R.id.captureActivityMbStopBurstCapture:
                stopBurstCapture();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        captureActivityKit.release();
        super.onDestroy();
    }
}