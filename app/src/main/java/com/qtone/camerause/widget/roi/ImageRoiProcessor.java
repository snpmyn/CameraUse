package com.qtone.camerause.widget.roi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import com.qtone.camerause.util.log.LogKit;
import com.qtone.camerause.util.media.MediaScanKit;
import com.qtone.camerause.widget.storage.MediaStorageConfig;
import com.qtone.camerause.widget.storage.StorageType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created on 2026/8/18.
 *
 * @author 郑少鹏
 * @desc 图片 ROI 处理器
 */
public class ImageRoiProcessor {
    /**
     * 从图片文件裁剪 ROI
     *
     * @param context             上下文
     * @param imagePath           图片路径
     * @param multiRoiOverlayView MultiRoiOverlayView
     * @return 从图片文件裁剪 ROI 后路径集
     */
    @SuppressWarnings("UnusedReturnValue")
    public static @NotNull List<String> cropRoiFromImageFile(Context context, String imagePath, MultiRoiOverlayView multiRoiOverlayView) {
        CenterCropTransform centerCropTransform = calculateMappedRoiRects(imagePath, multiRoiOverlayView);
        if (centerCropTransform == null) {
            return Collections.emptyList();
        }
        Bitmap srcBitmap = centerCropTransform.srcBitmap;
        List<RectF> mappedRects = centerCropTransform.mappedRects;
        List<String> croppedPaths = new ArrayList<>();
        try {
            // 遍历各个 ROI 区域并执行裁剪
            for (int i = 0; i < mappedRects.size(); i++) {
                RectF targetRect = mappedRects.get(i);
                // 边界裁剪保护 (转成整数像素矩形)
                int cropLeft = (int) targetRect.left;
                int cropTop = (int) targetRect.top;
                int cropRight = (int) targetRect.right;
                int cropBottom = (int) targetRect.bottom;
                int cropWidth = (cropRight - cropLeft);
                int cropHeight = (cropBottom - cropTop);
                // 过滤掉无效 (宽高 <= 0) 区域
                if ((cropWidth <= 0) || (cropHeight <= 0)) {
                    continue;
                }
                // 创建裁剪子图 Bitmap
                Bitmap croppedBitmap = Bitmap.createBitmap(srcBitmap, cropLeft, cropTop, cropWidth, cropHeight);
                File saveFile = MediaStorageConfig.getInstance().generateSaveFile(StorageType.ROI_CROP, imagePath, i + 1);
                if (saveFile == null) {
                    if (!croppedBitmap.isRecycled()) {
                        croppedBitmap.recycle();
                    }
                    continue;
                }
                String outputPath = saveFile.getAbsolutePath();
                // 写入文件
                try (FileOutputStream fileOutputStream = new FileOutputStream(outputPath)) {
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.flush();
                    croppedPaths.add(outputPath);
                } catch (IOException e) {
                    Log.e(LogKit.TAG, "从图片文件裁剪 ROI 失败 - 图片 ROI 裁剪 || " + outputPath, e);
                } finally {
                    // 回收单张裁剪 Bitmap
                    if (!croppedBitmap.isRecycled()) {
                        croppedBitmap.recycle();
                    }
                }
            }
            // 批量更新媒体库
            if (!croppedPaths.isEmpty()) {
                MediaScanKit.scanBatchFile(context, croppedPaths.toArray(new String[0]));
            }
        } finally {
            // 显式回收原图 Bitmap 内存
            if (!srcBitmap.isRecycled()) {
                srcBitmap.recycle();
            }
        }
        return croppedPaths;
    }

    /**
     * 绘制 ROI 到图片文件
     *
     * @param context             上下文
     * @param imagePath           图片路径
     * @param multiRoiOverlayView MultiRoiOverlayView
     * @param isOverwrite         是否覆盖
     * @return 绘制 ROI 到图片文件后路径
     */
    @SuppressWarnings("UnusedReturnValue")
    public static String drawRoiToImageFile(Context context, String imagePath, MultiRoiOverlayView multiRoiOverlayView, boolean isOverwrite) {
        CenterCropTransform transform = calculateMappedRoiRects(imagePath, multiRoiOverlayView);
        if (transform == null) {
            return imagePath;
        }
        Bitmap srcBitmap = transform.srcBitmap;
        int imgWidth = transform.imgWidth;
        List<RectF> mappedRects = transform.mappedRects;
        // 创建可绘制的像素副本 (ARGB_8888 格式)
        Bitmap resultBitmap = srcBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(resultBitmap);
        // 配置绘制红框与文字 Paint
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
        // 绘制矩形框和编号标签
        for (int i = 0; i < mappedRects.size(); i++) {
            RectF targetRect = mappedRects.get(i);
            // 绘制矩形框
            canvas.drawRect(targetRect, strokePaint);
            // 绘制编号标签
            String label = ("#" + (i + 1));
            float textX = (targetRect.left + strokeWidth + 8f);
            // 结合线宽与 FontMetrics Ascent 绝对值计算基线 Y 坐标
            float textBaseLineY = (targetRect.top + (strokeWidth / 2f) + Math.abs(fontMetrics.ascent));
            canvas.drawText(label, textX, textBaseLineY, textPaint);
        }
        String outputPath;
        if (isOverwrite) {
            // 覆盖原图
            outputPath = imagePath;
        } else {
            File saveFile = MediaStorageConfig.getInstance().generateSaveFile(StorageType.ROI_OVERLAY, imagePath, -1);
            if (saveFile == null) {
                if (!srcBitmap.isRecycled()) {
                    srcBitmap.recycle();
                }
                if (!resultBitmap.isRecycled()) {
                    resultBitmap.recycle();
                }
                return imagePath;
            }
            outputPath = saveFile.getAbsolutePath();
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(outputPath)) {
            // 将 Bitmap 编码压缩写入文件
            // JPEG 格式 + 品质 100
            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            MediaScanKit.scanSingleFile(context, outputPath, "image/jpeg");
        } catch (IOException e) {
            Log.e(LogKit.TAG, "绘制 ROI 到图片文件失败 - 图片 ROI 覆盖 || " + outputPath, e);
            return imagePath;
        } finally {
            // 显式回收 Bitmap 内存资源
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
     * 计算并映射 View 上的 ROI 区域到原图真实像素坐标
     *
     * @param imagePath           图片路径
     * @param multiRoiOverlayView MultiRoiOverlayView
     * @return 变换参数及映射区域结果 [失败或无需绘制返回 null]
     */
    private static @Nullable CenterCropTransform calculateMappedRoiRects(String imagePath, MultiRoiOverlayView multiRoiOverlayView) {
        // 参数合法性校验
        if ((imagePath == null) || (multiRoiOverlayView == null)) {
            return null;
        }
        // 获取所有 ROI 区域相对百分比列表
        List<RectF> percentages = multiRoiOverlayView.getAllRoiPercentages();
        if ((percentages == null) || percentages.isEmpty()) {
            return null;
        }
        // 读取原图 Bitmap 并自动纠正 Exif 旋转角并得到直立 Bitmap
        Bitmap srcBitmap = decodeAndRotateBitmap(imagePath);
        if (srcBitmap == null) {
            return null;
        }
        int imgWidth = srcBitmap.getWidth();
        int imgHeight = srcBitmap.getHeight();
        // 获取 View 测量尺寸
        int viewWidth = multiRoiOverlayView.getWidth();
        int viewHeight = multiRoiOverlayView.getHeight();
        if ((viewWidth <= 0) || (viewHeight <= 0)) {
            viewWidth = imgWidth;
            viewHeight = imgHeight;
        }
        // 计算相机预览画面 (CenterCrop 模式) 在 View 中的真实显示区域与偏移量
        float viewRatio = (float) viewWidth / (float) viewHeight;
        float imgRatio = (float) imgWidth / (float) imgHeight;
        float scale;
        float dx = 0f;
        float dy = 0f;
        if (viewRatio > imgRatio) {
            scale = (float) viewWidth / (float) imgWidth;
            dy = (viewHeight - imgHeight * scale) / 2f;
        } else {
            scale = (float) viewHeight / (float) imgHeight;
            dx = (viewWidth - imgWidth * scale) / 2f;
        }
        List<RectF> mappedRects = new ArrayList<>();
        // 将 View 上的触控百分比精准映射回照片真实像素点
        for (int i = 0; i < percentages.size(); i++) {
            RectF percent = percentages.get(i);
            // ① 先还原为 View 上的物理像素坐标
            float vLeft = percent.left * viewWidth;
            float vTop = percent.top * viewHeight;
            float vRight = percent.right * viewWidth;
            float vBottom = percent.bottom * viewHeight;
            // ② 逆向消除 CenterCrop 的缩放 scale 以及偏移 dx / dy
            float left = (vLeft - dx) / scale;
            float top = (vTop - dy) / scale;
            float right = (vRight - dx) / scale;
            float bottom = (vBottom - dy) / scale;
            // ③ 边界裁剪保护
            left = Math.max(0f, Math.min(left, imgWidth));
            top = Math.max(0f, Math.min(top, imgHeight));
            right = Math.max(0f, Math.min(right, imgWidth));
            bottom = Math.max(0f, Math.min(bottom, imgHeight));
            mappedRects.add(new RectF(left, top, right, bottom));
        }
        return new CenterCropTransform(srcBitmap, imgWidth, imgHeight, mappedRects);
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
            Log.e(LogKit.TAG, "获取图片 Exif 旋转角度失败 - 图片 ROI", e);
        }
        return degree;
    }

    /**
     * CenterCrop 变换参数与映射后的 ROI 区域结果数据
     */
    private static class CenterCropTransform {
        final Bitmap srcBitmap;
        final int imgWidth;
        final int imgHeight;
        final List<RectF> mappedRects;

        CenterCropTransform(Bitmap srcBitmap, int imgWidth, int imgHeight, List<RectF> mappedRects) {
            this.srcBitmap = srcBitmap;
            this.imgWidth = imgWidth;
            this.imgHeight = imgHeight;
            this.mappedRects = mappedRects;
        }
    }
}