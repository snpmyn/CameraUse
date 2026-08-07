package com.qtone.camerause;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.jiangdg.ausbc.utils.ToastUtils;
import com.qtone.camerause.capture.CaptureActivity;
import com.qtone.camerause.scancode.ScanCodeActivity;

/**
 * @decs: 主页
 * @author: 郑少鹏
 * @date: 2026/7/28 16:14
 * @version: v 1.0
 */
public class MainActivity extends AppCompatActivity {
    /**
     * 请求相机权限码
     */
    private static final int REQUEST_CAMERA_PERMISSION_CODE = 100;
    /**
     * 请求管理外部存储权限码
     */
    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE_PERMISSION_CODE = 102;
    /**
     * 动作标识
     */
    private static final int ACTION_NONE = 0;
    private static final int ACTION_GO_TO_CAPTURE_ACTIVITY = 1;
    private static final int ACTION_GO_TO_SCAN_CODE_ACTIVITY = 2;
    /**
     * 当前待办动作标识
     */
    private int currentPendingAction = ACTION_NONE;
    /**
     * 控件
     */
    private MaterialButton mainActivityMbCapture;
    private MaterialButton mainActivityMbScanCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 初始化视图
        initView();
        // 初始化监听
        initListener();
        // 检查并请求权限
        checkAndRequestPermission(ACTION_NONE);
    }

    /**
     * 初始化视图
     */
    private void initView() {
        mainActivityMbCapture = findViewById(R.id.mainActivityMbCapture);
        mainActivityMbScanCode = findViewById(R.id.mainActivityMbScanCode);
    }

    /**
     * 初始化监听
     */
    private void initListener() {
        mainActivityMbCapture.setOnClickListener(v -> {
            // 1. 先检查权限
            if (hasAllPermissions()) {
                // 2. 有权限 -> 跳转拍照页
                navigateToCaptureActivity();
            } else {
                // 3. 无权限 -> 检查并请求权限
                checkAndRequestPermission(ACTION_GO_TO_CAPTURE_ACTIVITY);
            }
        });
        mainActivityMbScanCode.setOnClickListener(v -> {
            // 1. 先检查权限
            if (hasAllPermissions()) {
                // 2. 有权限 -> 跳转扫码页
                navigateToScanCodeActivity();
            } else {
                // 3. 无权限 -> 检查并请求权限
                checkAndRequestPermission(ACTION_GO_TO_SCAN_CODE_ACTIVITY);
            }
        });
    }

    /**
     * 是否拥有全部权限
     *
     * @return 是否拥有全部权限
     */
    private boolean hasAllPermissions() {
        // 1. 检查相机权限
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if (!hasCameraPermission) {
            return false;
        }
        // 2. 检查存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            // 检查所有文件管理权限
            return Environment.isExternalStorageManager();
        } else {
            // Android 10-
            // 检查普通读写权限
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * 检查并请求权限
     *
     * @param targetAction 目标动作标识
     */
    private void checkAndRequestPermission(int targetAction) {
        this.currentPendingAction = targetAction;
        // 1. 检查常规运行时权限
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean hasStoragePermission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                || (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasCameraPermission || !hasStoragePermission) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CAMERA_PERMISSION_CODE
            );
            return;
        }
        // 2. 检查所有文件管理权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE_PERMISSION_CODE);
                return;
            }
        }
        // 3. 执行待办动作
        executePendingAction();
    }

    /**
     * 导航至拍照页
     */
    private void navigateToCaptureActivity() {
        startActivity(new Intent(this, CaptureActivity.class));
    }

    /**
     * 导航至扫码页
     */
    private void navigateToScanCodeActivity() {
        startActivity(new Intent(this, ScanCodeActivity.class));
    }

    /**
     * 执行待办动作标识
     */
    private void executePendingAction() {
        switch (currentPendingAction) {
            case ACTION_GO_TO_CAPTURE_ACTIVITY:
                navigateToCaptureActivity();
                break;
            case ACTION_GO_TO_SCAN_CODE_ACTIVITY:
                navigateToScanCodeActivity();
                break;
            case ACTION_NONE:
            default:
                break;
        }
        currentPendingAction = ACTION_NONE;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_CODE) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // 常规运行时权限通过
                // 继续检查所有文件管理权限
                checkAndRequestPermission(currentPendingAction);
            } else {
                // 权限申请被拒
                currentPendingAction = ACTION_NONE;
                ToastUtils.show("需要相机权限才能使用 USB 相机");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANAGE_EXTERNAL_STORAGE_PERMISSION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // 用户在系统设置页授权成功
                    // 执行待办动作
                    executePendingAction();
                } else {
                    // 用户未授权
                    currentPendingAction = ACTION_NONE;
                    ToastUtils.show("需要所有文件管理权限才能存储文件");
                }
            }
        }
    }
}