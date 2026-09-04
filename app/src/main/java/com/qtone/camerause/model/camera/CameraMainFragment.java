package com.qtone.camerause.model.camera;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.jiangdg.ausbc.utils.ToastUtils;
import com.jiangdg.ausbc.widget.AspectRatioTextureView;
import com.qtone.camerause.R;
import com.qtone.camerause.base.BaseCameraFragment;
import com.qtone.camerause.model.camera.kit.CameraMainFragmentKit;
import com.qtone.camerause.value.CameraResolution;
import com.qtone.camerause.widget.button.OnShimmerButtonCallback;
import com.qtone.camerause.widget.button.ShimmerButton;
import com.qtone.camerause.widget.button.ShimmerButtonState;
import com.qtone.camerause.widget.roi.MultiRoiOverlayView;
import com.qtone.camerause.widget.scan.ViewFinderView;

import org.jetbrains.annotations.NotNull;

/**
 * Created on 2026/8/11.
 *
 * @author 郑少鹏
 * @desc 相机主碎片
 */
public class CameraMainFragment extends BaseCameraFragment implements View.OnClickListener {
    /**
     * 控件
     */
    public ShimmerButton cameraMainFragmentSbSingleCapture;
    public ShimmerButton cameraMainFragmentSbBurstCapture;
    public ShimmerButton cameraMainFragmentSbScanCode;
    private FrameLayout cameraMainFragmentFl;
    private AspectRatioTextureView cameraMainFragmentArtv;
    private ViewFinderView cameraMainFragmentVfv;
    private MultiRoiOverlayView multiRoiOverlayView;
    /**
     * 相机主碎片配套原件
     */
    private CameraMainFragmentKit cameraMainFragmentKit;

    /**
     * 获取布局 ID
     *
     * @return 布局 ID
     */
    @Override
    protected int getLayoutId() {
        return R.layout.fragment_camera_main;
    }

    /**
     * 获取 TextureView 容器
     *
     * @return TextureView 容器
     */
    @Override
    protected ViewGroup getTextureViewContainer() {
        return cameraMainFragmentFl;
    }

    /**
     * 获取 TextureView
     *
     * @return TextureView
     */
    @Override
    public AspectRatioTextureView getTextureView() {
        return cameraMainFragmentArtv;
    }

    /**
     * 获取 MultiRoiOverlayView
     *
     * @return MultiRoiOverlayView
     */
    @Override
    public MultiRoiOverlayView getMultiRoiOverlayView() {
        return multiRoiOverlayView;
    }

    /**
     * 初始化组件
     *
     * @param rootView 根视图
     */
    @Override
    protected void initWidget(@NotNull View rootView) {
        cameraMainFragmentFl = rootView.findViewById(R.id.cameraMainFragmentFl);
        cameraMainFragmentArtv = rootView.findViewById(R.id.cameraMainFragmentArtv);
        cameraMainFragmentVfv = rootView.findViewById(R.id.cameraMainFragmentVfv);
        multiRoiOverlayView = rootView.findViewById(R.id.cameraMainFragmentMrov);
        cameraMainFragmentSbSingleCapture = rootView.findViewById(R.id.cameraMainFragmentSbSingleCapture);
        cameraMainFragmentSbSingleCapture.setOnShimmerButtonCallback(new OnShimmerButtonCallback() {
            @Override
            public boolean onShimmerButtonIntercept() {
                boolean enableHandleSingleCaptureButton = cameraMainFragmentSbSingleCapture.isBusy();
                if (enableHandleSingleCaptureButton) {
                    ToastUtils.show(R.string.noClickRepeat);
                }
                return enableHandleSingleCaptureButton;
            }

            @Override
            public void onShimmerButtonStart(ShimmerButtonState currentShimmerButtonState) {
                if (cameraMainFragmentSbBurstCapture.isBusy()) {
                    ToastUtils.show(R.string.burstCaptureRunning);
                    cameraMainFragmentSbSingleCapture.stop();
                    return;
                }
                // 单拍按钮点击事件
                cameraMainFragmentKit.onSingleCaptureClicked();
                /*File mediaDir = MediaStorageConfig.getInstance().getDirectoryFileByStorageType(MediaStorageConfig.StorageType.CAPTURE);
                String fileName = String.format(Locale.CHINA, "IMG_%d_%04d.jpg", CurrentTimeMillisClock.getInstance().now(), 1);
                CameraController.getInstance().captureImage(getCurrentCamera(), new ICaptureCallBack() {
                    @Override
                    public void onBegin() {
                        Log.d(LogKit.TAG, "SDK - onBegin");
                    }

                    @Override
                    public void onError(@Nullable String error) {
                        Log.d(LogKit.TAG, "SDK - onError" + error);
                    }

                    @Override
                    public void onComplete(@Nullable String path) {
                        Log.d(LogKit.TAG, "SDK - onComplete" + path);
                    }
                }, new File(mediaDir, fileName).getAbsolutePath());*/
            }

            @Override
            public void onShimmerButtonChargeCancel() {

            }

            @Override
            public void onShimmerButtonStop() {

            }
        });
        cameraMainFragmentSbBurstCapture = rootView.findViewById(R.id.cameraMainFragmentSbBurstCapture);
        cameraMainFragmentSbBurstCapture.setOnShimmerButtonCallback(new OnShimmerButtonCallback() {
            @Override
            public void onShimmerButtonStart(ShimmerButtonState currentShimmerButtonState) {
                if (cameraMainFragmentSbSingleCapture.isBusy()) {
                    ToastUtils.show(R.string.singleCaptureRunning);
                    cameraMainFragmentSbBurstCapture.stop();
                    return;
                }
                // 连拍按钮点击事件
                cameraMainFragmentKit.onBurstCaptureClicked(3000);
            }

            @Override
            public void onShimmerButtonChargeCancel() {

            }

            @Override
            public void onShimmerButtonStop() {
                // 停止连拍按钮点击事件
                cameraMainFragmentKit.onStopBurstCaptureClicked();
            }
        });
        cameraMainFragmentSbScanCode = rootView.findViewById(R.id.cameraMainFragmentSbScanCode);
        cameraMainFragmentSbScanCode.setOnShimmerButtonCallback(new OnShimmerButtonCallback() {
            @Override
            public void onShimmerButtonStart(ShimmerButtonState currentShimmerButtonState) {
                // 扫码按钮点击事件
                cameraMainFragmentKit.onScanCodeClicked(cameraMainFragmentVfv, 1200);
            }

            @Override
            public void onShimmerButtonChargeCancel() {

            }

            @Override
            public void onShimmerButtonStop() {
                // 停止扫码按钮点击事件
                cameraMainFragmentKit.onStopScanCodeClicked(cameraMainFragmentVfv);
            }
        });
        rootView.findViewById(R.id.cameraMainFragmentMbGallery).setOnClickListener(this);
    }

    /**
     * 初始化数据
     * <p>
     * 子类重写须调 super.initData()
     */
    @Override
    protected void initData() {
        super.initData();
        cameraMainFragmentKit = new CameraMainFragmentKit(this);
    }

    /**
     * 开始逻辑
     */
    @Override
    protected void startLogic() {
        /*String filePath = "/storage/emulated/0/Android/data/com.qtone.camerause/files/Pictures/IMG_1786010873667_0001.jpg";*/
    }

    /**
     * 获取相机分辨率
     *
     * @return 相机分辨率
     */
    @NonNull
    @Override
    protected CameraResolution getCameraResolution() {
        return CameraResolution.RES_1280_720;
    }

    /**
     * 预览帧
     *
     * @param data       图像帧字节数组
     * @param width      帧物理宽
     * @param height     帧物理高
     * @param dataFormat 数据格式
     */
    @Override
    protected void onPreviewFrame(byte[] data, int width, int height, IPreviewDataCallBack.DataFormat dataFormat) {
        cameraMainFragmentKit.processFrame(data, width, height, dataFormat);
    }

    @Override
    public void onClick(@NotNull View v) {
        int id = v.getId();
        if (id == R.id.cameraMainFragmentMbGallery) {
            // 图库按钮点击事件
            cameraMainFragmentKit.onGalleryClicked();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraMainFragmentKit.release();
    }
}