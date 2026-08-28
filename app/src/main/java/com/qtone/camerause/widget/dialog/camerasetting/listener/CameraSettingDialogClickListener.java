package com.qtone.camerause.widget.dialog.camerasetting.listener;

import com.qtone.camerause.widget.dialog.camerasetting.CameraSettingDialog;

/**
 * Created on 2026/8/26.
 *
 * @author 郑少鹏
 * @desc 相机设置对话框点击监听
 */
public interface CameraSettingDialogClickListener {
    /**
     * 取消
     *
     * @param cameraSettingDialog 相机设置对话框
     */
    default void onCancel(CameraSettingDialog cameraSettingDialog) {

    }

    /**
     * 确认
     *
     * @param cameraSettingDialog 相机设置对话框
     */
    default void onConfirm(CameraSettingDialog cameraSettingDialog) {

    }
}