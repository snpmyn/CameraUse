package com.qtone.camerause.widget.camera;

import android.hardware.usb.UsbDevice;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.utils.ToastUtils;
import com.qtone.camerause.util.list.ListUtils;
import com.qtone.camerause.widget.dialog.singleselect.SingleSelectDialog;
import com.qtone.camerause.widget.dialog.singleselect.bean.SingleSelectDialogBean;
import com.qtone.camerause.widget.dialog.singleselect.kit.SingleSelectDialogKit;
import com.qtone.camerause.widget.dialog.singleselect.listener.SingleSelectDialogClickListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        return String.format(Locale.US, "0x%04X_0x%04X", usbDevice.getVendorId(), usbDevice.getProductId());
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
        if ((iCamera == null) || (multiCameraClient == null) || (currentUsbDevice == null)) {
            ToastUtils.show("未检测到摄像头");
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
        List<SingleSelectDialogBean> singleSelectDialogBeanList = new ArrayList<>();
        int defaultSelectedIndex = 0;
        for (int i = 0; i < usbDeviceList.size(); i++) {
            UsbDevice usbDevice = usbDeviceList.get(i);
            String usbDeviceUniqueId = getUsbDeviceUniqueId(usbDevice);
            // 文本组装
            String productName = usbDevice.getProductName();
            if (TextUtils.isEmpty(productName)) {
                // 文本兜底
                productName = usbDevice.getDeviceName();
            }
            String itemText = productName + " [" + usbDeviceUniqueId + "]";
            // 是否选中
            boolean isChecked = false;
            if (!TextUtils.isEmpty(currentUsbDeviceUniqueId) && usbDeviceUniqueId.equals(currentUsbDeviceUniqueId)) {
                isChecked = true;
                defaultSelectedIndex = i;
            }
            // 添加单选对话框数据
            singleSelectDialogBeanList.add(new SingleSelectDialogBean(itemText, isChecked));
        }
        final int finalDefaultSelectedIndex = defaultSelectedIndex;
        // 显示单选弹框 + 点击切换相机
        SingleSelectDialogKit.showSingleSelectDialog(appCompatActivity, "切换摄像头", singleSelectDialogBeanList, finalDefaultSelectedIndex, "切换", new SingleSelectDialogClickListener() {
            @Override
            public <T> void onConfirm(SingleSelectDialog singleSelectDialog, int position, T t) {
                singleSelectDialog.dismiss();
                // 点击默认选中不处理
                if (position == finalDefaultSelectedIndex) {
                    return;
                }
                // 边界校验
                if ((position >= 0) && (position < usbDeviceList.size())) {
                    // 切换相机
                    CameraController.getInstance().switchCamera(iCamera, multiCameraClient, usbDeviceList.get(position));
                }
            }
        });
    }
}