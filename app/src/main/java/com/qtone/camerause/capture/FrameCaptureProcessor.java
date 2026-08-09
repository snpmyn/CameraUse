package com.qtone.camerause.capture;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import com.qtone.camerause.util.log.LogKit;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Locale;

/**
 * Created on 2026/8/8.
 *
 * @author 郑少鹏
 * @desc 帧拍照处理器
 */
public class FrameCaptureProcessor extends CommonCaptureProcessor {
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
    public void onPreviewFrame(byte[] nv21Data, int width, int height, CaptureProcessor.OnCaptureCallBack onCaptureCallBack) {
        if (nv21Data == null) {
            return;
        }
        boolean shouldCapture = false;
        if (currentCaptureMode == CaptureMode.SINGLE) {
            // 单拍
            shouldCapture = isSingleModeActive.compareAndSet(true, false);
        } else if (currentCaptureMode == CaptureMode.BURST) {
            // 连拍
            // 依据 上一次成功捕获预览帧的时间戳 + 最小拍照时间间隔 控制频率
            if (isBurstModeActive.get()) {
                long currentTime = System.currentTimeMillis();
                if ((currentTime - lastCaptureTimestamp) >= burstIntervalMs) {
                    lastCaptureTimestamp = currentTime;
                    shouldCapture = true;
                }
            }
        }
        if (shouldCapture) {
            Log.d(LogKit.TAG, "捕获帧成功 [" + currentCaptureMode.name() + "] 尺寸 || " + width + "x" + height);
            String savePath = generateSavePath(onCaptureCallBack);
            if (savePath == null) {
                return;
            }
            processFrameAsync(nv21Data, width, height, savePath, onCaptureCallBack);
        }
    }

    /**
     * 异步处理帧
     * <p>
     * 异步切线程处理 NV21 深拷贝与转码任务
     *
     * @param nv21Data          相机底层输出的原始 NV21 / RGBA 字节数组
     * @param width             帧物理宽
     * @param height            帧物理高
     * @param savePath          保存路径
     * @param onCaptureCallBack 拍照回调
     */
    private void processFrameAsync(@NotNull byte @NotNull [] nv21Data, int width, int height, String savePath, CaptureProcessor.OnCaptureCallBack onCaptureCallBack) {
        // 校验 NV21 物理空间合法性 (width * height * 1.5 Byte)
        int minRequiredSize = width * height * 3 / 2;
        if (nv21Data.length < minRequiredSize) {
            Log.e(LogKit.TAG, String.format(Locale.CHINA, "NV21 字节流异常 || 实际长度 (%d) 小于 %dx%d 所需空间", nv21Data.length, width, height));
            notifyError(onCaptureCallBack, "YUV 数据帧截断");
            return;
        }
        // 深拷贝隔离内存 Buffer
        // 防止相机底层预览帧覆盖正在处理的数据
        final byte[] processData = Arrays.copyOf(nv21Data, nv21Data.length);
        // 优先切回主线程通知 NV21 原始数据捕获成功
        // 供算法实时分析使用
        handler.post(() -> {
            if (onCaptureCallBack != null) {
                onCaptureCallBack.onCaptureNv21(processData, width, height, currentCaptureMode);
            }
        });
        // 提交后台单线程池进行 100% 质量 JPEG 编码与磁盘 IO 写盘
        if ((executorService != null) && !executorService.isShutdown()) {
            executorService.execute(() -> doProcessYuvToJpeg(processData, width, height, savePath, onCaptureCallBack));
        }
    }

    /**
     * 将 NV21 格式数据编码转码为无损 JPEG 并保存至磁盘
     * <p>
     * 工作在后台线程池
     *
     * @param nv21Data          相机底层输出的原始 NV21 / RGBA 字节数组
     * @param width             帧物理宽
     * @param height            帧物理高
     * @param savePath          保存路径
     * @param onCaptureCallBack 拍照回调
     */
    private void doProcessYuvToJpeg(byte[] nv21Data, int width, int height, String savePath, CaptureProcessor.OnCaptureCallBack onCaptureCallBack) {
        try {
            YuvImage yuvImage = new YuvImage(nv21Data, ImageFormat.NV21, width, height, null);
            File targetFile = new File(savePath);
            try (FileOutputStream fileOutputStream = new FileOutputStream(targetFile)) {
                yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, fileOutputStream);
                fileOutputStream.flush();
            }
            // 扫描媒体文件
            scanMediaFile(targetFile.getAbsolutePath());
            // 切回主线程通知写盘成功
            handler.post(() -> {
                if (onCaptureCallBack != null) {
                    Log.d(LogKit.TAG, "物理 1:1 无损图片生成成功\n当前拍照模式 " + currentCaptureMode.name() + "\n分辨率 " + width + "x" + height + "\n保存路径 " + savePath);
                    onCaptureCallBack.onCaptureSuccess(savePath, width, height, currentCaptureMode);
                }
            });
        } catch (Exception e) {
            Log.e(LogKit.TAG, "处理 YUV 数据写盘异常", e);
            notifyError(onCaptureCallBack, "处理 YUV 数据写盘异常");
        }
    }
}