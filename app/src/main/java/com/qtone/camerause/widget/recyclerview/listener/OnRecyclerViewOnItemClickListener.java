package com.qtone.camerause.widget.recyclerview.listener;

import android.view.View;

/**
 * Created on 2021/1/27
 *
 * @author zsp
 * @desc RecyclerView 条目短点监听
 */
public interface OnRecyclerViewOnItemClickListener {
    /**
     * 短点
     *
     * @param view     视图
     * @param position 位置
     * @param t        T
     * @param <T>      <T>
     */
    <T> void onItemClick(View view, int position, T t);
}