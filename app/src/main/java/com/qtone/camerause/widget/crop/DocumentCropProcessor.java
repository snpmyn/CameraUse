package com.qtone.camerause.widget.crop;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.qtone.camerause.util.log.LogKit;
import com.qtone.camerause.util.media.MediaScanKit;
import com.qtone.camerause.widget.storage.MediaStorageConfig;
import com.qtone.camerause.widget.storage.MediaStorageType;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created on 2026/8/3.
 *
 * @author 郑少鹏
 * @desc 文档裁剪处理器
 */
public class DocumentCropProcessor {
    /**
     * 线程消息调度器
     */
    private final Handler handler = new Handler(Looper.getMainLooper());
    /**
     * 增强实现
     */
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * 通过路径处理
     *
     * @param context                上下文
     * @param path                   路径
     * @param onDocumentCropCallback 文档裁剪回调
     */
    public void processByPath(@NonNull Context context, String path, OnDocumentCropCallback onDocumentCropCallback) {
        if (executorService.isShutdown()) {
            notifyError(onDocumentCropCallback, "文档裁剪处理器已释放");
            return;
        }
        executorService.execute(() -> {
            Mat srcMat = Imgcodecs.imread(path);
            if (srcMat.empty()) {
                notifyError(onDocumentCropCallback, "加载原图失败 - 文档裁剪");
                return;
            }
            String fileName = new File(path).getName();
            processMatAsync(context, srcMat, "已拍照片", fileName, onDocumentCropCallback);
        });
    }

    /**
     * 通过图像帧字节数组处理
     *
     * @param context                上下文
     * @param data                   图像帧字节数组
     * @param width                  物理帧宽
     * @param height                 物理帧高
     * @param onDocumentCropCallback 文档裁剪回调
     */
    public void processByData(@NonNull Context context, byte[] data, int width, int height, OnDocumentCropCallback onDocumentCropCallback) {
        if (executorService.isShutdown()) {
            notifyError(onDocumentCropCallback, "文档裁剪处理器已释放");
            return;
        }
        executorService.execute(() -> {
            if ((data == null) || (data.length < (width * height * 3 / 2))) {
                notifyError(onDocumentCropCallback, "数据帧异常 - 文档裁剪");
                return;
            }
            Mat yuvMat = new Mat(height + height / 2, width, CvType.CV_8UC1);
            yuvMat.put(0, 0, data);
            Mat bgrMat = new Mat();
            Imgproc.cvtColor(yuvMat, bgrMat, Imgproc.COLOR_YUV2BGR_NV21);
            yuvMat.release();
            processMatAsync(context, bgrMat, "原始数据", null, onDocumentCropCallback);
        });
    }

    /**
     * 异步处理 Mat 矩阵
     *
     * @param context                上下文
     * @param inputMat               输入 Mat 矩阵
     * @param logTagSource           日志来源标识
     * @param originalFileName       原始文件名
     * @param onDocumentCropCallback 文档裁剪回调
     */
    private void processMatAsync(@NonNull Context context, Mat inputMat, String logTagSource, String originalFileName, OnDocumentCropCallback onDocumentCropCallback) {
        Mat croppedMat = cropPaperBody(inputMat);
        inputMat.release();
        if ((croppedMat == null) || croppedMat.empty()) {
            notifyError(onDocumentCropCallback, "未能精确识别到试卷白纸主体 - 文档裁剪");
            return;
        }
        File destFile = MediaStorageConfig.getInstance().generateSaveFile(MediaStorageType.DOCUMENT_CROP, originalFileName, -1);
        if (destFile == null) {
            croppedMat.release();
            notifyError(onDocumentCropCallback, "无法获取裁剪图片保存目录 - 文档裁剪");
            return;
        }
        String outputPath = destFile.getAbsolutePath();
        boolean saved = Imgcodecs.imwrite(outputPath, croppedMat);
        Bitmap resultBitmap = matToBitmap(croppedMat);
        croppedMat.release();
        if (saved && (resultBitmap != null)) {
            MediaScanKit.scanSingleFile(context, outputPath, "image/jpeg");
            handler.post(() -> {
                Log.d(LogKit.TAG, "试卷四角透视矫正成功 - 文档裁剪 - " + logTagSource + " - 保存路径 || " + outputPath);
                if (onDocumentCropCallback != null) {
                    onDocumentCropCallback.onDocumentCropSuccess(outputPath, resultBitmap);
                }
            });
        } else {
            notifyError(onDocumentCropCallback, "保存裁剪图片失败 - 文档裁剪");
        }
    }

    /**
     * 拷贝纸张体
     * <p>
     * 大白纸区域轮廓检测 + 智能内缩微调
     *
     * @param srcMat 原始输入的 OpenCV 矩阵对象
     * @return 矫正裁剪后的 OpenCV 矩阵对象
     */
    @Nullable
    private Mat cropPaperBody(Mat srcMat) {
        Mat gray = new Mat();
        Mat blur = new Mat();
        Mat thresh = new Mat();
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        try {
            // 1. 转灰度与降噪平滑
            Imgproc.cvtColor(srcMat, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(gray, blur, new Size(9, 9), 0);
            // 2. Otsu 大津二值化把白纸提取出来
            Imgproc.threshold(blur, thresh, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
            // 3. 形态学闭运算
            // 使整个试卷形成无缝连通区
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(15, 15));
            Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_CLOSE, kernel);
            kernel.release();
            // 4. 提取最大白色轮廓
            Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            double maxArea = 0;
            MatOfPoint maxContour = null;
            double imgArea = srcMat.width() * (double) srcMat.height();
            for (MatOfPoint matOfPoint : contours) {
                double area = Imgproc.contourArea(matOfPoint);
                if ((area > imgArea * 0.20) && (area > maxArea)) {
                    maxArea = area;
                    maxContour = matOfPoint;
                }
            }
            if (maxContour == null) {
                Log.w(LogKit.TAG, "未能在场景中找到足够大的白色纸张 - 文档裁剪");
                return null;
            }
            // 5. 试卷四角拟合
            MatOfPoint2f c2f = new MatOfPoint2f(maxContour.toArray());
            double peri = Imgproc.arcLength(c2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true);
            Point[] points = approx.toArray();
            c2f.release();
            approx.release();
            Point[] paperCorners;
            if (points.length == 4) {
                paperCorners = sortFourCornersClockwise(points);
            } else {
                paperCorners = findExtremeCorners(maxContour.toArray());
            }
            // 6. 计算变换目标宽高
            double widthTop = Math.hypot(paperCorners[1].x - paperCorners[0].x, paperCorners[1].y - paperCorners[0].y);
            double widthBottom = Math.hypot(paperCorners[2].x - paperCorners[3].x, paperCorners[2].y - paperCorners[3].y);
            int targetWidth = (int) Math.max(widthTop, widthBottom);
            double heightLeft = Math.hypot(paperCorners[3].x - paperCorners[0].x, paperCorners[3].y - paperCorners[0].y);
            double heightRight = Math.hypot(paperCorners[2].x - paperCorners[1].x, paperCorners[2].y - paperCorners[1].y);
            int targetHeight = (int) Math.max(heightLeft, heightRight);
            if ((targetWidth <= 0) || (targetHeight <= 0)) {
                return null;
            }
            // 7. 透视变换矫正
            MatOfPoint2f srcPts = new MatOfPoint2f(paperCorners);
            MatOfPoint2f dstPts = new MatOfPoint2f(
                    new Point(0, 0),
                    new Point(targetWidth - 1, 0),
                    new Point(targetWidth - 1, targetHeight - 1),
                    new Point(0, targetHeight - 1)
            );
            Mat transformMatrix = Imgproc.getPerspectiveTransform(srcPts, dstPts);
            Mat destMat = new Mat();
            Imgproc.warpPerspective(srcMat, destMat, transformMatrix, new Size(targetWidth, targetHeight));
            srcPts.release();
            dstPts.release();
            transformMatrix.release();
            // 8. 边缘内缩 0.8%
            // 自动剔除左侧露出的极窄背景黑边
            int offsetX = (int) (targetWidth * 0.008);
            int offsetY = (int) (targetHeight * 0.008);
            int cropW = (targetWidth - (offsetX * 2));
            int cropH = (targetHeight - (offsetY * 2));
            if ((cropW > 0) && (cropH > 0)) {
                org.opencv.core.Rect roi = new org.opencv.core.Rect(offsetX, offsetY, cropW, cropH);
                Mat finalSubMat = new Mat(destMat, roi);
                Mat croppedResult = finalSubMat.clone();
                finalSubMat.release();
                destMat.release();
                return croppedResult;
            }
            return destMat;
        } catch (Exception e) {
            Log.e(LogKit.TAG, "试卷主体裁剪处理异常 - 文档裁剪", e);
            return null;
        } finally {
            // 统一释放轮廓集合Native资源
            // 彻底规避内存泄漏
            for (MatOfPoint matOfPoint : contours) {
                if (matOfPoint != null) {
                    matOfPoint.release();
                }
            }
            contours.clear();
            gray.release();
            blur.release();
            thresh.release();
            hierarchy.release();
        }
    }

    /**
     * 计算并寻找点集中的四个极值角点
     * <p>
     * 左上、右上、右下、左下
     *
     * @param points 输入的轮廓点集数组
     * @return 按顺时针排序的四个角点数组
     */
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    private Point @NotNull [] findExtremeCorners(@NotNull Point @NotNull [] points) {
        Point topLeft = points[0];
        Point topRight = points[0];
        Point bottomRight = points[0];
        Point bottomLeft = points[0];
        double minSum = Double.MAX_VALUE;
        double maxDiff = -Double.MAX_VALUE;
        double maxSum = -Double.MAX_VALUE;
        double minDiff = Double.MAX_VALUE;
        for (Point point : points) {
            double sum = (point.x + point.y);
            double diff = (point.x - point.y);
            if (sum < minSum) {
                minSum = sum;
                topLeft = point;
            }
            if (diff > maxDiff) {
                maxDiff = diff;
                topRight = point;
            }
            if (sum > maxSum) {
                maxSum = sum;
                bottomRight = point;
            }
            if (diff < minDiff) {
                minDiff = diff;
                bottomLeft = point;
            }
        }
        return new Point[]{topLeft, topRight, bottomRight, bottomLeft};
    }

    /**
     * 将四个角点按顺时针方向排序
     *
     * @param points 原始角点数组
     * @return 排序后的角点数组 [左上, 右上, 右下, 左下]
     */
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    private Point @NotNull [] sortFourCornersClockwise(Point[] points) {
        return findExtremeCorners(points);
    }

    /**
     * 将 OpenCV 的 Mat 矩阵转换为 Android 的 Bitmap 对象
     *
     * @param mat 输入的 OpenCV 矩阵
     * @return 转换后的 Bitmap 对象
     */
    @Nullable
    private Bitmap matToBitmap(Mat mat) {
        try {
            Mat rgbMat = new Mat();
            Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_BGR2RGB);
            Bitmap bitmap = Bitmap.createBitmap(rgbMat.cols(), rgbMat.rows(), Bitmap.Config.ARGB_8888);
            org.opencv.android.Utils.matToBitmap(rgbMat, bitmap);
            rgbMat.release();
            return bitmap;
        } catch (Exception e) {
            Log.e(LogKit.TAG, "Mat 转 Bitmap 失败 - 文档裁剪", e);
            return null;
        }
    }

    /**
     * 通知错误
     *
     * @param onDocumentCropCallback 文档裁剪回调
     * @param errorMsg               错误消息
     */
    private void notifyError(OnDocumentCropCallback onDocumentCropCallback, String errorMsg) {
        handler.post(() -> {
            Log.e(LogKit.TAG, "试卷透视矫正失败 - 文档裁剪 || " + errorMsg);
            if (onDocumentCropCallback != null) {
                onDocumentCropCallback.onDocumentCropError(errorMsg);
            }
        });
    }

    /**
     * 释放
     * <p>
     * 释放线程池资源
     */
    public void release() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    /**
     * 文档裁剪回调
     */
    public interface OnDocumentCropCallback {
        /**
         * 文档裁剪成功
         *
         * @param croppedPath  已拷路径
         * @param resultBitmap 结果像素数据
         */
        void onDocumentCropSuccess(String croppedPath, Bitmap resultBitmap);

        /**
         * 文档裁剪错误
         *
         * @param errorMsg 错误消息
         */
        void onDocumentCropError(String errorMsg);
    }
}