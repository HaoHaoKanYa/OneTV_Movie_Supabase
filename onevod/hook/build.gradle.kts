plugins {
    id("com.android.library")
}

android {
    namespace = "com.github.tvbox.osc.hook"
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
}

dependencies {
    api("androidx.annotation:annotation:1.6.0")
    api(project(":onevod:catvod"))
}
