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
# 1. libausbc 自身包名防混淆 (包含自定义控件、Camera 预览组件、ROI 处理类等)
# ==============================================================================
-keep class com.jiangdg.ausbc.** { *; }
-dontwarn com.jiangdg.ausbc.**

# ==============================================================================
# 2. 本地 NDK / JNI 防混淆 (核心：防止 C/C++ 底层回调 Java/Kotlin 方法崩溃)
# ==============================================================================
# 保留所有包含 native 方法的类名及 native 方法体
-keepclasseswithmembernames class * {
    native <methods>;
}

# ==============================================================================
# 3. 依赖模块 libnative 与 libuvc 的防混淆 (防止底层 C/C++ 反射加载类失败)
# ==============================================================================
# 如果你的 libnative / libuvc 中有固定包名（如 com.jiangdg.uvc 或 com.serenegiant.** 等）
# 建议将对应 Native 接口包名进行 keep，防止底层 C++ 通过 FindClass 查找失败
-keep class com.serenegiant.** { *; }
-dontwarn com.serenegiant.**

# 保持所有被 NDK / JNI C++ 层回调的 Java 字段和方法（如 Surface, Native 方法回调接口等）
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}