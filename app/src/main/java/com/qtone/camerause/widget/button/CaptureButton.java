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
 * @desc 拍照按钮
 */
public class CaptureButton extends View {
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
     * 单拍蓝
     * <p>
     * 默认颜色标准
     */
    private int colorPrimary = Color.parseColor("#29A1F7");
    /**
     * 连拍橙
     * <p>
     * 默认颜色标准
     */
    private int colorBurst = Color.parseColor("#FF9800");
    /**
     * 禁用灰
     * <p>
     * 默认颜色标准
     */
    private int colorDisabled = Color.parseColor("#B0BEC5");
    /**
     * 文本色
     */
    private int textColor = Color.WHITE;
    /**
     * 当前拍照按钮状态
     */
    private CaptureButtonState currentCaptureButtonState = CaptureButtonState.IDLE_SINGLE_CAPTURE;
    /**
     * 当前拍照按钮触发模式
     */
    private CaptureButtonTriggerMode currentCaptureButtonTriggerMode = CaptureButtonTriggerMode.CLICK;
    /**
     * 当前拍照按钮形状
     */
    private CaptureButtonShape currentCaptureButtonShape = CaptureButtonShape.CIRCLE;
    /**
     * 方形圆角半径
     * <p>
     * 默认 12dp
     */
    private float cornerRadius = 0f;
    /**
     * 自定义外环圆圈宽度
     * <p>
     * 默认 -1 表示按比例自适应计算
     */
    private float customRingWidth = -1f;
    /**
     * 自定义中间图像和外环之间的距离
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
     * 默认 -1 表示跟随系统按比例计算
     */
    private float customTextSize = -1f;
    /**
     * 动画控制
     */
    private ValueAnimator colorAnimator;
    private ValueAnimator scaleAnimator;
    /**
     * 流光无限旋转动画
     */
    private ValueAnimator runningAnimator;
    /**
     * 蓄力 0~360° 动画
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
    private String text = "拍摄";
    /**
     * 记录空闲状态下的文本
     * <p>
     * 便于停止时恢复
     */
    private String idleText = "拍摄";
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
     * 拍照按钮回调
     */
    private OnCaptureButtonCallback onCaptureButtonCallback;
    /**
     * 长按任务
     */
    private final Runnable longPressRunnable = this::startChargingAnimation;

    /**
     * constructor
     *
     * @param context 上下文
     */
    public CaptureButton(Context context) {
        this(context, null);
    }

    /**
     * constructor
     *
     * @param context 上下文
     * @param attrs   属性集
     */
    public CaptureButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * constructor
     *
     * @param context      上下文
     * @param attrs        属性集
     * @param defStyleAttr 默认样式属性
     */
    public CaptureButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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
        cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CaptureButton);
            if (typedArray.hasValue(R.styleable.CaptureButton_cbText)) {
                String xmlText = typedArray.getString(R.styleable.CaptureButton_cbText);
                if (xmlText != null) {
                    text = xmlText;
                    idleText = xmlText;
                }
            }
            textColor = typedArray.getColor(R.styleable.CaptureButton_cbTextColor, textColor);
            customTextSize = typedArray.getDimension(R.styleable.CaptureButton_cbTextSize, -1f);
            colorPrimary = typedArray.getColor(R.styleable.CaptureButton_cbSingleCaptureColor, colorPrimary);
            colorBurst = typedArray.getColor(R.styleable.CaptureButton_cbBurstCaptureColor, colorBurst);
            colorDisabled = typedArray.getColor(R.styleable.CaptureButton_cbDisabledColor, colorDisabled);
            int modeVal = typedArray.getInt(R.styleable.CaptureButton_cbCaptureMode, 0);
            currentCaptureButtonState = (modeVal == 1) ? CaptureButtonState.IDLE_BURST_CAPTURE : CaptureButtonState.IDLE_SINGLE_CAPTURE;
            int triggerVal = typedArray.getInt(R.styleable.CaptureButton_cbTriggerMode, 0);
            currentCaptureButtonTriggerMode = CaptureButtonTriggerMode.fromValue(triggerVal);
            int shapeVal = typedArray.getInt(R.styleable.CaptureButton_cbShape, 0);
            currentCaptureButtonShape = CaptureButtonShape.fromValue(shapeVal);
            cornerRadius = typedArray.getDimension(R.styleable.CaptureButton_cbCornerRadius, cornerRadius);
            chargeDuration = typedArray.getInt(R.styleable.CaptureButton_cbChargeDuration, (int) chargeDuration);
            rotateDuration = typedArray.getInt(R.styleable.CaptureButton_cbRotateDuration, (int) rotateDuration);
            customRingWidth = typedArray.getDimension(R.styleable.CaptureButton_cbRingWidth, -1f);
            customInnerPadding = typedArray.getDimension(R.styleable.CaptureButton_cbInnerPadding, -1f);
            typedArray.recycle();
        }
        currentInnerColor = (currentCaptureButtonState == CaptureButtonState.IDLE_BURST_CAPTURE) ? colorBurst : colorPrimary;
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
     * 构建外围方形 Path (包含圆角)
     */
    private void setupSquarePath() {
        if (outerRadius <= 0) {
            return;
        }
        outerSquarePath.reset();
        float outerStrokeHalf = outerRingPaint.getStrokeWidth() / 2f;
        float rectSize = outerRadius - outerStrokeHalf;
        innerRectBounds.set(centerX - rectSize, centerY - rectSize, centerX + rectSize, centerY + rectSize);
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
        if (currentCaptureButtonShape == CaptureButtonShape.SQUARE) {
            canvas.drawPath(outerSquarePath, outerRingPaint);
        } else {
            canvas.drawCircle(centerX, centerY, outerRadius, outerRingPaint);
        }
        // 2. 蓄力阶段
        // 绘制随时间填满的彩色弧线 / 轨迹
        if (isCharging) {
            loadingArcPaint.setShader(null);
            loadingArcPaint.setColor(getActiveColor());
            if (currentCaptureButtonShape == CaptureButtonShape.SQUARE) {
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
            if (currentCaptureButtonShape == CaptureButtonShape.SQUARE) {
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
        float currentRadius = innerRadiusBase * currentInnerScale;
        innerCirclePaint.setColor(currentInnerColor);
        if (currentCaptureButtonShape == CaptureButtonShape.SQUARE) {
            float scaledCornerRadius = cornerRadius * currentInnerScale;
            innerRectBounds.set(centerX - currentRadius, centerY - currentRadius, centerX + currentRadius, centerY + currentRadius);
            canvas.drawRoundRect(innerRectBounds, scaledCornerRadius, scaledCornerRadius, innerCirclePaint);
        } else {
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
     */
    private int getActiveColor() {
        return ((currentCaptureButtonState == CaptureButtonState.BURST_CAPTURE_RUNNING) || (currentCaptureButtonState == CaptureButtonState.IDLE_BURST_CAPTURE)) ? colorBurst : colorPrimary;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return super.onTouchEvent(event);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if ((currentCaptureButtonState == CaptureButtonState.SINGLE_CAPTURE_RUNNING) || (currentCaptureButtonState == CaptureButtonState.STOPPING)) {
                    return true;
                }
                if (currentCaptureButtonState == CaptureButtonState.BURST_CAPTURE_RUNNING) {
                    animateInnerScale(0.88f, 100, new DecelerateInterpolator());
                    return true;
                }
                if ((onCaptureButtonCallback != null) && !onCaptureButtonCallback.onCaptureButtonPreCheck()) {
                    return false;
                }
                isTouchDown = true;
                isChargedTriggered = false;
                // 按下缩小效果
                animateInnerScale(0.88f, 100, new DecelerateInterpolator());
                if (currentCaptureButtonTriggerMode == CaptureButtonTriggerMode.LONG_PRESS_CHARGE) {
                    handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout());
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if ((currentCaptureButtonState == CaptureButtonState.SINGLE_CAPTURE_RUNNING) || (currentCaptureButtonState == CaptureButtonState.STOPPING)) {
                    return true;
                }
                // 松开弹回效果
                animateInnerScale(1.0f, 200, new OvershootInterpolator(2.0f));
                if (isChargedTriggered) {
                    isChargedTriggered = false;
                    isTouchDown = false;
                    return true;
                }
                if (currentCaptureButtonState == CaptureButtonState.BURST_CAPTURE_RUNNING) {
                    setCaptureButtonState(CaptureButtonState.IDLE_BURST_CAPTURE, idleText);
                    if (onCaptureButtonCallback != null) {
                        onCaptureButtonCallback.onCaptureButtonStop();
                    }
                    return true;
                }
                isTouchDown = false;
                if (currentCaptureButtonTriggerMode == CaptureButtonTriggerMode.LONG_PRESS_CHARGE) {
                    handler.removeCallbacks(longPressRunnable);
                    if (isCharging) {
                        cancelChargingAnimation();
                    }
                } else if ((currentCaptureButtonTriggerMode == CaptureButtonTriggerMode.CLICK) && !isBusy() && (event.getAction() == MotionEvent.ACTION_UP)) {
                    triggerCapture();
                }
                return true;
        }
        return super.onTouchEvent(event);
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
                    triggerCapture();
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
        if (onCaptureButtonCallback != null) {
            onCaptureButtonCallback.onCaptureButtonChargeCancel();
        }
    }

    /**
     * 内部触发拍照逻辑
     */
    private void triggerCapture() {
        CaptureButtonState targetCaptureButtonState = (currentCaptureButtonState == CaptureButtonState.IDLE_BURST_CAPTURE) ? CaptureButtonState.BURST_CAPTURE_RUNNING : CaptureButtonState.SINGLE_CAPTURE_RUNNING;
        setCaptureButtonState(targetCaptureButtonState, this.text);
        if (onCaptureButtonCallback != null) {
            onCaptureButtonCallback.onCaptureButtonStart(targetCaptureButtonState);
        }
    }

    /**
     * 判断当前是否正在运行拍照/处理中
     *
     * @return boolean
     */
    public boolean isBusy() {
        return ((currentCaptureButtonState == CaptureButtonState.SINGLE_CAPTURE_RUNNING) || (currentCaptureButtonState == CaptureButtonState.BURST_CAPTURE_RUNNING) || (currentCaptureButtonState == CaptureButtonState.STOPPING));
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
     * 触发连拍一次脉冲缩放效果 (供外部在每拍完一张照片时调用)
     */
    public void triggerBurstPulse() {
        animateInnerScale(0.82f, 80, new DecelerateInterpolator());
        postDelayed(() -> animateInnerScale(1.0f, 120, new OvershootInterpolator(1.5f)), 80);
    }

    /**
     * 获取当前按钮状态
     *
     * @return 按钮状态
     */
    public CaptureButtonState getCaptureButtonState() {
        return currentCaptureButtonState;
    }

    /**
     * 设置按钮状态与文字
     *
     * @param captureButtonState 按钮状态
     * @param buttonText         按钮文本
     */
    public void setCaptureButtonState(CaptureButtonState captureButtonState, String buttonText) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            post(() -> setCaptureButtonState(captureButtonState, buttonText));
            return;
        }
        this.text = buttonText;
        if ((captureButtonState == CaptureButtonState.IDLE_SINGLE_CAPTURE) || (captureButtonState == CaptureButtonState.IDLE_BURST_CAPTURE)) {
            this.idleText = buttonText;
        }
        if (this.currentCaptureButtonState == captureButtonState) {
            updateLoadingAnimation();
            invalidate();
            return;
        }
        this.currentCaptureButtonState = captureButtonState;
        int targetColor;
        switch (captureButtonState) {
            case BURST_CAPTURE_RUNNING:
            case IDLE_BURST_CAPTURE:
                targetColor = colorBurst;
                break;
            case SINGLE_CAPTURE_RUNNING:
            case IDLE_SINGLE_CAPTURE:
                targetColor = colorPrimary;
                break;
            case STOPPING:
            default:
                targetColor = colorDisabled;
                break;
        }
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
     * 设置单拍主体颜色
     *
     * @param color 颜色
     */
    public void setSingleCaptureColor(int color) {
        this.colorPrimary = color;
        if (!isBusy() && (currentCaptureButtonState != CaptureButtonState.IDLE_BURST_CAPTURE)) {
            this.currentInnerColor = color;
        }
        invalidate();
    }

    /**
     * 设置连拍主体颜色
     *
     * @param color 颜色
     */
    public void setBurstCaptureColor(int color) {
        this.colorBurst = color;
        if (!isBusy() && (currentCaptureButtonState == CaptureButtonState.IDLE_BURST_CAPTURE)) {
            this.currentInnerColor = color;
        }
        invalidate();
    }

    /**
     * 设置停止中主体颜色
     *
     * @param color 颜色
     */
    public void setDisabledColor(int color) {
        this.colorDisabled = color;
        invalidate();
    }

    /**
     * 获取当前拍照按钮触发模式
     *
     * @return 当前拍照按钮触发模式
     */
    public CaptureButtonTriggerMode getCurrentCaptureButtonTriggerMode() {
        return currentCaptureButtonTriggerMode;
    }

    /**
     * 设置拍照按钮触发模式
     *
     * @param captureButtonTriggerMode 拍照按钮触发模式
     */
    public void setCurrentCaptureButtonTriggerMode(CaptureButtonTriggerMode captureButtonTriggerMode) {
        this.currentCaptureButtonTriggerMode = captureButtonTriggerMode;
    }

    /**
     * 获取拍照按钮形状
     *
     * @return 拍照按钮形状
     */
    public CaptureButtonShape getCaptureButtonShape() {
        return currentCaptureButtonShape;
    }

    /**
     * 设置拍照按钮形状
     *
     * @param captureButtonShape 拍照按钮形状
     */
    public void setCaptureButtonShape(CaptureButtonShape captureButtonShape) {
        this.currentCaptureButtonShape = captureButtonShape;
        setupSquarePath();
        invalidate();
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
     * 设置拍照按钮回调
     *
     * @param onCaptureButtonCallback 拍照按钮回调
     */
    public void setOnCaptureButtonCallback(OnCaptureButtonCallback onCaptureButtonCallback) {
        this.onCaptureButtonCallback = onCaptureButtonCallback;
    }

    /**
     * 停止拍照
     *
     * @param resetText 重置文本
     */
    public void stopCapture(String resetText) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            post(() -> stopCapture(resetText));
            return;
        }
        CaptureButtonState targetCaptureButtonState = ((currentCaptureButtonState == CaptureButtonState.IDLE_BURST_CAPTURE) || (currentCaptureButtonState == CaptureButtonState.BURST_CAPTURE_RUNNING) || (currentInnerColor == colorBurst))
                ? CaptureButtonState.IDLE_BURST_CAPTURE
                : CaptureButtonState.IDLE_SINGLE_CAPTURE;
        // 1. 恢复空闲状态与颜色动画
        setCaptureButtonState(targetCaptureButtonState, resetText);
        // 2. 主动触发拍照停止
        if (onCaptureButtonCallback != null) {
            onCaptureButtonCallback.onCaptureButtonStop();
        }
    }

    /**
     * 停止拍照
     */
    public void stopCapture() {
        stopCapture(this.idleText);
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