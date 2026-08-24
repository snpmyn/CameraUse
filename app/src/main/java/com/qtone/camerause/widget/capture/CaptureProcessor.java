package com.qtone.camerause.widget.capture;

import android.content.Context;
import android.util.Log;

import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.qtone.camerause.util.log.LogKit;

/**
 * @decs: 拍照处理器
 * @author: 郑少鹏
 * @date: 2026/8/5 15:20
 * @version: v 1.0
 */
public class CaptureProcessor {
    /**
     * SDK 拍照处理器
     */
    private final SdkCaptureProcessor sdkCaptureProcessor;
    /**
     * 帧拍照处理器
     */
    private final FrameCaptureProcessor frameCaptureProcessor;
    /**
     * 当前拍照策略
     * <p>
     * 默认 {@link CaptureStrategy#FRAME_CAPTURE}
     */
    private volatile CaptureStrategy captureStrategy = CaptureStrategy.FRAME_CAPTURE;

    /**
     * constructor
     */
    public CaptureProcessor() {
        // SDK 拍照处理器
        sdkCaptureProcessor = new SdkCaptureProcessor();
        // 帧拍照处理器
        frameCaptureProcessor = new FrameCaptureProcessor();
    }

    /**
     * 设置拍照策略
     *
     * @param captureStrategy 拍照策略
     */
    public void setCaptureStrategy(CaptureStrategy captureStrategy) {
        if ((captureStrategy != null) && (this.captureStrategy != captureStrategy)) {
            this.captureStrategy = captureStrategy;
            Log.d(LogKit.TAG, "设置拍照策略 || " + this.captureStrategy.name());
        }
    }

    /**
     * 处理帧
     *
     * @param data       图像帧字节数组
     * @param width      帧物理宽
     * @param height     帧物理高
     * @param dataFormat 数据格式
     */
    public void processFrame(byte[] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat, OnCaptureCallback onCaptureCallBack) {
        if (captureStrategy == CaptureStrategy.FRAME_CAPTURE) {
            frameCaptureProcessor.processFrame(data, width, height, dataFormat, onCaptureCallBack);
        }
    }

    /**
     * 开始单拍
     *
     * @param context           上下文
     * @param iCamera           相机实例
     * @param onCaptureCallBack 拍照回調
     */
    public void startSingleCapture(Context context, MultiCameraClient.ICamera iCamera, OnCaptureCallback onCaptureCallBack) {
        if (captureStrategy == CaptureStrategy.SDK_CAPTURE) {
            sdkCaptureProcessor.startSingleCapture(context, iCamera, onCaptureCallBack);
        } else {
            frameCaptureProcessor.startSingleCapture(context, iCamera, onCaptureCallBack);
        }
    }

    /**
     * 开始连拍
     *
     * @param context           上下文
     * @param iCamera           相机实例
     * @param intervalMs        间隔毫秒
     * @param onCaptureCallBack 拍照回調
     */
    public void startBurstCapture(Context context, MultiCameraClient.ICamera iCamera, long intervalMs, OnCaptureCallback onCaptureCallBack) {
        if (captureStrategy == CaptureStrategy.SDK_CAPTURE) {
            sdkCaptureProcessor.startBurstCapture(context, iCamera, intervalMs, onCaptureCallBack);
        } else {
            frameCaptureProcessor.startBurstCapture(context, iCamera, intervalMs, onCaptureCallBack);
        }
    }

    /**
     * 停止连拍
     */
    public void stopBurstCapture() {
        sdkCaptureProcessor.stopBurstCapture();
        frameCaptureProcessor.stopBurstCapture();
    }

    /**
     * 释放
     */
    public void release() {
        sdkCaptureProcessor.release();
        frameCaptureProcessor.release();
    }

    /**
     * 拍照回调
     */
    public interface OnCaptureCallback {
        /**
         * 拍照开始
         */
        void onCaptureBegin();

        /**
         * 拍照处理中
         *
         * @param data        图像帧字节数组
         * @param width       帧物理宽
         * @param height      帧物理高
         * @param captureMode 拍照模式
         */
        void onCaptureProcessing(byte[] data, int width, int height, CaptureMode captureMode);

        /**
         * 拍照成功
         *
         * @param savePath    保存路径
         * @param width       帧物理宽
         * @param height      帧物理高
         * @param captureMode 拍照模式
         */
        void onCaptureSuccess(String savePath, int width, int height, CaptureMode captureMode);

        /**
         * 拍照错误
         *
         * @param errorMsg 错误消息
         */
        void onCaptureError(String errorMsg);
    }
}