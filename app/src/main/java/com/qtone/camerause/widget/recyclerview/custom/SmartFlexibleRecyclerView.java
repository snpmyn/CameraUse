package com.qtone.camerause.widget.recyclerview.custom;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @decs: 智能灵活 RecyclerView
 * @author: 郑少鹏
 * @date: 2025/10/12 16:36
 * @version: v 1.0
 */
public class SmartFlexibleRecyclerView extends RecyclerView {
    private GridLayoutManager gridLayoutManager;
    private int spanCount = 2;
    private int spacingPx = dpToPx(8);
    private float cornerRadiusPx = dpToPx(12);
    private ItemDecoration spacingDecoration;
    private ItemTouchHelper itemTouchHelper;

    public SmartFlexibleRecyclerView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public SmartFlexibleRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SmartFlexibleRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        gridLayoutManager = new GridLayoutManager(context, spanCount);
        setLayoutManager(gridLayoutManager);
        setHasFixedSize(true);
        setItemAnimator(new FadeInUpItemAnimator());
    }

    private int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    public void setSpanCount(int spanCount) {
        this.spanCount = spanCount;
        gridLayoutManager.setSpanCount(spanCount);
    }

    public void setSpacing(int dp) {
        this.spacingPx = dpToPx(dp);
    }

    public void setCornerRadius(float dp) {
        this.cornerRadiusPx = dpToPx(dp);
    }

    @Override
    public void setAdapter(@Nullable Adapter adapter) {
        super.setAdapter(adapter);
        if (adapter == null) {
            return;
        }
        // 移除旧装饰器
        // 避免重复累加
        if (spacingDecoration != null) {
            removeItemDecoration(spacingDecoration);
        }
        // 移除旧 ItemTouchHelper
        // 避免重复附加
        if (itemTouchHelper != null) {
            itemTouchHelper.attachToRecyclerView(null);
        }
        // 自动最后一项占满整行
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                Adapter<?> a = getAdapter();
                if (null == a) {
                    return 1;
                }
                int itemCount = a.getItemCount();
                boolean isLastOdd = (position == (itemCount - 1)) && (itemCount % spanCount != 0);
                return (isLastOdd ? spanCount : 1);
            }
        });
        // 创建新间距装饰器
        spacingDecoration = new ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull State state) {
                int position = parent.getChildAdapterPosition(view);
                if (position == RecyclerView.NO_POSITION) {
                    return;
                }
                int spanSize = 1;
                if (getAdapter() != null) {
                    assert getLayoutManager() != null;
                    spanSize = ((GridLayoutManager) getLayoutManager()).getSpanSizeLookup().getSpanSize(position);
                }
                if (spanSize == spanCount) {
                    outRect.left = spacingPx;
                    outRect.right = spacingPx;
                } else {
                    int column = (position % spanCount);
                    outRect.left = (spacingPx - column * spacingPx / spanCount);
                    outRect.right = ((column + 1) * spacingPx / spanCount);
                }
                if (position < spanCount) {
                    outRect.top = spacingPx;
                }
                outRect.bottom = spacingPx;
                if (view instanceof CardView) {
                    ((CardView) view).setRadius(cornerRadiusPx);
                }
            }
        };
        // 添加新间距装饰器
        addItemDecoration(spacingDecoration);
        // 初始化拖动排序
        initDrag(adapter);
    }

    @SuppressWarnings("rawtypes")
    private void initDrag(@Nullable Adapter adapter) {
        if (null == adapter) {
            return;
        }
        // 禁止滑动删除
        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull ViewHolder viewHolder) {
                int dragFlags = (ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
                return makeMovementFlags(dragFlags, 0);
            }

            @SuppressWarnings("rawtypes")
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull ViewHolder viewHolder, @NonNull ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();
                Adapter currentAdapter = getAdapter();
                if (currentAdapter instanceof DraggableAdapter) {
                    ((DraggableAdapter) currentAdapter).swapData(from, to);
                    currentAdapter.notifyItemMoved(from, to);
                }
                return true;
            }

            @Override
            public void onSwiped(@NonNull ViewHolder viewHolder, int direction) {
                // 禁止滑动删除
            }

            @Override
            public boolean isLongPressDragEnabled() {
                // 启用长按拖动功能
                // 注意：会拦截长按事件，或与其它长按手势冲突。
                return true;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                // 明确禁用侧滑删除功能
                return false;
            }
        });
        itemTouchHelper.attachToRecyclerView(this);
    }

    public interface DraggableAdapter {
        void swapData(int fromPosition, int toPosition);
    }

    /**
     * 淡入淡出条目动画
     */
    public static class FadeInUpItemAnimator extends DefaultItemAnimator {
        @Override
        public boolean animateAdd(@NonNull ViewHolder holder) {
            Animation fadeIn = new AlphaAnimation(0.0F, 1.0F);
            fadeIn.setDuration(300);
            fadeIn.setInterpolator(new DecelerateInterpolator());
            Animation slideUp = new TranslateAnimation(0, 0, 50, 0);
            slideUp.setDuration(300);
            slideUp.setInterpolator(new DecelerateInterpolator());
            holder.itemView.startAnimation(fadeIn);
            holder.itemView.startAnimation(slideUp);
            return super.animateAdd(holder);
        }
    }
}