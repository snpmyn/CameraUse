package com.qtone.camerause.widget.dialog.singleselect.bean;

/**
 * Created on 2026/8/26.
 *
 * @author 郑少鹏
 * @desc 单选对话框数据
 */
public class SingleSelectDialogBean {
    /**
     * 内容
     */
    private String content;
    /**
     * 是否选中
     */
    private boolean isChecked;

    /**
     * constructor
     *
     * @param content   内容
     * @param isChecked 是否选中
     */
    public SingleSelectDialogBean(String content, boolean isChecked) {
        this.content = content;
        this.isChecked = isChecked;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}