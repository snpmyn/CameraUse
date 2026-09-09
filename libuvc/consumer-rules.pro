# ==============================================================================
# 1. 保留 libuvc 库的所有 Java/Kotlin 包名与 Native 接口类
# ==============================================================================
# 替换为你 libuvc 模块实际的包名（常用如 com.serenegiant.usb.** 或 com.jiangdg.uvc.** 等）
-keep class com.serenegiant.usb.** { *; }
-keep class com.serenegiant.widget.** { *; }
-dontwarn com.serenegiant.**

# ==============================================================================
# 2. C/C++ 与 JNI 交互核心规则（防止 C++ 通过 FindClass 查找 Java 类失败）
# ==============================================================================
# 保留所有包含 native 方法的类名及 native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留被 C/C++ 回调的字段、方法和内部类（如 Frame 回调、USB 传输回调）
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# ==============================================================================
# 3. 日志与依赖库（xlog & libuvccommon）
# ==============================================================================
# XLog
-keep class com.elvishew.xlog.** { *; }
-dontwarn com.elvishew.xlog.**

# libuvccommon 对应包名
-keep class com.serenegiant.common.** { *; }
-dontwarn com.serenegiant.common.**