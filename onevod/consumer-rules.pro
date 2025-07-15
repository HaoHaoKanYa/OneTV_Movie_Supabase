# OneTV onevod模块 ProGuard规则

# 保持所有公共API
-keep public class top.cywin.onetv.tv.** { *; }

# 保持爬虫相关类
-keep class com.github.catvod.** { *; }

# 保持解析器相关类
-keep class com.forcetech.** { *; }
-keep class com.jianpian.** { *; }
-keep class com.thunder.** { *; }
-keep class com.tvbus.** { *; }

# 保持Hook相关类
-keep class com.github.tvbox.osc.hook.** { *; }

# 保持JavaScript引擎
-keep class com.script.** { *; }

# 保持Python引擎
-keep class com.chaquo.python.** { *; }

# 保持Room数据库
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# 保持Gson序列化
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# 保持OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# 保持EventBus
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# 保持Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# 保持Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
