plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("com.chaquo.python")
}

android {
    namespace = "com.fongmi.chaquo"
    compileSdk = 35

    flavorDimensions += listOf("abi")

    defaultConfig {
        minSdk = 24  // Chaquopy 16.0.0要求minSdk 24+
        targetSdk = 34
        consumerProguardFiles("consumer-rules.pro")

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    productFlavors {
        create("arm64_v8a") {
            dimension = "abi"
            ndk { abiFilters += "arm64-v8a" }
        }
        create("armeabi_v7a") {
            dimension = "abi"
            ndk { abiFilters += "armeabi-v7a" }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Chaquopy配置 - 使用官方推荐的语法
chaquopy {
    defaultConfig {
        version = "3.8"
        pip {
            install("-r", "requirements.txt")
        }
    }
    sourceSets {
        getByName("main") {
            srcDir("src/main/python")
        }
    }
}

dependencies {
    api("androidx.annotation:annotation:1.6.0")
    implementation(project(":onevod:catvod"))
}
