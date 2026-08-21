package com.qtone.camerause.widget.recyclerview.decoration;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created on 2019/3/22.
 *
 * @author 郑少鹏
 * @desc 表格布局条目间距装饰
 */
public class GridLayoutSpaceItemDecoration extends RecyclerView.ItemDecoration {
    private final int spanCount;
    private final int spacing;
    private final boolean firstRowHaveTopSpaceDecoration;
    private final boolean includeEdge;

    public GridLayoutSpaceItemDecoration(int spanCount, int spacing, boolean firstRowHaveTopSpaceDecoration, boolean includeEdge) {
        this.spanCount = spanCount;
        this.spacing = spacing;
        this.firstRowHaveTopSpaceDecoration = firstRowHaveTopSpaceDecoration;
        this.includeEdge = includeEdge;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        // item position
        int position = parent.getChildAdapterPosition(view);
        // item column
        int column = position % spanCount;
        if (includeEdge) {
            // spacing - column * ((1.0F / spanCount) * spacing)
            outRect.left = (spacing - column * spacing / spanCount);
            // (column + 1) * ((1.0F / spanCount) * spacing)
            outRect.right = (column + 1) * spacing / spanCount;
            if (position < spanCount) {
                // top edge
                outRect.top = spacing;
            }
            // item bottom
            outRect.bottom = spacing;
        } else {
            // column * ((1.0F / spanCount) * spacing)
            outRect.left = column * spacing / spanCount;
            // spacing - (column + 1) * ((1.0F / spanCount) * spacing)
            outRect.right = (spacing - (column + 1) * spacing / spanCount);
            if (position >= spanCount) {
                // item top
                outRect.top = spacing;
            }
        }
        if (!firstRowHaveTopSpaceDecoration) {
            if (position < spanCount) {
                // top edge
                outRect.top = 0;
            }
        }
    }
}