package com.qtone.camerause.widget.capture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.jiangdg.ausbc.utils.ToastUtils;
import com.qtone.camerause.util.log.LogKit;
import com.qtone.camerause.util.log.LogUtils;
import com.qtone.camerause.util.media.MediaScanKit;
import com.qtone.camerause.widget.scan.one.ImageProcessor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created on 2026/8/8.
 *
 * @author 郑少鹏
 * @desc 帧拍照处理器
 */
public class FrameCaptureProcessor {
    /**
     * 单拍状态锁
     * <p>
     * 使用 AtomicBoolean 保证多线程并发环境下的绝对原子性
     */
    private final AtomicBoolean isSingleActive = new AtomicBoolean(false);
    /**
     * 连拍状态锁
     * <p>
     * 使用 AtomicBoolean 保证多线程并发环境下的绝对原子性
     */
    private final AtomicBoolean isBurstActive = new AtomicBoolean(false);
    /**
     * 线程消息调度器
     */
    private final Handler handler = new Handler(Looper.getMainLooper());
    /**
     * 全局 Application Context
     * <p>
     * 规避 Activity / Fragment 内存泄漏
     */
    private volatile Context applicationContext;
    /**
     * 当前拍照模式
     * <p>
     * 使用 volatile 保证多线程读写可见性
     */
    private volatile CaptureMode currentCaptureMode = CaptureMode.SINGLE_CAPTURE;
    /**
     * 连拍模式上次成功捕获预览帧时间戳
     * <p>
     * 单位 - 毫秒
     */
    private volatile long lastCaptureTimestamp = 0L;
    /**
     * 连拍间隔毫秒
     */
    private volatile long burstIntervalMs = 500L;
    /**
     * 增强实现
     */
    private ExecutorService executorService;
    /**
     * 状态
     */
    private volatile boolean isProcessing = false;
    /**
     * ImageProcessor
     */
    private ImageProcessor imageProcessor;

    /**
     * constructor
     */
    public FrameCaptureProcessor() {
        executorService = Executors.newSingleThreadExecutor();
        imageProcessor = new ImageProcessor();
    }

    public static Mat nv21ToMat(byte[] nv21, int width, int height) {
        // 把整个 NV21 当成一个 1.5H × W 的单通道 Mat
        Mat yuv = new Mat(height * 3 / 2, width, CvType.CV_8UC1);
        yuv.put(0, 0, nv21);

        Mat bgr = new Mat();
        Imgproc.cvtColor(yuv, bgr, Imgproc.COLOR_YUV2BGR_NV21);

        yuv.release();
        return bgr;
    }

    /**
     * 开始单拍
     *
     * @param context           上下文
     * @param iCamera           相机实例
     * @param onCaptureCallBack 拍照回调
     */
    public void startSingleCapture(Context context, MultiCameraClient.ICamera iCamera, CaptureProcessor.OnCaptureCallback onCaptureCallBack) {
        Log.d(LogKit.TAG, "开始单拍 - 帧拍照");
        if (CaptureHelper.isCameraNotReady(iCamera, handler, onCaptureCallBack)) {
            return;
        }
        if (context != null) {
            applicationContext = context.getApplicationContext();
        }
        // 当前拍照模式
        currentCaptureMode = CaptureMode.SINGLE_CAPTURE;
        // 单拍状态锁
        isSingleActive.set(true);
        // 连拍状态锁
        isBurstActive.set(false);
        // 重置序号
        CaptureHelper.resetSequence();
        // 通知开始
        CaptureHelper.notifyBegin(handler, onCaptureCallBack);
    }

    /**
     * 开始连拍
     *
     * @param context           上下文
     * @param iCamera           相机实例
     * @param intervalMs        间隔毫秒
     * @param onCaptureCallBack 拍照回调
     */
    public void startBurstCapture(Context context, MultiCameraClient.ICamera iCamera, long intervalMs, CaptureProcessor.OnCaptureCallback onCaptureCallBack) {
        Log.d(LogKit.TAG, "开始连拍 - 帧拍照");
        if (CaptureHelper.isCameraNotReady(iCamera, handler, onCaptureCallBack)) {
            return;
        }
        if (context != null) {
            applicationContext = context.getApplicationContext();
        }
        // 当前拍照模式
        currentCaptureMode = CaptureMode.BURST_CAPTURE;
        // 连拍状态锁
        isBurstActive.set(true);
        // 单拍状态锁
        isSingleActive.set(false);
        // 重置序号
        CaptureHelper.resetSequence();
        // 连拍模式上次成功捕获预览帧时间戳
        lastCaptureTimestamp = 0L;
        // 连拍间隔毫秒
        // 硬性限制下限 150ms 规避硬件写盘过载
        burstIntervalMs = Math.max(150L, intervalMs);
        Log.d(LogKit.TAG, "连拍间隔毫秒 - 帧拍照 || " + burstIntervalMs);
        // 通知开始
        CaptureHelper.notifyBegin(handler, onCaptureCallBack);
    }

    /**
     * 停止连拍
     */
    public void stopBurstCapture() {
        Log.d(LogKit.TAG, "停止连拍 - 帧拍照");
        // 连拍状态锁
        isBurstActive.set(false);
        // 当前拍照模式
        currentCaptureMode = CaptureMode.SINGLE_CAPTURE;
    }

    /**
     * 处理帧
     *
     * @param data       图像帧字节数组
     * @param width      帧物理宽
     * @param height     帧物理高
     * @param dataFormat 数据格式
     */
    public void processFrame(byte[] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat, CaptureProcessor.OnCaptureCallback onCaptureCallBack) {
        if (data == null) {
            return;
        }
        boolean shouldCapture = false;
        if (currentCaptureMode == CaptureMode.SINGLE_CAPTURE) {
            // 单拍
            shouldCapture = isSingleActive.compareAndSet(true, false);
        } else if (currentCaptureMode == CaptureMode.BURST_CAPTURE) {
            // 连拍
            // 依据 [上一次成功捕获预览帧的时间戳 + 最小拍照时间间隔] 控制频率
            if (isBurstActive.get()) {
                long currentTime = SystemClock.elapsedRealtime();
                if ((currentTime - lastCaptureTimestamp) >= burstIntervalMs) {
                    lastCaptureTimestamp = currentTime;
                    shouldCapture = true;
                }
            }
        }
        if (shouldCapture) {
            String savePath = CaptureHelper.generateSavePath(handler, onCaptureCallBack);
            if (savePath == null) {
                return;
            }
            processFrameAsync(data, width, height, dataFormat, savePath, onCaptureCallBack);
        }
    }

    /**
     * 异步处理帧
     *
     * @param data              图像帧字节数组
     * @param width             帧物理宽
     * @param height            帧物理高
     * @param dataFormat        数据格式
     * @param savePath          保存路径
     * @param onCaptureCallBack 拍照回调
     */
    private void processFrameAsync(@NotNull byte @NotNull [] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat, String savePath, CaptureProcessor.OnCaptureCallback onCaptureCallBack) {
        // 校验图像数据物理空间合法性
        // RGBA: width * height * 4 Byte
        // NV21: width * height * 1.5 Byte
        int minRequiredSize = (dataFormat == IPreviewDataCallBack.DataFormat.RGBA) ? (width * height * 4) : (width * height * 3 / 2);
        if (data.length < minRequiredSize) {
            Log.e(LogKit.TAG, String.format(Locale.CHINA, "数据帧异常 - 帧拍照 || 实际长度 (%d) 小于 %dx%d 所需空间", data.length, width, height));
            CaptureHelper.notifyError(handler, onCaptureCallBack, "数据帧截断 - 帧拍照");
            return;
        }
        Log.d(LogKit.TAG, "数据帧捕获成功 - 帧拍照 [" + currentCaptureMode.name() + "] 尺寸 || " + width + "x" + height);
        // 深拷贝隔离内存 Buffer
        // 防止相机底层预览帧覆盖正在处理的数据
        final byte[] processData = Arrays.copyOf(data, data.length);
        // 优先切回主线程通知图像帧字节数组捕获成功
        // 供算法实时分析使用
        handler.post(() -> {
            if (onCaptureCallBack != null) {
                onCaptureCallBack.onCaptureProcessing(processData, width, height, currentCaptureMode);
            }
        });
        // 提取 Context 局部变量
        // 防止异步写盘期间 applicationContext 被 release 显式置空导致 NPE
        final Context appContext = applicationContext;
        // 提交后台单线程池进行 100% 质量 JPEG 编码与磁盘 IO 写盘
        if ((executorService != null) && !executorService.isShutdown()) {
            executorService.execute(() -> processToJpeg(appContext, processData, width, height, dataFormat, savePath, onCaptureCallBack));
        }
    }

    /**
     * 处理帧
     *
     * @param data       图像帧字节数组
     * @param width      帧物理宽
     * @param height     帧物理高
     * @param dataFormat 数据格式
     */
    public void processPaperTestFrame(byte[] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat, CaptureProcessor.OnCaptureCallback onCaptureCallBack) {
        if (data == null) {
            return;
        }
        processPaperTestFrameAsync(data, width, height, dataFormat, onCaptureCallBack);
    }

    /**
     * 试卷检测，异步处理帧
     *
     * @param data              图像帧字节数组
     * @param width             帧物理宽
     * @param height            帧物理高
     * @param dataFormat        数据格式
     * @param onCaptureCallBack 拍照回调
     */
    private void processPaperTestFrameAsync(@NotNull byte @NotNull [] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat, CaptureProcessor.OnCaptureCallback onCaptureCallBack) {
        // 校验图像数据物理空间合法性
        // RGBA: width * height * 4 Byte
        // NV21: width * height * 1.5 Byte
        int minRequiredSize = (dataFormat == IPreviewDataCallBack.DataFormat.RGBA) ? (width * height * 4) : (width * height * 3 / 2);
        if (data.length < minRequiredSize) {
            Log.e(LogKit.TAG, String.format(Locale.CHINA, "数据帧异常 - 帧拍照 || 实际长度 (%d) 小于 %dx%d 所需空间", data.length, width, height));
            CaptureHelper.notifyError(handler, onCaptureCallBack, "数据帧截断 - 帧拍照");
            return;
        }
        Log.d(LogKit.TAG, "数据帧捕获成功 - 帧拍照 [" + currentCaptureMode.name() + "] 尺寸 || " + width + "x" + height);
        // 深拷贝隔离内存 Buffer
        // 防止相机底层预览帧覆盖正在处理的数据
        final byte[] processData = Arrays.copyOf(data, data.length);
        // 优先切回主线程通知图像帧字节数组捕获成功
        // 供算法实时分析使用
        handler.post(() -> {
            if (onCaptureCallBack != null) {
                onCaptureCallBack.onCaptureProcessing(processData, width, height, currentCaptureMode);
            }
        });
        // 提取 Context 局部变量
        // 防止异步写盘期间 applicationContext 被 release 显式置空导致 NPE
        final Context appContext = applicationContext;
        // 提交后台单线程池进行 100% 质量 JPEG 编码与磁盘 IO 写盘
        if ((executorService != null) && !executorService.isShutdown()) {
            executorService.execute(() -> {
                processPaperTest(processData, width, height, dataFormat, onCaptureCallBack);
            });
        }
    }

    /**
     * 处理单帧图像 - 完整的图像处理管线
     * <p>
     * 此方法在后台线程执行
     */
    private void processPaperTest(@NotNull byte @NotNull [] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat, CaptureProcessor.OnCaptureCallback onCaptureCallBack) {
        try {
            // --- Step 1: UvcFrame → OpenCV Mat (RGBA) ---
            Mat rgbaMat = nv21ToMat(data, width, height);
            LogUtils.w("onMatToBitmapProcessing", " UvcFrame → OpenCV Mat (RGBA)");

            if (rgbaMat.empty()) {
                LogUtils.w("onMatToBitmapProcessing", "Failed to convert UvcFrame to Mat");
                finishFrame();
                return;
            }
            Bitmap bitmap = ImageProcessor.nv21ToBitmap(data, width, height);

            if (bitmap != null) {
                handler.post(() -> onCaptureCallBack.onMatToBitmapProcessing(bitmap));
            } else {
                LogUtils.w("onMatToBitmapProcessing", "UvcFrame 转图片为空");
            }

            // --- Step 2: OpenCV 图像处理管线 ---
            // 灰度 → 高斯模糊 → Canny → 轮廓检测 → 四边形筛选 → 透视变换
            Mat warpedMat = imageProcessor.processFrame(rgbaMat);

            if (warpedMat != null && !warpedMat.empty()) {
                // --- Step 3: OCR 预处理 ---
                Mat ocrMat = imageProcessor.preprocessForOcr(warpedMat);

                if (ocrMat != null && !ocrMat.empty()) {
                    // --- Step 4: 转为 Bitmap 并送入 OCR识别 ---
                    Bitmap bitmap2 = ImageProcessor.matToBitmap(ocrMat);
                    if (bitmap2 != null) {
                        // 调用 OCR 识别
                        String recognizedText = "OCR识别图像生成成功";
                        // 将识别结果传回主线程更新 UI
                        final String finalText = recognizedText;
                        handler.post(() -> ToastUtils.show(finalText));

                        LogUtils.d(LogKit.TAG, recognizedText);
                        // 可选：保存识别结果截图
                        saveFrame(ocrMat, "ocr_result");

                        bitmap2.recycle();
                    }
                }

                ocrMat.release();
            }

            rgbaMat.release();

        } catch (Exception e) {
            Log.e(LogKit.TAG, "Error processing frame: " + e.getMessage(), e);
        } finally {
            finishFrame();
        }
    }

    /**
     * 处理为 JPEG
     *
     * @param context           上下文局部引用
     * @param data              图像帧字节数组
     * @param width             帧物理宽
     * @param height            帧物理高
     * @param dataFormat        数据格式
     * @param savePath          保存路径
     * @param onCaptureCallBack 拍照回调
     */
    private void processToJpeg(Context context, byte[] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat, String savePath, CaptureProcessor.OnCaptureCallback onCaptureCallBack) {
        try {
            File targetFile = new File(savePath);
            try (FileOutputStream fileOutputStream = new FileOutputStream(targetFile)) {
                if (dataFormat == IPreviewDataCallBack.DataFormat.RGBA) {
                    // 兼容 RGBA 数据格式
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(data));
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    bitmap.recycle();
                } else {
                    // 默认 NV21 数据格式
                    YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
                    yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, fileOutputStream);
                }
                fileOutputStream.flush();
            }
            // 压缩并覆盖
            // 内部自检测
            CaptureCompressHelper.getInstance().compressAndOverwrite(context, savePath, finalPath -> handleCaptureComplete(context, finalPath, width, height, dataFormat, onCaptureCallBack)
            );
        } catch (Exception e) {
            Log.e(LogKit.TAG, "数据帧写盘异常 - 帧拍照", e);
            CaptureHelper.notifyError(handler, onCaptureCallBack, "数据帧写盘异常 - 帧拍照");
        }
    }

    /**
     * 处理拍照完成
     *
     * @param context           上下文
     * @param savePath          保存路径
     * @param width             帧物理宽
     * @param height            帧物理高
     * @param dataFormat        数据格式
     * @param onCaptureCallBack 拍照回调
     */
    private void handleCaptureComplete(Context context, String savePath, int width, int height, IPreviewDataCallBack.DataFormat dataFormat, CaptureProcessor.OnCaptureCallback onCaptureCallBack) {
        if ((context != null) && (savePath != null)) {
            MediaScanKit.scanSingleFile(context, savePath);
        }
        handler.post(() -> {
            if (onCaptureCallBack != null) {
                Log.d(LogKit.TAG, "图片生成成功 - 帧拍照\n当前拍照模式 " + currentCaptureMode.name() + "\n分辨率 " + width + " x " + height + "\n数据格式 " + dataFormat.name() + "\n保存路径 " + savePath);
                onCaptureCallBack.onCaptureSuccess(savePath, width, height, currentCaptureMode);
            }
        });
    }

    /**
     * 释放
     */
    public void release() {
        // 单拍状态锁
        isSingleActive.set(false);
        // 连拍状态锁
        isBurstActive.set(false);
        // 当前拍照模式
        currentCaptureMode = CaptureMode.SINGLE_CAPTURE;
        // 连拍模式上次成功捕获预览帧时间戳
        lastCaptureTimestamp = 0L;
        // 线程消息调度器
        handler.removeCallbacksAndMessages(null);
        // 增强实现
        if (executorService != null) {
            if (!executorService.isShutdown()) {
                executorService.shutdownNow();
            }
            executorService = null;
        }
        // 全局 Application Context
        applicationContext = null;
        // 拍照压缩辅助者
        CaptureCompressHelper.getInstance().release();
    }

    /**
     * MJPEG 帧解码为 RGBA Mat
     * 使用 Android 的 BitmapFactory 解码 JPEG 数据
     */
    private @Nullable Mat decodeMjpegToMat(byte[] mjpegData, int width, int height) {
        // 将 byte[] 转为 Bitmap
        Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(mjpegData, 0, mjpegData.length);

        if (bitmap == null) {
            Log.e(LogKit.TAG, "Failed to decode MJPEG frame");
            return null;
        }

        // Bitmap → Mat
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        bitmap.recycle();

        // 确保是 4 通道 (RGBA)
        if (mat.channels() == 3) {
            Mat bgrMat = mat;
            mat = new Mat();
            org.opencv.imgproc.Imgproc.cvtColor(bgrMat, mat, org.opencv.imgproc.Imgproc.COLOR_RGB2RGBA);
            bgrMat.release();
        }

        return mat;
    }

    /**
     * YUYV 帧转换为 RGBA Mat
     * YUYV → BGR → RGBA
     */
    private Mat convertYuyvToMat(byte[] yuyvData, int width, int height) {
        // YUYV 每 2 个字节表示一个像素 (Y0 U0 Y1 V1)
        // 先构建 YUV Mat
        Mat yuvMat = new Mat(height * 2, width, CvType.CV_8UC1);
        yuvMat.put(0, 0, yuyvData);

        // YUYV → BGR
        Mat bgrMat = new Mat();
        org.opencv.imgproc.Imgproc.cvtColor(yuvMat, bgrMat, org.opencv.imgproc.Imgproc.COLOR_YUV2BGR_YUYV);

        // BGR → RGBA
        Mat rgbaMat = new Mat();
        org.opencv.imgproc.Imgproc.cvtColor(bgrMat, rgbaMat, org.opencv.imgproc.Imgproc.COLOR_BGR2RGBA);

        yuvMat.release();
        bgrMat.release();

        return rgbaMat;
    }

    /**
     * 保存处理后的帧到本地
     * <p>
     * 调试用
     */
    private void saveFrame(Mat mat, String prefix) {
        try {
            String fileName = CaptureHelper.generateSavePath(handler, null);
            if (fileName == null) {
                return;
            }
            // Mat → byte[]
            MatOfByte matOfByte = new MatOfByte();
            Imgcodecs.imencode(".png", mat, matOfByte);
            byte[] bytes = matOfByte.toArray();

            // 写入文件
            java.io.FileOutputStream fos = new java.io.FileOutputStream(fileName);
            fos.write(bytes);
            fos.close();

            Log.d(LogKit.TAG, "Frame saved: " + fileName);
        } catch (Exception e) {
            Log.e(LogKit.TAG, "Error saving frame: " + e.getMessage(), e);
        }
    }

    /**
     * 标记当前帧处理完成
     */
    private void finishFrame() {
        isProcessing = false;
    }
}