//package com.qtone.camerause;
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
//    private static final String TAG = MlKitOcrHelper.class.getSimpleName();
//
//    /**
//     * 根据本地绝对路径识别图片文本
//     *
//     * @param context  上下文
//     * @param filePath 本地文件绝对路径
//     */
//    public void recognizeTextFromPath(Context context, String filePath) {
//        // 1. 初始化 TextRecognizer 客户端
//        TextRecognizer recognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
//        try {
//            // 2. 将本地绝对路径转为 File 和 Uri 对象
//            File imageFile = new File(filePath);
//            Uri imageUri = Uri.fromFile(imageFile);
//            Log.d(TAG, "图片路径 || " + filePath);
//            Log.d(TAG, "文件是否存在 || " + imageFile.exists());
//            // 3. 从 File Path 构建 InputImage
//            InputImage image = InputImage.fromFilePath(context, imageUri);
//            // 4. 执行识别
//            recognizer.process(image)
//                    .addOnSuccessListener(visionText -> {
//                        Log.d(TAG, "========== OCR 识别成功 ==========");
//                        // 全部文本
//                        String resultText = visionText.getText();
//                        Log.d(TAG, "全部文本：\n" + resultText);
//                        // 分块打印
//                        int blockIndex = 0;
//                        for (Text.TextBlock block : visionText.getTextBlocks()) {
//                            Log.d(TAG, "---- Block " + blockIndex + " ----");
//                            Log.d(TAG, "Block内容：" + block.getText());
//                            int lineIndex = 0;
//                            for (Text.Line line : block.getLines()) {
//                                Log.d(TAG, "Line[" + lineIndex + "]：" + line.getText());
//                                lineIndex++;
//                            }
//                            blockIndex++;
//                        }
//                        Log.d(TAG, "========== OCR 识别结束 ==========");
//                    })
//                    .addOnFailureListener(e -> Log.e(TAG, "OCR 识别失败", e));
//        } catch (IOException e) {
//            Log.e(TAG, "创建 InputImage 失败", e);
//        }
//    }
//}