package com.qtone.camerause.function.capture;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.qtone.camerause.utils.log.LogKit;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created on 2026/8/8.
 *
 * @author 郑少鹏
 * @desc 帧拍照处理器
 */
public class FrameCaptureProcessor {
    /**
     * 单拍状态锁
     * <p>
     * 使用 AtomicBoolean 保证多线程并发环境下的绝对原子性
     */
    private final AtomicBoolean isSingleActive = new AtomicBoolean(false);
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
     * 全局 Application Context 引用
     * <p>
     * 规避 Activity / Fragment 内存泄漏
     */
    private volatile Context applicationContext;
    /**
     * 当前拍照模式
     * <p>
     * 使用 volatile 保证多线程读写可见性
     */
    private volatile CaptureMode currentCaptureMode = CaptureMode.SINGLE;
    /**
     * 连拍模式上次成功捕获预览帧时间戳
     * <p>
     * 单位 - 毫秒
     */
    private volatile long lastCaptureTimestamp = 0L;
    /**
     * 连拍间隔
     * <p>
     * 单位 - 毫秒
     * 默认 500ms
     */
    private volatile long burstInterval = 500L;
    /**
     * 增强实现
     */
    private ExecutorService executorService;

    /**
     * constructor
     */
    public FrameCaptureProcessor() {
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
        Log.d(LogKit.TAG, "帧拍照 - 开始单拍");
        if (CaptureHelper.isCameraNotReady(iCamera, handler, onCaptureCallBack)) {
            return;
        }
        if (context != null) {
            applicationContext = context.getApplicationContext();
        }
        // 当前拍照模式
        currentCaptureMode = CaptureMode.SINGLE;
        // 单拍状态锁
        isSingleActive.set(true);
        // 连拍状态锁
        isBurstActive.set(false);
        // 通知开始
        CaptureHelper.notifyBegin(handler, onCaptureCallBack);
    }

    /**
     * 开始连拍
     *
     * @param context           上下文
     * @param iCamera           相机实例
     * @param interval          时间间隔
     *                          单位 - 毫秒
     * @param onCaptureCallBack 拍照回调
     */
    public void startBurstCapture(Context context, MultiCameraClient.ICamera iCamera, long interval, CaptureProcessor.OnCaptureCallBack onCaptureCallBack) {
        Log.d(LogKit.TAG, "帧拍照 - 开始连拍");
        if (CaptureHelper.isCameraNotReady(iCamera, handler, onCaptureCallBack)) {
            return;
        }
        if (context != null) {
            applicationContext = context.getApplicationContext();
        }
        // 当前拍照模式
        currentCaptureMode = CaptureMode.BURST;
        // 连拍状态锁
        isBurstActive.set(true);
        // 单拍状态锁
        isSingleActive.set(false);
        // 重置连拍序号
        CaptureHelper.resetBurstSequence();
        // 连拍模式上次成功捕获预览帧时间戳
        lastCaptureTimestamp = 0L;
        // 连拍间隔
        // 硬性限制下限 150ms 规避硬件写盘过载
        burstInterval = Math.max(150L, interval);
        Log.d(LogKit.TAG, "帧拍照 - 连拍间隔 || " + burstInterval + " 毫秒");
        // 通知开始
        CaptureHelper.notifyBegin(handler, onCaptureCallBack);
    }

    /**
     * 停止连拍
     */
    public void stopBurstCapture() {
        Log.d(LogKit.TAG, "帧拍照 - 停止连拍");
        // 连拍状态锁
        isBurstActive.set(false);
        // 当前拍照模式
        currentCaptureMode = CaptureMode.SINGLE;
    }

    /**
     * 处理帧
     *
     * @param data       图像帧字节数组
     * @param width      帧物理宽
     * @param height     帧物理高
     * @param dataFormat 数据格式
     */
    public void processFrame(byte[] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat, CaptureProcessor.OnCaptureCallBack onCaptureCallBack) {
        if (data == null) {
            return;
        }
        boolean shouldCapture = false;
        if (currentCaptureMode == CaptureMode.SINGLE) {
            // 单拍
            shouldCapture = isSingleActive.compareAndSet(true, false);
        } else if (currentCaptureMode == CaptureMode.BURST) {
            // 连拍
            // 依据 上一次成功捕获预览帧的时间戳 + 最小拍照时间间隔 控制频率
            if (isBurstActive.get()) {
                long currentTime = System.currentTimeMillis();
                if ((currentTime - lastCaptureTimestamp) >= burstInterval) {
                    lastCaptureTimestamp = currentTime;
                    shouldCapture = true;
                }
            }
        }
        if (shouldCapture) {
            Log.d(LogKit.TAG, "捕获帧成功 [" + currentCaptureMode.name() + "] 尺寸 || " + width + "x" + height);
            String savePath = CaptureHelper.generateSavePath(applicationContext, handler, onCaptureCallBack);
            if (savePath == null) {
                return;
            }
            processFrameAsync(data, width, height, dataFormat, savePath, onCaptureCallBack);
        }
    }

    /**
     * 异步处理帧
     *
     * @param data              图像帧字节数组
     * @param width             帧物理宽
     * @param height            帧物理高
     * @param dataFormat        数据格式
     * @param savePath          保存路径
     * @param onCaptureCallBack 拍照回调
     */
    private void processFrameAsync(@NotNull byte @NotNull [] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat, String savePath, CaptureProcessor.OnCaptureCallBack onCaptureCallBack) {
        // 校验 NV21 物理空间合法性
        // width * height * 1.5 Byte
        int minRequiredSize = width * height * 3 / 2;
        if (data.length < minRequiredSize) {
            Log.e(LogKit.TAG, String.format(Locale.CHINA, "NV21 字节流异常 || 实际长度 (%d) 小于 %dx%d 所需空间", data.length, width, height));
            CaptureHelper.notifyError(handler, onCaptureCallBack, "YUV 数据帧截断");
            return;
        }
        // 深拷贝隔离内存 Buffer
        // 防止相机底层预览帧覆盖正在处理的数据
        final byte[] processData = Arrays.copyOf(data, data.length);
        // 优先切回主线程通知 NV21 原始数据捕获成功
        // 供算法实时分析使用
        handler.post(() -> {
            if (onCaptureCallBack != null) {
                onCaptureCallBack.onCaptureProcessing(processData, width, height, currentCaptureMode);
            }
        });
        // 提取 Context 局部变量
        // 防止异步写盘期间 applicationContext 被 release 显式置空导致 NPE
        final Context appContext = applicationContext;
        // 提交后台单线程池进行 100% 质量 JPEG 编码与磁盘 IO 写盘
        if ((executorService != null) && !executorService.isShutdown()) {
            executorService.execute(() -> processToJpeg(appContext, processData, width, height, dataFormat, savePath, onCaptureCallBack));
        }
    }

    /**
     * 处理为 JPEG
     *
     * @param context           上下文局部引用
     * @param data              图像帧字节数组
     * @param width             帧物理宽
     * @param height            帧物理高
     * @param dataFormat        数据格式
     * @param savePath          保存路径
     * @param onCaptureCallBack 拍照回调
     */
    private void processToJpeg(Context context, byte[] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat, String savePath, CaptureProcessor.OnCaptureCallBack onCaptureCallBack) {
        try {
            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
            File targetFile = new File(savePath);
            try (FileOutputStream fileOutputStream = new FileOutputStream(targetFile)) {
                yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, fileOutputStream);
                fileOutputStream.flush();
            }
            if (context != null) {
                CaptureHelper.scanMediaFile(context, targetFile.getAbsolutePath());
            }
            handler.post(() -> {
                if (onCaptureCallBack != null) {
                    Log.d(LogKit.TAG, "物理 1:1 无损图片生成成功\n当前拍照模式 " + currentCaptureMode.name() + "\n分辨率 " + width + "x" + height + "\n数据格式 " + dataFormat.name() + "\n保存路径 " + savePath);
                    onCaptureCallBack.onCaptureSuccess(savePath, width, height, currentCaptureMode);
                }
            });
        } catch (Exception e) {
            Log.e(LogKit.TAG, "处理 YUV 数据写盘异常", e);
            CaptureHelper.notifyError(handler, onCaptureCallBack, "处理 YUV 数据写盘异常");
        }
    }

    /**
     * 释放
     */
    public void release() {
        // 1. 单拍状态锁
        isSingleActive.set(false);
        // 2. 连拍状态锁
        isBurstActive.set(false);
        // 3. 当前拍照模式
        currentCaptureMode = CaptureMode.SINGLE;
        // 4. 连拍模式上次成功捕获预览帧时间戳
        lastCaptureTimestamp = 0L;
        // 5. 线程消息调度器（清空主线程待执行任务）
        handler.removeCallbacksAndMessages(null);
        // 6. 增强实现（关闭后台线程池）
        if (executorService != null) {
            if (!executorService.isShutdown()) {
                executorService.shutdownNow();
            }
            executorService = null;
        }
        // 7. 全局 Application Context 引用
        applicationContext = null;
    }
}