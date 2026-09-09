package com.qtone.camerause.widget.dialog.countdown;

import android.content.Context;
import android.os.CountDownTimer;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.qtone.camerause.R;
import com.qtone.camerause.widget.dialog.base.BaseLifecycleDialog;
import com.qtone.camerause.widget.dialog.countdown.listener.CountdownDialogListener;

/**
 * Created on 2026/9/7.
 *
 * @author 郑少鹏
 * @desc 倒计时对话框
 */
public class CountdownDialog extends BaseLifecycleDialog {
    /**
     * 控件
     */
    private TextView countdownDialogTv;
    /**
     * 总秒数
     */
    private int totalSeconds = 3;
    /**
     * 倒计时定时器
     */
    private CountDownTimer countDownTimer;
    /**
     * 倒计时对话框监听
     */
    private CountdownDialogListener countdownDialogListener;

    /**
     * constructor
     *
     * @param context 上下文
     */
    public CountdownDialog(@NonNull Context context) {
        super(context);
    }

    /**
     * 获取布局 ID
     *
     * @return 布局 ID
     */
    @Override
    protected int getLayoutId() {
        return R.layout.dialog_countdown;
    }

    /**
     * 初始化控件
     */
    @Override
    protected void initView() {
        countdownDialogTv = findViewById(R.id.countdownDialogTv);
    }

    /**
     * 初始化数据
     */
    @Override
    protected void initData() {
        countdownDialogTv.setText(String.valueOf(totalSeconds));
    }

    /**
     * 初始化事件
     */
    @Override
    protected void initEvent() {

    }

    /**
     * 开始逻辑
     */
    @Override
    protected void startLogic() {
        startCountdown();
    }

    /**
     * 启动倒计时
     */
    private void startCountdown() {
        cancelCountdown();
        countDownTimer = new CountDownTimer(totalSeconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) Math.ceil(millisUntilFinished / 1000.0f);
                countdownDialogTv.setText(String.valueOf(seconds));
                if (countdownDialogListener != null) {
                    countdownDialogListener.onTick(CountdownDialog.this, seconds);
                }
            }

            @Override
            public void onFinish() {
                countdownDialogTv.setText("0");
                if (countdownDialogListener != null) {
                    countdownDialogListener.onFinish(CountdownDialog.this);
                }
                dismiss();
            }
        }.start();
    }

    /**
     * 取消倒计时
     */
    private void cancelCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    /**
     * 设置总秒数
     *
     * @param totalSeconds 总秒数
     * @return 倒计时对话框
     */
    public CountdownDialog setTotalSeconds(int totalSeconds) {
        this.totalSeconds = totalSeconds;
        return this;
    }

    /**
     * 设置倒计时对话框监听
     *
     * @param countdownDialogListener 倒计时对话框监听
     * @return 倒计时对话框
     */
    public CountdownDialog setCountdownDialogListener(CountdownDialogListener countdownDialogListener) {
        this.countdownDialogListener = countdownDialogListener;
        return this;
    }

    /**
     * 清理资源
     * <p>
     * 对话框从 Window 移除触发
     */
    @Override
    protected void onClearResource() {
        super.onClearResource();
        // 取消倒计时
        cancelCountdown();
        // 置空倒计时对话框监听
        countdownDialogListener = null;
    }
}