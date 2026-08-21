package com.qtone.camerause.widget.recyclerview.listener;

import android.view.View;

/**
 * Created on 2025/10/4.
 *
 * @author 郑少鹏
 * @desc RecyclerView 条目内部短点监听
 */
public interface OnRecyclerViewOnItemInnerClickListener {
    /**
     * 内部短点
     *
     * @param view     视图
     * @param position 位置
     * @param t        T
     * @param <T>      <T>
     */
    <T> void onItemInnerClick(View view, int position, T t);
}