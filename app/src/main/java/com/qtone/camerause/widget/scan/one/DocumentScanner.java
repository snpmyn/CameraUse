package com.qtone.camerause.widget.scan.one;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 文档角标检测 + 透视矫正一体化扫描器。
 *
 * <p>由原 {@code CornerMarkerDetector}（检测）与 {@code PerspectiveCorrector}（矫正）合并而来，
 * 纯 Java 实现，不依赖 OpenCV 等任何第三方库。
 *
 * <h2>目标标记特征</h2>
 * 照片四角上的黑色方形定位框，为三层同心结构（已在真实扫描件上确认）：
 * <pre>
 *   ┌─────────┐
 *   │ ■■■■■■■ │   黑色方形外环（环厚约 0.20 × 边长）
 *   │ ■ □□□ ■ │   环内一圈白色间隙
 *   │ ■ □山□ ■ │   间隙内是黑色字形
 *   │ ■■■■■■■ │
 *   └─────────┘
 * </pre>
 *
 * <h2>检测流程</h2>
 * <ol>
 *   <li>转灰度（ITU-R BT.601）</li>
 *   <li>光照归一化：大窗口 box blur 估计背景 → {@code gray×255/background}，消除页面阴影</li>
 *   <li>形态学闭运算（3×3）：连接褪色导致的环边断裂</li>
 *   <li>4-连通域提取 + 尺寸/长宽比过滤</li>
 *   <li>环状结构校验：环带黑度 / 白隙占比 / 实心度 / 填充率</li>
 *   <li>角归属：每角取半径内最近候选（借此排除二维码定位角）</li>
 * </ol>
 *
 * <h2>矫正流程</h2>
 * <ol>
 *   <li>四点排序为 TL→TR→BR→BL（按 x+y 与 y−x 的极值，不依赖输入顺序）</li>
 *   <li>输出尺寸取对边最大长度，不裁掉内容</li>
 *   <li>加自适应边距，让角标本身完整留在输出图内</li>
 *   <li>解 8×8 线性方程组求单应矩阵（高斯消元 + 部分主元）</li>
 *   <li>反向映射 + 双线性插值，避免前向映射的空洞</li>
 * </ol>
 *
 * <h2>实测验证（1280×1707 真实扫描件）</h2>
 * <ul>
 *   <li>检测：正样本 4/4 检出；正文区、二维码区、表格区三组负样本 0 误报</li>
 *   <li>矫正：角标中心映射误差 0.00px；正文行分布集中度 2.567 → 3.108</li>
 * </ul>
 *
 * <h2>典型用法</h2>
 * <pre>{@code
 * DocumentScanner scanner = new DocumentScanner();
 * DocumentScanner.ScanResult r = scanner.scan(bitmap);   // 检测 + 矫正一步完成
 * if (r.isSuccess()) {
 *     imageView.setImageBitmap(r.getCorrection().getBitmap());
 * } else {
 *     Log.w("Scan", r.getErrorType() + ": " + r.getErrorMessage());
 * }
 * }</pre>
 *
 * <p><b>线程</b>：所有方法均为逐像素运算，请在子线程调用。
 *
 * <p><b>输入尺寸</b>：短边不建议低于约 800px，否则褪色角标可能漏检。
 */
public final class DocumentScanner {
    private final Config config;

    public DocumentScanner() {
        this(Config.defaults());
    }

    public DocumentScanner(Config config) {
        this.config = config != null ? config : Config.defaults();
    }

    /**
     * 对单点应用单应变换；分母接近 0（退化）时返回 {@code null}
     */
    public static PointF applyHomography(double[] h, float x, float y) {
        if (h == null || h.length < 9) throw new IllegalArgumentException("单应矩阵长度须为 9");
        double xd = x;
        double yd = y;
        double den = h[6] * xd + h[7] * yd + h[8];
        if (Math.abs(den) < 1e-12) return null;
        return new PointF(
                (float) ((h[0] * xd + h[1] * yd + h[2]) / den),
                (float) ((h[3] * xd + h[4] * yd + h[5]) / den));
    }

    // ================================================================== //
    //  公共类型
    // ================================================================== //

    /**
     * 求单应矩阵 H（3×3，行优先，H[8]=1）使 {@code dst = H · src}（齐次坐标）。
     *
     * <p>每点对给出两个方程，4 点共 8 个方程，解 8 元线性方程组。
     * 采用高斯消元 + 部分主元法，比固定消元顺序更抗数值病态。
     *
     * @throws IllegalStateException 四点共线或重复导致矩阵奇异
     */
    public static double[] solveHomography(List<PointF> src, List<PointF> dst) {
        if (src == null || dst == null || src.size() != 4 || dst.size() != 4) {
            throw new IllegalArgumentException("src/dst 各需 4 个点");
        }
        final int n = 8;
        double[][] a = new double[n][n];
        double[] b = new double[n];

        for (int i = 0; i < 4; i++) {
            double x = src.get(i).x;
            double y = src.get(i).y;
            double u = dst.get(i).x;
            double v = dst.get(i).y;

            // u = (h0·x + h1·y + h2) / (h6·x + h7·y + 1)
            int r0 = 2 * i;
            a[r0][0] = x;
            a[r0][1] = y;
            a[r0][2] = 1.0;
            a[r0][3] = 0.0;
            a[r0][4] = 0.0;
            a[r0][5] = 0.0;
            a[r0][6] = -x * u;
            a[r0][7] = -y * u;
            b[r0] = u;

            // v = (h3·x + h4·y + h5) / (h6·x + h7·y + 1)
            int r1 = 2 * i + 1;
            a[r1][0] = 0.0;
            a[r1][1] = 0.0;
            a[r1][2] = 0.0;
            a[r1][3] = x;
            a[r1][4] = y;
            a[r1][5] = 1.0;
            a[r1][6] = -x * v;
            a[r1][7] = -y * v;
            b[r1] = v;
        }

        for (int col = 0; col < n; col++) {
            int pivot = col;
            double maxAbs = Math.abs(a[col][col]);
            for (int r = col + 1; r < n; r++) {
                double v = Math.abs(a[r][col]);
                if (v > maxAbs) {
                    maxAbs = v;
                    pivot = r;
                }
            }
            if (maxAbs < 1e-12) {
                throw new IllegalStateException("第 " + col + " 列主元过小，四点可能共线或重复");
            }
            if (pivot != col) {
                double[] tmpRow = a[col];
                a[col] = a[pivot];
                a[pivot] = tmpRow;
                double tmpB = b[col];
                b[col] = b[pivot];
                b[pivot] = tmpB;
            }
            double pv = a[col][col];
            for (int j = col; j < n; j++) a[col][j] /= pv;
            b[col] /= pv;
            for (int r = 0; r < n; r++) {
                if (r == col) continue;
                double f = a[r][col];
                if (f == 0.0) continue;
                for (int j = col; j < n; j++) a[r][j] -= f * a[col][j];
                b[r] -= f * b[col];
            }
        }

        return new double[]{b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7], 1.0};
    }

    /**
     * 输出宽：取上下两边长度的较大值
     */
    private static int outputWidthOf(List<PointF> quad) {
        return Math.round(Math.max(distance(quad.get(0), quad.get(1)),
                distance(quad.get(3), quad.get(2))));
    }

    /**
     * 输出高：取左右两边长度的较大值
     */
    private static int outputHeightOf(List<PointF> quad) {
        return Math.round(Math.max(distance(quad.get(0), quad.get(3)),
                distance(quad.get(1), quad.get(2))));
    }

    private static float distance(PointF a, PointF b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 鞋带公式求四边形面积，用于退化判定
     */
    private static float quadArea(List<PointF> quad) {
        double sum = 0.0;
        for (int i = 0; i < 4; i++) {
            PointF a = quad.get(i);
            PointF b = quad.get((i + 1) % 4);
            sum += (double) a.x * b.y - (double) b.x * a.y;
        }
        return (float) Math.abs(sum / 2.0);
    }

    /**
     * 转灰度（ITU-R BT.601）。
     *
     * <p>逐行调用 {@code getPixels} 而非一次性读取整幅：批量读取仍远快于逐点
     * {@code getPixel}，但像素缓冲从 4n 字节降为 4w 字节（1280×1707 图约省 8.7MB）。
     */
    private static byte[] toGrayscale(Bitmap bitmap, int w, int h) {
        int[] row = new int[w];
        byte[] gray = new byte[w * h];
        for (int y = 0; y < h; y++) {
            bitmap.getPixels(row, 0, w, 0, y, w, 1);
            int base = y * w;
            for (int x = 0; x < w; x++) {
                int p = row[x];
                gray[base + x] = (byte) ((Color.red(p) * 299 + Color.green(p) * 587
                        + Color.blue(p) * 114) / 1000);
            }
        }
        return gray;
    }

    /**
     * 形态学闭运算（膨胀后腐蚀，行列可分离）。
     *
     * <p>用于填补褪色造成的环边断裂。核大小须小（默认 3）：
     * 实测核取 9 会把角标与邻近文字、二维码粘连成一片，导致漏检。
     *
     * <p><b>内存</b>：只额外分配 1 份全图缓冲（原实现为 4 份），加上 O(w) 行缓冲
     * 与 O(k×w) 环形缓冲。对 1280×1707 图像约省 6.5MB，避免在 Android 受限堆下 OOM。
     *
     * <p>注意：可分离形态学<b>不能</b>直接在 buf 上就地扫描——水平趟会读到本轮
     * 已写入的值，使单个黑点沿整行扩散（实测 ..#.... 会变成 ####... 而非 .###...）。
     * 故水平趟先把整行读入 scratch，垂直趟用环形缓冲保存尚未被覆盖的 k 行原值。
     */
    private static byte[] morphologicalClosing(byte[] src, int w, int h, int k) {
        if (k < 3) return src;
        byte[] buf = src.clone();
        dilateSeparable(buf, w, h, k);
        erodeSeparable(buf, w, h, k);
        return buf;
    }

    /**
     * 膨胀：窗口内任一为 1 则输出 1。边界按 clamp 处理（窗口在边界处变窄）。
     *
     * @param buf 输入输出同一数组，内部用行缓冲/环形缓冲避免读写冲突
     */
    private static void dilateSeparable(byte[] buf, int w, int h, int k) {
        final int half = k / 2;

        // 水平趟：输出行只依赖同一输入行，w 字节 scratch 即可
        byte[] scratch = new byte[w];
        for (int y = 0; y < h; y++) {
            int base = y * w;
            System.arraycopy(buf, base, scratch, 0, w);
            for (int x = 0; x < w; x++) {
                int lo = Math.max(0, x - half);
                int hi = Math.min(w - 1, x + half);
                int v = 0;
                for (int xx = lo; xx <= hi; xx++) {
                    if (scratch[xx] == 1) {
                        v = 1;
                        break;
                    }
                }
                buf[base + x] = (byte) v;
            }
        }

        // 垂直趟：环形缓冲保存原行 y-half .. y+half。
        // 越界行按 clamp 取边界行——膨胀是 OR 运算，重复计入边界行不影响结果。
        byte[][] ring = new byte[k][w];
        for (int j = 0; j < k; j++) {
            int yy = Math.max(0, Math.min(h - 1, j - half));
            System.arraycopy(buf, yy * w, ring[j], 0, w);
        }
        for (int y = 0; y < h; y++) {
            int base = y * w;
            for (int x = 0; x < w; x++) {
                int v = 0;
                for (int j = 0; j < k; j++) {
                    if (ring[j][x] == 1) {
                        v = 1;
                        break;
                    }
                }
                buf[base + x] = (byte) v;
            }
            if (y + 1 < h) {
                // 滚动：丢弃最上一行，末尾载入原行 y+1+half。
                // 该行尚未被本轮写入（已写行号 ≤ y < y+1+half），可安全从 buf 读取。
                byte[] recycled = ring[0];
                System.arraycopy(ring, 1, ring, 0, k - 1);
                ring[k - 1] = recycled;
                int nextRow = Math.min(h - 1, y + 1 + half);
                System.arraycopy(buf, nextRow * w, ring[k - 1], 0, w);
            }
        }
    }

    // ================================================================== //
    //  公共 API
    // ================================================================== //

    /**
     * 腐蚀：窗口内任一为 0 或越界则输出 0。
     *
     * <p>与膨胀的边界语义不同：越界一律视为 0，因此图像边缘 k/2 行/列会被腐蚀掉，
     * 与原始实现（{@code lo < 0 || hi >= w → v = 0}）保持一致。
     */
    private static void erodeSeparable(byte[] buf, int w, int h, int k) {
        final int half = k / 2;

        byte[] scratch = new byte[w];
        for (int y = 0; y < h; y++) {
            int base = y * w;
            System.arraycopy(buf, base, scratch, 0, w);
            for (int x = 0; x < w; x++) {
                int lo = x - half;
                int hi = x + half;
                int v = 1;
                if (lo < 0 || hi >= w) {
                    v = 0;
                } else {
                    for (int xx = lo; xx <= hi; xx++) {
                        if (scratch[xx] != 1) {
                            v = 0;
                            break;
                        }
                    }
                }
                buf[base + x] = (byte) v;
            }
        }

        // 越界行填 0：腐蚀是 AND 运算，任一 0 即输出 0
        byte[][] ring = new byte[k][w];
        for (int j = 0; j < k; j++) {
            int yy = j - half;
            if (yy < 0 || yy >= h) {
                Arrays.fill(ring[j], (byte) 0);
            } else {
                System.arraycopy(buf, yy * w, ring[j], 0, w);
            }
        }
        for (int y = 0; y < h; y++) {
            int base = y * w;
            for (int x = 0; x < w; x++) {
                int v = 1;
                for (int j = 0; j < k; j++) {
                    if (ring[j][x] != 1) {
                        v = 0;
                        break;
                    }
                }
                buf[base + x] = (byte) v;
            }
            if (y + 1 < h) {
                byte[] recycled = ring[0];
                System.arraycopy(ring, 1, ring, 0, k - 1);
                ring[k - 1] = recycled;
                int nextRow = y + 1 + half;
                if (nextRow >= h) {
                    Arrays.fill(ring[k - 1], (byte) 0);
                } else {
                    System.arraycopy(buf, nextRow * w, ring[k - 1], 0, w);
                }
            }
        }
    }

    private static int clamp255(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    public Config getConfig() {
        return config;
    }

    /**
     * 检测 + 矫正一步完成。这是最常用的入口。
     *
     * @param bitmap 原始照片
     * @return 成功时含矫正图；角标不全或四点退化时含失败原因
     */
    public ScanResult scan(Bitmap bitmap) {
        DetectionResult detection = detect(bitmap);
        return correct(bitmap, detection);
    }

    /**
     * 检测四个角上的黑色方形定位框。
     *
     * @param bitmap 待检测图片
     * @return 检测结果；缺失的角不会出现在 map 中
     */
    public DetectionResult detect(Bitmap bitmap) {
        final int w = bitmap.getWidth();
        final int h = bitmap.getHeight();
        if (w == 0 || h == 0) {
            return new DetectionResult(new EnumMap<Corner, Marker>(Corner.class), w, h);
        }
        byte[] gray = toGrayscale(bitmap, w, h);
        return detectFromGrayInternal(gray, w, h);
    }

    /**
     * 从 ARGB_8888 像素数组检测，便于对接 CameraX 的 ImageProxy，省去 Bitmap 拷贝。
     *
     * @param pixels 行优先 ARGB 像素，长度须不小于 width × height
     */
    public DetectionResult detectFromPixels(int[] pixels, int width, int height) {
        if (pixels == null) throw new IllegalArgumentException("pixels 不能为 null");
        if (width < 0 || height < 0) throw new IllegalArgumentException("尺寸不能为负");
        if (pixels.length < (long) width * height) {
            throw new IllegalArgumentException(
                    "pixels 长度不足：需要 " + ((long) width * height) + "，实际 " + pixels.length);
        }
        if (width == 0 || height == 0) {
            return new DetectionResult(new EnumMap<Corner, Marker>(Corner.class), width, height);
        }
        byte[] gray = new byte[width * height];
        for (int i = 0; i < gray.length; i++) {
            int p = pixels[i];
            gray[i] = (byte) ((Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000);
        }
        return detectFromGrayInternal(gray, width, height);
    }

    /**
     * 从 8bit 灰度数组检测。
     *
     * @param gray 行优先灰度值，长度须不小于 width × height
     */
    public DetectionResult detectFromGray(byte[] gray, int width, int height) {
        if (gray == null) throw new IllegalArgumentException("gray 不能为 null");
        if (width < 0 || height < 0) throw new IllegalArgumentException("尺寸不能为负");
        if (gray.length < (long) width * height) {
            throw new IllegalArgumentException(
                    "gray 长度不足：需要 " + ((long) width * height) + "，实际 " + gray.length);
        }
        if (width == 0 || height == 0) {
            return new DetectionResult(new EnumMap<Corner, Marker>(Corner.class), width, height);
        }
        // 内部只读不改，直接使用调用方数组以避免一次大拷贝
        return detectFromGrayInternal(gray, width, height);
    }

    // ================================================================== //
    //  几何工具（public，便于单元测试与复用）
    // ================================================================== //

    /**
     * 用已有检测结果矫正
     */
    public ScanResult correct(Bitmap source, DetectionResult detection) {
        if (detection == null) {
            return ScanResult.failure(null, ErrorType.NOT_ENOUGH_MARKERS, "检测结果为 null");
        }
        if (!detection.isComplete()) {
            List<String> names = new ArrayList<>(4);
            for (Corner c : detection.getMissingCorners()) names.add(c.name());
            return ScanResult.failure(detection, ErrorType.NOT_ENOUGH_MARKERS,
                    "需要 4 个角标，实际检出 " + detection.getFoundCount()
                            + " 个，缺失：" + String.join(",", names));
        }

        List<PointF> points = new ArrayList<>(4);
        double sizeSum = 0.0;
        for (Corner c : new Corner[]{Corner.TOP_LEFT, Corner.TOP_RIGHT,
                Corner.BOTTOM_RIGHT, Corner.BOTTOM_LEFT}) {
            Marker m = detection.get(c);
            // 用外接框的浮点中心而非四舍五入后的整数中心：
            // 整数中心会让输出尺寸产生约 1px 偏差（实测 1609 vs 1610）
            points.add(m.centerAsFloat());
            // 角标平均边长（取宽高较大值），用于自适应边距
            sizeSum += Math.max(m.getWidth(), m.getHeight());
        }
        float avgMarkerSize = (float) (sizeSum / 4.0);

        return correct(source, points, avgMarkerSize, detection);
    }

    /**
     * 用任意四点矫正（不依赖角标检测，可手动指定或来自其他检测器）。
     *
     * @param source     原始图片
     * @param quad       四个点，顺序任意，内部会重排为 TL,TR,BR,BL
     * @param markerSize 参考标记尺寸，用于计算自适应边距；{@code <=0} 时用 {@code fallbackMargin}
     */
    public ScanResult correct(Bitmap source, List<PointF> quad, float markerSize) {
        return correct(source, quad, markerSize, null);
    }

    private ScanResult correct(Bitmap source, List<PointF> quad, float markerSize,
                               DetectionResult detection) {
        if (quad == null || quad.size() != 4) {
            int found = quad == null ? 0 : quad.size();
            return ScanResult.failure(detection, ErrorType.NOT_ENOUGH_MARKERS,
                    "需要 4 个点，实际提供 " + found + " 个");
        }

        List<PointF> ordered = orderPoints(quad);
        if (quadArea(ordered) <= 1f) {
            return ScanResult.failure(detection, ErrorType.DEGENERATE_POINTS,
                    "四边形面积≈0，四点可能共线");
        }

        int docW = outputWidthOf(ordered);
        int docH = outputHeightOf(ordered);
        if (docW < 2 || docH < 2) {
            return ScanResult.failure(detection, ErrorType.INVALID_SIZE,
                    "输出尺寸非法：" + docW + "x" + docH);
        }

        int margin = 0;
        if (config.keepMargin) {
            margin = markerSize > 0f
                    ? Math.max(2, Math.round(markerSize * config.marginRatio))
                    : config.fallbackMargin;
        }

        int outW = docW + 2 * margin;
        int outH = docH + 2 * margin;
        float scale = 1f;

        if (config.maxOutputSize > 0) {
            int longSide = Math.max(outW, outH);
            if (longSide > config.maxOutputSize) {
                scale = (float) config.maxOutputSize / longSide;
                outW = Math.max(1, Math.round(outW * scale));
                outH = Math.max(1, Math.round(outH * scale));
            }
        }

        // 目标四点（输出图坐标系），顺序 TL,TR,BR,BL
        float m = margin * scale;
        List<PointF> dst = Arrays.asList(
                new PointF(m, m),
                new PointF(m + (docW - 1) * scale, m),
                new PointF(m + (docW - 1) * scale, m + (docH - 1) * scale),
                new PointF(m, m + (docH - 1) * scale)
        );

        double[] hToSrc;
        double[] hToDst;
        try {
            hToSrc = solveHomography(dst, ordered);   // 反向映射用：dst -> src
            hToDst = solveHomography(ordered, dst);   // 正向：src -> dst，供坐标换算
        } catch (IllegalStateException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "矩阵奇异";
            return ScanResult.failure(detection, ErrorType.DEGENERATE_POINTS, msg);
        }

        Bitmap warped = warpBitmap(source, hToSrc, outW, outH);

        CorrectionResult cr = new CorrectionResult(warped, dst, hToSrc, hToDst,
                outW, outH, margin, scale);
        return ScanResult.success(detection, cr);
    }

    /**
     * 把无序四点排成 TL, TR, BR, BL。
     *
     * <p>对凸四边形：x+y 最小者为左上、最大者为右下；y−x 最小者为右上、最大者为左下。
     * 不依赖输入顺序，因此检测器返回顺序变化也不影响结果。
     */
    public List<PointF> orderPoints(List<PointF> pts) {
        if (pts == null || pts.size() != 4) {
            throw new IllegalArgumentException("需要恰好 4 个点");
        }
        List<PointF> bySum = new ArrayList<>(pts);
        Collections.sort(bySum, new Comparator<PointF>() {
            @Override
            public int compare(PointF a, PointF b) {
                return Float.compare(a.x + a.y, b.x + b.y);
            }
        });
        PointF tl = bySum.get(0);
        PointF br = bySum.get(3);

        List<PointF> byDiff = new ArrayList<>(pts);
        Collections.sort(byDiff, new Comparator<PointF>() {
            @Override
            public int compare(PointF a, PointF b) {
                return Float.compare(a.y - a.x, b.y - b.x);
            }
        });
        PointF tr = byDiff.get(0);
        PointF bl = byDiff.get(3);

        return Arrays.asList(tl, tr, br, bl);
    }

    private DetectionResult detectFromGrayInternal(byte[] gray, int w, int h) {
        byte[] black = normalizeAndBinarize(gray, w, h);
        byte[] closed = morphologicalClosing(black, w, h, config.closingKernel);
        List<Candidate> candidates = extractCandidates(closed, black, w, h);
        Map<Corner, Marker> markers = assignToCorners(candidates, w, h);
        return new DetectionResult(markers, w, h);
    }

    /**
     * 光照归一化 + 二值化。
     *
     * <p>用大窗口均值估计背景光照，再以 {@code gray×255/background} 归一化后做固定阈值二值化。
     * 相比全局阈值或局部自适应阈值，对「半页处于阴影中」的文档照片更鲁棒
     * （实测样本下半页背景灰度低至 48）。
     *
     * <p><b>内存</b>：用「滑动窗口列和 + 行前缀和」求窗口均值，只需两个 O(w) 数组。
     * 原实现用整幅 int 积分图（(w+1)×(h+1)×4 字节，1280×1707 约 8.7MB），
     * 是本流程单笔最大的分配，在 Android 受限堆下极易触发 OOM。
     *
     * <p>垂直窗口随 y 下移时增量更新（加一行、减一行），水平窗口用 colSum 的前缀和
     * O(1) 取区间和，总复杂度仍为 O(n)，与原积分图实现结果完全一致。
     */
    private byte[] normalizeAndBinarize(byte[] gray, int w, int h) {
        int minDim = Math.min(w, h);
        // 保证为奇数，且不小于 31
        int block = Math.max(31, Math.round(minDim * config.lightWindowRatio) | 1);
        int half = block / 2;
        int threshold = config.normThreshold;

        byte[] black = new byte[w * h];
        int[] colSum = new int[w];          // 当前垂直窗口内各列的灰度和
        int[] rowPrefix = new int[w + 1];   // colSum 的前缀和，用于水平窗口 O(1) 求和

        // 初始化 y=0 时的垂直窗口 [0, min(h-1, half)]
        int initBottom = Math.min(h - 1, half);
        for (int y = 0; y <= initBottom; y++) {
            int base = y * w;
            for (int x = 0; x < w; x++) colSum[x] += (gray[base + x] & 0xFF);
        }

        for (int y = 0; y < h; y++) {
            if (y > 0) {
                // 窗口下移一行：新行进入、旧行离开
                int addY = y + half;
                if (addY <= h - 1) {
                    int base = addY * w;
                    for (int x = 0; x < w; x++) colSum[x] += (gray[base + x] & 0xFF);
                }
                int subY = y - half - 1;
                if (subY >= 0) {
                    int base = subY * w;
                    for (int x = 0; x < w; x++) colSum[x] -= (gray[base + x] & 0xFF);
                }
            }

            rowPrefix[0] = 0;
            for (int x = 0; x < w; x++) rowPrefix[x + 1] = rowPrefix[x] + colSum[x];

            int y0 = Math.max(0, y - half);
            int y1 = Math.min(h - 1, y + half);
            int rowCount = y1 - y0 + 1;
            int base = y * w;

            for (int x = 0; x < w; x++) {
                int x0 = Math.max(0, x - half);
                int x1 = Math.min(w - 1, x + half);
                int count = (x1 - x0 + 1) * rowCount;
                int sum = rowPrefix[x1 + 1] - rowPrefix[x0];
                int bg = sum / count;
                if (bg <= 0) bg = 1;

                int normalized = ((gray[base + x] & 0xFF) * 255) / bg;
                if (normalized < threshold) black[base + x] = 1;
            }
        }
        return black;
    }

    /**
     * 连通域提取 + 尺寸/长宽比过滤 + 环状结构校验
     */
    private List<Candidate> extractCandidates(byte[] closed, byte[] black, int w, int h) {
        int minDim = Math.min(w, h);
        int minSide = Math.max(config.sizeMinAbsolute, Math.round(minDim * config.sizeMinRatio));
        int maxSide = Math.round(minDim * config.sizeMaxRatio);

        // 尺寸窗口保护：minSide 带绝对下限 16px，而 maxSide 按比例计算无下限。
        // 短边 < 约267px 时会出现 maxSide < minSide，若直接早退将「既不报错也不检出」。
        // 这里抬高 maxSide 使其不小于 minSide，让极小图仍能工作而非无声失败。
        if (maxSide < minSide) maxSide = minSide;

        int n = w * h;
        List<Candidate> result = new ArrayList<>();

        // 显式栈，避免深递归导致 StackOverflowError。
        // 容量按需增长而非固定 n：角标连通域通常只有千级像素，栈实际只占几 KB；
        // 上限 n 保证极端情况（整幅连通）仍正确，不会溢出。
        int[] stack = new int[Math.min(n, 4096)];

        for (int start = 0; start < n; start++) {
            // closed 仅在本方法内使用，故直接就地置 0 标记已访问，省掉一份 n 字节 visited 数组。
            // 注意 ring 指标读的是 black（未被修改），不受影响。
            if (closed[start] != 1) continue;

            int sp = 0;
            stack[sp++] = start;
            closed[start] = 0;

            int minX = start % w, maxX = minX;
            int minY = start / w, maxY = minY;

            while (sp > 0) {
                int idx = stack[--sp];
                int x = idx % w;
                int y = idx / w;
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;

                // 本轮最多压入 4 个邻居，一次性预留容量，避免在内层分支里重复判断
                if (sp + 4 > stack.length) {
                    int grown = Math.min(n, Math.max(sp + 4, stack.length * 2));
                    stack = Arrays.copyOf(stack, grown);
                }

                // 4-邻域
                if (x > 0) {
                    int j = idx - 1;
                    if (closed[j] == 1) {
                        closed[j] = 0;
                        stack[sp++] = j;
                    }
                }
                if (x < w - 1) {
                    int j = idx + 1;
                    if (closed[j] == 1) {
                        closed[j] = 0;
                        stack[sp++] = j;
                    }
                }
                if (y > 0) {
                    int j = idx - w;
                    if (closed[j] == 1) {
                        closed[j] = 0;
                        stack[sp++] = j;
                    }
                }
                if (y < h - 1) {
                    int j = idx + w;
                    if (closed[j] == 1) {
                        closed[j] = 0;
                        stack[sp++] = j;
                    }
                }
            }

            int bw = maxX - minX + 1;
            int bh = maxY - minY + 1;

            if (bw < minSide || bw > maxSide || bh < minSide || bh > maxSide) continue;

            float aspect = (float) bw / bh;
            if (aspect < config.aspectMin || aspect > config.aspectMax) continue;

            Candidate c = computeRingMetrics(minX, minY, maxX, maxY, black, w, h);
            if (c != null) result.add(c);
        }
        return result;
    }

    // ================================================================== //
    //  检测实现
    // ================================================================== //

    /**
     * 环状结构校验。
     *
     * <p>以外接框为基准，按「到最近边的距离 d = min(dx, dy)」划分三个同心带：
     * <ul>
     *   <li>{@code d < e1}（e1 = 0.20×边长）：外环带 → 应主要为黑</li>
     *   <li>{@code e1 <= d < e2}（e2 = 0.34×边长）：白隙带 → 应主要为白</li>
     *   <li>{@code d >= e2}：核心区，内部字形形态多变，<b>不作为判据</b></li>
     * </ul>
     *
     * <p>核心区不参与判定是实测结论：同一批角标的核心区黑度从 0.16 到 0.92 不等，
     * 差异来自内部字形不同，用它做阈值会误杀正常角标。
     *
     * @return 通过全部结构阈值时返回候选，否则 {@code null}
     */
    private Candidate computeRingMetrics(int minX, int minY, int maxX, int maxY,
                                         byte[] black, int w, int h) {
        int bw = maxX - minX + 1;
        int bh = maxY - minY + 1;
        int box = bw * bh;
        float side = (bw + bh) / 2f;
        int e1 = Math.max(2, Math.round(0.20f * side));
        int e2 = Math.max(e1 + 1, Math.round(0.34f * side));

        int ringTotal = 0, ringBlackCount = 0;
        int gapTotal = 0, gapWhiteCount = 0;
        int blackCount = 0;
        float soliditySum = 0f;

        for (int y = minY; y <= maxY; y++) {
            int dy = Math.min(y - minY, maxY - y);
            int rowBase = y * w;
            for (int x = minX; x <= maxX; x++) {
                int dx = Math.min(x - minX, maxX - x);
                int d = Math.min(dx, dy);
                boolean isBlack = black[rowBase + x] == 1;

                if (d < e1) {
                    ringTotal++;
                    if (isBlack) ringBlackCount++;
                } else if (d < e2) {
                    gapTotal++;
                    if (!isBlack) gapWhiteCount++;
                }

                if (isBlack) {
                    blackCount++;
                    // 3×3 邻域黑色占比
                    int cnt = 0, tot = 0;
                    int yLo = Math.max(0, y - 1), yHi = Math.min(h - 1, y + 1);
                    int xLo = Math.max(0, x - 1), xHi = Math.min(w - 1, x + 1);
                    for (int yy = yLo; yy <= yHi; yy++) {
                        int b = yy * w;
                        for (int xx = xLo; xx <= xHi; xx++) {
                            tot++;
                            if (black[b + xx] == 1) cnt++;
                        }
                    }
                    soliditySum += (float) cnt / tot;
                }
            }
        }

        if (ringTotal == 0 || gapTotal == 0 || blackCount == 0) return null;

        float fill = (float) blackCount / box;
        float ringBlack = (float) ringBlackCount / ringTotal;
        float gapWhite = (float) gapWhiteCount / gapTotal;
        float solidity = soliditySum / blackCount;

        if (fill < config.fillMin || fill > config.fillMax) return null;
        if (ringBlack < config.ringBlackMin) return null;
        if (gapWhite < config.gapWhiteMin) return null;
        if (solidity < config.solidityMin || solidity > config.solidityMax) return null;

        return new Candidate(minX, minY, maxX, maxY,
                (minX + maxX) / 2f, (minY + maxY) / 2f,
                fill, ringBlack, gapWhite, solidity);
    }

    /**
     * 角归属：每个图像角取「距离最近且在半径内」的候选。
     *
     * <p>这一步同时解决了二维码定位角的干扰——二维码的三个定位角结构与目标角标几乎一致，
     * 无法靠形状区分，但它们离图像角落明显更远（实测 110px vs 真角标 95px），
     * 会被半径约束或「最近优先」自然排除。
     */
    private Map<Corner, Marker> assignToCorners(List<Candidate> candidates, int w, int h) {
        Map<Corner, Marker> result = new EnumMap<>(Corner.class);
        if (candidates.isEmpty()) return result;

        float limit = config.cornerDistanceRatio * Math.min(w, h);

        for (Corner corner : Corner.values()) {
            boolean isLeft = corner == Corner.TOP_LEFT || corner == Corner.BOTTOM_LEFT;
            boolean isTop = corner == Corner.TOP_LEFT || corner == Corner.TOP_RIGHT;
            float ax = isLeft ? 0f : w;
            float ay = isTop ? 0f : h;

            Candidate best = null;
            float bestDist = Float.MAX_VALUE;

            for (Candidate c : candidates) {
                float dx = c.centerX - ax;
                float dy = c.centerY - ay;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = c;
                }
            }

            if (best != null && bestDist <= limit) {
                result.put(corner, new Marker(
                        corner,
                        best.minX, best.minY, best.maxX, best.maxY,
                        Math.round(best.centerX), Math.round(best.centerY),
                        bestDist, best.fill, best.ringBlack, best.gapWhite, best.solidity));
            }
        }
        return result;
    }

    /**
     * 反向映射重采样：对输出图每个像素，求其在原图中的位置再插值取色。
     *
     * <p>用反向映射而非前向映射，是为了避免输出图出现未被覆盖的空洞。
     *
     * <p><b>内存</b>：只保留一份源像素数组，通道用位运算即时提取（与查表同速），
     * 输出按行写入 Bitmap。相比原实现（源像素 + 三份通道数组 + 整幅输出缓冲）
     * 峰值从约 20n 字节降到 4n+O(w)，1280×1707 图约省 35MB。
     */
    private Bitmap warpBitmap(Bitmap source, double[] hToSrc, int outW, int outH) {
        final int sw = source.getWidth();
        final int sh = source.getHeight();
        final int sn = sw * sh;

        int[] srcPixels = new int[sn];
        source.getPixels(srcPixels, 0, sw, 0, 0, sw, sh);

        int[] outRowBuf = new int[outW];
        int border = config.borderColor;

        double h0 = hToSrc[0], h1 = hToSrc[1], h2 = hToSrc[2];
        double h3 = hToSrc[3], h4 = hToSrc[4], h5 = hToSrc[5];
        double h6 = hToSrc[6], h7 = hToSrc[7], h8 = hToSrc[8];

        float maxSX = sw - 1f;
        float maxSY = sh - 1f;
        boolean useBilinear = config.bilinear;

        Bitmap out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);

        for (int y = 0; y < outH; y++) {
            double yd = y;
            // 行内增量：H 作用于 (x,y)，x 每加 1 时分子分母的增量固定，可累加提速
            double numX = h1 * yd + h2;
            double numY = h4 * yd + h5;
            double den = h7 * yd + h8;

            for (int x = 0; x < outW; x++) {
                if (Math.abs(den) < 1e-12) {
                    outRowBuf[x] = border;
                } else {
                    float sx = (float) (numX / den);
                    float sy = (float) (numY / den);

                    if (sx < 0f || sy < 0f || sx > maxSX || sy > maxSY) {
                        outRowBuf[x] = border;
                    } else if (useBilinear) {
                        int x0 = (int) sx;
                        int y0 = (int) sy;
                        int x1 = Math.min(x0 + 1, sw - 1);
                        int y1 = Math.min(y0 + 1, sh - 1);
                        float fx = sx - x0;
                        float fy = sy - y0;
                        float w00 = (1 - fx) * (1 - fy);
                        float w10 = fx * (1 - fy);
                        float w01 = (1 - fx) * fy;
                        float w11 = fx * fy;

                        int p00 = srcPixels[y0 * sw + x0];
                        int p10 = srcPixels[y0 * sw + x1];
                        int p01 = srcPixels[y1 * sw + x0];
                        int p11 = srcPixels[y1 * sw + x1];

                        // 通道用位运算即时提取，避免为 R/G/B 各存一份全图数组
                        int r = Math.round(((p00 >> 16 & 0xFF) * w00 + (p10 >> 16 & 0xFF) * w10
                                + (p01 >> 16 & 0xFF) * w01 + (p11 >> 16 & 0xFF) * w11));
                        int g = Math.round(((p00 >> 8 & 0xFF) * w00 + (p10 >> 8 & 0xFF) * w10
                                + (p01 >> 8 & 0xFF) * w01 + (p11 >> 8 & 0xFF) * w11));
                        int b = Math.round(((p00 & 0xFF) * w00 + (p10 & 0xFF) * w10
                                + (p01 & 0xFF) * w01 + (p11 & 0xFF) * w11));

                        outRowBuf[x] = Color.rgb(clamp255(r), clamp255(g), clamp255(b));
                    } else {
                        // 最近邻
                        int ix = Math.min(sw - 1, Math.round(sx));
                        int iy = Math.min(sh - 1, Math.round(sy));
                        outRowBuf[x] = srcPixels[iy * sw + ix] | 0xFF000000;
                    }
                }

                numX += h0;
                numY += h3;
                den += h6;
            }

            out.setPixels(outRowBuf, 0, outW, 0, y, outW, 1);
        }

        return out;
    }

    /**
     * 四个角的位置
     */
    public enum Corner {TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT}

    /**
     * 失败类型
     */
    public enum ErrorType {
        /**
         * 角标不足四个，无法矫正
         */
        NOT_ENOUGH_MARKERS,
        /**
         * 四点共线或重复，无法求唯一单应
         */
        DEGENERATE_POINTS,
        /**
         * 计算出的输出尺寸非法
         */
        INVALID_SIZE
    }

    /**
     * 平面点
     */
    public static final class PointF {
        public final float x;
        public final float y;

        public PointF(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PointF)) return false;
            PointF p = (PointF) o;
            return Float.compare(p.x, x) == 0 && Float.compare(p.y, y) == 0;
        }

        @Override
        public int hashCode() {
            return 31 * Float.hashCode(x) + Float.hashCode(y);
        }

        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }
    }

    /**
     * 单个角标的检测结果，坐标均为原图坐标系
     */
    public static final class Marker {
        private final Corner corner;
        private final int left;
        private final int top;
        private final int right;
        private final int bottom;
        private final int centerX;
        private final int centerY;
        private final float cornerDistance;
        private final float fillRatio;
        private final float ringBlackRatio;
        private final float gapWhiteRatio;
        private final float solidity;

        Marker(Corner corner, int left, int top, int right, int bottom,
               int centerX, int centerY, float cornerDistance,
               float fillRatio, float ringBlackRatio, float gapWhiteRatio, float solidity) {
            this.corner = corner;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.centerX = centerX;
            this.centerY = centerY;
            this.cornerDistance = cornerDistance;
            this.fillRatio = fillRatio;
            this.ringBlackRatio = ringBlackRatio;
            this.gapWhiteRatio = gapWhiteRatio;
            this.solidity = solidity;
        }

        public Corner getCorner() {
            return corner;
        }

        public int getLeft() {
            return left;
        }

        public int getTop() {
            return top;
        }

        public int getRight() {
            return right;
        }

        public int getBottom() {
            return bottom;
        }

        public int getCenterX() {
            return centerX;
        }

        public int getCenterY() {
            return centerY;
        }

        /**
         * 角标中心到对应图像角的距离
         */
        public float getCornerDistance() {
            return cornerDistance;
        }

        /**
         * 黑色像素占外接框面积比例
         */
        public float getFillRatio() {
            return fillRatio;
        }

        /**
         * 外环带内黑色像素占比，越高说明环越完整
         */
        public float getRingBlackRatio() {
            return ringBlackRatio;
        }

        /**
         * 白隙带内白色像素占比，越高说明环与内部图案分离越清晰
         */
        public float getGapWhiteRatio() {
            return gapWhiteRatio;
        }

        /**
         * 实心度：黑色像素 3×3 邻域内黑色占比，用于排除二维码稀疏模块
         */
        public float getSolidity() {
            return solidity;
        }

        public int getWidth() {
            return right - left + 1;
        }

        public int getHeight() {
            return bottom - top + 1;
        }

        /**
         * 外接框中心（浮点，用于矫正）
         */
        PointF centerAsFloat() {
            return new PointF((left + right) / 2f, (top + bottom) / 2f);
        }

        @Override
        public String toString() {
            return "Marker{" + corner + " bbox=(" + left + "," + top + ")-(" + right + "," + bottom + ")"
                    + " center=(" + centerX + "," + centerY + ")"
                    + " fill=" + String.format("%.2f", fillRatio)
                    + " ring=" + String.format("%.2f", ringBlackRatio)
                    + " gap=" + String.format("%.2f", gapWhiteRatio)
                    + " solid=" + String.format("%.2f", solidity) + "}";
        }
    }

    /**
     * 检测结果：检出的角标按角归类，未检出的角不会出现在 map 中
     */
    public static final class DetectionResult {
        private final Map<Corner, Marker> markers;
        private final int imageWidth;
        private final int imageHeight;

        DetectionResult(Map<Corner, Marker> markers, int imageWidth, int imageHeight) {
            this.markers = Collections.unmodifiableMap(markers);
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
        }

        public Map<Corner, Marker> getMarkers() {
            return markers;
        }

        public int getImageWidth() {
            return imageWidth;
        }

        public int getImageHeight() {
            return imageHeight;
        }

        /**
         * 是否四个角标全部检出（矫正的前提）
         */
        public boolean isComplete() {
            return markers.size() == 4;
        }

        /**
         * 检出的角标数量
         */
        public int getFoundCount() {
            return markers.size();
        }

        /**
         * 指定角是否检出
         */
        public boolean has(Corner corner) {
            return markers.containsKey(corner);
        }

        /**
         * 取指定角的角标，未检出返回 {@code null}
         */
        public Marker get(Corner corner) {
            return markers.get(corner);
        }

        /**
         * 缺失的角，按 TL,TR,BL,BR 顺序
         */
        public List<Corner> getMissingCorners() {
            List<Corner> missing = new ArrayList<>(4);
            for (Corner c : Corner.values()) {
                if (!markers.containsKey(c)) missing.add(c);
            }
            return missing;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("DetectionResult(complete=").append(isComplete())
                    .append(", found=").append(markers.size()).append("/4");
            if (!markers.isEmpty()) {
                sb.append(", ");
                List<String> names = new ArrayList<>(markers.size());
                for (Corner c : markers.keySet()) names.add(c.name());
                sb.append(String.join(",", names));
            }
            List<Corner> missing = getMissingCorners();
            if (!missing.isEmpty()) {
                sb.append(", missing=");
                List<String> names = new ArrayList<>(missing.size());
                for (Corner c : missing) names.add(c.name());
                sb.append(String.join(",", names));
            }
            return sb.append(")").toString();
        }
    }

    /**
     * 矫正结果
     */
    public static final class CorrectionResult {
        private final Bitmap bitmap;
        private final List<PointF> outputCorners;
        private final double[] homographyToSource;
        private final double[] homographyToOutput;
        private final int outputWidth;
        private final int outputHeight;
        private final int margin;
        private final float scale;

        CorrectionResult(Bitmap bitmap, List<PointF> outputCorners,
                         double[] homographyToSource, double[] homographyToOutput,
                         int outputWidth, int outputHeight, int margin, float scale) {
            this.bitmap = bitmap;
            this.outputCorners = Collections.unmodifiableList(outputCorners);
            this.homographyToSource = homographyToSource;
            this.homographyToOutput = homographyToOutput;
            this.outputWidth = outputWidth;
            this.outputHeight = outputHeight;
            this.margin = margin;
            this.scale = scale;
        }

        /**
         * 矫正后的图片
         */
        public Bitmap getBitmap() {
            return bitmap;
        }

        /**
         * 输出图中四个角标的位置，顺序为 TL,TR,BR,BL
         */
        public List<PointF> getOutputCorners() {
            return outputCorners;
        }

        /**
         * 单应矩阵 H（dst → src），长度 9，行优先。用于反向映射
         */
        public double[] getHomographyToSource() {
            return homographyToSource.clone();
        }

        /**
         * 单应矩阵 H（src → dst），长度 9，行优先。用于把原图坐标换算到矫正图
         */
        public double[] getHomographyToOutput() {
            return homographyToOutput.clone();
        }

        public int getOutputWidth() {
            return outputWidth;
        }

        public int getOutputHeight() {
            return outputHeight;
        }

        /**
         * 四周保留的边距（未缩放前的像素值）
         */
        public int getMargin() {
            return margin;
        }

        /**
         * 因 {@code maxOutputSize} 而产生的缩放系数，未缩放时为 1
         */
        public float getScale() {
            return scale;
        }

        /**
         * 把原图坐标映射到矫正图坐标，退化时返回 {@code null}
         */
        public PointF sourceToOutput(float x, float y) {
            return applyHomography(homographyToOutput, x, y);
        }

        /**
         * 把矫正图坐标映射回原图坐标，退化时返回 {@code null}
         */
        public PointF outputToSource(float x, float y) {
            return applyHomography(homographyToSource, x, y);
        }

        /**
         * 文档区域在输出图中的左边界（已扣除边距与缩放）
         */
        public int getDocumentLeft() {
            return Math.round(margin * scale);
        }

        public int getDocumentTop() {
            return Math.round(margin * scale);
        }

        public int getDocumentRight() {
            return outputWidth - 1 - getDocumentLeft();
        }

        public int getDocumentBottom() {
            return outputHeight - 1 - getDocumentTop();
        }

        @Override
        public String toString() {
            return "CorrectionResult(" + outputWidth + "x" + outputHeight
                    + ", margin=" + margin + ", scale=" + String.format("%.3f", scale) + ")";
        }
    }

    /**
     * 扫描结果：成功时含检测结果与矫正图，失败时含原因
     */
    public static final class ScanResult {
        private final DetectionResult detection;
        private final CorrectionResult correction;
        private final ErrorType errorType;
        private final String errorMessage;

        private ScanResult(DetectionResult detection, CorrectionResult correction,
                           ErrorType errorType, String errorMessage) {
            this.detection = detection;
            this.correction = correction;
            this.errorType = errorType;
            this.errorMessage = errorMessage;
        }

        static ScanResult success(DetectionResult d, CorrectionResult c) {
            return new ScanResult(d, c, null, null);
        }

        static ScanResult failure(DetectionResult d, ErrorType t, String msg) {
            return new ScanResult(d, null, t, msg);
        }

        public boolean isSuccess() {
            return correction != null;
        }

        /**
         * 检测结果（即使矫正失败也可用，便于排查缺哪个角）
         */
        public DetectionResult getDetection() {
            return detection;
        }

        /**
         * 矫正结果，失败时为 {@code null}
         */
        public CorrectionResult getCorrection() {
            return correction;
        }

        /**
         * 失败类型，成功时为 {@code null}
         */
        public ErrorType getErrorType() {
            return errorType;
        }

        /**
         * 失败描述，成功时为 {@code null}
         */
        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String toString() {
            return isSuccess()
                    ? "ScanResult(success, " + correction + ")"
                    : "ScanResult(failed, " + errorType + ": " + errorMessage + ")";
        }
    }

    // ================================================================== //
    //  图像重映射
    // ================================================================== //

    /**
     * 可调参数。默认值已在真实文档照片上验证，一般无需修改。
     *
     * <p>调参建议：
     * <ul>
     *   <li><b>漏检</b>：调高 {@code normThreshold}（如 150），或降低 {@code ringBlackMin}（如 0.35）</li>
     *   <li><b>误报</b>：提高 {@code sizeMinAbsolute} 或 {@code sizeMinRatio}</li>
     * </ul>
     * 建议先把各 {@link Marker} 的 ring/gap/solidity 打日志，看真实分布再定阈值。
     */
    public static final class Config {

        // ---------------- 检测参数 ----------------
        /**
         * 归一化后二值化阈值，越大越激进（更多像素判为黑）
         */
        public final int normThreshold;
        /**
         * 光照估计窗口占短边比例，需远大于角标尺寸
         */
        public final float lightWindowRatio;
        /**
         * 闭运算核大小；3 用于连接褪色断裂，过大（如 9）会粘连邻近文字与二维码
         */
        public final int closingKernel;
        /**
         * 角标边长占短边比例下限。0.015 兼容拍摄较远的小标记（实测 25px/1280 短边）
         */
        public final float sizeMinRatio;
        /**
         * 角标边长占短边比例上限
         */
        public final float sizeMaxRatio;
        /**
         * 角标边长绝对下限（px），防止小图上把字符误判为角标
         */
        public final int sizeMinAbsolute;
        /**
         * 长宽比下限
         */
        public final float aspectMin;
        /**
         * 长宽比上限
         */
        public final float aspectMax;
        /**
         * 填充率下限；环状结构约 0.3~0.6
         */
        public final float fillMin;
        /**
         * 填充率上限；用于排除实心块
         */
        public final float fillMax;
        /**
         * 外环带黑度下限
         */
        public final float ringBlackMin;
        /**
         * 白隙带白度下限。0.20 兼容环壁较厚、无明显独立白隙的标记（实测厚壁样式 gap=0.26）
         */
        public final float gapWhiteMin;
        /**
         * 实心度下限
         */
        public final float solidityMin;
        /**
         * 实心度上限：排除桌面阴影、折角阴影等近纯实黑块（实测阴影 solid=0.98，真角标 ≤0.90）
         */
        public final float solidityMax;
        /**
         * 角标中心到图像角的最大距离占短边比例
         */
        public final float cornerDistanceRatio;

        // ---------------- 矫正参数 ----------------
        /**
         * 是否在输出图四周保留边距。
         * {@code true}：角标完整可见，便于复核；{@code false}：紧贴文档边缘裁切，角标会被切掉一半。
         */
        public final boolean keepMargin;
        /**
         * 边距系数，按「角标平均边长 × 该系数」计算。实测 0.6 可完整保留 38px 角标
         */
        public final float marginRatio;
        /**
         * 未提供角标尺寸时使用的固定边距（px）
         */
        public final int fallbackMargin;
        /**
         * 映射到原图之外的像素填充色
         */
        public final int borderColor;
        /**
         * 输出长边上限（px），超过则等比缩小；0 表示不限制
         */
        public final int maxOutputSize;
        /**
         * 是否双线性插值。{@code false} 为最近邻，更快但边缘有锯齿
         */
        public final boolean bilinear;

        private Config(Builder b) {
            this.normThreshold = b.normThreshold;
            this.lightWindowRatio = b.lightWindowRatio;
            this.closingKernel = b.closingKernel;
            this.sizeMinRatio = b.sizeMinRatio;
            this.sizeMaxRatio = b.sizeMaxRatio;
            this.sizeMinAbsolute = b.sizeMinAbsolute;
            this.aspectMin = b.aspectMin;
            this.aspectMax = b.aspectMax;
            this.fillMin = b.fillMin;
            this.fillMax = b.fillMax;
            this.ringBlackMin = b.ringBlackMin;
            this.gapWhiteMin = b.gapWhiteMin;
            this.solidityMin = b.solidityMin;
            this.solidityMax = b.solidityMax;
            this.cornerDistanceRatio = b.cornerDistanceRatio;
            this.keepMargin = b.keepMargin;
            this.marginRatio = b.marginRatio;
            this.fallbackMargin = b.fallbackMargin;
            this.borderColor = b.borderColor;
            this.maxOutputSize = b.maxOutputSize;
            this.bilinear = b.bilinear;
        }

        /**
         * 默认配置（即已验证的参数组合）
         */
        public static Config defaults() {
            return builder().build();
        }

        public static Builder builder() {
            return new Builder();
        }

        @Override
        public String toString() {
            return "Config{normThreshold=" + normThreshold
                    + ", sizeMinRatio=" + sizeMinRatio
                    + ", sizeMaxRatio=" + sizeMaxRatio
                    + ", sizeMinAbsolute=" + sizeMinAbsolute
                    + ", ringBlackMin=" + ringBlackMin
                    + ", gapWhiteMin=" + gapWhiteMin
                    + ", solidityMin=" + solidityMin
                    + ", solidityMax=" + solidityMax
                    + ", keepMargin=" + keepMargin
                    + ", marginRatio=" + marginRatio + "}";
        }

        /**
         * 配置构建器
         */
        public static final class Builder {
            private int normThreshold = 140;
            private float lightWindowRatio = 0.12f;
            private int closingKernel = 3;
            private float sizeMinRatio = 0.015f;
            private float sizeMaxRatio = 0.060f;
            private int sizeMinAbsolute = 16;
            private float aspectMin = 0.55f;
            private float aspectMax = 1.80f;
            private float fillMin = 0.30f;
            private float fillMax = 0.90f;
            private float ringBlackMin = 0.40f;
            private float gapWhiteMin = 0.20f;
            private float solidityMin = 0.60f;
            private float solidityMax = 0.95f;
            private float cornerDistanceRatio = 0.30f;
            private boolean keepMargin = true;
            private float marginRatio = 0.6f;
            private int fallbackMargin = 24;
            private int borderColor = Color.WHITE;
            private int maxOutputSize = 0;
            private boolean bilinear = true;

            public Builder normThreshold(int v) {
                this.normThreshold = v;
                return this;
            }

            public Builder lightWindowRatio(float v) {
                this.lightWindowRatio = v;
                return this;
            }

            public Builder closingKernel(int v) {
                this.closingKernel = v;
                return this;
            }

            public Builder sizeMinRatio(float v) {
                this.sizeMinRatio = v;
                return this;
            }

            public Builder sizeMaxRatio(float v) {
                this.sizeMaxRatio = v;
                return this;
            }

            public Builder sizeMinAbsolute(int v) {
                this.sizeMinAbsolute = v;
                return this;
            }

            public Builder aspectMin(float v) {
                this.aspectMin = v;
                return this;
            }

            public Builder aspectMax(float v) {
                this.aspectMax = v;
                return this;
            }

            public Builder fillMin(float v) {
                this.fillMin = v;
                return this;
            }

            public Builder fillMax(float v) {
                this.fillMax = v;
                return this;
            }

            public Builder ringBlackMin(float v) {
                this.ringBlackMin = v;
                return this;
            }

            public Builder gapWhiteMin(float v) {
                this.gapWhiteMin = v;
                return this;
            }

            public Builder solidityMin(float v) {
                this.solidityMin = v;
                return this;
            }

            public Builder solidityMax(float v) {
                this.solidityMax = v;
                return this;
            }

            public Builder cornerDistanceRatio(float v) {
                this.cornerDistanceRatio = v;
                return this;
            }

            public Builder keepMargin(boolean v) {
                this.keepMargin = v;
                return this;
            }

            public Builder marginRatio(float v) {
                this.marginRatio = v;
                return this;
            }

            public Builder fallbackMargin(int v) {
                this.fallbackMargin = v;
                return this;
            }

            public Builder borderColor(int v) {
                this.borderColor = v;
                return this;
            }

            public Builder maxOutputSize(int v) {
                this.maxOutputSize = v;
                return this;
            }

            public Builder bilinear(boolean v) {
                this.bilinear = v;
                return this;
            }

            public Config build() {
                return new Config(this);
            }
        }
    }

    /**
     * 内部候选结构
     */
    private static final class Candidate {
        final int minX, minY, maxX, maxY;
        final float centerX, centerY;
        final float fill, ringBlack, gapWhite, solidity;

        Candidate(int minX, int minY, int maxX, int maxY,
                  float centerX, float centerY,
                  float fill, float ringBlack, float gapWhite, float solidity) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.centerX = centerX;
            this.centerY = centerY;
            this.fill = fill;
            this.ringBlack = ringBlack;
            this.gapWhite = gapWhite;
            this.solidity = solidity;
        }
    }
}