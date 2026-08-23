package com.qtone.camerause.model.gallery;

import androidx.viewbinding.ViewBinding;

import com.qtone.camerause.base.BasePoolActivity;
import com.qtone.camerause.databinding.ActivityGalleryDetailBinding;
import com.qtone.camerause.model.gallery.kit.GalleryDetailActivityKit;
import com.qtone.camerause.util.intent.IntentVerify;
import com.qtone.camerause.util.media.MediaScanner;
import com.qtone.camerause.value.IntentConstant;

/**
 * @decs: 图库详情页
 * @author: 郑少鹏
 * @date: 2026/8/21 13:23
 * @version: v 1.0
 */
public class GalleryDetailActivity extends BasePoolActivity {
    /**
     * ActivityGalleryDetailBinding
     */
    private ActivityGalleryDetailBinding activityGalleryDetailBinding;

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
        activityGalleryDetailBinding = ActivityGalleryDetailBinding.inflate(getLayoutInflater());
        return activityGalleryDetailBinding;
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
        // 文件夹条目
        MediaScanner.FolderItem folderItem = (MediaScanner.FolderItem) IntentVerify.getSerializableExtra(getIntent(), IntentConstant.GALLERY_ACTIVITY_$_FOLDER_ITEM);
        // 图库详情页配套原件
        GalleryDetailActivityKit galleryDetailActivityKit = new GalleryDetailActivityKit();
        galleryDetailActivityKit.execute(this, activityGalleryDetailBinding.galleryDetailActivityRv, folderItem, 3, 12, 48);
    }
}