package com.qtone.camerause.function.capture;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.callback.ICaptureCallBack;
import com.qtone.camerause.util.log.LogKit;
import com.qtone.camerause.util.media.MediaScanKit;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created on 2026/8/8.
 *
 * @author 郑少鹏
 * @desc SDK 拍照处理器
 */
public class SdkCaptureProcessor {
    /**
     * 连拍状态锁
     * <p>
     * 使用 AtomicBoolean 保证多线程并发环境下的绝对原子性
     */
    private final AtomicBoolean isBurstActive = new AtomicBoolean(false);
    /**
     * 线程消息调度器
     */
    private final Handler handler = new Handler(Looper.getMainLooper());
    /**
     * 全局 Application Context
     * <p>
     * 规避 Activity / Fragment 内存泄漏
     */
    private volatile Context applicationContext;
    /**
     * 当前拍照模式
     * <p>
     * 使用 volatile 保证多线程读写可见性
     */
    private volatile CaptureMode currentCaptureMode = CaptureMode.SINGLE_CAPTURE;
    /**
     * 连拍间隔毫秒
     */
    private volatile long burstIntervalMs = 500L;
    /**
     * 相机实例
     * <p>
     * 使用 volatile 保证多线程读写可见性
     */
    private volatile MultiCameraClient.ICamera iCamera;
    /**
     * 拍照回调
     * <p>
     * 使用 volatile 保证多线程读写可见性
     */
    private volatile CaptureProcessor.OnCaptureCallback onCaptureCallBack;
    /**
     * SDK 连拍定时器任务
     */
    private final Runnable sdkBurstRunnable = new Runnable() {
        @Override
        public void run() {
            if (isBurstActive.get()) {
                executeSdkCapture(iCamera, onCaptureCallBack);
                // 清除队列残留并发起下次循环调度
                handler.removeCallbacks(this);
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
    public void startSingleCapture(Context context, MultiCameraClient.ICamera iCamera, CaptureProcessor.OnCaptureCallback onCaptureCallBack) {
        Log.d(LogKit.TAG, "开始单拍 - SDK 拍照");
        if (CaptureHelper.isCameraNotReady(iCamera, handler, onCaptureCallBack)) {
            return;
        }
        if (context != null) {
            applicationContext = context.getApplicationContext();
        }
        // 当前拍照模式
        currentCaptureMode = CaptureMode.SINGLE_CAPTURE;
        // 连拍状态锁
        isBurstActive.set(false);
        // 线程消息调度器
        handler.removeCallbacks(sdkBurstRunnable);
        // 相机实例
        this.iCamera = iCamera;
        // 拍照回调
        this.onCaptureCallBack = onCaptureCallBack;
        // 通知开始
        CaptureHelper.notifyBegin(handler, onCaptureCallBack);
        // 执行 SDK 拍照
        executeSdkCapture(iCamera, onCaptureCallBack);
    }

    /**
     * 开始连拍
     *
     * @param context           上下文
     * @param iCamera           相机实例
     * @param intervalMs        间隔毫秒
     * @param onCaptureCallBack 拍照回调
     */
    public void startBurstCapture(Context context, MultiCameraClient.ICamera iCamera, long intervalMs, CaptureProcessor.OnCaptureCallback onCaptureCallBack) {
        Log.d(LogKit.TAG, "开始连拍 - SDK 拍照");
        if (CaptureHelper.isCameraNotReady(iCamera, handler, onCaptureCallBack)) {
            return;
        }
        if (context != null) {
            applicationContext = context.getApplicationContext();
        }
        // 当前拍照模式
        currentCaptureMode = CaptureMode.BURST_CAPTURE;
        // 连拍状态锁
        isBurstActive.set(true);
        // 重置连拍序号
        CaptureHelper.resetBurstSequence();
        // 连拍间隔毫秒
        // 硬性限制下限 150ms 规避硬件写盘过载
        burstIntervalMs = Math.max(150L, intervalMs);
        Log.d(LogKit.TAG, "连拍间隔毫秒 - SDK 拍照 || " + burstIntervalMs);
        // 线程消息调度器
        handler.removeCallbacks(sdkBurstRunnable);
        // 相机实例
        this.iCamera = iCamera;
        // 拍照回调
        this.onCaptureCallBack = onCaptureCallBack;
        // 通知开始
        CaptureHelper.notifyBegin(handler, onCaptureCallBack);
        // 执行 SDK 拍照
        executeSdkCapture(iCamera, onCaptureCallBack);
        // 线程消息调度器
        handler.postDelayed(sdkBurstRunnable, this.burstIntervalMs);
    }

    /**
     * 停止连拍
     */
    public void stopBurstCapture() {
        Log.d(LogKit.TAG, "停止连拍 - SDK 拍照");
        // 连拍状态锁
        isBurstActive.set(false);
        // 当前拍照模式
        currentCaptureMode = CaptureMode.SINGLE_CAPTURE;
        // 线程消息调度器
        handler.removeCallbacks(sdkBurstRunnable);
    }

    /**
     * 执行 SDK 拍照
     *
     * @param iCamera           相机实例
     * @param onCaptureCallBack 拍照回调
     */
    public void executeSdkCapture(MultiCameraClient.ICamera iCamera, CaptureProcessor.OnCaptureCallback onCaptureCallBack) {
        if (CaptureHelper.isCameraNotReady(iCamera, handler, onCaptureCallBack)) {
            return;
        }
        String savePath = CaptureHelper.generateSavePath(handler, onCaptureCallBack);
        if (savePath == null) {
            return;
        }
        final Context appContext = applicationContext;
        iCamera.captureImage(new ICaptureCallBack() {
            @Override
            public void onBegin() {
                Log.d(LogKit.TAG, "SDK 拍照开始");
            }

            @Override
            public void onError(@Nullable String error) {
                CaptureHelper.notifyError(handler, onCaptureCallBack, error);
            }

            @Override
            public void onComplete(@Nullable String path) {
                if ((appContext != null) && (path != null)) {
                    MediaScanKit.scanSingleFile(appContext, path);
                }
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
    public void release() {
        // 连拍状态锁
        isBurstActive.set(false);
        // 当前拍照模式
        currentCaptureMode = CaptureMode.SINGLE_CAPTURE;
        // 线程消息调度器
        handler.removeCallbacks(sdkBurstRunnable);
        handler.removeCallbacksAndMessages(null);
        // 相机实例
        iCamera = null;
        // 拍照回调
        onCaptureCallBack = null;
        // 全局 Application Context
        applicationContext = null;
    }
}