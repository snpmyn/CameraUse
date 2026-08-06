package com.qtone.camerause.capture;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.jiangdg.ausbc.utils.ToastUtils;
import com.qtone.camerause.R;
import com.qtone.camerause.base.BaseCameraActivity;
import com.qtone.camerause.value.CameraResolution;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Created on 2026/8/5.
 *
 * @author 郑少鹏
 * @desc 拍照页
 */
public class CaptureActivity extends BaseCameraActivity implements View.OnClickListener, ExamCropProcessor.OnExamCropCallback {
    private static final String TAG = CaptureActivity.class.getSimpleName();
    /**
     * 默认分辨率
     */
    private static final CameraResolution DEFAULT_RESOLUTION = CameraResolution.RES_2592_1944;
    /**
     * 拍照处理器
     */
    private CaptureProcessor captureProcessor;
    /**
     * 试卷裁剪处理器
     */
    private ExamCropProcessor examCropProcessor;

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
     * <p>
     * 子类按需重写
     *
     * @param savedInstanceState Bundle
     */
    @Override
    protected void startLogic(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.startLogic(savedInstanceState);
        // 拍照处理器
        captureProcessor = new CaptureProcessor();
        // 试卷裁剪处理器
        examCropProcessor = new ExamCropProcessor();
        // MaterialButton
        findViewById(R.id.captureActivityMbSingleCapture).setOnClickListener(this);
        findViewById(R.id.captureActivityMbBurstCapture).setOnClickListener(this);
        findViewById(R.id.captureActivityMbStopBurstCapture).setOnClickListener(this);
    }

    /**
     * 预览帧
     * <p>
     * 子类按需重写
     * <p>
     * 实时接收原始帧数据
     *
     * @param data       相机底层输出的原始 NV21 / RGBA 字节数组
     * @param width      帧物理宽度
     * @param height     帧物理高度
     * @param dataFormat 数据格式
     */
    @Override
    protected void onPreviewFrame(byte[] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat) {
        if (captureProcessor != null) {
            captureProcessor.onPreviewFrame(data, width, height, createCaptureResultListener());
        }
    }

    /**
     * 开始单拍
     */
    public void startSingleCapture() {
        if (captureProcessor != null) {
            captureProcessor.startSingleCapture(this, getCurrentCamera(), this::captureImage, createCaptureResultListener());
        }
    }

    /**
     * 开始连拍
     *
     * @param intervalMs 连拍时间间隔
     *                   单位 毫秒
     */
    public void startBurstCapture(long intervalMs) {
        if (captureProcessor != null) {
            captureProcessor.startBurstCapture(this, getCurrentCamera(), intervalMs, this::captureImage, createCaptureResultListener());
        }
    }

    /**
     * 停止连拍
     */
    public void stopBurstCapture() {
        if (captureProcessor != null) {
            captureProcessor.stopBurstCapture();
        }
    }

    /**
     * 创建拍照与处理结果回调监听器
     *
     * @return 拍照与处理结果回调监听器
     */
    @NotNull
    @Contract(" -> new")
    private CaptureProcessor.OnCaptureResultListener createCaptureResultListener() {
        return new CaptureProcessor.OnCaptureResultListener() {
            @Override
            public void onBegin() {
                Log.d(TAG, "开始拍照");
                ToastUtils.show("开始拍照");
            }

            @Override
            public void onNv21Captured(byte[] nv21Data, int width, int height, CaptureMode captureMode) {
                // 页面已销毁或正在销毁时终止回调
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                // 隔离出的 NV21 数据帧异步送入试卷裁剪处理器
                if (examCropProcessor != null) {
                    try {
                        examCropProcessor.processNv21Async(CaptureActivity.this, nv21Data, width, height, CaptureActivity.this);
                    } catch (Exception e) {
                        Log.e(TAG, "processNv21Async 失败 || " + e.getMessage());
                    }
                }
            }

            @Override
            public void onSuccess(String savePath, int width, int height, CaptureMode captureMode) {
                // 页面已销毁或正在销毁时终止回调
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                Log.d(TAG, "物理 1:1 无损图片生成成功\n模式 " + captureMode.name() + "\n分辨率 " + width + "x" + height + "\n路径 " + savePath);
                ToastUtils.show("拍照完成");
                // 试卷裁剪处理器异步处理写盘成功后的 JPEG 文件
                if (examCropProcessor != null) {
                    try {
                        examCropProcessor.processAsync(CaptureActivity.this, savePath, CaptureActivity.this);
                    } catch (Exception e) {
                        Log.e(TAG, "processAsync 失败 || " + e.getMessage());
                    }
                }
            }

            @Override
            public void onError(String errorMsg) {
                // 页面已销毁或正在销毁时终止回调
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                Log.e(TAG, "拍照异常 || " + errorMsg);
                ToastUtils.show("拍照异常");
            }
        };
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

    /**
     * 试卷裁剪成功
     *
     * @param croppedPath  已拷贝路径
     * @param resultBitmap 结果像素数据
     */
    @Override
    public void onExamCropSuccess(String croppedPath, Bitmap resultBitmap) {

    }

    /**
     * 试卷裁剪错误
     *
     * @param errorMsg 错误消息
     */
    @Override
    public void onExamCropError(String errorMsg) {

    }

    @Override
    protected void onDestroy() {
        if (captureProcessor != null) {
            captureProcessor.release();
            captureProcessor = null;
        }
        if (examCropProcessor != null) {
            examCropProcessor.destroy();
            examCropProcessor = null;
        }
        super.onDestroy();
    }
}