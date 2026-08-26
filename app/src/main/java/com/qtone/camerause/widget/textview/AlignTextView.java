package com.qtone.camerause.widget.textview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * Created on 2026/8/26.
 *
 * @author 郑少鹏
 * @desc 对齐 TextView
 * <p>
 * 1. 默认单行 / 多行居中
 * 2. 显式设置 {@link #setCenterLongestLeftRest(boolean)} + 多行折行 -> 最长行居中其余行靠左
 */
public class AlignTextView extends AppCompatTextView {
    /**
     * 最长行居中其余行靠左
     */
    private boolean centerLongestLeftRest = false;

    /**
     * constructor
     *
     * @param context 上下文
     */
    public AlignTextView(@NonNull Context context) {
        this(context, null);
    }

    /**
     * constructor
     *
     * @param context      上下文
     * @param attributeSet 属性集
     */
    public AlignTextView(@NonNull Context context, @Nullable AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    /**
     * constructor
     *
     * @param context           上下文
     * @param attributeSet      属性集
     * @param defStyleAttribute 默认样式属性
     */
    public AlignTextView(@NonNull Context context, @Nullable AttributeSet attributeSet, int defStyleAttribute) {
        super(context, attributeSet, defStyleAttribute);
        setGravity(Gravity.CENTER);
    }

    /**
     * 获取最长行居中其余行靠左
     *
     * @return 最长行居中其余行靠左
     */
    public boolean getCenterLongestLeftRest() {
        return centerLongestLeftRest;
    }

    /**
     * 设置最长行居中其余行靠左
     *
     * @param centerLongestLeftRest 最长行居中其余行靠左
     */
    public void setCenterLongestLeftRest(boolean centerLongestLeftRest) {
        if (this.centerLongestLeftRest != centerLongestLeftRest) {
            this.centerLongestLeftRest = centerLongestLeftRest;
            requestLayout();
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 1. 默认未开启 CENTER_LONGEST_LEFT_REST
        // 不论单行 / 多行
        // 无脑走原生全居中
        if (!centerLongestLeftRest) {
            super.onDraw(canvas);
            return;
        }
        CharSequence charSequence = getText();
        Layout layout = getLayout();
        if ((charSequence == null) || (charSequence.length() == 0) || (layout == null)) {
            super.onDraw(canvas);
            return;
        }
        int lineCount = layout.getLineCount();
        // 2. 开启 CENTER_LONGEST_LEFT_REST
        // 只有单行
        // 无脑走原生全居中
        if (lineCount <= 1) {
            super.onDraw(canvas);
            return;
        }
        // 3. 显式设置 {@link #setCenterLongestLeftRest(boolean)} + 多行折行
        // 触发自定绘制
        TextPaint textPaint = getPaint();
        textPaint.setColor(getCurrentTextColor());
        textPaint.drawableState = getDrawableState();
        // 显式设置居左对齐
        // 防止继承 Gravity.CENTER 导致二次偏移
        textPaint.setTextAlign(Paint.Align.LEFT);
        // 精确计算最长行实际绘制宽度
        float maxLineWidth = 0;
        for (int i = 0; i < lineCount; i++) {
            maxLineWidth = Math.max(maxLineWidth, layout.getLineWidth(i));
        }
        canvas.save();
        // 最长行水平居中时的起始 X 点
        // 其余行以该 X 点为基准左对齐
        int usableWidth = (getWidth() - getPaddingLeft() - getPaddingRight());
        float startX = (getPaddingLeft() + Math.max(0, (usableWidth - maxLineWidth) / 2f));
        // 垂直居中偏移
        int usableHeight = (getHeight() - getPaddingTop() - getPaddingBottom());
        float startY = (getPaddingTop() + Math.max(0, (usableHeight - layout.getHeight()) / 2f));
        canvas.translate(startX, startY);
        // 绘制多行文本
        for (int i = 0; i < lineCount; i++) {
            int lineStart = layout.getLineStart(i);
            // 自动排除末尾换行符 / 空格
            int lineEnd = layout.getLineVisibleEnd(i);
            if (lineEnd > lineStart) {
                canvas.drawText(charSequence, lineStart, lineEnd, 0, layout.getLineBaseline(i), textPaint);
            }
        }
        canvas.restore();
    }
}