package com.qtone.camerause.model.gallery;

import androidx.viewbinding.ViewBinding;

import com.qtone.camerause.base.BasePoolActivity;
import com.qtone.camerause.databinding.ActivityGalleryBinding;
import com.qtone.camerause.model.gallery.kit.GalleryActivityKit;

/**
 * @decs: 图库页
 * @author: 郑少鹏
 * @date: 2026/8/20 19:11
 * @version: v 1.0
 */
public class GalleryActivity extends BasePoolActivity {
    /**
     * ActivityGalleryBinding
     */
    private ActivityGalleryBinding activityGalleryBinding;

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
        activityGalleryBinding = ActivityGalleryBinding.inflate(getLayoutInflater());
        return activityGalleryBinding;
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

    }

    /**
     * 设置监听
     */
    @Override
    protected void setListener() {

    }

    /**
     * 开始逻辑
     */
    @Override
    protected void startLogic() {
        GalleryActivityKit galleryActivityKit = new GalleryActivityKit();
        galleryActivityKit.execute(this, activityGalleryBinding.galleryActivityRv, 3, 12, 48);
    }
}