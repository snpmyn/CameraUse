package com.qtone.camerause.model.gallery;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.qtone.camerause.R;
import com.qtone.camerause.function.media.MediaScanner;
import com.qtone.camerause.model.gallery.kit.GalleryDetailActivityKit;
import com.qtone.camerause.utils.intent.IntentVerify;
import com.qtone.camerause.value.IntentConstant;

/**
 * @decs: 图库详情页
 * @author: 郑少鹏
 * @date: 2026/8/21 13:23
 * @version: v 1.0
 */
public class GalleryDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_detail);
        // RecyclerView
        RecyclerView galleryDetailActivityRv = findViewById(R.id.galleryDetailActivityRv);
        // 文件夹条目
        MediaScanner.FolderItem folderItem = (MediaScanner.FolderItem) IntentVerify.getSerializableExtra(getIntent(), IntentConstant.GALLERY_ACTIVITY_$_FOLDER_ITEM);
        // 图库详情页配套原件
        GalleryDetailActivityKit galleryDetailActivityKit = new GalleryDetailActivityKit();
        galleryDetailActivityKit.execute(this, galleryDetailActivityRv, folderItem, 3, 12, 48);
    }
}