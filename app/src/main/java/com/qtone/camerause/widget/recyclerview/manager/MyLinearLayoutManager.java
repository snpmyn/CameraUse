package com.qtone.camerause.widget.recyclerview.manager;

import android.content.Context;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created on 2019/6/26.
 *
 * @author 郑少鹏
 * @desc MyLinearLayoutManager
 */
public class MyLinearLayoutManager extends LinearLayoutManager {
    /**
     * 布局子控件监听
     */
    private final OnLayoutChildrenListener onLayoutChildrenListener;

    /**
     * Creates a vertical LinearLayoutManager
     *
     * @param context                  Current context, will be used to access resources.
     * @param onLayoutChildrenListener The listener of layout children
     */
    public MyLinearLayoutManager(Context context, OnLayoutChildrenListener onLayoutChildrenListener) {
        super(context);
        this.onLayoutChildrenListener = onLayoutChildrenListener;
    }

    /**
     * @param context                  Current context, will be used to access resources.
     * @param orientation              Layout orientation. Should be {@link #HORIZONTAL} or {@link #VERTICAL}.
     * @param reverseLayout            When set to true, layouts from end to start.
     * @param onLayoutChildrenListener The listener of layout children
     */
    public MyLinearLayoutManager(Context context, int orientation, boolean reverseLayout, OnLayoutChildrenListener onLayoutChildrenListener) {
        super(context, orientation, reverseLayout);
        this.onLayoutChildrenListener = onLayoutChildrenListener;
    }

    /**
     * {@inheritDoc}
     *
     * @param recycler
     * @param state
     */
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