package com.qtone.camerause.model.main;

import androidx.viewbinding.ViewBinding;

import com.qtone.camerause.R;
import com.qtone.camerause.base.BasePoolActivity;
import com.qtone.camerause.databinding.ActivityMainBinding;
import com.qtone.camerause.model.main.kit.MainActivityKit;
import com.qtone.camerause.util.materialtoolbar.MaterialToolbarKit;

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
        MaterialToolbarKit.getInstance().setMenuOverflowIconSize(this, activityMainBinding.mainActivityMt, R.dimen.dp_24);
        MaterialToolbarKit.getInstance().setMenuOverflowIconTintColor(this, activityMainBinding.mainActivityMt, R.color.white);
        /*MaterialToolbarKit.getInstance().setMenuOverflowIcon(this, activityMainBinding.mainActivityMt, R.drawable.ic_arrows_more_down_cos_24dp);*/
        MaterialToolbarKit.getInstance().setMenuItemIconMarginRight(this, activityMainBinding.mainActivityMt, R.id.mainActivityMenuDeviceInfo, R.dimen.dp_10);
        MaterialToolbarKit.getInstance().setMenuItemIconMarginRight(this, activityMainBinding.mainActivityMt, R.id.mainActivityMenuSwitchResolution, R.dimen.dp_10);
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
}