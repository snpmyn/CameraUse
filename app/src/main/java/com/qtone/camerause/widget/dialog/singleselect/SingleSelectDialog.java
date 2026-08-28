package com.qtone.camerause.widget.dialog.singleselect;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.qtone.camerause.R;
import com.qtone.camerause.util.list.ListUtils;
import com.qtone.camerause.util.view.ViewUtils;
import com.qtone.camerause.widget.dialog.base.BaseLifecycleDialog;
import com.qtone.camerause.widget.dialog.singleselect.adapter.SingleSelectDialogAdapter;
import com.qtone.camerause.widget.dialog.singleselect.bean.SingleSelectDialogBean;
import com.qtone.camerause.widget.dialog.singleselect.listener.SingleSelectDialogClickListener;
import com.qtone.camerause.widget.recyclerview.configure.RecyclerViewConfigure;
import com.qtone.camerause.widget.recyclerview.controller.RecyclerViewDisplayController;
import com.qtone.camerause.widget.recyclerview.listener.OnRecyclerViewOnItemClickListener;

import java.util.List;

/**
 * Created on 2026/8/26.
 *
 * @author 郑少鹏
 * @desc 单选对话框
 */
public class SingleSelectDialog extends BaseLifecycleDialog {
    /**
     * 控件
     */
    private TextView singleDialogTvTitle;
    private RecyclerView singleDialogRv;
    private Button singleDialogMbNegative;
    private Button singleDialogMbPositive;
    /**
     * 标题
     */
    private String title;
    /**
     * 单选对话框数据集
     */
    private List<SingleSelectDialogBean> singleSelectDialogBeanList;
    /**
     * 默认选中位置
     */
    private int defaultSelectedPosition;
    /**
     * 单选对话框适配器
     */
    private SingleSelectDialogAdapter singleSelectDialogAdapter;
    /**
     * 消极文本
     */
    private String negativeText;
    /**
     * 显示消极
     */
    private boolean showNegative = false;
    /**
     * 积极文本
     */
    private String positiveText;
    /**
     * 单选对话框点击监听
     */
    private SingleSelectDialogClickListener singleSelectDialogClickListener;

    /**
     * constructor
     *
     * @param context 上下文
     */
    public SingleSelectDialog(@NonNull Context context) {
        super(context);
    }

    /**
     * 获取布局 ID
     *
     * @return 布局 ID
     */
    @Override
    protected int getLayoutId() {
        return R.layout.dialog_single_select;
    }

    /**
     * 初始化控件
     */
    @Override
    protected void initView() {
        singleDialogTvTitle = findViewById(R.id.singleDialogTvTitle);
        singleDialogRv = findViewById(R.id.singleDialogRv);
        singleDialogMbNegative = findViewById(R.id.singleDialogMbNegative);
        singleDialogMbPositive = findViewById(R.id.singleDialogMbPositive);
    }

    /**
     * 初始化数据
     */
    @Override
    protected void initData() {
        // 标题
        if (!TextUtils.isEmpty(title)) {
            singleDialogTvTitle.setText(title);
            singleDialogTvTitle.setVisibility(View.VISIBLE);
        }
        // 列表
        if (ListUtils.listIsNotEmpty(singleSelectDialogBeanList)) {
            // 控件
            ViewUtils.showView(singleDialogRv);
            RecyclerViewConfigure recyclerViewConfigure = new RecyclerViewConfigure(getAppCompatActivity(), singleDialogRv);
            recyclerViewConfigure.linearVerticalLayout(true, 12, true, true);
            // 适配器
            singleSelectDialogAdapter = new SingleSelectDialogAdapter(getAppCompatActivity());
            singleSelectDialogAdapter.setData(singleSelectDialogBeanList, defaultSelectedPosition);
            singleSelectDialogAdapter.setOnRecyclerViewOnItemClickListener(new OnRecyclerViewOnItemClickListener() {
                @Override
                public <T> void onItemClick(View view, int position, T t) {

                }
            });
            // 展示
            RecyclerViewDisplayController.display(singleDialogRv, singleSelectDialogAdapter);
        }
        // 消极
        if (showNegative) {
            singleDialogMbNegative.setText(TextUtils.isEmpty(negativeText) ? getContext().getText(R.string.cancel) : negativeText);
            singleDialogMbNegative.setVisibility(View.VISIBLE);
        }
        // 积极
        singleDialogMbPositive.setText(TextUtils.isEmpty(positiveText) ? getContext().getText(R.string.ensure) : positiveText);
    }

    /**
     * 初始化事件
     */
    @Override
    protected void initEvent() {
        singleDialogMbNegative.setOnClickListener(v -> {
            if (singleSelectDialogClickListener != null) {
                singleSelectDialogClickListener.onCancel(this);
            }
        });
        singleDialogMbPositive.setOnClickListener(v -> {
            if (singleSelectDialogClickListener != null) {
                singleSelectDialogClickListener.onConfirm(this, singleSelectDialogAdapter.getCurrentSelectedPosition(), singleSelectDialogAdapter.getCurrentSelectedSingleSelectDialogBean());
            }
        });
    }

    /**
     * 清理资源
     * <p>
     * 对话框从 Window 移除时触发
     */
    @Override
    protected void onClearResource() {
        super.onClearResource();
        // 规避内存泄漏
        // 置空单选对话框点击监听
        singleSelectDialogClickListener = null;
    }

    /**
     * 设置标题
     *
     * @param title 标题
     * @return 单选对话框
     */
    public SingleSelectDialog setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * 设置单选对话框数据集
     *
     * @param singleSelectDialogBeanList 单选对话框数据集
     * @return 单选对话框
     */
    public SingleSelectDialog setSingleSelectDialogBeanList(List<SingleSelectDialogBean> singleSelectDialogBeanList) {
        this.singleSelectDialogBeanList = singleSelectDialogBeanList;
        return this;
    }

    /**
     * 设置默认选中位置
     *
     * @param defaultSelectedPosition 默认选中位置
     */
    public SingleSelectDialog setDefaultSelectedPosition(int defaultSelectedPosition) {
        this.defaultSelectedPosition = defaultSelectedPosition;
        return this;
    }

    /**
     * 设置消极文本
     *
     * @param negativeText 消极文本
     * @return 单选对话框
     */
    public SingleSelectDialog setNegativeText(String negativeText) {
        this.negativeText = negativeText;
        return this;
    }

    /**
     * 设置显示消极
     *
     * @param showNegative 显示消极
     * @return 单选对话框
     */
    public SingleSelectDialog setShowNegative(boolean showNegative) {
        this.showNegative = showNegative;
        return this;
    }

    /**
     * 设置积极文本
     *
     * @param positiveText 积极文本
     * @return 单选对话框
     */
    public SingleSelectDialog setPositiveText(String positiveText) {
        this.positiveText = positiveText;
        return this;
    }

    /**
     * 设置单选对话框点击监听
     *
     * @param singleSelectDialogClickListener 单选对话框点击监听
     * @return 单选对话框
     */
    public SingleSelectDialog setSingleSelectDialogClickListener(SingleSelectDialogClickListener singleSelectDialogClickListener) {
        this.singleSelectDialogClickListener = singleSelectDialogClickListener;
        return this;
    }
}