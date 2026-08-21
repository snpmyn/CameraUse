package com.qtone.camerause.widget.image;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.qtone.camerause.R;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import timber.log.Timber;

/**
 * Created on 2026/6/5.
 *
 * @author 郑少鹏
 * @desc 图片查看浮层
 */
public class ImageViewerOverlay extends FrameLayout implements View.OnTouchListener {
    /**
     * 线程池句柄
     * <p>
     * 固定容量为 4 的并行线程池
     * 用于原生网络 (NATIVE) 字节流异步拉取
     */
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(4);
    /**
     * 空闲状态
     */
    private static final int TOUCH_NONE = 0;
    /**
     * 单指平移拖拽状态
     */
    private static final int TOUCH_DRAG = 1;
    /**
     * 双指多点缩放捏合状态
     */
    private static final int TOUCH_PINCH = 2;
    /**
     * 单指下拉退出交互状态
     */
    private static final int TOUCH_EXIT_DRAG = 3;
    /**
     * 双击判定死区时间间隔上限
     * <p>
     * 毫秒
     */
    private static final long DOUBLE_TAP_MS = 300;
    /**
     * 主线程调度器
     * <p>
     * 用于 safe 分发异步加载成功的 Bitmap 资产
     */
    private final Handler handler = new Handler(Looper.getMainLooper());
    /**
     * 全局核心变换矩阵
     * <p>
     * 承载当前视图渲染的最终位置、缩放比例及旋转状态
     */
    private final Matrix matrix = new Matrix();
    /**
     * 矩阵状态快照缓存
     * <p>
     * 手势初次按下时捕获的干净矩阵
     * 作为差分计算的绝对基准
     */
    private final Matrix savedMatrix = new Matrix();
    /**
     * 手势落点绝对起始坐标点
     */
    private final PointF downTouch = new PointF();
    /**
     * 上一次触摸触发的局部坐标点
     */
    private final PointF lastTouch = new PointF();
    /**
     * 双指触控的中心物理坐标点
     */
    private final PointF midPoint = new PointF();
    /**
     * 系统级触发拖拽动作的滑行死区阈值
     * <p>
     * 用于过滤微小颤抖
     */
    private final float touchSlop;
    /**
     * 核心资产加载策略分流器
     */
    private LoadStrategy loadStrategy = LoadStrategy.NATIVE;
    /**
     * 异步任务持有句柄
     * <p>
     * 用于在组件销毁或重置时强行中断未完成的网络流
     */
    private Future<?> loadFuture;
    /**
     * 最小缩放绝对限制阈值
     */
    private float minScale = 0.3f;
    /**
     * 最大缩放绝对限制阈值
     */
    private float maxScale = 6.0f;
    /**
     * 双击放大的目标缩放基准比例倍数
     */
    private float doubleTapTargetScale = 2.5f;
    /**
     * 在单指平移时
     * <p>
     * 允许图片超出视窗边界的最大安全物理阻尼距离
     */
    private float overscrollLimit = 120f;
    /**
     * 越界拖拽或缩放释放后
     * <p>
     * 弹性复原动画的持续时间
     * 毫秒
     */
    private int springDuration = 260;
    /**
     * 控制按钮响应或双击缩放矩阵平滑过渡的动画持续时间
     * <p>
     * 毫秒
     */
    private int zoomAnimDuration = 300;
    /**
     * 单指处于下拉退出状态时
     * <p>
     * 纵向位移超过视窗总高度的临界比例
     * 0.0f - 1.0f
     */
    private float exitDragThreshold = 0.35f;
    /**
     * 全屏容器根布局
     */
    private View imageViewerOverlayFl;
    /**
     * 自定义高级变换图像视窗
     * <p>
     * 承载矩阵变化的核心 View
     */
    private AccessibleImageView imageViewerOverlayView;
    /**
     * 当前视图的绝对旋转角度值
     * <p>
     * 在 0f、90f、180f、270f 间流转
     * 消除浮点数累加误差
     */
    private float currentRotation = 0f;
    /**
     * 长图鉴别标识
     * <p>
     * 宽高比 >= 2.2 时激活
     * 初始化展示将顶部对齐并改变平移边界机制
     */
    private boolean isLongImage = false;
    /**
     * 全屏自适应基准缩放比
     * <p>
     * 根据图片宽高与宿主视窗宽高解算后的初始无损缩放系数
     */
    private float baseScale = 1.0f;
    /**
     * 当前手势交互所处的引擎模式
     * <p>
     * TOUCH_NONE、TOUCH_DRAG 等
     */
    private int touchMode = TOUCH_NONE;
    /**
     * 双指初次落点时的物理跨度跨两点间直线距离
     */
    private float initDist = 1f;
    /**
     * 上一次单击触发的时间戳
     * <p>
     * 用于双击判定
     */
    private long lastTapTime = 0;
    /**
     * 销毁及关闭事件外置回调监听器
     */
    private OnCloseListener onCloseListener;
    /**
     * 旋转动画专用调度控制柄
     * <p>
     * 用于快速连续点击旋转时强行中断前置未完动画
     * 规避残影
     */
    private ValueAnimator rotationAnimator;
    /**
     * 缩放动画专用调度控制柄
     * <p>
     * 用于快速连续点击缩放时强行中断前置未完动画
     * 规避因状态锁导致的响应延迟
     */
    private ValueAnimator zoomAnimator;
    /**
     * 动画执行锁状态机
     * <p>
     * 用于拦截矩阵平滑过渡期间产生的高频微调请求
     * 避免物理越界与过渡死锁
     */
    private boolean isAnimating = false;
    /**
     * 骨架层级加载反馈加载条
     */
    private ProgressBar imageViewerOverlayPb;
    /**
     * 骨架层级错误状态文本提示器
     */
    private TextView imageViewerOverlayTv;

    /**
     * 构造函数
     *
     * @param context 上下文
     */
    public ImageViewerOverlay(Context context) {
        this(context, null);
    }

    /**
     * 构造函数
     *
     * @param context      上下文
     * @param attributeSet AttributeSet
     */
    public ImageViewerOverlay(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        // 获取系统级滑动死区
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        inflate(context);
    }

    /**
     * 计算多点触控两点间的直线物理跨度距离
     *
     * @param motionEvent 触控事件
     * @return 跨度距离值
     */
    private static float spacing(@NonNull MotionEvent motionEvent) {
        float dx = (motionEvent.getX(0) - motionEvent.getX(1));
        float dy = (motionEvent.getY(0) - motionEvent.getY(1));
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 动态解算双指触控的中点物理坐标
     *
     * @param pointF      承载结果的容器点
     * @param motionEvent 触控事件
     */
    private static void midPoint(@NonNull PointF pointF, @NonNull MotionEvent motionEvent) {
        pointF.set((motionEvent.getX(0) + motionEvent.getX(1)) / 2f, (motionEvent.getY(0) + motionEvent.getY(1)) / 2f);
    }

    /**
     * 矩阵缩放比例提取算法
     * <p>
     * 提取 X 轴的主缩放分量与 Y 轴的切变分量
     * 通过勾股定理计算出无视旋转角度干扰的真实绝对复合缩放因子
     *
     * @param matrix 变换矩阵
     * @return 当前 Matrix 产生的物理图像真实拉伸比例
     */
    private static float getMatrixScale(@NonNull Matrix matrix) {
        float[] v = new float[9];
        matrix.getValues(v);
        return (float) Math.sqrt(v[Matrix.MSCALE_X] * v[Matrix.MSCALE_X] + v[Matrix.MSKEW_Y] * v[Matrix.MSKEW_Y]);
    }

    /**
     * 工作线程同步读取网络文件字节流并解码为 Bitmap 资产
     *
     * @param urlString     图片链接
     * @param weakReference 弱引用持有
     *                      防止内存泄漏
     */
    private static void loadUrlInBackground(String urlString, WeakReference<ImageViewerOverlay> weakReference) {
        Bitmap bitmap;
        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;
        try {
            URL url = new URL(urlString);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(10_000);
            httpURLConnection.setReadTimeout(15_000);
            httpURLConnection.setDoInput(true);
            httpURLConnection.connect();
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            inputStream = httpURLConnection.getInputStream();
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {
            bitmap = null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception ignored) {
            }
            try {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            } catch (Exception exception) {
                Timber.e(exception);
            }
        }
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        final Bitmap resultBitmap = bitmap;
        ImageViewerOverlay imageViewerOverlay = weakReference.get();
        if (imageViewerOverlay != null) {
            imageViewerOverlay.handler.post(() -> {
                if (imageViewerOverlay.isAttachedToWindow()) {
                    imageViewerOverlay.onImageLoaded(resultBitmap);
                }
            });
        }
    }

    /**
     * 初始化视图并绑定各交互控制单元的点击拦截总线
     *
     * @param context 上下文
     */
    private void inflate(Context context) {
        LayoutInflater.from(context).inflate(R.layout.image_viewer_overlay, this, true);
        imageViewerOverlayFl = findViewById(R.id.imageViewerOverlayFl);
        imageViewerOverlayView = findViewById(R.id.imageViewerOverlayView);
        imageViewerOverlayPb = findViewById(R.id.imageViewerOverlayPb);
        imageViewerOverlayTv = findViewById(R.id.imageViewerOverlayTv);
        if (imageViewerOverlayFl == null) {
            imageViewerOverlayFl = this;
        }
        // 控制按钮点击分发
        findViewById(R.id.imageViewerOverlayRlClose).setOnClickListener(v -> dismiss());
        findViewById(R.id.imageViewerOverlayRlShrink).setOnClickListener(v -> zoomOutByButton(0.3f));
        findViewById(R.id.imageViewerOverlayRlTurnLeft).setOnClickListener(v -> rotateByMatrix(-90));
        findViewById(R.id.imageViewerOverlayRlTurnRight).setOnClickListener(v -> rotateByMatrix(90));
        findViewById(R.id.imageViewerOverlayRlEnlarge).setOnClickListener(v -> zoomInByButton(0.3f));
        // 显式将本类手势内核挂载到图像视窗的触摸反馈总线上
        imageViewerOverlayView.setOnTouchListener(this);
    }

    /**
     * View.OnTouchListener 触摸事件拦截总线实现
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return handleImageTouch(v, event);
    }

    /**
     * 控制按钮对外公开接口
     * <p>
     * 步进放大
     *
     * @param stepFactor 放大比例步进增量系数
     */
    public void zoomInByButton(float stepFactor) {
        if (imageViewerOverlayView.getDrawable() == null) {
            return;
        }
        float currentScale = getMatrixScale(matrix);
        float targetScale = Math.min(maxScale, currentScale + (baseScale * stepFactor));
        executeSmoothZoom(targetScale);
    }

    /**
     * 控制按钮对外公开接口
     * <p>
     * 步进缩小
     *
     * @param stepFactor 缩小比例步进减量系数
     */
    public void zoomOutByButton(float stepFactor) {
        if (imageViewerOverlayView.getDrawable() == null) {
            return;
        }
        float currentScale = getMatrixScale(matrix);
        // 核心对齐修改点：计算按钮缩小的绝对限制极限
        float targetScale = Math.max(baseScale * 0.4f, currentScale - (baseScale * stepFactor));
        executeSmoothZoom(targetScale);
    }

    /**
     * 触发围绕中心点的平滑矩阵自适应缩放过渡
     *
     * @param targetScale 绝对目标缩放比系数
     */
    private void executeSmoothZoom(float targetScale) {
        float currentScale = getMatrixScale(matrix);
        if (currentScale <= 0) {
            return;
        }
        float factor = (targetScale / currentScale);
        PointF center = getImageCenter();
        Matrix targetMatrix = new Matrix(matrix);
        targetMatrix.postScale(factor, factor, center.x, center.y);
        boolean oldAnimating = isAnimating;
        isAnimating = false;
        RectF targetRect = getImageRect(targetMatrix);
        float overX = calcOverscroll(targetRect, true);
        float overY = calcOverscroll(targetRect, false);
        targetMatrix.postTranslate(-overX, -overY);
        isAnimating = oldAnimating;
        animateMatrix(matrix, targetMatrix);
    }

    public void setLoadStrategy(LoadStrategy strategy) {
        this.loadStrategy = strategy;
    }

    public void setExitDragThreshold(float threshold) {
        this.exitDragThreshold = threshold;
    }

    public void setMinScale(float minScale) {
        this.minScale = minScale;
    }

    public void setMaxScale(float maxScale) {
        this.maxScale = maxScale;
    }

    public void setDoubleTapTargetScale(float doubleTapTargetScale) {
        this.doubleTapTargetScale = doubleTapTargetScale;
    }

    public void setOverscrollLimit(float overscrollLimit) {
        this.overscrollLimit = overscrollLimit;
    }

    public void setSpringDuration(int springDuration) {
        this.springDuration = springDuration;
    }

    public void setZoomAnimDuration(int zoomAnimDuration) {
        this.zoomAnimDuration = zoomAnimDuration;
    }

    /**
     * 装载并拉起大图预览浮层
     *
     * @param rootView 上层挂载的根容器视图
     * @param imageUrl 大图的目标网络 URL
     */
    public void show(ViewGroup rootView, String imageUrl) {
        resetState();
        if (getParent() == null) {
            this.setClickable(true);
            this.setFocusable(true);
            rootView.addView(this, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        if ((null != loadFuture) && !loadFuture.isDone()) {
            loadFuture.cancel(true);
        }
        if (loadStrategy == LoadStrategy.GLIDE) {
            executeGlideLoad(imageUrl);
        } else {
            executeNativeLoad(imageUrl);
        }
    }

    /**
     * 执行原生线程池流式下载及解码任务
     *
     * @param imageUrl 图片链接
     */
    private void executeNativeLoad(String imageUrl) {
        WeakReference<ImageViewerOverlay> weakOverlay = new WeakReference<>(this);
        loadFuture = EXECUTOR_SERVICE.submit(() -> loadUrlInBackground(imageUrl, weakOverlay));
    }

    /**
     * 执行三方库 Glide 的图片拓扑加载逻辑
     *
     * @param imageUrl 图片链接
     */
    private void executeGlideLoad(String imageUrl) {
        Glide.with(getContext()).asBitmap().load(imageUrl).into(new com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                onImageLoaded(resource);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                imageViewerOverlayView.setImageBitmap(null);
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                imageViewerOverlayPb.setVisibility(View.GONE);
                imageViewerOverlayTv.setText(getContext().getString(R.string.loadImageFail));
                imageViewerOverlayTv.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * 强行优雅中断并离架销毁当前图片组件
     * <p>
     * 解绑资源保护内存
     */
    public void dismiss() {
        if (rotationAnimator != null) {
            rotationAnimator.cancel();
        }
        if (zoomAnimator != null) {
            zoomAnimator.cancel();
        }
        handler.removeCallbacksAndMessages(null);
        if (loadStrategy == LoadStrategy.GLIDE) {
            Glide.with(getContext()).clear(imageViewerOverlayView);
        } else {
            if (loadFuture != null) {
                loadFuture.cancel(true);
            }
        }
        if (getParent() instanceof ViewGroup) {
            ((ViewGroup) getParent()).removeView(this);
        }
        if (onCloseListener != null) {
            onCloseListener.onClose();
        }
    }

    /**
     * 设置关闭监听
     *
     * @param onCloseListener 关闭监听
     */
    public void setOnCloseListener(OnCloseListener onCloseListener) {
        this.onCloseListener = onCloseListener;
    }

    /**
     * 指定绝对数值进行缩放重组
     * <p>
     * 并自动拉起物理修正弹回动画
     *
     * @param targetScale 绝对目标缩放值
     */
    public void setScale(float targetScale) {
        targetScale = Math.max(minScale, Math.min(maxScale, targetScale));
        float curScale = getMatrixScale(matrix);
        if (curScale <= 0f) {
            return;
        }
        float factor = (targetScale / curScale);
        PointF center = getImageCenter();
        matrix.postScale(factor, factor, center.x, center.y);
        imageViewerOverlayView.setImageMatrix(matrix);
        springBack();
    }

    /**
     * 获取当前图像绝对复合真实拉伸比例
     */
    public float getCurrentScale() {
        return getMatrixScale(matrix);
    }

    /**
     * 核心旋转控制引擎
     * <p>
     * 防变斜、防多层动画叠加闪烁抖动
     *
     * @param degrees 相对当前的旋转步进角度值
     *                例如 90f 或 -90f
     */
    private void rotateByMatrix(float degrees) {
        if (imageViewerOverlayView.getDrawable() == null) {
            return;
        }
        if ((rotationAnimator != null) && rotationAnimator.isRunning()) {
            rotationAnimator.cancel();
        }
        handler.removeCallbacksAndMessages(null);
        final Matrix startMatrix = new Matrix(matrix);
        final PointF imgCenter = getImageCenter();
        currentRotation = (currentRotation + degrees) % 360;
        if (currentRotation < 0) {
            currentRotation += 360;
        }
        rotationAnimator = ValueAnimator.ofFloat(0f, degrees);
        rotationAnimator.setDuration(280);
        rotationAnimator.setInterpolator(new DecelerateInterpolator());
        rotationAnimator.addUpdateListener(anim -> {
            float curDelta = (float) anim.getAnimatedValue();
            matrix.set(startMatrix);
            matrix.postRotate(curDelta, imgCenter.x, imgCenter.y);
            imageViewerOverlayView.setImageMatrix(matrix);
        });
        rotationAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                springBack();
            }
        });
        rotationAnimator.start();
    }

    /**
     * 获取当前图片包围盒的物理中心坐标
     */
    @NonNull
    private PointF getImageCenter() {
        RectF rect = getImageRect(matrix);
        return new PointF(rect.centerX(), rect.centerY());
    }

    /**
     * 多点触控与手势交互调度内核
     *
     * @param v     视图
     * @param event 触摸事件
     * @return 是否触摸消费
     */
    private boolean handleImageTouch(View v, MotionEvent event) {
        if (imageViewerOverlayView.getDrawable() == null) {
            return false;
        }
        if (((rotationAnimator != null) && rotationAnimator.isRunning()) || isAnimating) {
            return true;
        }
        boolean handled = false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                long now = System.currentTimeMillis();
                if (now - lastTapTime < DOUBLE_TAP_MS) {
                    touchMode = TOUCH_NONE;
                    handleDoubleTapZoom(event.getX(), event.getY());
                    lastTapTime = 0;
                    return true;
                }
                lastTapTime = now;
                savedMatrix.set(matrix);
                downTouch.set(event.getX(), event.getY());
                lastTouch.set(event.getX(), event.getY());
                touchMode = TOUCH_DRAG;
                handled = true;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() >= 2) {
                    initDist = spacing(event);
                    if (initDist > 5f) {
                        savedMatrix.set(matrix);
                        midPoint(midPoint, event);
                        touchMode = TOUCH_PINCH;
                        handled = true;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - lastTouch.x;
                float dy = event.getY() - lastTouch.y;
                if (touchMode == TOUCH_DRAG) {
                    float curScale = getMatrixScale(matrix);
                    if ((curScale <= baseScale * 1.02f) && ((event.getY() - downTouch.y) > 0) && (Math.abs(event.getY() - downTouch.y) > Math.abs(event.getX() - downTouch.x) * 1.5f)) {
                        touchMode = TOUCH_EXIT_DRAG;
                    } else {
                        Matrix testMatrix = new Matrix(matrix);
                        testMatrix.postTranslate(dx, dy);
                        RectF currentRect = getImageRect(testMatrix);
                        float overX = calcOverscroll(currentRect, true);
                        float overY = calcOverscroll(currentRect, false);
                        if (overX != 0) {
                            dx = dx * (1.0f - Math.min(0.85f, Math.abs(overX) / (overscrollLimit * 2f)));
                        }
                        if (overY != 0) {
                            dy = dy * (1.0f - Math.min(0.85f, Math.abs(overY) / (overscrollLimit * 2f)));
                        }
                        matrix.postTranslate(dx, dy);
                        imageViewerOverlayView.setImageMatrix(matrix);
                    }
                    lastTouch.set(event.getX(), event.getY());
                    handled = true;
                }
                if (touchMode == TOUCH_EXIT_DRAG) {
                    float totalDeltaY = (event.getY() - downTouch.y);
                    float translationY = Math.max(0, totalDeltaY);
                    float exitScale = (1.0f - (translationY / getHeight()) * 0.5f);
                    exitScale = Math.max(0.618f, exitScale);
                    matrix.set(savedMatrix);
                    matrix.postScale(exitScale, exitScale, downTouch.x, downTouch.y);
                    matrix.postTranslate(event.getX() - downTouch.x, translationY);
                    imageViewerOverlayView.setImageMatrix(matrix);
                    float alpha = (1.0f - (translationY / (getHeight() * 0.4f)));
                    imageViewerOverlayFl.setAlpha(Math.max(0f, Math.min(1.0f, alpha)));
                    handled = true;
                } else if (touchMode == TOUCH_PINCH && event.getPointerCount() >= 2) {
                    float newDist = spacing(event);
                    if (newDist > 5f) {
                        float scale = (newDist / initDist);
                        float curScale = getMatrixScale(savedMatrix);
                        float next = (curScale * scale);
                        // 核心对齐修改点
                        // 统一使用 baseScale * 0.4f 作为双指捏合的物理绝对下限
                        // 确保手势跟按钮锁死完全一致
                        float dynamicMinScale = baseScale * 0.4f;
                        if (next < dynamicMinScale) {
                            scale = dynamicMinScale / curScale;
                        }
                        if (next > maxScale) {
                            scale = maxScale / curScale;
                        }
                        matrix.set(savedMatrix);
                        matrix.postScale(scale, scale, midPoint.x, midPoint.y);
                        imageViewerOverlayView.setImageMatrix(matrix);
                    }
                    handled = true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (touchMode == TOUCH_EXIT_DRAG) {
                    float moveY = (event.getY() - downTouch.y);
                    if (moveY > getHeight() * exitDragThreshold) {
                        dismiss();
                    } else {
                        animateBackgroundAlpha(imageViewerOverlayFl.getAlpha());
                        animateMatrix(matrix, savedMatrix);
                    }
                    handled = true;
                } else if (touchMode == TOUCH_DRAG) {
                    float totalX = (event.getX() - downTouch.x);
                    float totalY = (event.getY() - downTouch.y);
                    if ((Math.abs(totalX) < touchSlop) && (Math.abs(totalY) < touchSlop)) {
                        v.performClick();
                    } else {
                        springBack();
                    }
                    handled = true;
                } else if (touchMode != TOUCH_NONE) {
                    springBack();
                    handled = true;
                }
                touchMode = TOUCH_NONE;
                break;
            default:
                break;
        }
        return handled;
    }

    /**
     * 双击行为响应逻辑
     * <p>
     * 自适应双击放大或多尺寸恢复原状
     *
     * @param clickX X 轴点击坐标
     * @param clickY Y 轴点击坐标
     */
    private void handleDoubleTapZoom(float clickX, float clickY) {
        if (imageViewerOverlayView.getDrawable() == null) {
            return;
        }
        float currentScale = getMatrixScale(matrix);
        Matrix targetMatrix = new Matrix();
        if (currentScale > (baseScale * 1.05f)) {
            int bmpW = imageViewerOverlayView.getDrawable().getIntrinsicWidth();
            int bmpH = imageViewerOverlayView.getDrawable().getIntrinsicHeight();
            if (isLongImage) {
                targetMatrix.postScale(baseScale, baseScale);
                float dx = (getWidth() - bmpW * baseScale) / 2f;
                targetMatrix.postTranslate(dx, 0f);
            } else {
                float dx = (getWidth() - bmpW * baseScale) / 2f;
                float dy = (getHeight() - bmpH * baseScale) / 2f;
                targetMatrix.postScale(baseScale, baseScale);
                targetMatrix.postTranslate(dx, dy);
            }
            currentRotation = 0f;
        } else {
            float targetScale = baseScale * doubleTapTargetScale;
            float factor = (targetScale / currentScale);
            targetMatrix.set(matrix);
            targetMatrix.postScale(factor, factor, clickX, clickY);
            boolean oldAnimating = isAnimating;
            isAnimating = false;
            RectF targetRect = getImageRect(targetMatrix);
            float overX = calcOverscroll(targetRect, true);
            float overY = calcOverscroll(targetRect, false);
            isAnimating = oldAnimating;
            targetMatrix.postTranslate(-overX, -overY);
        }
        animateMatrix(matrix, targetMatrix);
    }

    /**
     * 基于矩阵九宫格参数直接进行插值的全属性平滑过渡动画执行器
     *
     * @param startMatrix  变换起始矩阵
     * @param targetMatrix 目标终点绝对矩阵
     */
    private void animateMatrix(@NonNull Matrix startMatrix, @NonNull Matrix targetMatrix) {
        if (zoomAnimator != null && zoomAnimator.isRunning()) {
            zoomAnimator.cancel();
        }
        float[] startVals = new float[9];
        float[] targetVals = new float[9];
        startMatrix.getValues(startVals);
        targetMatrix.getValues(targetVals);
        float[] currentVals = new float[9];
        isAnimating = true;
        zoomAnimator = ValueAnimator.ofFloat(0f, 1f);
        zoomAnimator.setDuration(zoomAnimDuration);
        zoomAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        zoomAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            for (int i = 0; i < 9; i++) {
                currentVals[i] = startVals[i] + (targetVals[i] - startVals[i]) * fraction;
            }
            matrix.setValues(currentVals);
            imageViewerOverlayView.setImageMatrix(matrix);
        });
        zoomAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimating = false;
                springBack();
            }
        });
        zoomAnimator.start();
    }

    /**
     * 下拉未成功退出时
     * <p>
     * 全屏容器背景不透明度的平滑回弹动画
     *
     * @param startAlpha 开始透明度
     */
    private void animateBackgroundAlpha(float startAlpha) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(startAlpha, (float) 1.0);
        valueAnimator.setDuration(200);
        valueAnimator.addUpdateListener(a -> imageViewerOverlayFl.setAlpha((float) a.getAnimatedValue()));
        valueAnimator.start();
    }

    /**
     * 映射出图片当前缩放 / 旋转状态下的实际物理边界矩形围盒 (RectF)
     *
     * @param matrix 矩阵
     * @return 图片当前缩放 / 旋转状态下的实际物理边界矩形围盒 (RectF)
     */
    @NonNull
    private RectF getImageRect(Matrix matrix) {
        if (imageViewerOverlayView.getDrawable() == null) {
            return new RectF();
        }
        RectF rect = new RectF(0, 0, imageViewerOverlayView.getDrawable().getIntrinsicWidth(), imageViewerOverlayView.getDrawable().getIntrinsicHeight());
        matrix.mapRect(rect);
        return rect;
    }

    /**
     * 计算图片物理边界在当前宿主视窗规格中的绝对越界溢出值
     *
     * @param imageRect  待校验的图片围盒矩形
     * @param horizontal 轴向选择
     *                   true 为水平 X 轴
     *                   false 为纵向 Y 轴
     * @return 溢出的物理像素绝对值
     * 0f 代表内容完美契合视窗 -> 未出现物理过边界
     */
    private float calcOverscroll(RectF imageRect, boolean horizontal) {
        if ((touchMode == TOUCH_PINCH) || isAnimating) {
            return 0f;
        }
        if (horizontal) {
            float imgW = imageRect.width();
            float viewW = getWidth();
            if (imgW <= viewW) {
                return (imageRect.left - (viewW - imgW) / 2f);
            } else {
                if (imageRect.left > 0) {
                    return imageRect.left;
                }
                if (imageRect.right < viewW) {
                    return imageRect.right - viewW;
                }
            }
        } else {
            float imgH = imageRect.height();
            float viewH = getHeight();
            if (imgH <= viewH) {
                if (isLongImage && getMatrixScale(matrix) <= baseScale * 1.05f) {
                    return imageRect.top;
                }
                return (imageRect.top - (viewH - imgH) / 2f);
            } else {
                if (imageRect.top > 0) {
                    return imageRect.top;
                }
                if (imageRect.bottom < viewH) {
                    return imageRect.bottom - viewH;
                }
            }
        }
        return 0f;
    }

    /**
     * 执行位置及比例溢出的自适应归位弹性恢复动画
     * <p>
     * Spring Back
     */
    private void springBack() {
        if (isAnimating) return;
        RectF imgRect = getImageRect(matrix);
        int oldMode = touchMode;
        touchMode = TOUCH_NONE;
        float overX = calcOverscroll(imgRect, true);
        float overY = calcOverscroll(imgRect, false);
        touchMode = oldMode;
        if ((overX == 0f) && (overY == 0f)) {
            return;
        }
        final float fixDx = -overX;
        final float fixDy = -overY;
        final Matrix startMatrix = new Matrix(matrix);
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0f, 1f);
        valueAnimator.setDuration(springDuration);
        valueAnimator.setInterpolator(new DecelerateInterpolator(2.0f));
        valueAnimator.addUpdateListener(anim -> {
            float fraction = (float) anim.getAnimatedValue();
            matrix.set(startMatrix);
            matrix.postTranslate(fixDx * fraction, fixDy * fraction);
            imageViewerOverlayView.setImageMatrix(matrix);
        });
        valueAnimator.start();
    }

    /**
     * 核心自适应排版映射引擎
     * <p>
     * 全面适配普通宽高图与非标准比例的长卷试卷对齐
     *
     * @param bitmapWith   源位图资产的真实像素宽度
     * @param bitmapHeight 源位图资产的真实像素高度
     */
    private void centerAndFit(int bitmapWith, int bitmapHeight) {
        if ((bitmapWith <= 0) || (bitmapHeight <= 0) || (getWidth() <= 0) || (getHeight() <= 0)) {
            return;
        }
        float viewW = getWidth();
        float viewH = getHeight();
        float scaleW = viewW / bitmapWith;
        float scaleH = viewH / bitmapHeight;
        isLongImage = (((float) bitmapHeight / bitmapWith) >= 2.2f);
        matrix.reset();
        if (isLongImage) {
            baseScale = scaleW;
            matrix.postScale(baseScale, baseScale);
            float dx = (viewW - bitmapWith * baseScale) / 2f;
            matrix.postTranslate(dx, 0f);
        } else {
            baseScale = Math.min(scaleW, scaleH);
            matrix.postScale(baseScale, baseScale);
            float dx = (viewW - bitmapWith * baseScale) / 2f;
            float dy = (viewH - bitmapHeight * baseScale) / 2f;
            matrix.postTranslate(dx, dy);
        }
        imageViewerOverlayView.setImageMatrix(matrix);
        currentRotation = 0f;
    }

    /**
     * 干净抹除状态机全量参数
     * <p>
     * 初始化环境
     */
    private void resetState() {
        if (rotationAnimator != null) {
            rotationAnimator.cancel();
        }
        if (zoomAnimator != null) {
            zoomAnimator.cancel();
        }
        handler.removeCallbacksAndMessages(null);
        matrix.reset();
        currentRotation = 0f;
        isLongImage = false;
        baseScale = 1.0f;
        touchMode = TOUCH_NONE;
        isAnimating = false;
        imageViewerOverlayFl.setAlpha(1.0f);
        imageViewerOverlayView.setImageBitmap(null);
        imageViewerOverlayPb.setVisibility(View.VISIBLE);
        imageViewerOverlayTv.setVisibility(View.GONE);
    }

    /**
     * 接收并渲染异步加载完毕的位图实体
     *
     * @param bitmap Bitmap
     */
    void onImageLoaded(Bitmap bitmap) {
        imageViewerOverlayPb.setVisibility(View.GONE);
        if (bitmap == null) {
            imageViewerOverlayTv.setText(getContext().getString(R.string.loadImageFail));
            imageViewerOverlayTv.setVisibility(View.VISIBLE);
            return;
        }
        if ((getWidth() > 0) && (getHeight() > 0)) {
            centerAndFit(bitmap.getWidth(), bitmap.getHeight());
            imageViewerOverlayView.setImageBitmap(bitmap);
        } else {
            imageViewerOverlayView.setVisibility(View.INVISIBLE);
            imageViewerOverlayView.setImageBitmap(bitmap);
            imageViewerOverlayView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int l, int t, int r, int b, int oldL, int oldT, int oldR, int oldB) {
                    imageViewerOverlayView.removeOnLayoutChangeListener(this);
                    centerAndFit(bitmap.getWidth(), bitmap.getHeight());
                    imageViewerOverlayView.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    /**
     * 加载策略
     * <p>
     * 提供给上层的架构分流设计
     */
    public enum LoadStrategy {
        NATIVE, GLIDE
    }

    /**
     * 外置浮层销毁监听拦截接口
     */
    public interface OnCloseListener {
        /**
         * 关闭
         */
        void onClose();
    }

    /**
     * 专为 TalkBack 读屏及无障碍焦点点击优化重写的 ImageView 交互单元
     */
    public static class AccessibleImageView extends androidx.appcompat.widget.AppCompatImageView {
        public AccessibleImageView(Context context) {
            super(context);
        }

        public AccessibleImageView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public AccessibleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }
    }
}