package com.qtone.camerause.widget.recyclerview.decoration;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created on 2019/3/22.
 *
 * @author 郑少鹏
 * @desc 线性布局垂直条目间距装饰
 * 头条目上间距，其后都下间距。
 */
public class LinearLayoutVerticalSpaceItemDecoration extends RecyclerView.ItemDecoration {
    /**
     * 间距
     */
    private final int space;
    /**
     * 左右偏移
     */
    private final boolean leftAndRightOffset;

    /**
     * constructor
     *
     * @param space              间距
     * @param leftAndRightOffset 左右偏移
     */
    public LinearLayoutVerticalSpaceItemDecoration(int space, boolean leftAndRightOffset) {
        this.space = space;
        this.leftAndRightOffset = leftAndRightOffset;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        outRect.bottom = space;
        if (leftAndRightOffset) {
            outRect.left = space;
            outRect.right = space;
        }
        // Add top margin only for the first item to avoid double space between items.
        if (parent.getChildLayoutPosition(view) == 0) {
            outRect.top = space;
        } else {
            outRect.top = 0;
        }
    }
}