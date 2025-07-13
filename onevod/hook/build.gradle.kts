plugins {
    id("com.android.library")
}

android {
    namespace = "com.github.tvbox.osc.hook"
    compileSdk = 35
    
    defaultConfig {
        minSdk = 21
        targetSdk = 28
        consumerProguardFiles("consumer-rules.pro")
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api("androidx.annotation:annotation:1.6.0")
    api(project(":onevod:catvod"))
}
