package com.qtone.camerause.widget.dialog.countdown.kit;

import androidx.appcompat.app.AppCompatActivity;

import com.qtone.camerause.widget.dialog.countdown.CountdownDialog;
import com.qtone.camerause.widget.dialog.countdown.listener.CountdownDialogListener;

/**
 * Created on 2026/9/7.
 *
 * @author 郑少鹏
 * @desc 倒计时对话框配套原件
 */
public class CountdownDialogKit {
    /**
     * 是否已触发
     * <p>
     * 使用 volatile 保证多线程读写可见性
     */
    private volatile boolean isTriggered = false;

    /**
     * constructor
     * <p>
     * 私有构造函数 + 防止实例化
     */
    private CountdownDialogKit() {

    }

    /**
     * 获取实例
     *
     * @return 倒计时对话框配套原件
     */
    public static CountdownDialogKit getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * 显示倒计时对话框
     *
     * @param appCompatActivity 活动
     * @param totalSeconds      总秒数
     * @param runnable          任务
     */
    public void showCountdownDialog(AppCompatActivity appCompatActivity, int totalSeconds, Runnable runnable) {
        showCountdownDialog(appCompatActivity, totalSeconds, null, runnable);
    }

    /**
     * 显示倒计时对话框
     *
     * @param appCompatActivity 活动
     * @param totalSeconds      总秒数
     * @param onTickRunnable    滴答任务
     * @param onFinishRunnable  完成任务
     */
    public void showCountdownDialog(AppCompatActivity appCompatActivity, int totalSeconds, Runnable onTickRunnable, Runnable onFinishRunnable) {
        if ((appCompatActivity == null) || appCompatActivity.isFinishing() || appCompatActivity.isDestroyed() || isTriggered) {
            return;
        }
        // 标记已触发
        isTriggered = true;
        CountdownDialog countdownDialog = new CountdownDialog(appCompatActivity);
        countdownDialog.setCancelable(false);
        countdownDialog.setTotalSeconds(totalSeconds)
                .setCountdownDialogListener(new CountdownDialogListener() {
                    @Override
                    public void onTick(CountdownDialog countdownDialog, int remainingSeconds) {
                        if (onTickRunnable != null) {
                            onTickRunnable.run();
                        }
                    }

                    @Override
                    public void onFinish(CountdownDialog countdownDialog) {
                        if (onFinishRunnable != null) {
                            onFinishRunnable.run();
                        }
                    }
                }).show();
    }

    /**
     * 是否已触发
     *
     * @return 是否已触发
     */
    public boolean isTriggered() {
        return isTriggered;
    }

    /**
     * 设置是否已触发
     *
     * @param isTriggered 是否已触发
     */
    public void setTriggered(boolean isTriggered) {
        this.isTriggered = isTriggered;
    }

    /**
     * 重置触发
     */
    public void resetTrigger() {
        this.isTriggered = false;
    }

    private static class InstanceHolder {
        private static final CountdownDialogKit INSTANCE = new CountdownDialogKit();
    }
}