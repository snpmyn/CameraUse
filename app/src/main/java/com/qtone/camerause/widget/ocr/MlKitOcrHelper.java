//package com.qtone.camerause.function.ocr;
//
//import android.content.Context;
//import android.net.Uri;
//import android.util.Log;
//
//import com.google.mlkit.vision.common.InputImage;
//import com.google.mlkit.vision.text.Text;
//import com.google.mlkit.vision.text.TextRecognition;
//import com.google.mlkit.vision.text.TextRecognizer;
//import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
//import com.qtone.camerause.utils.log.LogKit;
//
//import java.io.File;
//import java.io.IOException;
//
///**
// * Created on 2026/8/3.
// *
// * @author 郑少鹏
// * @desc ML Kit OCR 辅助者
// */
//public class MlKitOcrHelper {
//    /**
//     * 通过路径识别文本
//     *
//     * @param context  上下文
//     * @param filePath 文件路径
//     *                 本地文件绝对路径
//     */
//    public void recognizeTextFromPath(Context context, String filePath) {
//        if ((filePath == null) || filePath.trim().isEmpty()) {
//            Log.e(LogKit.TAG, "OCR 识别失败 - 图片路径不能为空");
//            return;
//        }
//        File imageFile = new File(filePath);
//        if (!imageFile.exists() || !imageFile.isFile()) {
//            Log.e(LogKit.TAG, "OCR 识别失败 - 找不到目标图片文件 || " + filePath);
//            return;
//        }
//        // 1. 初始化 TextRecognizer 客户端
//        TextRecognizer textRecognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
//        try {
//            // 2. 将本地绝对路径转为 Uri 并构建 InputImage
//            Uri imageUri = Uri.fromFile(imageFile);
//            Log.d(LogKit.TAG, "OCR 准备识别图片 - 路径 || " + filePath);
//            InputImage image = InputImage.fromFilePath(context, imageUri);
//            // 3. 执行识别
//            textRecognizer.process(image)
//                    .addOnSuccessListener(visionText -> {
//                        Log.d(LogKit.TAG, "========== OCR 识别成功 ==========");
//                        // 全部文本
//                        String resultText = visionText.getText();
//                        Log.d(LogKit.TAG, "全部文本\n" + resultText);
//                        // 分块打印
//                        int blockIndex = 0;
//                        for (Text.TextBlock block : visionText.getTextBlocks()) {
//                            Log.d(LogKit.TAG, "---- Block " + blockIndex + " ----");
//                            Log.d(LogKit.TAG, "Block 内容 || " + block.getText());
//                            int lineIndex = 0;
//                            for (Text.Line line : block.getLines()) {
//                                Log.d(LogKit.TAG, "Line [" + lineIndex + "] || " + line.getText());
//                                lineIndex++;
//                            }
//                            blockIndex++;
//                        }
//                        Log.d(LogKit.TAG, "========== OCR 识别结束 ==========");
//                        // 释放识别器资源
//                        textRecognizer.close();
//                    })
//                    .addOnFailureListener(e -> {
//                        Log.e(LogKit.TAG, "OCR 识别失败", e);
//                        // 释放识别器资源
//                        textRecognizer.close();
//                    });
//        } catch (IOException e) {
//            Log.e(LogKit.TAG, "创建 InputImage 失败 || " + e.getMessage(), e);
//            textRecognizer.close();
//        }
//    }
//}