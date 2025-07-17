# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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

# ===== FongMi_TV架构相关混淆规则 =====

# 保持所有Bean类不被混淆
-keep class top.cywin.onetv.movie.bean.** { *; }

# 保持所有API接口不被混淆
-keep class top.cywin.onetv.movie.api.** { *; }

# 保持所有ViewModel不被混淆
-keep class top.cywin.onetv.movie.model.** { *; }

# 保持所有事件类不被混淆
-keep class top.cywin.onetv.movie.event.** { *; }

# 保持所有回调接口不被混淆
-keep class top.cywin.onetv.movie.impl.** { *; }

# 保持所有异常类不被混淆
-keep class top.cywin.onetv.movie.exception.** { *; }

# 保持所有工具类不被混淆
-keep class top.cywin.onetv.movie.utils.** { *; }

# 保持所有服务器相关类不被混淆
-keep class top.cywin.onetv.movie.server.** { *; }

# 保持所有服务类不被混淆
-keep class top.cywin.onetv.movie.service.** { *; }

# 保持所有接收器类不被混淆
-keep class top.cywin.onetv.movie.receiver.** { *; }

# 保持所有播放器相关类不被混淆
-keep class top.cywin.onetv.movie.player.** { *; }

# 保持所有网络配置类不被混淆
-keep class top.cywin.onetv.movie.network.** { *; }

# 保持所有网盘解析类不被混淆
-keep class top.cywin.onetv.movie.cloudrive.** { *; }

# 保持所有适配器类不被混淆
-keep class top.cywin.onetv.movie.adapter.** { *; }

# ===== 第三方库混淆规则 =====

# Gson混淆规则
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# OkHttp混淆规则
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# EventBus混淆规则
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Logger混淆规则
-keep class com.orhanobut.logger.** { *; }

# Jsoup混淆规则
-keep class org.jsoup.** { *; }

# ExoPlayer混淆规则
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**

# Room混淆规则
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ===== 反射相关混淆规则 =====

# 保持所有使用反射的类
-keepclassmembers class * {
    public <init>(...);
}

# 保持所有枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保持所有Serializable类
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===== 网络安全相关 =====

# 保持SSL相关类
-keep class javax.net.ssl.** { *; }
-keep class org.apache.http.** { *; }

# 保持网络配置类
-keep class android.security.NetworkSecurityPolicy { *; }

# ===== 其他优化规则 =====

# 移除日志
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# 优化
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify
