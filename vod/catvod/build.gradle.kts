plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.github.catvod.crawler"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    
    // 核心依赖
    api("androidx.annotation:annotation:1.6.0")
    api("androidx.preference:preference:1.2.1")
    api("com.google.code.gson:gson:2.11.0")
    api("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    api("com.squareup.okhttp3:okhttp-dnsoverhttps:5.0.0-alpha.14")
    api("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.14")
    api("com.googlecode.juniversalchardet:juniversalchardet:1.0.3")
    api("com.orhanobut:logger:2.2.0")
    
    // Cronet支持
    api("com.google.net.cronet:cronet-okhttp:0.1.0")
    api("org.chromium.net:cronet-embedded:76.3809.111")
    
    // Guava工具库
    api("com.google.guava:guava:33.3.1-android") {
        exclude(group = "com.google.code.findbugs", module = "jsr305")
        exclude(group = "org.checkerframework", module = "checker-compat-qual")
        exclude(group = "org.checkerframework", module = "checker-qual")
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
        exclude(group = "com.google.j2objc", module = "j2objc-annotations")
        exclude(group = "org.codehaus.mojo", module = "animal-sniffer-annotations")
    }
}
