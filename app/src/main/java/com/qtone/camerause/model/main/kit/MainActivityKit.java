package com.qtone.camerause.model.main.kit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.jiangdg.ausbc.utils.ToastUtils;
import com.qtone.camerause.R;
import com.qtone.camerause.model.camera.CameraMainFragment;
import com.qtone.camerause.model.main.MainActivity;
import com.qtone.camerause.widget.camera.CameraController;
import com.qtone.camerause.widget.camera.CameraSwitchManager;
import com.qtone.camerause.widget.dialog.camerasetting.CameraSettingDialog;
import com.qtone.camerause.widget.dialog.camerasetting.listener.CameraSettingDialogClickListener;
import com.qtone.camerause.widget.dialog.common.kit.CommonDialogKit;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2026/8/25.
 *
 * @author 郑少鹏
 * @desc 主页配套原件
 */
public class MainActivityKit {
    /**
     * 请求相机权限码
     */
    public static final int REQUEST_CAMERA_PERMISSION_CODE = 100;
    /**
     * 主页
     */
    private final MainActivity mainActivity;
    /**
     * 管理应用所有文件权限活动结果启动器
     */
    private final ActivityResultLauncher<Intent> manageAppAllFilesAccessPermissionActivityResultLauncher;

    /**
     * constructor
     *
     * @param mainActivity 主页
     */
    public MainActivityKit(@NotNull MainActivity mainActivity) {
        // 主页
        this.mainActivity = mainActivity;
        // 管理应用所有文件权限活动结果启动器
        this.manageAppAllFilesAccessPermissionActivityResultLauncher = mainActivity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
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
    }

    /**
     * 加载相机主碎片
     */
    private void loadCameraMainFragment() {
        if (mainActivity.isFinishing() || mainActivity.isDestroyed()) {
            return;
        }
        String tag = CameraMainFragment.class.getSimpleName();
        if (mainActivity.getSupportFragmentManager().findFragmentByTag(tag) == null) {
            mainActivity.getSupportFragmentManager().beginTransaction()
                    .replace(mainActivity.activityMainBinding.mainActivityFcv.getId(), new CameraMainFragment(), tag)
                    .commit();
        }
    }

    /**
     * 获取相机主碎片
     *
     * @return 相机主碎片
     */
    public CameraMainFragment getCameraMainFragment() {
        if (mainActivity.isFinishing() || mainActivity.isDestroyed()) {
            return null;
        }
        Fragment fragment = mainActivity.getSupportFragmentManager().findFragmentByTag(CameraMainFragment.class.getSimpleName());
        if (fragment instanceof CameraMainFragment) {
            return (CameraMainFragment) fragment;
        }
        return null;
    }

    /**
     * 检查并请求权限
     */
    public void checkAndRequestPermission() {
        // 1. 检查常规运行时权限
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean hasStoragePermission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                || (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasCameraPermission || !hasStoragePermission) {
            List<String> permissionsNeeded = new ArrayList<>();
            if (!hasCameraPermission) {
                permissionsNeeded.add(Manifest.permission.CAMERA);
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
            ActivityCompat.requestPermissions(
                    mainActivity,
                    permissionsNeeded.toArray(new String[0]),
                    REQUEST_CAMERA_PERMISSION_CODE
            );
            return;
        }
        // 2. 检查所有文件管理权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + mainActivity.getPackageName()));
                manageAppAllFilesAccessPermissionActivityResultLauncher.launch(intent);
                return;
            }
        }
        // 加载相机主碎片
        loadCameraMainFragment();
    }

    /**
     * 菜单条目点击执行
     *
     * @param menuItem 菜单条目
     */
    public void menuItemClickToExecute(@NotNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.mainActivityMenuDeviceInfo) {
            // 设备信息
            deviceInfo();
        } else if (itemId == R.id.mainActivityMenuCameraSetting) {
            // 相机设置
            CameraSettingDialog cameraSettingDialog = new CameraSettingDialog(mainActivity);
            cameraSettingDialog.setCancelable(false);
            cameraSettingDialog.setTitle("相机设置")
                    .setCameraMainFragment(getCameraMainFragment())
                    .setPositiveText("关闭")
                    .setShowNegative(true)
                    .setNegativeText("重置")
                    .setCameraSettingDialogClickListener(new CameraSettingDialogClickListener() {
                        @Override
                        public void onCancel(CameraSettingDialog cameraSettingDialog) {
                            cameraSettingDialog.reset();
                        }

                        @Override
                        public void onConfirm(CameraSettingDialog cameraSettingDialog) {
                            cameraSettingDialog.dismiss();
                        }
                    })
                    .show();
        } else if (itemId == R.id.mainActivityMenuSwitchCamera) {
            // 切换相机
            CameraSwitchManager.showCameraSelectDialog(mainActivity, getCameraMainFragment().getCurrentCamera(),
                    getCameraMainFragment().getMultiCameraClient(), CameraController.getInstance().getUsbDevice(getCameraMainFragment().getCurrentCamera()));
        }
    }

    /**
     * 设备信息
     */
    private void deviceInfo() {
        CameraMainFragment fragment = getCameraMainFragment();
        if (fragment == null) {
            return;
        }
        UsbDevice usbDevice = CameraController.getInstance().getUsbDevice(fragment.getCurrentCamera());
        if (usbDevice == null) {
            ToastUtils.show("未连接设备");
            return;
        }
        String deviceInfo = "产品名称\t" + usbDevice.getProductName()
                + "\n厂商 ID (VID)\t" + String.format("0x%04X", usbDevice.getVendorId())
                + "\n产品 ID (PID)\t" + String.format("0x%04X", usbDevice.getProductId())
                + "\n生产厂商\t" + usbDevice.getManufacturerName()
                + "\n固件版本\t" + usbDevice.getVersion()
                + "\n序列号\t" + usbDevice.getSerialNumber()
                + "\n设备节点\t" + usbDevice.getDeviceName()
                + "\n设备 ID\t" + usbDevice.getDeviceId();
        CommonDialogKit.showInfoDialog(mainActivity, mainActivity.getString(R.string.deviceInfo), deviceInfo, true, mainActivity.getString(R.string.iKonw), null);
    }
}