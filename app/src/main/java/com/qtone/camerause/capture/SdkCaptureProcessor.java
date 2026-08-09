package com.qtone.camerause.capture;

import android.content.Context;
import android.util.Log;

import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.callback.ICaptureCallBack;
import com.qtone.camerause.util.log.LogKit;

import org.jetbrains.annotations.Nullable;

/**
 * Created on 2026/8/8.
 *
 * @author 郑少鹏
 * @desc SDK 拍照处理器
 */
public class SdkCaptureProcessor extends CommonCaptureProcessor {
    /**
     * 相机实例
     */
    private MultiCameraClient.ICamera iCamera;
    /**
     * 拍照回调
     */
    private CaptureProcessor.OnCaptureCallBack onCaptureCallBack;
    /**
     * SDK 连拍定时器 Task
     */
    private final Runnable sdkBurstRunnable = new Runnable() {
        @Override
        public void run() {
            if (isBurstModeActive.get()) {
                executeSdkCapture(iCamera, onCaptureCallBack);
                handler.postDelayed(this, burstIntervalMs);
            }
        }
    };

    /**
     * 开始单拍
     *
     * @param context           上下文
     * @param iCamera           相机实例
     * @param onCaptureCallBack 拍照回调
     */
    @Override
    public void startSingleCapture(Context context, MultiCameraClient.ICamera iCamera, CaptureProcessor.OnCaptureCallBack onCaptureCallBack) {
        super.startSingleCapture(context, iCamera, onCaptureCallBack);
        handler.removeCallbacks(sdkBurstRunnable);
        this.iCamera = iCamera;
        this.onCaptureCallBack = onCaptureCallBack;
        executeSdkCapture(iCamera, onCaptureCallBack);
    }

    /**
     * 开始连拍
     *
     * @param context           上下文
     * @param iCamera           相机实例
     * @param intervalMs        连拍间隔
     *                          单位 - 毫秒
     * @param onCaptureCallBack 拍照回调
     */
    @Override
    public void startBurstCapture(Context context, MultiCameraClient.ICamera iCamera, long intervalMs, CaptureProcessor.OnCaptureCallBack onCaptureCallBack) {
        super.startBurstCapture(context, iCamera, intervalMs, onCaptureCallBack);
        handler.removeCallbacks(sdkBurstRunnable);
        this.iCamera = iCamera;
        this.onCaptureCallBack = onCaptureCallBack;
        executeSdkCapture(iCamera, onCaptureCallBack);
        handler.postDelayed(sdkBurstRunnable, this.burstIntervalMs);
    }

    /**
     * 停止连拍
     */
    @Override
    public void stopBurstCapture() {
        super.stopBurstCapture();
        handler.removeCallbacks(sdkBurstRunnable);
    }

    /**
     * 执行 SDK 拍照
     *
     * @param iCamera           相机实例
     * @param onCaptureCallBack 拍照回调
     */
    public void executeSdkCapture(MultiCameraClient.ICamera iCamera, CaptureProcessor.OnCaptureCallBack onCaptureCallBack) {
        if ((iCamera == null) || !iCamera.isCameraOpened()) {
            notifyError(onCaptureCallBack, "相机未准备就绪");
            return;
        }
        String savePath = generateSavePath(onCaptureCallBack);
        if (savePath == null) {
            return;
        }
        iCamera.captureImage(new ICaptureCallBack() {
            @Override
            public void onBegin() {
                Log.d(LogKit.TAG, "开始 SDK 拍照");
            }

            @Override
            public void onError(@Nullable String error) {
                notifyError(onCaptureCallBack, error);
            }

            @Override
            public void onComplete(@Nullable String path) {
                scanMediaFile(path);
                handler.post(() -> {
                    if (onCaptureCallBack != null) {
                        Log.d(LogKit.TAG, "SDK 拍照成功\n当前拍照模式 " + currentCaptureMode.name() + "\n保存路径 " + path);
                        onCaptureCallBack.onCaptureSuccess(path, 0, 0, currentCaptureMode);
                    }
                });
            }
        }, savePath);
    }

    /**
     * 释放
     */
    @Override
    public void release() {
        super.release();
        handler.removeCallbacks(sdkBurstRunnable);
        iCamera = null;
        onCaptureCallBack = null;
    }
}