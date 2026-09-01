package com.qtone.camerause.widget.button;

/**
 * Created on 2026/9/1.
 *
 * @author 郑少鹏
 * @desc 拍照按钮回调
 */
public interface OnCaptureButtonCallback {
    /**
     * 拍照按钮预检查
     *
     * @return 是否允许
     */
    default boolean onCaptureButtonPreCheck() {
        return true;
    }

    /**
     * 拍照按钮开始
     *
     * @param currentCaptureButtonState 当前拍照按钮状态
     */
    void onCaptureButtonStart(CaptureButtonState currentCaptureButtonState);

    /**
     * 拍照按钮蓄力中途取消
     */
    void onCaptureButtonChargeCancel();

    /**
     * 拍照按钮停止
     */
    void onCaptureButtonStop();
}