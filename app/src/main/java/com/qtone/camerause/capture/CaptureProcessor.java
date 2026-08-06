package com.qtone.camerause.capture;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaScannerConnection;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.callback.ICaptureCallBack;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @decs: 拍照处理器
 * @author: 郑少鹏
 * @date: 2026/8/5 15:20
 * @version: v 1.0
 */
public class CaptureProcessor {
    private static final String TAG = CaptureProcessor.class.getSimpleName();
    /**
     * 单拍开关状态锁
     * <p>
     * 防止多次连续抓帧
     * 使用 AtomicBoolean 保证多线程并发环境下的绝对原子性
     * <p>
     * true - 已发起单拍请求 (等待抢占并消费下一个可用预览帧)
     * false - 当前无单拍请求或已完成帧消费
     */
    private final AtomicBoolean isSingleModeActive = new AtomicBoolean(false);
    /**
     * 连拍开关状态锁
     * <p>
     * 防止多次连续抓帧
     * 使用 AtomicBoolean 保证多线程并发环境下的绝对原子性
     * <p>
     * true - 连拍持续开启中 (允许根据时间间隔频繁捕获预览帧)
     * false - 连拍已关闭
     */
    private final AtomicBoolean isBurstModeActive = new AtomicBoolean(false);
    /**
     * 连拍序号自动生成器
     * <p>
     * 解决同毫秒生成文件名冲突覆盖的致命漏洞
     */
    private final AtomicLong burstSequence = new AtomicLong(0);
    /**
     * 线程消息调度器
     */
    private final Handler handler = new Handler(Looper.getMainLooper());
    /**
     * 当前拍照模式
     * <p>
     * 使用 volatile 保证多线程读写可见性
     */
    private volatile CaptureMode currentCaptureMode = CaptureMode.SINGLE;
    /**
     * 连拍模式下上一次成功捕获预览帧的时间戳
     * <p>
     * 单位 毫秒
     */
    private volatile long lastCaptureTimestamp = 0L;
    /**
     * 连拍模式下最小抓拍时间间隔
     * <p>
     * 单位 毫秒
     * 默认 500ms
     */
    private volatile long burstIntervalMs = 500L;
    /**
     * 后台单线程池
     * <p>
     * 门承载 NV21 到 JPEG 的耗时转码编码与文件磁盘 IO 操作
     */
    private ExecutorService executorService;
    /**
     * 全局 Application Context 引用
     * <p>
     * 规避 Activity / Fragment 内存泄漏
     */
    private volatile Context applicationContext;

    /**
     * constructor
     */
    public CaptureProcessor() {
        executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * 开始单拍
     *
     * @param context                 上下文
     * @param camera                  相机硬件实例
     *                                用于状态校验
     * @param captureInvoker          SDK 拍照触发器解耦接口
     * @param onCaptureResultListener 拍照结果监听
     */
    public void startSingleCapture(Context context, MultiCameraClient.ICamera camera, CaptureInvoker captureInvoker, OnCaptureResultListener onCaptureResultListener) {
        currentCaptureMode = CaptureMode.SINGLE;
        isBurstModeActive.set(false);
        triggerCaptureInternal(context, camera, captureInvoker, onCaptureResultListener);
    }

    /**
     * 开始连拍
     *
     * @param context                 上下文
     * @param camera                  相机硬件实例
     *                                用于状态校验
     * @param intervalMs              连拍时间间隔
     *                                单位 毫秒
     *                                硬性限制下限为 150ms 以防硬件写盘过载
     * @param captureInvoker          SDK 拍照触发器解耦接口
     * @param onCaptureResultListener 拍照与处理结果回调监听器
     */
    public void startBurstCapture(Context context, MultiCameraClient.ICamera camera, long intervalMs, CaptureInvoker captureInvoker, OnCaptureResultListener onCaptureResultListener) {
        currentCaptureMode = CaptureMode.BURST;
        // 限制硬件编码与 IO 安全下限不低于 150ms
        this.burstIntervalMs = Math.max(150L, intervalMs);
        this.lastCaptureTimestamp = 0L;
        burstSequence.set(0);
        isBurstModeActive.set(true);
        Log.d(TAG, "开启连续拍照模式 - 间隔 || " + this.burstIntervalMs + " ms");
        triggerCaptureInternal(context, camera, captureInvoker, onCaptureResultListener);
    }

    /**
     * 停止连拍
     */
    public void stopBurstCapture() {
        isBurstModeActive.set(false);
        currentCaptureMode = CaptureMode.SINGLE;
        Log.d(TAG, "已停止连拍");
    }

    /**
     * 内部触发拍照
     *
     * @param context                 上下文
     * @param camera                  相机硬件实例
     *                                用于状态校验
     * @param captureInvoker          SDK 拍照触发器解耦接口
     * @param onCaptureResultListener 拍照与处理结果回调监听器
     */
    private void triggerCaptureInternal(Context context, MultiCameraClient.ICamera camera, CaptureInvoker captureInvoker, OnCaptureResultListener onCaptureResultListener) {
        if ((camera == null) || !camera.isCameraOpened()) {
            postError(onCaptureResultListener, "相机未准备就绪");
            return;
        }
        if (context != null) {
            applicationContext = context.getApplicationContext();
        }
        isSingleModeActive.set(true);
        if (captureInvoker != null) {
            long timestamp = System.currentTimeMillis();
            File cacheDir = (context != null) ? context.getCacheDir() : null;
            String tempFramePath = (cacheDir != null) ? new File(cacheDir, "TEMP_" + timestamp + ".jpg").getAbsolutePath() : null;
            captureInvoker.invoke(new ICaptureCallBack() {
                @Override
                public void onBegin() {
                    handler.post(() -> {
                        if (onCaptureResultListener != null) {
                            onCaptureResultListener.onBegin();
                        }
                    });
                }

                @Override
                public void onError(String msg) {
                    Log.e(TAG, "拍照错误 || " + msg);
                    isSingleModeActive.set(false);
                    isBurstModeActive.set(false);
                    postError(onCaptureResultListener, "拍照错误 || " + msg);
                }

                @Override
                public void onComplete(String path) {
                    // 拍照完成后，静默删除 SDK 框架生成的低清缓存临时文件。
                    if (path != null) {
                        File tempFile = new File(path);
                        if (tempFile.exists()) {
                            boolean deleted = tempFile.delete();
                            Log.d(TAG, "私有缓存临时文件清理状态 || " + deleted);
                        }
                    }
                }
            }, tempFramePath);
        }
    }

    /**
     * 检查并消费 NV21 帧
     * <p>
     * 须在相机的 onPreviewFrame 帧回调线程中同步调用
     *
     * @param nv21Data                相机底层输出的原始 NV21 字节数组
     * @param width                   帧物理宽度
     * @param height                  帧物理高度
     * @param onCaptureResultListener 拍照与处理结果回调监听器
     */
    public void onPreviewFrame(byte[] nv21Data, int width, int height, OnCaptureResultListener onCaptureResultListener) {
        if (nv21Data == null) {
            return;
        }
        boolean shouldCapture = false;
        if (currentCaptureMode == CaptureMode.SINGLE) {
            // 单拍
            // 利用 CAS 原子操作抢占唯一一次消费机会
            shouldCapture = isSingleModeActive.compareAndSet(true, false);
        } else if (currentCaptureMode == CaptureMode.BURST) {
            // 连拍
            // 依据时间戳与最小间隔进行频率控制
            if (isBurstModeActive.get()) {
                long currentTime = System.currentTimeMillis();
                if ((currentTime - lastCaptureTimestamp) >= burstIntervalMs) {
                    lastCaptureTimestamp = currentTime;
                    shouldCapture = true;
                }
            }
        }
        if (shouldCapture) {
            Log.d(TAG, "捕获帧成功 [" + currentCaptureMode.name() + "] 尺寸 || " + width + "x" + height);
            // 1. 检查并创建 Pictures 存储路径
            File mediaDir = (applicationContext != null) ? applicationContext.getExternalFilesDir("Pictures") : null;
            if ((mediaDir != null) && !mediaDir.exists()) {
                boolean created = mediaDir.mkdirs();
                if (!created && !mediaDir.exists()) {
                    Log.e(TAG, "创建存储目录失败 || " + mediaDir.getAbsolutePath());
                    postError(onCaptureResultListener, "创建图片保存目录失败");
                    return;
                }
            }
            if (mediaDir == null) {
                postError(onCaptureResultListener, "无法获取存储目录");
                return;
            }
            // 2. 引入 毫秒时间戳 + 递增序列号
            // 彻底消除高频连拍同毫秒文件名冲突覆盖漏洞
            String fileName = String.format(Locale.CHINA, "IMG_%d_%04d.jpg", System.currentTimeMillis(), burstSequence.incrementAndGet());
            String savePath = new File(mediaDir, fileName).getAbsolutePath();
            processFrameAsync(nv21Data, width, height, savePath, onCaptureResultListener);
        }
    }

    /**
     * 异步切线程处理 NV21 深拷贝与转码任务
     *
     * @param nv21Data                相机底层输出的原始 NV21 字节数组
     * @param width                   帧物理宽度
     * @param height                  帧物理高度
     * @param savePath                保存路径
     * @param onCaptureResultListener 拍照与处理结果回调监听器
     */
    private void processFrameAsync(@NotNull byte[] nv21Data, int width, int height, String savePath, OnCaptureResultListener onCaptureResultListener) {
        // 校验 NV21 物理空间合法性 (width * height * 1.5 Byte)
        int minRequiredSize = width * height * 3 / 2;
        if (nv21Data.length < minRequiredSize) {
            Log.e(TAG, String.format(Locale.CHINA, "NV21 字节流异常 || 实际长度 (%d) 小于 %dx%d 所需空间", nv21Data.length, width, height));
            postError(onCaptureResultListener, "YUV 数据帧截断");
            return;
        }
        // 深拷贝隔离内存 Buffer，防止相机底层预览帧覆盖正在处理的数据。
        final byte[] processData = Arrays.copyOf(nv21Data, nv21Data.length);
        // 优先切回主线程通知 NV21 原始数据捕获成功 (供算法实时分析使用)
        handler.post(() -> {
            if (onCaptureResultListener != null) {
                onCaptureResultListener.onNv21Captured(processData, width, height, currentCaptureMode);
            }
        });
        // 提交后台单线程池进行 100% 质量 JPEG 编码与磁盘 IO 写盘
        if ((executorService != null) && !executorService.isShutdown()) {
            executorService.execute(() -> doProcessYuvToJpeg(processData, width, height, savePath, onCaptureResultListener));
        }
    }

    /**
     * 将 NV21 格式数据编码转码为无损 JPEG 并保存至磁盘
     * <p>
     * 工作在后台线程池
     *
     * @param nv21Data                相机底层输出的原始 NV21 字节数组
     * @param width                   帧物理宽度
     * @param height                  帧物理高度
     * @param savePath                保存路径
     * @param onCaptureResultListener 拍照与处理结果回调监听器
     */
    private void doProcessYuvToJpeg(byte[] nv21Data, int width, int height, String savePath, OnCaptureResultListener onCaptureResultListener) {
        try {
            YuvImage yuvImage = new YuvImage(nv21Data, ImageFormat.NV21, width, height, null);
            File targetFile = new File(savePath);
            try (FileOutputStream fileOutputStream = new FileOutputStream(targetFile)) {
                yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, fileOutputStream);
                fileOutputStream.flush();
            }
            // 刷新 Android 系统 MediaScanner 媒体库
            if (applicationContext != null) {
                MediaScannerConnection.scanFile(applicationContext, new String[]{targetFile.getAbsolutePath()}, null, null);
            }
            // 切回主线程通知写盘成功
            handler.post(() -> {
                if (onCaptureResultListener != null) {
                    onCaptureResultListener.onSuccess(savePath, width, height, currentCaptureMode);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "处理 YUV 数据写盘异常", e);
            postError(onCaptureResultListener, "处理高清 YUV 帧失败");
        }
    }

    /**
     * 发送错误
     *
     * @param onCaptureResultListener 拍照与处理结果回调监听器
     * @param errorMsg                错误消息
     */
    private void postError(OnCaptureResultListener onCaptureResultListener, String errorMsg) {
        handler.post(() -> {
            if (onCaptureResultListener != null) {
                onCaptureResultListener.onError(errorMsg);
            }
        });
    }

    /**
     * 释放
     * <p>
     * 停止连拍、清空消息队列并关闭线程池
     */
    public void release() {
        stopBurstCapture();
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        handler.removeCallbacksAndMessages(null);
    }

    /**
     * 拍照与处理结果回调监听器
     */
    public interface OnCaptureResultListener {
        /**
         * 开始拍照处理时回调
         */
        void onBegin();

        /**
         * 成功捕获并隔离 NV21 原始数据帧时回调
         *
         * @param nv21Data    深拷贝后的 NV21 字节数据
         * @param width       帧物理宽度
         * @param height      帧物理高度
         * @param captureMode 拍照模式
         */
        void onNv21Captured(byte[] nv21Data, int width, int height, CaptureMode captureMode);

        /**
         * 成像 JPEG 图片写盘并刷新媒体库成功时回调
         *
         * @param savePath    保存路径
         * @param width       帧物理宽度
         * @param height      帧物理高度
         * @param captureMode 拍照模式
         */
        void onSuccess(String savePath, int width, int height, CaptureMode captureMode);

        /**
         * 发生错误时回调
         *
         * @param errorMsg 错误消息
         */
        void onError(String errorMsg);
    }

    /**
     * SDK 拍照触发器解耦接口
     */
    @FunctionalInterface
    public interface CaptureInvoker {
        /**
         * 执行 SDK 底层拍照逻辑
         *
         * @param iCaptureCallBack SDK 拍照回调
         * @param path             临时文件缓存路径
         */
        void invoke(ICaptureCallBack iCaptureCallBack, String path);
    }
}