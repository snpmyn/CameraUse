package com.qtone.camerause.widget.dialog.common.listener;

import com.qtone.camerause.widget.dialog.base.BaseLifecycleDialog;

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
     * @param baseLifecycleDialog 生命周期对话框基类
     */
    default void onCancel(BaseLifecycleDialog baseLifecycleDialog) {

    }

    /**
     * 确认
     *
     * @param baseLifecycleDialog 生命周期对话框基类
     */
    default void onConfirm(BaseLifecycleDialog baseLifecycleDialog) {

    }
}