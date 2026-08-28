package com.qtone.camerause.widget.dialog.common.listener;

import com.qtone.camerause.widget.dialog.common.CommonDialog;

/**
 * Created on 2026/8/26.
 *
 * @author 郑少鹏
 * @desc 普通对话框点击监听
 */
public interface CommonDialogClickListener {
    /**
     * 取消
     *
     * @param commonDialog 普通对话框
     */
    default void onCancel(CommonDialog commonDialog) {

    }

    /**
     * 确认
     *
     * @param commonDialog 普通对话框
     */
    default void onConfirm(CommonDialog commonDialog) {

    }
}