plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.kotlin.kapt")
    // KotlinPoet专业重构 - 移除Hilt插件
    // alias(libs.plugins.hilt)
}

// 移除 KSP 配置
// ksp {
//     arg("room.schemaLocation", "$projectDir/schemas")
//     arg("room.incremental", "true")
//     arg("room.expandProjection", "true")
// }

android {
    namespace = "top.cywin.onetv.movie"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        // 注解处理器配置 - 按照原项目FongMi_TV配置
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "eventBusIndex" to "top.cywin.onetv.movie.event.EventIndex"
                )
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // 启用BuildConfig生成
    buildFeatures {
        buildConfig = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf(
            "-Xjvm-default=all",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    // 自定义资源目录配置 - 使用vod_前缀的资源结构
    sourceSets {
        getByName("main") {
            // 创建临时的标准资源目录结构
            res.srcDirs(
                "src/main/vod_res_processed"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // 解决Kotlin jar包冲突 - 使用新的语法
            pickFirsts.add("**/kotlin-compiler-embeddable*.jar")
            pickFirsts.add("**/kotlin-stdlib*.jar")
            pickFirsts.add("**/kotlin-reflect*.jar")
            pickFirsts.add("**/kotlin-scripting*.jar")
            // 处理META-INF冲突
            pickFirsts.add("META-INF/versions/9/previous-compilation-data.bin")
            pickFirsts.add("META-INF/com.onetv.tools/r8-from-*.version")
        }
    }
}

// 自定义资源处理任务 - 将vod_前缀目录转换为标准Android资源结构
tasks.register("processVodResources") {
    group = "movie"
    description = "处理vod_前缀的自定义资源目录结构"

    val sourceDir = file("src/main/vod_res")
    val targetDir = file("src/main/vod_res_processed")

    inputs.dir(sourceDir)
    outputs.dir(targetDir)

    doLast {
        // 清理目标目录
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
        targetDir.mkdirs()

        // 处理vod_values -> values
        val vodValuesDir = file("$sourceDir/vod_values")
        if (vodValuesDir.exists()) {
            val valuesDir = file("$targetDir/values")
            valuesDir.mkdirs()
            vodValuesDir.listFiles()?.forEach { file ->
                file.copyTo(File(valuesDir, file.name), overwrite = true)
            }
        }

        // 处理vod_drawable -> drawable
        val vodDrawableDir = file("$sourceDir/vod_drawable")
        if (vodDrawableDir.exists()) {
            val drawableDir = file("$targetDir/drawable")
            drawableDir.mkdirs()
            vodDrawableDir.listFiles()?.forEach { file ->
                file.copyTo(File(drawableDir, file.name), overwrite = true)
            }
        }

        println("✓ VOD资源处理完成: $sourceDir -> $targetDir")
    }
}

// 确保在资源合并前处理VOD资源
tasks.named("preBuild") {
    dependsOn("processVodResources")
}

dependencies {
    // 核心模块依赖
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:util"))

    // Android核心库
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // FongMi_TV架构依赖 - ViewModel支持
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // FongMi_TV架构依赖 - Fragment支持
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation(libs.androidx.appcompat)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // 网络请求
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)

    // FongMi_TV架构依赖 - 网络和解析
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jsoup:jsoup:1.17.2")

    // FongMi_TV架构依赖 - 事件总线
    implementation("org.greenrobot:eventbus:3.3.1")

    // FongMi_TV架构依赖 - 日志系统
    implementation("com.orhanobut:logger:2.2.0")

    // 图片加载
    implementation(libs.coil.compose)

    // 播放器 (复用现有)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.hls)
    // implementation(libs.androidx.media3.decoder.ffmpeg) // 暂时注释，避免仓库问题

    // FongMi_TV架构依赖 - 媒体和播放器扩展 (版本同步到1.6.1)
    implementation("androidx.media3:media3-session:1.6.1")
    implementation("androidx.media3:media3-common:1.6.1")
    implementation("androidx.media3:media3-datasource:1.6.1")

    // FongMi_TV架构依赖 - 通知和服务
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

    // 数据库 - Room依赖已在下方统一配置

    // KotlinPoet专业代码生成 - 替代Hilt依赖注入 (仅编译时使用)
    compileOnly(libs.kotlinpoet)
    compileOnly(libs.kotlinpoet.ksp)
    // 移除kotlin-compiler依赖，避免与KSP插件冲突
    // implementation(libs.kotlin.compiler)
    // implementation(libs.kotlin.scripting)

    // 协程
    implementation(libs.kotlinx.coroutines.android)

    // JSON处理
    implementation(libs.kotlinx.serialization.json)

    // Debug工具
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Cling DLNA/UPnP 依赖已在下方统一配置

    // QuickJS依赖 - 按照原项目FongMi_TV配置
    implementation("wang.harlon.quickjs:wrapper-java:3.2.0")
    implementation("wang.harlon.quickjs:wrapper-android:3.2.0")
    implementation("net.sourceforge.streamsupport:android-retrofuture:1.7.4")

    // FongMi_TV核心依赖 - 媒体播放器扩展 (版本同步到1.6.1)
    implementation("androidx.media3:media3-container:1.6.1")
    implementation("androidx.media3:media3-database:1.6.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.6.1")
    implementation("androidx.media3:media3-datasource-rtmp:1.6.1")
    implementation("androidx.media3:media3-decoder:1.6.1")
    implementation("androidx.media3:media3-effect:1.6.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.6.1")
    implementation("androidx.media3:media3-exoplayer-rtsp:1.6.1")
    implementation("androidx.media3:media3-exoplayer-smoothstreaming:1.6.1")
    implementation("androidx.media3:media3-extractor:1.6.1")

    // FongMi_TV核心依赖 - 网络协议支持
    implementation("com.github.thegrizzlylabs:sardine-android:0.9")
    implementation("com.github.teamnewpipe:NewPipeExtractor:v0.24.6")
    implementation("com.hierynomus:smbj:0.14.0")
    implementation("io.antmedia:rtmp-client:3.2.0")

    // FongMi_TV核心依赖 - 服务器和网络服务
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("org.eclipse.jetty:jetty-client:8.1.21.v20160908")
    implementation("org.eclipse.jetty:jetty-server:8.1.21.v20160908") {
        exclude(group = "org.eclipse.jetty.orbit", module = "javax.servlet")
    }
    implementation("org.eclipse.jetty:jetty-servlet:8.1.21.v20160908") {
        exclude(group = "org.eclipse.jetty.orbit", module = "javax.servlet")
    }
    implementation("javax.servlet:javax.servlet-api:3.1.0")

    // FongMi_TV核心依赖 - 图像处理
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.bumptech.glide:annotations:4.16.0")
    implementation("com.github.bumptech.glide:avif-integration:4.16.0") {
        exclude(group = "org.aomedia.avif.android", module = "avif")
    }
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0")
    implementation("org.aomedia.avif.android:avif:1.1.1.14d8e3c4")

    // FongMi_TV核心依赖 - 工具库
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.guolindev.permissionx:permissionx:1.8.0")
    implementation("org.simpleframework:simple-xml:2.7.1") {
        exclude(group = "stax", module = "stax-api")
        exclude(group = "xpp3", module = "xpp3")
    }
    implementation("cat.ereza:customactivityoncrash:2.4.0")

    // FongMi_TV核心依赖 - 本地AAR文件 (包含弹幕库)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))

    // FongMi_TV核心依赖 - 弹幕库NDK支持
    implementation("com.github.ctiao:ndkbitmap-armv7a:0.9.21")
    implementation("com.github.ctiao:ndkbitmap-armv5:0.9.21")
    implementation("com.github.ctiao:ndkbitmap-x86:0.9.21")

    // FongMi_TV核心依赖 - 缺少的重要依赖
    implementation("androidx.media:media:1.7.0")
    implementation("com.github.anilbeesetti.nextlib:nextlib-media3ext:0.8.4") {
        exclude(group = "androidx.media3")
    }
    implementation("com.github.bassaer:materialdesigncolors:1.0.0")
    implementation("com.github.jahirfiquitiva:TextDrawable:1.0.3")
    implementation("com.google.android.material:material:1.12.0")

    // FongMi_TV核心依赖 - Cling版本同步
    implementation("org.fourthline.cling:cling-core:2.1.1")
    implementation("org.fourthline.cling:cling-support:2.1.1")

    // FongMi_TV核心依赖 - Room数据库
    implementation("androidx.room:room-runtime:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")

    // FongMi_TV核心依赖 - 通用依赖 (移除构建变体特定依赖)
    implementation("androidx.leanback:leanback:1.2.0")
    implementation("com.github.JessYanCoding:AndroidAutoSize:1.2.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0") {
        isTransitive = false
    }

    // FongMi_TV核心依赖 - 注解处理器
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    kapt("org.greenrobot:eventbus-annotation-processor:3.3.1")

    // FongMi_TV核心依赖 - 核心库脱糖
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.4")
}
