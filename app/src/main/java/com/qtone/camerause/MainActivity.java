package com.qtone.camerause;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
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
            if (hasCameraPermission()) {
                // 2. 有权限
                // 跳转拍照页
                navigateToCaptureActivity();
            } else {
                // 3. 无权限
                // 检查并请求权限
                checkAndRequestPermission(ACTION_GO_TO_CAPTURE_ACTIVITY);
            }
        });
        mainActivityMbScanCode.setOnClickListener(v -> {
            // 1. 先检查权限
            if (hasCameraPermission()) {
                // 2. 有权限
                // 跳转扫码页
                navigateToScanCodeActivity();
            } else {
                // 3. 无权限
                //检查并请求权限
                checkAndRequestPermission(ACTION_GO_TO_SCAN_CODE_ACTIVITY);
            }
        });
    }

    /**
     * 是否拥有相机权限
     *
     * @return 是否拥有相机权限
     */
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 检查并请求权限
     *
     * @param targetAction 目标动作标识
     */
    private void checkAndRequestPermission(int targetAction) {
        this.currentPendingAction = targetAction;
        if (hasCameraPermission()) {
            // 执行待办动作标识
            executePendingAction();

        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CAMERA_PERMISSION_CODE
            );
        }
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
                // 1. 权限申请成功
                // 执行待办动作标识
                executePendingAction();
            } else {
                // 2. 权限申请拒绝
                currentPendingAction = ACTION_NONE;
                ToastUtils.show("需要相机权限才能使用 USB 相机");
            }
        }
    }
}