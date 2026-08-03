//package com.qtone.camerause.application;
//
//import android.graphics.ImageFormat;
//import android.graphics.Rect;
//import android.graphics.YuvImage;
//import android.media.MediaScannerConnection;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//
//import com.jiangdg.ausbc.MultiCameraClient;
//import com.jiangdg.ausbc.base.CameraFragment;
//import com.jiangdg.ausbc.callback.ICaptureCallBack;
//import com.jiangdg.ausbc.camera.bean.CameraRequest;
//import com.jiangdg.ausbc.render.env.RotateType;
//import com.jiangdg.ausbc.utils.ToastUtils;
//import com.jiangdg.ausbc.widget.AspectRatioTextureView;
//import com.jiangdg.ausbc.widget.IAspectRatio;
//import com.qtone.camerause.ExamHeaderProcessor;
//import com.qtone.camerause.R;
//
//import org.jetbrains.annotations.NotNull;
//
//import java.io.File;
//import java.io.FileOutputStream;
//
///**
// * Created on 2026/7/29.
// *
// * @author 郑少鹏
// * @desc 拍照碎片 - 物理级 1:1 真无损全清版
// */
//public class CaptureFragment extends CameraFragment {
//    private static final String TAG = CaptureFragment.class.getSimpleName();
//    private static final int PREVIEW_WIDTH = 2592;
//    private static final int PREVIEW_HEIGHT = 1944;
//
//    private AspectRatioTextureView aspectRatioTextureView;
//    private ViewGroup mContainer;
//
//    /**
//     * 试卷头处理器
//     */
//    private ExamHeaderProcessor examHeaderProcessor;
//    private ExamHeaderProcessor.OnHeaderCropCallback onHeaderCropCallback;
//
//    // 💡 标记当前是否触发了拍照，用于从 rawData 回调中抓取最新一帧真正的 2592x1944 YUV 纯硬件数据
//    private volatile boolean isCaptureRequested = false;
//    private String mTargetSavePath = null;
//
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//        if (getContext() != null) {
//            examHeaderProcessor = new ExamHeaderProcessor(getContext());
//        }
//    }
//
//    @Nullable
//    @Override
//    protected View getRootView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container) {
//        View root = inflater.inflate(R.layout.fragment_capture, container, false);
//        aspectRatioTextureView = root.findViewById(R.id.captureFragmentArtv);
//        mContainer = root.findViewById(R.id.camera_container);
//        return root;
//    }
//
//    @Nullable
//    @Override
//    protected IAspectRatio getCameraView() {
//        return aspectRatioTextureView;
//    }
//
//    @Nullable
//    @Override
//    protected ViewGroup getCameraViewContainer() {
//        return mContainer;
//    }
//
//    @NonNull
//    @Override
//    protected CameraRequest getCameraRequest() {
//        return new CameraRequest.Builder()
//                .setPreviewWidth(PREVIEW_WIDTH)
//                .setPreviewHeight(PREVIEW_HEIGHT)
//                .setRenderMode(CameraRequest.RenderMode.OPENGL)
//                .setDefaultRotateType(RotateType.ANGLE_0)
//                .setAspectRatioShow(true)
//                // 💡 必须开启 RawPreviewData，底层硬件才会抛出 2592x1944 原始点阵
//                .setRawPreviewData(true)
//                .create();
//    }
//
//    @Override
//    public void onCameraState(@NotNull MultiCameraClient.ICamera self, @NotNull State code, @Nullable String msg) {
//        if (code == State.OPENED) {
//            Log.d(TAG, "拍照相机打开成功");
//
//            if (aspectRatioTextureView != null && getActivity() != null) {
//                getActivity().runOnUiThread(() -> aspectRatioTextureView.setAspectRatio(PREVIEW_WIDTH, PREVIEW_HEIGHT));
//            }
//
//            // 💡 正确的 API：使用 addPreviewDataCallBack 监听相机原始 NV21 硬件帧
//            if (getCurrentCamera() != null) {
//                getCurrentCamera().addPreviewDataCallBack((data, width, height, format) -> {
//                    if (isCaptureRequested && data != null) {
//                        isCaptureRequested = false; // 只抓取触发后的第一帧无损数据
//                        Log.d(TAG, "成功捕获硬件底层纯净 YUV 数据帧: " + width + "x" + height + " | 格式: " + format);
//
//                        // 切换到后台线程压缩写盘，避免阻塞主线程
//                        new Thread(() -> processRawYuvToJpeg(data, width, height, mTargetSavePath)).start();
//                    }
//                });
//            }
//
//        } else if (code == State.ERROR) {
//            Log.e(TAG, "拍照相机打开错误 || " + msg);
//        }
//    }
//
//    /**
//     * 拍照
//     */
//    public void capture() {
//        if ((getCurrentCamera() == null) || !getCurrentCamera().isCameraOpened()) {
//            ToastUtils.show("相机未准备就绪");
//            return;
//        }
//        File mediaDir = (getContext() != null) ? getContext().getExternalFilesDir("Pictures") : null;
//        if (mediaDir != null && !mediaDir.exists()) {
//            boolean isCreated = mediaDir.mkdirs();
//            if (!isCreated) {
//                Log.w(TAG, "创建 Pictures 图片保存目录失败");
//            }
//        }
//
//        long timestamp = System.currentTimeMillis();
//
//        // 💡 1. HQ 无损原图：存放在 Pictures 目录下，带有 HQ_RAW_ 前缀标识
//        mTargetSavePath = new File(mediaDir, "HQ_RAW_" + timestamp + ".jpg").getAbsolutePath();
//
//        // 💡 2. TEMP 框架临时文件：重定向存放在私有 Cache 目录下，完全保留原来的 "IMG_时间戳.jpg" 命名
//        File cacheDir = (getContext() != null) ? getContext().getCacheDir() : mediaDir;
//        String tempFramePath = new File(cacheDir, "IMG_" + timestamp + ".jpg").getAbsolutePath();
//
//        // 💡 3. 触发抓取标记：从 addPreviewDataCallBack 中直接提取下一帧物理原图
//        isCaptureRequested = true;
//
//        // 保留框架 captureImage 原生调用，确保回调链路完备
//        captureImage(new ICaptureCallBack() {
//            @Override
//            public void onBegin() {
//                Log.d(TAG, "开始拍照...");
//                ToastUtils.show("开始拍照 ");
//            }
//
//            @Override
//            public void onError(@Nullable String msg) {
//                Log.e(TAG, "拍照错误 || " + msg);
//                ToastUtils.show("拍照错误 || " + msg);
//                isCaptureRequested = false;
//                if (onHeaderCropCallback != null) {
//                    onHeaderCropCallback.onError("拍照错误 || " + msg);
//                }
//            }
//
//            @Override
//            public void onComplete(@Nullable String path) {
//                Log.d(TAG, "相机原生 captureImage 完成触发，私有缓存临时文件路径: " + path);
//                // 静默清空 Cache 目录下的临时低清文件
//                if (path != null) {
//                    File tempFile = new File(path);
//                    if (tempFile.exists()) {
//                        boolean deleted = tempFile.delete();
//                        Log.d(TAG, "私有缓存临时文件自动清理: " + deleted);
//                    }
//                }
//            }
//        }, tempFramePath);
//    }
//
//    /**
//     * 将 UVC 相机 2592x1944 物理底层 NV21 数组直接转写为 100% 质量 JPEG
//     * 绕过纹理渲染层，解决放大模糊、小字糊的问题
//     */
//    private void processRawYuvToJpeg(byte[] nv21Data, int width, int height, String targetPath) {
//        try {
//            YuvImage yuvImage = new YuvImage(nv21Data, ImageFormat.NV21, width, height, null);
//            File file = new File(targetPath);
//
//            // 1. 直接通过 FileOutputStream 写入磁盘，100% 极高品质写入（保留每一个点阵纹理）
//            try (FileOutputStream fos = new FileOutputStream(file)) {
//                yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, fos);
//                fos.flush();
//            }
//
//            // 2. 🔥 核心修复：通知系统 MediaScanner 刷新该文件，保证电脑通过 USB(MTP) 连入时能显示真实的 2MB+ 体积
//            if (getContext() != null) {
//                MediaScannerConnection.scanFile(
//                        getContext(),
//                        new String[]{file.getAbsolutePath()},
//                        null,
//                        null
//                );
//            }
//
//            Log.d(TAG, "🎉 物理 1:1 无损高清图生成成功！真实分辨率: " + width + "x" + height + " | 文件体积: " + (file.length() / 1024) + " KB");
//
//            if (getActivity() != null) {
//                getActivity().runOnUiThread(() -> {
//                    ToastUtils.show("拍照完成 || " + targetPath);
//                    if (examHeaderProcessor != null) {
//                        examHeaderProcessor.process(targetPath, onHeaderCropCallback);
//                    }
//                });
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "处理原始 YUV 数据失败", e);
//            if (onHeaderCropCallback != null && getActivity() != null) {
//                getActivity().runOnUiThread(() -> onHeaderCropCallback.onError("处理高清 YUV 帧失败"));
//            }
//        }
//    }
//
//    /**
//     * 设置头裁剪回调
//     */
//    public void setOnHeaderCropCallback(ExamHeaderProcessor.OnHeaderCropCallback onHeaderCropCallback) {
//        this.onHeaderCropCallback = onHeaderCropCallback;
//    }
//
//    @Override
//    public void onDestroyView() {
//        if (examHeaderProcessor != null) {
//            examHeaderProcessor.destroy();
//        }
//        super.onDestroyView();
//    }
//}