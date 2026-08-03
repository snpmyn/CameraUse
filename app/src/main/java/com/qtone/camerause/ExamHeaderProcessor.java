package com.qtone.camerause;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2026/7/30.
 *
 * @author 郑少鹏
 * @desc 试卷头部处理器
 */
public class ExamHeaderProcessor {
    private static final String TAG = ExamHeaderProcessor.class.getSimpleName();
    /**
     * 上下文
     */
    private final Context context;
    /**
     * 二维码扫描器
     */
    private final BarcodeScanner barcodeScanner;
    /**
     * 线程消息调度器
     */
    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * constructor
     *
     * @param context 上下文
     */
    public ExamHeaderProcessor(@NotNull Context context) {
        this.context = context.getApplicationContext();
        // 初始化 ML Kit 识别器 (仅针对 QR Code)
        BarcodeScannerOptions barcodeScannerOptions = new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build();
        this.barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions);
    }

    /**
     * 处理
     *
     * @param imagePath            图像路径
     * @param onHeaderCropCallback 头部裁剪回调
     */
    public void process(String imagePath, OnHeaderCropCallback onHeaderCropCallback) {
        if ((context == null) || (barcodeScanner == null)) {
            notifyError(onHeaderCropCallback, "Processor 未初始化");
            return;
        }
        try {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                notifyError(onHeaderCropCallback, "图片文件不存在");
                return;
            }
            // 1. 获取图片 Exif 旋转角度
            int exifDegrees = getExifRotationDegrees(imagePath);
            // 2. 针对低分辨率 / 远距离
            // 保留 100% 细节像素
            // 取消下采样 (inSampleSize = 1)
            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = 1;
            Bitmap rawBitmap = BitmapFactory.decodeFile(imagePath, decodeOptions);
            if (rawBitmap == null) {
                notifyError(onHeaderCropCallback, "解析图片 Bitmap 失败");
                return;
            }
            int originalWidth = rawBitmap.getWidth();
            int originalHeight = rawBitmap.getHeight();
            // 3. 根据 Exif 进行初始方向校正
            Bitmap rotatedBitmap = rotateBitmapByAngle(rawBitmap, exifDegrees);
            if ((rotatedBitmap != rawBitmap) && !rawBitmap.isRecycled()) {
                // 确保如果未旋转且生成了新 Bitmap 时，及时回收原始 rawBitmap 避免内存泄漏。
                rawBitmap.recycle();
            }
            // 4. 灰度化 + 拉伸黑白对比度预处理
            // 解决低分辨率下小二维码特征模糊问题
            Bitmap enhancedBitmap = enhanceLowResImage(rotatedBitmap);
            if ((enhancedBitmap != rotatedBitmap) && !rotatedBitmap.isRecycled()) {
                rotatedBitmap.recycle();
            }
            // 5. 开启多角度容错扫描模式
            tryRecognizeWithRotations(imagePath, enhancedBitmap, originalWidth, originalHeight, 0, onHeaderCropCallback);
        } catch (Exception e) {
            Log.e(TAG, "处理图片异常", e);
            notifyError(onHeaderCropCallback, "读取图片失败");
        }
    }

    /**
     * 低分辨率图像增强
     * <p>
     * 通过灰度化和拉伸黑白对比度，突出小二维码的特征。
     *
     * @param bitmap 像素数据
     * @return 像素数据
     */
    private Bitmap enhanceLowResImage(@NotNull Bitmap bitmap) {
        Bitmap bitmapGray = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapGray);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        // 灰度化
        colorMatrix.setSaturation(0);
        // 提高 1.4 倍对比度
        float contrast = 1.4f;
        float translate = (-0.5f * contrast + 0.5f) * 255f;
        float[] matrix = {
                contrast, 0, 0, 0, translate,
                0, contrast, 0, 0, translate,
                0, 0, contrast, 0, translate,
                0, 0, 0, 1, 0
        };
        colorMatrix.postConcat(new ColorMatrix(matrix));
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return bitmapGray;
    }

    /**
     * 多角度旋转容错识别
     *
     * @param imagePath            图像路径
     *                             绝对路径
     * @param bitmap               像素数据
     *                             当前待检 Bitmap
     * @param imageOriginalWidth   图像原始宽
     * @param imageOriginalHeight  图像原始高
     * @param attemptStep          尝试步骤
     *                             依次尝试 0°、90°、180°、270° 旋转角度识别二维码
     * @param onHeaderCropCallback 头部裁剪回调
     */
    private void tryRecognizeWithRotations(String imagePath, Bitmap bitmap, int imageOriginalWidth, int imageOriginalHeight, int attemptStep, OnHeaderCropCallback onHeaderCropCallback) {
        InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
        barcodeScanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    if ((barcodes == null) || barcodes.isEmpty()) {
                        // 当前角度未找到 -> 尝试旋转 90 度继续识别
                        if (attemptStep < 3) {
                            Bitmap nextRotated = rotateBitmapByAngle(bitmap, 90);
                            tryRecognizeWithRotations(imagePath, nextRotated, imageOriginalWidth, imageOriginalHeight, attemptStep + 1, onHeaderCropCallback);
                            if ((nextRotated != bitmap) && !bitmap.isRecycled()) {
                                bitmap.recycle();
                            }
                        } else {
                            // 4 个方向全尝试完毕仍无二维码
                            Log.d(TAG, "未在图片中检测到二维码 (已尝试全部 4 个方向)");
                            if (!bitmap.isRecycled()) {
                                bitmap.recycle();
                            }
                            notifySkip(onHeaderCropCallback, "未检测到二维码");
                        }
                        return;
                    }
                    // 未经下采样
                    // 放缩比例为 1:1 (精确映射原图坐标)
                    float scaleX = (float) bitmap.getWidth() / (float) imageOriginalWidth;
                    float scaleY = (float) bitmap.getHeight() / (float) imageOriginalHeight;
                    // 解析坐标 -> 直接裁剪
                    handleDetectedBarcodes(imagePath, barcodes, imageOriginalWidth, imageOriginalHeight, scaleX, scaleY, onHeaderCropCallback);
                    if (!bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "ML Kit 扫码失败 || " + e.getMessage(), e);
                    if (!bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                    notifyError(onHeaderCropCallback, "扫码异常 || " + e.getMessage());
                });
    }

    /**
     * 处理已检测到的二维码
     * <p>
     * 解析坐标、换算原图尺寸并直接触发裁剪
     *
     * @param imagePath            图像路径
     *                             原始图像路径
     * @param barcodes             检测出的二维码列表
     * @param imageOriginalWidth   图像原始宽
     * @param imageOriginalHeight  图像原始高
     * @param scaleX               X方向采样缩放比
     * @param scaleY               Y方向采样缩放比
     * @param onHeaderCropCallback 头部裁剪回调
     */
    private void handleDetectedBarcodes(String imagePath, @NotNull List<Barcode> barcodes, int imageOriginalWidth, int imageOriginalHeight, float scaleX, float scaleY, OnHeaderCropCallback onHeaderCropCallback) {
        List<Barcode> validBarcodes = new ArrayList<>();
        for (Barcode barcode : barcodes) {
            if ((barcode != null) && (barcode.getBoundingBox() != null)) {
                validBarcodes.add(barcode);
            }
        }
        if (validBarcodes.isEmpty()) {
            notifySkip(onHeaderCropCallback, "未能获取到有效坐标信息");
            return;
        }
        // 按 Y 轴中心点 (从大到小) 排序，优先处理靠下的二维码。
        validBarcodes.sort((b1, b2) -> {
            Rect box1 = b1.getBoundingBox();
            Rect box2 = b2.getBoundingBox();
            int y1 = (box1 != null) ? box1.centerY() : 0;
            int y2 = (box2 != null) ? box2.centerY() : 0;
            return Integer.compare(y2, y1);
        });
        Barcode currentQrCode = validBarcodes.get(0);
        Rect scaledQrCodeRect = currentQrCode.getBoundingBox();
        if (scaledQrCodeRect == null) {
            notifySkip(onHeaderCropCallback, "未能获取到有效坐标信息");
            return;
        }
        // 坐标还原到原图尺寸
        Rect qrRect = new Rect(
                Math.round(scaledQrCodeRect.left / scaleX),
                Math.round(scaledQrCodeRect.top / scaleY),
                Math.round(scaledQrCodeRect.right / scaleX),
                Math.round(scaledQrCodeRect.bottom / scaleY)
        );
        String qrCodeContent = currentQrCode.getRawValue();
        Log.d(TAG, String.format("当前试卷二维码 (映射原图): 内容 = [%s], 坐标 = [L:%d, T:%d, R:%d, B:%d], 中心点 Y = %d",
                qrCodeContent, qrRect.left, qrRect.top, qrRect.right, qrRect.bottom, qrRect.centerY()));
        int qrCenterY = qrRect.centerY();
        // 适度放宽触发区域 (10% ~ 90%)
        // 提升扫描仪低帧率场景下的捕获概率
        int triggerMinY = (int) (imageOriginalHeight * 0.10);
        int triggerMaxY = (int) (imageOriginalHeight * 0.90);
        // 只要落在有效触发区域内，直接触发裁剪。
        if (qrCenterY >= triggerMinY && qrCenterY <= triggerMaxY) {
            // 根据换算后的原图二维码坐标计算试卷头区域
            Rect headerRect = calculateExamHeaderRect(qrRect, imageOriginalWidth, imageOriginalHeight);
            // 使用 RegionDecoder 从磁盘图片高保真裁剪高清原图
            Bitmap headerBitmap = cropImageWithRegionDecoder(imagePath, headerRect);
            if (headerBitmap != null) {
                // 异步保存小图文件，方便后面调上传接口。
                saveBitmapAsync(headerBitmap, (savedFile) -> notifySuccess(onHeaderCropCallback, headerBitmap, qrCodeContent, savedFile));
            } else {
                notifyError(onHeaderCropCallback, "图片裁剪失败");
            }
        } else {
            Log.d(TAG, "跳过裁剪：未落入触发区 Y = " + qrCenterY);
            notifySkip(onHeaderCropCallback, "未落入黄金触发区");
        }
    }

    @NotNull
    private Rect calculateExamHeaderRect(@NotNull Rect qrRect, int imgWidth, int imgHeight) {
        int qrX = qrRect.left;
        int qrY = qrRect.top;
        int qrW = qrRect.width();
        int qrH = qrRect.height();

        // 针对远距离小二维码，适当加大比例，保证完整裁剪包含试卷头的区域。
        int leftOffset = (int) (qrW * 1.0f);
        int topOffset = (int) (qrH * 1.0f);
        int headerWidth = (int) (qrW * 10.0f);
        int headerHeight = (int) (qrH * 5.0f);

        int cropLeft = Math.max(0, qrX - leftOffset);
        int cropTop = Math.max(0, qrY - topOffset);
        int cropRight = Math.min(imgWidth, cropLeft + headerWidth);
        int cropBottom = Math.min(imgHeight, cropTop + headerHeight);

        return new Rect(cropLeft, cropTop, cropRight, cropBottom);
    }

    @Nullable
    private Bitmap cropImageWithRegionDecoder(String imagePath, Rect cropRect) {
        BitmapRegionDecoder bitmapRegionDecoder = null;
        try {
            // API 31 及以上使用单参数 API
            // 旧版本使用双参数 API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bitmapRegionDecoder = BitmapRegionDecoder.newInstance(imagePath);
            } else {
                @SuppressWarnings("deprecation")
                BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(imagePath, false);
                bitmapRegionDecoder = decoder;
            }
            if (bitmapRegionDecoder == null) {
                return null;
            }
            Rect validRect = new Rect(
                    Math.max(0, Math.min(cropRect.left, bitmapRegionDecoder.getWidth())),
                    Math.max(0, Math.min(cropRect.top, bitmapRegionDecoder.getHeight())),
                    Math.max(0, Math.min(cropRect.right, bitmapRegionDecoder.getWidth())),
                    Math.max(0, Math.min(cropRect.bottom, bitmapRegionDecoder.getHeight()))
            );
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return bitmapRegionDecoder.decodeRegion(validRect, options);
        } catch (Exception e) {
            Log.e(TAG, "BitmapRegionDecoder 裁剪出错", e);
            return null;
        } finally {
            if (bitmapRegionDecoder != null) {
                bitmapRegionDecoder.recycle();
            }
        }
    }

    /**
     * 获取图片 EXIF 旋转角度
     * <p>
     * 解析 JPEG 元数据中的方向信息 (0°、90°、180° 或 270°)
     *
     * @param imagePath 图像路径
     * @return 图片 EXIF 旋转角度
     */
    private int getExifRotationDegrees(String imagePath) {
        try {
            ExifInterface exifInterface = new ExifInterface(imagePath);
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (IOException e) {
            Log.e(TAG, "读取 Exif 属性失败", e);
            return 0;
        }
    }

    /**
     * 根据角度旋转 Bitmap
     *
     * @param source 资源
     *               像素数据
     * @param angle  角度
     * @return Bitmap
     */
    private Bitmap rotateBitmapByAngle(Bitmap source, int angle) {
        if ((angle == 0) || (source == null)) {
            return source;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap rotated = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        if (rotated != source && !source.isRecycled()) {
            source.recycle();
        }
        return rotated;
    }

    /**
     * 异步保存像素数据
     *
     * @param bitmap             素数据
     * @param onFileSaveListener 文件保存监听
     */
    private void saveBitmapAsync(Bitmap bitmap, OnFileSaveListener onFileSaveListener) {
        new Thread(() -> {
            File cacheDir = context.getCacheDir();
            File cropFile = new File(cacheDir, "HEADER_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream fileOutputStream = new FileOutputStream(cropFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
                fileOutputStream.flush();
                if (onFileSaveListener != null) {
                    onFileSaveListener.onSaved(cropFile);
                }
            } catch (IOException e) {
                Log.e(TAG, "保存裁剪小图失败", e);
                if (onFileSaveListener != null) {
                    onFileSaveListener.onSaved(null);
                }
            }
        }).start();
    }

    /**
     * 通知成功
     *
     * @param onHeaderCropCallback 头部裁剪回调
     * @param bitmap               像素数据
     * @param content              内容
     * @param file                 文件
     */
    private void notifySuccess(OnHeaderCropCallback onHeaderCropCallback, Bitmap bitmap, String content, File file) {
        if (onHeaderCropCallback == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onHeaderCropCallback.onHeaderCropSuccess(bitmap, content, file);
        } else {
            handler.post(() -> onHeaderCropCallback.onHeaderCropSuccess(bitmap, content, file));
        }
    }

    /**
     * 通知跳过
     *
     * @param onHeaderCropCallback 头部裁剪回调
     * @param reason               原因
     */
    private void notifySkip(OnHeaderCropCallback onHeaderCropCallback, String reason) {
        if (onHeaderCropCallback == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onHeaderCropCallback.onHeaderCropSkip(reason);
        } else {
            handler.post(() -> onHeaderCropCallback.onHeaderCropSkip(reason));
        }
    }

    /**
     * 通知错误
     *
     * @param onHeaderCropCallback 头部裁剪回调
     * @param errorMsg             错误消息
     */
    private void notifyError(OnHeaderCropCallback onHeaderCropCallback, String errorMsg) {
        if (onHeaderCropCallback == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onHeaderCropCallback.onHeaderCropError(errorMsg);
        } else {
            handler.post(() -> onHeaderCropCallback.onHeaderCropError(errorMsg));
        }
    }

    /**
     * 销毁
     */
    public void destroy() {
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
    }

    /**
     * 头部裁剪回调
     */
    public interface OnHeaderCropCallback {
        /**
         * 头部裁剪成功
         *
         * @param headerBitmap  头像素数据
         * @param qrCodeContent 二维码内容
         * @param cropFile      裁剪文件
         *                      异步保存的本地图片文件
         *                      方便直接拿去上传接口
         */
        void onHeaderCropSuccess(Bitmap headerBitmap, String qrCodeContent, File cropFile);

        /**
         * 头部裁剪跳过
         * <p>
         * 业务过滤 (未在触发区、缝隙未识别到二维码等)
         *
         * @param reason 原因
         */
        void onHeaderCropSkip(String reason);

        /**
         * 头部裁剪错误
         * <p>
         * 系统错误 (拍照失败、引擎崩溃、文件读取失败、图片裁剪失败等)
         *
         * @param errorMsg 错误消息
         */
        void onHeaderCropError(String errorMsg);
    }

    /**
     * 文件保存监听
     */
    private interface OnFileSaveListener {
        /**
         * 已保存
         *
         * @param file 文件
         */
        void onSaved(File file);
    }
}