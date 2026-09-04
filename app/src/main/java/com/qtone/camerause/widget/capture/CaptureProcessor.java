package com.qtone.camerause.widget.capture;

import android.content.Context;
import android.util.Log;

import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.qtone.camerause.R;
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
     * 拍照策略
     * <p>
     * 默认 {@link CaptureStrategy#FRAME_CAPTURE}
     */
    private volatile CaptureStrategy captureStrategy = CaptureStrategy.FRAME_CAPTURE;
    /**
     * 拍照状态
     * <p>
     * 默认 {@link CaptureState#IDLE}
     * 使用 volatile 保证多线程读写可见性
     */
    private volatile CaptureState captureState = CaptureState.IDLE;
    /**
     * 单拍包装回调
     */
    private volatile OnCaptureCallback singleCaptureWrapCallback;

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
     * 获取拍照状态
     *
     * @return 拍照状态
     */
    public CaptureState getCaptureState() {
        return captureState;
    }

    /**
     * 设置拍照状态
     *
     * @param captureState 拍照状态
     */
    public void setCaptureState(CaptureState captureState) {
        this.captureState = captureState;
    }

    /**
     * 处理帧
     *
     * @param data              图像帧字节数组
     * @param width             帧物理宽
     * @param height            帧物理高
     * @param dataFormat        数据格式
     * @param onCaptureCallBack 拍照回调
     */
    public void processFrame(byte[] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat, OnCaptureCallback onCaptureCallBack) {
        if (captureStrategy == CaptureStrategy.FRAME_CAPTURE) {
            OnCaptureCallback targetCallback = ((captureState == CaptureState.SINGLE_CAPTURE_RUNNING) && (singleCaptureWrapCallback != null))
                    ? singleCaptureWrapCallback
                    : onCaptureCallBack;
            frameCaptureProcessor.processFrame(data, width, height, dataFormat, targetCallback);
        }
    }

    /**
     * 开始单拍
     *
     * @param context           上下文
     * @param iCamera           相机实例
     * @param onCaptureCallBack 拍照回调
     */
    public void startSingleCapture(Context context, MultiCameraClient.ICamera iCamera, OnCaptureCallback onCaptureCallBack) {
        // 单拍进行中
        // 不重复触发
        if (captureState == CaptureState.SINGLE_CAPTURE_RUNNING) {
            notifyError(onCaptureCallBack, context.getString(R.string.singleCaptureRunning));
            return;
        }
        // 连拍进行中
        // 不触发单拍
        if (captureState == CaptureState.BURST_CAPTURE_RUNNING) {
            notifyError(onCaptureCallBack, context.getString(R.string.burstCaptureRunning));
            return;
        }
        // 标记单拍进行中
        captureState = CaptureState.SINGLE_CAPTURE_RUNNING;
        // 包装回调
        OnCaptureCallback wrapCallback = new OnCaptureCallback() {
            @Override
            public void onCaptureBegin() {
                if (onCaptureCallBack != null) {
                    onCaptureCallBack.onCaptureBegin();
                }
            }

            @Override
            public void onCaptureProcessing(byte[] data, int width, int height, CaptureMode captureMode) {
                if (onCaptureCallBack != null) {
                    onCaptureCallBack.onCaptureProcessing(data, width, height, captureMode);
                }
            }

            @Override
            public void onCaptureSuccess(String savePath, int width, int height, CaptureMode captureMode) {
                captureState = CaptureState.IDLE;
                singleCaptureWrapCallback = null;
                if (onCaptureCallBack != null) {
                    onCaptureCallBack.onCaptureSuccess(savePath, width, height, captureMode);
                }
            }

            @Override
            public void onCaptureError(String errorMsg) {
                captureState = CaptureState.IDLE;
                singleCaptureWrapCallback = null;
                if (onCaptureCallBack != null) {
                    onCaptureCallBack.onCaptureError(errorMsg);
                }
            }
        };
        this.singleCaptureWrapCallback = wrapCallback;
        if (captureStrategy == CaptureStrategy.SDK_CAPTURE) {
            // 开始单拍
            sdkCaptureProcessor.startSingleCapture(context, iCamera, wrapCallback);
        } else {
            // 开始单拍
            frameCaptureProcessor.startSingleCapture(context, iCamera, wrapCallback);
        }
    }

    /**
     * 开始连拍
     *
     * @param context           上下文
     * @param iCamera           相机实例
     * @param intervalMs        间隔毫秒
     * @param onCaptureCallBack 拍照回调
     */
    public void startBurstCapture(Context context, MultiCameraClient.ICamera iCamera, long intervalMs, OnCaptureCallback onCaptureCallBack) {
        // 连拍进行中
        // 不重复触发
        if (captureState == CaptureState.BURST_CAPTURE_RUNNING) {
            notifyError(onCaptureCallBack, context.getString(R.string.burstCaptureRunning));
            return;
        }
        // 单拍进行中
        // 不触发连拍
        if (captureState == CaptureState.SINGLE_CAPTURE_RUNNING) {
            notifyError(onCaptureCallBack, context.getString(R.string.singleCaptureRunning));
            return;
        }
        // 标记连拍进行中
        captureState = CaptureState.BURST_CAPTURE_RUNNING;
        if (captureStrategy == CaptureStrategy.SDK_CAPTURE) {
            // 开始连拍
            sdkCaptureProcessor.startBurstCapture(context, iCamera, intervalMs, onCaptureCallBack);
        } else {
            // 开始连拍
            frameCaptureProcessor.startBurstCapture(context, iCamera, intervalMs, onCaptureCallBack);
        }
    }

    /**
     * 停止连拍
     */
    public void stopBurstCapture() {
        sdkCaptureProcessor.stopBurstCapture();
        frameCaptureProcessor.stopBurstCapture();
        if (captureState == CaptureState.BURST_CAPTURE_RUNNING) {
            captureState = CaptureState.IDLE;
        }
    }

    /**
     * 拍照成功是否来自单拍
     *
     * @param captureMode 拍照模式
     * @return 拍照成功是否来自单拍
     */
    public boolean captureSuccessFromSingleCapture(CaptureMode captureMode) {
        return ((captureMode == CaptureMode.SINGLE_CAPTURE));
    }

    /**
     * 拍照错误是否来自单拍
     * <p>
     * 包装回调触发上层错误回调后才切状态为 {@link CaptureState#IDLE}
     * 因此调时 {@link #captureState} 仍为 {@link CaptureState#SINGLE_CAPTURE_RUNNING}
     *
     * @return 拍照错误是否来自单拍
     */
    public boolean captureErrorFromSingleCapture() {
        return (captureState == CaptureState.SINGLE_CAPTURE_RUNNING);
    }

    /**
     * 拍照成功是否来自连拍
     *
     * @param captureMode 拍照模式
     * @return 拍照成功是否来自连拍
     */
    public boolean captureSuccessFromBurstCapture(CaptureMode captureMode) {
        return ((captureMode == CaptureMode.BURST_CAPTURE));
    }

    /**
     * 通知错误
     *
     * @param onCaptureCallBack 拍照回调
     * @param errorMsg          错误消息
     */
    private void notifyError(CaptureProcessor.OnCaptureCallback onCaptureCallBack, String errorMsg) {
        if (onCaptureCallBack != null) {
            Log.e(LogKit.TAG, "拍照错误 || " + errorMsg);
            onCaptureCallBack.onCaptureError(errorMsg);
        }
    }

    /**
     * 释放
     */
    public void release() {
        sdkCaptureProcessor.release();
        frameCaptureProcessor.release();
        captureState = CaptureState.IDLE;
        singleCaptureWrapCallback = null;
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