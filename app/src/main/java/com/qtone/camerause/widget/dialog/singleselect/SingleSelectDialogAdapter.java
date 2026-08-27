package com.qtone.camerause.widget.dialog.singleselect;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.radiobutton.MaterialRadioButton;
import com.qtone.camerause.R;
import com.qtone.camerause.util.list.ListUtils;
import com.qtone.camerause.widget.recyclerview.listener.OnRecyclerViewOnItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2026/8/26.
 *
 * @author 郑少鹏
 * @desc 单选对话框适配器
 */
public class SingleSelectDialogAdapter extends RecyclerView.Adapter<SingleSelectDialogAdapter.ViewHolder> {
    /**
     * 上下文
     */
    private final Context context;
    /**
     * 单选对话框数据集
     */
    private List<SingleSelectDialogBean> singleSelectDialogBeanList;
    /**
     * 当前选中位置
     */
    private int currentSelectedPosition = -1;
    /**
     * RecyclerView 条目短点监听
     */
    private OnRecyclerViewOnItemClickListener onRecyclerViewOnItemClickListener;

    /**
     * constructor
     *
     * @param context 上下文
     */
    public SingleSelectDialogAdapter(Context context) {
        this.context = context;
    }

    /**
     * 设置 RecyclerView 条目短点监听
     *
     * @param onRecyclerViewOnItemClickListener RecyclerView 条目短点监听
     */
    public void setOnRecyclerViewOnItemClickListener(OnRecyclerViewOnItemClickListener onRecyclerViewOnItemClickListener) {
        this.onRecyclerViewOnItemClickListener = onRecyclerViewOnItemClickListener;
    }

    /**
     * 设置数据
     *
     * @param singleSelectDialogBeanList 单选对话框数据集
     * @param defaultSelectedPosition    默认选中位置
     */
    public void setData(List<SingleSelectDialogBean> singleSelectDialogBeanList, int defaultSelectedPosition) {
        if (this.singleSelectDialogBeanList == null) {
            this.singleSelectDialogBeanList = new ArrayList<>();
        } else {
            this.singleSelectDialogBeanList.clear();
        }
        if (singleSelectDialogBeanList != null) {
            this.singleSelectDialogBeanList.addAll(singleSelectDialogBeanList);
        }
        if ((defaultSelectedPosition >= 0) && (defaultSelectedPosition < this.singleSelectDialogBeanList.size())) {
            // 有效选中位置
            this.currentSelectedPosition = defaultSelectedPosition;
            for (int i = 0; i < this.singleSelectDialogBeanList.size(); i++) {
                this.singleSelectDialogBeanList.get(i).setChecked(i == defaultSelectedPosition);
            }
        } else {
            // 无效选中位置
            this.currentSelectedPosition = -1;
            for (int i = 0; i < this.singleSelectDialogBeanList.size(); i++) {
                if (this.singleSelectDialogBeanList.get(i).isChecked()) {
                    this.currentSelectedPosition = i;
                    break;
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_single_select_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SingleSelectDialogBean singleSelectDialogBean = singleSelectDialogBeanList.get(position);
        holder.singleSelectDialogItemMrb.setChecked(position == currentSelectedPosition);
        holder.singleSelectDialogItemMrb.setText(singleSelectDialogBean.getContent());
        holder.singleSelectDialogItemMrb.setOnClickListener(v -> execute(holder.itemView, holder.getAdapterPosition()));
    }

    /**
     * 执行
     *
     * @param view           视图
     * @param targetPosition 目标位置
     */
    private void execute(View view, int targetPosition) {
        if ((targetPosition == RecyclerView.NO_POSITION) || (targetPosition == currentSelectedPosition)) {
            return;
        }
        int previousPosition = currentSelectedPosition;
        currentSelectedPosition = targetPosition;
        // 刷新
        if ((previousPosition != -1) && (previousPosition < singleSelectDialogBeanList.size())) {
            singleSelectDialogBeanList.get(previousPosition).setChecked(false);
            notifyItemChanged(previousPosition);
        }
        singleSelectDialogBeanList.get(currentSelectedPosition).setChecked(true);
        notifyItemChanged(currentSelectedPosition);
        // 短点
        if (onRecyclerViewOnItemClickListener != null) {
            onRecyclerViewOnItemClickListener.onItemClick(view, currentSelectedPosition, singleSelectDialogBeanList.get(currentSelectedPosition));
        }
    }

    @Override
    public int getItemCount() {
        return ListUtils.listIsNotEmpty(singleSelectDialogBeanList) ? singleSelectDialogBeanList.size() : 0;
    }

    /**
     * 获取当前选中位置
     *
     * @return 当前选中位置
     */
    public int getCurrentSelectedPosition() {
        return currentSelectedPosition;
    }

    /**
     * 获取当前选中单选对话框数据
     *
     * @return 当前选中单选对话框数据
     */
    public SingleSelectDialogBean getCurrentSelectedSingleSelectDialogBean() {
        if (ListUtils.listIsNotEmpty(singleSelectDialogBeanList) && (currentSelectedPosition >= 0) && (currentSelectedPosition < singleSelectDialogBeanList.size())) {
            return singleSelectDialogBeanList.get(currentSelectedPosition);
        }
        return null;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialRadioButton singleSelectDialogItemMrb;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            singleSelectDialogItemMrb = itemView.findViewById(R.id.singleSelectDialogItemMrb);
        }
    }
}