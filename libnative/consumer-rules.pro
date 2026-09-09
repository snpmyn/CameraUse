# ==============================================================================
# 1. 保留该 Native 模块的所有 Java/Kotlin 接口类与包名
# ==============================================================================
# 替换为你的 libnative 实际 Java/Kotlin 包名（如 com.jiangdg.native code / com.qtone.native 等）
-keep class com.your.libnative.package.** { *; }
-dontwarn com.your.libnative.package.**

# ==============================================================================
# 2. C/C++ 与 JNI 交互的核心防混淆规则（关键）
# ==============================================================================
# 保留所有声明了 native 方法的类以及 native 方法声明本身
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留被 C/C++ 通过 JNI 反射调用的构造函数、字段和方法
# C++ 层如果使用 JNIEnv->FindClass / GetFieldID / GetMethodID 调用过任何类，必须 Keep
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# ==============================================================================
# 3. JNI 结构体 / 回调数据实体类（数据传递模型）
# ==============================================================================
# 如果 Native 层会将数据/结构体以 Bean/Model 的形式传回 Java 层，必须保持成员变量名不被混淆
-keepclassmembers class com.your.libnative.package.**$* {
    *;
}