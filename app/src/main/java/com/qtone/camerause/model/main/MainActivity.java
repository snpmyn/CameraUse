package com.qtone.camerause.model.main;

import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import com.jiangdg.ausbc.utils.ToastUtils;
import com.qtone.camerause.base.BasePoolActivity;
import com.qtone.camerause.databinding.ActivityMainBinding;
import com.qtone.camerause.model.main.kit.MainActivityKit;

/**
 * @decs: 主页
 * @author: 郑少鹏
 * @date: 2026/7/28 16:14
 * @version: v 1.0
 */
public class MainActivity extends BasePoolActivity {
    /**
     * ActivityMainBinding
     */
    public ActivityMainBinding activityMainBinding;
    /**
     * 主页配套原件
     */
    private MainActivityKit mainActivityKit;

    /**
     * ViewBinding
     * <p>
     * Java 动态绑定
     * Java 运行时多态
     * Java 动态分派机制
     * <p>
     * 如果子类重写 viewBinding()
     * 那么 onCreate() 中调用时会优先执行子类的方法
     *
     * @return ViewBinding
     */
    @Override
    protected ViewBinding viewBinding() {
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        return activityMainBinding;
    }

    /**
     * 初始控件
     */
    @Override
    protected void stepUi() {

    }

    /**
     * 初始配置
     */
    @Override
    protected void initConfiguration() {
        mainActivityKit = new MainActivityKit(this);
    }

    /**
     * 设置监听
     */
    @Override
    protected void setListener() {
        activityMainBinding.mainActivityMt.setOnMenuItemClickListener(item -> {
            mainActivityKit.menuItemClickToExecute(item);
            return true;
        });
    }

    /**
     * 开始逻辑
     */
    @Override
    protected void startLogic() {
        // 检查并请求权限
        mainActivityKit.checkAndRequestPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MainActivityKit.REQUEST_CAMERA_PERMISSION_CODE) {
            // 校验申请的常规运行时权限是否均被授予
            boolean allGranted = true;
            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
            } else {
                allGranted = false;
            }
            if (allGranted) {
                // 常规运行时权限均被授予 -> 继续检查所有文件管理权限
                mainActivityKit.checkAndRequestPermission();
            } else {
                // 权限申请被拒
                ToastUtils.show("需要相机和存储权限才能正常使用");
            }
        }
    }
}