package com.qtone.camerause.widget.permissionx.dialog;

import android.Manifest;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.permissionx.guolindev.dialog.RationaleDialog;
import com.qtone.camerause.R;
import com.qtone.camerause.util.layoutparams.LayoutParamsUtils;
import com.qtone.camerause.util.list.ListUtils;
import com.qtone.camerause.util.screen.ScreenUtils;
import com.qtone.camerause.util.window.WindowKit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created on 2025/9/28.
 *
 * @author 郑少鹏
 * @desc 权限对话框
 */
public class PermissionDialog extends RationaleDialog {
    /**
     * 权限名集
     */
    private static final Map<String, String> permissionNameMap;
    /**
     * 权限图集
     */
    private static final Map<String, Integer> permissionIconMap;

    static {
        // 权限名集
        permissionNameMap = new HashMap<>();
        // 权限图集
        permissionIconMap = new HashMap<>();
        // 存储
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissionNameMap.put(Manifest.permission.MANAGE_EXTERNAL_STORAGE, "存储");
        }
        permissionIconMap.put("存储", R.drawable.ic_folder_cos_24dp);
        // Manifest.permission_group.STORAGE
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionNameMap.put(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission_group.STORAGE);
            permissionNameMap.put(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission_group.STORAGE);
        }*/
        permissionNameMap.put(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission_group.STORAGE);
        permissionNameMap.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission_group.STORAGE);
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissionNameMap.put(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED, Manifest.permission_group.STORAGE);
        }*/
        permissionIconMap.put(Manifest.permission_group.STORAGE, R.drawable.ic_topic_cos_24dp);
        // Manifest.permission_group.LOCATION
        permissionNameMap.put(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission_group.LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionNameMap.put(Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission_group.LOCATION);
        }
        permissionIconMap.put(Manifest.permission_group.LOCATION, R.drawable.ic_globe_cos_24dp);
        // 通知
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionNameMap.put(PermissionX.permission.POST_NOTIFICATIONS, "通知");
        }*/
        permissionIconMap.put("通知", R.drawable.ic_notifications_cos_24dp);
        // Manifest.permission_group.SMS
        permissionNameMap.put(Manifest.permission.SEND_SMS, Manifest.permission_group.SMS);
        permissionIconMap.put(Manifest.permission_group.SMS, R.drawable.ic_inbox_text_cos_24dp);
        // 安装未知来源 APK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            permissionNameMap.put(Manifest.permission.REQUEST_INSTALL_PACKAGES, "安装未知来源 APK");
        }
        permissionIconMap.put("安装未知来源 APK", R.drawable.ic_folder_zip_cos_24dp);
        // Manifest.permission_group.MICROPHONE
        permissionNameMap.put(Manifest.permission.RECORD_AUDIO, Manifest.permission_group.MICROPHONE);
        permissionIconMap.put(Manifest.permission_group.MICROPHONE, R.drawable.ic_music_video_cos_24dp);
        // Manifest.permission_group.CAMERA
        permissionNameMap.put(Manifest.permission.CAMERA, Manifest.permission_group.CAMERA);
        permissionIconMap.put(Manifest.permission_group.CAMERA, R.drawable.ic_photo_camera_cos_24dp);
    }

    /**
     * 拒绝集
     */
    private final List<String> deniedList;
    /**
     * 内容
     */
    private final String message;
    /**
     * 消极文本
     */
    private final String negativeText;
    /**
     * 积极文本
     */
    private final String positiveText;
    /**
     * 权限组集
     */
    private final Set<String> permissionGroupSet = new HashSet<>();
    /**
     * 消极材料按钮
     */
    private MaterialButton permissionDialogMbNegative;
    /**
     * 积极材料按钮
     */
    private MaterialButton permissionDialogMbPositive;

    public PermissionDialog(@NonNull Context context, List<String> deniedList, String message, String positiveText, String negativeText) {
        super(context);
        this.deniedList = deniedList;
        this.message = message;
        this.positiveText = positiveText;
        this.negativeText = negativeText;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        // 设置背景位图资源透明
        WindowKit.Companion.setBackgroundDrawableResourceTransparent(window);
        setContentView(R.layout.dialog_permission);
        // 设置窗口管理器布局参数
        LayoutParamsUtils.setWindowManagerLayoutParams(window, true, (int) (ScreenUtils.screenWidth(getContext()) * 0.4), false, 0);
        // 设置
        set();
    }

    /**
     * 设置
     */
    private void set() {
        // 内容
        TextView permissionDialogTvMessage = findViewById(R.id.permissionDialogTvMessage);
        permissionDialogTvMessage.setText(message);
        // 构建权限布局
        buildPermissionLayout();
        // 消极材料按钮
        permissionDialogMbNegative = findViewById(R.id.permissionDialogMbNegative);
        permissionDialogMbNegative.setText(negativeText);
        // 积极材料按钮
        permissionDialogMbPositive = findViewById(R.id.permissionDialogMbPositive);
        permissionDialogMbPositive.setText(positiveText);
    }

    /**
     * 构建权限布局
     */
    private void buildPermissionLayout() {
        LinearLayout permissionDialogLlPermission = findViewById(R.id.permissionDialogLlPermission);
        if ((null == permissionDialogLlPermission) || ListUtils.listIsEmpty(deniedList)) {
            return;
        }
        // 移除所有视图
        permissionDialogLlPermission.removeAllViews();
        // 下标
        // 头条
        int index = 0;
        // 循环拒绝集
        for (String permissionName : deniedList) {
            // 通过权限名获取权限组
            String permissionGroup = permissionNameMap.get(permissionName);
            if (!TextUtils.isEmpty(permissionGroup) && !permissionGroupSet.contains(permissionGroup)) {
                // 权限组非空且不在权限组集中
                TextView textView = (TextView) getLayoutInflater().inflate(R.layout.dialog_permission_item, permissionDialogLlPermission, false);
                try {
                    String label = getContext().getPackageManager().getPermissionGroupInfo(permissionGroup, 0).loadLabel(getContext().getPackageManager()).toString();
                    textView.setText(label);
                } catch (Exception e) {
                    textView.setText(permissionGroup);
                }
                Integer icon = permissionIconMap.get(permissionGroup);
                if (null != icon) {
                    Drawable drawable = ContextCompat.getDrawable(getContext(), icon);
                    int width = (int) (18 * getContext().getResources().getDisplayMetrics().density);
                    int height = (int) (18 * getContext().getResources().getDisplayMetrics().density);
                    assert drawable != null;
                    drawable.setBounds(0, 0, width, height);
                    textView.setCompoundDrawables(drawable, null, null, null);
                }
                // 设上边距
                // 仅非头条
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) textView.getLayoutParams();
                if (index > 0) {
                    layoutParams.topMargin = (int) (8 * getContext().getResources().getDisplayMetrics().density);
                }
                textView.setLayoutParams(layoutParams);
                // 添加视图
                permissionDialogLlPermission.addView(textView);
                // 处理后保存在权限组集中
                // 避免重复处理
                permissionGroupSet.add(permissionGroup);
                // 下一条
                index++;
            }
        }
    }

    /**
     * Return the instance of positive button on the dialog. Your dialog must have a positive button to proceed request.
     *
     * @return The instance of positive button on the dialog.
     */
    @NonNull
    @Override
    public View getPositiveButton() {
        return permissionDialogMbPositive;
    }

    /**
     * Return the instance of negative button on the dialog.
     * If the permissions that you request are mandatory, your dialog can have no negative button.
     * In this case, you can simply return null.
     *
     * @return The instance of positive button on the dialog, or null if your dialog has no negative button.
     */
    @Nullable
    @Override
    public View getNegativeButton() {
        return permissionDialogMbNegative;
    }

    /**
     * Provide permissions to request. These permissions should be the ones that shows on your rationale dialog.
     *
     * @return Permissions list to request.
     */
    @NonNull
    @Override
    public List<String> getPermissionsToRequest() {
        return deniedList;
    }
}