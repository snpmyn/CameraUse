package com.qtone.camerause.model.gallery;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.qtone.camerause.R;
import com.qtone.camerause.model.gallery.kit.GalleryActivityKit;

/**
 * @decs: 图库页
 * @author: 郑少鹏
 * @date: 2026/8/20 19:11
 * @version: v 1.0
 */
public class GalleryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        // RecyclerView
        RecyclerView recyclerView = findViewById(R.id.galleryActivityRv);
        // 图库页配套原件
        GalleryActivityKit galleryActivityKit = new GalleryActivityKit();
        galleryActivityKit.execute(this, recyclerView, 3, 12, 48);
    }
}