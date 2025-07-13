pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
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

include(":core:data")
include(":core:util")
include(":core:designsystem")
include(":tv")
include(":mobile")
include(":movie")
// include(":film") // 暂时移除film模块避免配置冲突

// FongMi_TV集成到onevod模块 - 包含所有功能模块
include(":onevod")                    // 主模块
include(":onevod:catvod")            // 爬虫核心引擎
include(":onevod:chaquo")            // Python引擎
include(":onevod:quickjs")           // JavaScript引擎
include(":onevod:hook")              // Hook机制
include(":onevod:forcetech")         // 解析器模块
include(":onevod:jianpian")          // 解析器模块
include(":onevod:thunder")           // 迅雷解析器
include(":onevod:tvbus")             // 直播解析器
