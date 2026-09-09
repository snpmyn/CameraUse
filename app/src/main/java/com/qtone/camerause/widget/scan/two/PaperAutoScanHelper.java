package com.qtone.camerause.widget.scan.two;

import android.os.Handler;
import android.os.HandlerThread;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created on 2026/9/8.
 *
 * @author 郑少鹏
 * @desc 试卷自动检测辅助者
 */
public class PaperAutoScanHelper {
    /**
     * pHash 距离阈值
     * <p>
     * 算法参数
     */
    private static final int PHASH_THRESHOLD = 8;
    /**
     * 降频
     * <p>
     * 节省 CPU
     * 每 150ms 最多处理一帧
     * <p>
     * 算法参数
     */
    private static final long FRAME_INTERVAL_MS = 150;
    /**
     * 连续无手帧数
     * <p>
     * 算法参数
     */
    private static final int REQUIRED_NO_HAND_FRAMES = 5;
    /**
     * 线程消息调度器
     */
    private final Handler handler;
    /**
     * 工作线程
     */
    private final HandlerThread workerThread;
    /**
     * 试卷自动扫描回调
     */
    private final OnPaperAutoScanCallback onPaperAutoScanCallback;
    /**
     * 并发锁 + 防止卡积队列
     */
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    /**
     * 上一次识别成功的试卷感知哈希（pHash）指纹
     * <p>
     * 用于过滤重复拍摄的同一张试卷
     */
    private byte[] lastPHash = null;
    /**
     * 上一次处理帧的时间戳（毫秒）
     * <p>
     * 用于控制帧率/节流，避免过度频繁投递计算任务
     */
    private long lastProcessTime = 0;
    /**
     * 连续检测到无手状态的帧数计数器
     * <p>
     * 必须达到 REQUIRED_NO_HAND_FRAMES 后才认为画面处于无手稳定状态
     */
    private int noHandFrameCounter = 0;
    /**
     * 手部状态标志位（支持多线程安全读写）
     */
    private volatile boolean isHandPresent = false;

    /**
     * constructor
     *
     * @param onPaperAutoScanCallback 试卷自动扫描回调
     */
    public PaperAutoScanHelper(OnPaperAutoScanCallback onPaperAutoScanCallback) {
        this.onPaperAutoScanCallback = onPaperAutoScanCallback;
        // 工作线程
        // 创建独立后台线程运行 OpenCV 计算 -> 绝不阻塞相机预览线程
        this.workerThread = new HandlerThread("PaperAutoScanWorker");
        this.workerThread.start();
        // 线程消息调度器
        this.handler = new Handler(this.workerThread.getLooper());
    }

    /**
     * 手被检测到
     */
    public void onHandDetected() {
        this.isHandPresent = true;
    }

    /**
     * 手完全拿走
     */
    public void onHandRemoved() {
        this.isHandPresent = false;
    }

    /**
     * 处理帧
     * <p>
     * 非阻塞
     *
     * @param data   图像帧字节数组
     * @param width  帧物理宽
     * @param height 帧物理高
     */
    public void processFrame(byte[] data, int width, int height) {
        long currentTime = System.currentTimeMillis();
        // 1. 降频控制
        // 未达到时间间隔或上一帧仍在计算中 -> 直接跳过
        if ((currentTime - lastProcessTime) < FRAME_INTERVAL_MS) {
            return;
        }
        if (!isProcessing.compareAndSet(false, true)) {
            return;
        }
        lastProcessTime = currentTime;
        // 2. 浅拷贝 / 深拷贝数据丢进后台线程异步计算
        byte[] frameData = data.clone();
        handler.post(() -> {
            try {
                analyzeFrame(frameData, width, height);
            } finally {
                // 释放锁
                isProcessing.set(false);
            }
        });
    }

    /**
     * 分析帧
     *
     * @param data   图像帧字节数组
     * @param width  帧物理宽
     * @param height 帧物理高
     */
    private void analyzeFrame(byte[] data, int width, int height) {
        // 1. 调用 MediaPipe 判断画面中是否有手
        boolean hasHand = isHandPresent;
        if (hasHand) {
            noHandFrameCounter = 0;
            notifyStatus("检测到手部操作");
            return;
        }
        noHandFrameCounter++;
        if (noHandFrameCounter < REQUIRED_NO_HAND_FRAMES) {
            notifyStatus("等待画面稳定...");
            return;
        }
        // 2. YUV / NV21 转 OpenCV Mat
        // 通常 UVC 预览数据为 NV21 / YUV_420_888
        Mat grayMat = yuvToGrayMat(data, width, height);
        if (grayMat.empty()) {
            return;
        }
        // 3. OpenCV 检测试卷边缘
        MatOfPoint2f approxContour = findPaperContour(grayMat);
        if (approxContour == null) {
            notifyStatus("未找到完整试卷边缘");
            grayMat.release();
            return;
        }
        // 4. 透视矫正
        Mat warped = fourPointTransform(grayMat, approxContour);
        grayMat.release();
        // 5. pHash 校验是否重复
        byte[] currentPHash = computePHash(warped);
        warped.release();
        if (lastPHash != null) {
            int distance = computeHammingDistance(currentPHash, lastPHash);
            if (distance <= PHASH_THRESHOLD) {
                notifyStatus("重复试卷 (" + distance + ")");
                return;
            }
        }
        // 6. 满足所有条件
        // 更新 Hash 记录并触发拍照
        lastPHash = currentPHash;
        notifyStatus("触发连拍!");
        if (onPaperAutoScanCallback != null) {
            onPaperAutoScanCallback.onTriggerCapture(data, width, height);
        }
    }

    /**
     * YUV 转灰度图像 Mat
     *
     * @param data   图像帧字节数组
     * @param width  帧物理宽
     * @param height 帧物理高
     * @return 灰度图像 Mat
     */
    private Mat yuvToGrayMat(byte[] data, int width, int height) {
        // 取 NV21 / YUV 的 Y 通道 (灰度图)
        // 极快 + 满足边缘提取和 pHash 即可
        Mat yuvMat = new Mat(height + height / 2, width, CvType.CV_8UC1);
        yuvMat.put(0, 0, data);
        Mat grayMat = new Mat(height, width, CvType.CV_8UC1);
        Imgproc.cvtColor(yuvMat, grayMat, Imgproc.COLOR_YUV2GRAY_NV21);
        yuvMat.release();
        return grayMat;
    }

    /**
     * 查找试卷轮廓
     *
     * @param grayMat 灰度图像 Mat
     * @return 包含试卷 4 个顶点的多边形轮廓
     */
    private MatOfPoint2f findPaperContour(Mat grayMat) {
        Mat blurred = new Mat();
        Mat edged = new Mat();
        Imgproc.GaussianBlur(grayMat, blurred, new Size(5, 5), 0);
        Imgproc.Canny(blurred, edged, 50, 150);
        blurred.release();

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edged, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        edged.release();
        hierarchy.release();

        // 至少占画面 20%
        double maxArea = grayMat.rows() * grayMat.cols() * 0.2;
        MatOfPoint2f bestApprox = null;

        for (MatOfPoint matOfPoint : contours) {
            double area = Imgproc.contourArea(matOfPoint);
            if (area > maxArea) {
                MatOfPoint2f matOfPoint2f = new MatOfPoint2f(matOfPoint.toArray());
                double peri = Imgproc.arcLength(matOfPoint2f, true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(matOfPoint2f, approx, 0.02 * peri, true);

                if (approx.total() == 4) {
                    maxArea = area;
                    bestApprox = approx;
                }
            }
        }
        return bestApprox;
    }

    /**
     * 四点透视变换矫正图像
     *
     * @param mat          源图像 Mat
     * @param matOfPoint2f 包含 4 个顶点的多边形轮廓
     * @return 矫正拉平后的图像 Mat
     */
    private Mat fourPointTransform(Mat mat, MatOfPoint2f matOfPoint2f) {
        Point[] points = matOfPoint2f.toArray();
        // 简单按 Y 坐标粗略排序
        // 也可引入更精确的四角点排序
        Point[] rect = sortPoints(points);

        double widthA = Math.hypot(rect[2].x - rect[3].x, rect[2].y - rect[3].y);
        double widthB = Math.hypot(rect[1].x - rect[0].x, rect[1].y - rect[0].y);
        int maxWidth = (int) Math.max(widthA, widthB);

        double heightA = Math.hypot(rect[1].x - rect[2].x, rect[1].y - rect[2].y);
        double heightB = Math.hypot(rect[0].x - rect[3].x, rect[0].y - rect[3].y);
        int maxHeight = (int) Math.max(heightA, heightB);

        MatOfPoint2f srcPts = new MatOfPoint2f(rect);
        MatOfPoint2f dstPts = new MatOfPoint2f(
                new Point(0, 0),
                new Point(maxWidth - 1, 0),
                new Point(maxWidth - 1, maxHeight - 1),
                new Point(0, maxHeight - 1)
        );

        Mat transform = Imgproc.getPerspectiveTransform(srcPts, dstPts);
        Mat warped = new Mat();
        Imgproc.warpPerspective(mat, warped, transform, new Size(maxWidth, maxHeight));

        srcPts.release();
        dstPts.release();
        transform.release();

        return warped;
    }

    /**
     * 对输入的 4 个顶点坐标按照 [左上、右上、右下、左下] 进行顺时针排序
     *
     * @param points 包含 4 个顶点的数组
     * @return 排序完成后的 4 个顶点数组
     */
    private Point[] sortPoints(Point[] points) {
        Point[] result = new Point[4];
        double[] sum = new double[4];
        double[] diff = new double[4];

        for (int i = 0; i < 4; i++) {
            sum[i] = (points[i].x + points[i].y);
            diff[i] = (points[i].y - points[i].x);
        }

        int minSumIdx = 0, maxSumIdx = 0, minDiffIdx = 0, maxDiffIdx = 0;
        for (int i = 1; i < 4; i++) {
            if (sum[i] < sum[minSumIdx]) minSumIdx = i;
            if (sum[i] > sum[maxSumIdx]) maxSumIdx = i;
            if (diff[i] < diff[minDiffIdx]) minDiffIdx = i;
            if (diff[i] > diff[maxDiffIdx]) maxDiffIdx = i;
        }

        // 左上
        result[0] = points[minSumIdx];
        // 右上
        result[1] = points[minDiffIdx];
        // 右下
        result[2] = points[maxSumIdx];
        // 左下
        result[3] = points[maxDiffIdx];
        return result;
    }

    /**
     * 计算图像的感知哈希 (pHash) 指纹
     *
     * @param mat 输入图像 Mat
     * @return 64 字节的 pHash 特征数组
     */
    private byte[] computePHash(Mat mat) {
        Mat resized = new Mat();
        Imgproc.resize(mat, resized, new Size(32, 32));

        Mat floatMat = new Mat();
        resized.convertTo(floatMat, CvType.CV_32F);
        resized.release();

        Mat dct = new Mat();
        Core.dct(floatMat, dct);
        floatMat.release();

        Mat dct8 = dct.submat(0, 8, 0, 8);
        Scalar meanScalar = Core.mean(dct8);
        double avg = meanScalar.val[0];

        byte[] hash = new byte[64];
        int idx = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                hash[idx++] = (byte) (dct8.get(r, c)[0] > avg ? 1 : 0);
            }
        }
        dct8.release();
        dct.release();
        return hash;
    }

    /**
     * 计算两组 pHash 特征指纹之间的汉明距离 (Hamming Distance)
     *
     * @param hash1 指纹数组 1
     * @param hash2 纹数组 2
     * @return 汉明距离 [差异的位数量 + 值越小说明两张图片越相似]
     */
    private int computeHammingDistance(byte[] hash1, byte[] hash2) {
        int dist = 0;
        for (int i = 0; i < hash1.length; i++) {
            if (hash1[i] != hash2[i]) dist++;
        }
        return dist;
    }

    /**
     * 通知状态
     *
     * @param statusMsg 状态消息
     */
    private void notifyStatus(String statusMsg) {
        if (onPaperAutoScanCallback != null) {
            onPaperAutoScanCallback.onStatusUpdate(statusMsg);
        }
    }

    /**
     * 释放
     */
    public void release() {
        if (workerThread != null) {
            workerThread.quitSafely();
        }
    }

    /**
     * 试卷自动扫描回调
     */
    public interface OnPaperAutoScanCallback {
        /**
         * 触发拍照
         *
         * @param data   图像帧字节数组
         *               当前合格的预览数据
         * @param width  帧物理宽
         * @param height 帧物理高
         */
        void onTriggerCapture(byte[] data, int width, int height);

        /**
         * 状态更新
         *
         * @param statusMsg 状态消息
         */
        void onStatusUpdate(String statusMsg);
    }
}