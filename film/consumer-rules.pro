# Film 模块混淆规则

# ========== Kotlin 相关 ==========
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# ========== Kotlinx Serialization ==========
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class top.cywin.onetv.film.**$$serializer { *; }
-keepclassmembers class top.cywin.onetv.film.** {
    *** Companion;
}
-keepclasseswithmembers class top.cywin.onetv.film.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ========== QuickJS 相关 ==========
-keep class com.github.seven332.quickjs.** { *; }
-keepclassmembers class ** {
    native <methods>;
}

# ========== OkHttp 相关 ==========
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ========== Jsoup 相关 ==========
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# ========== Room 相关 ==========
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# ========== Film 模块核心类 ==========
-keep class top.cywin.onetv.film.FilmApp { *; }
-keep class top.cywin.onetv.film.spider.** { *; }
-keep class top.cywin.onetv.film.engine.** { *; }
-keep class top.cywin.onetv.film.data.models.** { *; }

# ========== Spider 相关 ==========
-keep class * extends top.cywin.onetv.film.spider.Spider { *; }
-keepclassmembers class * extends top.cywin.onetv.film.spider.Spider {
    public <methods>;
}

# ========== 反射相关 ==========
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
