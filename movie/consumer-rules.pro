# OneTV Movie Module ProGuard Rules

# Keep KotlinPoet classes (编译时工具，但可能在运行时被引用)
-keep class com.squareup.kotlinpoet.** { *; }
-dontwarn com.squareup.kotlinpoet.**

# Keep javax.lang.model classes (KotlinPoet依赖)
-dontwarn javax.lang.model.**
-keep class javax.lang.model.** { *; }

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep VOD data models
-keep class top.cywin.onetv.movie.data.models.** { *; }
-keep class top.cywin.onetv.movie.data.network.** { *; }

# Keep generated adapters
-keep class top.cywin.onetv.movie.codegen.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Keep JsonElement and related classes
-keep class kotlinx.serialization.json.JsonElement { *; }
-keep class kotlinx.serialization.json.JsonPrimitive { *; }
-keep class kotlinx.serialization.json.JsonObject { *; }
-keep class kotlinx.serialization.json.JsonArray { *; }

# ===== FongMi_TV架构相关消费者规则 =====

# 保持所有Bean类
-keep class top.cywin.onetv.movie.bean.** { *; }

# 保持所有API接口
-keep class top.cywin.onetv.movie.api.** { *; }

# 保持所有ViewModel
-keep class top.cywin.onetv.movie.model.** { *; }

# 保持所有事件类
-keep class top.cywin.onetv.movie.event.** { *; }

# 保持所有适配器
-keep class top.cywin.onetv.movie.adapter.** { *; }

# 保持所有工具类
-keep class top.cywin.onetv.movie.utils.** { *; }

# Gson规则
-keep class com.google.gson.** { *; }
-keepattributes Signature

# EventBus规则
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}

# Jsoup规则
-keep class org.jsoup.** { *; }
