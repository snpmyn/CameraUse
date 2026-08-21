package com.qtone.camerause.model.gallery.kit;

import android.content.Intent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.qtone.camerause.function.media.MediaScanner;
import com.qtone.camerause.function.storage.MediaStorageConfig;
import com.qtone.camerause.model.gallery.GalleryActivity;
import com.qtone.camerause.model.gallery.GalleryDetailActivity;
import com.qtone.camerause.model.gallery.adapter.MediaImageGroupAdapter;
import com.qtone.camerause.utils.intent.IntentJump;
import com.qtone.camerause.value.IntentConstant;
import com.qtone.camerause.widget.recyclerview.configure.RecyclerViewConfigure;
import com.qtone.camerause.widget.recyclerview.controller.RecyclerViewDisplayController;
import com.qtone.camerause.widget.recyclerview.listener.OnRecyclerViewOnItemClickListener;

/**
 * Created on 2026/8/20.
 *
 * @author 郑少鹏
 * @desc 图库页配套原件
 */
public class GalleryActivityKit {
    /**
     * 执行
     *
     * @param galleryActivity 图库页
     * @param recyclerView    RecyclerView
     * @param spanCount       跨距数
     * @param space           间距
     * @param totalMargin     总外边距
     */
    public void execute(GalleryActivity galleryActivity, RecyclerView recyclerView, int spanCount, int space, int totalMargin) {
        // 控件
        RecyclerViewConfigure recyclerViewConfigure = new RecyclerViewConfigure(galleryActivity, recyclerView);
        recyclerViewConfigure.gridLayout(spanCount, space, true, true);
        // 媒体图片组适配器
        MediaImageGroupAdapter mediaImageGroupAdapter = new MediaImageGroupAdapter(galleryActivity, spanCount, totalMargin);
        mediaImageGroupAdapter.setOnRecyclerViewOnItemClickListener(new OnRecyclerViewOnItemClickListener() {
            @Override
            public <T> void onItemClick(View view, int position, T t) {
                MediaScanner.FolderItem folderItem = (MediaScanner.FolderItem) t;
                Intent intent = new Intent(galleryActivity, GalleryDetailActivity.class);
                intent.putExtra(IntentConstant.GALLERY_ACTIVITY_$_FOLDER_ITEM, folderItem);
                IntentJump.getInstance().jumpWithAnimation(intent, galleryActivity, false, GalleryDetailActivity.class, 0, 0);
            }
        });
        // 媒体扫描器
        MediaScanner mediaScanner = new MediaScanner();
        mediaScanner.scanImageRecursively(MediaStorageConfig.getInstance().getDirectoryFile(), mediaImageGroupAdapter::submitData);
        // 展示
        RecyclerViewDisplayController.display(recyclerView, mediaImageGroupAdapter);
    }
}