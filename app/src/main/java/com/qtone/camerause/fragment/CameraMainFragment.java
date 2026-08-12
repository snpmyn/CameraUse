package com.qtone.camerause.fragment;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.jiangdg.ausbc.widget.AspectRatioTextureView;
import com.qtone.camerause.R;
import com.qtone.camerause.base.BaseCameraFragment;
import com.qtone.camerause.fragment.kit.CameraMainFragmentKit;
import com.qtone.camerause.function.crop.DocumentCropProcessor;
import com.qtone.camerause.value.CameraResolution;

import org.jetbrains.annotations.NotNull;

/**
 * Created on 2026/8/11.
 *
 * @author 郑少鹏
 * @desc 相机主碎片
 * <p>
 * 继承自 {@link BaseCameraFragment}
 */
public class CameraMainFragment extends BaseCameraFragment implements View.OnClickListener {
    /**
     * 默认分辨率
     */
    private static final CameraResolution DEFAULT_RESOLUTION = CameraResolution.RES_1280_720;
    /**
     * 控件
     */
    private FrameLayout cameraMainFragmentFl;
    private AspectRatioTextureView cameraMainFragmentArtv;
    /*private ViewFinderView scanCodeActivityVfv;*/
    /*private MultiRoiOverlayView multiRoiOverlayView;*/
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
    protected AspectRatioTextureView getTextureView() {
        return cameraMainFragmentArtv;
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
        /*scanCodeActivityVfv = rootView.findViewById(R.id.scanCodeActivityVfv);*/
        /*scanCodeActivityVfv.showScanner();*/
        /*multiRoiOverlayView = rootView.findViewById(R.id.multiRoiOverlayView);*/
        rootView.findViewById(R.id.cameraMainFragmentMbSingleCapture).setOnClickListener(this);
        rootView.findViewById(R.id.cameraMainFragmentMbBurstCapture).setOnClickListener(this);
        rootView.findViewById(R.id.cameraMainFragmentMbStopBurstCapture).setOnClickListener(this);
        rootView.findViewById(R.id.cameraMainFragmentMbSwitchResolution).setOnClickListener(this);
        rootView.findViewById(R.id.scanCodeActivityMbScanCode).setOnClickListener(this);
    }

    /**
     * 初始化数据
     * <p>
     * 子类重写必须调用 super.initData()
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
        String filePath = "/storage/emulated/0/Android/data/com.qtone.camerause/files/Pictures/IMG_1786010873667_0001.jpg";
        //String filePath = "/storage/emulated/0/Android/data/com.qtone.camerause/files/Pictures/IMG_1786086059174_0001.jpg";
        cameraMainFragmentKit.getExamCropProcessor().processAsync(requireActivity(), filePath, new DocumentCropProcessor.OnDocumentCropCallback() {
            @Override
            public void onDocumentCropSuccess(String croppedPath, Bitmap resultBitmap) {

            }

            @Override
            public void onDocumentCropError(String errorMsg) {

            }
        });
    }

    /**
     * 获取相机分辨率
     *
     * @return 相机分辨率
     */
    @NonNull
    @Override
    protected CameraResolution getCustomResolution() {
        return DEFAULT_RESOLUTION;
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
        if (id == R.id.cameraMainFragmentMbSingleCapture) {
            // 单拍按钮点击事件
            cameraMainFragmentKit.onSingleCaptureClicked();
        } else if (id == R.id.cameraMainFragmentMbBurstCapture) {
            // 连拍按钮点击事件
            cameraMainFragmentKit.onBurstCaptureClicked(3000);
        } else if (id == R.id.cameraMainFragmentMbStopBurstCapture) {
            // 停止连拍按钮点击事件
            cameraMainFragmentKit.onStopBurstCaptureClicked();
        } else if (id == R.id.cameraMainFragmentMbSwitchResolution) {
            // 切换分辨率按钮点击事件
            cameraMainFragmentKit.onSwitchResolutionClicked();
        } else if (id == R.id.scanCodeActivityMbScanCode) {
            // 扫码按钮点击事件
            cameraMainFragmentKit.onScanCodeClicked(1200);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraMainFragmentKit.release();
    }
}