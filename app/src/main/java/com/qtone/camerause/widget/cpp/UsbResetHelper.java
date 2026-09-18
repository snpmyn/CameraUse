package com.qtone.camerause.widget.cpp;

import android.hardware.usb.UsbDeviceConnection;
import android.util.Log;

import com.qtone.camerause.util.log.LogKit;

/**
 * Created on 2026/9/17.
 *
 * @author 郑少鹏
 * @desc USB 重置辅助者
 */
public class UsbResetHelper {
    static {
        try {
            // 加载 CMake 生成的 so 库
            System.loadLibrary("usb_reset_lib");
        } catch (UnsatisfiedLinkError e) {
            Log.e(LogKit.TAG, "Failed to load native library usb_reset_lib", e);
        }
    }

    /**
     * 重置 USB 设备
     *
     * @param fd 通过 UsbDeviceConnection.getFileDescriptor() 获取的文件描述符
     * @return 是否重置成功
     */
    public static native boolean resetUsbDevice(int fd);

    /**
     * 重置并关闭
     * <p>
     * 注意事项与行为表现
     * 1. 本方法发送底层硬件复位指令 (USBDEVFS_RESET) 并强行关闭当前 UsbDeviceConnection
     * 2. 调用后当前视频预览流会立即中断 (画面冻结 / 黑屏) 但无需重启 APP 进程
     * 3. 画面不会自动恢复，需在底层重置 (1 ~ 2 秒) 后，由业务代码重新 openDevice 并重启预览流。
     *
     * @param usbDeviceConnection UsbDeviceConnection
     * @param onUsbResetCallback  USB 重置回调
     */
    public static void resetAndClose(UsbDeviceConnection usbDeviceConnection, OnUsbResetCallback onUsbResetCallback) {
        if (usbDeviceConnection == null) {
            Log.w(LogKit.TAG, "UsbDeviceConnection is null, skip reset.");
            if (onUsbResetCallback != null) {
                onUsbResetCallback.onUsbResetFinish(false);
            }
            return;
        }
        int fd = usbDeviceConnection.getFileDescriptor();
        boolean success = false;
        if (fd >= 0) {
            success = resetUsbDevice(fd);
            Log.i(LogKit.TAG, "Reset USB result: " + success);
        } else {
            Log.e(LogKit.TAG, "Invalid FileDescriptor from UsbDeviceConnection");
        }
        // 重置指令发送完成后，关闭当前连接。
        try {
            usbDeviceConnection.close();
        } catch (Exception e) {
            Log.e(LogKit.TAG, "error closing connection", e);
        }
        // 执行完成回调
        if (onUsbResetCallback != null) {
            onUsbResetCallback.onUsbResetFinish(success);
        }
    }

    /**
     * USB 重置回调
     */
    public interface OnUsbResetCallback {
        /**
         * USB 重置完成
         *
         * @param success 是否重置成功
         */
        void onUsbResetFinish(boolean success);
    }
}