package com.qtone.camerause.widget.wechat;

import android.content.Context;
import android.util.Log;

import com.qtone.camerause.util.asset.AssetFileKit;
import com.qtone.camerause.util.log.LogKit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
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
     * OpenCV DNN 模块
     */
    private Net srNet;
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
            Log.e(LogKit.TAG, "OpenCV 基础库初始化失败  - 微信裁剪");
            return;
        }
        try {
            String p1 = AssetFileKit.copyAssetFileToCache(context, "wechat_model", "detect.prototxt");
            String p2 = AssetFileKit.copyAssetFileToCache(context, "wechat_model", "detect.caffemodel");
            String p3 = AssetFileKit.copyAssetFileToCache(context, "wechat_model", "sr.prototxt");
            String p4 = AssetFileKit.copyAssetFileToCache(context, "wechat_model", "sr.caffemodel");
            // 1. 初始化包含 SR 超分增强能力的微信二维码检测引擎
            weChatQRCode = new WeChatQRCode(p1, p2, p3, p4);
            // 2. 初始化纯 SR 神经网络
            // 用于独立对图像做 2 倍深度超分辨率重建
            srNet = Dnn.readNetFromCaffe(p3, p4);
            isInitialized = true;
            Log.d(LogKit.TAG, "微信 AI 定位与 SR 超分辨率模型加载成功");
        } catch (Exception e) {
            Log.e(LogKit.TAG, "初始化微信视觉模型异常 || " + e.getMessage(), e);
        }
    }

    /**
     * 针对图像进行纯 2 倍 SR (Super-Resolution) 深度学习重建
     *
     * @param srcMat 原始单通道 / 三通道图像
     * @return 2 倍超分辨率重建后 Mat
     */
    @Nullable
    public Mat superResolve(Mat srcMat) {
        if (!isInitialized || (srNet == null) || (srcMat == null) || srcMat.empty()) {
            return null;
        }
        Mat inputBlob = null;
        Mat outputBlob = null;
        List<Mat> blobImages = new ArrayList<>();
        Mat floatMat = new Mat();
        Mat normalizedMat = new Mat();
        Mat ySr8U = new Mat();
        // 通道分离 / 转换用到的 Mat
        Mat ycrcbMat = new Mat();
        List<Mat> ycrcbChannels = new ArrayList<>();
        Mat crResized = new Mat();
        Mat cbResized = new Mat();
        List<Mat> srYcrcbChannels = new ArrayList<>();
        Mat mergedYcrcb = new Mat();
        Mat colorResult = new Mat();
        try {
            boolean isColor = srcMat.channels() >= 3;
            Mat yMat;
            if (isColor) {
                // 1. 转到 YCrCb 颜色空间
                // 提取 Y(亮度通道) 与 Cr, Cb(色彩通道)
                Imgproc.cvtColor(srcMat, ycrcbMat, Imgproc.COLOR_BGR2YCrCb);
                Core.split(ycrcbMat, ycrcbChannels);
                // Y 通道
                yMat = ycrcbChannels.get(0);
            } else {
                yMat = srcMat;
            }
            // 2. 仅对 Y 通道进行 SR 神经网络推理
            inputBlob = Dnn.blobFromImage(yMat, 1.0, new Size(yMat.cols(), yMat.rows()));
            srNet.setInput(inputBlob);
            outputBlob = srNet.forward();
            Dnn.imagesFromBlob(outputBlob, blobImages);
            if (blobImages.isEmpty() || blobImages.get(0).empty()) {
                return null;
            }
            Mat rawOutput = blobImages.get(0);
            rawOutput.convertTo(floatMat, CvType.CV_32FC1);
            // 3. 归一化 Y 通道超分结果
            Core.normalize(floatMat, normalizedMat, 0, 255, Core.NORM_MINMAX, CvType.CV_32FC1);
            normalizedMat.convertTo(ySr8U, CvType.CV_8UC1);
            // 4. 如果是彩色图，把 Cr 和 Cb 通道按照超分后的 Y 通道尺寸放大并重新融合色彩
            if (isColor) {
                // 超分后的新尺寸 [OutW, OutH]
                Size srSize = ySr8U.size();
                // 对 Cr, Cb 双三次插值放大到与 Y 通道相同尺寸
                Imgproc.resize(ycrcbChannels.get(1), crResized, srSize, 0, 0, Imgproc.INTER_CUBIC);
                Imgproc.resize(ycrcbChannels.get(2), cbResized, srSize, 0, 0, Imgproc.INTER_CUBIC);
                // 合并 [SR_Y, Resized_Cr, Resized_Cb]
                srYcrcbChannels.add(ySr8U);
                srYcrcbChannels.add(crResized);
                srYcrcbChannels.add(cbResized);
                Core.merge(srYcrcbChannels, mergedYcrcb);
                // 还原回 BGR 彩色通道
                Imgproc.cvtColor(mergedYcrcb, colorResult, Imgproc.COLOR_YCrCb2BGR);
                return colorResult;
            }
            return ySr8U;
        } catch (Exception e) {
            Log.e(LogKit.TAG, "SR 超分辨率推理异常 - 微信裁剪 || " + e.getMessage(), e);
            ySr8U.release();
            return null;
        } finally {
            if (inputBlob != null) {
                inputBlob.release();
            }
            if (outputBlob != null) {
                outputBlob.release();
            }
            for (Mat mat : blobImages) {
                if (mat != null) {
                    mat.release();
                }
            }
            floatMat.release();
            normalizedMat.release();
            ycrcbMat.release();
            for (Mat mat : ycrcbChannels) {
                if (mat != null) {
                    mat.release();
                }
            }
            crResized.release();
            cbResized.release();
            mergedYcrcb.release();
        }
    }

    /**
     * 检测并裁剪图中所有的二维码 / 条形码
     * <p>
     * 内置 AI 超分增强与多码识别
     *
     * @param srcMat Mat
     * @return 裁剪出来的目标 Mat 列表
     */
    @NotNull
    public List<Mat> detectAndCrop(Mat srcMat) {
        List<Mat> cropResultList = new ArrayList<>();
        if (!isInitialized || (weChatQRCode == null) || (srcMat == null) || srcMat.empty()) {
            return cropResultList;
        }
        List<Mat> pointsList = new ArrayList<>();
        Mat detectMat = srcMat;
        double scale = 1.0;
        try {
            int maxDim = Math.max(srcMat.cols(), srcMat.rows());
            if (maxDim > 1080) {
                scale = 1080.0 / maxDim;
                detectMat = new Mat();
                Imgproc.resize(srcMat, detectMat, new Size(srcMat.cols() * scale, srcMat.rows() * scale));
            }
            // 内部检测并拉取图中所有二维码的角点
            weChatQRCode.detectAndDecode(detectMat, pointsList);
            // 遍历所有检测到的二维码位置矩阵
            for (Mat cornerMat : pointsList) {
                if ((cornerMat != null) && !cornerMat.empty()) {
                    MatOfPoint2f matOfPoint2f = new MatOfPoint2f(cornerMat);
                    Point[] points = matOfPoint2f.toArray();
                    if (points.length >= 4) {
                        Point[] codeCorners = new Point[4];
                        for (int i = 0; i < 4; i++) {
                            codeCorners[i] = new Point(points[i].x / scale, points[i].y / scale);
                        }
                        Mat cropped = cropCodeArea(srcMat, codeCorners);
                        if ((cropped != null) && !cropped.empty()) {
                            cropResultList.add(cropped);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(LogKit.TAG, "微信 AI 二维码 / 条码检测异常 || " + e.getMessage(), e);
        } finally {
            releasePointsList(pointsList);
            if (detectMat != srcMat) {
                detectMat.release();
            }
        }
        return cropResultList;
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
     * 根据二维码 / 条形码 4 个顶点坐标进行透视变换 (Perspective Transform) 矫正并裁剪图像
     * <p>
     * 核心处理逻辑
     * 1. 计算 4 个顶点间边长，动态确定矫正后的目标宽高。
     * 2. 自动计算 10% 的外边距，防止紧贴边缘导致识别困难或切掉二维码定位角。
     * 3. 使用 OpenCV 的 {@link Imgproc#getPerspectiveTransform} 计算单应性透视变换矩阵
     * 4. 执行 {@link Imgproc#warpPerspective} 将倾斜、变形的二维码拉直为标准矩形区域
     *
     * @param srcMat      输入的原始图像矩阵
     *                    BGR 或单通道灰度
     * @param codeCorners 二维码 / 条形码 4 个顶点坐标数组
     *                    索引顺序通常为
     *                    [0] 左上 (Top-Left)
     *                    [1] 右上 (Top-Right)
     *                    [2] 右下 (Bottom-Right)
     *                    [3] 左下 (Bottom-Left)
     * @return 矫正并裁剪后的标准矩形图像 {@link Mat} [计算失败或坐标异常则返回 {@code null}]
     */
    @Nullable
    private Mat cropCodeArea(Mat srcMat, @NotNull Point @NotNull [] codeCorners) {
        double widthTop = Math.hypot(codeCorners[1].x - codeCorners[0].x, codeCorners[1].y - codeCorners[0].y);
        double widthBottom = Math.hypot(codeCorners[2].x - codeCorners[3].x, codeCorners[2].y - codeCorners[3].y);
        int targetWidth = (int) Math.max(widthTop, widthBottom);
        double heightLeft = Math.hypot(codeCorners[3].x - codeCorners[0].x, codeCorners[3].y - codeCorners[0].y);
        double heightRight = Math.hypot(codeCorners[2].x - codeCorners[1].x, codeCorners[2].y - codeCorners[1].y);
        int targetHeight = (int) Math.max(heightLeft, heightRight);
        if ((targetWidth <= 0) || (targetHeight <= 0)) {
            return null;
        }
        int paddingX = (int) (targetWidth * 0.10);
        int paddingY = (int) (targetHeight * 0.10);
        int finalWidth = targetWidth + paddingX * 2;
        int finalHeight = targetHeight + paddingY * 2;
        MatOfPoint2f srcPts = null;
        MatOfPoint2f dstPts = null;
        Mat transformMatrix = null;
        Mat destMat = new Mat();
        try {
            srcPts = new MatOfPoint2f(codeCorners);
            dstPts = new MatOfPoint2f(
                    new Point(paddingX, paddingY),
                    new Point(paddingX + targetWidth - 1, paddingY),
                    new Point(paddingX + targetWidth - 1, paddingY + targetHeight - 1),
                    new Point(paddingX, paddingY + targetHeight - 1)
            );
            transformMatrix = Imgproc.getPerspectiveTransform(srcPts, dstPts);
            Imgproc.warpPerspective(srcMat, destMat, transformMatrix, new Size(finalWidth, finalHeight));
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