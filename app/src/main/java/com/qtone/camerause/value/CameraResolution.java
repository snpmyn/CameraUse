package com.qtone.camerause.value;

/**
 * Created on 2026/8/5.
 *
 * @author 郑少鹏
 * @desc 相机分辨率
 * <p>
 * 在 USB 摄像头硬件驱动协商 (Negotiation) 过程中
 * 640 x 480 @ 30fps 是所有 USB 摄像头设备在 firmware(固件) 中强制要求必须具备的基础保底分辨率
 * <p>
 * 当应用层通过 CameraRequest 请求一个外接摄像头固件不支持的分辨率 (如之前的 1080 x 720) 时
 * 如果 UVC 底层 C/C++ 库 (如 libuvc / UVCCamera) 无法与硬件建立匹配的数据流管道
 * 那么为防止程序直接崩溃或黑屏
 * 底层便会自动触发降级保护
 * 自动回退到绝对安全分辨率 640 x 480
 * <p>
 * 请求分辨率 - 1080 x 720
 * 硬件匹配状态 - 非标准 / 无硬件支持
 * 实际输出分辨率 - 640 x 480
 * 输出 NV21 字节公式 - W x H x 1.5
 * 实际字节流计算 - 640 x 480 x 1.5
 * 实际字节流大小 - 460,800 Byte
 * 状态解析 - UVC 驱动协商失败 (自动安全降级至 VGA)
 */
@SuppressWarnings("unused")
public enum CameraResolution {
    /**
     * 请求分辨率 - 3840 x 2160
     * 硬件匹配状态 - 硬件支持
     * 宽高比 - 16 : 9
     * 实际输出分辨率 - 3840 x 2160
     * 输出 NV21 字节公式 - W x H x 1.5
     * 实际字节流计算 - 3840 x 2160 x 1.5
     * 实际字节流大小 - 12,441,600 Byte
     * 状态解析 - 4K 超高清输出 (8.3MP)
     * <p>
     * 4K 视频录制 / 极清拍照 / 细节展示
     */
    RES_3840_2160(3840, 2160),
    /**
     * 请求分辨率 - 2592 x 1944
     * 硬件匹配状态 - 硬件支持
     * 宽高比 - 4 : 3
     * 实际输出分辨率 - 2592 x 1944
     * 输出 NV21 字节公式 - W x H x 1.5
     * 实际字节流计算 - 2592 x 1944 x 1.5
     * 实际字节流大小 - 7,558,272 Byte
     * 状态解析 - 完整高清输出 (5MP)
     * <p>
     * 文档拍照 / 试卷识别 / OCR (高清无损)
     */
    RES_2592_1944(2592, 1944),
    /**
     * 请求分辨率 - 1920 x 1080
     * 硬件匹配状态 - 硬件支持
     * 宽高比 - 16 : 9
     * 实际输出分辨率 - 1920 x 1080
     * 输出 NV21 字节公式 - W x H x 1.5
     * 实际字节流计算 - 1920 x 1080 x 1.5
     * 实际字节流大小 - 3,110,400 Byte
     * 状态解析 - 完整 FHD 1080p 输出
     * <p>
     * 高质量实时预览 / 高清扫码
     */
    RES_1920_1080(1920, 1080),
    /**
     * 请求分辨率 - 1280 x 720
     * 硬件匹配状态 - 硬件支持
     * 宽高比 - 16 : 9
     * 实际输出分辨率 - 1280 x 720
     * 输出 NV21 字节公式 - W x H x 1.5
     * 实际字节流计算 - 1280 x 720 x 1.5
     * 实际字节流大小 - 1,382,400 Byte
     * 状态解析 - 完整 HD 720p 输出
     * <p>
     * 流畅扫码 / 低功耗实时分析
     */
    RES_1280_720(1280, 720);
    /**
     * 宽
     */
    private final int width;
    /**
     * 高
     */
    private final int height;

    /**
     * constructor
     *
     * @param width  宽
     * @param height 高
     */
    CameraResolution(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * 获取宽
     *
     * @return 宽
     */
    public int getWidth() {
        return width;
    }

    /**
     * 获取高
     *
     * @return 高
     */
    public int getHeight() {
        return height;
    }
}