package com.qtone.camerause.function.roi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.MediaScannerConnection;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import com.qtone.camerause.utils.log.LogKit;
import com.qtone.camerause.widget.MultiRoiOverlayView;

import org.jetbrains.annotations.Nullable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created on 2026/8/18.
 *
 * @author 郑少鹏
 * @desc 图片 ROI 处理器
 */
public class ImageRoiProcessor {
    /**
     * 绘制 ROI 到图片文件
     *
     * @param context             上下文
     * @param imagePath           图片路径
     * @param multiRoiOverlayView MultiRoiOverlayView
     * @param isOverwrite         是否覆盖
     * @return 绘制 ROI 到图片文件后路径
     */
    public static String drawRoiToImageFile(Context context, String imagePath, MultiRoiOverlayView multiRoiOverlayView, boolean isOverwrite) {
        // 参数合法性校验
        // 路径或 OverlayView 为空直接返回原路径
        if ((imagePath == null) || (multiRoiOverlayView == null)) {
            return imagePath;
        }
        // 获取所有 ROI 区域相对百分比列表，为空则无需绘制。
        List<RectF> percentages = multiRoiOverlayView.getAllRoiPercentages();
        if ((percentages == null) || percentages.isEmpty()) {
            return imagePath;
        }
        // 1. 读取原图 Bitmap 并自动纠正 Exif 旋转角并得到直立 Bitmap
        Bitmap srcBitmap = decodeAndRotateBitmap(imagePath);
        if (srcBitmap == null) {
            return imagePath;
        }
        // 创建可绘制的像素副本 (ARGB_8888 格式)
        Bitmap resultBitmap = srcBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(resultBitmap);
        int imgWidth = resultBitmap.getWidth();
        int imgHeight = resultBitmap.getHeight();
        // 2. 获取 View 测量尺寸
        int viewWidth = multiRoiOverlayView.getWidth();
        int viewHeight = multiRoiOverlayView.getHeight();
        // 若 View 尚未测量完成 (宽度或高度 <= 0)
        // 退化使用图片实际像素尺寸计算
        if (viewWidth <= 0 || viewHeight <= 0) {
            viewWidth = imgWidth;
            viewHeight = imgHeight;
        }
        // 3. 计算相机预览画面 (CenterCrop 模式) 在 View 中的真实显示区域 displayedRect
        // 从而求出真实被裁剪掉的 dx (水平偏移) 和 dy (垂直偏移)
        float viewRatio = (float) viewWidth / (float) viewHeight;
        float imgRatio = (float) imgWidth / (float) imgHeight;
        float scale;
        float dx = 0f;
        float dy = 0f;
        if (viewRatio > imgRatio) {
            // View 比照片更宽
            // 照片为填满 View 宽度 -> 上下部分被截跑到了 View 外部
            scale = (float) viewWidth / (float) imgWidth;
            // dy 为负值 -> 说明 View 上下超出画面
            dy = (viewHeight - imgHeight * scale) / 2f;
        } else {
            // View 比照片更高
            // 照片为填满 View 高度 -> 左右部分被截跑到了 View 外部
            scale = (float) viewHeight / (float) imgHeight;
            // dx 为负值 -> 说明 View 左右超出画面
            dx = (viewWidth - imgWidth * scale) / 2f;
        }
        // 4. 配置绘制红框与文字 Paint
        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(Color.RED);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        // 根据图片宽度动态计算矩形框线宽
        // 最小保底 6px
        float strokeWidth = Math.max(6f, imgWidth / 150f);
        strokePaint.setStrokeWidth(strokeWidth);
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        // 根据图片宽度动态计算标签文字大小
        // 最小保底 30px
        float textSize = Math.max(30f, imgWidth / 35f);
        textPaint.setTextSize(textSize);
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        // 5. 将 View 上的触控百分比精准映射回照片真实像素点
        for (int i = 0; i < percentages.size(); i++) {
            RectF percent = percentages.get(i);
            // ① 先还原为 View 上的物理像素坐标
            float vLeft = percent.left * viewWidth;
            float vTop = percent.top * viewHeight;
            float vRight = percent.right * viewWidth;
            float vBottom = percent.bottom * viewHeight;
            // ② 逆向消除 CenterCrop 的缩放 scale 以及偏移 dx / dy
            // 算法 imgX = (viewX - dx) / scale
            float left = (vLeft - dx) / scale;
            float top = (vTop - dy) / scale;
            float right = (vRight - dx) / scale;
            float bottom = (vBottom - dy) / scale;
            // ③ 边界裁剪保护
            // 确保不会超过 0 ~ imgWidth / imgHeight
            left = Math.max(0f, Math.min(left, imgWidth));
            top = Math.max(0f, Math.min(top, imgHeight));
            right = Math.max(0f, Math.min(right, imgWidth));
            bottom = Math.max(0f, Math.min(bottom, imgHeight));
            RectF targetRect = new RectF(left, top, right, bottom);
            // 绘制矩形框
            canvas.drawRect(targetRect, strokePaint);
            // 绘制编号标签
            String label = ("#" + (i + 1));
            float textX = (left + strokeWidth + 8f);
            // 结合线宽与 FontMetrics Ascent 绝对值计算基线 Y 坐标
            // 确保文字绘制在矩形框内上方
            float textBaseLineY = (top + (strokeWidth / 2f) + Math.abs(fontMetrics.ascent));
            canvas.drawText(label, textX, textBaseLineY, textPaint);
        }
        // 6. 保存图片文件
        String outputPath;
        if (isOverwrite) {
            // 覆盖原图保存
            outputPath = imagePath;
        } else {
            // 生成带 "_roi" 后缀的新图片路径
            int dotIndex = imagePath.lastIndexOf(".");
            if (dotIndex != -1) {
                outputPath = (imagePath.substring(0, dotIndex) + "_ROI" + imagePath.substring(dotIndex));
            } else {
                outputPath = (imagePath + "_ROI.jpg");
            }
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(outputPath)) {
            // 将 Bitmap 编码压缩写入文件
            // JPEG 格式 + 品质 100
            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            if (context != null) {
                // 触发系统 MediaScanner 媒体库刷新
                MediaScannerConnection.scanFile(
                        context.getApplicationContext(),
                        new String[]{outputPath},
                        null,
                        (path, uri) -> Log.d(LogKit.TAG, "媒体库刷新完成 - Uri || " + uri)
                );
            }
        } catch (IOException e) {
            Log.e(LogKit.TAG, "绘制 ROI 到图片文件", e);
            return imagePath;
        } finally {
            // 显式回收 Bitmap 内存资源
            // 防止内存泄漏或 OOM
            if (!srcBitmap.isRecycled()) {
                srcBitmap.recycle();
            }
            if (!resultBitmap.isRecycled()) {
                resultBitmap.recycle();
            }
        }
        return outputPath;
    }

    /**
     * 获取图片 Exif 旋转角度
     *
     * @param imagePath 图片路径
     * @return 图片 Exif 旋转角度 [0, 90, 180, 270]
     */
    private static int getImageExifDegree(String imagePath) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(imagePath);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            Log.e(LogKit.TAG, "获取图片 Exif 旋转角度失败", e);
        }
        return degree;
    }

    /**
     * 解码并旋转像素数据
     *
     * @param imagePath 图片路径
     * @return 解码并旋转后像素数据
     */
    private static @Nullable Bitmap decodeAndRotateBitmap(String imagePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap == null) {
            return null;
        }
        int degree = getImageExifDegree(imagePath);
        if (degree != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(degree);
            // 依据旋转矩阵创建直立的 Bitmap
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            // 回收中间未旋转的原 Bitmap
            bitmap.recycle();
            return rotatedBitmap;
        }
        return bitmap;
    }
}