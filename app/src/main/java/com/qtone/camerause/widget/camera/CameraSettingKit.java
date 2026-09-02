package com.qtone.camerause.widget.camera;

import android.util.Log;

import com.jiangdg.ausbc.MultiCameraClient;
import com.qtone.camerause.util.log.LogKit;

/**
 * Created on 2026/9/2.
 *
 * @author 郑少鹏
 * @desc 相机设置配套原件
 */
public class CameraSettingKit {
    /**
     * 相机设置
     * <p>
     * 相机打开后接着进行相机设置导致黑屏
     * 1. 相机参数 (如 Gain, Gamma, Brightness 等) 本质上是作用于物理 Sensor (传感器) 和 ISP (图像信号处理器) 的硬件寄存器或驱动状态
     * 2. 无设备句柄
     * 在相机打开前，底层驱动程序尚未初始化，操作系统和 SDK 根本不知道要把这些参数发送给哪块硬件。
     * 3. 硬件未通电 / 复位
     * 相机处于关闭状态时，Sensor 可能处于低功耗模式或未通电状态，此时发送任何控制指令都会返回错误 (如 NullPointer、DeviceNotReady) 或直接被驱动丢弃。
     * <p>
     * 无法设置增益
     * 1. 在 UVC 协议中，增益 (Gain) 受自动曝光 (Auto Exposure) 强管控。
     * 只要摄像头处于自动曝光状态，ISP 芯片就会强制覆盖用户传入的 Gain 值，导致 setGain 写入失败或被清零。
     * 2. 意味这台 USB 摄像头的硬件固件 (Firmware) 在 UVC 标准 Processing Unit 中完全没有开放 Gain (增益) 控制寄存器
     * 底层向摄像头发送 UVC 控制指令时，由于硬件不支持该控制项，摄像头直接拒绝响应，底层 C/C++ 驱动层返回了错误码 -1。
     *
     * @param iCamera 相机实例
     */
    public static void cameraSetting(MultiCameraClient.ICamera iCamera) {
        // 亮度
        Integer bMin = CameraController.getInstance().getBrightnessMin(iCamera);
        Integer bMax = CameraController.getInstance().getBrightnessMax(iCamera);
        int brightness = calculateValidHardwareValue(43, bMin, bMax);
        CameraController.getInstance().setBrightness(iCamera, brightness);
        if (CameraController.getInstance().getBrightness(iCamera) != 43) {
            CameraController.getInstance().setBrightness(iCamera, brightness + 1);
        }
        Log.d(LogKit.TAG, "亮度 [硬件范围 " + bMin + " ~ " + bMax + "] - 设置 " + brightness + " || 实际 " + CameraController.getInstance().getBrightness(iCamera));
        // 对比度
        Integer cMin = CameraController.getInstance().getContrastMin(iCamera);
        Integer cMax = CameraController.getInstance().getContrastMax(iCamera);
        int contrast = calculateValidHardwareValue(58, cMin, cMax);
        CameraController.getInstance().setContrast(iCamera, contrast);
        if (CameraController.getInstance().getContrast(iCamera) != 58) {
            CameraController.getInstance().setContrast(iCamera, contrast + 1);
        }
        Log.d(LogKit.TAG, "对比度 [硬件范围 " + cMin + " ~ " + cMax + "] - 设置 " + contrast + " || 实际 " + CameraController.getInstance().getContrast(iCamera));
        // 增益
        Integer gMin = CameraController.getInstance().getGainMin(iCamera);
        Integer gMax = CameraController.getInstance().getGainMax(iCamera);
        int gain = calculateValidHardwareValue(60, gMin, gMax);
        CameraController.getInstance().setGain(iCamera, gain);
        if (CameraController.getInstance().getGain(iCamera) != 60) {
            CameraController.getInstance().setGain(iCamera, gain + 1);
        }
        Log.d(LogKit.TAG, "增益 [硬件范围 " + gMin + " ~ " + gMax + "] - 设置 " + gain + " || 实际 " + CameraController.getInstance().getGain(iCamera));
        // Gamma
        Integer gammaMin = CameraController.getInstance().getGammaMin(iCamera);
        Integer gammaMax = CameraController.getInstance().getGammaMax(iCamera);
        int gamma = calculateValidHardwareValue(31, gammaMin, gammaMax);
        CameraController.getInstance().setGamma(iCamera, gamma);
        if (CameraController.getInstance().getGamma(iCamera) != 31) {
            CameraController.getInstance().setGamma(iCamera, gamma + 1);
        }
        Log.d(LogKit.TAG, "Gamma [硬件范围 " + gammaMin + " ~ " + gammaMax + "] - 设置 " + gamma + " || 实际 " + CameraController.getInstance().getGamma(iCamera));
        // 色调
        Integer hMin = CameraController.getInstance().getHueMin(iCamera);
        Integer hMax = CameraController.getInstance().getHueMax(iCamera);
        int hue = calculateValidHardwareValue(52, hMin, hMax);
        CameraController.getInstance().setHue(iCamera, hue);
        if (CameraController.getInstance().getHue(iCamera) != 52) {
            CameraController.getInstance().setHue(iCamera, hue + 1);
        }
        Log.d(LogKit.TAG, "色调 [硬件范围 " + hMin + " ~ " + hMax + "] - 设置 " + hue + " || 实际 " + CameraController.getInstance().getHue(iCamera));
        // 锐度
        Integer sMin = CameraController.getInstance().getSharpnessMin(iCamera);
        Integer sMax = CameraController.getInstance().getSharpnessMax(iCamera);
        int sharpness = calculateValidHardwareValue(60, sMin, sMax);
        CameraController.getInstance().setSharpness(iCamera, sharpness);
        if (CameraController.getInstance().getSharpness(iCamera) != 60) {
            CameraController.getInstance().setSharpness(iCamera, sharpness + 1);
        }
        Log.d(LogKit.TAG, "锐度 [硬件范围 " + sMin + " ~ " + sMax + "] - 设置 " + sharpness + " || 实际 " + CameraController.getInstance().getSharpness(iCamera));
        // 饱和度
        Integer satMin = CameraController.getInstance().getSaturationMin(iCamera);
        Integer satMax = CameraController.getInstance().getSaturationMax(iCamera);
        int saturation = calculateValidHardwareValue(82, satMin, satMax);
        CameraController.getInstance().setSaturation(iCamera, saturation);
        if (CameraController.getInstance().getSaturation(iCamera) != 82) {
            CameraController.getInstance().setSaturation(iCamera, saturation + 1);
        }
        Log.d(LogKit.TAG, "饱和度 [硬件范围 " + satMin + " ~ " + satMax + "] - 设置 " + saturation + " || 实际 " + CameraController.getInstance().getSaturation(iCamera));
    }

    /**
     * 计算对齐硬件值
     * <p>
     * 计算对齐目标值到 UVC 合法硬件范围内
     *
     * @param targetValue 目标值
     * @param minValue    最小值
     * @param maxValue    最大值
     * @return 硬件最终完美接受的值
     */
    private static int calculateValidHardwareValue(int targetValue, Integer minValue, Integer maxValue) {
        if ((minValue == null) || (maxValue == null)) {
            return targetValue;
        }
        return Math.max(minValue, Math.min(maxValue, targetValue));
    }

    /**
     * 重置
     *
     * @param iCamera 相机实例
     */
    public static void reset(MultiCameraClient.ICamera iCamera) {
        // 重置亮度
        CameraController.getInstance().resetBrightness(iCamera);
        // 重置对比度
        CameraController.getInstance().resetContrast(iCamera);
        // 重置增益
        CameraController.getInstance().resetGain(iCamera);
        // 重置 Gamma
        CameraController.getInstance().resetGamma(iCamera);
        // 重置色调
        CameraController.getInstance().resetHue(iCamera);
        // 重置锐度
        CameraController.getInstance().resetSharpness(iCamera);
        // 重置饱和度
        CameraController.getInstance().resetSaturation(iCamera);
    }
}