package com.qtone.camerause;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.jiangdg.ausbc.utils.ToastUtils;

import java.io.File;

/**
 * @decs: 主页
 * @author: 郑少鹏
 * @date: 2026/7/28 16:14
 * @version: v 1.0
 */
public class MainActivity extends AppCompatActivity implements ExamCropProcessor.OnCropCallback, ExamHeaderProcessor.OnHeaderCropCallback {
    private static final String TAG = MainActivity.class.getSimpleName();
    /**
     * 请求相机权限码
     */
    private static final int REQUEST_CAMERA_PERMISSION_CODE = 100;
    /**
     * 控件
     */
    private MaterialButton mainActivityMbCapture;
    private MaterialButton mainActivityMbScanCode;
    /**
     * 当前碎片
     */
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 布局包含 ID 为 R.id.mainActivityFl 的 FrameLayout
        setContentView(R.layout.activity_main);
        // 初始化视图
        initView();
        // 初始化监听
        initListener();
        // 检查并请求权限
        checkAndRequestPermission();
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
            if (!hasCameraPermission()) {
                checkAndRequestPermission();
                return;
            }
            // 2. 有权限再进行相应操作
            if (currentFragment instanceof CaptureFragment) {
                // 当前已在拍照碎片 -> 直接拍照
                ((CaptureFragment) currentFragment).capture();
            } else {
                // 加载拍照碎片
                loadCaptureFragment();
            }
        });
        mainActivityMbScanCode.setOnClickListener(v -> {
            // 检查权限
            if (hasCameraPermission()) {
                // 切换至扫码碎片
                switchFragment(new ScanCodeFragment());
            } else {
                // 检查并请求权限
                checkAndRequestPermission();
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
     */
    private void checkAndRequestPermission() {
        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CAMERA_PERMISSION_CODE
            );
        } else {
            loadScanCodeFragment();
        }
    }

    /**
     * 加载拍照碎片
     */
    private void loadCaptureFragment() {
        CaptureFragment captureFragment = new CaptureFragment();
        captureFragment.setOnCropCallback(this);
        captureFragment.setOnHeaderCropCallback(this);
        switchFragment(captureFragment);
    }

    /**
     * 加载扫码碎片
     */
    private void loadScanCodeFragment() {
        switchFragment(new ScanCodeFragment());
    }

    /**
     * 切换碎片
     * <p>
     * 使用 replace 释放底层 USB 相机设备锁
     * 保证硬件顺利重新占用
     *
     * @param targetFragment 目标碎片
     */
    private void switchFragment(Fragment targetFragment) {
        if ((currentFragment != null) && (currentFragment.getClass().equals(targetFragment.getClass()))) {
            return;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainActivityFl, targetFragment)
                .commitAllowingStateLoss();
        currentFragment = targetFragment;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_CODE) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                loadScanCodeFragment();
            } else {
                ToastUtils.show("需要相机权限才能使用 USB 相机");
            }
        }
    }

    /**
     * 裁剪成功
     *
     * @param croppedPath  已拷贝路径
     * @param resultBitmap 结果像素数据
     */
    @Override
    public void onCropSuccess(String croppedPath, Bitmap resultBitmap) {

    }

    /**
     * 裁剪错误
     *
     * @param errorMsg 错误消息
     */
    @Override
    public void onCropError(String errorMsg) {

    }

    /**
     * 头部裁剪成功
     *
     * @param headerBitmap  头像素数据
     * @param qrCodeContent 二维码内容
     * @param cropFile      裁剪文件
     *                      异步保存的本地图片文件
     */
    @Override
    public void onHeaderCropSuccess(Bitmap headerBitmap, String qrCodeContent, File cropFile) {
        String cropFilePath = ((cropFile != null) ? cropFile.getAbsolutePath() : "null");
        Log.d(TAG, "成功" +
                "\n二维码内容 = " + qrCodeContent +
                "\n裁剪文件路径 = " + cropFilePath);
        ToastUtils.show("成功 || " + cropFilePath);
    }

    /**
     * 头部裁剪跳过
     * <p>
     * 业务过滤 (未在触发区、缝隙未识别到二维码等)
     *
     * @param reason 原因
     */
    @Override
    public void onHeaderCropSkip(String reason) {
        Log.d(TAG, "跳过 || " + reason);
        ToastUtils.show("跳过 || " + reason);
    }

    /**
     * 头部裁剪错误
     * <p>
     * 系统错误 (拍照失败、引擎崩溃、文件读取失败、图片裁剪失败等)
     *
     * @param errorMsg 错误消息
     */
    @Override
    public void onHeaderCropError(String errorMsg) {
        Log.e(TAG, "错误 || " + errorMsg);
        ToastUtils.show("错误 || " + errorMsg);
    }
}