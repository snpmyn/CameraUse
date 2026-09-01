package com.qtone.camerause.widget.capture;

/**
 * Created on 2026/8/31.
 *
 * @author 郑少鹏
 * @desc 拍照状态
 */
public enum CaptureState {
    /**
     * 空闲
     */
    IDLE,
    /**
     * 单拍进行中
     */
    SINGLE_CAPTURE_RUNNING,
    /**
     * 连拍进行中
     */
    BURST_CAPTURE_RUNNING
}