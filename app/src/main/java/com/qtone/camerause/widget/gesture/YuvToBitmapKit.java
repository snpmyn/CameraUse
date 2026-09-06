package com.qtone.camerause.widget.gesture;

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
 * @desc YUV 转像素数据配套原价
 */
public class YuvToBitmapKit {
    /**
     * NV21 转像素数据
     * <p>
     * 将 NV21 / YUV420SP 格式的数据转为 Bitmap
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
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // 压缩质量 80% 足以让 AI 识别且能大幅提升转码速率
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 80, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();
            return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } catch (Exception e) {
            Log.e(LogKit.TAG, "NV21 转像素数据 - 异常 || " + e.getMessage());
            return null;
        }
    }
}