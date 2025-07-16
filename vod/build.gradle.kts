import org.gradle.process.CommandLineArgumentProvider

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    // Python插件只在chaquo子模块中使用，避免冲突
}

android {
    namespace ="top.cywin.onetv.vod"
    //namespace = project.property("APP_APPLICATION_ID") as String
    compileSdk = libs.versions.compileSdk.get().toInt()

    flavorDimensions += listOf("mode", "api", "abi")
    productFlavors {
        create("leanback") {
            dimension = "mode"
            buildConfigField("boolean", "FLAVOR_LEANBACK", "true")
            buildConfigField("boolean", "FLAVOR_MOBILE", "false")
        }
        // create("mobile") { dimension = "mode" }
        create("java") {
            dimension = "api"
            buildConfigField("boolean", "FLAVOR_JAVA", "true")
            buildConfigField("boolean", "FLAVOR_PYTHON", "false")
        }
        // create("python") { dimension = "api" }
        create("arm64_v8a") { dimension = "abi" }
        // create("armeabi_v7a") { dimension = "abi" }
    }

    variantFilter {
        val mode = flavors.find { it.dimension == "mode" }?.name
        val api = flavors.find { it.dimension == "api" }?.name
        val abi = flavors.find { it.dimension == "abi" }?.name
        if (!(mode == "leanback" && api == "java" && abi == "arm64_v8a")) {
            ignore = true
        }
    }

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas"
                )
            }
        }

        // 库模块：移除应用级BuildConfig字段，使用库模块专用配置
        // 库模块不需要APPLICATION_ID、VERSION_NAME、VERSION_CODE
        // 这些信息将由主应用(TV模块)提供
    }
    
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    packaging {
        resources {
            excludes += listOf(
                "META-INF/beans.xml",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            )
        }
    }
    
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true  // 启用BuildConfig生成
    }
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    
    // 子模块依赖
    implementation(project(":vod:catvod"))
    implementation(project(":vod:chaquo"))
    implementation(project(":vod:quickjs"))
    implementation(project(":vod:hook"))
    implementation(project(":vod:forcetech"))
    implementation(project(":vod:jianpian"))
    implementation(project(":vod:thunder"))
    implementation(project(":vod:tvbus"))
    
    // 核心Android依赖
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.media:media:1.7.0")
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("com.google.android.material:material:1.12.0")
    
    // Media3依赖
    implementation("androidx.media3:media3-common:1.6.1")
    implementation("androidx.media3:media3-container:1.6.1")
    implementation("androidx.media3:media3-database:1.6.1")
    implementation("androidx.media3:media3-datasource:1.6.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.6.1")
    implementation("androidx.media3:media3-datasource-rtmp:1.6.1")
    implementation("androidx.media3:media3-decoder:1.6.1")
    implementation("androidx.media3:media3-effect:1.6.1")
    implementation("androidx.media3:media3-exoplayer:1.6.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.6.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.6.1")
    implementation("androidx.media3:media3-exoplayer-rtsp:1.6.1")
    implementation("androidx.media3:media3-exoplayer-smoothstreaming:1.6.1")
    implementation("androidx.media3:media3-extractor:1.6.1")
    implementation("androidx.media3:media3-ui:1.6.1")
    
    // 网络和解析
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:5.0.0-alpha.14")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.14")
    implementation("com.google.code.gson:gson:2.11.0")
    
    // UI组件
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.bumptech.glide:annotations:4.16.0")
    implementation("com.github.bumptech.glide:avif-integration:4.16.0") {
        exclude(group = "org.aomedia.avif.android", module = "avif")
    }
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0")
    
    // 其他依赖
    implementation("cat.ereza:customactivityoncrash:2.4.0")
    implementation("com.github.anilbeesetti.nextlib:nextlib-media3ext:0.8.4") {
        exclude(group = "androidx.media3")
    }
    implementation("com.github.bassaer:materialdesigncolors:1.0.0")
    implementation("com.github.jahirfiquitiva:TextDrawable:1.0.3")
    implementation("com.github.thegrizzlylabs:sardine-android:0.9")
    implementation("com.github.teamnewpipe:NewPipeExtractor:v0.24.6")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.guolindev.permissionx:permissionx:1.8.0")
    implementation("com.hierynomus:smbj:0.14.0")
    implementation("io.antmedia:rtmp-client:3.2.0")
    implementation("javax.servlet:javax.servlet-api:3.1.0")
    implementation("org.aomedia.avif.android:avif:1.1.1.14d8e3c4")
    implementation("org.eclipse.jetty:jetty-client:8.1.21.v20160908")
    implementation("org.eclipse.jetty:jetty-server:8.1.21.v20160908") {
        exclude(group = "org.eclipse.jetty.orbit", module = "javax.servlet")
    }
    implementation("org.eclipse.jetty:jetty-servlet:8.1.21.v20160908") {
        exclude(group = "org.eclipse.jetty.orbit", module = "javax.servlet")
    }
    implementation("org.fourthline.cling:cling-core:2.1.1")
    implementation("org.fourthline.cling:cling-support:2.1.1")
    implementation("org.greenrobot:eventbus:3.3.1")
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("org.simpleframework:simple-xml:2.7.1") {
        exclude(group = "stax", module = "stax-api")
        exclude(group = "xpp3", module = "xpp3")
    }
    
    // Leanback特定依赖
    // "leanbackImplementation"("androidx.leanback:leanback:1.0.0")
    implementation("androidx.leanback:leanback:1.0.0")
    // "leanbackImplementation"("com.github.JessYanCoding:AndroidAutoSize:1.2.1")
    implementation("com.github.JessYanCoding:AndroidAutoSize:1.2.1")
    
    // Mobile特定依赖
    // "mobileImplementation"("androidx.biometric:biometric:1.1.0")
    implementation("androidx.biometric:biometric:1.1.0")
    // "mobileImplementation"("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    // "mobileImplementation"("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    // "mobileImplementation"("com.journeyapps:zxing-android-embedded:4.3.0") { isTransitive = false }
    implementation("com.journeyapps:zxing-android-embedded:4.3.0") { isTransitive = false }
    
    // 注解处理器 - 库模块只需要Room和Glide，EventBus Index只在主应用模块中生成
    annotationProcessor("androidx.room:room-compiler:2.7.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    // EventBus注解处理器只在主应用模块中使用，库模块不需要
    // annotationProcessor("org.greenrobot:eventbus-annotation-processor:3.3.1")
    
    coreLibraryDesugaring(libs.desugar.jdk)
}
