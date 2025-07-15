plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.script"
    compileSdk = 35
    
    defaultConfig {
        minSdk = 21
        targetSdk = 34
        consumerProguardFiles("consumer-rules.pro")
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    buildFeatures {
        buildConfig = true
    }
    
    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    api("androidx.annotation:annotation:1.6.0")
    implementation(project(":onevod:catvod"))

    // FongMi_TV原项目使用的QuickJS依赖 - 使用正确的Android版本
    implementation("wang.harlon.quickjs:wrapper-android:3.2.3")
    implementation("net.sourceforge.streamsupport:android-retrofuture:1.7.4")
}
