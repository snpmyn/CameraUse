package com.qtone.camerause.widget.button;

/**
 * Created on 2026/9/1.
 *
 * @author 郑少鹏
 * @desc 流光按钮回调
 */
public interface OnShimmerButtonCallback {
    /**
     * 流光按钮拦截
     * <p>
     * 默不拦截
     *
     * @return 是否拦截
     */
    default boolean onShimmerButtonIntercept() {
        return false;
    }

    /**
     * 流光按钮开始
     *
     * @param currentShimmerButtonState 当前流光按钮状态
     */
    void onShimmerButtonStart(ShimmerButtonState currentShimmerButtonState);

    /**
     * 流光按钮蓄力中途取消
     */
    void onShimmerButtonChargeCancel();

    /**
     * 流光按钮停止
     */
    void onShimmerButtonStop();
}