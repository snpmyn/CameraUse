package com.qtone.camerause.function.wechat;

import android.content.Context;
import android.util.Log;

import com.qtone.camerause.util.asset.AssetFileKit;
import com.qtone.camerause.util.log.LogKit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.wechat_qrcode.WeChatQRCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2026/8/4.
 *
 * @author 郑少鹏
 * @desc 微信裁剪辅助者
 */
public class WeChatCropHelper {
    /**
     * WeChatQRCode
     */
    private WeChatQRCode weChatQRCode;
    /**
     * 是否已初始化
     */
    private volatile boolean isInitialized = false;

    /**
     * 初始化
     *
     * @param context 上下文
     */
    public void init(Context context) {
        if (!OpenCVLoader.initDebug()) {
            Log.e(LogKit.TAG, "OpenCV 基础库初始化失败");
            return;
        }
        try {
            String p1 = AssetFileKit.copyAssetFileToCache(context, "wechat_model", "detect.prototxt");
            String p2 = AssetFileKit.copyAssetFileToCache(context, "wechat_model", "detect.caffemodel");
            String p3 = AssetFileKit.copyAssetFileToCache(context, "wechat_model", "sr.prototxt");
            String p4 = AssetFileKit.copyAssetFileToCache(context, "wechat_model", "sr.caffemodel");
            weChatQRCode = new WeChatQRCode(p1, p2, p3, p4);
            isInitialized = true;
            Log.d(LogKit.TAG, "微信视觉模型加载成功");
        } catch (Exception e) {
            Log.e(LogKit.TAG, "初始化微信视觉模型异常 || " + e.getMessage(), e);
        }
    }

    /**
     * 检测并裁剪
     *
     * @param srcMat 原始图像
     * @return 裁剪拉平后的 Mat
     */
    public Mat detectAndCrop(Mat srcMat) {
        if (!isInitialized || (weChatQRCode == null) || (srcMat == null) || srcMat.empty()) {
            return null;
        }
        // 1. 优先提取右上角 ROI
        // 覆盖宽度的右上 45% + 高度的顶部 40%
        int cols = srcMat.cols();
        int rows = srcMat.rows();
        int roiX = (int) (cols * 0.55);
        int roiY = 0;
        int roiWidth = Math.max(1, cols - roiX);
        int roiHeight = Math.max(1, (int) (rows * 0.40));
        Rect roiRect = new Rect(roiX, roiY, roiWidth, roiHeight);
        Mat roiMat = null;
        List<Mat> pointsList = new ArrayList<>();
        Point[] paperCorners = null;
        try {
            roiMat = new Mat(srcMat, roiRect);
            weChatQRCode.detectAndDecode(roiMat, pointsList);
            if (!pointsList.isEmpty()) {
                // ROI 区域识别成功，将坐标映射回全图坐标系。
                Mat cornerMat = pointsList.get(0);
                paperCorners = new Point[4];
                for (int i = 0; i < 4; i++) {
                    double[] p = cornerMat.get(i, 0);
                    paperCorners[i] = new Point(p[0] + roiX, p[1] + roiY);
                }
            } else {
                // 2. 若 ROI 未匹配
                // 释放列表并回退至全图检测
                releasePointsList(pointsList);
                weChatQRCode.detectAndDecode(srcMat, pointsList);
                if (!pointsList.isEmpty()) {
                    Mat cornerMat = pointsList.get(0);
                    paperCorners = new Point[4];
                    for (int i = 0; i < 4; i++) {
                        double[] p = cornerMat.get(i, 0);
                        paperCorners[i] = new Point(p[0], p[1]);
                    }
                }
            }
        } finally {
            // 确保中间矩阵与角点列表百分之百被释放
            // 绝不泄露 Native 内存
            releasePointsList(pointsList);
            if (roiMat != null) {
                roiMat.release();
            }
        }
        if (paperCorners != null) {
            return warpPerspective(srcMat, paperCorners);
        }
        return null;
    }

    /**
     * 释放角点矩阵集合
     *
     * @param pointsList 角点矩阵集合
     */
    private void releasePointsList(List<Mat> pointsList) {
        if (pointsList != null) {
            for (Mat mat : pointsList) {
                if (mat != null) {
                    mat.release();
                }
            }
            pointsList.clear();
        }
    }

    /**
     * 透视变换
     */
    @Nullable
    private Mat warpPerspective(Mat srcMat, @NotNull Point @NotNull [] paperCorners) {
        double widthTop = Math.hypot(paperCorners[1].x - paperCorners[0].x, paperCorners[1].y - paperCorners[0].y);
        double widthBottom = Math.hypot(paperCorners[2].x - paperCorners[3].x, paperCorners[2].y - paperCorners[3].y);
        int targetWidth = (int) Math.max(widthTop, widthBottom);
        double heightLeft = Math.hypot(paperCorners[3].x - paperCorners[0].x, paperCorners[3].y - paperCorners[0].y);
        double heightRight = Math.hypot(paperCorners[2].x - paperCorners[1].x, paperCorners[2].y - paperCorners[1].y);
        int targetHeight = (int) Math.max(heightLeft, heightRight);
        if ((targetWidth <= 0) || (targetHeight <= 0)) {
            return null;
        }
        MatOfPoint2f srcPts = null;
        MatOfPoint2f dstPts = null;
        Mat transformMatrix = null;
        Mat destMat = new Mat();
        try {
            srcPts = new MatOfPoint2f(paperCorners);
            dstPts = new MatOfPoint2f(
                    new Point(0, 0),
                    new Point(targetWidth - 1, 0),
                    new Point(targetWidth - 1, targetHeight - 1),
                    new Point(0, targetHeight - 1)
            );
            transformMatrix = Imgproc.getPerspectiveTransform(srcPts, dstPts);
            Imgproc.warpPerspective(srcMat, destMat, transformMatrix, new Size(targetWidth, targetHeight));
            return destMat;
        } catch (Exception e) {
            destMat.release();
            return null;
        } finally {
            // 在 finally 中防偏泄露释放所有临时 C++ 矩阵
            if (srcPts != null) {
                srcPts.release();
            }
            if (dstPts != null) {
                dstPts.release();
            }
            if (transformMatrix != null) {
                transformMatrix.release();
            }
        }
    }
}