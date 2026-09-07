package com.qtone.camerause.widget.gesture;

import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult;

/**
 * Created on 2026/9/7.
 *
 * @author 郑少鹏
 * @desc 手势识别回调
 */
public interface GestureRecognizerCallback {
    /**
     * 手势识别结果
     *
     * @param gestureRecognizerResult 手势识别结果
     * @param topGestureName          最高置信度的手势名称
     *                                如 "Victory", "Open_Palm", "None"
     * @param inferenceTimeMs         推理耗时毫秒
     */
    void onGestureRecognizerResult(GestureRecognizerResult gestureRecognizerResult, String topGestureName, long inferenceTimeMs);

    /**
     * 手势识别错误
     *
     * @param errorMsg 错误信息
     */
    void onGestureRecognizerError(String errorMsg);
}