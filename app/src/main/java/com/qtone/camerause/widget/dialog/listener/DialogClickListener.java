package com.qtone.camerause.widget.dialog.listener;

import com.qtone.camerause.widget.dialog.base.BaseLifecycleDialog;

/**
 * Created on 2026/8/26.
 *
 * @author 郑少鹏
 * @desc 对话框点击监听
 */
public interface DialogClickListener {
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