package com.qtone.camerause.capture;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.base.CameraFragment;
import com.jiangdg.ausbc.callback.ICaptureCallBack;
import com.jiangdg.ausbc.camera.bean.CameraRequest;
import com.jiangdg.ausbc.render.env.RotateType;
import com.jiangdg.ausbc.utils.ToastUtils;
import com.jiangdg.ausbc.widget.AspectRatioTextureView;
import com.jiangdg.ausbc.widget.IAspectRatio;
import com.qtone.camerause.R;
import com.qtone.camerause.kit.CameraAspectRatioKit;
import com.qtone.camerause.wechat.WeChatCropEngine;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created on 2026/7/29.
 *
 * @author 郑少鹏
 * @desc 拍照碎片
 */
public class CaptureFragment extends CameraFragment {
    private static final String TAG = CaptureFragment.class.getSimpleName();
    /**
     * 默认请求的相机物理分辨率
     * <p>
     * 宽
     */
    private static final int PREVIEW_WIDTH = 2592;
    /**
     * 默认请求的相机物理分辨率
     * <p>
     * 高
     */
    private static final int PREVIEW_HEIGHT = 1944;
    /**
     * 拍照标记锁
     * <p>
     * 防止多次连续抓帧
     * 使用 AtomicBoolean 保证多线程并发环境下的绝对原子性
     */
    private final AtomicBoolean isCaptureRequested = new AtomicBoolean(false);
    /**
     * 渲染控件与容器
     */
    private AspectRatioTextureView aspectRatioTextureView;
    /**
     * 相机宽高比配套原件
     */
    private CameraAspectRatioKit cameraAspectRatioKit;
    /**
     * 容器
     */
    private ViewGroup container;
    /**
     * 目标保存路径
     * <p>
     * 高清无损图片磁盘最终目标路径
     */
    private String targetSavePath = null;
    /**
     * 试卷裁剪处理器
     */
    private ExamCropProcessor examCropProcessor;
    /**
     * 裁剪回调
     */
    private ExamCropProcessor.OnCropCallback onCropCallback;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (aspectRatioTextureView != null) {
            // 相机宽高比配套原件
            cameraAspectRatioKit = new CameraAspectRatioKit(aspectRatioTextureView);
        }
        if (getContext() != null) {
            // 试卷裁剪处理器
            examCropProcessor = new ExamCropProcessor();

            //examCropProcessor.processAsync(getContext(), new File(getContext().getExternalFilesDir("Pictures"), "HQ_RAW_" + "1785734938447" + ".jpg").getAbsolutePath(), onCropCallback);
            //examCropProcessor.processAsync(getContext(), new File(getContext().getExternalFilesDir("Pictures"), "HQ_RAW_" + "1785752165469" + ".jpg").getAbsolutePath(), onCropCallback);

            WeChatCropEngine cropEngine = new WeChatCropEngine(getActivity());
            cropEngine.process(getActivity(), new File(getContext().getExternalFilesDir("Pictures"), "HQ_RAW_" + "1785752165469" + ".jpg").getAbsolutePath(), true, new WeChatCropEngine.OnCropListener() {
                @Override
                public void onSuccess(Bitmap resultBitmap, String savedPath) {
                    ToastUtils.show("裁剪成功 || " + savedPath);
                }

                @Override
                public void onError(String errorMessage) {
                    ToastUtils.show(errorMessage);
                }
            });
        }
    }

    @Nullable
    @Override
    protected View getRootView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container) {
        View root = inflater.inflate(R.layout.fragment_capture, container, false);
        aspectRatioTextureView = root.findViewById(R.id.captureFragmentArtv);
        this.container = root.findViewById(R.id.camera_container);
        return root;
    }

    @Nullable
    @Override
    protected IAspectRatio getCameraView() {
        // 提供相机渲染组件
        return aspectRatioTextureView;
    }

    @Nullable
    @Override
    protected ViewGroup getCameraViewContainer() {
        // 提供相机渲染容器
        return container;
    }

    /**
     * 构建 CameraRequest 相机配置参数
     *
     * @return CameraRequest 实体
     */
    @NonNull
    @Override
    protected CameraRequest getCameraRequest() {
        return new CameraRequest.Builder()
                .setPreviewWidth(PREVIEW_WIDTH)
                .setPreviewHeight(PREVIEW_HEIGHT)
                // 若仅需扫码且无滤镜需求则设为 CameraRequest.RenderMode.NORMAL
                // 可直接输出 NV21 数据，效率比 OPENGL (RGBA) 更高。
                .setRenderMode(CameraRequest.RenderMode.OPENGL)
                .setDefaultRotateType(RotateType.ANGLE_0)
                .setAspectRatioShow(true)
                // 开启硬件 RawPreviewData 数据输出 (以便在回调中抓取 NV21 原始点阵数据)
                .setRawPreviewData(true)
                .create();
    }

    /**
     * 相机状态变更监听回调
     *
     * @param self 相机客户端对象
     * @param code 相机当前状态枚举
     * @param msg  异常或状态描述信息
     */
    @Override
    public void onCameraState(@NotNull MultiCameraClient.ICamera self, @NotNull State code, @Nullable String msg) {
        if (code == State.OPENED) {
            Log.d(TAG, "拍照相机打开成功");
            // 初始时按默认分辨率配置初始化预览控件展示比例
            if (cameraAspectRatioKit != null) {
                cameraAspectRatioKit.updateAspectRatio(getActivity(), PREVIEW_WIDTH, PREVIEW_HEIGHT);
            }
            // 注册预览帧回调
            self.addPreviewDataCallBack((data, width, height, format) -> {
                // 1. UI 视角动态适配逻辑
                // 直接交由 CameraAspectRatioKit 处理 (内置分辨率去重与线程安全切换，防止界面拉伸或频繁 re-layout)
                if (cameraAspectRatioKit != null) {
                    cameraAspectRatioKit.updateAspectRatio(getActivity(), width, height);
                }
                // 2. 无损拍照捕获逻辑
                // 通过原子操作判断并消费拍照标记，抢占当前唯一的原始硬件 YUV 帧。
                // compareAndSet(true, false) (检查当前值是否为 true，是则立刻将其修改为 false 并返回 true)
                if (isCaptureRequested.compareAndSet(true, false) && (data != null)) {
                    Log.d(TAG, "成功捕获硬件底层 NV21 帧, 字节大小: " + data.length + " Byte | 帧尺寸: " + width + "x" + height);
                    // 开启后台异步子线程进行物理字节转换与写盘
                    new Thread(() -> processRawYuvToJpeg(data, width, height, targetSavePath)).start();
                }
            });
        } else if (code == State.CLOSED) {
            // 相机关闭或断开连接时
            // 重置 CameraAspectRatioKit 内缓存的分辨率记录
            if (cameraAspectRatioKit != null) {
                cameraAspectRatioKit.reset();
            }
        } else if (code == State.ERROR) {
            Log.e(TAG, "拍照相机打开错误 || " + msg);
        }
    }

    /**
     * 拍照
     */
    public void capture() {
        if ((getCurrentCamera() == null) || !getCurrentCamera().isCameraOpened()) {
            ToastUtils.show("相机未准备就绪");
            return;
        }
        // 创建图片保存目录
        File mediaDir = (getContext() != null) ? getContext().getExternalFilesDir("Pictures") : null;
        if ((mediaDir != null) && !mediaDir.exists()) {
            boolean isCreated = mediaDir.mkdirs();
            if (!isCreated) {
                Log.w(TAG, "创建 Pictures 图片保存目录失败");
            }
        }
        long timestamp = System.currentTimeMillis();
        // 1. 最终 HQ 无损原图路径
        // 存储在外部 Pictures 目录
        targetSavePath = new File(mediaDir, "HQ_RAW_" + timestamp + ".jpg").getAbsolutePath();
        // 2. 框架临时低清图路径
        // 放在私有 Cache 目录，完成后自动销毁。
        File cacheDir = (getContext() != null) ? getContext().getCacheDir() : mediaDir;
        String tempFramePath = new File(cacheDir, "IMG_" + timestamp + ".jpg").getAbsolutePath();
        // 3. 置位拍照标记
        // 唤醒 PreviewDataCallBack 进行抓帧
        isCaptureRequested.set(true);
        // 调用相机原生 captureImage 方法，保证框架内部抓拍回调链路正常响应。
        captureImage(new ICaptureCallBack() {
            @Override
            public void onBegin() {
                Log.d(TAG, "开始拍照...");
                ToastUtils.show("开始拍照");
            }

            @Override
            public void onError(@Nullable String msg) {
                Log.e(TAG, "拍照错误 || " + msg);
                ToastUtils.show("拍照错误 || " + msg);
                // 异常时复位拍照标记
                isCaptureRequested.set(false);
                if (onCropCallback != null) {
                    onCropCallback.onCropError("拍照错误 || " + msg);
                }
            }

            @Override
            public void onComplete(@Nullable String path) {
                Log.d(TAG, "原生 captureImage 触发完成，临时文件路径: " + path);
                // 静默删除框架生成在 Cache 目录下的低清缓存图片
                if (path != null) {
                    File tempFile = new File(path);
                    if (tempFile.exists()) {
                        boolean deleted = tempFile.delete();
                        Log.d(TAG, "私有缓存临时文件清理状态: " + deleted);
                    }
                }
            }
        }, tempFramePath);
    }

    /**
     * 将底层物理 NV21 字节数据无损转换为 100% 质量 JPEG 文件
     * <p>
     * 根据 NV21 采样格式物理特性 (Y 占 100%，UV 占 50%，每像素占 1.5 字节)
     * 最小字节长度安全阈值为 width * height * 1.5 (即 width * height * 3 / 2)
     *
     * @param nv21Data   硬件底层抛出的 NV21 字节数组
     * @param width      硬件实际帧宽度
     * @param height     硬件实际帧高度
     * @param targetPath 无损图片输出路径
     */
    private void processRawYuvToJpeg(byte[] nv21Data, int width, int height, String targetPath) {
        if ((nv21Data == null) || (nv21Data.length == 0)) {
            Log.e(TAG, "处理原始 YUV 数据失败: nv21Data 为空");
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (onCropCallback != null) {
                        onCropCallback.onCropError("YUV 数据为空");
                    }
                });
            }
            return;
        }
        // 基础内存物理安全校验
        // NV21 格式的总字节数必须不小于 (width * height * 1.5)
        if (nv21Data.length < (width * height * 3 / 2)) {
            Log.e(TAG, String.format("NV21 字节流长度异常: 实际长度 (%d Byte) 小于 %dx%d 所需的物理空间", nv21Data.length, width, height));
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (onCropCallback != null) {
                        onCropCallback.onCropError("YUV 数据帧截断或损坏");
                    }
                });
            }
            return;
        }
        if (getContext() != null) {
            examCropProcessor.processNv21Async(getContext(), nv21Data, width, height, onCropCallback);
        }
        try {
            // 直接将 1:1 底层点阵转为 YuvImage 实体
            YuvImage yuvImage = new YuvImage(nv21Data, ImageFormat.NV21, width, height, null);
            File file = new File(targetPath);
            // 1. 采用 100% 极高品质写入磁盘
            try (FileOutputStream fos = new FileOutputStream(file)) {
                yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, fos);
                fos.flush();
            }
            // 2. 刷新系统 MediaScanner 媒体库
            // 确保外接 MTP 或文件管理器能实时查看到全尺寸图片
            if (getContext() != null) {
                MediaScannerConnection.scanFile(
                        getContext(),
                        new String[]{file.getAbsolutePath()},
                        null,
                        null
                );
            }
            Log.d(TAG, "物理 1:1 无损图片生成成功！分辨率: " + width + "x" + height + " | 文件大小: " + (file.length() / 1024) + " KB");
            // 3. 切回 UI 线程触发后续业务逻辑 (试卷头裁剪处理与试卷四角透视矫正)
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    ToastUtils.show("拍照完成 || " + targetPath);
                    // 试卷裁剪处理器工作
                    if ((examCropProcessor != null) && (getContext() != null)) {
                        examCropProcessor.processAsync(getContext(), targetPath, onCropCallback);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "处理原始 YUV 数据并写盘时抛出异常", e);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (onCropCallback != null) {
                        onCropCallback.onCropError("处理高清 YUV 帧失败");
                    }
                });
            }
        }
    }

    /**
     * 设置裁剪回调
     *
     * @param onCropCallback 裁剪回调
     */
    public void setOnCropCallback(ExamCropProcessor.OnCropCallback onCropCallback) {
        this.onCropCallback = onCropCallback;
    }

    @Override
    public void onDestroyView() {
        if (examCropProcessor != null) {
            examCropProcessor.destroy();
        }
        if (cameraAspectRatioKit != null) {
            cameraAspectRatioKit.release();
            cameraAspectRatioKit = null;
        }
        super.onDestroyView();
    }
}