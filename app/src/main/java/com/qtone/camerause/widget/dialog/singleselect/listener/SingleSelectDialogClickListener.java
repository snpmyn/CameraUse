package com.qtone.camerause.widget.dialog.singleselect.listener;

import com.qtone.camerause.widget.dialog.singleselect.SingleSelectDialog;

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
     * @param singleSelectDialog 单选对话框
     */
    default void onCancel(SingleSelectDialog singleSelectDialog) {

    }

    /**
     * 确认
     *
     * @param singleSelectDialog 单选对话框
     * @param position           位置
     * @param t                  T
     * @param <T>                <T>
     */
    default <T> void onConfirm(SingleSelectDialog singleSelectDialog, int position, T t) {

    }
}