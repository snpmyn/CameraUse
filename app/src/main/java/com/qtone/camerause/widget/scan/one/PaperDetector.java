package com.qtone.camerause.widget.scan.one;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 试卷检测器：负责从相机帧中识别并提取试卷区域
 * 修复：orderPoints 返回值和接收变量统一为 float[][]，解决类型不匹配错误
 */
public class PaperDetector {
    private static final double AREA_THRESHOLD = 0.15;
    private static final double APPROX_POLY_EPSILON = 0.02;
    private static final long COOLDOWN_MS = 1000;
    private long lastDetectTime = 0;

    /**
     * 主检测方法：传入相机帧，返回裁剪并透视变换后的试卷图像
     *
     * @param frame 原始相机帧 (RGBA格式)
     * @return 处理后的试卷图像，如果未检测到则返回null
     */
    public Mat detect(Mat frame) {
        // 1. 冷却时间检查，防止频繁触发
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDetectTime < COOLDOWN_MS) {
            return null;
        }

        // 2. 图像预处理：灰度化 + 高斯模糊 + Canny边缘检测
        Mat gray = new Mat();
        Mat blurred = new Mat();
        Mat edges = new Mat();

        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);
        Imgproc.Canny(blurred, edges, 50, 150);

        // 3. 寻找轮廓
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // 4. 按面积降序排序轮廓
        Collections.sort(contours, (o1, o2) -> {
            double area1 = Imgproc.contourArea(o1);
            double area2 = Imgproc.contourArea(o2);
            return Double.compare(area2, area1);
        });

        double frameArea = frame.rows() * frame.cols();
        MatOfPoint2f paperContour = null;

        // 5. 遍历轮廓，寻找符合条件的四边形
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area < frameArea * AREA_THRESHOLD) {
                break;
            }

            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            double peri = Imgproc.arcLength(contour2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(contour2f, approx, APPROX_POLY_EPSILON * peri, true);

//            if (approx.rows() == 4 && Imgproc.isContourConvex(approx)) {
//                paperContour = approx;
//                break;
//            }
        }

        // 释放临时Mat
        gray.release();
        blurred.release();
        edges.release();
        hierarchy.release();

        // 6. 如果未找到试卷轮廓，返回null
        if (paperContour == null) {
            return null;
        }

        // 7. 提取四个顶点并排序
        float[][] currentPoints = new float[4][2];
        Point[] pts = paperContour.toArray();
        for (int i = 0; i < 4; i++) {
            currentPoints[i][0] = (float) pts[i].x;
            currentPoints[i][1] = (float) pts[i].y;
        }

        // ✅ 修复：使用二维数组 float[][] 接收排序后的点（原代码用 float[] 导致类型错误）
        float[][] orderedCurrent = orderPoints(currentPoints);

        // 8. 执行透视变换
        Mat result = fourPointTransform(frame, orderedCurrent);
        lastDetectTime = currentTime;
        return result;
    }

    /**
     * 对四个顶点进行排序：[左上, 右上, 右下, 左下]
     *
     * @param points 输入的四个点，格式为 float[4][2]
     * @return 排序后的 4x2 二维数组
     */
    private float[][] orderPoints(float[][] points) {
        float[][] ordered = new float[4][2];

        // 1. 根据 x + y 的和排序：最小的为左上，最大的为右下
        float[] sums = new float[4];
        for (int i = 0; i < 4; i++) {
            sums[i] = points[i][0] + points[i][1];
        }
        ordered[0] = points[getMinIndex(sums)]; // 左上
        ordered[2] = points[getMaxIndex(sums)]; // 右下

        // 2. 根据 y - x 的差值排序：最小的为右上，最大的为左下
        float[] diffs = new float[4];
        for (int i = 0; i < 4; i++) {
            diffs[i] = points[i][1] - points[i][0];
        }
        ordered[1] = points[getMinIndex(diffs)]; // 右上
        ordered[3] = points[getMaxIndex(diffs)]; // 左下

        return ordered;
    }

    // 辅助方法：获取数组最小值的索引
    private int getMinIndex(float[] arr) {
        int minIdx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] < arr[minIdx]) minIdx = i;
        }
        return minIdx;
    }

    // 辅助方法：获取数组最大值的索引
    private int getMaxIndex(float[] arr) {
        int maxIdx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[maxIdx]) maxIdx = i;
        }
        return maxIdx;
    }

    /**
     * 四点透视变换
     */
    private Mat fourPointTransform(Mat src, float[][] pts) {
        float widthTop = (float) Math.sqrt(
                Math.pow(pts[1][0] - pts[0][0], 2) + Math.pow(pts[1][1] - pts[0][1], 2));
        float widthBottom = (float) Math.sqrt(
                Math.pow(pts[2][0] - pts[3][0], 2) + Math.pow(pts[2][1] - pts[3][1], 2));
        int maxWidth = (int) Math.max(widthTop, widthBottom);

        float heightLeft = (float) Math.sqrt(
                Math.pow(pts[3][0] - pts[0][0], 2) + Math.pow(pts[3][1] - pts[0][1], 2));
        float heightRight = (float) Math.sqrt(
                Math.pow(pts[2][0] - pts[1][0], 2) + Math.pow(pts[2][1] - pts[1][1], 2));
        int maxHeight = (int) Math.max(heightLeft, heightRight);

        Mat srcMat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dstMat = new Mat(4, 1, CvType.CV_32FC2);

        srcMat.put(0, 0,
                pts[0][0], pts[0][1],
                pts[1][0], pts[1][1],
                pts[2][0], pts[2][1],
                pts[3][0], pts[3][1]);

        dstMat.put(0, 0,
                0, 0,
                maxWidth - 1, 0,
                maxWidth - 1, maxHeight - 1,
                0, maxHeight - 1);

        Mat transformMatrix = Imgproc.getPerspectiveTransform(srcMat, dstMat);
        Mat warped = new Mat();
        Imgproc.warpPerspective(src, warped, transformMatrix, new Size(maxWidth, maxHeight));

        srcMat.release();
        dstMat.release();
        transformMatrix.release();

        return warped;
    }
}