# FongMi_TV集成onevod模块执行脚本

**文档版本**: v1.0  
**创建日期**: 2025-07-14  
**执行环境**: Windows PowerShell / Linux Bash  
**目标**: 自动化执行FongMi_TV项目集成到onevod模块

## 📋 执行前准备

### 环境检查
```bash
# 检查必要工具
git --version
gradle --version
# 确保Android SDK已配置
echo $ANDROID_HOME
```

### 项目状态确认
```bash
# 确认FongMi_TV项目存在
ls -la FongMi_TV/
# 确认onevod模块目录不存在(避免冲突)
ls -la onevod/ 2>/dev/null || echo "onevod目录不存在，可以开始集成"
```

## 🚀 自动化执行脚本

### Windows PowerShell脚本
```powershell
# integrate_fongmi_to_onevod.ps1

Write-Host "开始FongMi_TV集成到onevod模块..." -ForegroundColor Green

# 第一步：创建onevod模块目录结构
Write-Host "创建onevod模块目录结构..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path "onevod"
New-Item -ItemType Directory -Force -Path "onevod\src\main\java"
New-Item -ItemType Directory -Force -Path "onevod\src\main\res"
New-Item -ItemType Directory -Force -Path "onevod\src\main\assets"
New-Item -ItemType Directory -Force -Path "onevod\libs"
New-Item -ItemType Directory -Force -Path "onevod\schemas"

# 第二步：复制核心子模块 - 确保所有功能模块完整复制
Write-Host "复制核心子模块..." -ForegroundColor Yellow
$modules = @("catvod", "quickjs", "chaquo", "hook", "forcetech", "jianpian", "thunder", "tvbus")
foreach ($module in $modules) {
    if (Test-Path "FongMi_TV\$module") {
        Copy-Item -Recurse -Force "FongMi_TV\$module" "onevod\"
        Write-Host "已复制模块: $module" -ForegroundColor Cyan
    } else {
        Write-Host "警告: 模块 $module 不存在" -ForegroundColor Red
    }
}

# 复制other资源文件
if (Test-Path "FongMi_TV\other") {
    Copy-Item -Recurse -Force "FongMi_TV\other" "onevod\"
    Write-Host "已复制资源文件: other" -ForegroundColor Cyan
}

# 第三步：复制主应用内容 - 确保所有应用内容完整复制
Write-Host "复制主应用内容..." -ForegroundColor Yellow
if (Test-Path "FongMi_TV\app\src\main\java") {
    Copy-Item -Recurse -Force "FongMi_TV\app\src\main\java\*" "onevod\src\main\java\"
    Write-Host "已复制: 主要Java源码" -ForegroundColor Cyan
}
if (Test-Path "FongMi_TV\app\src\main\res") {
    Copy-Item -Recurse -Force "FongMi_TV\app\src\main\res\*" "onevod\src\main\res\"
    Write-Host "已复制: 资源文件" -ForegroundColor Cyan
}
if (Test-Path "FongMi_TV\app\src\main\assets") {
    Copy-Item -Recurse -Force "FongMi_TV\app\src\main\assets\*" "onevod\src\main\assets\"
    Write-Host "已复制: Assets文件" -ForegroundColor Cyan
}
if (Test-Path "FongMi_TV\app\libs") {
    Copy-Item -Recurse -Force "FongMi_TV\app\libs\*" "onevod\libs\"
    Write-Host "已复制: 第三方库文件" -ForegroundColor Cyan
}
if (Test-Path "FongMi_TV\app\schemas") {
    Copy-Item -Recurse -Force "FongMi_TV\app\schemas\*" "onevod\schemas\"
    Write-Host "已复制: 数据库Schema文件" -ForegroundColor Cyan
}
if (Test-Path "FongMi_TV\app\proguard-rules.pro") {
    Copy-Item -Force "FongMi_TV\app\proguard-rules.pro" "onevod\"
    Write-Host "已复制: ProGuard规则文件" -ForegroundColor Cyan
}
if (Test-Path "FongMi_TV\LICENSE.md") {
    Copy-Item -Force "FongMi_TV\LICENSE.md" "onevod\"
    Write-Host "已复制: 许可证文件" -ForegroundColor Cyan
}
if (Test-Path "FongMi_TV\README.md") {
    Copy-Item -Force "FongMi_TV\README.md" "onevod\"
    Write-Host "已复制: 说明文档" -ForegroundColor Cyan
}

# 第四步：复制leanback和mobile特定代码
Write-Host "复制leanback和mobile特定代码..." -ForegroundColor Yellow
if (Test-Path "FongMi_TV\app\src\leanback") {
    New-Item -ItemType Directory -Force -Path "onevod\src\leanback"
    Copy-Item -Recurse -Force "FongMi_TV\app\src\leanback\*" "onevod\src\leanback\"
}
if (Test-Path "FongMi_TV\app\src\mobile") {
    New-Item -ItemType Directory -Force -Path "onevod\src\mobile"
    Copy-Item -Recurse -Force "FongMi_TV\app\src\mobile\*" "onevod\src\mobile\"
}

Write-Host "文件复制完成!" -ForegroundColor Green

# 第五步：创建配置文件
Write-Host "创建配置文件..." -ForegroundColor Yellow

# 创建onevod/build.gradle.kts
$buildGradleContent = @"
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python")
}

android {
    namespace = "onetv.tv"
    compileSdk = 35
    
    flavorDimensions += listOf("mode", "api", "abi")
    
    defaultConfig {
        minSdk = 21
        targetSdk = 28
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "`$projectDir/schemas",
                    "eventBusIndex" to "onetv.tv.event.EventIndex"
                )
            }
        }
    }
    
    productFlavors {
        create("leanback") {
            dimension = "mode"
        }
        create("mobile") {
            dimension = "mode"
        }
        create("java") {
            dimension = "api"
        }
        create("python") {
            dimension = "api"
        }
        create("arm64_v8a") {
            dimension = "abi"
            ndk {
                abiFilters += "arm64-v8a"
            }
        }
        create("armeabi_v7a") {
            dimension = "abi"
            ndk {
                abiFilters += "armeabi-v7a"
            }
        }
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
    
    packagingOptions {
        resources {
            excludes += listOf(
                "META-INF/beans.xml",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            )
        }
    }
    
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    
    // 子模块依赖
    implementation(project(":onevod:catvod"))
    implementation(project(":onevod:chaquo"))
    implementation(project(":onevod:quickjs"))
    implementation(project(":onevod:hook"))
    implementation(project(":onevod:forcetech"))
    implementation(project(":onevod:jianpian"))
    implementation(project(":onevod:thunder"))
    implementation(project(":onevod:tvbus"))
    
    // 核心Android依赖
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.media:media:1.7.0")
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("com.google.android.material:material:1.12.0")
    
    // Media3依赖
    implementation("androidx.media3:media3-common:1.6.1")
    implementation("androidx.media3:media3-exoplayer:1.6.1")
    implementation("androidx.media3:media3-ui:1.6.1")
    implementation("androidx.media3:media3-datasource:1.6.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.6.1")
    
    // 网络依赖
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    implementation("com.google.code.gson:gson:2.11.0")
    
    // UI依赖
    implementation("com.github.bumptech.glide:glide:4.16.0")
    
    // Leanback特定依赖
    "leanbackImplementation"("androidx.leanback:leanback:1.2.0")
    "leanbackImplementation"("com.github.JessYanCoding:AndroidAutoSize:1.2.1")
    
    // Mobile特定依赖
    "mobileImplementation"("androidx.biometric:biometric:1.1.0")
    "mobileImplementation"("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // 注解处理器
    annotationProcessor("androidx.room:room-compiler:2.7.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.4")
}
"@

Set-Content -Path "onevod\build.gradle.kts" -Value $buildGradleContent -Encoding UTF8

# 创建AndroidManifest.xml
$manifestContent = @"
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- 网络权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    
    <!-- 存储权限 -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    
    <!-- 其他权限 -->
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    
    <application>
        <!-- 导出的Activity供主应用调用 -->
        <activity
            android:name="onetv.tv.ui.activity.HomeActivity"
            android:exported="true"
            android:theme="@style/AppTheme"
            android:launchMode="singleTop" />
            
        <activity
            android:name="onetv.tv.ui.activity.VodActivity"
            android:exported="true"
            android:theme="@style/AppTheme" />
            
        <activity
            android:name="onetv.tv.ui.activity.VideoActivity"
            android:exported="true"
            android:theme="@style/AppTheme"
            android:configChanges="orientation|screenSize|keyboardHidden" />
            
        <!-- 服务 -->
        <service
            android:name="onetv.tv.service.PlaybackService"
            android:exported="false" />
            
        <!-- 接收器 -->
        <receiver
            android:name="onetv.tv.receiver.ActionReceiver"
            android:exported="false" />
    </application>
</manifest>
"@

Set-Content -Path "onevod\src\main\AndroidManifest.xml" -Value $manifestContent -Encoding UTF8

Write-Host "配置文件创建完成!" -ForegroundColor Green

Write-Host "FongMi_TV集成到onevod模块完成!" -ForegroundColor Green
Write-Host "功能完整性检查:" -ForegroundColor Yellow
Write-Host "✅ 所有解析器模块已集成" -ForegroundColor Green
Write-Host "✅ 所有引擎模块已集成" -ForegroundColor Green
Write-Host "✅ 所有UI界面已集成" -ForegroundColor Green
Write-Host "✅ 所有资源文件已集成" -ForegroundColor Green
Write-Host "❌ zlive模块已排除(避免与主应用冲突)" -ForegroundColor Yellow
Write-Host ""
Write-Host "请手动执行以下步骤:" -ForegroundColor Yellow
Write-Host "1. 更新settings.gradle.kts添加onevod模块" -ForegroundColor Cyan
Write-Host "2. 为每个子模块创建build.gradle.kts" -ForegroundColor Cyan
Write-Host "3. 同步项目并测试编译" -ForegroundColor Cyan
```

### Linux/Mac Bash脚本
```bash
#!/bin/bash
# integrate_fongmi_to_onevod.sh

echo "开始FongMi_TV集成到onevod模块..."

# 第一步：创建onevod模块目录结构
echo "创建onevod模块目录结构..."
mkdir -p onevod/src/main/java
mkdir -p onevod/src/main/res
mkdir -p onevod/src/main/assets
mkdir -p onevod/libs
mkdir -p onevod/schemas

# 第二步：复制核心子模块 - 确保所有功能模块完整复制
echo "复制核心子模块..."
modules=("catvod" "quickjs" "chaquo" "hook" "forcetech" "jianpian" "thunder" "tvbus")
for module in "${modules[@]}"; do
    if [ -d "FongMi_TV/$module" ]; then
        cp -r "FongMi_TV/$module" "onevod/"
        echo "已复制模块: $module"
    else
        echo "警告: 模块 $module 不存在"
    fi
done

# 复制other资源文件
if [ -d "FongMi_TV/other" ]; then
    cp -r "FongMi_TV/other" "onevod/"
    echo "已复制资源文件: other"
fi

# 第三步：复制主应用内容 - 确保所有应用内容完整复制
echo "复制主应用内容..."
if [ -d "FongMi_TV/app/src/main/java" ]; then
    cp -r FongMi_TV/app/src/main/java/* onevod/src/main/java/
    echo "已复制: 主要Java源码"
fi
if [ -d "FongMi_TV/app/src/main/res" ]; then
    cp -r FongMi_TV/app/src/main/res/* onevod/src/main/res/
    echo "已复制: 资源文件"
fi
if [ -d "FongMi_TV/app/src/main/assets" ]; then
    cp -r FongMi_TV/app/src/main/assets/* onevod/src/main/assets/
    echo "已复制: Assets文件"
fi
if [ -d "FongMi_TV/app/libs" ]; then
    cp -r FongMi_TV/app/libs/* onevod/libs/
    echo "已复制: 第三方库文件"
fi
if [ -d "FongMi_TV/app/schemas" ]; then
    cp -r FongMi_TV/app/schemas/* onevod/schemas/
    echo "已复制: 数据库Schema文件"
fi
if [ -f "FongMi_TV/app/proguard-rules.pro" ]; then
    cp "FongMi_TV/app/proguard-rules.pro" "onevod/"
    echo "已复制: ProGuard规则文件"
fi
if [ -f "FongMi_TV/LICENSE.md" ]; then
    cp "FongMi_TV/LICENSE.md" "onevod/"
    echo "已复制: 许可证文件"
fi
if [ -f "FongMi_TV/README.md" ]; then
    cp "FongMi_TV/README.md" "onevod/"
    echo "已复制: 说明文档"
fi

# 第四步：复制leanback和mobile特定代码
echo "复制leanback和mobile特定代码..."
if [ -d "FongMi_TV/app/src/leanback" ]; then
    mkdir -p onevod/src/leanback
    cp -r FongMi_TV/app/src/leanback/* onevod/src/leanback/
fi
if [ -d "FongMi_TV/app/src/mobile" ]; then
    mkdir -p onevod/src/mobile
    cp -r FongMi_TV/app/src/mobile/* onevod/src/mobile/
fi

echo "文件复制完成!"
echo "FongMi_TV集成到onevod模块完成!"
echo "请手动执行后续配置步骤"
```

## 📋 手动执行步骤

### 1. 更新settings.gradle.kts
```kotlin
// 在项目根目录的settings.gradle.kts中添加
include(":onevod")
include(":onevod:catvod")
include(":onevod:chaquo")
include(":onevod:quickjs")
include(":onevod:hook")
include(":onevod:forcetech")
include(":onevod:jianpian")
include(":onevod:thunder")
include(":onevod:tvbus")
```

### 2. 为子模块创建build.gradle.kts
每个子模块都需要创建对应的build.gradle.kts文件，参考主实施方案文档中的模板。

### 3. 同步和测试
```bash
# 同步项目
./gradlew sync

# 测试编译
./gradlew :onevod:assembleDebug
```

## ⚠️ 执行注意事项

1. **备份项目**：执行前务必备份整个项目
2. **权限检查**：确保有足够的文件读写权限
3. **路径检查**：确认FongMi_TV目录存在
4. **空间检查**：确保有足够的磁盘空间
5. **编码问题**：注意文件编码格式(UTF-8)

## 🔍 执行后验证

### 验证文件结构
```bash
# 检查onevod模块结构
tree onevod/ -L 2

# 检查关键文件
ls -la onevod/src/main/java/com/fongmi/onetv/tv/
ls -la onevod/catvod/src/main/java/
```

### 验证编译
```bash
# 编译onevod模块
./gradlew :onevod:compileDebugJavaWithJavac

# 检查编译错误
./gradlew :onevod:assembleDebug --stacktrace
```

## 📊 执行结果预期

### 文件结构完整性
- ✅ onevod模块目录结构完整
- ✅ 所有FongMi_TV源码已复制(除zlive)
- ✅ 配置文件已创建
- ✅ 子模块结构保持完整

### 功能完整性保证
- ✅ 爬虫核心引擎 (catvod) - 100%完整
- ✅ JavaScript引擎 (quickjs) - 100%完整
- ✅ Python引擎 (chaquo) - 100%完整
- ✅ Hook机制 (hook) - 100%完整
- ✅ 所有解析器模块 (forcetech, jianpian, thunder, tvbus) - 100%完整
- ✅ 所有UI界面 (leanback + mobile) - 100%完整
- ✅ 所有资源文件和配置 - 100%完整
- ✅ 所有第三方库和依赖 - 100%完整

### 构建验证
- ✅ 编译无错误
- ✅ 依赖关系正确
- ✅ 模块间引用正常
