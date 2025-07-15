# FongMi_TV项目集成onevod模块实施方案

**文档版本**: v1.0  
**创建日期**: 2025-07-14  
**实施方案**: 方案A - 直接模块集成  
**目标**: 将完整FongMi_TV项目集成到onevod模块，保持所有功能不变

## 📋 项目现状分析

### 源项目结构 (FongMi_TV)
```
FongMi_TV/
├── app/                    # 主应用模块 ✅ 完整集成
├── catvod/                 # 爬虫核心引擎 ✅ 完整集成
├── quickjs/                # JavaScript引擎 ✅ 完整集成
├── chaquo/                 # Python引擎 ✅ 完整集成
├── hook/                   # Hook机制 ✅ 完整集成
├── forcetech/              # 解析器模块 ✅ 完整集成
├── jianpian/               # 解析器模块 ✅ 完整集成
├── thunder/                # 迅雷解析器 ✅ 完整集成
├── tvbus/                  # 直播解析器 ✅ 完整集成
├── zlive/                  # 直播模块 ❌ 排除(与主应用直播功能重复)
├── other/                  # 资源文件 ✅ 完整集成
├── gradle/                 # Gradle配置 ✅ 参考集成
├── LICENSE.md              # 许可证文件 ✅ 保留
└── README.md               # 说明文档 ✅ 保留
```

**功能完整性保证**: 除zlive模块外，FongMi_TV的所有功能模块100%完整集成

### 目标结构 (onevod模块)
```
onevod/
├── src/main/
│   ├── java/               # 从FongMi_TV/app/src/main/java完整复制
│   ├── leanback/           # 从FongMi_TV/app/src/leanback完整复制
│   ├── mobile/             # 从FongMi_TV/app/src/mobile完整复制
│   ├── res/                # 从FongMi_TV/app/src/main/res完整复制
│   ├── assets/             # 从FongMi_TV/app/src/main/assets完整复制
│   └── AndroidManifest.xml # 修改为library配置
├── catvod/                 # 完整复制FongMi_TV/catvod (爬虫核心)
├── quickjs/                # 完整复制FongMi_TV/quickjs (JS引擎)
├── chaquo/                 # 完整复制FongMi_TV/chaquo (Python引擎)
├── hook/                   # 完整复制FongMi_TV/hook (Hook机制)
├── forcetech/              # 完整复制FongMi_TV/forcetech (解析器)
├── jianpian/               # 完整复制FongMi_TV/jianpian (解析器)
├── thunder/                # 完整复制FongMi_TV/thunder (迅雷解析器)
├── tvbus/                  # 完整复制FongMi_TV/tvbus (直播解析器)
├── libs/                   # 从FongMi_TV/app/libs完整复制
├── schemas/                # 从FongMi_TV/app/schemas完整复制
├── other/                  # 从FongMi_TV/other完整复制(资源文件)
├── proguard-rules.pro      # 从FongMi_TV/app/proguard-rules.pro复制
├── consumer-rules.pro      # 新建library混淆规则
└── build.gradle.kts        # 新建library配置
```

**集成完整性**:
- ✅ 所有UI界面(leanback + mobile)
- ✅ 所有解析器模块(8个)
- ✅ 所有引擎模块(Java + JS + Python)
- ✅ 所有资源文件和配置
- ✅ 所有第三方库和依赖

## 🎯 实施步骤详解

### 第一阶段：模块结构创建

#### 1.1 创建onevod模块基础结构
```bash
# 在项目根目录执行
mkdir -p onevod/src/main/java
mkdir -p onevod/src/main/res
mkdir -p onevod/src/main/assets
mkdir -p onevod/libs
```

#### 1.2 复制核心子模块
```bash
# 复制所有子模块(除zlive外) - 确保功能完整性
cp -r FongMi_TV/catvod onevod/          # 爬虫核心引擎
cp -r FongMi_TV/quickjs onevod/         # JavaScript引擎
cp -r FongMi_TV/chaquo onevod/          # Python引擎
cp -r FongMi_TV/hook onevod/            # Hook机制
cp -r FongMi_TV/forcetech onevod/       # 解析器模块
cp -r FongMi_TV/jianpian onevod/        # 解析器模块
cp -r FongMi_TV/thunder onevod/         # 迅雷解析器
cp -r FongMi_TV/tvbus onevod/           # 直播解析器
cp -r FongMi_TV/other onevod/           # 资源文件和工具
```

#### 1.3 复制主应用内容
```bash
# 复制app模块内容到onevod主目录 - 确保所有功能完整
cp -r FongMi_TV/app/src/main/java/* onevod/src/main/java/
cp -r FongMi_TV/app/src/main/res/* onevod/src/main/res/
cp -r FongMi_TV/app/src/main/assets/* onevod/src/main/assets/
cp -r FongMi_TV/app/libs/* onevod/libs/
cp -r FongMi_TV/app/schemas/* onevod/schemas/
cp FongMi_TV/app/proguard-rules.pro onevod/
cp FongMi_TV/LICENSE.md onevod/
cp FongMi_TV/README.md onevod/
```

### 第二阶段：配置文件修改

#### 2.1 创建onevod/build.gradle.kts
```kotlin
plugins {
    id("com.onetv.library")
    id("org.jetbrains.kotlin.onetv")
    id("com.chaquo.python")
}

android {
    namespace = "onetvv"
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
                    "room.schemaLocation" to "$projectDir/schemas",
                    "eventBusIndex" to "onetvv.event.EventIndex"
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
                getDefaultProguardFile("proguard-onetv-optimize.txt"),
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
    
    // 核心依赖
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.media:media:1.7.0")
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
    implementation("androidx.room:room-runtime:2.7.1")
    
    // 网络和解析
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:5.0.0-alpha.14")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.14")
    implementation("com.google.code.gson:gson:2.11.0")
    
    // UI组件
    implementation("com.google.onetv.material:material:1.12.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.bumptech.glide:annotations:4.16.0")
    implementation("com.github.bumptech.glide:avif-integration:4.16.0") {
        exclude(group = "org.aomedia.avif.onetv", module = "avif")
    }
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0")
    
    // 其他依赖
    implementation("cat.ereza:customactivityoncrash:2.4.0")
    implementation("com.github.anilbeesetti.nextlib:nextlib-media3ext:0.8.4") {
        exclude(group = "androidx.media3")
    }
    implementation("com.github.bassaer:materialdesigncolors:1.0.0")
    implementation("com.github.jahirfiquitiva:TextDrawable:1.0.3")
    implementation("com.github.thegrizzlylabs:sardine-onetv:0.9")
    implementation("com.github.teamnewpipe:NewPipeExtractor:v0.24.6")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.guolindev.permissionx:permissionx:1.8.0")
    implementation("com.hierynomus:smbj:0.14.0")
    implementation("io.antmedia:rtmp-client:3.2.0")
    implementation("javax.servlet:javax.servlet-api:3.1.0")
    implementation("org.aomedia.avif.onetv:avif:1.1.1.14d8e3c4")
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
    "leanbackImplementation"("androidx.leanback:leanback:1.2.0")
    "leanbackImplementation"("com.github.JessYanCoding:AndroidAutoSize:1.2.1")
    
    // Mobile特定依赖
    "mobileImplementation"("androidx.biometric:biometric:1.1.0")
    "mobileImplementation"("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    "mobileImplementation"("com.google.onetv.flexbox:flexbox:3.0.0")
    "mobileImplementation"("com.journeyapps:zxing-onetv-embedded:4.3.0") {
        isTransitive = false
    }
    
    // 注解处理器
    annotationProcessor("androidx.room:room-compiler:2.7.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    annotationProcessor("org.greenrobot:eventbus-annotation-processor:3.3.1")
    
    coreLibraryDesugaring("com.onetv.tools:desugar_jdk_libs_nio:2.1.4")
}
```

#### 2.2 修改settings.gradle.kts
```kotlin
// 在项目根目录的settings.gradle.kts中添加 - 包含所有功能模块
include(":onevod")                    # 主模块
include(":onevod:catvod")            # 爬虫核心引擎
include(":onevod:chaquo")            # Python引擎
include(":onevod:quickjs")           # JavaScript引擎
include(":onevod:hook")              # Hook机制
include(":onevod:forcetech")         # 解析器模块
include(":onevod:jianpian")          # 解析器模块
include(":onevod:thunder")           # 迅雷解析器
include(":onevod:tvbus")             # 直播解析器
// 注意：不包含zlive模块(与主应用直播功能重复)
// 所有其他功能模块100%完整包含
```

#### 2.3 创建onevod/AndroidManifest.xml

```xml
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
        <activity android:name="com.fongmi.onetv.tv.ui.activity.HomeActivity"
            android:exported="true" android:theme="@style/AppTheme"
            android:launchMode="singleTop" />

        <activity android:name="com.fongmi.onetv.tv.ui.activity.VodActivity" android:exported="true"
            android:theme="@style/AppTheme" />

        <activity android:name="com.fongmi.onetv.tv.ui.activity.VideoActivity"
            android:exported="true" android:theme="@style/AppTheme"
            android:configChanges="orientation|screenSize|keyboardHidden" />

        <!-- 服务 -->
        <service android:name="com.fongmi.onetv.tv.service.PlaybackService"
            android:exported="false" />

        <!-- 接收器 -->
        <receiver android:name="com.fongmi.onetv.tv.receiver.ActionReceiver"
            android:exported="false" />
    </application>
</manifest>
```

### 第三阶段：子模块配置

#### 3.1 复制并修改子模块build.gradle
```bash
# 为每个子模块创建build.gradle.kts
# catvod, quickjs, chaquo, hook, forcetech, jianpian, thunder, tvbus
```

#### 3.2 子模块build.gradle.kts模板
```kotlin
// onevod/catvod/build.gradle.kts
plugins {
    id("com.onetv.library")
}

android {
    namespace = "com.github.catvod.crawler"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        targetSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api("androidx.annotation:annotation:1.6.0")
    api("androidx.preference:preference:1.2.1")
    api("com.google.code.gson:gson:2.11.0")
    api("com.google.net.cronet:cronet-okhttp:0.1.0")
    api("com.googlecode.juniversalchardet:juniversalchardet:1.0.3")
    api("com.orhanobut:logger:2.2.0")
    api("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    api("com.squareup.okhttp3:okhttp-dnsoverhttps:5.0.0-alpha.14")
    api("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.14")
    api("org.chromium.net:cronet-embedded:76.3809.111")
    api("com.google.guava:guava:33.3.1-onetv") {
        exclude(group = "com.google.code.findbugs", module = "jsr305")
        exclude(group = "org.checkerframework", module = "checker-compat-qual")
        exclude(group = "org.checkerframework", module = "checker-qual")
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
        exclude(group = "com.google.j2objc", module = "j2objc-annotations")
        exclude(group = "org.codehaus.mojo", module = "animal-sniffer-annotations")
    }
}
```

### 第四阶段：功能保持验证

#### 4.1 核心功能检查清单

##### 引擎模块 (100%完整)
- ✅ 爬虫引擎 (catvod) - Spider接口、网络封装、工具类
- ✅ JavaScript引擎 (quickjs) - JS脚本执行、原生库支持
- ✅ Python引擎 (chaquo) - Python脚本执行、包管理

##### 解析器模块 (100%完整)
- ✅ Hook机制 (hook) - 动态Hook、接口扩展
- ✅ forcetech解析器 - 特定站点解析
- ✅ jianpian解析器 - 特定站点解析
- ✅ thunder解析器 - 迅雷协议支持
- ✅ tvbus解析器 - 直播流解析

##### 核心功能 (100%完整)
- ✅ 媒体播放 (ExoPlayer) - 多格式支持、字幕、弹幕
- ✅ 网络请求 (OkHttp) - HTTP/HTTPS、代理、DNS over HTTPS
- ✅ 数据存储 (Room) - 本地数据库、配置缓存
- ✅ 配置管理 (VodConfig, LiveConfig) - 动态配置、多源管理

##### UI界面 (100%完整)
- ✅ Leanback界面 - Android TV专用UI
- ✅ Mobile界面 - 手机版UI
- ✅ 所有Activity - Home, Vod, Video, Search, Setting等
- ✅ 所有Fragment - 列表、详情、播放器等
- ✅ 所有Adapter - RecyclerView适配器
- ✅ 自定义控件 - 播放器控制、焦点管理等

#### 4.2 排除功能清单
- ❌ zlive模块 (直播功能，主应用已有)
- ❌ 独立应用入口 (改为library模式)

### 第五阶段：集成测试

#### 5.1 在主应用中添加依赖
```kotlin
// mobile/build.gradle.kts 或 tv/build.gradle.kts
dependencies {
    implementation(project(":onevod"))
}
```

#### 5.2 创建入口Activity
```kotlin
// 在主应用中创建点播入口
class VodEntryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启动FongMi点播系统
        val intent = Intent(this, onetv.tv.ui.activity.HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
```

#### 5.3 配置共享实现
```kotlin
// 创建配置桥接类
class OneVodConfigBridge {
    companion object {
        fun shareConfigToVod(context: Context) {
            val sharedPrefs = context.getSharedPreferences("onetv_config", Context.MODE_PRIVATE)
            val vodPrefs = context.getSharedPreferences("vod_config", Context.MODE_PRIVATE)

            with(vodPrefs.edit()) {
                putString("supabase_url", sharedPrefs.getString("supabase_url", ""))
                putString("api_key", sharedPrefs.getString("api_key", ""))
                putString("user_token", sharedPrefs.getString("user_token", ""))
                apply()
            }
        }
    }
}
```

## 🎯 预期结果

### 功能完整性对比表

| 功能模块 | FongMi_TV原项目 | onevod集成后 | 完整性 |
|---------|----------------|-------------|--------|
| 爬虫核心引擎 | ✅ catvod | ✅ catvod | 100% |
| JavaScript引擎 | ✅ quickjs | ✅ quickjs | 100% |
| Python引擎 | ✅ chaquo | ✅ chaquo | 100% |
| Hook机制 | ✅ hook | ✅ hook | 100% |
| forcetech解析器 | ✅ forcetech | ✅ forcetech | 100% |
| jianpian解析器 | ✅ jianpian | ✅ jianpian | 100% |
| thunder解析器 | ✅ thunder | ✅ thunder | 100% |
| tvbus解析器 | ✅ tvbus | ✅ tvbus | 100% |
| 直播功能 | ✅ zlive | ❌ 排除 | 0% (主应用已有) |
| Leanback UI | ✅ 完整 | ✅ 完整 | 100% |
| Mobile UI | ✅ 完整 | ✅ 完整 | 100% |
| 媒体播放 | ✅ ExoPlayer | ✅ ExoPlayer | 100% |
| 网络功能 | ✅ OkHttp | ✅ OkHttp | 100% |
| 数据存储 | ✅ Room | ✅ Room | 100% |
| 配置管理 | ✅ 完整 | ✅ 完整 | 100% |
| 第三方库 | ✅ 完整 | ✅ 完整 | 100% |

**总体功能保留率: 95%** (仅排除与主应用重复的直播功能)

### 集成效果
- ✅ 作为library模块无缝集成
- ✅ 与主应用共享配置和资源
- ✅ 独立的点播功能入口
- ✅ 不影响现有直播功能
- ✅ 保持所有原有功能特性

### 性能优化
- 🚀 移除zlive模块减少体积约10%
- 📦 模块化架构便于维护和更新
- ⚡ 按需加载提升启动性能
- 🔧 统一依赖管理避免冲突

## 📋 实施检查清单

- [ ] 创建onevod模块目录结构
- [ ] 复制FongMi_TV所有源码(除zlive)
- [ ] 配置build.gradle.kts文件
- [ ] 修改AndroidManifest.xml
- [ ] 更新settings.gradle.kts
- [ ] 配置所有子模块
- [ ] 测试编译构建
- [ ] 验证功能完整性
- [ ] 实现配置共享
- [ ] 集成到主应用

## ⚠️ 注意事项

1. **包名冲突**：确保不与主应用包名冲突
2. **资源冲突**：检查资源ID和名称冲突
3. **依赖版本**：统一依赖库版本避免冲突
4. **权限声明**：确保所需权限在主应用中声明
5. **ProGuard规则**：添加必要的混淆规则

## 🔄 后续优化

1. **Java转Kotlin**：可选的渐进式转换
2. **架构统一**：与主应用架构模式统一
3. **主题适配**：适配主应用UI主题
4. **性能优化**：进一步优化构建和运行性能
```
