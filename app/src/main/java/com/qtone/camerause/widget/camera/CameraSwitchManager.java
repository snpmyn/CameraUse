package com.qtone.camerause.widget.camera;

import android.hardware.usb.UsbDevice;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.utils.ToastUtils;
import com.qtone.camerause.R;
import com.qtone.camerause.util.density.DensityUtils;
import com.qtone.camerause.util.list.ListUtils;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @decs: 相机切换管理器
 * @author: 郑少鹏
 * @date: 2026/8/25 17:59
 * @version: v 1.0
 */
public class CameraSwitchManager {
    /**
     * 获取 USB 设备唯一标识
     *
     * @param usbDevice USB 设备
     * @return USB 设备唯一标识
     */
    private static @NotNull String getUsbDeviceUniqueId(UsbDevice usbDevice) {
        if (usbDevice == null) {
            return "";
        }
        return (usbDevice.getVendorId() + "_" + usbDevice.getProductId());
    }

    /**
     * 显示相机选择对话框
     *
     * @param appCompatActivity 活动
     * @param iCamera           相机实例
     * @param multiCameraClient 多相机客户端
     * @param currentUsbDevice  当前 USB 设备
     */
    public static void showCameraSelectDialog(AppCompatActivity appCompatActivity, MultiCameraClient.ICamera iCamera, MultiCameraClient multiCameraClient, UsbDevice currentUsbDevice) {
        if ((appCompatActivity == null) || appCompatActivity.isFinishing() || appCompatActivity.isDestroyed()) {
            return;
        }
        if ((iCamera == null) || (multiCameraClient == null) || (currentUsbDevice == null)) {
            return;
        }
        List<UsbDevice> usbDeviceList = CameraController.getInstance().getDeviceList(multiCameraClient);
        if (ListUtils.listIsEmpty(usbDeviceList)) {
            ToastUtils.show("未检测到摄像头");
            return;
        }
        // 当前 USB 设备唯一标识
        String currentUsbDeviceUniqueId = getUsbDeviceUniqueId(currentUsbDevice);
        // 构建弹框选项列表 + 定位默认选中索引
        String[] items = new String[usbDeviceList.size()];
        // 默认选中下标
        int defaultSelectedIndex = 0;
        for (int i = 0; i < usbDeviceList.size(); i++) {
            UsbDevice usbDevice = usbDeviceList.get(i);
            String usbDeviceUniqueId = getUsbDeviceUniqueId(usbDevice);
            // 组装显示文本
            String productName = usbDevice.getProductName();
            if (TextUtils.isEmpty(productName)) {
                productName = usbDevice.getDeviceName();
            }
            items[i] = productName + "\n[" + usbDeviceUniqueId + "]";
            // 高亮逻辑判断
            if (!TextUtils.isEmpty(currentUsbDeviceUniqueId) && usbDeviceUniqueId.equals(currentUsbDeviceUniqueId)) {
                defaultSelectedIndex = i;
            }
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                appCompatActivity,
                com.google.android.material.R.layout.select_dialog_singlechoice_material,
                items
        ) {
            @NotNull
            @Override
            public View getView(int position, View convertView, @NotNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    // 条目字体大小
                    ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_SP, DensityUtils.spResToSp(appCompatActivity, R.dimen.sp_16));
                }
                return view;
            }
        };
        final int finalDefaultSelectedIndex = defaultSelectedIndex;
        // 显示单选弹框 + 点击切换相机
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(appCompatActivity, R.style.CustomMaterialAlertDialogTheme)
                .setTitle("选择摄像头")
                .setSingleChoiceItems(arrayAdapter, defaultSelectedIndex, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == finalDefaultSelectedIndex) {
                        // 点击默认选中不处理
                        return;
                    }
                    UsbDevice selectedUsbDevice = usbDeviceList.get(which);
                    CameraController.getInstance().switchCamera(iCamera, multiCameraClient, selectedUsbDevice);
                })
                .show();
        if (alertDialog.getListView() != null) {
            alertDialog.getListView().setVerticalScrollBarEnabled(false);
        }
    }
}