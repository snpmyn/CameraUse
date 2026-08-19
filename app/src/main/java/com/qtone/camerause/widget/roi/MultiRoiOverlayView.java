package com.qtone.camerause.widget.roi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @decs: 多 ROI 覆盖视图
 * @author: 郑少鹏
 * @date: 2026/8/8 21:54
 * @version: v 1.0
 */
@SuppressWarnings("unused")
public class MultiRoiOverlayView extends View {
    /**
     * 空闲状态：未触发任何有效手势操作
     * <p>
     * 手势识别状态常量定义
     */
    private static final int MODE_NONE = 0;
    /**
     * 单指拖拽状态：整体平移选中的 ROI
     * <p>
     * 手势识别状态常量定义
     */
    private static final int MODE_DRAG = 1;
    /**
     * 双指捏合状态：等比例缩放及跟随双指中心点平移
     * <p>
     * 手势识别状态常量定义
     */
    private static final int MODE_ZOOM = 2;
    /**
     * 单指变形状态：拖动单个角点调整矩形尺寸
     * <p>
     * 手势识别状态常量定义
     */
    private static final int MODE_RESIZE = 3;
    /**
     * 未选中任何控制点
     * <p>
     * 控制角点索引常量定义
     */
    private static final int HANDLE_NONE = -1;
    /**
     * 左上角控制点
     * <p>
     * 控制角点索引常量定义
     */
    private static final int HANDLE_TOP_LEFT = 0;
    /**
     * 右上角控制点
     * <p>
     * 控制角点索引常量定义
     */
    private static final int HANDLE_TOP_RIGHT = 1;
    /**
     * 右下角控制点
     * <p>
     * 控制角点索引常量定义
     */
    private static final int HANDLE_BOTTOM_RIGHT = 2;
    /**
     * 左下角控制点
     * <p>
     * 控制角点索引常量定义
     */
    private static final int HANDLE_BOTTOM_LEFT = 3;
    /**
     * 选中框四个角点的视觉绘制半径
     * <p>
     * 单位 - px
     * <p>
     * 触控与绘制参数定义
     */
    private static final float HANDLE_RADIUS = 20f;
    /**
     * 角点触控感应区域半径
     * <p>
     * 放大触控范围以提高盲操命中率
     * <p>
     * 单位 - px
     * <p>
     * 触控与绘制参数定义
     */
    private static final float TOUCH_TARGET_SIZE = 60f;
    /**
     * ROI 矩形框的最小允许宽度和高度
     * <p>
     * 防止框被缩灭或倒置
     * <p>
     * 单位 - px
     * <p>
     * 触控与绘制参数定义
     */
    private static final float MIN_RECT_SIZE = 60f;
    /**
     * 存储当前 View 内所有的 ROI 实例
     * <p>
     * 数据与状态管理变量
     */
    private final List<RoiItem> roiList = new ArrayList<>();
    /**
     * 记录手势按下或滑动过程中上一次的触摸坐标
     * <p>
     * 数据与状态管理变量
     */
    private final PointF startTouch = new PointF();
    /**
     * 记录手势开始变化前 activeRoi 的原始坐标矩形
     * <p>
     * 数据与状态管理变量
     */
    private final RectF startRect = new RectF();
    /**
     * 复用中心点对象
     * <p>
     * 避免在 onTouchEvent 高频分配内存引发频繁 GC
     * <p>
     * 数据与状态管理变量
     */
    private final PointF reuseCenterPoint = new PointF();
    /**
     * 当前处于被选中或被手势操控状态的 ROI 实例
     * <p>
     * 数据与状态管理变量
     */
    private RoiItem activeRoi = null;
    /**
     * 递增的 ROI 唯一标识符生成器
     * <p>
     * 用于生成如 #1, #2 等标签
     * <p>
     * 数据与状态管理变量
     */
    private int nextRoiId = 1;
    /**
     * 当前处于拖拽变形状态的角点索引
     * <p>
     * 取值范围 HANDLE_TOP_LEFT 至 HANDLE_BOTTOM_LEFT
     * <p>
     * 数据与状态管理变量
     */
    private int activeHandle = HANDLE_NONE;
    /**
     * 当前 View 处于的手势操作模式
     * <p>
     * 取值范围 MODE_NONE, MODE_DRAG, MODE_ZOOM, MODE_RESIZE
     * <p>
     * 数据与状态管理变量
     */
    private int mode = MODE_NONE;
    /**
     * 双指捏合开始时两指间的初始欧氏距离
     * <p>
     * 数据与状态管理变量
     */
    private float oldDist = 1f;
    /**
     * 手势检测器
     * <p>
     * 用于处理单指快速双击创建 ROI
     * <p>
     * 数据与状态管理变量
     */
    private GestureDetector gestureDetector;
    /**
     * 未选中状态下 ROI 框的 Paint
     * <p>
     * 绘图 Paint 实例
     * 默认绿色细线
     */
    private Paint boxPaint;
    /**
     * 选中状态下 ROI 框的 Paint
     * <p>
     * 绘图 Paint 实例
     * 默认红色粗线
     */
    private Paint selectedBoxPaint;
    /**
     * 四角控制点圆圈的 Paint
     * <p>
     * 绘图 Paint 实例
     * 默认黄色实心填充
     */
    private Paint handlePaint;
    /**
     * 绘制编号文本 (如 "#1") 的 Paint
     * <p>
     * 绘图 Paint 实例
     * 默认白色文字
     */
    private Paint textPaint;
    /**
     * ROI 变化回调
     * <p>
     * 用于向外部通知 ROI 数量变化或删除事件
     */
    private OnRoiChangeCallback onRoiChangeCallback;

    /**
     * constructor
     * <p>
     * 用于在代码中动态创建或加载 View
     *
     * @param context 上下文
     */
    public MultiRoiOverlayView(Context context) {
        super(context);
        init(context);
    }

    /**
     * constructor
     * <p>
     * 用于在 XML 布局文件中声明并加载 View
     *
     * @param context      上下文
     * @param attributeSet 属性集
     */
    public MultiRoiOverlayView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    /**
     * 初始化
     * <p>
     * 初始化画笔属性与双击手势检测器
     *
     * @param context 上下文
     */
    private void init(Context context) {
        // 未选中框：绿色、线框模式、线宽 6px
        boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(6f);
        // 选中框：红色、线框模式、线宽 8px
        selectedBoxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedBoxPaint.setColor(Color.RED);
        selectedBoxPaint.setStyle(Paint.Style.STROKE);
        selectedBoxPaint.setStrokeWidth(8f);
        // 四角控制点：黄色、实心填充
        handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handlePaint.setColor(Color.YELLOW);
        handlePaint.setStyle(Paint.Style.FILL);
        // 编号文本：白色、字号 36px
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36f);
        // 初始化手势检测器：处理单指快速双击创建 ROI
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(@NotNull MotionEvent e) {
                // 以双击触摸点坐标为中心创建新的 ROI
                activeRoi = createNewRoi(e.getX(), e.getY());
                highlightRoi(activeRoi);
                // 重置手势状态，拦截后续单击判定。
                mode = MODE_NONE;
                activeHandle = HANDLE_NONE;
                if (activeRoi != null) {
                    startRect.set(activeRoi.rect);
                }
                return true;
            }
        });
    }

    /**
     * 设置 ROI 变化回调
     *
     * @param onRoiChangeCallback ROI 变化回调
     */
    public void setOnRoiChangeCallback(OnRoiChangeCallback onRoiChangeCallback) {
        this.onRoiChangeCallback = onRoiChangeCallback;
    }

    /**
     * 更新视图宽高比并重新映射原有 ROI 坐标
     * <p>
     * 布局适配接口
     * 当预览比例变化时自动对齐已有 ROI 框坐标
     *
     * @param width  物理帧宽
     * @param height 物理帧高
     */
    public void updateAspectRatio(int width, int height) {
        if ((width <= 0) || (height <= 0)) {
            return;
        }
        int oldViewWidth = getWidth();
        int oldViewHeight = getHeight();
        // 1. 获取 View 父容器的可用宽度和高度
        View parentView = (View) getParent();
        if (parentView == null) {
            return;
        }
        int parentWidth = parentView.getWidth();
        int parentHeight = parentView.getHeight();
        if ((parentWidth <= 0) || (parentHeight <= 0)) {
            return;
        }
        // 2. 根据相机的宽高比计算等比例渲染时的实际 View 宽高
        float targetRatio = (float) width / (float) height;
        float parentRatio = (float) parentWidth / (float) parentHeight;
        int calculatedWidth;
        int calculatedHeight;
        if (parentRatio > targetRatio) {
            // 父容器太宽 -> 以父容器高度为基准计算宽度
            calculatedHeight = parentHeight;
            calculatedWidth = (int) (parentHeight * targetRatio);
        } else {
            // 父容器太高 (全面屏最常见) -> 以父容器宽度为基准计算高度
            calculatedWidth = parentWidth;
            calculatedHeight = (int) (parentWidth / targetRatio);
        }
        // 3. 动态更新 Overlay View 的 LayoutParams 并强制设置居中 (Gravity.CENTER)
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams != null) {
            layoutParams.width = calculatedWidth;
            layoutParams.height = calculatedHeight;
            // 如果父容器是 FrameLayout
            if (layoutParams instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) layoutParams).gravity = android.view.Gravity.CENTER;
            }
            // 如果父容器是 RelativeLayout
            else if (layoutParams instanceof RelativeLayout.LayoutParams) {
                ((RelativeLayout.LayoutParams) layoutParams).addRule(RelativeLayout.CENTER_IN_PARENT);
            }
            setLayoutParams(layoutParams);
        }
        // 4. 将原有 ROI 坐标按比例映射到新的 View 物理尺寸上
        post(() -> {
            int newViewWidth = getWidth();
            int newViewHeight = getHeight();
            if ((oldViewWidth > 0) && (oldViewHeight > 0) && (newViewWidth > 0) && (newViewHeight > 0)) {
                if ((oldViewWidth != newViewWidth) || (oldViewHeight != newViewHeight)) {
                    float scaleX = (float) newViewWidth / oldViewWidth;
                    float scaleY = (float) newViewHeight / oldViewHeight;
                    List<RoiItem> tempMapList = new ArrayList<>(roiList);
                    for (RoiItem roiItem : tempMapList) {
                        roiItem.rect.set(
                                roiItem.rect.left * scaleX,
                                roiItem.rect.top * scaleY,
                                roiItem.rect.right * scaleX,
                                roiItem.rect.bottom * scaleY
                        );
                    }
                    invalidate();
                }
            }
        });
    }

    /**
     * 核心手势处理函数
     * <p>
     * 分发并响应单指点击、单指拖动、角点拉伸、双指捏合等动作
     *
     * @param event 触摸事件
     * @return 总是返回 true 表示消费该事件
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NotNull MotionEvent event) {
        // 1. 优先交给 GestureDetector 处理
        // 若消费了事件 (如双击新建) 则直接拦截返回，防止手势冲突。
        if (gestureDetector.onTouchEvent(event)) {
            return true;
        }
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        float x = event.getX();
        float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // 记录首指按下的屏幕物理坐标
                startTouch.set(x, y);
                // 1. 优先判定
                // 若当前已有选中的 activeRoi 则检查按下的点是否落在其 4 个控制角点范围内
                if (activeRoi != null) {
                    activeHandle = hitTestHandle(activeRoi.rect, x, y);
                } else {
                    activeHandle = HANDLE_NONE;
                }
                if (activeHandle != HANDLE_NONE) {
                    // 2. 命中角点
                    // 切换为 [角点拖拽变形] 模式
                    mode = MODE_RESIZE;
                    startRect.set(activeRoi.rect);
                } else {
                    // 3. 未命中角点
                    // 倒序遍历 + 优先响应最上层
                    // 尝试查找按下的坐标是否位于某个已有 ROI 的矩形内部
                    RoiItem touchedRoi = findTouchedRoi(x, y);
                    if (touchedRoi != null) {
                        activeRoi = touchedRoi;
                        // 切换高亮状态
                        highlightRoi(activeRoi);
                        // 切换为 [单指平移] 模式
                        mode = MODE_DRAG;
                        startRect.set(activeRoi.rect);
                    } else {
                        // 4. 点击了没有任何 ROI 的空白区域
                        // 延缓清除高亮
                        // 不在此处设 activeRoi = null，防止双击的第一下点击误解高亮框。
                        mode = MODE_NONE;
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                // 第二根手指按下
                // 处理 [双指捏合缩放]
                if (event.getPointerCount() == 2) {
                    float dist = spacing(event);
                    // 过滤极微小误触
                    if (dist > 20f) {
                        oldDist = dist;
                        // 计算两指交汇中心点并填充给复用变量 reuseCenterPoint
                        calculateCenterPoint(event, reuseCenterPoint);
                        startTouch.set(reuseCenterPoint.x, reuseCenterPoint.y);
                        if (activeRoi != null) {
                            // 已有选中 ROI 时正常进行缩放
                            mode = MODE_ZOOM;
                            startRect.set(activeRoi.rect);
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == MODE_RESIZE) {
                    if (activeRoi == null) break;
                    // 模式 A
                    // 拖拽单个角点改变形状
                    float dx = x - startTouch.x;
                    float dy = y - startTouch.y;
                    // 根据相对位移更新 activeRoi 的 Rect 坐标
                    resizeRoi(activeRoi.rect, startRect, activeHandle, dx, dy);
                    // 触发重绘
                    invalidate();
                } else if (mode == MODE_DRAG && event.getPointerCount() == 1) {
                    if (activeRoi == null) break;
                    // 模式 B
                    // 单指整体平移 ROI 框
                    float dx = x - startTouch.x;
                    float dy = y - startTouch.y;
                    // 保持宽高不变
                    // 更新 left / top / right / bottom 偏移值
                    activeRoi.rect.set(
                            startRect.left + dx,
                            startRect.top + dy,
                            startRect.right + dx,
                            startRect.bottom + dy
                    );
                    invalidate();
                } else if ((mode == MODE_ZOOM) && (event.getPointerCount() >= 2)) {
                    // 模式 C
                    // 双指等比例缩放与中心平移
                    float newDist = spacing(event);
                    if (newDist > 20f) {
                        if (activeRoi != null) {
                            // 计算缩放比例
                            float scale = newDist / oldDist;
                            calculateCenterPoint(event, reuseCenterPoint);
                            // 中心点平移 X / Y 轴偏移量
                            float cDx = (reuseCenterPoint.x - startTouch.x);
                            float cDy = (reuseCenterPoint.y - startTouch.y);
                            float currentWidth = startRect.width() * scale;
                            float currentHeight = startRect.height() * scale;
                            // 满足最小尺寸限制时，计算缩放及位移后的矩形边界。
                            if ((currentWidth >= MIN_RECT_SIZE) && (currentHeight >= MIN_RECT_SIZE)) {
                                float cx = (startRect.centerX() + cDx);
                                float cy = (startRect.centerY() + cDy);
                                activeRoi.rect.set(
                                        cx - currentWidth / 2f,
                                        cy - currentHeight / 2f,
                                        cx + currentWidth / 2f,
                                        cy + currentHeight / 2f
                                );
                                invalidate();
                            }
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                // 安全获取剩余手指索引，防止 IndexOutOfBoundsException 崩溃。
                int upIndex = event.getActionIndex();
                int remainIndex = findRemainingPointerIndex(event, upIndex);
                if (remainIndex != -1) {
                    startTouch.set(event.getX(remainIndex), event.getY(remainIndex));
                    if (activeRoi != null) {
                        startRect.set(activeRoi.rect);
                        // 无缝切换为单指拖拽模式，避免松开一指后继续滑动失效的问题
                        mode = MODE_DRAG;
                    } else {
                        mode = MODE_NONE;
                    }
                } else {
                    mode = MODE_NONE;
                }
                break;
            case MotionEvent.ACTION_UP:
                // 在单击抬起且未命中任何 ROI 时，取消当前的选择状态。
                if (mode == MODE_NONE && activeHandle == HANDLE_NONE && findTouchedRoi(x, y) == null) {
                    activeRoi = null;
                    highlightRoi(null);
                }
                // 最后一根手指抬起，检查当前活跃的 ROI 是否超出了 View 的边界范围，超出则执行移除。
                if (activeRoi != null && mode != MODE_NONE) {
                    checkAndRemoveIfOutOfBounds(activeRoi);
                }
                // 重置所有手势标志位
                mode = MODE_NONE;
                activeHandle = HANDLE_NONE;
                break;
        }
        return true;
    }

    /**
     * 安全查找抬起一根手指后，剩余的第一根有效手指 Pointer 索引。
     *
     * @param event   手势事件
     * @param upIndex 当前抬起的手指索引
     * @return 依然按在屏幕上的有效手指索引 [若无则返回 -1]
     */
    private int findRemainingPointerIndex(MotionEvent event, int upIndex) {
        int count = event.getPointerCount();
        for (int i = 0; i < count; i++) {
            if (i != upIndex) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 根据当前拖拽的控制角点重新计算并设定矩形的四边边界坐标
     * 包含了 View 的视口边界截断保护与矩形最小尺寸约束，防止框体反转与挤压过小。
     *
     * @param target 需要修改的目标 RectF 对象
     * @param start  变形开始前 RectF 的基准坐标
     * @param handle 当前正在拖动的角点类型 (Top-Left, Top-Right 等)
     * @param dx     X 轴方向上的移动偏移量
     * @param dy     Y 轴方向上的移动偏移量
     */
    private void resizeRoi(RectF target, @NotNull RectF start, int handle, float dx, float dy) {
        float left = start.left;
        float top = start.top;
        float right = start.right;
        float bottom = start.bottom;
        int viewW = getWidth();
        int viewH = getHeight();
        switch (handle) {
            case HANDLE_TOP_LEFT:
                // 拖动左上角：修改 left 与 top
                left = Math.min(start.left + dx, right - MIN_RECT_SIZE);
                left = Math.max(0, left);
                top = Math.min(start.top + dy, bottom - MIN_RECT_SIZE);
                top = Math.max(0, top);
                break;
            case HANDLE_TOP_RIGHT:
                // 拖动右上角：修改 right 与 top
                right = Math.max(start.right + dx, left + MIN_RECT_SIZE);
                right = Math.min(viewW, right);
                top = Math.min(start.top + dy, bottom - MIN_RECT_SIZE);
                top = Math.max(0, top);
                break;
            case HANDLE_BOTTOM_RIGHT:
                // 拖动右下角：修改 right 与 bottom
                right = Math.max(start.right + dx, left + MIN_RECT_SIZE);
                right = Math.min(viewW, right);
                bottom = Math.max(start.bottom + dy, top + MIN_RECT_SIZE);
                bottom = Math.min(viewH, bottom);
                break;
            case HANDLE_BOTTOM_LEFT:
                // 拖动左下角：修改 left 与 bottom
                left = Math.min(start.left + dx, right - MIN_RECT_SIZE);
                left = Math.max(0, left);
                bottom = Math.max(start.bottom + dy, top + MIN_RECT_SIZE);
                bottom = Math.min(viewH, bottom);
                break;
        }
        // 重新赋值给目标的 RectF
        target.set(left, top, right, bottom);
    }

    /**
     * 角点碰撞检测算法
     * <p>
     * 遍历检测触控点落在哪一个角点的感应范围内，并自动取距离最近的那个 (全局最近邻法) 从而彻底消除角点重叠死锁。
     *
     * @param rect 目标 ROI 矩形
     * @param x    触摸点 X 坐标
     * @param y    触摸点 Y 坐标
     * @return 返回命中的角点索引常量，若未命中任何角点则返回 HANDLE_NONE
     */
    private int hitTestHandle(@NotNull RectF rect, float x, float y) {
        int bestHandle = HANDLE_NONE;
        float minSqDist = Float.MAX_VALUE;
        float touchRadiusSq = TOUCH_TARGET_SIZE * TOUCH_TARGET_SIZE;
        float[][] handles = {
                {rect.left, rect.top},
                {rect.right, rect.top},
                {rect.right, rect.bottom},
                {rect.left, rect.bottom}
        };
        for (int i = 0; i < handles.length; i++) {
            float dx = (x - handles[i][0]);
            float dy = (y - handles[i][1]);
            float sqDist = (dx * dx + dy * dy);
            if ((sqDist <= touchRadiusSq) && (sqDist < minSqDist)) {
                minSqDist = sqDist;
                bestHandle = i;
            }
        }
        return bestHandle;
    }

    /**
     * 以给定的中心点 (cx, cy) 为基准生成新的 ROI 项
     * 生成默认尺寸为 240x160 px 的矩形 ROI 项并添加到列表中，对边界施加收敛保护防止被误判删除。
     *
     * @param cx 中心点 X 坐标
     * @param cy 中心点 Y 坐标
     * @return 新创建的 RoiItem 实例
     */
    private @NotNull RoiItem createNewRoi(float cx, float cy) {
        // 默认
        // 宽度 240px
        // 高度 160px
        float halfW = 120f;
        float halfH = 80f;
        // 边界钳位控制
        // 防止框生成在视口边界外导致刚创建就被自动判定移除
        int viewW = getWidth();
        int viewH = getHeight();
        if ((viewW > 0) && (viewH > 0)) {
            cx = Math.max(halfW, Math.min(cx, viewW - halfW));
            cy = Math.max(halfH, Math.min(cy, viewH - halfH));
        }
        RectF rectF = new RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
        RoiItem roiItem = new RoiItem(nextRoiId++, rectF);
        roiList.add(roiItem);
        // 触发监听回调，告知外部数量变化。
        if (onRoiChangeCallback != null) {
            onRoiChangeCallback.onRoiCountChanged(roiList.size());
        }
        // 刷新画布
        invalidate();
        return roiItem;
    }

    /**
     * 判定 ROI 是否移出 View 视口边缘
     * 若满足删除条件则自动丢弃该 ROI
     * <p>
     * 判定规则
     * 只要 ROI 的中心点越界，或者 ROI 移出 View 边缘超过自身尺寸的一半，即判定为移出屏幕。
     *
     * @param roi 待校验的 ROI 项
     */
    private void checkAndRemoveIfOutOfBounds(@NotNull RoiItem roi) {
        float halfW = roi.rect.width() / 2f;
        float halfH = roi.rect.height() / 2f;
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        // 判定 1
        // 中心点是否已不在 View 区域内部
        boolean isOutOfBounds = (roi.rect.centerX() < 0) || (roi.rect.centerX() > viewWidth) || (roi.rect.centerY() < 0) || (roi.rect.centerY() > viewHeight);
        // 判定 2
        // 四边移出 View 边界是否超过了自身宽度 / 高度的一半
        boolean isHalfOut = (roi.rect.left + halfW < 0) || (roi.rect.right - halfW > viewWidth) || (roi.rect.top + halfH < 0) || (roi.rect.bottom - halfH > viewHeight);
        // 符合任意删除条件则清空该 ROI 框
        if (isHalfOut || isOutOfBounds) {
            roiList.remove(roi);
            // 清空活跃引用
            if (activeRoi == roi) {
                activeRoi = null;
            }
            if (onRoiChangeCallback != null) {
                // 通知特定 ROI 被删除并更新剩余总数
                onRoiChangeCallback.onRoiDeleted(roi.id);
                onRoiChangeCallback.onRoiCountChanged(roiList.size());
            }
            // 刷新画布
            invalidate();
        }
    }

    /**
     * 设置特定 ROI 的高亮选中状态，非目标 ROI 取消高亮状态更新方法。
     *
     * @param target 需要高亮的 ROI 实例
     *               传入 null 表示全不选中
     */
    private void highlightRoi(RoiItem target) {
        for (RoiItem roiItem : roiList) {
            roiItem.isSelected = (roiItem == target);
        }
        invalidate();
    }

    /**
     * 给定触摸坐标寻找落在此坐标之下的 ROI 项
     * <p>
     * 使用倒序遍历 (反向 List)
     * 确保最晚添加或位于图层最上方的 ROI 优先响应点击
     *
     * @param x 触摸点 X
     * @param y 触摸点 Y
     * @return 命中的 RoiItem [未命中任何 ROI 则返回 null]
     */
    private @Nullable RoiItem findTouchedRoi(float x, float y) {
        for (int i = (roiList.size() - 1); i >= 0; i--) {
            if (roiList.get(i).rect.contains(x, y)) {
                return roiList.get(i);
            }
        }
        return null;
    }

    /**
     * 计算 MotionEvent 中前两个触摸点之间的直线欧氏距离
     * <p>
     * 触摸计算辅助方法
     * 用于 Index 0 和 Index 1 两点间距离计算
     *
     * @param event 手势事件
     * @return 两点间的像素距离
     */
    private float spacing(@NotNull MotionEvent event) {
        if (event.getPointerCount() < 2) return 0f;
        float x = (event.getX(0) - event.getX(1));
        float y = (event.getY(0) - event.getY(1));
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 计算 MotionEvent 中前两个触摸点的中心点坐标并写入复用 PointF 变量
     * <p>
     * 触摸计算辅助方法，避免内存高频分配。
     *
     * @param event    手势事件
     * @param outPoint 用于接收输出结果的 PointF 实例
     */
    private void calculateCenterPoint(@NotNull MotionEvent event, PointF outPoint) {
        if (event.getPointerCount() < 2) return;
        float x = (event.getX(0) + event.getX(1)) / 2f;
        float y = (event.getY(0) + event.getY(1)) / 2f;
        outPoint.set(x, y);
    }

    /**
     * 绘制函数
     * <p>
     * 渲染所有 ROI 矩形框、ID 标签文本以及选中框的四角控制点
     *
     * @param canvas 画布
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (RoiItem roi : roiList) {
            // 根据选中状态选择画笔样式
            // 高亮为红粗线，常态为绿细线
            Paint paint = roi.isSelected ? selectedBoxPaint : boxPaint;
            // 1. 绘制 ROI 矩形边框
            canvas.drawRect(roi.rect, paint);
            // 2. 在矩形左上角绘制编号标签文本 (如 "#1")
            // 直接使用缓存文本避免内存抖动
            canvas.drawText(roi.labelText, roi.rect.left + 10, roi.rect.top + 40, textPaint);
            // 3. 若当前框处于选中状态，在其 4 个顶点绘制拉伸控制圆圈。
            if (roi.isSelected) {
                drawHandles(canvas, roi.rect);
            }
        }
    }

    /**
     * 绘制辅助方法
     * <p>
     * 在矩形框的四个顶点上绘制控制点圆圈
     *
     * @param canvas 画布
     * @param rect   目标矩形
     */
    private void drawHandles(@NotNull Canvas canvas, @NotNull RectF rect) {
        canvas.drawCircle(rect.left, rect.top, HANDLE_RADIUS, handlePaint);
        canvas.drawCircle(rect.right, rect.top, HANDLE_RADIUS, handlePaint);
        canvas.drawCircle(rect.right, rect.bottom, HANDLE_RADIUS, handlePaint);
        canvas.drawCircle(rect.left, rect.bottom, HANDLE_RADIUS, handlePaint);
    }

    /**
     * 获取所有 ROI 框的归一化相对百分比坐标比例
     * 转换并返回 [0.0, 1.0] 范围内的比例数据
     * 便于业务层换算相机像素点
     *
     * @return 归一化的 RectF 列表 (left, top, right, bottom 均在 [0.0, 1.0] 范围内)
     */
    public List<RectF> getAllRoiPercentages() {
        List<RectF> percentList = new ArrayList<>();
        float w = getWidth();
        float h = getHeight();
        if ((w == 0) || (h == 0)) {
            // 控件尚未完成 Layout 绘制时返回空集合
            return percentList;
        }
        for (RoiItem roiItem : roiList) {
            RectF rectF = new RectF(
                    roiItem.rect.left / w,
                    roiItem.rect.top / h,
                    roiItem.rect.right / w,
                    roiItem.rect.bottom / h
            );
            percentList.add(rectF);
        }
        return percentList;
    }

    /**
     * 清空当前所有 ROI 框并重置选中状态与视图绘制
     * <p>
     * 外部数据重置接口
     */
    public void clearAllRoi() {
        roiList.clear();
        activeRoi = null;
        invalidate();
        if (onRoiChangeCallback != null) {
            onRoiChangeCallback.onRoiCountChanged(0);
        }
    }

    /**
     * ROI 变化回调
     * <p>
     * 用于监听屏幕 ROI 数量变动或特定项的清理事件
     */
    public interface OnRoiChangeCallback {
        /**
         * 当前屏幕上的 ROI 框总数量发生变化时回调
         * 包含新增、手势滑出删除、清空等操作触发的数量变更
         *
         * @param count 当前剩余的 ROI 总数
         */
        void onRoiCountChanged(int count);

        /**
         * 某个特定 ID 的 ROI 框被移除或清理时回调
         * 单项移除事件通知
         *
         * @param roiId 被删除的 ROI 唯一标识符
         */
        void onRoiDeleted(int roiId);
    }

    /**
     * 单个 ROI 数据模型实体类
     * 包含其物理坐标矩形、唯一标识以及选中状态标志
     */
    public static class RoiItem {
        /**
         * 对应在 View 屏幕物理坐标系下的矩形边界
         * 像素点坐标 RectF 实例
         */
        public RectF rect;
        /**
         * 唯一数字 ID
         * 自动递增的数值标识
         */
        public int id;
        /**
         * 是否处于高亮选中状态
         * 手势交互与绘制标记位
         */
        public boolean isSelected;
        /**
         * 预渲染缓存文本
         * 用于解决 onDraw 内高频拼接 String 导致的 GC 卡顿问题
         */
        public String labelText;

        /**
         * 构造函数
         * 用于创建新的 RoiItem 实体
         *
         * @param id   唯一标识 ID
         * @param rect 初始矩形区域
         */
        public RoiItem(int id, RectF rect) {
            this.id = id;
            this.rect = rect;
            this.isSelected = false;
            this.labelText = ("#" + id);
        }
    }
}