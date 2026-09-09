# ==============================================================================
# 1. 保留 libuvccommon 的核心工具类与 Java/Kotlin 包名
# ==============================================================================
# 保留 sPort/serenegiant 的通用工具包（根据你库中实际包名调整，通常为 com.serenegiant.**）
-keep class com.serenegiant.common.** { *; }
-keep class com.serenegiant.utils.** { *; }
-keep class com.serenegiant.glutils.** { *; }
-dontwarn com.serenegiant.**

# ==============================================================================
# 2. JNI & C/C++ 通用回调与基础方法
# ==============================================================================
# 保留所有声明了 native 方法的类名及 native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留被 C/C++ 反射调用的字段、构造方法及带有 @Keep 注解的方法
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# ==============================================================================
# 3. AndroidX 控件与序列化兼容 (RecyclerView/Preference/Legacy)
# ==============================================================================
# 保留通过 XML 或反射加载的 Custom View
-keepclasseswithmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# 保留 Parcelable 序列化对象
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}