package com.qtone.camerause.function.scancode;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.qtone.camerause.utils.log.LogKit;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @decs: 扫码处理器
 * @author: 郑少鹏
 * @date: 2026/7/28 16:16
 * @version: v 1.0
 */
public class ScanCodeProcessor {
    /**
     * BarcodeScanner
     */
    private final BarcodeScanner barcodeScanner;
    /**
     * 扫码回调
     */
    private final OnScanCodeCallBack onScanCodeCallBack;
    /**
     * 处理中状态锁
     * <p>
     * 使用 AtomicBoolean 保证多线程并发环境下的绝对原子性
     */
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    /**
     * 缓存 Bitmap 对象
     * <p>
     * 避免 RGBA 模式下频繁创建对象导致 GC 卡顿
     */
    private Bitmap reusableBitmap;
    /**
     * 防抖
     * <p>
     * 控制扫码成功后的冷却时间
     */
    private volatile long lastSuccessTime = 0;
    /**
     * 扫描间隔毫秒
     * <p>
     * 扫码成功冷却时间
     */
    private volatile long scanIntervalMs = 1500;

    /**
     * constructor
     *
     * @param onScanCodeCallBack 扫码回调
     */
    public ScanCodeProcessor(OnScanCodeCallBack onScanCodeCallBack) {
        // BarcodeScanner
        this.barcodeScanner = BarcodeScanning.getClient();
        // 扫码回调
        this.onScanCodeCallBack = onScanCodeCallBack;
    }

    /**
     * 设置扫描间隔毫秒
     *
     * @param scanIntervalMs 扫描间隔毫秒
     *                       扫码成功冷却时间
     */
    public void setScanIntervalMs(long scanIntervalMs) {
        this.scanIntervalMs = scanIntervalMs;
    }

    /**
     * 处理帧
     *
     * @param data            图像帧字节数组
     * @param width           帧物理宽
     * @param height          帧物理高
     * @param dataFormat      数据格式
     * @param rotationDegrees 旋转角度
     */
    public void processFrame(byte[] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat, int rotationDegrees) {
        if ((data == null) || (data.length == 0)) {
            return;
        }
        // 扫码防抖
        if ((System.currentTimeMillis() - lastSuccessTime) < scanIntervalMs) {
            return;
        }
        // 丢帧机制
        // 上一帧还在 MLKit 分析中则抛弃当前帧
        if (!isProcessing.compareAndSet(false, true)) {
            return;
        }
        try {
            InputImage inputImage = null;
            if (dataFormat == IPreviewDataCallBack.DataFormat.NV21) {
                // NV21 数据
                // 直接给 ML Kit 解析 (性能最高)
                inputImage = InputImage.fromByteArray(
                        data,
                        width,
                        height,
                        rotationDegrees,
                        InputImage.IMAGE_FORMAT_NV21
                );
            } else if (dataFormat == IPreviewDataCallBack.DataFormat.RGBA) {
                // RGBA 数据
                // 复用 Bitmap 避免操作与格式解析错误 (降低 GC 卡顿风险)
                if ((reusableBitmap == null) || (reusableBitmap.getWidth() != width) || (reusableBitmap.getHeight() != height)) {
                    if ((reusableBitmap != null) && !reusableBitmap.isRecycled()) {
                        reusableBitmap.recycle();
                    }
                    reusableBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                }
                reusableBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(data));
                inputImage = InputImage.fromBitmap(reusableBitmap, rotationDegrees);
            }
            if (inputImage == null) {
                isProcessing.set(false);
                return;
            }
            barcodeScanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        if ((barcodes != null) && !barcodes.isEmpty()) {
                            Barcode barcode = barcodes.get(0);
                            String rawValue = barcode.getRawValue();
                            if ((rawValue != null) && (onScanCodeCallBack != null)) {
                                lastSuccessTime = System.currentTimeMillis();
                                Log.d(LogKit.TAG, "扫码成功 || " + rawValue);
                                onScanCodeCallBack.onScanCodeSuccess(rawValue, barcode);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (onScanCodeCallBack != null) {
                            Log.e(LogKit.TAG, "扫码失败 || ", e);
                            onScanCodeCallBack.onScanCodeFailure(e);
                        }
                    })
                    .addOnCompleteListener(task -> isProcessing.set(false));
        } catch (Throwable t) {
            // 捕获所有运行时异常与 Error
            // 确保出现异常时可重置处理中状态锁，防止线程卡死。
            isProcessing.set(false);
        }
    }

    /**
     * 释放
     */
    public void release() {
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
        if ((reusableBitmap != null) && !reusableBitmap.isRecycled()) {
            reusableBitmap.recycle();
            reusableBitmap = null;
        }
    }

    /**
     * 扫码回调
     */
    public interface OnScanCodeCallBack {
        /**
         * 扫码成功
         *
         * @param result  结果
         * @param barcode Barcode
         */
        void onScanCodeSuccess(String result, Barcode barcode);

        /**
         * 扫码失败
         *
         * @param e 异常
         */
        void onScanCodeFailure(Exception e);
    }
}