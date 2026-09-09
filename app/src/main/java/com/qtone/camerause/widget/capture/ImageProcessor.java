/**
 * ImageProcessor.java
 * 
 * OpenCV 图像处理管线 —— 封装灰度化、模糊、Canny边缘、轮廓检测、
 * 多边形近似、四边形筛选、顶点排序、透视变换、OCR预处理
 * 
 * 所有方法均为静态工具方法，无状态，线程安全
 */
package com.qtone.camerause.widget.capture;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ImageProcessor {

    private static final String TAG = "ImageProcessor";

    // ========== 配置参数（可根据实际效果调优）==========

    /** Canny 边缘检测低阈值 */
    private static final int CANNY_LOW = 50;
    /** Canny 边缘检测高阈值 */
    private static final int CANNY_HIGH = 150;
    /** 高斯模糊核大小 */
    private static final int GAUSSIAN_KERNEL = 5;
    /** 面积阈值比例：屏幕面积的百分比，低于此比例的轮廓被忽略 */
    private static final double MIN_AREA_RATIO = 0.10;
    /** 多边形近似 epsilon 系数：epsilon = coefficient * arcLength */
    private static final double APPROX_EPSILON_COEFF = 0.02;
    /** 最大近似尝试次数（逐步放宽 epsilon） */
    private static final int MAX_APPROX_TRIALS = 3;

    // ========== 公开 API ==========

    /**
     * 完整的图像处理管线：
     * 输入 RGBA Mat → 灰度 → 高斯模糊 → Canny → 轮廓检测 → 四边形筛选 → 透视变换
     *
     * @param rgbaFrame 输入的 RGBA 图像 Mat
     * @return 矫正后的试卷图像 Mat（标准矩形），如果检测失败返回 null
     */
    public static Mat processFrame(Mat rgbaFrame) {
        if (rgbaFrame == null || rgbaFrame.empty()) {
            Log.w(TAG, "Input frame is null or empty");
            return null;
        }

        Mat grayMat = null;
        Mat blurredMat = null;
        Mat edgesMat = null;

        try {
            // --- Step 1: 灰度化 ---
            grayMat = new Mat();
            Imgproc.cvtColor(rgbaFrame, grayMat, Imgproc.COLOR_RGBA2GRAY);

            // --- Step 2: 高斯模糊（去噪） ---
            blurredMat = new Mat();
            Imgproc.GaussianBlur(grayMat, blurredMat,
                    new Size(GAUSSIAN_KERNEL, GAUSSIAN_KERNEL), 0);

            // --- Step 3: Canny 边缘检测 ---
            edgesMat = new Mat();
            Imgproc.Canny(blurredMat, edgesMat, CANNY_LOW, CANNY_HIGH);

            // --- Step 4: 轮廓检测 → 面积过滤 → 四边形筛选 ---
            List<MatOfPoint> quadrilateral = detectQuadrilateral(edgesMat);
            if (quadrilateral.isEmpty()) {
                Log.w(TAG, "No quadrilateral detected");
                return null;
            }

            MatOfPoint bestContour = quadrilateral.get(0);
            Point[] vertices = bestContour.toArray();

            // --- Step 5: 顶点排序 ---
            Point[] ordered = orderPoints(vertices);
            if (ordered == null) {
                Log.w(TAG, "Vertex ordering failed");
                return null;
            }

            // --- Step 6: 透视变换 ---
            Mat warped = fourPointTransform(grayMat, ordered);
            Log.d(TAG, "Warp perspective done: " + warped.cols() + "x" + warped.rows());

            return warped;

        } catch (Exception e) {
            Log.e(TAG, "Error in processFrame: " + e.getMessage(), e);
            return null;
        } finally {
            // 释放中间 Mat（注意：不释放输入的 rgbaFrame）
            if (grayMat != null) grayMat.release();
            if (blurredMat != null) blurredMat.release();
            if (edgesMat != null) edgesMat.release();
        }
    }

    /**
     * OCR 预处理：对矫正后的图像做自适应二值化 + 形态学操作
     *
     * @param warped 透视变换后的图像
     * @return 二值化后的图像 Mat
     */
    public static Mat preprocessForOcr(Mat warped) {
        if (warped == null || warped.empty()) {
            return null;
        }

        Mat binary = new Mat();

        // 自适应高斯阈值二值化（比全局阈值效果更好，适应光照不均）
        Imgproc.adaptiveThreshold(
                warped, binary, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 15, 8);

        // 形态学闭合操作：填平字符内部空隙，使文字更完整
        Mat kernel = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT, new Size(2, 2));
        Imgproc.morphologyEx(binary, binary,
                Imgproc.MORPH_CLOSE, kernel);

        return binary;
    }

    // ========== 内部实现 ==========

    /**
     * 轮廓检测与四边形筛选
     *
     * @param edges Canny 边缘图
     * @return 按面积从大到小排序的四边形轮廓列表（最多返回第一个）
     */
    private static List<MatOfPoint> detectQuadrilateral(Mat edges) {
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(edges, contours, new Mat(),
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE);

        if (contours.isEmpty()) {
            return Collections.emptyList();
        }

        // 计算面积阈值
        double screenArea = edges.rows() * edges.cols();
        double minArea = MIN_AREA_RATIO * screenArea;

        // 过滤大轮廓并按面积排序
        List<MatOfPoint> largeContours = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area > minArea) {
                largeContours.add(contour);
            }
        }

        largeContours.sort(Comparator.comparingDouble(
                c -> Imgproc.contourArea((Mat) c)).reversed());

        // 对每个大轮廓尝试多边形近似，找四边形
        for (MatOfPoint contour : largeContours) {
            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            MatOfPoint2f approx = new MatOfPoint2f();

            double peri = Imgproc.arcLength(contour2f, true);

            // 逐步放宽 epsilon 以适配不同形状的试卷
            for (int trial = 0; trial < MAX_APPROX_TRIALS; trial++) {
                double epsilon = (APPROX_EPSILON_COEFF + trial * 0.01) * peri;
                Imgproc.approxPolyDP(contour2f, approx, epsilon, true);

                if (approx.rows() == 4) {
                    MatOfPoint approxPoints = new MatOfPoint(approx.toArray());
                    if (Imgproc.isContourConvex(approxPoints)) {
                        Log.d(TAG, "Quadrilateral found on trial "
                                + (trial + 1) + " with epsilon=" + epsilon);
                        return Collections.singletonList(approxPoints);
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * 顶点排序：将4个顶点按 左上 → 右上 → 右下 → 左下 排序
     *
     * @param pts 4个顶点坐标
     * @return 排序后的顶点数组，如果顶点数不为4则返回 null
     */
    public static Point[] orderPoints(Point[] pts) {
        if (pts == null || pts.length != 4) {
            Log.e(TAG, "orderPoints requires exactly 4 points, got "
                    + (pts == null ? 0 : pts.length));
            return null;
        }

        Point topLeft = null;
        Point topRight = null;
        Point bottomRight = null;
        Point bottomLeft = null;

        for (Point p : pts) {
            // 左上：x + y 最小
            if (topLeft == null || (p.x + p.y) < (topLeft.x + topLeft.y)) {
                topLeft = p;
            }
            // 右上：y - x 最小（即 x 最大、y 最小的方向）
            if (topRight == null || (p.y - p.x) < (topRight.y - topRight.x)) {
                topRight = p;
            }
            // 右下：x + y 最大
            if (bottomRight == null || (p.x + p.y) > (bottomRight.x + bottomRight.y)) {
                bottomRight = p;
            }
            // 左下：x - y 最小
            if (bottomLeft == null || (p.x - p.y) < (bottomLeft.x - bottomLeft.y)) {
                bottomLeft = p;
            }
        }

        return new Point[]{topLeft, topRight, bottomRight, bottomLeft};
    }

    /**
     * 透视变换（四点变换）
     * 将不规则四边形的试卷区域拉伸为标准矩形
     *
     * @param src 源图像
     * @param pts 已排序的4个顶点 {左上, 右上, 右下, 左下}
     * @return 矫正后的矩形图像 Mat
     */
    public static Mat fourPointTransform(Mat src, Point[] pts) {
        // 计算新图像的宽度和高度
        double widthAB = Math.sqrt(
                Math.pow(pts[1].x - pts[0].x, 2)
                + Math.pow(pts[1].y - pts[0].y, 2));
        double widthCD = Math.sqrt(
                Math.pow(pts[2].x - pts[3].x, 2)
                + Math.pow(pts[2].y - pts[3].y, 2));
        int maxWidth = (int) Math.max(widthAB, widthCD);

        double heightAD = Math.sqrt(
                Math.pow(pts[3].x - pts[0].x, 2)
                + Math.pow(pts[3].y - pts[0].y, 2));
        double heightBC = Math.sqrt(
                Math.pow(pts[2].x - pts[1].x, 2)
                + Math.pow(pts[2].y - pts[1].y, 2));
        int maxHeight = (int) Math.max(heightAD, heightBC);

        // 源顶点（原图四边形）- 使用 OpenCV 的 Point
        Point[] srcPoints = new Point[]{
                new Point(pts[0].x, pts[0].y),
                new Point(pts[1].x, pts[1].y),
                new Point(pts[2].x, pts[2].y),
                new Point(pts[3].x, pts[3].y)
        };

        // 目标顶点（标准矩形）
        Point[] dstPoints = new Point[]{
                new Point(0, 0),
                new Point(maxWidth, 0),
                new Point(maxWidth, maxHeight),
                new Point(0, maxHeight)
        };

        // 构建变换矩阵
        Mat srcMat = new Mat(4, 1, CvType.CV_32F);
        Mat dstMat = new Mat(4, 1, CvType.CV_32F);

        for (int i = 0; i < 4; i++) {
            srcMat.put(i, 0, srcPoints[i].x, srcPoints[i].y);
            dstMat.put(i, 0, dstPoints[i].x, dstPoints[i].y);
        }

        // 计算透视变换矩阵并执行变换
        Mat M = Imgproc.getPerspectiveTransform(srcMat, dstMat);
        Mat result = new Mat();
        Imgproc.warpPerspective(src, result, M,
                new Size(maxWidth, maxHeight),
                Imgproc.INTER_CUBIC);

        // 释放临时资源
        srcMat.release();
        dstMat.release();
        M.release();

        return result;
    }

    /**
     * 将 OpenCV Mat 转为 Bitmap（用于传给 Tesseract OCR）
     */
    public static Bitmap matToBitmap(Mat mat) {
        if (mat == null || mat.empty()) {
            return null;
        }
        // 确保是 4 通道
        Mat rgba = new Mat();
        if (mat.channels() == 1) {
            Imgproc.cvtColor(mat, rgba, Imgproc.COLOR_GRAY2RGBA);
        } else if (mat.channels() == 3) {
            Imgproc.cvtColor(mat, rgba, Imgproc.COLOR_RGB2RGBA);
        } else {
            mat.copyTo(rgba);
        }

        Bitmap bitmap = Bitmap.createBitmap(
                rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(rgba, bitmap);
        rgba.release();
        return bitmap;
    }

    public static Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
        try {
            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
            byte[] jpegBytes = out.toByteArray();
            return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
