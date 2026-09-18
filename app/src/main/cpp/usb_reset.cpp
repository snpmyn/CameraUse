#include <jni.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/usbdevice_fs.h>
#include <unistd.h>
#include <android/log.h>

#define LOG_TAG "UsbResetJni"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jboolean JNICALL
// 精确定位包名 com.qtone.camerause.widget.cpp 和静态类 UsbResetHelper
Java_com_qtone_camerause_widget_cpp_UsbResetHelper_resetUsbDevice(
        JNIEnv *env,
        jclass clazz,
        jint fd) {
    if (fd < 0) {
        LOGE("Invalid file descriptor: %d", fd);
        return JNI_FALSE;
    }

    // 向内核发送 USBDEVFS_RESET 强制重置 Endpoint 状态
    int result = ioctl(fd, USBDEVFS_RESET, 0);
    if (result < 0) {
        LOGE("ioctl USBDEVFS_RESET failed with result: %d", result);
        return JNI_FALSE;
    }

    LOGI("USBDEVFS_RESET succeeded on fd: %d", fd);
    return JNI_TRUE;
}