package com.qtone.camerause.widget.permissionx.kit;

import androidx.fragment.app.FragmentActivity;

import com.permissionx.guolindev.PermissionX;
import com.qtone.camerause.widget.permissionx.dialog.PermissionDialog;
import com.qtone.camerause.widget.permissionx.listener.PermissionxKitListener;

import java.util.List;

/**
 * Created on 2025/9/1.
 *
 * @author 郑少鹏
 * @desc PermissionX 配套原件
 */
public class PermissionxKit {
    /**
     * 执行
     *
     * @param fragmentActivity                    FragmentActivity
     * @param custom                              自定义否
     * @param permissionList                      权限集
     * @param requestReasonDialogMessageResId     请求原因对话框内容资源 ID
     * @param forwardToSettingsDialogMessageResId 前往设置对话框内容资源 ID
     * @param dialogPositiveTextResId             对话框积极文本资源 ID
     * @param dialogNegativeTextResId             对话框消极文本资源 ID
     * @param permissionxKitListener              PermissionX 配套原件监听
     */
    public static void execute(FragmentActivity fragmentActivity, boolean custom, List<String> permissionList, int requestReasonDialogMessageResId, int forwardToSettingsDialogMessageResId, int dialogPositiveTextResId, int dialogNegativeTextResId, PermissionxKitListener permissionxKitListener) {
        PermissionX.init(fragmentActivity).permissions(permissionList).explainReasonBeforeRequest().onExplainRequestReason((scope, deniedList) -> {
            if (custom) {
                PermissionDialog permissionDialog = new PermissionDialog(fragmentActivity, deniedList, fragmentActivity.getString(requestReasonDialogMessageResId), fragmentActivity.getString(dialogPositiveTextResId), fragmentActivity.getString(dialogNegativeTextResId));
                scope.showRequestReasonDialog(permissionDialog);
                return;
            }
            scope.showRequestReasonDialog(deniedList, fragmentActivity.getString(requestReasonDialogMessageResId), fragmentActivity.getString(dialogPositiveTextResId), fragmentActivity.getString(dialogNegativeTextResId));
        }).onForwardToSettings((scope, deniedList) -> {
            if (custom) {
                PermissionDialog permissionDialog = new PermissionDialog(fragmentActivity, deniedList, fragmentActivity.getString(requestReasonDialogMessageResId), fragmentActivity.getString(dialogPositiveTextResId), fragmentActivity.getString(dialogNegativeTextResId));
                scope.showForwardToSettingsDialog(permissionDialog);
                return;
            }
            scope.showForwardToSettingsDialog(deniedList, fragmentActivity.getString(forwardToSettingsDialogMessageResId), fragmentActivity.getString(dialogPositiveTextResId), fragmentActivity.getString(dialogNegativeTextResId));
        }).request((allGranted, grantedList, deniedList) -> {
            if (allGranted) {
                permissionxKitListener.allGranted();
            } else {
                permissionxKitListener.allGrantedContrary();
            }
        });
    }
}