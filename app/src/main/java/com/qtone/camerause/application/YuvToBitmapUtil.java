package com.qtone.camerause.application;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import java.io.ByteArrayOutputStream;

/**
 * Created on 2026/9/4.
 *
 * @author 郑少鹏
 * @desc
 */
public class YuvToBitmapUtil {
    /**
     * 将 NV21 / YUV420SP 格式的数据转为 Bitmap
     *
     * @param nv21   NV21 字节数组
     * @param width  图像宽度
     * @param height 图像高度
     * @return 转换后的 Bitmap (ARGB_8888)
     */
    public static Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
        if ((nv21 == null) || (width <= 0) || (height <= 0)) {
            return null;
        }
        try {
            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            // 压缩质量 80% 足以让 AI 识别且能大幅提升转码速率
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 80, stream);
            byte[] imageBytes = stream.toByteArray();
            stream.close();
            return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}