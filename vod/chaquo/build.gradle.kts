plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("com.chaquo.python")
}

android {
    namespace = "com.fongmi.chaquo"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    /*
    flavorDimensions += listOf("abi")
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
    */

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
    implementation(project(":vod:catvod"))
}
