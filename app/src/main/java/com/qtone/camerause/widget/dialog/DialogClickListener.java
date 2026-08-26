package com.qtone.camerause.widget.dialog;

/**
 * Created on 2026/8/26.
 *
 * @author 郑少鹏
 * @desc 对话框点击监听
 */
public interface DialogClickListener {
    /**
     * 取消
     */
    default void onCancel() {

    }

    /**
     * 确认
     */
    default void onConfirm() {

    }
}