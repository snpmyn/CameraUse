package com.qtone.camerause.widget.recyclerview.configure;

import android.content.Context;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qtone.camerause.util.density.DensityUtils;
import com.qtone.camerause.widget.recyclerview.decoration.GridLayoutSpaceItemDecoration;
import com.qtone.camerause.widget.recyclerview.decoration.LinearLayoutHorizontalSpaceItemDecoration;
import com.qtone.camerause.widget.recyclerview.decoration.LinearLayoutVerticalSpaceItemDecoration;
import com.qtone.camerause.widget.recyclerview.manager.MyGridLayoutManager;
import com.qtone.camerause.widget.recyclerview.manager.MyLinearLayoutManager;

/**
 * Created on 2019/5/22.
 *
 * @author 郑少鹏
 * @desc RecyclerViewConfigure
 */
public class RecyclerViewConfigure {
    /**
     * 上下文
     */
    private final Context context;
    /**
     * 控件
     */
    private final RecyclerView recyclerView;

    /**
     * constructor
     *
     * @param context      控件
     * @param recyclerView 控件
     */
    public RecyclerViewConfigure(Context context, RecyclerView recyclerView) {
        this.context = context;
        this.recyclerView = recyclerView;
    }

    /**
     * 线性水平布局
     *
     * @param needSpace          需间距
     * @param space              间距
     * @param topAndBottomOffset 上下偏移
     * @param hasFixedSize       已固定大小
     */
    public void linearHorizontalLayout(boolean needSpace, int space, boolean topAndBottomOffset, boolean hasFixedSize) {
        // 条目装饰数量
        if (recyclerView.getItemDecorationCount() > 0) {
            recyclerView.removeItemDecorationAt(0);
        }
        // 设置布局管理器
        // false 头至尾 / true 尾至头（默 false）
        recyclerView.setLayoutManager(new MyLinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false, (recycler, state) -> {

        }));
        // 固定 RecyclerView 高（避 RecyclerView 重 measure）
        recyclerView.setHasFixedSize(hasFixedSize);
        if (needSpace) {
            recyclerView.addItemDecoration(new LinearLayoutHorizontalSpaceItemDecoration(DensityUtils.dipToPxByInt(space), topAndBottomOffset));
        }
    }

    /**
     * 线性垂直布局
     *
     * @param needSpace          需间距
     * @param space              间距
     * @param leftAndRightOffset 左右偏移
     * @param hasFixedSize       已固定大小
     */
    public void linearVerticalLayout(boolean needSpace, int space, boolean leftAndRightOffset, boolean hasFixedSize) {
        // 条目装饰数量
        if (recyclerView.getItemDecorationCount() > 0) {
            recyclerView.removeItemDecorationAt(0);
        }
        // 设置布局管理器
        recyclerView.setLayoutManager(new MyLinearLayoutManager(context, (recycler, state) -> {

        }));
        // 固定 RecyclerView 高（避 RecyclerView 重 measure）
        recyclerView.setHasFixedSize(hasFixedSize);
        if (needSpace) {
            recyclerView.addItemDecoration(new LinearLayoutVerticalSpaceItemDecoration(DensityUtils.dipToPxByInt(space), leftAndRightOffset));
        }
    }

    /**
     * 表格布局
     *
     * @param spanCount                      跨距数
     * @param space                          间距
     * @param firstRowHaveTopSpaceDecoration 头行有上间距装饰否
     * @param hasFixedSize                   已固定大小
     */
    public void gridLayout(int spanCount, int space, boolean firstRowHaveTopSpaceDecoration, boolean hasFixedSize) {
        // 条目装饰数量
        if (recyclerView.getItemDecorationCount() > 0) {
            recyclerView.removeItemDecorationAt(0);
        }
        // 设置布局管理器
        recyclerView.setLayoutManager(new MyGridLayoutManager(context, spanCount, (recycler, state) -> {

        }));
        // 固定 RecyclerView 高（避 RecyclerView 重 measure）
        recyclerView.setHasFixedSize(hasFixedSize);
        recyclerView.addItemDecoration(new GridLayoutSpaceItemDecoration(spanCount, DensityUtils.dipToPxByInt(space), firstRowHaveTopSpaceDecoration, true));
    }
}