plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp) // 保留KSP用于Room
    // KotlinPoet专业重构 - 移除Hilt插件
    // alias(libs.plugins.hilt)
}

android {
    namespace = "top.cywin.onetv.movie"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    kotlinOptions {
        jvmTarget = "1.8"
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

    // 图片加载
    implementation(libs.coil.compose)

    // 播放器 (复用现有)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.hls)
    // implementation(libs.androidx.media3.decoder.ffmpeg) // 暂时注释，避免仓库问题

    // 数据库
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

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
}
