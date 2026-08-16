package com.qtone.camerause.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

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
     * 控制角点 (Anchor Handle) 索引常量定义
     */
    private static final int HANDLE_NONE = -1;
    /**
     * 左上角控制点
     * <p>
     * 控制角点 (Anchor Handle) 索引常量定义
     */
    private static final int HANDLE_TOP_LEFT = 0;
    /**
     * 右上角控制点
     * <p>
     * 控制角点 (Anchor Handle) 索引常量定义
     */
    private static final int HANDLE_TOP_RIGHT = 1;
    /**
     * 右下角控制点
     * <p>
     * 控制角点 (Anchor Handle) 索引常量定义
     */
    private static final int HANDLE_BOTTOM_RIGHT = 2;
    /**
     * 左下角控制点
     * <p>
     * 控制角点 (Anchor Handle) 索引常量定义
     */
    private static final int HANDLE_BOTTOM_LEFT = 3;
    /**
     * 选中框四个角点的视觉绘制半径
     * <p>
     * 触控与绘制参数 (单位 px)
     */
    private static final float HANDLE_RADIUS = 20f;
    /**
     * 角点触控感应区域半径
     * <p>
     * 放大触控范围 -> 提高盲操命中率
     * <p>
     * 触控与绘制参数 (单位 px)
     */
    private static final float TOUCH_TARGET_SIZE = 60f;
    /**
     * ROI 矩形框的最小允许宽度和高度
     * <p>
     * 防止框被缩灭或倒置
     * <p>
     * 触控与绘制参数 (单位 px)
     */
    private static final float MIN_RECT_SIZE = 60f;
    /**
     * 存储当前 View 上的所有 ROI 实例
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
     * 记录手势开始变化前 ActiveRoi 的原始坐标矩阵
     * <p>
     * 数据与状态管理变量
     */
    private final RectF startRect = new RectF();
    /**
     * 当前处于被选中 / 被手势操控状态的 ROI 实例
     * <p>
     * 数据与状态管理变量
     */
    private RoiItem activeRoi = null;
    /**
     * 递增的 ROI 唯一标识符
     * <p>
     * 生成如 #1, #2 的标签
     * <p>
     * 数据与状态管理变量
     */
    private int nextRoiId = 1;
    /**
     * 当前处于拖拽变形状态的角点索引
     * <p>
     * 数据与状态管理变量
     */
    private int activeHandle = HANDLE_NONE;
    /**
     * 当前 View 处于的手势操作模式
     * <p>
     * 数据与状态管理变量
     */
    private int mode = MODE_NONE;
    /**
     * 双指捏合开始时
     * <p>
     * 两指间的初始欧氏距离
     * <p>
     * 数据与状态管理变量
     */
    private float oldDist = 1f;
    /**
     * 未选中状态下 ROI 框的 Paint
     * <p>
     * 默认绿色细线
     * <p>
     * 绘图 Paint 实例
     */
    private Paint boxPaint;
    /**
     * 选中状态下 ROI 框的 Paint
     * <p>
     * 默认红色粗线
     * <p>
     * 绘图 Paint 实例
     */
    private Paint selectedBoxPaint;
    /**
     * 四角控制点圆圈的 Paint
     * <p>
     * 默认黄色填充
     * <p>
     * 绘图 Paint 实例
     */
    private Paint handlePaint;
    /**
     * 绘制编号文本 (如 "#1") 的 Paint
     * <p>
     * 默认白色文字
     * <p>
     * 绘图 Paint 实例
     */
    private Paint textPaint;
    /**
     * 状态变更监听回调接口
     * <p>
     * 绘图 Paint 实例
     */
    private OnRoiChangeListener onRoiChangeListener;

    /**
     * constructor
     *
     * @param context      上下文
     * @param attributeSet AttributeSet
     */
    public MultiRoiOverlayView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    /**
     * 初始化画笔属性
     * <p>
     * 开启抗锯齿以保障图层边缘光滑
     */
    private void init() {
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
    }

    /**
     * 设置 ROI 数量变动与删除事件的回调监听器
     */
    public void setOnRoiChangeListener(OnRoiChangeListener onRoiChangeListener) {
        this.onRoiChangeListener = onRoiChangeListener;
    }

    /**
     * 更新宽高比
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
        // 1. 获取并更新 LayoutParams 以触发系统重新测量
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams != null) {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            requestLayout();
        }
        // 2. 之前已有 View 尺寸且画布上已存在 ROI 则将旧 ROI 物理坐标按比例等比映射到新尺寸上
        post(() -> {
            int newViewWidth = getWidth();
            int newViewHeight = getHeight();
            if ((oldViewWidth > 0) && (oldViewHeight > 0) && (newViewWidth > 0) && (newViewHeight > 0)) {
                if ((oldViewWidth != newViewWidth) || (oldViewHeight != newViewHeight)) {
                    float scaleX = (float) newViewWidth / oldViewWidth;
                    float scaleY = (float) newViewHeight / oldViewHeight;
                    for (RoiItem roiItem : roiList) {
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
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NotNull MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        float x = event.getX();
        float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // 记录首指按下的屏幕物理坐标
                startTouch.set(x, y);
                // 1. 优先判定
                // 若当前已有选中的 activeRoi，检查按下的点是否落在其 4 个控制角点范围内。
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
                    // 尝试查找按下的坐标是否位于某个已有 ROI 的矩形内部
                    // 倒序遍历，优先响应最上层。
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
                        // 取消当前的所有选中状态
                        activeRoi = null;
                        highlightRoi(null);
                        mode = MODE_NONE;
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                // 第二根手指按下
                // 处理 [双指捏合缩放] 或 [双指新建 ROI]
                if (event.getPointerCount() == 2) {
                    // 计算两手指间距
                    float dist = spacing(event);
                    // 过滤极微小的误触
                    if (dist > 20f) {
                        oldDist = dist;
                        // 获取两指交汇的中心点
                        PointF center = getCenterPoint(event);
                        // 若当前在空白处直接双指按下
                        // 自动在双指中心创建一个默认矩形 ROI
                        if (activeRoi == null) {
                            activeRoi = createNewRoi(center.x, center.y);
                            highlightRoi(activeRoi);
                        }
                        // 切换为 [双指缩放] 模式
                        mode = MODE_ZOOM;
                        startTouch.set(center.x, center.y);
                        startRect.set(activeRoi.rect);
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (activeRoi == null) break;
                if (mode == MODE_RESIZE) {
                    // 模式 A
                    // 拖拽单个角点改变形状
                    float dx = x - startTouch.x;
                    float dy = y - startTouch.y;
                    // 根据相对位移更新 activeRoi 的 Rect 坐标
                    resizeRoi(activeRoi.rect, startRect, activeHandle, dx, dy);
                    // 实时同步基准坐标
                    // 消除连续拖动时的跳变闪烁现象
                    startTouch.set(x, y);
                    startRect.set(activeRoi.rect);
                    invalidate(); // 触发重绘
                } else if (mode == MODE_DRAG && event.getPointerCount() == 1) {
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
                        // 计算缩放比例
                        float scale = newDist / oldDist;
                        PointF newCenter = getCenterPoint(event);
                        // 中心点平移 X 偏移量
                        float cDx = (newCenter.x - startTouch.x);
                        // 中心点平移 Y 偏移量
                        float cDy = (newCenter.y - startTouch.y);
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
                break;
            case MotionEvent.ACTION_POINTER_UP:
                // 当多指操作过程中有一根手指抬起时，强制重置手势模式。
                // 防止 ACTION_MOVE 继续读取已被释放的手指 Index 导致 IndexOutOfBoundsException
                mode = MODE_NONE;
                break;
            case MotionEvent.ACTION_UP:
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
     * 重测 ROI
     * <p>
     * 根据当前拖拽的控制角点，重新计算并设定矩形的四边边界坐标。
     * 包含了 View 的视口边界截断保护与矩形最小尺寸约束
     *
     * @param target 需要修改的目标 RectF 对象
     * @param start  变形开始前 RectF 的基准坐标
     * @param handle 当前正在拖动的角点类型 (Top-Left, Top-Right, 等)
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
                // 边界限制：不能低于 View 边缘 0 且保证 width / height 不小于 MIN_RECT_SIZE
                left = Math.max(0, Math.min(start.left + dx, right - MIN_RECT_SIZE));
                top = Math.max(0, Math.min(start.top + dy, bottom - MIN_RECT_SIZE));
                break;
            case HANDLE_TOP_RIGHT:
                // 拖动右上角：修改 right 与 top
                right = Math.min(viewW, Math.max(start.right + dx, left + MIN_RECT_SIZE));
                top = Math.max(0, Math.min(start.top + dy, bottom - MIN_RECT_SIZE));
                break;
            case HANDLE_BOTTOM_RIGHT:
                // 拖动右下角：修改 right 与 bottom
                right = Math.min(viewW, Math.max(start.right + dx, left + MIN_RECT_SIZE));
                bottom = Math.min(viewH, Math.max(start.bottom + dy, top + MIN_RECT_SIZE));
                break;
            case HANDLE_BOTTOM_LEFT:
                // 拖动左下角：修改 left 与 bottom
                left = Math.max(0, Math.min(start.left + dx, right - MIN_RECT_SIZE));
                bottom = Math.min(viewH, Math.max(start.bottom + dy, top + MIN_RECT_SIZE));
                break;
        }
        // 重新赋值给目标的 RectF
        target.set(left, top, right, bottom);
    }

    /**
     * 碰撞检测
     * <p>
     * 检查触控点是否落在了给定矩形框的 4 个顶点触控敏感区内
     *
     * @return 返回命中的角点索引常量 (若未命中任何角点则返回 HANDLE_NONE)
     */
    private int hitTestHandle(@NotNull RectF rect, float x, float y) {
        if (isNear(x, y, rect.left, rect.top)) {
            return HANDLE_TOP_LEFT;
        }
        if (isNear(x, y, rect.right, rect.top)) {
            return HANDLE_TOP_RIGHT;
        }
        if (isNear(x, y, rect.right, rect.bottom)) {
            return HANDLE_BOTTOM_RIGHT;
        }
        if (isNear(x, y, rect.left, rect.bottom)) {
            return HANDLE_BOTTOM_LEFT;
        }
        return HANDLE_NONE;
    }

    /**
     * 辅助几何计算
     * <p>
     * 判定点 (x1, y1) 是否位于以 (x2, y2) 为中心、TOUCH_TARGET_SIZE 为半径的圆形感应区域内
     * 使用欧式距离平方计算
     * 避免调用平方根 (Math.sqrt) 带来性能损耗
     */
    private boolean isNear(float x1, float y1, float x2, float y2) {
        float dx = (x1 - x2);
        float dy = (y1 - y2);
        return ((dx * dx + dy * dy) <= (TOUCH_TARGET_SIZE * TOUCH_TARGET_SIZE));
    }

    /**
     * 以给定的中心点 (cx, cy) 为基准，生成并新建一个默认尺寸为 240x160 px 的矩形 ROI 项。
     */
    private @NotNull RoiItem createNewRoi(float cx, float cy) {
        // 默认宽度 240px
        float halfW = 120f;
        // 默认高度 160px
        float halfH = 80f;
        RectF rect = new RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
        RoiItem roiItem = new RoiItem(nextRoiId++, rect);
        roiList.add(roiItem);
        // 触发监听回调
        // 告知外部数量变化
        if (onRoiChangeListener != null) {
            onRoiChangeListener.onRoiCountChanged(roiList.size());
        }
        // 刷新画布
        invalidate();
        return roiItem;
    }

    /**
     * 越界安全校验
     * <p>
     * 判定 ROI 是否移出 View 视口边缘
     * <p>
     * 判定规则
     * 只要 ROI 的中心点越界
     * 或者 ROI 移出 View 边缘超过自身尺寸的一半
     * 即判定为需要丢弃并自动删除
     */
    private void checkAndRemoveIfOutOfBounds(@NotNull RoiItem roi) {
        float halfW = roi.rect.width() / 2f;
        float halfH = roi.rect.height() / 2f;
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        // 判定 1：中心点是否已不在 View 区域内部
        boolean isOutOfBounds = (roi.rect.centerX() < 0) || (roi.rect.centerX() > viewWidth)
                || (roi.rect.centerY() < 0) || (roi.rect.centerY() > viewHeight);
        // 判定 2：四边移出 View 边界是否超过了自身宽度 / 高度的一半
        boolean isHalfOut = (roi.rect.left + halfW < 0) || (roi.rect.right - halfW > viewWidth)
                || (roi.rect.top + halfH < 0) || (roi.rect.bottom - halfH > viewHeight);
        // 符合任意删除条件
        // 则清空该 ROI 框
        if (isHalfOut || isOutOfBounds) {
            roiList.remove(roi);
            // 清空活跃引用
            if (activeRoi == roi) {
                activeRoi = null;
            }
            if (onRoiChangeListener != null) {
                // 通知特定 ROI 被删除
                onRoiChangeListener.onRoiDeleted(roi.id);
                // 通知剩余总数
                onRoiChangeListener.onRoiCountChanged(roiList.size());
            }
            // 刷新画布
            invalidate();
        }
    }

    /**
     * 设置特定 ROI 的高亮选中状态，非目标 ROI 取消高亮。
     */
    private void highlightRoi(RoiItem target) {
        for (RoiItem roiItem : roiList) {
            roiItem.isSelected = (roiItem == target);
        }
        invalidate();
    }

    /**
     * 给定触摸坐标 (x, y)
     * 寻找落在此坐标之下的 ROI 项
     * <p>
     * 使用倒序遍历 (反向 List)
     * 确保最晚添加 / 位于图层最上方的 ROI 优先响应点击
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
     * 计算 MotionEvent 中前两个触摸点 (Index 0 和 Index 1) 之间的直线欧氏距离
     */
    private float spacing(@NotNull MotionEvent event) {
        float x = (event.getX(0) - event.getX(1));
        float y = (event.getY(0) - event.getY(1));
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 计算 MotionEvent 中前两个触摸点的中心点坐标
     */
    private @NotNull PointF getCenterPoint(@NotNull MotionEvent event) {
        float x = (event.getX(0) + event.getX(1)) / 2;
        float y = (event.getY(0) + event.getY(1)) / 2;
        return new PointF(x, y);
    }

    /**
     * 绘制函数
     * <p>
     * 渲染所有 ROI 矩形框、ID 标签文本以及选中框的四角控制点
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (RoiItem roi : roiList) {
            // 根据选中状态选择画笔样式
            // 高亮为红粗线
            // 常态为绿细线
            Paint paint = roi.isSelected ? selectedBoxPaint : boxPaint;
            // 1. 绘制 ROI 矩形边框
            canvas.drawRect(roi.rect, paint);
            // 2. 在矩形左上角绘制编号标签文本 (如 "#1")
            canvas.drawText("#" + roi.id, roi.rect.left + 10, roi.rect.top + 40, textPaint);
            // 3. 若当前框处于选中状态，在其 4 个顶点绘制拉伸控制圆圈。
            if (roi.isSelected) {
                drawHandles(canvas, roi.rect);
            }
        }
    }

    /**
     * 在矩形框的四个顶点上绘制控制点圆圈
     */
    private void drawHandles(@NotNull Canvas canvas, @NotNull RectF rect) {
        canvas.drawCircle(rect.left, rect.top, HANDLE_RADIUS, handlePaint);
        canvas.drawCircle(rect.right, rect.top, HANDLE_RADIUS, handlePaint);
        canvas.drawCircle(rect.right, rect.bottom, HANDLE_RADIUS, handlePaint);
        canvas.drawCircle(rect.left, rect.bottom, HANDLE_RADIUS, handlePaint);
    }

    /**
     * 核心接口工具
     * <p>
     * 获取所有 ROI 框的归一化相对百分比坐标比例 (取值范围：0.0f ~ 1.0f)
     * <p>
     * 使用场景
     * UI 层的 View 物理分辨率与摄像头 Preview / ImageAnalysis 的真实像素分辨率通常不一致
     * 通过返回相对比例，业务层只需乘以相机实际分辨率即可精确还原对应的图像 ROI 区域。
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
            RectF p = new RectF(
                    roiItem.rect.left / w,
                    roiItem.rect.top / h,
                    roiItem.rect.right / w,
                    roiItem.rect.bottom / h
            );
            percentList.add(p);
        }
        return percentList;
    }

    /**
     * 清空当前所有 ROI 框并重置选中状态与视图绘制
     */
    public void clearAllRoi() {
        roiList.clear();
        activeRoi = null;
        invalidate();
        if (onRoiChangeListener != null) {
            onRoiChangeListener.onRoiCountChanged(0);
        }
    }

    /**
     * ROI 状态变化监听接口定义
     */
    public interface OnRoiChangeListener {
        /**
         * 当前屏幕上的 ROI 框总数量发生变化时回调
         *
         * @param count 当前剩余的 ROI 总数
         */
        void onRoiCountChanged(int count);

        /**
         * 某个特定 ID 的 ROI 框被移除 / 清理时回调
         *
         * @param roiId 被删除的 ROI 唯一标识符
         */
        void onRoiDeleted(int roiId);
    }

    /**
     * 单个 ROI 数据模型实体类
     */
    public static class RoiItem {
        /**
         * 对应在 View 屏幕物理坐标系下的矩形边界 (像素点坐标)
         * <p>
         * 单个 ROI 数据模型实体类
         */
        public RectF rect;
        /**
         * 唯一数字 ID
         * <p>
         * 单个 ROI 数据模型实体类
         */
        public int id;
        /**
         * 是否处于高亮选中状态
         * <p>
         * 单个 ROI 数据模型实体类
         */
        public boolean isSelected;

        public RoiItem(int id, RectF rect) {
            this.id = id;
            this.rect = rect;
            this.isSelected = false;
        }
    }
}