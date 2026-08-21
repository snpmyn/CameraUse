package com.qtone.camerause.model.gallery.kit;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.qtone.camerause.function.media.MediaScanner;
import com.qtone.camerause.model.gallery.GalleryDetailActivity;
import com.qtone.camerause.model.gallery.adapter.MediaImageDetailAdapter;
import com.qtone.camerause.widget.recyclerview.configure.RecyclerViewConfigure;
import com.qtone.camerause.widget.recyclerview.controller.RecyclerViewDisplayController;
import com.qtone.camerause.widget.recyclerview.listener.OnRecyclerViewOnItemInnerClickListener;

/**
 * Created on 2026/8/21.
 *
 * @author 郑少鹏
 * @desc 图库详情页配套原件
 */
public class GalleryDetailActivityKit {
    /**
     * 执行
     *
     * @param galleryDetailActivity 图库详情页
     * @param recyclerView          RecyclerView
     * @param folderItem            文件夹条目
     * @param spanCount             跨距数
     * @param space                 间距
     * @param totalMargin           总外边距
     */
    public void execute(GalleryDetailActivity galleryDetailActivity, RecyclerView recyclerView, MediaScanner.FolderItem folderItem, int spanCount, int space, int totalMargin) {
        // 控件
        RecyclerViewConfigure recyclerViewConfigure = new RecyclerViewConfigure(galleryDetailActivity, recyclerView);
        recyclerViewConfigure.gridLayout(spanCount, space, true, true);
        // 媒体图片详情适配器
        MediaImageDetailAdapter mediaImageDetailAdapter = new MediaImageDetailAdapter(galleryDetailActivity, spanCount, totalMargin);
        mediaImageDetailAdapter.submitData(folderItem);
        mediaImageDetailAdapter.setOnRecyclerViewOnItemInnerClickListener(new OnRecyclerViewOnItemInnerClickListener() {
            @Override
            public <T> void onItemInnerClick(View view, int position, T t) {

            }
        });
        // 展示
        RecyclerViewDisplayController.display(recyclerView, mediaImageDetailAdapter);
    }
}