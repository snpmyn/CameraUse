package com.qtone.camerause.widget.dialog.singleselect.listener;

import com.qtone.camerause.widget.dialog.base.BaseLifecycleDialog;

/**
 * Created on 2026/8/27.
 *
 * @author 郑少鹏
 * @desc 单选对话框点击监听
 */
public interface SingleSelectDialogClickListener {
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
     * @param position            位置
     * @param t                   T
     * @param <T>                 <T>
     */
    default <T> void onConfirm(BaseLifecycleDialog baseLifecycleDialog, int position, T t) {

    }
}