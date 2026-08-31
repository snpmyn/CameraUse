package com.qtone.camerause.widget.button;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
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
 * @desc 相机拍照按钮
 */
public class CaptureButton extends View {
    private final RectF ringBounds = new RectF();
    private final Matrix gradientMatrix = new Matrix();
    private final Handler handler = new Handler(Looper.getMainLooper());
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
    private int textColor = Color.WHITE;
    /**
     * 当前状态
     */
    private State currentState = State.IDLE_SINGLE;
    /**
     * 当前触发模式
     */
    private TriggerMode currentTriggerMode = TriggerMode.CLICK;
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
     * 渐变与矩阵
     * <p>
     * 用于流光光晕
     */
    private SweepGradient sweepGradient;
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
     * 记录待命状态下的文本
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
    private OnCaptureCallback captureListener;
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
        // 读取 XML 自定属性
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
            // 读取单拍与连拍主题色
            colorPrimary = typedArray.getColor(R.styleable.CaptureButton_cbSingleCaptureColor, colorPrimary);
            colorBurst = typedArray.getColor(R.styleable.CaptureButton_cbBurstCaptureColor, colorBurst);
            colorDisabled = typedArray.getColor(R.styleable.CaptureButton_cbDisabledColor, colorDisabled);

            // 当前状态
            int modeVal = typedArray.getInt(R.styleable.CaptureButton_cbCaptureMode, 0);
            currentState = (modeVal == 1) ? State.IDLE_BURST : State.IDLE_SINGLE;

            // 当前触发模式
            int triggerVal = typedArray.getInt(R.styleable.CaptureButton_cbTriggerMode, 0);
            currentTriggerMode = TriggerMode.fromValue(triggerVal);

            chargeDuration = typedArray.getInt(R.styleable.CaptureButton_cbChargeDuration, (int) chargeDuration);
            rotateDuration = typedArray.getInt(R.styleable.CaptureButton_cbRotateDuration, (int) rotateDuration);

            typedArray.recycle();
        }
        currentInnerColor = (currentState == State.IDLE_BURST) ? colorBurst : colorPrimary;
        // 1. 外层静态轨道
        outerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerRingPaint.setStyle(Paint.Style.STROKE);
        // 2. 中间实心按钮
        innerCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerCirclePaint.setStyle(Paint.Style.FILL);
        innerCirclePaint.setColor(currentInnerColor);
        // 3. 中央文本
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        // 4. 动态外环
        // 流光 / 蓄力
        loadingArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        loadingArcPaint.setStyle(Paint.Style.STROKE);
        loadingArcPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    /**
     * 调整颜色透明度
     *
     * @param color 原始颜色
     * @param alpha 透明度比例
     *              范围 0.0 ~ 1.0
     * @return 调整颜色透明度后的颜色值
     */
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
        float strokeWidth = minSize * 0.06f;
        outerRadius = ((minSize / 2f) - (strokeWidth / 2f) - 4f);
        innerRadiusBase = (outerRadius - strokeWidth * 1.5f);
        outerRingPaint.setStrokeWidth(strokeWidth);
        loadingArcPaint.setStrokeWidth(strokeWidth);
        if (customTextSize > 0) {
            textPaint.setTextSize(customTextSize);
        } else {
            textPaint.setTextSize(minSize * 0.22f);
        }
        ringBounds.set(centerX - outerRadius, centerY - outerRadius, centerX + outerRadius, centerY + outerRadius);
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
        canvas.drawCircle(centerX, centerY, outerRadius, outerRingPaint);
        // 2. 蓄力阶段
        // 绘制 0 -> 360° 实心进度弧线
        if (isCharging) {
            loadingArcPaint.setShader(null);
            loadingArcPaint.setColor(getActiveColor());
            canvas.drawArc(ringBounds, -90f, currentChargeSweepAngle, false, loadingArcPaint);
        }
        // 3. 进行中状态
        // 绘制 SweepGradient 扫掠旋转光晕
        else if (isBusy()) {
            int activeColor = getActiveColor();
            sweepGradient = new SweepGradient(centerX, centerY, new int[]{Color.TRANSPARENT, activeColor}, new float[]{0.2f, 1.0f});
            gradientMatrix.setRotate(currentRotateAngle, centerX, centerY);
            sweepGradient.setLocalMatrix(gradientMatrix);
            loadingArcPaint.setShader(sweepGradient);
            canvas.drawArc(ringBounds, currentRotateAngle, 280f, false, loadingArcPaint);
        }
        // 4. 绘制中间实心圆
        float currentRadius = innerRadiusBase * currentInnerScale;
        innerCirclePaint.setColor(currentInnerColor);
        canvas.drawCircle(centerX, centerY, currentRadius, innerCirclePaint);
        // 5. 绘制居中文本
        if ((text != null) && !text.isEmpty()) {
            textPaint.setColor(textColor);
            Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
            float baseline = (centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f);
            canvas.drawText(text, centerX, baseline, textPaint);
        }
    }

    /**
     * 获取当前活动状态对应的颜色
     *
     * @return 当前活动状态对应的颜色
     */
    private int getActiveColor() {
        return ((currentState == State.BURST_RUNNING) || (currentState == State.IDLE_BURST)) ? colorBurst : colorPrimary;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return super.onTouchEvent(event);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 1. 单拍进行中 / 收尾中
                // 拦截点击，坚决不作任何回应，直至外部通过 setState 重设状态。
                if ((currentState == State.SINGLE_CAPTURING) || (currentState == State.STOPPING)) {
                    return true;
                }
                // 2. 连拍进行中
                // 按下响应缩放动画，等待抬起时直接停止。
                if (currentState == State.BURST_RUNNING) {
                    animateInnerScale(0.88f, 100, new DecelerateInterpolator());
                    return true;
                }
                isTouchDown = true;
                // 重置蓄力成功触发标记
                isChargedTriggered = false;
                animateInnerScale(0.88f, 100, new DecelerateInterpolator());
                // 只有在 LONG_PRESS_CHARGE 模式下才投递蓄力任务
                if (currentTriggerMode == TriggerMode.LONG_PRESS_CHARGE) {
                    handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout());
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 单拍进行中 / 收尾中
                // 忽略抬起事件
                if ((currentState == State.SINGLE_CAPTURING) || (currentState == State.STOPPING)) {
                    return true;
                }
                // 恢复按压缩放
                animateInnerScale(1.0f, 200, new OvershootInterpolator(2.0f));
                // 如果是蓄力成功满一圈后的自然抬手，直接忽略，保持当前拍摄运行状态，等待下次点击停止。
                if (isChargedTriggered) {
                    isChargedTriggered = false;
                    isTouchDown = false;
                    return true;
                }
                // 3. 连拍进行中
                // 主动再次点击并抬手时停止连拍，切回待命并回调停止。
                if (currentState == State.BURST_RUNNING) {
                    setState(State.IDLE_BURST, idleText);
                    if (captureListener != null) {
                        captureListener.onCaptureStop();
                    }
                    return true;
                }
                isTouchDown = false;
                // 待命状态下的抬起处理
                if (currentTriggerMode == TriggerMode.LONG_PRESS_CHARGE) {
                    handler.removeCallbacks(longPressRunnable);
                    if (isCharging) {
                        cancelChargingAnimation();
                    }
                } else if ((currentTriggerMode == TriggerMode.CLICK) && !isBusy()) {
                    // CLICK 模式
                    // 点击直接触发拍摄
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
                    // 蓄力成功满一圈并触发
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
        if (captureListener != null) {
            captureListener.onChargeCancel();
        }
    }

    /**
     * 触发拍摄
     * <p>
     * 根据当前待命状态触发拍摄
     */
    private void triggerCapture() {
        State targetState = (currentState == State.IDLE_BURST) ? State.BURST_RUNNING : State.SINGLE_CAPTURING;
        setState(targetState, this.text);
        if (captureListener != null) {
            captureListener.onCaptureStart(targetState);
        }
    }

    /**
     * 是否忙碌中
     *
     * @return 是否忙碌中
     */
    public boolean isBusy() {
        return ((currentState == State.SINGLE_CAPTURING) || (currentState == State.BURST_RUNNING) || (currentState == State.STOPPING));
    }

    /**
     * 更新进度动画
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
     * 执行内部实心圆按压缩放动画
     *
     * @param targetScale  目标缩放比例
     * @param duration     动画时长
     *                     单位 - 毫秒
     * @param interpolator 插值器
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
     * 触发连拍快门脉冲回弹缩放效果
     * <p>
     * 供外部单张连拍成功时调用
     */
    public void triggerBurstPulse() {
        animateInnerScale(0.82f, 80, new DecelerateInterpolator());
        postDelayed(() -> animateInnerScale(1.0f, 120, new OvershootInterpolator(1.5f)), 80);
    }

    /**
     * 设置状态
     *
     * @param state      状态
     * @param buttonText 按钮文本
     */
    public void setState(State state, String buttonText) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            post(() -> setState(state, buttonText));
            return;
        }
        this.text = buttonText;
        if ((state == State.IDLE_SINGLE) || (state == State.IDLE_BURST)) {
            this.idleText = buttonText;
        }
        if (this.currentState == state) {
            updateLoadingAnimation();
            invalidate();
            return;
        }
        this.currentState = state;
        int targetColor;
        switch (state) {
            case BURST_RUNNING:
            case IDLE_BURST:
                targetColor = colorBurst;
                break;
            case SINGLE_CAPTURING:
            case IDLE_SINGLE:
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
        ArgbEvaluator argbEvaluator = new ArgbEvaluator();
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
     * 获取文本
     *
     * @return 文本
     */
    public String getText() {
        return text;
    }

    /**
     * 设置文本
     *
     * @param text 文本
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
     *              例如 R.dimen.sp_16
     */
    public void setTextSize(@androidx.annotation.DimenRes int resId) {
        try {
            // getDimension() 自动将 SP 转换为对应 PX 像素值
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
     * 设置单拍颜色
     *
     * @param color 颜色
     */
    public void setSingleCaptureColor(int color) {
        this.colorPrimary = color;
        if (!isBusy() && (currentState != State.IDLE_BURST)) {
            this.currentInnerColor = color;
        }
        invalidate();
    }

    /**
     * 设置连拍颜色
     *
     * @param color 颜色
     */
    public void setBurstCaptureColor(int color) {
        this.colorBurst = color;
        if (!isBusy() && (currentState == State.IDLE_BURST)) {
            this.currentInnerColor = color;
        }
        invalidate();
    }

    /**
     * 设置禁用颜色
     * <p>
     * 禁用 / 收尾
     *
     * @param color 颜色
     */
    public void setDisabledColor(int color) {
        this.colorDisabled = color;
        invalidate();
    }

    /**
     * 获取当前触发模式
     *
     * @return 当前触发模式
     */
    public TriggerMode getCurrentTriggerMode() {
        return currentTriggerMode;
    }

    /**
     * 设置当前触发模式
     *
     * @param triggerMode 触发模式
     */
    public void setCurrentTriggerMode(TriggerMode triggerMode) {
        this.currentTriggerMode = triggerMode;
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
     * 设置拍照回调
     *
     * @param onCaptureCallback 拍照回调
     */
    public void setOnCaptureCallback(OnCaptureCallback onCaptureCallback) {
        this.captureListener = onCaptureCallback;
    }

    /**
     * 获取状态
     *
     * @return 状态
     */
    public State getState() {
        return currentState;
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
        State targetState = ((currentState == State.IDLE_BURST) || (currentState == State.BURST_RUNNING) || (currentInnerColor == colorBurst))
                ? State.IDLE_BURST
                : State.IDLE_SINGLE;
        // 1. 恢复待命状态与颜色动画
        setState(targetState, resetText);
        // 2. 主动触发拍照停止
        if (captureListener != null) {
            captureListener.onCaptureStop();
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

    /**
     * 触发模式
     */
    public enum TriggerMode {
        /**
         * 普通点击触发
         * <p>
         * 默认 + 点一下立刻触发
         */
        CLICK(0),
        /**
         * 长按蓄力触发
         * <p>
         * 按住走满一圈触发 + 中途松手取消
         */
        LONG_PRESS_CHARGE(1);
        /**
         * 值
         */
        final int value;

        /**
         * constructor
         *
         * @param value 值
         */
        TriggerMode(int value) {
            this.value = value;
        }

        /**
         * 从值
         *
         * @param val 变量
         * @return 触发模式
         */
        static TriggerMode fromValue(int val) {
            for (TriggerMode triggerMode : values()) {
                if (triggerMode.value == val) {
                    return triggerMode;
                }
            }
            return CLICK;
        }
    }

    /**
     * 状态
     */
    public enum State {
        /**
         * 单拍待命
         */
        IDLE_SINGLE,
        /**
         * 连拍待命
         */
        IDLE_BURST,
        /**
         * 单拍进行中
         */
        SINGLE_CAPTURING,
        /**
         * 连拍进行中
         */
        BURST_RUNNING,
        /**
         * 停止收尾中
         */
        STOPPING
    }

    /**
     * 拍照回调
     */
    public interface OnCaptureCallback {
        /**
         * 拍照开始
         *
         * @param targetState 当前状态
         */
        void onCaptureStart(State targetState);

        /**
         * 蓄力中途取消
         */
        void onChargeCancel();

        /**
         * 拍照停止
         */
        void onCaptureStop();
    }
}