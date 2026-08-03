package com.qtone.camerause;

import android.graphics.Bitmap;
import android.util.Log;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created on 2026/8/3.
 *
 * @author 郑少鹏
 * @desc 试卷裁剪处理器
 */
public class ExamCropProcessor {
    private static final String TAG = ExamCropProcessor.class.getSimpleName();
    /**
     * 增强实现
     */
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * 异步处理
     * <p>
     * 异步处理本地图片文件的裁剪与透视矫正
     *
     * @param inputPath      输入路径
     *                       原始图像文件路径
     * @param outputPath     输出路径
     *                       裁剪后输出图像文件路径
     * @param onCropCallback 裁剪回调
     */
    public void processAsync(String inputPath, String outputPath, OnCropCallback onCropCallback) {
        executorService.execute(() -> {
            Mat srcMat = Imgcodecs.imread(inputPath);
            if (srcMat.empty()) {
                notifyError(onCropCallback, "加载原图失败");
                return;
            }
            Mat croppedMat = cropPaperBody(srcMat);
            srcMat.release();
            if ((croppedMat == null) || croppedMat.empty()) {
                notifyError(onCropCallback, "未能精确识别到试卷白纸主体");
                return;
            }
            boolean saved = Imgcodecs.imwrite(outputPath, croppedMat);
            Bitmap resultBitmap = matToBitmap(croppedMat);
            croppedMat.release();
            if (saved && (resultBitmap != null)) {
                if (onCropCallback != null) {
                    onCropCallback.onCropSuccess(outputPath, resultBitmap);
                }
            } else {
                notifyError(onCropCallback, "保存裁剪图像失败");
            }
        });
    }

    /**
     * 异步处理 NV21
     * <p>
     * 异步处理摄像头实时采集的 NV21 数据帧的裁剪与透视矫正
     *
     * @param nv21Data       NV21 数据
     *                       NV21 格式的图像字节数组
     * @param width          宽
     * @param height         高
     * @param outputPath     输出路径
     *                       裁剪后输出图像文件路径
     * @param onCropCallback 裁剪回调
     */
    public void processNv21Async(byte[] nv21Data, int width, int height, String outputPath, OnCropCallback onCropCallback) {
        executorService.execute(() -> {
            if ((nv21Data == null) || (nv21Data.length < width * height * 3 / 2)) {
                notifyError(onCropCallback, "NV21 数据帧异常");
                return;
            }
            Mat yuvMat = new Mat(height + height / 2, width, CvType.CV_8UC1);
            yuvMat.put(0, 0, nv21Data);

            Mat bgrMat = new Mat();
            Imgproc.cvtColor(yuvMat, bgrMat, Imgproc.COLOR_YUV2BGR_NV21);
            yuvMat.release();

            Mat croppedMat = cropPaperBody(bgrMat);
            bgrMat.release();

            if ((croppedMat == null) || croppedMat.empty()) {
                notifyError(onCropCallback, "未能精确识别到试卷白纸主体");
                return;
            }

            boolean saved = Imgcodecs.imwrite(outputPath, croppedMat);
            Bitmap resultBitmap = matToBitmap(croppedMat);
            croppedMat.release();

            if (saved && (resultBitmap != null)) {
                if (onCropCallback != null) {
                    onCropCallback.onCropSuccess(outputPath, resultBitmap);
                }
            } else {
                notifyError(onCropCallback, "保存裁剪图像失败");
            }
        });
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
                Log.w(TAG, "未能在场景中找到足够大的白色纸张");
                return null;
            }
            // 5. 试卷四角拟合
            MatOfPoint2f c2f = new MatOfPoint2f(maxContour.toArray());
            double peri = Imgproc.arcLength(c2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true);
            Point[] points = approx.toArray();
            Point[] paperCorners;
            if (points.length == 4) {
                paperCorners = sortFourCornersClockwise(points);
            } else {
                paperCorners = findExtremeCorners(maxContour.toArray());
            }
            for (MatOfPoint matOfPoint : contours) {
                matOfPoint.release();
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
            Log.e(TAG, "试卷主体裁剪处理异常", e);
            return null;
        } finally {
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
    private Point[] findExtremeCorners(@NotNull Point[] points) {
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
    private Point[] sortFourCornersClockwise(Point[] points) {
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
            Log.e(TAG, "Mat 转 Bitmap 失败", e);
            return null;
        }
    }

    /**
     * 通知错误
     *
     * @param onCropCallback 裁剪回调
     * @param errorMsg       错误消息
     */
    private void notifyError(OnCropCallback onCropCallback, String errorMsg) {
        if (onCropCallback != null) {
            onCropCallback.onCropError(errorMsg);
        }
    }

    /**
     * 销毁
     * <p>
     * 释放线程池资源
     */
    public void destroy() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    /**
     * 裁剪接口
     */
    public interface OnCropCallback {
        /**
         * 裁剪成功
         *
         * @param croppedPath  已拷贝路径
         * @param resultBitmap 结果像素数据
         */
        void onCropSuccess(String croppedPath, Bitmap resultBitmap);

        /**
         * 裁剪错误
         *
         * @param errorMsg 错误消息
         */
        void onCropError(String errorMsg);
    }
}