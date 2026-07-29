package com.qtone.camerause;

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

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Created on 2026/7/29.
 *
 * @author 郑少鹏
 * @desc 拍照碎片
 */
public class CaptureFragment extends CameraFragment {
    private static final String TAG = CaptureFragment.class.getSimpleName();
    private AspectRatioTextureView aspectRatioTextureView;
    private ViewGroup mContainer;

    @Nullable
    @Override
    protected View getRootView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container) {
        View root = inflater.inflate(R.layout.fragment_capture, container, false);
        aspectRatioTextureView = root.findViewById(R.id.captureFragmentArtv);
        mContainer = root.findViewById(R.id.camera_container);
        return root;
    }

    @Nullable
    @Override
    protected IAspectRatio getCameraView() {
        return aspectRatioTextureView;
    }

    @Nullable
    @Override
    protected ViewGroup getCameraViewContainer() {
        return mContainer;
    }

    @NonNull
    @Override
    protected CameraRequest getCameraRequest() {
        return new CameraRequest.Builder()
                .setPreviewWidth(1280)
                .setPreviewHeight(720)
                .setRenderMode(CameraRequest.RenderMode.OPENGL)
                .setDefaultRotateType(RotateType.ANGLE_0)
                .setAspectRatioShow(true)
                // 纯拍照无需抛出原始预览帧
                .setRawPreviewData(false)
                .create();
    }

    @Override
    public void onCameraState(@NotNull MultiCameraClient.ICamera self, @NotNull State code, @Nullable String msg) {
        if (code == State.OPENED) {
            Log.d(TAG, "拍照相机打开成功");
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
        File mediaDir = (getContext() != null) ? getContext().getExternalFilesDir("Pictures") : null;
        if ((mediaDir != null) && !mediaDir.exists()) {
            mediaDir.mkdirs();
        }
        String savePath = new File(mediaDir, "IMG_" + System.currentTimeMillis() + ".jpg").getAbsolutePath();
        captureImage(new ICaptureCallBack() {
            @Override
            public void onBegin() {
                Log.d(TAG, "开始拍照...");
                ToastUtils.show("开始拍照 ");
            }

            @Override
            public void onError(@Nullable String msg) {
                Log.e(TAG, "拍照错误 || " + msg);
                ToastUtils.show("拍照错误 || " + msg);
            }

            @Override
            public void onComplete(@org.jetbrains.annotations.Nullable String path) {
                Log.d(TAG, "拍照成功 || " + path);
                ToastUtils.show("拍照成功 || " + path);
            }
        }, savePath);
    }
}