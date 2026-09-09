package com.qtone.camerause.widget.camera;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import com.qtone.camerause.util.log.LogKit;

import java.io.ByteArrayOutputStream;

/**
 * Created on 2026/9/4.
 *
 * @author 郑少鹏
 * @desc YUV 转像素数据配套原件
 * 1. YUV420SP 是大类名称 (Semi-Planar - 半平面模式)，分为两个平面存储：
 * - 第一平面：连续存放所有 Y (亮度) 数据
 * - 第二平面：交错存放 U/V (色度) 数据
 * 2. NV21 与 NV12 均属 YUV420SP
 * - NV21：Android 摄像头默认回调格式，第二平面为 VU 交错。
 * - NV12：iOS 及部分硬件解码器标准格式，第二平面为 UV 交错。
 */
public class YuvToBitmapKit {
    /**
     * 流缓存
     * <p>
     * 复用内存流
     * 避免高频创建对象引发垃圾回收 (GC) 导致卡顿
     */
    private static final ThreadLocal<ReusableByteArrayOutputStream> STREAM_BUFFER = ThreadLocal.withInitial(() -> new ReusableByteArrayOutputStream(1024 * 512));

    /**
     * NV21 转像素数据
     *
     * @param data   图像帧字节数组
     * @param width  帧物理宽
     * @param height 帧物理高
     * @return 像素数据
     */
    public static Bitmap nv21ToBitmap(byte[] data, int width, int height) {
        if ((data == null) || (width <= 0) || (height <= 0)) {
            return null;
        }
        try {
            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
            ReusableByteArrayOutputStream byteArrayOutputStream = STREAM_BUFFER.get();
            if (byteArrayOutputStream == null) {
                byteArrayOutputStream = new ReusableByteArrayOutputStream(1024 * 512);
                STREAM_BUFFER.set(byteArrayOutputStream);
            }
            byteArrayOutputStream.reset();
            // 压缩质量 80% 足以让 AI 识别且能大幅提升转码速率
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 80, byteArrayOutputStream);
            // 直接读取内部数组
            // 避免 toByteArray() 产生一次额外的内存复制
            return android.graphics.BitmapFactory.decodeByteArray(byteArrayOutputStream.getBuffer(), 0, byteArrayOutputStream.size());
        } catch (Exception e) {
            Log.e(LogKit.TAG, "NV21 转像素数据 - 异常 || " + e.getMessage());
            return null;
        }
    }

    /**
     * 内部扩展类
     * <p>
     * 直接暴露底层的 byte 数组以消除拷贝开销
     */
    private static class ReusableByteArrayOutputStream extends ByteArrayOutputStream {
        public ReusableByteArrayOutputStream(int size) {
            super(size);
        }

        public byte[] getBuffer() {
            return buf;
        }
    }
}