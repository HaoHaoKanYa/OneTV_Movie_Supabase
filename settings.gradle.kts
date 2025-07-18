pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.onetv.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                // 允许 AGP 插件及其所有依赖项下载
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.android\\.tools.*")
                // 添加对 signflinger 和 zipflinger 的支持
                includeGroup("com.android")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 确保Google仓库没有内容限制
        google()
        mavenCentral()
        jcenter() // FongMi_TV需要
        maven { url = uri("https://jitpack.io") }
        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/releases")
        }
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev") }
        // FongMi_TV特定仓库
        maven {
            url = uri("http://4thline.org/m2")
            isAllowInsecureProtocol = true
        }
        flatDir { dirs("onevod/libs") } // 本地AAR库
    }
}

rootProject.name = "壹来电视"
println("[OneTV-Build] 根项目名称: 壹来电视")

include(":core:data")
include(":core:util")
include(":core:designsystem")

// 恢复正确的构建顺序：TV主应用模块优先
println("[OneTV-Build] 包含TV模块 (主应用)")
include(":tv")
println("[OneTV-Build] :tv -> android.application")
include(":mobile")
include(":movie")
// include(":film") // 暂时移除film模块避免配置冲突

// vod影视点播应用模块 - 基于FongMi_TV完整移植
//println("[OneTV-Build] 包含VOD模块 (库模块)")
//include(":vod")                    // vod主应用模块 (对应原FongMi_TV的app模块)
//println("[OneTV-Build] :vod -> android.library")
//include(":vod:catvod")            // 爬虫核心引擎 (来自FongMi_TV)
//include(":vod:chaquo")            // Python引擎 (来自FongMi_TV)
//include(":vod:quickjs")           // JavaScript引擎 (来自FongMi_TV)
//include(":vod:hook")              // Hook机制 (来自FongMi_TV)
//include(":vod:forcetech")         // 解析器模块 (来自FongMi_TV)
//include(":vod:jianpian")          // 解析器模块 (来自FongMi_TV)
//include(":vod:thunder")           // 迅雷解析器 (来自FongMi_TV)
//include(":vod:tvbus")             // 直播解析器 (来自FongMi_TV)
