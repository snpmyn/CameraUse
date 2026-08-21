package com.qtone.camerause.widget.recyclerview.manager;

import android.content.Context;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created on 2017/11/19 0019.
 *
 * @author 郑少鹏
 * @desc MyGridLayoutManager
 */
public class MyGridLayoutManager extends GridLayoutManager {
    /**
     * 布局子控件监听
     */
    private final OnLayoutChildrenListener onLayoutChildrenListener;
    /**
     * 允滑
     */
    private boolean scrollEnable = true;

    /**
     * Creates a vertical GridLayoutManager
     *
     * @param context                  Current context, will be used to access resources.
     * @param spanCount                The number of columns in the grid
     * @param onLayoutChildrenListener The listener of layout children
     */
    public MyGridLayoutManager(Context context, int spanCount, OnLayoutChildrenListener onLayoutChildrenListener) {
        super(context, spanCount);
        this.onLayoutChildrenListener = onLayoutChildrenListener;
    }

    /**
     * 允滑
     *
     * @param flag 允滑
     */
    public void setScrollEnable(boolean flag) {
        this.scrollEnable = flag;
    }

    @Override
    public boolean canScrollVertically() {
        return scrollEnable && super.canScrollVertically();
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);
        if (null != onLayoutChildrenListener) {
            onLayoutChildrenListener.onLayoutChildren(recycler, state);
        }
    }

    /**
     * 布局子控件监听
     */
    public interface OnLayoutChildrenListener {
        /**
         * 布局子控件
         *
         * @param recycler RecyclerView.Recycler
         * @param state    RecyclerView.State
         */
        void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state);
    }
}