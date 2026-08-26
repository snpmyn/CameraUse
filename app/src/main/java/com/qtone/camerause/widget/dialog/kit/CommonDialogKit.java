package com.qtone.camerause.widget.dialog.kit;

import androidx.appcompat.app.AppCompatActivity;

import com.qtone.camerause.R;
import com.qtone.camerause.widget.dialog.CommonDialog;
import com.qtone.camerause.widget.dialog.DialogClickListener;

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
     * @param appCompatActivity     活动
     * @param content               内容
     * @param centerLongestLeftRest 最长行居中其余行靠左
     * @param runnable              可运行的
     */
    public static void showInfoDialog(AppCompatActivity appCompatActivity, String content, boolean centerLongestLeftRest, Runnable runnable) {
        showInfoDialog(appCompatActivity, appCompatActivity.getString(R.string.notice), content, centerLongestLeftRest, appCompatActivity.getString(R.string.iKonw), runnable);
    }

    /**
     * 显示信息对话框
     *
     * @param appCompatActivity     活动
     * @param title                 标题
     * @param content               内容
     * @param centerLongestLeftRest 最长行居中其余行靠左
     * @param positiveText          积极文本
     * @param runnable              可运行的
     */
    public static void showInfoDialog(AppCompatActivity appCompatActivity, String title, String content, boolean centerLongestLeftRest, String positiveText, Runnable runnable) {
        if ((appCompatActivity == null) || appCompatActivity.isFinishing() || appCompatActivity.isDestroyed()) {
            return;
        }
        CommonDialog commonDialog = new CommonDialog(appCompatActivity);
        commonDialog.setCancelable(false);
        commonDialog.setTitle(title)
                .setContent(content)
                .setCenterLongestLeftRest(centerLongestLeftRest)
                .setPositiveText(positiveText)
                .setDialogClickListener(new DialogClickListener() {
                    @Override
                    public void onConfirm() {
                        commonDialog.dismiss();
                        if (runnable != null) {
                            runnable.run();
                        }
                    }
                }).show();
    }
}