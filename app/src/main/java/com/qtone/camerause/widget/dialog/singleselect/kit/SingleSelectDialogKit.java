package com.qtone.camerause.widget.dialog.singleselect.kit;

import androidx.appcompat.app.AppCompatActivity;

import com.qtone.camerause.widget.dialog.listener.DialogClickListener;
import com.qtone.camerause.widget.dialog.singleselect.SingleSelectDialog;
import com.qtone.camerause.widget.dialog.singleselect.SingleSelectDialogBean;

import java.util.List;

/**
 * Created on 2026/8/25.
 *
 * @author 郑少鹏
 * @desc 单选对话框配套原件
 */
public class SingleSelectDialogKit {
    /**
     * 显示单选对话框
     *
     * @param appCompatActivity          活动
     * @param title                      标题
     * @param singleSelectDialogBeanList 单选对话框数据集
     * @param defaultSelectedPosition    默认选中位置
     * @param positiveText               积极文本
     * @param dialogClickListener        对话框点击监听
     */
    public static void showSingleSelectDialog(AppCompatActivity appCompatActivity, String title, List<SingleSelectDialogBean> singleSelectDialogBeanList, int defaultSelectedPosition, String positiveText, DialogClickListener dialogClickListener) {
        if ((appCompatActivity == null) || appCompatActivity.isFinishing() || appCompatActivity.isDestroyed()) {
            return;
        }
        SingleSelectDialog singleSelectDialog = new SingleSelectDialog(appCompatActivity);
        singleSelectDialog.setTitle(title)
                .setSingleSelectDialogBeanList(singleSelectDialogBeanList)
                .setDefaultSelectedPosition(defaultSelectedPosition)
                .setPositiveText(positiveText)
                .setDialogClickListener(dialogClickListener)
                .show();
    }
}