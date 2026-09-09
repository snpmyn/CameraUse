# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ==============================================================================
# 1. 百度 OCR
# ==============================================================================
-keep class com.baidu.ocr.sdk.** { *; }
-dontwarn com.baidu.ocr.**

# ==============================================================================
# 2. OpenCV
# ==============================================================================
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# ==============================================================================
# 3. Tencent MMKV
# ==============================================================================
-keep class com.tencent.mmkv.** { *; }
-keepclassmembers class com.tencent.mmkv.** {
    native <methods>;
}

# ==============================================================================
# 4. ZXing & ML Kit & MediaPipe
# ==============================================================================
# ZXing
-keep class com.google.zxing.** { *; }

# ML Kit Barcode Scanning
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# MediaPipe Tasks Vision & Protobuf
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keep class com.google.protobuf.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# ==============================================================================
# 5. Glide & Luban
# ==============================================================================
# Glide
-keep class com.bumptech.glide.** { *; }
-keep class * implements com.bumptech.glide.module.GlideModule
-keepclassmembers class * {
    @com.bumptech.glide.annotation.GlideModule <init>(...);
}

# Luban
-keep class top.zibin.luban.** { *; }

# ==============================================================================
# 6. 三方库
# ==============================================================================
# RxJava 1.x
-keep class rx.** { *; }
-dontwarn rx.**

# Timber
-keep class timber.log.** { *; }

# ImmersionBar
-keep class com.gyf.immersionbar.** { *; }

# PermissionX
-keep class com.guolindev.permissionx.** { *; }

# RxBus
-dontwarn util.rxbus.**
-keep class com.qtone.camerause.util.rxbus.** {*;}
-keep class com.qtone.camerause.util.rxbus.finder.** {*;}
-keep class com.qtone.camerause.util.rxbus.thread.EventThread {*;}
-keepclassmembers class ** {
    @com.qtone.camerause.util.rxbus.annotation.Subscribe public *;
    @com.qtone.camerause.util.rxbus.annotation.Produce public *;
    public void onEvent(**);
    public void onEventMainThread(**);
}

# ==============================================================================
# 7. Android 基础框架与 JNI 通用规则
# ==============================================================================
# 保留所有包含 native 方法的类名及 native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留 Parcelable 序列化类
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ViewBinding / DataBinding 保持
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(...);
}

# 保留 R 文件和 BuildConfig
-keep class **.R$* { *; }
-keep class **.BuildConfig { *; }

# ==============================================================================
# 8. UVC / Camera (ausbc & jiangdg & serenegiant)
# ==============================================================================
# 修复核心：JNI 注册依赖 com.jiangdg.uvc.UVCCamera 及相关 Callback 接口
-keep class com.jiangdg.uvc.** { *; }
-keep interface com.jiangdg.uvc.** { *; }

# 保持 ausbc 相机框架与相关 USB 包
-keep class com.jiangdg.ausbc.** { *; }
-keep interface com.jiangdg.ausbc.** { *; }
-keep class com.jiangdg.usb.** { *; }

# 保持 native 方法及其所在的类
-keepclassmembers class com.jiangdg.** {
    native <methods>;
}

# 保持底层 saki / serenegiant 库
-keep class com.serenegiant.** { *; }
-keep interface com.serenegiant.** { *; }
-keepclassmembers class com.serenegiant.** {
    native <methods>;
}
-dontwarn com.serenegiant.**
-dontwarn com.jiangdg.**