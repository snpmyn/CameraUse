package com.qtone.camerause.capture;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.jiangdg.ausbc.MultiCameraClient;
import com.qtone.camerause.util.log.LogKit;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created on 2026/8/8.
 *
 * @author 郑少鹏
 * @desc 公共拍照处理器
 */
public class CommonCaptureProcessor {
    /**
     * 单拍开关状态锁
     * <p>
     * 使用 AtomicBoolean 保证多线程并发环境下的绝对原子性
     */
    protected final AtomicBoolean isSingleModeActive = new AtomicBoolean(false);
    /**
     * 连拍开关状态锁
     * <p>
     * 使用 AtomicBoolean 保证多线程并发环境下的绝对原子性
     */
    protected final AtomicBoolean isBurstModeActive = new AtomicBoolean(false);
    /**
     * 连拍序号自动生成器
     * <p>
     * 解决同毫秒生成文件名冲突覆盖的致命漏洞
     */
    protected final AtomicLong burstSequence = new AtomicLong(0);
    /**
     * 线程消息调度器
     */
    protected final Handler handler = new Handler(Looper.getMainLooper());
    /**
     * 全局 Application Context 引用
     * <p>
     * 规避 Activity / Fragment 内存泄漏
     */
    protected volatile Context applicationContext;
    /**
     * 当前拍照模式
     * <p>
     * 使用 volatile 保证多线程读写可见性
     */
    protected volatile CaptureMode currentCaptureMode = CaptureMode.SINGLE;
    /**
     * 连拍模式下上一次成功捕获预览帧的时间戳
     * <p>
     * 单位 - 毫秒
     */
    protected volatile long lastCaptureTimestamp = 0L;
    /**
     * 连拍模式下最小拍照时间间隔
     * <p>
     * 单位 - 毫秒
     * 默认 500ms
     */
    protected volatile long burstIntervalMs = 500L;
    /**
     * 后台单线程池
     * <p>
     * 专门承载 NV21 到 JPEG 的耗时转码编码与文件磁盘 IO 操作
     */
    protected ExecutorService executorService;

    /**
     * constructor
     */
    public CommonCaptureProcessor() {
        executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * 开始单拍
     *
     * @param context           上下文
     * @param iCamera           相机实例
     * @param onCaptureCallBack 拍照回调
     */
    public void startSingleCapture(Context context, MultiCameraClient.ICamera iCamera, CaptureProcessor.OnCaptureCallBack onCaptureCallBack) {
        Log.d(LogKit.TAG, "开始单拍");
        currentCaptureMode = CaptureMode.SINGLE;
        isBurstModeActive.set(false);
        triggerCaptureInternal(context, iCamera, onCaptureCallBack);
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
    public void startBurstCapture(Context context, MultiCameraClient.ICamera iCamera, long intervalMs, CaptureProcessor.OnCaptureCallBack onCaptureCallBack) {
        Log.d(LogKit.TAG, "开始连拍");
        currentCaptureMode = CaptureMode.BURST;
        isBurstModeActive.set(true);
        burstSequence.set(0);
        this.lastCaptureTimestamp = 0L;
        // 硬性限制下限为 150ms 规避硬件写盘过载
        this.burstIntervalMs = Math.max(150L, intervalMs);
        Log.d(LogKit.TAG, "连拍间隔 || " + this.burstIntervalMs + " 毫秒");
        triggerCaptureInternal(context, iCamera, onCaptureCallBack);
    }

    /**
     * 停止连拍
     */
    public void stopBurstCapture() {
        Log.d(LogKit.TAG, "停止连拍");
        isBurstModeActive.set(false);
        currentCaptureMode = CaptureMode.SINGLE;
    }

    /**
     * 内部触发拍照
     *
     * @param context           上下文
     * @param iCamera           相机硬件实例
     * @param onCaptureCallBack 拍照回调
     */
    protected void triggerCaptureInternal(Context context, MultiCameraClient.ICamera iCamera, CaptureProcessor.OnCaptureCallBack onCaptureCallBack) {
        if ((iCamera == null) || !iCamera.isCameraOpened()) {
            notifyError(onCaptureCallBack, "相机未准备就绪");
            return;
        }
        if (context != null) {
            applicationContext = context.getApplicationContext();
        }
        isSingleModeActive.set(true);
        handler.post(() -> {
            if (onCaptureCallBack != null) {
                Log.d(LogKit.TAG, "拍照开始");
                onCaptureCallBack.onCaptureBegin();
            }
        });
    }

    /**
     * 生成保存路径
     *
     * @param onCaptureCallBack 拍照回调
     * @return 路径字符串 (失败返回 null)
     */
    protected String generateSavePath(CaptureProcessor.OnCaptureCallBack onCaptureCallBack) {
        File mediaDir = (applicationContext != null) ? applicationContext.getExternalFilesDir("Pictures") : null;
        if ((mediaDir != null) && !mediaDir.exists()) {
            boolean created = mediaDir.mkdirs();
            if (!created && !mediaDir.exists()) {
                Log.e(LogKit.TAG, "创建图片存储目录失败 || " + mediaDir.getAbsolutePath());
                notifyError(onCaptureCallBack, "创建图片存储目录失败");
                return null;
            }
        }
        if (mediaDir == null) {
            notifyError(onCaptureCallBack, "无法获取图片存储目录");
            return null;
        }
        String fileName = String.format(Locale.CHINA, "IMG_%d_%04d.jpg", System.currentTimeMillis(), burstSequence.incrementAndGet());
        return new File(mediaDir, fileName).getAbsolutePath();
    }

    /**
     * 扫描媒体文件
     * <p>
     * 刷新 Android 系统 MediaScanner 媒体库
     *
     * @param savePath 保存路径
     */
    protected void scanMediaFile(String savePath) {
        if ((applicationContext != null) && (savePath != null)) {
            MediaScannerConnection.scanFile(applicationContext, new String[]{savePath}, null, null);
        }
    }

    /**
     * 通知错误
     *
     * @param onCaptureCallBack 拍照回调
     * @param errorMsg          错误消息
     */
    protected void notifyError(CaptureProcessor.OnCaptureCallBack onCaptureCallBack, String errorMsg) {
        handler.post(() -> {
            if (onCaptureCallBack != null) {
                Log.e(LogKit.TAG, "拍照错误 || " + errorMsg);
                onCaptureCallBack.onCaptureError(errorMsg);
            }
        });
    }

    /**
     * 释放
     */
    public void release() {
        isSingleModeActive.set(false);
        stopBurstCapture();
        if (executorService != null) {
            if (!executorService.isShutdown()) {
                executorService.shutdownNow();
            }
            executorService = null;
        }
        handler.removeCallbacksAndMessages(null);
        applicationContext = null;
    }
}