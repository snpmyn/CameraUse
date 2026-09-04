package com.qtone.camerause.widget.button;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.Nullable;

import com.qtone.camerause.R;

/**
 * Created on 2026/8/29.
 *
 * @author 郑少鹏
 * @desc 流光按钮
 */
public class ShimmerButton extends View {
    private final RectF ringBounds = new RectF();
    private final RectF innerRectBounds = new RectF();
    private final Matrix gradientMatrix = new Matrix();
    /**
     * 线程消息调度器
     */
    private final Handler handler = new Handler(Looper.getMainLooper());
    /**
     * 方形外轨流光与蓄力绘制所需的 Path 与 PathMeasure 结构
     */
    private final Path outerSquarePath = new Path();
    private final Path segmentPath = new Path();
    private final PathMeasure pathMeasure = new PathMeasure();
    /**
     * 预分配复用对象
     * <p>
     * 解决 onDraw / Layout 内存分配警告
     */
    private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();
    private final int[] sweepColors = new int[]{Color.TRANSPARENT, Color.BLACK};
    private final float[] sweepPositions = new float[]{0.2f, 1.0f};
    private SweepGradient sweepGradient;
    private int lastSweepActiveColor = Color.TRANSPARENT;
    /**
     * 主体色
     */
    private int colorPrimary = Color.parseColor("#29A1F7");
    /**
     * 文本色
     */
    private int textColor = Color.WHITE;
    /**
     * 当前流光按钮状态
     */
    private ShimmerButtonState currentShimmerButtonState = ShimmerButtonState.IDLE;
    /**
     * 当前流光按钮形状
     */
    private ShimmerButtonShape currentShimmerButtonShape = ShimmerButtonShape.CIRCLE;
    /**
     * 当前流光按钮触发模式
     */
    private ShimmerButtonTriggerMode currentShimmerButtonTriggerMode = ShimmerButtonTriggerMode.CLICK;
    /**
     * 方形圆角半径
     */
    private float cornerRadius = 0f;
    /**
     * 自定义外环圆圈宽度
     * <p>
     * 默认 -1 表示按比例自适应计算
     */
    private float customRingWidth = -1f;
    /**
     * 自定义中间图像与外环间距
     * <p>
     * 默认 -1 表示按比例自适应计算
     */
    private float customInnerPadding = -1f;
    /**
     * 画笔
     */
    private Paint outerRingPaint;
    private Paint innerCirclePaint;
    private Paint textPaint;
    /**
     * 用于绘制流光与蓄力弧线的画笔
     */
    private Paint loadingArcPaint;
    /**
     * 尺寸与坐标
     */
    private float centerX;
    private float centerY;
    private float outerRadius;
    private float innerRadiusBase;
    /**
     * 内部实心圆缩放比例
     */
    private float currentInnerScale = 1.0f;
    /**
     * 自定义文本大小
     * <p>
     * 默认 -1 表示按比例自适应计算
     */
    private float customTextSize = -1f;
    /**
     * 颜色变化控制
     */
    private ValueAnimator colorAnimator;
    private ValueAnimator scaleAnimator;
    /**
     * 流光无限旋转动画
     */
    private ValueAnimator runningAnimator;
    /**
     * 蓄力 0 ~ 360° 动画
     */
    private ValueAnimator chargeAnimator;
    /**
     * 进行中流光旋转角度
     */
    private float currentRotateAngle = -90f;
    /**
     * 蓄力填充角度
     * <p>
     * 0 ~ 360
     */
    private float currentChargeSweepAngle = 0f;
    /**
     * 颜色过渡
     */
    private int currentInnerColor = colorPrimary;
    /**
     * 显示文本
     */
    private String text = "流光";
    /**
     * 记录空闲状态文本
     * <p>
     * 便于停止恢复
     */
    private String idleText = "流光";
    /**
     * 动画时长控制
     * <p>
     * 蓄力走满一圈所需时间
     * <p>
     * 单位 - 毫秒
     */
    private long chargeDuration = 1000L;
    /**
     * 进行中流光旋转一圈所需时间
     */
    private long rotateDuration = 900L;
    /**
     * 是否正在蓄力中
     */
    private boolean isCharging = false;
    /**
     * 标记本次按压是否已成功蓄力完成并触发
     */
    private boolean isChargedTriggered = false;
    private boolean isTouchDown = false;
    /**
     * 流光按钮回调
     */
    private OnShimmerButtonCallback onShimmerButtonCallback;
    /**
     * 长按任务
     */
    private final Runnable longPressRunnable = this::startChargingAnimation;

    /**
     * constructor
     *
     * @param context 上下文
     */
    public ShimmerButton(Context context) {
        this(context, null);
    }

    /**
     * constructor
     *
     * @param context 上下文
     * @param attrs   属性集
     */
    public ShimmerButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * constructor
     *
     * @param context      上下文
     * @param attrs        属性集
     * @param defStyleAttr 默认样式属性
     */
    public ShimmerButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * 初始化
     *
     * @param context 上下文
     * @param attrs   属性集
     */
    private void init(Context context, @Nullable AttributeSet attrs) {
        setCornerRadiusDp(R.dimen.dp_12);
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ShimmerButton);
            if (typedArray.hasValue(R.styleable.ShimmerButton_sbText)) {
                String xmlText = typedArray.getString(R.styleable.ShimmerButton_sbText);
                if (xmlText != null) {
                    text = xmlText;
                    idleText = xmlText;
                }
            }
            textColor = typedArray.getColor(R.styleable.ShimmerButton_sbTextColor, textColor);
            customTextSize = typedArray.getDimension(R.styleable.ShimmerButton_sbTextSize, -1f);
            colorPrimary = typedArray.getColor(R.styleable.ShimmerButton_sbPrimaryColor, colorPrimary);
            currentShimmerButtonState = ShimmerButtonState.IDLE;
            int triggerVal = typedArray.getInt(R.styleable.ShimmerButton_sbTriggerMode, 0);
            currentShimmerButtonTriggerMode = ShimmerButtonTriggerMode.fromValue(triggerVal);
            int shapeVal = typedArray.getInt(R.styleable.ShimmerButton_sbShape, 0);
            currentShimmerButtonShape = ShimmerButtonShape.fromValue(shapeVal);
            cornerRadius = typedArray.getDimension(R.styleable.ShimmerButton_sbCornerRadius, cornerRadius);
            chargeDuration = typedArray.getInt(R.styleable.ShimmerButton_sbChargeDuration, (int) chargeDuration);
            rotateDuration = typedArray.getInt(R.styleable.ShimmerButton_sbRotateDuration, (int) rotateDuration);
            customRingWidth = typedArray.getDimension(R.styleable.ShimmerButton_sbRingWidth, -1f);
            customInnerPadding = typedArray.getDimension(R.styleable.ShimmerButton_sbInnerPadding, -1f);
            typedArray.recycle();
        }

        currentInnerColor = colorPrimary;

        outerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerRingPaint.setStyle(Paint.Style.STROKE);

        innerCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerCirclePaint.setStyle(Paint.Style.FILL);
        innerCirclePaint.setColor(currentInnerColor);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        loadingArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        loadingArcPaint.setStyle(Paint.Style.STROKE);
        loadingArcPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    /**
     * 调整颜色透明度
     *
     * @param color 原颜色
     * @param alpha 透明度比例 (0.0f - 1.0f)
     * @return 调整后的颜色
     */
    @SuppressWarnings("SameParameterValue")
    private int adjustColorAlpha(int color, float alpha) {
        int alphaInt = Math.round(Color.alpha(color) * alpha);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alphaInt, red, green, blue);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        float minSize = Math.min(w, h);

        // 已自定外环宽度则用自定义值
        // 否则默认按控件大小比例计算
        float strokeWidth = (customRingWidth > 0) ? customRingWidth : (minSize * 0.06f);
        outerRadius = ((minSize / 2f) - (strokeWidth / 2f) - 4f);

        // 已自定内边距则使用自定义值
        // 否则默认按线宽计算
        float innerPadding = (customInnerPadding >= 0) ? customInnerPadding : (strokeWidth * 1f);
        innerRadiusBase = (outerRadius - strokeWidth / 2f - innerPadding);
        if (innerRadiusBase < 0) {
            innerRadiusBase = 0;
        }

        outerRingPaint.setStrokeWidth(strokeWidth);
        loadingArcPaint.setStrokeWidth(strokeWidth);

        if (customTextSize > 0) {
            textPaint.setTextSize(customTextSize);
        } else {
            textPaint.setTextSize(minSize * 0.22f);
        }

        ringBounds.set(centerX - outerRadius, centerY - outerRadius, centerX + outerRadius, centerY + outerRadius);
        sweepGradient = null;
        lastSweepActiveColor = Color.TRANSPARENT;
        setupSquarePath();
    }

    /**
     * 构建外围方形 Path
     */
    private void setupSquarePath() {
        if ((getWidth() <= 0) || (getHeight() <= 0)) {
            return;
        }
        outerSquarePath.reset();
        float outerStrokeHalf = outerRingPaint.getStrokeWidth() / 2f;
        float halfW = ((getWidth() / 2f) - outerStrokeHalf - 4f);
        float halfH = ((getHeight() / 2f) - outerStrokeHalf - 4f);
        if (halfW < 0) {
            halfW = 0;
        }
        if (halfH < 0) {
            halfH = 0;
        }

        innerRectBounds.set(centerX - halfW, centerY - halfH, centerX + halfW, centerY + halfH);

        float top = innerRectBounds.top;
        float bottom = innerRectBounds.bottom;
        float left = innerRectBounds.left;
        float right = innerRectBounds.right;

        // 顶部中心作为起始点
        outerSquarePath.moveTo(centerX, top);
        // 右上圆角
        outerSquarePath.lineTo(right - cornerRadius, top);
        outerSquarePath.quadTo(right, top, right, top + cornerRadius);
        // 右下圆角
        outerSquarePath.lineTo(right, bottom - cornerRadius);
        outerSquarePath.quadTo(right, bottom, right - cornerRadius, bottom);
        // 左下圆角
        outerSquarePath.lineTo(left + cornerRadius, bottom);
        outerSquarePath.quadTo(left, bottom, left, bottom - cornerRadius);
        // 左上圆角
        outerSquarePath.lineTo(left, top + cornerRadius);
        outerSquarePath.quadTo(left, top, left + cornerRadius, top);
        // 封闭至起始点
        outerSquarePath.close();

        pathMeasure.setPath(outerSquarePath, false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (outerRadius <= 0) {
            return;
        }
        // 1. 绘制外层半透明底轨
        int outerRingColor = adjustColorAlpha(currentInnerColor, 0.25f);
        outerRingPaint.setColor(outerRingColor);
        if (currentShimmerButtonShape == ShimmerButtonShape.SQUARE) {
            canvas.drawPath(outerSquarePath, outerRingPaint);
        } else {
            canvas.drawCircle(centerX, centerY, outerRadius, outerRingPaint);
        }
        // 2. 蓄力阶段
        // 绘制随时间填满的彩色弧线 / 轨迹
        if (isCharging) {
            loadingArcPaint.setShader(null);
            loadingArcPaint.setColor(getActiveColor());

            if (currentShimmerButtonShape == ShimmerButtonShape.SQUARE) {
                float totalLength = pathMeasure.getLength();
                float stopD = totalLength * (currentChargeSweepAngle / 360f);
                segmentPath.reset();
                pathMeasure.getSegment(0, stopD, segmentPath, true);
                canvas.drawPath(segmentPath, loadingArcPaint);
            } else {
                canvas.drawArc(ringBounds, -90f, currentChargeSweepAngle, false, loadingArcPaint);
            }
        }
        // 3. 进行中状态
        // 绘制转动的流光效果
        // 280 度带渐变尾巴
        else if (isBusy()) {
            int activeColor = getActiveColor();
            if ((sweepGradient == null) || (lastSweepActiveColor != activeColor)) {
                sweepColors[1] = activeColor;
                sweepGradient = new SweepGradient(centerX, centerY, sweepColors, sweepPositions);
                lastSweepActiveColor = activeColor;
            }
            gradientMatrix.setRotate(currentRotateAngle, centerX, centerY);
            sweepGradient.setLocalMatrix(gradientMatrix);
            loadingArcPaint.setShader(sweepGradient);
            if (currentShimmerButtonShape == ShimmerButtonShape.SQUARE) {
                float totalLength = pathMeasure.getLength();
                float sweepLength = totalLength * (280f / 360f);
                float startRatio = ((currentRotateAngle + 90f) % 360f + 360f) % 360f / 360f;
                float startD = totalLength * startRatio;
                float stopD = (startD + sweepLength);
                segmentPath.reset();
                if (stopD <= totalLength) {
                    pathMeasure.getSegment(startD, stopD, segmentPath, true);
                } else {
                    pathMeasure.getSegment(startD, totalLength, segmentPath, true);
                    pathMeasure.getSegment(0, stopD - totalLength, segmentPath, true);
                }
                canvas.drawPath(segmentPath, loadingArcPaint);
            } else {
                canvas.drawArc(ringBounds, currentRotateAngle, 280f, false, loadingArcPaint);
            }
        }
        // 4. 绘制中间实心区域
        // 圆角矩形或圆形
        innerCirclePaint.setColor(currentInnerColor);
        if (currentShimmerButtonShape == ShimmerButtonShape.SQUARE) {
            float strokeWidth = outerRingPaint.getStrokeWidth();
            float innerPadding = (customInnerPadding >= 0) ? customInnerPadding : (strokeWidth * 1f);
            float gap = ((strokeWidth / 2f) + innerPadding);
            float baseHalfW = ((getWidth() / 2f) - strokeWidth / 2f - 4f - gap);
            float baseHalfH = ((getHeight() / 2f) - strokeWidth / 2f - 4f - gap);
            if (baseHalfW < 0) {
                baseHalfW = 0;
            }
            if (baseHalfH < 0) {
                baseHalfH = 0;
            }
            float currentHalfW = baseHalfW * currentInnerScale;
            float currentHalfH = baseHalfH * currentInnerScale;
            float scaledCornerRadius = cornerRadius * currentInnerScale;
            innerRectBounds.set(centerX - currentHalfW, centerY - currentHalfH, centerX + currentHalfW, centerY + currentHalfH);
            canvas.drawRoundRect(innerRectBounds, scaledCornerRadius, scaledCornerRadius, innerCirclePaint);
        } else {
            float currentRadius = innerRadiusBase * currentInnerScale;
            canvas.drawCircle(centerX, centerY, currentRadius, innerCirclePaint);
        }
        // 5. 绘制居中文本
        if ((text != null) && !text.isEmpty()) {
            textPaint.setColor(textColor);
            Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
            float baseline = (centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f);
            canvas.drawText(text, centerX, baseline, textPaint);
        }
    }

    /**
     * 获取当前状态对应的激活颜色
     *
     * @return 当前状态对应的激活颜色
     */
    private int getActiveColor() {
        return colorPrimary;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return super.onTouchEvent(event);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if ((onShimmerButtonCallback != null) && onShimmerButtonCallback.onShimmerButtonIntercept()) {
                    // 拦截事件
                    return false;
                }
                isTouchDown = true;
                isChargedTriggered = false;
                // 按下缩小效果
                animateInnerScale(0.88f, 100, new DecelerateInterpolator());
                if (currentShimmerButtonTriggerMode == ShimmerButtonTriggerMode.LONG_PRESS_CHARGE) {
                    handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout());
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!isTouchDown) {
                    return true;
                }
                float x = event.getX();
                float y = event.getY();
                if ((x < 0) || (x > getWidth()) || (y < 0) || (y > getHeight())) {
                    resetTouchState();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!isTouchDown) {
                    return true;
                }
                // 松开弹回效果
                animateInnerScale(1.0f, 200, new OvershootInterpolator(2.0f));
                if (isChargedTriggered) {
                    isChargedTriggered = false;
                    isTouchDown = false;
                    return true;
                }
                isTouchDown = false;
                if (currentShimmerButtonTriggerMode == ShimmerButtonTriggerMode.LONG_PRESS_CHARGE) {
                    handler.removeCallbacks(longPressRunnable);
                    if (isCharging) {
                        cancelChargingAnimation();
                    } else if (isBusy()) {
                        stop();
                    }
                } else if (currentShimmerButtonTriggerMode == ShimmerButtonTriggerMode.CLICK) {
                    if (isBusy()) {
                        stop();
                    } else {
                        trigger();
                    }
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                resetTouchState();
                return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 重置按压与蓄力状态
     */
    private void resetTouchState() {
        if (isTouchDown) {
            isTouchDown = false;
            animateInnerScale(1.0f, 200, new OvershootInterpolator(2.0f));
            if (currentShimmerButtonTriggerMode == ShimmerButtonTriggerMode.LONG_PRESS_CHARGE) {
                handler.removeCallbacks(longPressRunnable);
                if (isCharging) {
                    cancelChargingAnimation();
                }
            }
        }
    }

    /**
     * 开启长按蓄力动画
     */
    private void startChargingAnimation() {
        if (!isTouchDown || isBusy()) {
            return;
        }
        isCharging = true;
        if (chargeAnimator != null) {
            chargeAnimator.cancel();
        }
        chargeAnimator = ValueAnimator.ofFloat(0f, 360f);
        chargeDuration = Math.max(0L, chargeDuration);
        chargeAnimator.setDuration(chargeDuration);
        chargeAnimator.setInterpolator(new DecelerateInterpolator());
        chargeAnimator.addUpdateListener(animation -> {
            currentChargeSweepAngle = (float) animation.getAnimatedValue();
            invalidate();
        });
        chargeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isCharging && isTouchDown) {
                    isCharging = false;
                    isChargedTriggered = true;
                    currentChargeSweepAngle = 0f;
                    trigger();
                }
            }
        });
        chargeAnimator.start();
    }

    /**
     * 取消长按蓄力动画
     */
    private void cancelChargingAnimation() {
        isCharging = false;
        if ((chargeAnimator != null) && chargeAnimator.isRunning()) {
            chargeAnimator.cancel();
        }
        currentChargeSweepAngle = 0f;
        invalidate();
        if (onShimmerButtonCallback != null) {
            onShimmerButtonCallback.onShimmerButtonChargeCancel();
        }
    }

    /**
     * 触发
     */
    private void trigger() {
        setShimmerButtonStateAndText(ShimmerButtonState.RUNNING, this.text);
        if (onShimmerButtonCallback != null) {
            onShimmerButtonCallback.onShimmerButtonStart(currentShimmerButtonState);
        }
    }

    /**
     * 是否忙碌中
     *
     * @return 是否忙碌中
     */
    public boolean isBusy() {
        return (currentShimmerButtonState == ShimmerButtonState.RUNNING);
    }

    /**
     * 控制流光旋转动画的开启与停止
     */
    private void updateLoadingAnimation() {
        if (isBusy()) {
            if (runningAnimator == null) {
                runningAnimator = ValueAnimator.ofFloat(0f, 360f);
                runningAnimator.setRepeatCount(ValueAnimator.INFINITE);
                runningAnimator.setInterpolator(new LinearInterpolator());
                runningAnimator.addUpdateListener(animation -> {
                    float animatedValue = (float) animation.getAnimatedValue();
                    currentRotateAngle = (-90f + animatedValue);
                    invalidate();
                });
            }
            rotateDuration = Math.max(0L, rotateDuration);
            runningAnimator.setDuration(rotateDuration);
            if (!runningAnimator.isRunning()) {
                runningAnimator.start();
            }
        } else {
            if ((runningAnimator != null) && runningAnimator.isRunning()) {
                runningAnimator.cancel();
            }
            invalidate();
        }
    }

    /**
     * 改变内部实心圆缩放动画
     */
    private void animateInnerScale(float targetScale, long duration, @Nullable android.view.animation.Interpolator interpolator) {
        if ((scaleAnimator != null) && scaleAnimator.isRunning()) {
            scaleAnimator.cancel();
        }
        scaleAnimator = ValueAnimator.ofFloat(currentInnerScale, targetScale);
        scaleAnimator.setDuration(duration);
        if (interpolator != null) {
            scaleAnimator.setInterpolator(interpolator);
        }
        scaleAnimator.addUpdateListener(animation -> {
            currentInnerScale = (float) animation.getAnimatedValue();
            invalidate();
        });
        scaleAnimator.start();
    }

    /**
     * 设置流光按钮状态与文本
     *
     * @param shimmerButtonState 流光按钮状态
     * @param text               文本
     */
    public void setShimmerButtonStateAndText(ShimmerButtonState shimmerButtonState, String text) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            post(() -> setShimmerButtonStateAndText(shimmerButtonState, text));
            return;
        }
        this.text = text;
        if (shimmerButtonState == ShimmerButtonState.IDLE) {
            this.idleText = text;
        }
        if (this.currentShimmerButtonState == shimmerButtonState) {
            updateLoadingAnimation();
            invalidate();
            return;
        }
        this.currentShimmerButtonState = shimmerButtonState;
        int targetColor = colorPrimary;
        if ((colorAnimator != null) && colorAnimator.isRunning()) {
            colorAnimator.cancel();
        }
        int startColor = currentInnerColor;
        colorAnimator = ValueAnimator.ofFloat(0f, 1f);
        colorAnimator.setDuration(200);
        colorAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            currentInnerColor = (int) argbEvaluator.evaluate(fraction, startColor, targetColor);
            invalidate();
        });
        colorAnimator.start();
        updateLoadingAnimation();
    }

    /**
     * 获取显示文本
     *
     * @return 显示文本
     */
    public String getText() {
        return text;
    }

    /**
     * 设置显示文本
     *
     * @param text 显示文本
     */
    public void setText(String text) {
        this.text = text;
        if (!isBusy()) {
            this.idleText = text;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            invalidate();
        } else {
            postInvalidate();
        }
    }

    /**
     * 设置文本颜色
     *
     * @param textColor 文本颜色
     */
    public void setTextColor(int textColor) {
        this.textColor = textColor;
        if (textPaint != null) {
            textPaint.setColor(textColor);
        }
        invalidate();
    }

    /**
     * 设置文本大小
     *
     * @param sp SP
     */
    public void setTextSize(float sp) {
        this.customTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
        if (textPaint != null) {
            textPaint.setTextSize(customTextSize);
        }
        invalidate();
    }

    /**
     * 设置文本大小
     *
     * @param resId 资源 ID
     *              例如 R.dimen.sp_12
     */
    public void setTextSize(@androidx.annotation.DimenRes int resId) {
        try {
            this.customTextSize = getResources().getDimension(resId);
            if (textPaint != null) {
                textPaint.setTextSize(customTextSize);
            }
            invalidate();
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置主体颜色
     *
     * @param color 颜色
     */
    public void setPrimaryColor(int color) {
        this.colorPrimary = color;
        if (!isBusy()) {
            this.currentInnerColor = color;
        }
        invalidate();
    }

    /**
     * 获取当前流光按钮状态
     *
     * @return 当前流光按钮状态
     */
    public ShimmerButtonState getCurrentShimmerButtonState() {
        return currentShimmerButtonState;
    }

    /**
     * 获取当前流光按钮形状
     *
     * @return 当前流光按钮形状
     */
    public ShimmerButtonShape getCurrentShimmerButtonShape() {
        return currentShimmerButtonShape;
    }

    /**
     * 设置当前流光按钮形状
     *
     * @param shimmerButtonShape 流光按钮形状
     */
    public void setCurrentShimmerButtonShape(ShimmerButtonShape shimmerButtonShape) {
        this.currentShimmerButtonShape = shimmerButtonShape;
        setupSquarePath();
        invalidate();
    }

    /**
     * 获取当前流光按钮触发模式
     *
     * @return 当前流光按钮触发模式
     */
    public ShimmerButtonTriggerMode getCurrentShimmerButtonTriggerMode() {
        return currentShimmerButtonTriggerMode;
    }

    /**
     * 设置当前流光按钮触发模式
     *
     * @param shimmerButtonTriggerMode 流光按钮触发模式
     */
    public void setCurrentShimmerButtonTriggerMode(ShimmerButtonTriggerMode shimmerButtonTriggerMode) {
        this.currentShimmerButtonTriggerMode = shimmerButtonTriggerMode;
    }

    /**
     * 获取方形圆角半径
     *
     * @return 方形圆角半径
     */
    public float getCornerRadius() {
        return cornerRadius;
    }

    /**
     * 设置方形圆角半径
     *
     * @param dp DP
     */
    public void setCornerRadiusDp(float dp) {
        this.cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
        setupSquarePath();
        invalidate();
    }

    /**
     * 设置方形圆角半径
     *
     * @param resId 资源 ID
     *              例如 R.dimen.dp_12
     */
    public void setCornerRadiusDp(@androidx.annotation.DimenRes int resId) {
        try {
            this.cornerRadius = getResources().getDimension(resId);
            setupSquarePath();
            invalidate();
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置方形圆角半径
     *
     * @param px 像素
     */
    public void setCornerRadiusPx(float px) {
        this.cornerRadius = px;
        setupSquarePath();
        invalidate();
    }

    /**
     * 获取外环圆圈宽度
     *
     * @return 外环圆圈宽度
     */
    public float getRingWidth() {
        return customRingWidth;
    }

    /**
     * 设置外环圆圈宽度
     *
     * @param dp DP
     */
    public void setRingWidthDp(float dp) {
        this.customRingWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
        requestLayout();
        invalidate();
    }

    /**
     * 设置外环圆圈宽度
     *
     * @param resId 资源 ID
     *              例如 R.dimen.dp_4
     */
    public void setRingWidthDp(@androidx.annotation.DimenRes int resId) {
        try {
            this.customRingWidth = getResources().getDimension(resId);
            requestLayout();
            invalidate();
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置外环圆圈宽度
     *
     * @param px 像素
     */
    public void setRingWidthPx(float px) {
        this.customRingWidth = px;
        requestLayout();
        invalidate();
    }

    /**
     * 获取中间图像与外环间距
     *
     * @return 中间图像与外环间距
     */
    public float getInnerPadding() {
        return customInnerPadding;
    }

    /**
     * 设置中间图像与外环间距
     *
     * @param dp DP
     */
    public void setInnerPaddingDp(float dp) {
        this.customInnerPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
        requestLayout();
        invalidate();
    }

    /**
     * 设置中间图像与外环间距
     *
     * @param resId 资源 ID
     *              例如 R.dimen.dp_6
     */
    public void setInnerPaddingDp(@androidx.annotation.DimenRes int resId) {
        try {
            this.customInnerPadding = getResources().getDimension(resId);
            requestLayout();
            invalidate();
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置中间图像与外环间距
     *
     * @param px 像素
     */
    public void setInnerPaddingPx(float px) {
        this.customInnerPadding = px;
        requestLayout();
        invalidate();
    }

    /**
     * 获取蓄力一圈时长
     *
     * @return 蓄力一圈时长 [毫秒]
     */
    public long getChargeDuration() {
        return chargeDuration;
    }

    /**
     * 设置蓄力一圈时长
     *
     * @param durationMs 时长毫秒
     */
    public void setChargeDuration(long durationMs) {
        this.chargeDuration = durationMs;
    }

    /**
     * 获取流光转动一圈时长
     *
     * @return 流光转动一圈时长 [毫秒]
     */
    public long getRotateDuration() {
        return rotateDuration;
    }

    /**
     * 设置流光转动一圈时长
     *
     * @param durationMs 时长毫秒
     */
    public void setRotateDuration(long durationMs) {
        this.rotateDuration = durationMs;
        if ((runningAnimator != null) && runningAnimator.isRunning()) {
            runningAnimator.setDuration(rotateDuration);
        }
    }

    /**
     * 设置流光按钮回调
     *
     * @param onShimmerButtonCallback 流光按钮回调
     */
    public void setOnShimmerButtonCallback(OnShimmerButtonCallback onShimmerButtonCallback) {
        this.onShimmerButtonCallback = onShimmerButtonCallback;
    }

    /**
     * 停止
     *
     * @param resetText 重置文本
     */
    public void stop(String resetText) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            post(() -> stop(resetText));
            return;
        }
        // 1. 恢复空闲状态与颜色动画
        setShimmerButtonStateAndText(ShimmerButtonState.IDLE, resetText);
        // 2. 主动触发拍照停止
        if (onShimmerButtonCallback != null) {
            onShimmerButtonCallback.onShimmerButtonStop();
        }
    }

    /**
     * 停止
     */
    public void stop() {
        stop(this.idleText);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacksAndMessages(null);
        if (runningAnimator != null) {
            runningAnimator.cancel();
        }
        if (chargeAnimator != null) {
            chargeAnimator.cancel();
        }
        if (colorAnimator != null) {
            colorAnimator.cancel();
        }
        if (scaleAnimator != null) {
            scaleAnimator.cancel();
        }
    }
}