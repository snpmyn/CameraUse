package com.qtone.camerause.widget.recyclerview.listener;

import android.view.View;

/**
 * Created on 2021/1/27
 *
 * @author zsp
 * @desc RecyclerView 条目长点监听
 */
public interface OnRecyclerViewOnItemLongClickListener {
    /**
     * 长点
     *
     * @param view     视图
     * @param position 位置
     * @param t        T
     * @param <T>      <T>
     */
    <T> void onItemLongClick(View view, int position, T t);
}