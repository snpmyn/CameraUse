package com.qtone.camerause.widget.dialog.countdown.listener;

import com.qtone.camerause.widget.dialog.countdown.CountdownDialog;

/**
 * Created on 2026/9/7.
 *
 * @author 郑少鹏
 * @desc 倒计时对话框监听
 */
public interface CountdownDialogListener {
    /**
     * 滴答
     *
     * @param countdownDialog  倒计时对话框
     * @param remainingSeconds 剩余秒数
     */
    default void onTick(CountdownDialog countdownDialog, int remainingSeconds) {

    }

    /**
     * 完成
     *
     * @param countdownDialog 倒计时对话框
     */
    void onFinish(CountdownDialog countdownDialog);
}