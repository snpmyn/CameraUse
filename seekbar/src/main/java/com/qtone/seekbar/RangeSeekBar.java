package com.qtone.seekbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static com.qtone.seekbar.SeekBar.INDICATOR_ALWAYS_HIDE;
import static com.qtone.seekbar.SeekBar.INDICATOR_ALWAYS_SHOW;

/**
 * @decs: 范围 SeekBar
 * @author: 郑少鹏
 * @date: 2026/9/2 11:55
 * @version: v 1.0
 */
public class RangeSeekBar extends View {
    // 单滑块模式
    public final static int SEEKBAR_MODE_SINGLE = 1;
    // 双滑块范围模式
    public final static int SEEKBAR_MODE_RANGE = 2;
    // 刻度模式：按数字实际比例排列
    public final static int TRICK_MARK_MODE_NUMBER = 0;
    // 刻度模式：均分排列
    public final static int TRICK_MARK_MODE_OTHER = 1;
    // 刻度对齐方向
    public final static int TICK_MARK_GRAVITY_LEFT = 0;
    public final static int TICK_MARK_GRAVITY_CENTER = 1;
    public final static int TICK_MARK_GRAVITY_RIGHT = 2;
    // 解决滑动冲突新增变量
    private final int touchSlop;
    float touchDownX, touchDownY;
    // 剩余最小间隔的进度百分比
    float reservePercent;
    boolean isScaleThumb = false;
    Paint paint = new Paint();
    RectF progressDefaultDstRect = new RectF();
    RectF progressDstRect = new RectF();
    Rect progressSrcRect = new Rect();
    RectF stepDivRect = new RectF();
    Rect tickMarkTextRect = new Rect();
    SeekBar leftSB;
    SeekBar rightSB;
    SeekBar currTouchSB;
    Bitmap progressBitmap;
    Bitmap progressDefaultBitmap;
    List<Bitmap> stepsBitmaps = new ArrayList<>();
    private int progressTop, progressBottom, progressLeft, progressRight;
    private int seekBarMode;
    // 刻度模式
    private int tickMarkMode;
    // 刻度与进度条间的间距
    private int tickMarkTextMargin;
    // 刻度文字大小
    private int tickMarkTextSize;
    private int tickMarkGravity;
    private int tickMarkLayoutGravity;
    private int tickMarkTextColor;
    private int tickMarkInRangeTextColor;
    // 刻度上显示的文字数组
    private CharSequence[] tickMarkTextArray;
    // 进度条圆角半径
    private float progressRadius;
    // 选中的进度条颜色
    private int progressColor;
    // 默认背景进度条颜色
    private int progressDefaultColor;
    // 选中的进度条 Drawable 资源
    private int progressDrawableId;
    // 默认背景进度条 Drawable 资源
    private int progressDefaultDrawableId;
    // 进度条高度
    private int progressHeight;
    // 进度条实际有效宽度
    private int progressWidth;
    // 左右滑块的最小间隔范围
    private float minInterval;
    private int gravity;
    // 是否开启两个滑块重叠
    private boolean enableThumbOverlap;
    // Step 分隔点颜色
    private int stepsColor;
    // Step 分隔点宽度
    private float stepsWidth;
    // Step 分隔点高度
    private float stepsHeight;
    // Step 分隔点圆角
    private float stepsRadius;
    // Step 节点数量
    // 为 0 时禁用 Step 功能
    private int steps;
    // 释放手指后滑块是否自动吸附到最近的 Step 点
    private boolean stepsAutoBonding;
    private int stepsDrawableId;
    // 用户设置的最大最小值
    private float minProgress, maxProgress;
    private boolean isEnable = true;
    private int progressPaddingRight;
    private OnRangeChangedListener callback;
    // 是否锁定为横向拖拽滑动状态
    private boolean isDragging;

    /**
     * constructor
     *
     * @param context 上下文
     */
    public RangeSeekBar(Context context) {
        this(context, null);
    }

    /**
     * constructor
     *
     * @param context 上下文
     * @param attrs   属性集
     */
    public RangeSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 获取系统触发移动的最小物理距离
        touchSlop = android.view.ViewConfiguration.get(context).getScaledTouchSlop();
        initAttrs(attrs);
        initPaint();
        initSeekBar(attrs);
        initStepsBitmap();
    }

    /**
     * 初始化进度条位图资源
     */
    private void initProgressBitmap() {
        if (progressBitmap == null) {
            progressBitmap = Utils.drawableToBitmap(getContext(), progressWidth, progressHeight, progressDrawableId);
        }
        if (progressDefaultBitmap == null) {
            progressDefaultBitmap = Utils.drawableToBitmap(getContext(), progressWidth, progressHeight, progressDefaultDrawableId);
        }
    }

    /**
     * 校验 Step 模式参数是否合法
     */
    private boolean verifyStepsMode() {
        return ((steps >= 1) && !(stepsHeight <= 0) && !(stepsWidth <= 0));
    }

    /**
     * 初始化 Step 节点图片列表
     */
    private void initStepsBitmap() {
        if (!verifyStepsMode() || (stepsDrawableId == 0)) {
            return;
        }
        if (stepsBitmaps.isEmpty()) {
            Bitmap bitmap = Utils.drawableToBitmap(getContext(), (int) stepsWidth, (int) stepsHeight, stepsDrawableId);
            for (int i = 0; i <= steps; i++) {
                stepsBitmaps.add(bitmap);
            }
        }
    }

    /**
     * 初始化左右滑块对象
     *
     * @param attrs 属性集
     */
    private void initSeekBar(AttributeSet attrs) {
        leftSB = new SeekBar(this, attrs, true);
        rightSB = new SeekBar(this, attrs, false);
        rightSB.setVisible(seekBarMode != SEEKBAR_MODE_SINGLE);
    }

    /**
     * 解析 XML 属性
     *
     * @param attrs 属性集
     */
    private void initAttrs(AttributeSet attrs) {
        try {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.RangeSeekBar);
            seekBarMode = typedArray.getInt(R.styleable.RangeSeekBar_rsb_mode, SEEKBAR_MODE_RANGE);
            minProgress = typedArray.getFloat(R.styleable.RangeSeekBar_rsb_min, 0);
            maxProgress = typedArray.getFloat(R.styleable.RangeSeekBar_rsb_max, 100);
            minInterval = typedArray.getFloat(R.styleable.RangeSeekBar_rsb_min_interval, 0);
            gravity = typedArray.getInt(R.styleable.RangeSeekBar_rsb_gravity, Gravity.TOP);
            progressColor = typedArray.getColor(R.styleable.RangeSeekBar_rsb_progress_color, 0xFF4BD962);
            progressRadius = (int) typedArray.getDimension(R.styleable.RangeSeekBar_rsb_progress_radius, -1);
            progressDefaultColor = typedArray.getColor(R.styleable.RangeSeekBar_rsb_progress_default_color, 0xFFD7D7D7);
            progressDrawableId = typedArray.getResourceId(R.styleable.RangeSeekBar_rsb_progress_drawable, 0);
            progressDefaultDrawableId = typedArray.getResourceId(R.styleable.RangeSeekBar_rsb_progress_drawable_default, 0);
            progressHeight = (int) typedArray.getDimension(R.styleable.RangeSeekBar_rsb_progress_height, Utils.dp2px(getContext(), 2));
            tickMarkMode = typedArray.getInt(R.styleable.RangeSeekBar_rsb_tick_mark_mode, TRICK_MARK_MODE_NUMBER);
            tickMarkGravity = typedArray.getInt(R.styleable.RangeSeekBar_rsb_tick_mark_gravity, TICK_MARK_GRAVITY_CENTER);
            tickMarkLayoutGravity = typedArray.getInt(R.styleable.RangeSeekBar_rsb_tick_mark_layout_gravity, Gravity.TOP);
            tickMarkTextArray = typedArray.getTextArray(R.styleable.RangeSeekBar_rsb_tick_mark_text_array);
            tickMarkTextMargin = (int) typedArray.getDimension(R.styleable.RangeSeekBar_rsb_tick_mark_text_margin, Utils.dp2px(getContext(), 7));
            tickMarkTextSize = (int) typedArray.getDimension(R.styleable.RangeSeekBar_rsb_tick_mark_text_size, Utils.dp2px(getContext(), 12));
            tickMarkTextColor = typedArray.getColor(R.styleable.RangeSeekBar_rsb_tick_mark_text_color, progressDefaultColor);
            tickMarkInRangeTextColor = typedArray.getColor(R.styleable.RangeSeekBar_rsb_tick_mark_in_range_text_color, progressColor);
            steps = typedArray.getInt(R.styleable.RangeSeekBar_rsb_steps, 0);
            stepsColor = typedArray.getColor(R.styleable.RangeSeekBar_rsb_step_color, 0xFF9d9d9d);
            stepsRadius = typedArray.getDimension(R.styleable.RangeSeekBar_rsb_step_radius, 0);
            stepsWidth = typedArray.getDimension(R.styleable.RangeSeekBar_rsb_step_width, 0);
            stepsHeight = typedArray.getDimension(R.styleable.RangeSeekBar_rsb_step_height, 0);
            stepsDrawableId = typedArray.getResourceId(R.styleable.RangeSeekBar_rsb_step_drawable, 0);
            stepsAutoBonding = typedArray.getBoolean(R.styleable.RangeSeekBar_rsb_step_auto_bonding, true);
            typedArray.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 计算并测量进度条及滑块的具体坐标范围
     *
     * @param w 宽
     * @param h 高
     */
    protected void onMeasureProgress(int w, int h) {
        int viewHeight = (h - getPaddingBottom() - getPaddingTop());
        if (h <= 0) {
            return;
        }
        if (gravity == Gravity.TOP) {
            float maxIndicatorHeight = 0;
            if ((leftSB.getIndicatorShowMode() != INDICATOR_ALWAYS_HIDE) || (rightSB.getIndicatorShowMode() != INDICATOR_ALWAYS_HIDE)) {
                maxIndicatorHeight = Math.max(leftSB.getIndicatorRawHeight(), rightSB.getIndicatorRawHeight());
            }
            float thumbHeight = Math.max(leftSB.getThumbScaleHeight(), rightSB.getThumbScaleHeight());
            thumbHeight -= progressHeight / 2f;
            progressTop = (int) (maxIndicatorHeight + (thumbHeight - progressHeight) / 2f);
            if ((tickMarkTextArray != null) && (tickMarkLayoutGravity == Gravity.TOP)) {
                progressTop = (int) Math.max(getTickMarkRawHeight(), maxIndicatorHeight + (thumbHeight - progressHeight) / 2f);
            }
            progressBottom = (progressTop + progressHeight);
        } else if (gravity == Gravity.BOTTOM) {
            if ((tickMarkTextArray != null) && (tickMarkLayoutGravity == Gravity.BOTTOM)) {
                progressBottom = (viewHeight - getTickMarkRawHeight());
            } else {
                progressBottom = (int) (viewHeight - Math.max(leftSB.getThumbScaleHeight(), rightSB.getThumbScaleHeight()) / 2f + progressHeight / 2f);
            }
            progressTop = (progressBottom - progressHeight);
        } else {
            progressTop = (viewHeight - progressHeight) / 2;
            progressBottom = (progressTop + progressHeight);
        }
        int maxThumbWidth = (int) Math.max(leftSB.getThumbScaleWidth(), rightSB.getThumbScaleWidth());
        progressLeft = (maxThumbWidth / 2 + getPaddingLeft());
        progressRight = (w - maxThumbWidth / 2 - getPaddingRight());
        progressWidth = (progressRight - progressLeft);
        progressDefaultDstRect.set(getProgressLeft(), getProgressTop(), getProgressRight(), getProgressBottom());
        progressPaddingRight = (w - progressRight);

        if (progressRadius <= 0) {
            progressRadius = (int) ((getProgressBottom() - getProgressTop()) * 0.45f);
        }
        initProgressBitmap();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.EXACTLY) {
            heightSize = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        } else if ((heightMode == MeasureSpec.AT_MOST) && getParent() instanceof ViewGroup && (heightSize == ViewGroup.LayoutParams.MATCH_PARENT)) {
            heightSize = MeasureSpec.makeMeasureSpec(((ViewGroup) getParent()).getMeasuredHeight(), MeasureSpec.AT_MOST);
        } else {
            int heightNeeded;
            if (gravity == Gravity.CENTER) {
                if ((tickMarkTextArray != null) && (tickMarkLayoutGravity == Gravity.BOTTOM)) {
                    heightNeeded = (int) (2 * (getRawHeight() - getTickMarkRawHeight()));
                } else {
                    heightNeeded = (int) (2 * (getRawHeight() - Math.max(leftSB.getThumbScaleHeight(), rightSB.getThumbScaleHeight()) / 2));
                }
            } else {
                heightNeeded = (int) getRawHeight();
            }
            heightSize = MeasureSpec.makeMeasureSpec(heightNeeded, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightSize);
    }

    /**
     * 获取刻度文字占据的物理高度
     */
    protected int getTickMarkRawHeight() {
        if ((tickMarkTextArray != null) && (tickMarkTextArray.length > 0)) {
            return (tickMarkTextMargin + Utils.measureText(String.valueOf(tickMarkTextArray[0]), tickMarkTextSize).height() + 3);
        }
        return 0;
    }

    /**
     * 获取控件整体需要的未裁剪原始高度
     */
    protected float getRawHeight() {
        float rawHeight;
        if (seekBarMode == SEEKBAR_MODE_SINGLE) {
            rawHeight = leftSB.getRawHeight();
            if (tickMarkLayoutGravity == Gravity.BOTTOM && tickMarkTextArray != null) {
                float h = Math.max((leftSB.getThumbScaleHeight() - progressHeight) / 2, getTickMarkRawHeight());
                rawHeight = rawHeight - leftSB.getThumbScaleHeight() / 2 + progressHeight / 2f + h;
            }
        } else {
            rawHeight = Math.max(leftSB.getRawHeight(), rightSB.getRawHeight());
            if (tickMarkLayoutGravity == Gravity.BOTTOM && tickMarkTextArray != null) {
                float thumbHeight = Math.max(leftSB.getThumbScaleHeight(), rightSB.getThumbScaleHeight());
                float h = Math.max((thumbHeight - progressHeight) / 2, getTickMarkRawHeight());
                rawHeight = rawHeight - thumbHeight / 2 + progressHeight / 2f + h;
            }
        }
        return rawHeight;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        onMeasureProgress(w, h);
        // set default value
        setRange(minProgress, maxProgress, minInterval);
        // initializes the positions of the two thumbs
        int lineCenterY = (getProgressBottom() + getProgressTop()) / 2;
        leftSB.onSizeChanged(getProgressLeft(), lineCenterY);
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.onSizeChanged(getProgressLeft(), lineCenterY);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        onDrawTickMark(canvas, paint);
        onDrawProgressBar(canvas, paint);
        onDrawSteps(canvas, paint);
        onDrawSeekBar(canvas);
    }

    /**
     * 绘制刻度文本
     * <p>
     * 绘制刻度并且根据当前位置是否在刻度范围内设置不同的颜色显示
     *
     * @param canvas 画布
     * @param paint  画笔
     */
    protected void onDrawTickMark(Canvas canvas, Paint paint) {
        if (tickMarkTextArray != null) {
            int trickPartWidth = progressWidth / (tickMarkTextArray.length - 1);
            for (int i = 0; i < tickMarkTextArray.length; i++) {
                final String text2Draw = tickMarkTextArray[i].toString();
                if (TextUtils.isEmpty(text2Draw)) continue;
                paint.getTextBounds(text2Draw, 0, text2Draw.length(), tickMarkTextRect);
                paint.setColor(tickMarkTextColor);
                float x;
                if (tickMarkMode == TRICK_MARK_MODE_OTHER) {
                    if (tickMarkGravity == TICK_MARK_GRAVITY_RIGHT) {
                        x = getProgressLeft() + i * trickPartWidth - tickMarkTextRect.width();
                    } else if (tickMarkGravity == TICK_MARK_GRAVITY_CENTER) {
                        x = getProgressLeft() + i * trickPartWidth - tickMarkTextRect.width() / 2f;
                    } else {
                        x = getProgressLeft() + i * trickPartWidth;
                    }
                } else {
                    float num = Utils.parseFloat(text2Draw);
                    SeekBarState[] states = getRangeSeekBarState();
                    if (Utils.compareFloat(num, states[0].value) != -1 && Utils.compareFloat(num, states[1].value) != 1 && (seekBarMode == SEEKBAR_MODE_RANGE)) {
                        paint.setColor(tickMarkInRangeTextColor);
                    }
                    x = getProgressLeft() + progressWidth * (num - minProgress) / (maxProgress - minProgress) - tickMarkTextRect.width() / 2f;
                }
                float y;
                if (tickMarkLayoutGravity == Gravity.TOP) {
                    y = getProgressTop() - tickMarkTextMargin;
                } else {
                    y = getProgressBottom() + tickMarkTextMargin + tickMarkTextRect.height();
                }
                canvas.drawText(text2Draw, x, y, paint);
            }
        }
    }

    /**
     * 绘制进度条
     * <p>
     * 背景与选中高亮区域
     *
     * @param canvas 画布
     * @param paint  画笔
     */
    protected void onDrawProgressBar(Canvas canvas, Paint paint) {
        // 1. 绘制背景默认进度条
        if (Utils.verifyBitmap(progressDefaultBitmap)) {
            canvas.drawBitmap(progressDefaultBitmap, null, progressDefaultDstRect, paint);
        } else {
            paint.setColor(progressDefaultColor);
            canvas.drawRoundRect(progressDefaultDstRect, progressRadius, progressRadius, paint);
        }
        // 2. 算高亮区域范围
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            progressDstRect.top = getProgressTop();
            progressDstRect.left = leftSB.left + leftSB.getThumbScaleWidth() / 2f + progressWidth * leftSB.currPercent;
            progressDstRect.right = rightSB.left + rightSB.getThumbScaleWidth() / 2f + progressWidth * rightSB.currPercent;
            progressDstRect.bottom = getProgressBottom();
        } else {
            progressDstRect.top = getProgressTop();
            progressDstRect.left = leftSB.left + leftSB.getThumbScaleWidth() / 2f;
            progressDstRect.right = leftSB.left + leftSB.getThumbScaleWidth() / 2f + progressWidth * leftSB.currPercent;
            progressDstRect.bottom = getProgressBottom();
        }
        // 3. 绘制高亮选中进度
        if (Utils.verifyBitmap(progressBitmap)) {
            progressSrcRect.top = 0;
            progressSrcRect.bottom = progressBitmap.getHeight();
            int bitmapWidth = progressBitmap.getWidth();
            if (seekBarMode == SEEKBAR_MODE_RANGE) {
                progressSrcRect.left = (int) (bitmapWidth * leftSB.currPercent);
                progressSrcRect.right = (int) (bitmapWidth * rightSB.currPercent);
            } else {
                progressSrcRect.left = 0;
                progressSrcRect.right = (int) (bitmapWidth * leftSB.currPercent);
            }
            canvas.drawBitmap(progressBitmap, progressSrcRect, progressDstRect, null);
        } else {
            paint.setColor(progressColor);
            canvas.drawRoundRect(progressDstRect, progressRadius, progressRadius, paint);
        }
    }

    /**
     * 绘制 Step 刻度节点
     *
     * @param canvas 画布
     * @param paint  画笔
     */
    protected void onDrawSteps(Canvas canvas, Paint paint) {
        if (!verifyStepsMode()) {
            return;
        }
        int stepMarks = getProgressWidth() / (steps);
        float extHeight = (stepsHeight - getProgressHeight()) / 2f;
        for (int k = 0; k <= steps; k++) {
            float x = getProgressLeft() + k * stepMarks - stepsWidth / 2f;
            stepDivRect.set(x, getProgressTop() - extHeight, x + stepsWidth, getProgressBottom() + extHeight);
            if (stepsBitmaps.isEmpty() || stepsBitmaps.size() <= k) {
                paint.setColor(stepsColor);
                canvas.drawRoundRect(stepDivRect, stepsRadius, stepsRadius, paint);
            } else {
                canvas.drawBitmap(stepsBitmaps.get(k), null, stepDivRect, paint);
            }
        }
    }

    /**
     * 绘制滑块与气泡 Indicator
     *
     * @param canvas 画布
     */
    protected void onDrawSeekBar(Canvas canvas) {
        if (leftSB.getIndicatorShowMode() == INDICATOR_ALWAYS_SHOW) {
            leftSB.setShowIndicatorEnable(true);
        }
        leftSB.draw(canvas);
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            if (rightSB.getIndicatorShowMode() == INDICATOR_ALWAYS_SHOW) {
                rightSB.setShowIndicatorEnable(true);
            }
            rightSB.draw(canvas);
        }
    }

    /**
     * 初始化画笔
     */
    private void initPaint() {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(progressDefaultColor);
        paint.setTextSize(tickMarkTextSize);
    }

    /**
     * 改变当前激活滑块的选中状态
     */
    private void changeThumbActivateState(boolean hasActivate) {
        if (hasActivate && currTouchSB != null) {
            boolean state = currTouchSB == leftSB;
            leftSB.setActivate(state);
            if (seekBarMode == SEEKBAR_MODE_RANGE)
                rightSB.setActivate(!state);
        } else {
            leftSB.setActivate(false);
            if (seekBarMode == SEEKBAR_MODE_RANGE)
                rightSB.setActivate(false);
        }
    }

    protected float getEventX(MotionEvent event) {
        return event.getX();
    }

    protected float getEventY(MotionEvent event) {
        return event.getY();
    }

    /**
     * 放大被触碰的滑块
     */
    private void scaleCurrentSeekBarThumb() {
        if (currTouchSB != null && currTouchSB.getThumbScaleRatio() > 1f && !isScaleThumb) {
            isScaleThumb = true;
            currTouchSB.scaleThumb();
        }
    }

    /**
     * 重置并还原滑块大小
     */
    private void resetCurrentSeekBarThumb() {
        if (currTouchSB != null && currTouchSB.getThumbScaleRatio() > 1f && isScaleThumb) {
            isScaleThumb = false;
            currTouchSB.resetThumb();
        }
    }

    /**
     * 根据触摸点 X 坐标计算对应进度百分比 (0.0 ~ 1.0)
     *
     * @param touchDownX 触摸点 X 坐标
     * @return 进度百分比 (0.0 ~ 1.0)
     */
    protected float calculateCurrentSeekBarPercent(float touchDownX) {
        if (currTouchSB == null) return 0;
        float percent = (touchDownX - getProgressLeft()) * 1f / (progressWidth);
        if (touchDownX < getProgressLeft()) {
            percent = 0;
        } else if (touchDownX > getProgressRight()) {
            percent = 1;
        }
        // 双滑块模式下的最小间隔边界把控
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            if (currTouchSB == leftSB) {
                if (percent > rightSB.currPercent - reservePercent) {
                    percent = rightSB.currPercent - reservePercent;
                }
            } else if (currTouchSB == rightSB) {
                if (percent < leftSB.currPercent + reservePercent) {
                    percent = leftSB.currPercent + reservePercent;
                }
            }
        }
        return percent;
    }

    /**
     * 判断触摸点是否落在 SeekBar 的有效响应区域
     * <p>
     * 滑块碰撞区或进度条本体
     * <p>
     * 用于横向滑动冲突判定
     * 避免在空白区域拦截父容器手势
     */
    private boolean isTouchInSeekBarArea(float x, float y) {
        if ((currTouchSB != null) && currTouchSB.collide(x, y)) {
            return true;
        }
        // 扩展垂直 10dp 的点击容错响应区
        return y >= getProgressTop() - Utils.dp2px(getContext(), 10)
                && y <= getProgressBottom() + Utils.dp2px(getContext(), 10)
                && x >= getProgressLeft() && x <= getProgressRight();
    }

    /**
     * 触摸事件拦截与滑动冲突调度处理
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnable) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = getEventX(event);
                touchDownY = getEventY(event);
                // 重置拖拽标志
                isDragging = false;
                // 预判目标滑块
                // 离触摸点近的滑块优先
                if (seekBarMode == SEEKBAR_MODE_RANGE) {
                    if (rightSB.currPercent >= 1 && leftSB.collide(getEventX(event), getEventY(event))) {
                        currTouchSB = leftSB;
                    } else if (rightSB.collide(getEventX(event), getEventY(event))) {
                        currTouchSB = rightSB;
                    } else {
                        float performClick = (touchDownX - getProgressLeft()) * 1f / progressWidth;
                        float distanceLeft = Math.abs(leftSB.currPercent - performClick);
                        float distanceRight = Math.abs(rightSB.currPercent - performClick);
                        currTouchSB = (distanceLeft < distanceRight) ? leftSB : rightSB;
                    }
                } else {
                    currTouchSB = leftSB;
                }
                // DOWN 阶段不强行拦截父容器，放行给 MOVE 阶段动态判定。
                return true;
            case MotionEvent.ACTION_MOVE:
                float x = getEventX(event);
                float y = getEventY(event);
                float dx = Math.abs(x - touchDownX);
                float dy = Math.abs(y - touchDownY);
                // 未锁定手势时，判断是纵向滑动还是横向滑动。
                if (!isDragging) {
                    if (dy > touchSlop && dy > dx) {
                        // 1. 纵向滑动偏移更大
                        // 释放控制权给纵向父容器 (如 ScrollView)
                        if (getParent() != null) {
                            getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        changeThumbActivateState(false);
                        resetCurrentSeekBarThumb();
                        return false;
                    } else if (dx > touchSlop && dx > dy) {
                        // 2. 横向滑动
                        // 进一步校验触摸点是否落在了滑块或进度条有效区
                        if (isTouchInSeekBarArea(touchDownX, touchDownY)) {
                            // 确认对 SeekBar 进行拖拽
                            // 阻止横向父容器 (如 ViewPager/HorizontalScrollView) 拦截手势
                            isDragging = true;
                            if (getParent() != null) {
                                getParent().requestDisallowInterceptTouchEvent(true);
                            }
                            scaleCurrentSeekBarThumb();
                            changeThumbActivateState(true);
                            if (callback != null) {
                                callback.onStartTrackingTouch(this, currTouchSB == leftSB);
                            }
                        } else {
                            // 触摸点在 View 空白处
                            // 放行手势给外层横向父容器
                            if (getParent() != null) {
                                getParent().requestDisallowInterceptTouchEvent(false);
                            }
                            return false;
                        }
                    }
                }
                // 进入横向拖拽锁定状态，实时更新滑块位置。
                if (isDragging) {
                    // 处理双滑块完全重合时的滑动方向交替切换
                    if ((seekBarMode == SEEKBAR_MODE_RANGE) && leftSB.currPercent == rightSB.currPercent) {
                        currTouchSB.materialRestore();
                        if (callback != null) {
                            callback.onStopTrackingTouch(this, currTouchSB == leftSB);
                        }
                        if (x - touchDownX > 0) {
                            if (currTouchSB != rightSB) {
                                currTouchSB.setShowIndicatorEnable(false);
                                resetCurrentSeekBarThumb();
                                currTouchSB = rightSB;
                            }
                        } else {
                            if (currTouchSB != leftSB) {
                                currTouchSB.setShowIndicatorEnable(false);
                                resetCurrentSeekBarThumb();
                                currTouchSB = leftSB;
                            }
                        }
                        if (callback != null) {
                            callback.onStartTrackingTouch(this, currTouchSB == leftSB);
                        }
                    }
                    scaleCurrentSeekBarThumb();
                    currTouchSB.material = currTouchSB.material >= 1 ? 1 : currTouchSB.material + 0.1f;
                    // 滑动更新当前百分比
                    currTouchSB.slide(calculateCurrentSeekBarPercent(x));
                    currTouchSB.setShowIndicatorEnable(true);
                    if (callback != null) {
                        SeekBarState[] states = getRangeSeekBarState();
                        callback.onRangeChanged(this, states[0].value, states[1].value, true);
                    }
                    invalidate();
                    changeThumbActivateState(true);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    // 1. 拖拽结束
                    // 如果开启了 Step 自动吸附，计算吸附点。
                    if (event.getAction() == MotionEvent.ACTION_UP && verifyStepsMode() && stepsAutoBonding) {
                        float percent = calculateCurrentSeekBarPercent(getEventX(event));
                        float stepPercent = 1.0f / steps;
                        int stepSelected = new BigDecimal(percent / stepPercent).setScale(0, RoundingMode.HALF_UP).intValue();
                        currTouchSB.slide(stepSelected * stepPercent);
                    }
                    if (seekBarMode == SEEKBAR_MODE_RANGE) {
                        rightSB.setShowIndicatorEnable(false);
                    }
                    leftSB.setShowIndicatorEnable(false);
                    if (currTouchSB != null) {
                        currTouchSB.materialRestore();
                    }
                    resetCurrentSeekBarThumb();
                    if (callback != null) {
                        SeekBarState[] states = getRangeSeekBarState();
                        callback.onRangeChanged(this, states[0].value, states[1].value, false);
                        callback.onStopTrackingTouch(this, currTouchSB == leftSB);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    // 2. 未触发拖拽的纯点击行为
                    // 滑块跳变到点击位置
                    float performClick = calculateCurrentSeekBarPercent(getEventX(event));
                    currTouchSB.slide(performClick);
                    if (callback != null) {
                        SeekBarState[] states = getRangeSeekBarState();
                        callback.onRangeChanged(this, states[0].value, states[1].value, false);
                    }
                    invalidate();
                }
                // 清理状态，还原父容器拦截标志。
                isDragging = false;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                changeThumbActivateState(false);
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.minValue = minProgress;
        ss.maxValue = maxProgress;
        ss.rangeInterval = minInterval;
        SeekBarState[] results = getRangeSeekBarState();
        ss.currSelectedMin = results[0].value;
        ss.currSelectedMax = results[1].value;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        try {
            SavedState ss = (SavedState) state;
            super.onRestoreInstanceState(ss.getSuperState());
            float min = ss.minValue;
            float max = ss.maxValue;
            float rangeInterval = ss.rangeInterval;
            setRange(min, max, rangeInterval);
            float currSelectedMin = ss.currSelectedMin;
            float currSelectedMax = ss.currSelectedMax;
            setProgress(currSelectedMin, currSelectedMax);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnRangeChangedListener(OnRangeChangedListener onRangeChangedListener) {
        callback = onRangeChangedListener;
    }

    public void setProgress(float value) {
        setProgress(value, maxProgress);
    }

    public void setProgress(float leftValue, float rightValue) {
        leftValue = Math.min(leftValue, rightValue);
        rightValue = Math.max(leftValue, rightValue);
        if ((rightValue - leftValue) < minInterval) {
            if ((leftValue - minProgress) > (maxProgress - rightValue)) {
                leftValue = (rightValue - minInterval);
            } else {
                rightValue = (leftValue + minInterval);
            }
        }
        if (leftValue < minProgress) {
            throw new IllegalArgumentException("setProgress() min < (preset min - offsetValue) . #min:" + leftValue + " #preset min:" + rightValue);
        }
        if (rightValue > maxProgress) {
            throw new IllegalArgumentException("setProgress() max > (preset max - offsetValue) . #max:" + rightValue + " #preset max:" + rightValue);
        }
        float range = (maxProgress - minProgress);
        leftSB.currPercent = Math.abs(leftValue - minProgress) / range;
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.currPercent = Math.abs(rightValue - minProgress) / range;
        }
        if (callback != null) {
            callback.onRangeChanged(this, leftValue, rightValue, false);
        }
        invalidate();
    }

    /**
     * 设置范围
     *
     * @param min 最小值
     * @param max 最大值
     */
    public void setRange(float min, float max) {
        setRange(min, max, minInterval);
    }

    /**
     * 设置范围
     *
     * @param min         最小值
     * @param max         最大值
     * @param minInterval 最小间隔
     */
    public void setRange(float min, float max, float minInterval) {
        if (max <= min) {
            throw new IllegalArgumentException("setRange() max must be greater than min ! #max:" + max + " #min:" + min);
        }
        if (minInterval < 0) {
            throw new IllegalArgumentException("setRange() interval must be greater than zero ! #minInterval:" + minInterval);
        }
        if (minInterval >= max - min) {
            throw new IllegalArgumentException("setRange() interval must be less than (max - min) ! #minInterval:" + minInterval + " #max - min:" + (max - min));
        }
        maxProgress = max;
        minProgress = min;
        this.minInterval = minInterval;
        reservePercent = minInterval / (max - min);
        // set default value
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            if (leftSB.currPercent + reservePercent <= 1 && leftSB.currPercent + reservePercent > rightSB.currPercent) {
                rightSB.currPercent = leftSB.currPercent + reservePercent;
            } else if (rightSB.currPercent - reservePercent >= 0 && rightSB.currPercent - reservePercent < leftSB.currPercent) {
                leftSB.currPercent = rightSB.currPercent - reservePercent;
            }
        }
        invalidate();
    }

    /**
     * 获取左右滑块的当前状态数据
     * <p>
     * 真实数值、提示文本、极值状态
     * <p>
     * see {@link SeekBarState}
     */
    public SeekBarState[] getRangeSeekBarState() {
        SeekBarState leftSeekBarState = new SeekBarState();
        leftSeekBarState.value = leftSB.getProgress();
        leftSeekBarState.indicatorText = String.valueOf(leftSeekBarState.value);
        if (Utils.compareFloat(leftSeekBarState.value, minProgress) == 0) {
            leftSeekBarState.isMin = true;
        } else if (Utils.compareFloat(leftSeekBarState.value, maxProgress) == 0) {
            leftSeekBarState.isMax = true;
        }
        SeekBarState rightSeekBarState = new SeekBarState();
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSeekBarState.value = rightSB.getProgress();
            rightSeekBarState.indicatorText = String.valueOf(rightSeekBarState.value);
            if (Utils.compareFloat(rightSB.currPercent, minProgress) == 0) {
                rightSeekBarState.isMin = true;
            } else if (Utils.compareFloat(rightSB.currPercent, maxProgress) == 0) {
                rightSeekBarState.isMax = true;
            }
        }
        return new SeekBarState[]{leftSeekBarState, rightSeekBarState};
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.isEnable = enabled;
    }

    public void setIndicatorText(String progress) {
        leftSB.setIndicatorText(progress);
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.setIndicatorText(progress);
        }
    }

    /**
     * format number indicator text
     *
     * @param formatPattern format rules
     */
    public void setIndicatorTextDecimalFormat(String formatPattern) {
        leftSB.setIndicatorTextDecimalFormat(formatPattern);
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.setIndicatorTextDecimalFormat(formatPattern);
        }
    }

    /**
     * format string indicator text
     *
     * @param formatPattern format rules
     */
    public void setIndicatorTextStringFormat(String formatPattern) {
        leftSB.setIndicatorTextStringFormat(formatPattern);
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.setIndicatorTextStringFormat(formatPattern);
        }
    }

    /**
     * If is single mode, please use it to get the SeekBar.
     *
     * @return left seek bar
     */
    public SeekBar getLeftSeekBar() {
        return leftSB;
    }

    public SeekBar getRightSeekBar() {
        return rightSB;
    }

    public int getProgressTop() {
        return progressTop;
    }

    public void setProgressTop(int progressTop) {
        this.progressTop = progressTop;
    }

    public int getProgressBottom() {
        return progressBottom;
    }

    public void setProgressBottom(int progressBottom) {
        this.progressBottom = progressBottom;
    }

    public int getProgressLeft() {
        return progressLeft;
    }

    public void setProgressLeft(int progressLeft) {
        this.progressLeft = progressLeft;
    }

    public int getProgressRight() {
        return progressRight;
    }

    public void setProgressRight(int progressRight) {
        this.progressRight = progressRight;
    }

    public int getProgressPaddingRight() {
        return progressPaddingRight;
    }

    public int getProgressHeight() {
        return progressHeight;
    }

    public void setProgressHeight(int progressHeight) {
        this.progressHeight = progressHeight;
    }

    public float getMinProgress() {
        return minProgress;
    }

    public float getMaxProgress() {
        return maxProgress;
    }

    public void setProgressColor(@ColorInt int progressDefaultColor, @ColorInt int progressColor) {
        this.progressDefaultColor = progressDefaultColor;
        this.progressColor = progressColor;
    }

    public int getTickMarkTextColor() {
        return tickMarkTextColor;
    }

    public void setTickMarkTextColor(@ColorInt int tickMarkTextColor) {
        this.tickMarkTextColor = tickMarkTextColor;
    }

    public int getTickMarkInRangeTextColor() {
        return tickMarkInRangeTextColor;
    }

    public void setTickMarkInRangeTextColor(@ColorInt int tickMarkInRangeTextColor) {
        this.tickMarkInRangeTextColor = tickMarkInRangeTextColor;
    }

    public int getSeekBarMode() {
        return seekBarMode;
    }

    /**
     * {@link #SEEKBAR_MODE_SINGLE} is single SeekBar
     * {@link #SEEKBAR_MODE_RANGE} is range SeekBar
     *
     * @param seekBarMode SeekBar 模式
     */
    public void setSeekBarMode(@SeekBarModeDef int seekBarMode) {
        this.seekBarMode = seekBarMode;
        rightSB.setVisible(seekBarMode != SEEKBAR_MODE_SINGLE);
    }

    public int getTickMarkMode() {
        return tickMarkMode;
    }

    /**
     * {@link #TICK_MARK_GRAVITY_LEFT} is number tick mark, it will locate the position according to the value.
     * {@link #TICK_MARK_GRAVITY_RIGHT} is text tick mark, it will be equally positioned.
     *
     * @param tickMarkMode TickMarkMode
     */
    public void setTickMarkMode(@TickMarkModeDef int tickMarkMode) {
        this.tickMarkMode = tickMarkMode;
    }

    public int getTickMarkTextMargin() {
        return tickMarkTextMargin;
    }

    public void setTickMarkTextMargin(int tickMarkTextMargin) {
        this.tickMarkTextMargin = tickMarkTextMargin;
    }

    public int getTickMarkTextSize() {
        return tickMarkTextSize;
    }

    public void setTickMarkTextSize(int tickMarkTextSize) {
        this.tickMarkTextSize = tickMarkTextSize;
    }

    public int getTickMarkGravity() {
        return tickMarkGravity;
    }

    /**
     * the tick mark text gravity
     * {@link #TICK_MARK_GRAVITY_LEFT}
     * {@link #TICK_MARK_GRAVITY_RIGHT}
     * {@link #TICK_MARK_GRAVITY_CENTER}
     *
     * @param tickMarkGravity TickMarkGravity
     */
    public void setTickMarkGravity(@TickMarkGravityDef int tickMarkGravity) {
        this.tickMarkGravity = tickMarkGravity;
    }

    public CharSequence[] getTickMarkTextArray() {
        return tickMarkTextArray;
    }

    public void setTickMarkTextArray(CharSequence[] tickMarkTextArray) {
        this.tickMarkTextArray = tickMarkTextArray;
    }

    public float getMinInterval() {
        return minInterval;
    }

    public float getProgressRadius() {
        return progressRadius;
    }

    public void setProgressRadius(float progressRadius) {
        this.progressRadius = progressRadius;
    }

    public int getProgressColor() {
        return progressColor;
    }

    public void setProgressColor(@ColorInt int progressColor) {
        this.progressColor = progressColor;
    }

    public int getProgressDefaultColor() {
        return progressDefaultColor;
    }

    public void setProgressDefaultColor(@ColorInt int progressDefaultColor) {
        this.progressDefaultColor = progressDefaultColor;
    }

    public int getProgressDrawableId() {
        return progressDrawableId;
    }

    public void setProgressDrawableId(@DrawableRes int progressDrawableId) {
        this.progressDrawableId = progressDrawableId;
        progressBitmap = null;
        initProgressBitmap();
    }

    public int getProgressDefaultDrawableId() {
        return progressDefaultDrawableId;
    }

    public void setProgressDefaultDrawableId(@DrawableRes int progressDefaultDrawableId) {
        this.progressDefaultDrawableId = progressDefaultDrawableId;
        progressDefaultBitmap = null;
        initProgressBitmap();
    }

    public int getProgressWidth() {
        return progressWidth;
    }

    public void setProgressWidth(int progressWidth) {
        this.progressWidth = progressWidth;
    }

    public void setTypeface(Typeface typeFace) {
        paint.setTypeface(typeFace);
    }

    public boolean isEnableThumbOverlap() {
        return enableThumbOverlap;
    }

    public void setEnableThumbOverlap(boolean enableThumbOverlap) {
        this.enableThumbOverlap = enableThumbOverlap;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public int getStepsColor() {
        return stepsColor;
    }

    public void setStepsColor(@ColorInt int stepsColor) {
        this.stepsColor = stepsColor;
    }

    public float getStepsWidth() {
        return stepsWidth;
    }

    public void setStepsWidth(float stepsWidth) {
        this.stepsWidth = stepsWidth;
    }

    public float getStepsHeight() {
        return stepsHeight;
    }

    public void setStepsHeight(float stepsHeight) {
        this.stepsHeight = stepsHeight;
    }

    public float getStepsRadius() {
        return stepsRadius;
    }

    public void setStepsRadius(float stepsRadius) {
        this.stepsRadius = stepsRadius;
    }

    public int getTickMarkLayoutGravity() {
        return tickMarkLayoutGravity;
    }

    /**
     * the tick mark layout gravity
     * <p>
     * Gravity.TOP and Gravity.BOTTOM
     *
     * @param tickMarkLayoutGravity TickMarkLayoutGravity
     */
    public void setTickMarkLayoutGravity(@TickMarkLayoutGravityDef int tickMarkLayoutGravity) {
        this.tickMarkLayoutGravity = tickMarkLayoutGravity;
    }

    public int getGravity() {
        return gravity;
    }

    /**
     * the RangeSeekBar gravity
     * <p>
     * Gravity.TOP and Gravity.BOTTOM
     *
     * @param gravity 位置
     */
    public void setGravity(@GravityDef int gravity) {
        this.gravity = gravity;
    }

    public boolean isStepsAutoBonding() {
        return stepsAutoBonding;
    }

    public void setStepsAutoBonding(boolean stepsAutoBonding) {
        this.stepsAutoBonding = stepsAutoBonding;
    }

    public int getStepsDrawableId() {
        return stepsDrawableId;
    }

    public void setStepsDrawableId(@DrawableRes int stepsDrawableId) {
        this.stepsBitmaps.clear();
        this.stepsDrawableId = stepsDrawableId;
        initStepsBitmap();
    }

    public List<Bitmap> getStepsBitmaps() {
        return stepsBitmaps;
    }

    public void setStepsBitmaps(List<Bitmap> stepsBitmaps) {
        if (stepsBitmaps == null || stepsBitmaps.isEmpty() || stepsBitmaps.size() <= steps) {
            throw new IllegalArgumentException("stepsBitmaps must > steps !");
        }
        this.stepsBitmaps.clear();
        this.stepsBitmaps.addAll(stepsBitmaps);
    }

    public void setStepsDrawable(List<Integer> stepsDrawableIds) {
        if (stepsDrawableIds == null || stepsDrawableIds.isEmpty() || stepsDrawableIds.size() <= steps) {
            throw new IllegalArgumentException("stepsDrawableIds must > steps !");
        }
        if (!verifyStepsMode()) {
            throw new IllegalArgumentException("stepsWidth must > 0, stepsHeight must > 0, steps must > 0 First !!");
        }
        List<Bitmap> stepsBitmaps = new ArrayList<>();
        for (int i = 0; i < stepsDrawableIds.size(); i++) {
            stepsBitmaps.add(Utils.drawableToBitmap(getContext(), (int) stepsWidth, (int) stepsHeight, stepsDrawableIds.get(i)));
        }
        setStepsBitmaps(stepsBitmaps);
    }

    @IntDef({SEEKBAR_MODE_SINGLE, SEEKBAR_MODE_RANGE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SeekBarModeDef {

    }

    @IntDef({TRICK_MARK_MODE_NUMBER, TRICK_MARK_MODE_OTHER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TickMarkModeDef {

    }

    @IntDef({TICK_MARK_GRAVITY_LEFT, TICK_MARK_GRAVITY_CENTER, TICK_MARK_GRAVITY_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TickMarkGravityDef {

    }

    @IntDef({Gravity.TOP, Gravity.BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TickMarkLayoutGravityDef {

    }

    @IntDef({Gravity.TOP, Gravity.CENTER, Gravity.BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface GravityDef {

    }

    public static class Gravity {
        public final static int TOP = 0;
        public final static int BOTTOM = 1;
        public final static int CENTER = 2;
    }
}