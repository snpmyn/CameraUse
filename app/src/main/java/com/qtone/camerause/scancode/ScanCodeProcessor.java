package com.qtone.camerause.scancode;

import android.graphics.Bitmap;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.jiangdg.ausbc.callback.IPreviewDataCallBack;

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
     * 成功后 1.5s 内不重复触发
     */
    private static final long SCAN_INTERVAL = 1500;
    private final BarcodeScanner scanner;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private final OnScanResultListener onScanResultListener;
    /**
     * 防抖
     * 控制扫码成功后的冷却时间
     */
    private long lastSuccessTime = 0;
    /**
     * 缓存 Bitmap 对象
     * 避免 RGBA 模式下频繁创建对象导致 GC 卡顿
     */
    private Bitmap reusableBitmap;

    public ScanCodeProcessor(OnScanResultListener onScanResultListener) {
        this.onScanResultListener = onScanResultListener;
        this.scanner = BarcodeScanning.getClient();
    }

    public void processFrame(byte[] data, int width, int height, IPreviewDataCallBack.DataFormat format, int rotationDegrees) {
        if ((data == null) || (data.length == 0)) {
            return;
        }
        // 扫码防抖
        if ((System.currentTimeMillis() - lastSuccessTime) < SCAN_INTERVAL) {
            return;
        }
        // 丢帧机制
        // 如果上一帧还在 MLKit 分析中
        // 抛弃当前帧
        if (!isProcessing.compareAndSet(false, true)) {
            return;
        }
        try {
            InputImage image = null;
            if (format == IPreviewDataCallBack.DataFormat.NV21) {
                // NV21 数据
                // 直接给 ML Kit 解析 (性能最高)
                image = InputImage.fromByteArray(
                        data,
                        width,
                        height,
                        rotationDegrees,
                        InputImage.IMAGE_FORMAT_NV21
                );
            } else if (format == IPreviewDataCallBack.DataFormat.RGBA) {
                // RGBA 数据
                // 转换成 Bitmap 后传给 ML Kit
                if ((reusableBitmap == null) || (reusableBitmap.getWidth() != width) || (reusableBitmap.getHeight() != height)) {
                    reusableBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                }
                reusableBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(data));
                image = InputImage.fromBitmap(reusableBitmap, rotationDegrees);
            }
            if (image == null) {
                isProcessing.set(false);
                return;
            }
            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if ((barcodes != null) && !barcodes.isEmpty()) {
                            Barcode barcode = barcodes.get(0);
                            String rawValue = barcode.getRawValue();
                            if ((rawValue != null) && (onScanResultListener != null)) {
                                lastSuccessTime = System.currentTimeMillis();
                                onScanResultListener.onSuccess(rawValue, barcode);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (onScanResultListener != null) {
                            onScanResultListener.onFailure(e);
                        }
                    })
                    .addOnCompleteListener(task -> isProcessing.set(false));
        } catch (Exception e) {
            isProcessing.set(false);
        }
    }

    public void release() {
        if (scanner != null) {
            scanner.close();
        }
        if ((reusableBitmap != null) && !reusableBitmap.isRecycled()) {
            reusableBitmap.recycle();
            reusableBitmap = null;
        }
    }

    public interface OnScanResultListener {
        void onSuccess(String result, Barcode barcode);

        void onFailure(Exception e);
    }
}