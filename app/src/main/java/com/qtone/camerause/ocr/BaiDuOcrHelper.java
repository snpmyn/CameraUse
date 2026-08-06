package com.qtone.camerause.ocr;

import android.content.Context;

import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.model.GeneralBasicParams;
import com.baidu.ocr.sdk.model.GeneralParams;
import com.baidu.ocr.sdk.model.GeneralResult;
import com.baidu.ocr.sdk.model.OcrRequestParams;
import com.baidu.ocr.sdk.model.OcrResponseResult;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Created on 2026/8/6.
 *
 * @author 郑少鹏
 * @desc 百度 OCR 辅助者
 */
public class BaiDuOcrHelper {
    /**
     * 通用文字识别 (高精度含位置信息版)
     *
     * @param context          上下文
     * @param filePath         文件路径
     * @param onResultListener 结果监听
     */
    public static void recognizeAccurate(@NotNull Context context, String filePath, OnResultListener<GeneralResult> onResultListener) {
        GeneralParams generalParams = new GeneralParams();
        generalParams.setDetectDirection(true);
        generalParams.setImageFile(new File(filePath));
        OCR.getInstance(context.getApplicationContext()).recognizeAccurate(generalParams, onResultListener);
    }

    /**
     * 通用文字识别 (含生僻字版)
     *
     * @param context          上下文
     * @param filePath         文件路径
     * @param onResultListener 结果监听
     */
    public static void recognizeGeneralEnhanced(@NotNull Context context, String filePath, OnResultListener<GeneralResult> onResultListener) {
        GeneralBasicParams generalBasicParams = new GeneralBasicParams();
        generalBasicParams.setDetectDirection(true);
        generalBasicParams.setImageFile(new File(filePath));
        OCR.getInstance(context.getApplicationContext()).recognizeGeneralEnhanced(generalBasicParams, onResultListener);
    }

    /**
     * 试卷分析与识别
     *
     * @param context          上下文
     * @param filePath         文件路径
     * @param onResultListener 结果监听
     */
    public static void recognizeExampleDoc(@NotNull Context context, String filePath, OnResultListener<OcrResponseResult> onResultListener) {
        OcrRequestParams ocrRequestParams = new OcrRequestParams();
        ocrRequestParams.setImageFile(new File(filePath));
        OCR.getInstance(context.getApplicationContext()).recoginzeExampleDoc(ocrRequestParams, onResultListener);
    }

    /**
     * 手写文字识别
     *
     * @param context          上下文
     * @param filePath         文件路径
     * @param onResultListener 结果监听
     */
    public static void recoginzeWrittenText(@NotNull Context context, String filePath, OnResultListener<OcrResponseResult> onResultListener) {
        OcrRequestParams ocrRequestParams = new OcrRequestParams();
        ocrRequestParams.setImageFile(new File(filePath));
        OCR.getInstance(context.getApplicationContext()).recoginzeWrittenText(ocrRequestParams, onResultListener);
    }
}