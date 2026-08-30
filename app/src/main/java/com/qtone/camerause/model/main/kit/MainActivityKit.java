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
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.jiangdg.ausbc.camera.bean.PreviewSize;
import com.jiangdg.ausbc.utils.ToastUtils;
import com.qtone.camerause.R;
import com.qtone.camerause.model.camera.CameraMainFragment;
import com.qtone.camerause.model.main.MainActivity;
import com.qtone.camerause.model.setting.SettingActivity;
import com.qtone.camerause.util.intent.IntentJump;
import com.qtone.camerause.util.list.ListUtils;
import com.qtone.camerause.widget.camera.CameraController;
import com.qtone.camerause.widget.camera.CameraSwitchManager;
import com.qtone.camerause.widget.dialog.camerasetting.CameraSettingDialog;
import com.qtone.camerause.widget.dialog.camerasetting.listener.CameraSettingDialogClickListener;
import com.qtone.camerause.widget.dialog.common.kit.CommonDialogKit;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import kotlin.jvm.functions.Function2;

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
        } else if (itemId == R.id.mainActivityMenuSwitchResolution) {
            // 切分辨率
            switchResolution();
        } else if (itemId == R.id.mainActivityMenuSwitchCamera) {
            // 切换相机
            switchCamera();
        } else if (itemId == R.id.mainActivityMenuCameraSetting) {
            // 相机设置
            cameraSetting();
        } else if (itemId == R.id.mainActivityMenuFunctionSetting) {
            // 功能设置
            functionSetting();
        }
    }

    /**
     * 设备信息
     */
    private void deviceInfo() {
        UsbDevice usbDevice = CameraController.getInstance().getUsbDevice(getCameraMainFragment().getCurrentCamera());
        if (usbDevice == null) {
            ToastUtils.show("未检测到摄像头");
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

    /**
     * 切分辨率
     * <p>
     * 不再调用
     * {@link CameraMainFragment#updateResolution(int, int)}
     * 替换调用
     * {@link CameraMainFragment#updatePreviewSize(int, int, Function2)}
     * <p>
     * 传输格式
     * 1. FRAME_FORMAT_MJPEG - 压缩流格式
     * - 图像在摄像头硬件内部经 Motion JPEG 压缩后再传入系统，占用 USB 带宽极小。
     * - 支持在大分辨率 (1080P / 4K) 下保持高帧率 (30 ~ 60 FPS)
     * - 作为首选默认切换格式
     * 2. FRAME_FORMAT_YUYV - 未压缩原始数据流格式
     * - 未经任何压缩的裸数据 (2 Bytes / Pixel)，对系统与 USB 总线带宽要求极高。
     * - 受限于 USB 2.0 带宽瓶颈，大分辨率下硬件帧率会被迫降至 5 ~ 15 FPS，甚至引发底层传输丢帧。
     * - 摄像头硬件在目标分辨率下不支持 MJPEG 格式时降级适配
     * <p>
     * 相机关闭原因说明
     * - 若 MJPEG 与 YUYV 两次 setPreviewSize 均抛异常，说明摄像头固件 (UVC Firmware) 根本不支持该目标分辨率或底层 USB 管道 (Pipe) 配流失败。
     * - 此时由于在尝试切换前已调 stopPreview() 停流，若不及时拦截抛出失败，系统将无法继续渲染后续帧，表现为预览画面黑屏 / 挂起 (即相机预览被迫关闭)。
     */
    private void switchResolution() {
        List<PreviewSize> previewSizes = CameraController.getInstance().getAllPreviewSizes(getCameraMainFragment().getCurrentCamera(), null);
        if (ListUtils.listIsEmpty(previewSizes)) {
            ToastUtils.show("获取预览分辨率失败");
            return;
        }
        int selectedIndex = -1;
        String[] items = new String[previewSizes.size()];
        PreviewSize currentPreviewSize = CameraController.getInstance().getCurrentPreviewSize(getCameraMainFragment().getCurrentCamera());
        for (int i = 0; i < previewSizes.size(); i++) {
            PreviewSize previewSize = previewSizes.get(i);
            int previewSizeWidth = previewSize.getWidth();
            int previewSizeHeight = previewSize.getHeight();
            if ((currentPreviewSize != null) && (currentPreviewSize.getWidth() == previewSizeWidth) && (currentPreviewSize.getHeight() == previewSizeHeight)) {
                selectedIndex = i;
            }
            items[i] = (previewSizeWidth + " x " + previewSizeHeight);
        }
        final int initialSelectedIndex = selectedIndex;
        int finalSelectedIndex = selectedIndex;
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(mainActivity)
                .setSingleChoiceItems(items, finalSelectedIndex, (dialog, which) -> {
                    // 相同分辨率无需重复做流重置
                    if (which != initialSelectedIndex) {
                        PreviewSize selectedPreviewSize = previewSizes.get(which);
                        CameraController.getInstance().updatePreviewSize(getCameraMainFragment().getCurrentCamera(),
                                selectedPreviewSize.getWidth(),
                                selectedPreviewSize.getHeight(),
                                getCameraMainFragment().getTextureView(),
                                (isSuccess, formatMode) -> {
                                    if (Boolean.TRUE.equals(isSuccess)) {
                                        String modeDesc = formatMode != null ? " [ " + formatMode + " ]" : "";
                                        ToastUtils.show("分辨率已切换为 " + selectedPreviewSize.getWidth() + " x " + selectedPreviewSize.getHeight() + modeDesc);
                                    } else {
                                        ToastUtils.show("分辨率切换失败");
                                    }
                                    return null;
                                }
                        );
                    }
                    dialog.dismiss();
                }).show();
        if (alertDialog.getListView() != null) {
            alertDialog.getListView().setVerticalScrollBarEnabled(false);
        }
    }

    /**
     * 切换相机
     */
    private void switchCamera() {
        CameraSwitchManager.showCameraSelectDialog(mainActivity, getCameraMainFragment().getCurrentCamera(),
                getCameraMainFragment().getMultiCameraClient(), CameraController.getInstance().getUsbDevice(getCameraMainFragment().getCurrentCamera()));
    }

    /**
     * 相机设置
     */
    private void cameraSetting() {
        if (getCameraMainFragment().getCurrentCamera() == null) {
            ToastUtils.show("未检测到摄像头");
            return;
        }
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
                }).show();
    }

    /**
     * 功能设置
     */
    private void functionSetting() {
        IntentJump.getInstance().jumpWithAnimation(null, mainActivity, false, SettingActivity.class, 0, 0);
    }
}