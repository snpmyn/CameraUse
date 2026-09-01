package com.qtone.camerause.widget.button;

/**
 * Created on 2026/9/1.
 *
 * @author 郑少鹏
 * @desc 拍照按钮状态
 */
public enum CaptureButtonState {
    /**
     * 单拍空闲
     */
    IDLE_SINGLE_CAPTURE,
    /**
     * 连拍空闲
     */
    IDLE_BURST_CAPTURE,
    /**
     * 单拍进行中
     */
    SINGLE_CAPTURE_RUNNING,
    /**
     * 连拍进行中
     */
    BURST_CAPTURE_RUNNING,
    /**
     * 停止中
     */
    STOPPING
}