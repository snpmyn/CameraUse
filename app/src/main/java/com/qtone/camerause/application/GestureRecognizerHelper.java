package com.qtone.camerause.application;

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

/**
 * Created on 2026/9/4.
 *
 * @author 郑少鹏
 * @desc 手势识别辅助者
 */
public class GestureRecognizerHelper {
    public static final int DELEGATE_CPU = 0;
    public static final int DELEGATE_GPU = 1;
    private static final String TAG = "GestureRecognizerHelper";
    private static final String MP_RECOGNIZER_TASK = "gesture_recognizer.task";
    private final Context context;
    private final GestureRecognizerListener listener;
    private GestureRecognizer gestureRecognizer;
    private float minHandDetectionConfidence = 0.5f;
    private float minHandTrackingConfidence = 0.5f;
    private float minHandPresenceConfidence = 0.5f;
    private int currentDelegate = DELEGATE_CPU;

    public GestureRecognizerHelper(Context context, GestureRecognizerListener listener) {
        this.context = context;
        this.listener = listener;
        setupGestureRecognizer();
    }

    public GestureRecognizerHelper(Context context,
                                   GestureRecognizerListener listener,
                                   float minHandDetectionConfidence,
                                   float minHandTrackingConfidence,
                                   float minHandPresenceConfidence,
                                   int currentDelegate) {
        this.context = context;
        this.listener = listener;
        this.minHandDetectionConfidence = minHandDetectionConfidence;
        this.minHandTrackingConfidence = minHandTrackingConfidence;
        this.minHandPresenceConfidence = minHandPresenceConfidence;
        this.currentDelegate = currentDelegate;
        setupGestureRecognizer();
    }

    /**
     * 初始化 MediaPipe GestureRecognizer
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
        baseOptionsBuilder.setModelAssetPath(MP_RECOGNIZER_TASK);
        try {
            BaseOptions baseOptions = baseOptionsBuilder.build();
            GestureRecognizer.GestureRecognizerOptions options =
                    GestureRecognizer.GestureRecognizerOptions.builder()
                            .setBaseOptions(baseOptions)
                            .setMinHandDetectionConfidence(minHandDetectionConfidence)
                            .setMinTrackingConfidence(minHandTrackingConfidence)
                            .setMinHandPresenceConfidence(minHandPresenceConfidence)
                            .setRunningMode(RunningMode.LIVE_STREAM) // 必须设为实时视频流模式
                            .setResultListener(this::returnLivestreamResult) // 结果回调
                            .setErrorListener(this::returnLivestreamError) // 异常回调
                            .build();
            gestureRecognizer = GestureRecognizer.createFromOptions(context, options);
        } catch (IllegalStateException e) {
            if (listener != null) {
                listener.onError("GestureRecognizer 初始化失败: " + e.getMessage());
            }
            Log.e(TAG, "加载任务失败", e);
        } catch (RuntimeException e) {
            if (listener != null) {
                listener.onError("GPU/硬件加速错误: " + e.getMessage());
            }
            Log.e(TAG, "加载任务失败", e);
        }
    }

    /**
     * 接收 UVC 相机的实时视频帧 Bitmap 并进行识别
     *
     * @param bitmap 当前视频帧 Bitmap
     *               推荐 ARGB_8888 格式
     */
    public void recognizeLiveStream(Bitmap bitmap) {
        if ((gestureRecognizer == null) || (bitmap == null) || bitmap.isRecycled()) {
            return;
        }
        long frameTime = SystemClock.uptimeMillis();
        // 1. 将 Android Bitmap 转为 MediaPipe 的 MPImage 对象
        MPImage mpImage = new BitmapImageBuilder(bitmap).build();
        // 2. 异步送入 AI 模型开始推理
        gestureRecognizer.recognizeAsync(mpImage, frameTime);
    }

    /**
     * 接收 MediaPipe 内部推断完成后的结果
     */
    private void returnLivestreamResult(GestureRecognizerResult result, MPImage input) {
        long finishTimeMs = SystemClock.uptimeMillis();
        // 修改说明
        // 使用 result.timestampMs() 替代 input.getTimestamp()，避免访问包级私有方法。
        long inferenceTime = (finishTimeMs - ((result != null) ? result.timestampMs() : finishTimeMs));
        String topGestureName = "None";
        // 解析最高置信度的手势名称
        if ((result != null) && !result.gestures().isEmpty() && !result.gestures().get(0).isEmpty()) {
            Category topGesture = result.gestures().get(0).get(0);
            topGestureName = topGesture.categoryName();
        }
        // 回调给外部使用
        if (listener != null) {
            listener.onResults(result, topGestureName, inferenceTime);
        }
    }

    /**
     * 接收识别过程中的异常
     */
    private void returnLivestreamError(RuntimeException error) {
        if (listener != null) {
            listener.onError((error != null) ? error.getMessage() : "未知识别错误");
        }
    }

    /**
     * 检查识别器是否已关闭
     */
    public boolean isClosed() {
        return gestureRecognizer == null;
    }

    /**
     * 释放
     */
    public void release() {
        if (gestureRecognizer != null) {
            gestureRecognizer.close();
            gestureRecognizer = null;
        }
    }

    // 回调接口：向外部抛出识别结果
    public interface GestureRecognizerListener {
        void onError(String error);

        void onResults(GestureRecognizerResult result, String topGestureName, long inferenceTime);
    }
}