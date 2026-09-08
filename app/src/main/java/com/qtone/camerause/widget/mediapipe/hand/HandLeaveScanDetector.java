package com.qtone.camerause.widget.mediapipe.hand;

import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult;

/**
 * Created on 2026/9/7.
 *
 * @author 郑少鹏
 * @desc 手部离开扫描检测器
 */
public class HandLeaveScanDetector {
    /**
     * 防抖阈值
     * <p>
     * 连续多少帧没检测到手 -> 才正式认定 “手已完全拿走”
     * <p>
     * 假设预览帧率约 15 - 20 fps
     * 那么 5 帧大约为 250 - 300ms
     */
    private static final int TRIGGER_NO_HAND_FRAMES = 5;
    /**
     * 手离开扫描回调
     */
    private final OnHandLeaveScanCallback onHandLeaveScanCallback;
    /**
     * 无手帧数计数器
     */
    private int noHandFrameCount = 0;
    /**
     * 上帧手是否存在
     */
    private boolean isHandPresentLastState = false;

    /**
     * constructor
     *
     * @param onHandLeaveScanCallback 手离开扫描回调
     */
    public HandLeaveScanDetector(OnHandLeaveScanCallback onHandLeaveScanCallback) {
        this.onHandLeaveScanCallback = onHandLeaveScanCallback;
    }

    /**
     * 设置上帧手是否存在
     *
     * @param isHandPresentLastState 上帧手是否存在
     */
    private void setHandPresentLastState(boolean isHandPresentLastState) {
        this.isHandPresentLastState = isHandPresentLastState;
    }

    /**
     * 帧中是否有手
     *
     * @param gestureRecognizerResult 手势识别结果
     * @return 帧中是否有手
     */
    private boolean isHandInFrame(GestureRecognizerResult gestureRecognizerResult) {
        return (gestureRecognizerResult != null)
                && (gestureRecognizerResult.landmarks() != null)
                && (!gestureRecognizerResult.landmarks().isEmpty());
    }

    /**
     * 处理手势识别结果
     *
     * @param gestureRecognizerResult 手势识别结果
     */
    public void processGestureRecognizerResult(GestureRecognizerResult gestureRecognizerResult) {
        // 判断当前帧是否存在手
        boolean hasHandInCurrentFrame = isHandInFrame(gestureRecognizerResult);
        if (hasHandInCurrentFrame) {
            // 1. 当前帧有手
            // 重置无手计数器
            noHandFrameCount = 0;
            if (!isHandPresentLastState) {
                setHandPresentLastState(true);
                notifyHandDetected();
            }
        } else {
            // 2. 当前帧无手
            if (isHandPresentLastState) {
                noHandFrameCount++;
                // 只有连续 N 帧都未检测到手，才确认手已移开，防止单帧误判。
                if (noHandFrameCount >= TRIGGER_NO_HAND_FRAMES) {
                    setHandPresentLastState(false);
                    noHandFrameCount = 0;
                    notifyHandRemoved();
                }
            }
        }
    }

    /**
     * 重置
     */
    public void reset() {
        noHandFrameCount = 0;
        setHandPresentLastState(false);
    }

    /**
     * 通知手被检测到
     */
    private void notifyHandDetected() {
        if (onHandLeaveScanCallback != null) {
            onHandLeaveScanCallback.onHandDetected();
        }
    }

    /**
     * 通知手完全拿走
     */
    private void notifyHandRemoved() {
        if (onHandLeaveScanCallback != null) {
            onHandLeaveScanCallback.onHandRemoved();
        }
    }

    /**
     * 手离开扫描回调
     */
    public interface OnHandLeaveScanCallback {
        /**
         * 手被检测到
         */
        void onHandDetected();

        /**
         * 手完全拿走
         */
        void onHandRemoved();
    }
}