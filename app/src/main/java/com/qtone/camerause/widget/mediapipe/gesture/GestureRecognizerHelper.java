package com.qtone.camerause.widget.mediapipe.gesture;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.Category;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult;
import com.qtone.camerause.util.log.LogKit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created on 2026/9/4.
 *
 * @author 郑少鹏
 * @desc 手势识别辅助者
 */
public class GestureRecognizerHelper {
    /**
     * 硬件加速 Delegate 模式
     * <p>
     * CPU
     */
    public static final int DELEGATE_CPU = 0;
    /**
     * 硬件加速 Delegate 模式
     * <p>
     * GPU
     */
    public static final int DELEGATE_GPU = 1;
    /**
     * mediapipe 识别任务
     */
    private static final String MEDIAPIPE_RECOGNIZER_TASK = "gesture_recognizer.task";
    /**
     * 待回收 Bitmap 映射集
     * <p>
     * 使用时间戳准确匹配
     * 规避多线程异步乱序回收错乱
     */
    private final Map<Long, Bitmap> pendingBitmapMap = new ConcurrentHashMap<>();
    /**
     * 上下文
     */
    private final Context context;
    /**
     * 手势识别回调
     */
    private final GestureRecognizerCallback gestureRecognizerCallback;
    /**
     * 当前硬件加速 Delegate 模式
     * <p>
     * 默认优先 GPU
     */
    private int currentDelegate = DELEGATE_GPU;
    /**
     * 手势识别器
     */
    private GestureRecognizer gestureRecognizer;
    /**
     * 最小手部检测置信度
     * <p>
     * 0.0 - 1.0
     * <p>
     * 降至 0.35f 提升识别灵敏度
     */
    private float minHandDetectionConfidence = 0.35f;
    /**
     * 最小手部追踪置信度
     * <p>
     * 0.0 - 1.0
     */
    private float minHandTrackingConfidence = 0.5f;
    /**
     * 最小手部存在置信度
     * <p>
     * 0.0 - 1.0
     * <p>
     * 降至 0.35f 提升识别灵敏度
     */
    private float minHandPresenceConfidence = 0.35f;

    /**
     * constructor
     *
     * @param context                   上下文
     * @param gestureRecognizerCallback 手势识别回调
     */
    public GestureRecognizerHelper(Context context, GestureRecognizerCallback gestureRecognizerCallback) {
        // 上下文
        this.context = context;
        // 手势识别回调
        this.gestureRecognizerCallback = gestureRecognizerCallback;
        // 初始化手势识别器
        setupGestureRecognizer();
    }

    /**
     * constructor
     *
     * @param context                    上下文
     * @param gestureRecognizerCallback  手势识别回调
     * @param minHandDetectionConfidence 最小手部检测置信度
     *                                   范围 0.0 - 1.0
     * @param minHandTrackingConfidence  最小手部追踪置信度
     *                                   范围 0.0 - 1.0
     * @param minHandPresenceConfidence  最小手部存在置信度
     *                                   范围 0.0 - 1.0
     * @param currentDelegate            当前硬件加速 Delegate 模式
     *                                   如 {@link #DELEGATE_CPU}
     *                                   或 {@link #DELEGATE_GPU}
     */
    public GestureRecognizerHelper(Context context, GestureRecognizerCallback gestureRecognizerCallback, float minHandDetectionConfidence, float minHandTrackingConfidence, float minHandPresenceConfidence, int currentDelegate) {
        // 上下文
        this.context = context;
        // 当前硬件加速 Delegate 模式
        this.currentDelegate = currentDelegate;
        // 手势识别回调
        this.gestureRecognizerCallback = gestureRecognizerCallback;
        // 最小手部检测置信度
        this.minHandDetectionConfidence = minHandDetectionConfidence;
        // 最小手部追踪置信度
        this.minHandTrackingConfidence = minHandTrackingConfidence;
        // 最小手部存在置信度
        this.minHandPresenceConfidence = minHandPresenceConfidence;
        // 初始化手势识别器
        setupGestureRecognizer();
    }

    /**
     * 初始化手势识别器
     */
    private void setupGestureRecognizer() {
        BaseOptions.Builder baseOptionsBuilder = BaseOptions.builder();
        // 配置硬件加速 Delegate
        if (currentDelegate == DELEGATE_GPU) {
            baseOptionsBuilder.setDelegate(Delegate.GPU);
        } else {
            baseOptionsBuilder.setDelegate(Delegate.CPU);
        }
        // 设置位于 assets 目录下的模型文件名
        baseOptionsBuilder.setModelAssetPath(MEDIAPIPE_RECOGNIZER_TASK);
        try {
            BaseOptions baseOptions = baseOptionsBuilder.build();
            GestureRecognizer.GestureRecognizerOptions gestureRecognizerOptions = GestureRecognizer.GestureRecognizerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinHandDetectionConfidence(minHandDetectionConfidence)
                    .setMinTrackingConfidence(minHandTrackingConfidence)
                    .setMinHandPresenceConfidence(minHandPresenceConfidence)
                    .setRunningMode(RunningMode.LIVE_STREAM) // 必须设为实时视频流模式
                    .setResultListener(this::returnLiveStreamResult) // 结果回调
                    .setErrorListener(this::returnLiveStreamError) // 异常回调
                    .build();
            gestureRecognizer = GestureRecognizer.createFromOptions(context, gestureRecognizerOptions);
        } catch (IllegalStateException e) {
            Log.e(LogKit.TAG, "手势识别 - 加载任务失败", e);
            if (gestureRecognizerCallback != null) {
                gestureRecognizerCallback.onGestureRecognizerError("手势识别器初始化失败: " + e.getMessage());
            }
        } catch (RuntimeException e) {
            Log.e(LogKit.TAG, "手势识别 - 加载任务失败", e);
            if (gestureRecognizerCallback != null) {
                gestureRecognizerCallback.onGestureRecognizerError("手势识别 - GPU / 硬件加速错误: " + e.getMessage());
            }
        }
    }

    /**
     * 识别实时流
     * <p>
     * 识别 UVC 相机实时视频帧像素数据
     *
     * @param bitmap 当前视频帧像素数据
     *               推荐 ARGB_8888 格式
     */
    public void recognizeLiveStream(Bitmap bitmap) {
        if ((gestureRecognizer == null) || (bitmap == null) || bitmap.isRecycled()) {
            return;
        }
        long frameTime = SystemClock.uptimeMillis();
        // 以时间戳为 Key 记录待回收 Bitmap
        pendingBitmapMap.put(frameTime, bitmap);
        try {
            // 1. 将 Android Bitmap 转为 MediaPipe 的 MPImage 对象
            MPImage mpImage = new BitmapImageBuilder(bitmap).build();
            // 2. 异步送入 AI 模型开始推理
            gestureRecognizer.recognizeAsync(mpImage, frameTime);
        } catch (Exception e) {
            // 识别过程出现异常
            // 立即清理回收
            Bitmap remove = pendingBitmapMap.remove(frameTime);
            if (remove != null && !remove.isRecycled()) {
                remove.recycle();
            }
            throw e;
        }
    }

    /**
     * 返回实时流结果
     * <p>
     * 接收 MediaPipe 内部推断完成后结果
     *
     * @param gestureRecognizerResult 手势识别结果
     * @param mpImage                 输入的 MediaPipe 图像对象
     */
    private void returnLiveStreamResult(GestureRecognizerResult gestureRecognizerResult, MPImage mpImage) {
        try {
            long finishTimeMs = SystemClock.uptimeMillis();
            long timestamp = (gestureRecognizerResult != null) ? gestureRecognizerResult.timestampMs() : finishTimeMs;
            long inferenceTime = finishTimeMs - timestamp;
            // 根据时间戳精确回收当前帧 Bitmap
            recycleBitmapByTimestamp(timestamp);
            String topGestureName = "None";
            // 解析最高置信度的手势名称
            if ((gestureRecognizerResult != null) && !gestureRecognizerResult.gestures().isEmpty() && !gestureRecognizerResult.gestures().get(0).isEmpty()) {
                Category topGesture = gestureRecognizerResult.gestures().get(0).get(0);
                topGestureName = topGesture.categoryName();
            }
            // 回调给外部使用
            if (gestureRecognizerCallback != null) {
                gestureRecognizerCallback.onGestureRecognizerResult(gestureRecognizerResult, topGestureName, inferenceTime);
            }
        } finally {
            if (mpImage != null) {
                mpImage.close();
            }
        }
    }

    /**
     * 返回实时流错误
     *
     * @param runtimeException 运行异常
     */
    private void returnLiveStreamError(RuntimeException runtimeException) {
        // 出错时清空并回收所有滞留 Bitmap
        clearAndRecycleAllBitmap();
        if (gestureRecognizerCallback != null) {
            gestureRecognizerCallback.onGestureRecognizerError((runtimeException != null) ? runtimeException.getMessage() : "未知手势识别错误");
        }
    }

    /**
     * 根据时间戳回收 Bitmap
     *
     * @param timestamp 时间戳
     *                  对应帧时间戳
     */
    private void recycleBitmapByTimestamp(long timestamp) {
        Bitmap bitmap = pendingBitmapMap.remove(timestamp);
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    /**
     * 清空并回收所有 Bitmap
     */
    private void clearAndRecycleAllBitmap() {
        for (Long timestamp : pendingBitmapMap.keySet()) {
            Bitmap bitmap = pendingBitmapMap.remove(timestamp);
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    /**
     * 设置硬件加速 Delegate 模式
     *
     * @param delegate 硬件加速 Delegate 模式
     *                 如 {@link #DELEGATE_CPU}
     *                 或 {@link #DELEGATE_GPU}
     */
    public void setDelegate(int delegate) {
        if (this.currentDelegate != delegate) {
            this.currentDelegate = delegate;
            // 释放
            release();
            // 初始化手势识别器
            setupGestureRecognizer();
        }
    }

    /**
     * 设置置信度阈值
     *
     * @param minHandDetectionConfidence 最小手部检测置信度
     * @param minHandTrackingConfidence  最小手部追踪置信度
     * @param minHandPresenceConfidence  最小手部存在置信度
     */
    public void setConfidenceThresholds(float minHandDetectionConfidence, float minHandTrackingConfidence, float minHandPresenceConfidence) {
        // 最小手部检测置信度
        this.minHandDetectionConfidence = minHandDetectionConfidence;
        // 最小手部追踪置信度
        this.minHandTrackingConfidence = minHandTrackingConfidence;
        // 最小手部存在置信度
        this.minHandPresenceConfidence = minHandPresenceConfidence;
        // 释放
        release();
        // 初始化手势识别器
        setupGestureRecognizer();
    }

    /**
     * 手势识别器是否已关闭
     *
     * @return 手势识别器是否已关闭
     */
    public boolean gestureRecognizerIsClosed() {
        return (gestureRecognizer == null);
    }

    /**
     * 释放
     */
    public void release() {
        clearAndRecycleAllBitmap();
        if (gestureRecognizer != null) {
            gestureRecognizer.close();
            gestureRecognizer = null;
        }
    }
}