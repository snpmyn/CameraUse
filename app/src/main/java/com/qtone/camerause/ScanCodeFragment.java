package com.qtone.camerause;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.base.CameraFragment;
import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.jiangdg.ausbc.camera.bean.CameraRequest;
import com.jiangdg.ausbc.render.env.RotateType;
import com.jiangdg.ausbc.utils.ToastUtils;
import com.jiangdg.ausbc.widget.AspectRatioTextureView;
import com.jiangdg.ausbc.widget.IAspectRatio;

import org.jetbrains.annotations.NotNull;

/**
 * @decs: 扫码碎片
 * @author: 郑少鹏
 * @date: 2026/7/28 16:18
 * @version: v 1.0
 */
public class ScanCodeFragment extends CameraFragment implements IPreviewDataCallBack {
    private static final String TAG = ScanCodeFragment.class.getSimpleName();
    private AspectRatioTextureView mTextureView;
    private ViewGroup mContainer;
    private UsbScanManager mScanManager;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 初始化 MLKit 扫码分析器
        mScanManager = new UsbScanManager(new UsbScanManager.OnScanResultListener() {
            @Override
            public void onSuccess(String result, Barcode barcode) {
                Log.d(TAG, "扫码成功 || " + result);
                ToastUtils.show("扫码成功 || " + result);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "扫码错误 || ", e);
            }
        });
    }

    @Nullable
    @Override
    protected View getRootView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container) {
        View root = inflater.inflate(R.layout.fragment_scan_code, container, false);
        mTextureView = root.findViewById(R.id.scanCodeFragmentArtv);
        mContainer = root.findViewById(R.id.camera_container);
        return root;
    }

    @Nullable
    @Override
    protected IAspectRatio getCameraView() {
        // 提供相机渲染组件
        return mTextureView;
    }

    @Nullable
    @Override
    protected ViewGroup getCameraViewContainer() {
        // 提供相机渲染容器
        return mContainer;
    }

    @NonNull
    @Override
    protected CameraRequest getCameraRequest() {
        // 配置相机参数
        return new CameraRequest.Builder()
                .setPreviewWidth(1280)
                .setPreviewHeight(720)
                // 若仅需扫码且无滤镜需求则设为 CameraRequest.RenderMode.NORMAL
                // 可直接输出 NV21 数据，效率比 OPENGL (RGBA) 更高。
                .setRenderMode(CameraRequest.RenderMode.OPENGL)
                .setDefaultRotateType(RotateType.ANGLE_0)
                .setAspectRatioShow(true)
                // 必须开启以抛出原始 preview 帧
                .setRawPreviewData(true)
                .create();
    }

    @Override
    public void onCameraState(@NotNull MultiCameraClient.ICamera self, @NotNull State code, @Nullable String msg) {
        // 监听相机打开状态
        if (code == State.OPENED) {
            Log.d(TAG, "相机打开成功，注册预览帧回调。");
            // 注册预览帧回调
            self.addPreviewDataCallBack(this);
        } else if (code == State.ERROR) {
            Log.e(TAG, "相机打开错误 || " + msg);
        }
    }

    @Override
    public void onPreviewData(@Nullable byte[] data, int width, int height, @NotNull DataFormat format) {
        // 实时预览数据抛出回调
        if ((data != null) && (mScanManager != null)) {
            // 将回调中的 format 准确透传给 UsbScanManager
            mScanManager.processFrame(data, width, height, format, 0);
        }
    }

    @Override
    public void onDestroyView() {
        // 解绑帧回调
        // 防止内存泄漏和后台无效解析
        if (getCurrentCamera() != null) {
            getCurrentCamera().removePreviewDataCallBack(this);
        }
        if (mScanManager != null) {
            mScanManager.release();
            mScanManager = null;
        }
        super.onDestroyView();
    }
}