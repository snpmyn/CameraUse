package com.qtone.camerause.widget.recyclerview.controller;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created on 2022/4/28
 *
 * @author zsp
 * @desc RecyclerViewAnimatorController
 */
public class RecyclerViewAnimatorController {
    /**
     * 关闭 RecyclerView 动画
     *
     * @param recyclerView RecyclerView
     */
    public static void closeRecyclerViewAnimator(@NonNull RecyclerView recyclerView) {
        DefaultItemAnimator defaultItemAnimator = (DefaultItemAnimator) recyclerView.getItemAnimator();
        if (null != defaultItemAnimator) {
            defaultItemAnimator.setSupportsChangeAnimations(false);
        }
    }
}