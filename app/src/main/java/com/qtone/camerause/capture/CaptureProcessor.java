package com.qtone.camerause.capture;

import android.content.Context;
import android.util.Log;

import com.jiangdg.ausbc.MultiCameraClient;
import com.qtone.camerause.util.log.LogKit;

/**
 * @decs: 拍照处理器
 * @author: 郑少鹏
 * @date: 2026/8/5 15:20
 * @version: v 1.0
 */
public class CaptureProcessor {
    /**
     * 帧拍照处理器
     */
    private final FrameCaptureProcessor frameCaptureProcessor;
    /**
     * SDK 拍照处理器
     */
    private final SdkCaptureProcessor sdkCaptureProcessor;
    /**
     * 当前拍照策略
     * <p>
     * 默认采用 NV21 帧处理策略
     */
    private volatile CaptureStrategy captureStrategy = CaptureStrategy.SDK_CAPTURE;

    /**
     * constructor
     */
    public CaptureProcessor() {
        frameCaptureProcessor = new FrameCaptureProcessor();
        sdkCaptureProcessor = new SdkCaptureProcessor();
    }

    /**
     * 获取当前拍照策略
     *
     * @return 当前拍照策略
     */
    public CaptureStrategy getCaptureStrategy() {
        return captureStrategy;
    }

    /**
     * 设置拍照策略
     *
     * @param captureStrategy 拍照策略
     */
    public void setCaptureStrategy(CaptureStrategy captureStrategy) {
        if (captureStrategy != null) {
            this.captureStrategy = captureStrategy;
            Log.d(LogKit.TAG, "切换拍照策略 || " + this.captureStrategy.name());
        }
    }

    /**
     * 预览帧
     * <p>
     * 须在相机 onPreviewFrame 帧回调线程中同步调用
     *
     * @param nv21Data          相机底层输出的原始 NV21 / RGBA 字节数组
     * @param width             帧物理宽
     * @param height            帧物理高
     * @param onCaptureCallBack 拍照回调
     */
    public void onPreviewFrame(byte[] nv21Data, int width, int height, OnCaptureCallBack onCaptureCallBack) {
        if (captureStrategy == CaptureStrategy.SDK_CAPTURE) {
            return;
        }
        frameCaptureProcessor.onPreviewFrame(nv21Data, width, height, onCaptureCallBack);
    }

    /**
     * 开始单拍
     *
     * @param context           上下文
     * @param camera            相机硬件实例
     * @param onCaptureCallBack 拍照结果监听
     */
    public void startSingleCapture(Context context, MultiCameraClient.ICamera camera, OnCaptureCallBack onCaptureCallBack) {
        if (captureStrategy == CaptureStrategy.SDK_CAPTURE) {
            sdkCaptureProcessor.startSingleCapture(context, camera, onCaptureCallBack);
        } else {
            frameCaptureProcessor.startSingleCapture(context, camera, onCaptureCallBack);
        }
    }

    /**
     * 开始连拍
     *
     * @param context           上下文
     * @param camera            相机硬件实例
     * @param intervalMs        连拍时间间隔
     *                          单位 毫秒
     * @param onCaptureCallBack 拍照回调
     */
    public void startBurstCapture(Context context, MultiCameraClient.ICamera camera, long intervalMs, OnCaptureCallBack onCaptureCallBack) {
        if (captureStrategy == CaptureStrategy.SDK_CAPTURE) {
            sdkCaptureProcessor.startBurstCapture(context, camera, intervalMs, onCaptureCallBack);
        } else {
            frameCaptureProcessor.startBurstCapture(context, camera, intervalMs, onCaptureCallBack);
        }
    }

    /**
     * 停止连拍
     */
    public void stopBurstCapture() {
        if (captureStrategy == CaptureStrategy.SDK_CAPTURE) {
            sdkCaptureProcessor.stopBurstCapture();
        } else {
            frameCaptureProcessor.stopBurstCapture();
        }
    }

    /**
     * 释放
     */
    public void release() {
        if (captureStrategy == CaptureStrategy.SDK_CAPTURE) {
            sdkCaptureProcessor.release();
        } else {
            frameCaptureProcessor.release();
        }
    }

    /**
     * 拍照回调
     */
    public interface OnCaptureCallBack {
        /**
         * 拍照开始
         */
        void onCaptureBegin();

        /**
         * 拍照 NV21
         * <p>
         * 成功捕获并隔离 NV21 原始数据帧时回调
         *
         * @param nv21Data    深拷贝后的 NV21 字节数据
         * @param width       帧物理宽
         * @param height      帧物理高
         * @param captureMode 拍照模式
         */
        void onCaptureNv21(byte[] nv21Data, int width, int height, CaptureMode captureMode);

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