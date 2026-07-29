package com.qtone.camerause;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.jiangdg.ausbc.utils.ToastUtils;

/**
 * @decs: 主页
 * @author: 郑少鹏
 * @date: 2026/7/28 16:14
 * @version: v 1.0
 */
public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 布局包含 ID 为 R.id.fragment_container 的 FrameLayout
        setContentView(R.layout.activity_main);
        checkAndRequestPermission();
    }

    private void checkAndRequestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CAMERA_CODE
            );
        } else {
            loadScanFragment();
        }
    }

    private void loadScanFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ScanFragment())
                .commitAllowingStateLoss();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_CODE) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                loadScanFragment();
            } else {
                ToastUtils.show("需要相机权限才能使用 USB 相机");
            }
        }
    }
}