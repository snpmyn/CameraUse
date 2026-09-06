package com.qtone.camerause.widget.gesture;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created on 2026/9/4.
 *
 * @author 郑少鹏
 * @desc 手势管理器
 */
public class GestureManager implements GestureRecognizerHelper.GestureRecognizerListener {
    private static final String TAG = GestureManager.class.getSimpleName();
    private final GestureRecognizerHelper gestureRecognizerHelper;
    private final OnGestureRecognizedListener externalListener;
    private final ExecutorService backgroundExecutor;
    private final Handler mainHandler;
    /**
     * 标志当前是否有帧正在处理中
     * <p>
     * 用于控频丢帧
     */
    private final AtomicBoolean isProcessingFrame = new AtomicBoolean(false);

    public GestureManager(Context context, OnGestureRecognizedListener listener) {
        this.externalListener = listener;
        this.mainHandler = new Handler(Looper.getMainLooper());
        // 创建单线程池
        // 专门用于处理图像转换与 AI 帧识别
        this.backgroundExecutor = Executors.newSingleThreadExecutor();
        // 初始化辅助类
        // 默认 CPU 模式
        // 可改为 DELEGATE_GPU
        this.gestureRecognizerHelper = new GestureRecognizerHelper(context, this);
    }

    /**
     * 处理预览帧
     *
     * @param data   图像帧字节数组
     * @param width  帧物理宽
     * @param height 帧物理高
     */
    public void processPreviewFrame(byte[] data, int width, int height) {
        if ((data == null) || (gestureRecognizerHelper == null) || (gestureRecognizerHelper.isClosed())) {
            return;
        }
        // 控频
        // 如果上一帧还在转换 / 识别中，直接丢弃当前帧，保障实时性与内存稳定。
        if (!isProcessingFrame.compareAndSet(false, true)) {
            return;
        }
        backgroundExecutor.execute(() -> {
            try {
                // 1. 将 YUV 格式转换成 Bitmap
                Bitmap frameBitmap = YuvToBitmapKit.nv21ToBitmap(data, width, height);
                if ((frameBitmap != null) && !gestureRecognizerHelper.isClosed()) {
                    // 2. 送入识别器开始识别
                    gestureRecognizerHelper.recognizeLiveStream(frameBitmap);
                } else {
                    isProcessingFrame.set(false);
                }
            } catch (Exception e) {
                Log.e(TAG, "处理帧数据异常", e);
                isProcessingFrame.set(false);
            }
        });
    }

    @Override
    public void onResults(GestureRecognizerResult result, String topGestureName, long inferenceTime) {
        // 完成处理，释放标志位，允许处理下一帧。
        isProcessingFrame.set(false);
        // 切回主线程交由外部 UI / 业务处理
        if (externalListener != null) {
            mainHandler.post(() -> externalListener.onGestureRecognized(topGestureName, result, inferenceTime));
        }
    }

    @Override
    public void onError(String error) {
        // 出错时同样重置标志位
        isProcessingFrame.set(false);
        if (externalListener != null) {
            mainHandler.post(() -> externalListener.onError(error));
        }
    }

    /**
     * 释放
     */
    public void release() {
        backgroundExecutor.shutdownNow();
        if (gestureRecognizerHelper != null) {
            gestureRecognizerHelper.release();
        }
    }

    /**
     * 外部业务监听接口
     */
    public interface OnGestureRecognizedListener {
        /**
         * 识别结果回调
         * <p>
         * 已自动切换至主线程
         *
         * @param gestureName             手势名称
         * @param gestureRecognizerResult 手势识别结果
         *                                如 "Victory", "Open_Palm", "Closed_Fist", "Thumb_Up", "None"
         * @param inferenceTimeMs         推理耗时毫秒
         */
        void onGestureRecognized(String gestureName, GestureRecognizerResult gestureRecognizerResult, long inferenceTimeMs);

        /**
         * 错误回调
         */
        void onError(String error);
    }
}