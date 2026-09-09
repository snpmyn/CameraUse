package com.qtone.camerause.widget.mediapipe.gesture;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult;
import com.qtone.camerause.util.log.LogKit;
import com.qtone.camerause.widget.camera.YuvToBitmapKit;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created on 2026/9/4.
 *
 * @author 郑少鹏
 * @desc 手势识别管理器
 */
public class GestureRecognizerManager implements GestureRecognizerCallback {
    /**
     * 防抖滑动窗口大小
     */
    private static final int WINDOW_SIZE = 3;
    /**
     * 线程消息调度器
     */
    private final Handler handler;
    /**
     * 增强实现
     */
    private final ExecutorService executorService;
    /**
     * 手势识别辅助者
     */
    private final GestureRecognizerHelper gestureRecognizerHelper;
    /**
     * 手势识别回调
     */
    private final GestureRecognizerCallback gestureRecognizerCallback;
    /**
     * 手势防抖队列
     */
    private final Queue<String> gestureWindow = new ArrayDeque<>();
    /**
     * 标志当前是否有帧正在处理中
     * <p>
     * 用于控频丢帧
     */
    private final AtomicBoolean isProcessingFrame = new AtomicBoolean(false);

    /**
     * constructor
     *
     * @param context                   上下文
     * @param gestureRecognizerCallback 手势识别回调
     */
    public GestureRecognizerManager(Context context, GestureRecognizerCallback gestureRecognizerCallback) {
        // 线程消息调度器
        this.handler = new Handler(Looper.getMainLooper());
        // 增强实现
        // 单线程池 + 专门处理图像转换与 AI 帧识别
        this.executorService = Executors.newSingleThreadExecutor();
        // 手势识别辅助者
        this.gestureRecognizerHelper = new GestureRecognizerHelper(context, this);
        // 手势识别回调
        this.gestureRecognizerCallback = gestureRecognizerCallback;
    }

    /**
     * 处理预览帧
     *
     * @param data   图像帧字节数组
     * @param width  帧物理宽
     * @param height 帧物理高
     */
    public void processPreviewFrame(byte[] data, int width, int height) {
        if ((data == null) || (gestureRecognizerHelper == null) || (gestureRecognizerHelper.gestureRecognizerIsClosed())) {
            return;
        }
        // 控频
        // 如果上一帧还在转换 / 识别中，直接丢弃当前帧，保障实时性与内存稳定。
        if (!isProcessingFrame.compareAndSet(false, true)) {
            return;
        }
        executorService.execute(() -> {
            try {
                // 1. 将 YUV 格式转换成 Bitmap
                Bitmap frameBitmap = YuvToBitmapKit.nv21ToBitmap(data, width, height);
                if ((frameBitmap != null) && !gestureRecognizerHelper.gestureRecognizerIsClosed()) {
                    // 2. 送入识别器开始识别
                    gestureRecognizerHelper.recognizeLiveStream(frameBitmap);
                } else {
                    isProcessingFrame.set(false);
                }
            } catch (Exception e) {
                Log.e(LogKit.TAG, "手势识别 - 处理帧数据异常", e);
                isProcessingFrame.set(false);
            }
        });
    }

    /**
     * 手势滤波防抖
     *
     * @param rawGesture 单帧手势
     * @return 滤波后的稳定手势
     */
    private String filterGesture(String rawGesture) {
        if (gestureWindow.size() >= WINDOW_SIZE) {
            gestureWindow.poll();
        }
        gestureWindow.add(rawGesture);
        Map<String, Integer> counts = new HashMap<>();
        for (String s : gestureWindow) {
            counts.merge(s, 1, Integer::sum);
        }
        String maxGesture = "None";
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                maxGesture = entry.getKey();
            }
        }
        return (maxCount > WINDOW_SIZE / 2) ? maxGesture : "None";
    }

    /**
     * 释放
     */
    public void release() {
        executorService.shutdownNow();
        if (gestureRecognizerHelper != null) {
            gestureRecognizerHelper.release();
        }
    }

    /**
     * 手势识别结果
     *
     * @param gestureRecognizerResult 手势识别结果
     * @param topGestureName          最高置信度的手势名称
     *                                如 "Victory", "Open_Palm", "None"
     * @param inferenceTimeMs         推理耗时毫秒
     */
    @Override
    public void onGestureRecognizerResult(GestureRecognizerResult gestureRecognizerResult, String topGestureName, long inferenceTimeMs) {
        // 完成处理，释放标志位，允许处理下一帧。
        isProcessingFrame.set(false);
        // 手势滤波防抖
        String smoothedGesture = filterGesture(topGestureName);
        // 切回主线程交由外部 UI / 业务处理
        if (gestureRecognizerCallback != null) {
            handler.post(() -> gestureRecognizerCallback.onGestureRecognizerResult(gestureRecognizerResult, smoothedGesture, inferenceTimeMs));
        }
    }

    /**
     * 手势识别错误
     *
     * @param errorMsg 错误信息
     */
    @Override
    public void onGestureRecognizerError(String errorMsg) {
        // 出错时同样重置标志位
        isProcessingFrame.set(false);
        if (gestureRecognizerCallback != null) {
            handler.post(() -> gestureRecognizerCallback.onGestureRecognizerError(errorMsg));
        }
    }
}