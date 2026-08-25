package com.qtone.camerause.widget.dialog.kit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.qtone.camerause.R;
import com.qtone.camerause.widget.dialog.CommonDialog;

/**
 * Created on 2026/8/25.
 *
 * @author 郑少鹏
 * @desc 普通对话框配套原件
 */
public class CommonDialogKit {
    /**
     * 显示信息对话框
     *
     * @param appCompatActivity 活动
     * @param content           上下文
     * @param runnable          可运行的
     */
    public static void showInfoDialog(AppCompatActivity appCompatActivity, String content, Runnable runnable) {
        showInfoDialog(appCompatActivity, appCompatActivity.getString(R.string.notice), content, appCompatActivity.getString(R.string.iKonw), runnable);
    }

    /**
     * 显示信息对话框
     *
     * @param appCompatActivity 活动
     * @param title             标题
     * @param content           内容
     * @param positiveText      积极文本
     * @param runnable          可运行的
     */
    public static void showInfoDialog(AppCompatActivity appCompatActivity, String title, String content, String positiveText, Runnable runnable) {
        // 1. 校验
        if ((appCompatActivity == null) || appCompatActivity.isFinishing() || appCompatActivity.isDestroyed()) {
            return;
        }
        CommonDialog commonDialog = new CommonDialog(appCompatActivity);
        commonDialog.setTitle(title)
                .setContent(content)
                .setPositiveText(positiveText)
                .setOnDialogClickListener(new CommonDialog.OnDialogClickListener() {
                    @Override
                    public void onConfirm() {
                        commonDialog.dismiss();
                        if (runnable != null) {
                            runnable.run();
                        }
                    }
                });
        commonDialog.setCancelable(false);
        // 2. 绑定生命周期
        appCompatActivity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                // AppCompatActivity 销毁时自动 dismiss
                // 防止 WindowLeaked 和内存泄漏
                if (commonDialog.isShowing()) {
                    commonDialog.dismiss();
                }
                // 移除监听
                owner.getLifecycle().removeObserver(this);
            }
        });
        // 3. 显示
        commonDialog.show();
    }
}