package com.qtone.camerause.model.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewbinding.ViewBinding;

import com.jiangdg.ausbc.utils.ToastUtils;
import com.qtone.camerause.base.BasePoolActivity;
import com.qtone.camerause.databinding.ActivityMainBinding;
import com.qtone.camerause.model.camera.CameraMainFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * @decs: 主页
 * @author: 郑少鹏
 * @date: 2026/7/28 16:14
 * @version: v 1.0
 */
public class MainActivity extends BasePoolActivity {
    /**
     * 请求相机权限码
     */
    private static final int REQUEST_CAMERA_PERMISSION_CODE = 100;
    /**
     * ActivityMainBinding
     */
    private ActivityMainBinding activityMainBinding;
    /**
     * 管理应用所有文件权限活动结果启动器
     */
    private final ActivityResultLauncher<Intent> manageAppAllFilesAccessPermissionActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (!Environment.isExternalStorageManager()) {
                        // 用户未授权
                        ToastUtils.show("需要所有文件管理权限才能存储文件");
                    } else {
                        // 加载相机主碎片
                        loadCameraMainFragment();
                    }
                }
            });

    /**
     * ViewBinding
     * <p>
     * Java 动态绑定
     * Java 运行时多态
     * Java 动态分派机制
     * <p>
     * 如果子类重写 viewBinding()
     * 那么 onCreate() 中调用时会优先执行子类的方法
     *
     * @return ViewBinding
     */
    @Override
    protected ViewBinding viewBinding() {
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        return activityMainBinding;
    }

    /**
     * 初始控件
     */
    @Override
    protected void stepUi() {

    }

    /**
     * 初始配置
     */
    @Override
    protected void initConfiguration() {

    }

    /**
     * 设置监听
     */
    @Override
    protected void setListener() {

    }

    /**
     * 开始逻辑
     */
    @Override
    protected void startLogic() {
        // 检查并请求权限
        checkAndRequestPermission();
    }

    /**
     * 加载相机主碎片
     */
    private void loadCameraMainFragment() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (getSupportFragmentManager().findFragmentById(activityMainBinding.mainActivityFcv.getId()) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(activityMainBinding.mainActivityFcv.getId(), new CameraMainFragment())
                    .commit();
        }
    }

    /**
     * 检查并请求权限
     */
    public void checkAndRequestPermission() {
        // 1. 检查常规运行时权限
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean hasStoragePermission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                || (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasCameraPermission || !hasStoragePermission) {
            List<String> permissionsNeeded = new ArrayList<>();
            if (!hasCameraPermission) {
                permissionsNeeded.add(Manifest.permission.CAMERA);
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
            ActivityCompat.requestPermissions(
                    this,
                    permissionsNeeded.toArray(new String[0]),
                    REQUEST_CAMERA_PERMISSION_CODE
            );
            return;
        }
        // 2. 检查所有文件管理权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                manageAppAllFilesAccessPermissionActivityResultLauncher.launch(intent);
                return;
            }
        }
        // 加载相机主碎片
        loadCameraMainFragment();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_CODE) {
            // 校验申请的常规运行时权限是否均被授予
            boolean allGranted = true;
            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
            } else {
                allGranted = false;
            }
            if (allGranted) {
                // 常规运行时权限均被授予 -> 继续检查所有文件管理权限
                checkAndRequestPermission();
            } else {
                // 权限申请被拒
                ToastUtils.show("需要相机和存储权限才能正常使用");
            }
        }
    }
}