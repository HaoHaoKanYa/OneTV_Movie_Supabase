package top.cywin.onetv.film.data.models

import kotlinx.serialization.Serializable

/**
 * 配置相关数据模型
 * 
 * 基于 FongMi/TV 的配置数据模型定义
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */

/**
 * 应用配置
 */
@Serializable
data class AppConfig(
    val version: String = "1.0.0",     // 应用版本
    val buildNumber: Int = 1,           // 构建号
    val apiVersion: String = "1.0",     // API 版本
    val minSupportedVersion: String = "1.0.0", // 最低支持版本
    val updateUrl: String = "",         // 更新地址
    val configUrl: String = "",         // 配置地址
    val backupUrl: String = "",         // 备份地址
    val feedbackUrl: String = "",       // 反馈地址
    val helpUrl: String = "",           // 帮助地址
    val privacyUrl: String = "",        // 隐私政策地址
    val termsUrl: String = "",          // 服务条款地址
    val enableAnalytics: Boolean = false, // 启用分析
    val enableCrashReport: Boolean = true, // 启用崩溃报告
    val enableAutoUpdate: Boolean = true, // 启用自动更新
    val enableBetaFeatures: Boolean = false, // 启用测试功能
    val maxCacheSize: Long = 500 * 1024 * 1024L, // 最大缓存大小
    val maxDownloadTasks: Int = 3,      // 最大下载任务数
    val networkTimeout: Long = 15000L,  // 网络超时
    val retryCount: Int = 3,            // 重试次数
    val logLevel: String = "INFO",      // 日志级别
    val theme: String = "dark",         // 主题
    val language: String = "zh-CN",     // 语言
    val region: String = "CN",          // 地区
    val createTime: Long = System.currentTimeMillis(), // 创建时间
    val updateTime: Long = System.currentTimeMillis()  // 更新时间
) {
    
    /**
     * 🔍 是否为调试模式
     */
    fun isDebugMode(): Boolean = logLevel == "DEBUG"
    
    /**
     * 🔍 是否启用测试功能
     */
    fun isBetaEnabled(): Boolean = enableBetaFeatures
    
    /**
     * 📊 获取配置摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "version" to version,
            "build_number" to buildNumber,
            "api_version" to apiVersion,
            "theme" to theme,
            "language" to language,
            "region" to region,
            "enable_analytics" to enableAnalytics,
            "enable_auto_update" to enableAutoUpdate,
            "enable_beta_features" to enableBetaFeatures,
            "max_cache_size_mb" to (maxCacheSize / 1024 / 1024),
            "max_download_tasks" to maxDownloadTasks,
            "network_timeout_ms" to networkTimeout,
            "log_level" to logLevel
        )
    }
}

/**
 * 用户配置
 */
@Serializable
data class UserConfig(
    val userId: String = "",            // 用户ID
    val username: String = "",          // 用户名
    val nickname: String = "",          // 昵称
    val avatar: String = "",            // 头像
    val email: String = "",             // 邮箱
    val phone: String = "",             // 手机号
    val birthday: String = "",          // 生日
    val gender: String = "",            // 性别
    val region: String = "",            // 地区
    val language: String = "zh-CN",     // 语言
    val theme: String = "dark",         // 主题
    val autoPlay: Boolean = true,       // 自动播放
    val autoNext: Boolean = true,       // 自动下一集
    val rememberPosition: Boolean = true, // 记住播放位置
    val enableNotification: Boolean = true, // 启用通知
    val enableVibration: Boolean = true, // 启用震动
    val enableSound: Boolean = true,    // 启用声音
    val parentalControl: Boolean = false, // 家长控制
    val adultContent: Boolean = false,  // 成人内容
    val downloadOnlyWifi: Boolean = true, // 仅WiFi下载
    val autoCleanCache: Boolean = true, // 自动清理缓存
    val backupSettings: Boolean = true, // 备份设置
    val syncData: Boolean = false,      // 同步数据
    val privacyMode: Boolean = false,   // 隐私模式
    val createTime: Long = System.currentTimeMillis(), // 创建时间
    val updateTime: Long = System.currentTimeMillis(), // 更新时间
    val lastLoginTime: Long = 0L,       // 最后登录时间
    val loginCount: Int = 0,            // 登录次数
    val deviceId: String = "",          // 设备ID
    val deviceName: String = "",        // 设备名称
    val appVersion: String = "",        // 应用版本
    val preferences: Map<String, String> = emptyMap() // 自定义偏好
) {
    
    /**
     * 🔍 是否为新用户
     */
    fun isNewUser(): Boolean = loginCount <= 1
    
    /**
     * 🔍 是否启用家长控制
     */
    fun isParentalControlEnabled(): Boolean = parentalControl
    
    /**
     * 📊 获取用户摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "user_id" to userId,
            "username" to username,
            "nickname" to nickname,
            "language" to language,
            "theme" to theme,
            "auto_play" to autoPlay,
            "auto_next" to autoNext,
            "remember_position" to rememberPosition,
            "parental_control" to parentalControl,
            "privacy_mode" to privacyMode,
            "login_count" to loginCount,
            "is_new_user" to isNewUser(),
            "device_name" to deviceName,
            "preferences_count" to preferences.size
        )
    }
}

/**
 * 站点配置
 */
@Serializable
data class SiteConfig(
    val configId: String = "",          // 配置ID
    val name: String,                   // 配置名称
    val description: String = "",       // 配置描述
    val version: String = "1.0.0",      // 配置版本
    val author: String = "",            // 作者
    val url: String = "",               // 配置地址
    val sites: List<VodSite> = emptyList(), // 站点列表
    val lives: List<LiveSite> = emptyList(), // 直播站点列表
    val parses: List<ParseSite> = emptyList(), // 解析站点列表
    val flags: List<String> = emptyList(), // 标识列表
    val ijk: Map<String, Any> = emptyMap(), // IJK 配置
    val ads: List<String> = emptyList(), // 广告列表
    val wallpaper: String = "",         // 壁纸地址
    val spider: String = "",            // 爬虫地址
    val warningText: String = "",       // 警告文本
    val createTime: Long = System.currentTimeMillis(), // 创建时间
    val updateTime: Long = System.currentTimeMillis(), // 更新时间
    val isDefault: Boolean = false,     // 是否默认配置
    val isEnabled: Boolean = true,      // 是否启用
    val priority: Int = 0,              // 优先级
    val tags: List<String> = emptyList(), // 标签
    val category: String = "默认",      // 分类
    val checksum: String = "",          // 校验和
    val size: Long = 0L                 // 配置大小
) {
    
    /**
     * 🔍 是否有站点
     */
    fun hasSites(): Boolean = sites.isNotEmpty()
    
    /**
     * 🔍 是否有直播站点
     */
    fun hasLives(): Boolean = lives.isNotEmpty()
    
    /**
     * 🔍 是否有解析站点
     */
    fun hasParses(): Boolean = parses.isNotEmpty()
    
    /**
     * 📊 获取配置摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "config_id" to configId,
            "name" to name,
            "version" to version,
            "author" to author,
            "sites_count" to sites.size,
            "lives_count" to lives.size,
            "parses_count" to parses.size,
            "is_default" to isDefault,
            "is_enabled" to isEnabled,
            "priority" to priority,
            "category" to category,
            "tags_count" to tags.size,
            "size_kb" to (size / 1024),
            "update_time" to updateTime
        )
    }
}

/**
 * 直播站点
 */
@Serializable
data class LiveSite(
    val key: String,                    // 站点标识
    val name: String,                   // 站点名称
    val type: Int = 0,                  // 站点类型
    val api: String,                    // API 地址
    val searchable: Int = 0,            // 是否可搜索
    val changeable: Int = 1,            // 是否可切换
    val ext: String = "",               // 扩展配置
    val timeout: Long = 15000L,         // 超时时间
    val headers: Map<String, String> = emptyMap(), // 请求头
    val enabled: Boolean = true,        // 是否启用
    val group: String = "默认",         // 分组
    val epg: String = "",               // EPG 地址
    val logo: String = "",              // Logo 地址
    val playerType: String = "",        // 播放器类型
    val userAgent: String = "",         // User-Agent
    val referer: String = ""            // Referer
) {
    
    /**
     * 🔍 是否可搜索
     */
    fun isSearchable(): Boolean = searchable == 1
    
    /**
     * 🔍 是否可切换
     */
    fun isChangeable(): Boolean = changeable == 1
}

/**
 * 解析站点
 */
@Serializable
data class ParseSite(
    val key: String,                    // 站点标识
    val name: String,                   // 站点名称
    val type: Int = 0,                  // 站点类型
    val url: String,                    // 解析地址
    val ext: Map<String, String> = emptyMap(), // 扩展配置
    val timeout: Long = 15000L,         // 超时时间
    val headers: Map<String, String> = emptyMap(), // 请求头
    val enabled: Boolean = true,        // 是否启用
    val priority: Int = 0,              // 优先级
    val group: String = "默认",         // 分组
    val userAgent: String = "",         // User-Agent
    val referer: String = ""            // Referer
)

/**
 * 设备信息
 */
@Serializable
data class DeviceInfo(
    val deviceId: String,               // 设备ID
    val deviceName: String,             // 设备名称
    val deviceModel: String = "",       // 设备型号
    val deviceBrand: String = "",       // 设备品牌
    val osName: String = "",            // 操作系统名称
    val osVersion: String = "",         // 操作系统版本
    val appVersion: String = "",        // 应用版本
    val screenWidth: Int = 0,           // 屏幕宽度
    val screenHeight: Int = 0,          // 屏幕高度
    val screenDensity: Float = 0f,      // 屏幕密度
    val cpuArch: String = "",           // CPU 架构
    val totalMemory: Long = 0L,         // 总内存
    val availableMemory: Long = 0L,     // 可用内存
    val totalStorage: Long = 0L,        // 总存储
    val availableStorage: Long = 0L,    // 可用存储
    val networkType: String = "",       // 网络类型
    val carrier: String = "",           // 运营商
    val language: String = "",          // 语言
    val timezone: String = "",          // 时区
    val createTime: Long = System.currentTimeMillis(), // 创建时间
    val updateTime: Long = System.currentTimeMillis()  // 更新时间
) {
    
    /**
     * 📊 获取设备摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "device_id" to deviceId,
            "device_name" to deviceName,
            "device_model" to deviceModel,
            "device_brand" to deviceBrand,
            "os_name" to osName,
            "os_version" to osVersion,
            "app_version" to appVersion,
            "screen_resolution" to "${screenWidth}x${screenHeight}",
            "cpu_arch" to cpuArch,
            "total_memory_gb" to String.format("%.1f", totalMemory / 1024.0 / 1024.0 / 1024.0),
            "available_memory_gb" to String.format("%.1f", availableMemory / 1024.0 / 1024.0 / 1024.0),
            "total_storage_gb" to String.format("%.1f", totalStorage / 1024.0 / 1024.0 / 1024.0),
            "available_storage_gb" to String.format("%.1f", availableStorage / 1024.0 / 1024.0 / 1024.0),
            "network_type" to networkType,
            "language" to language,
            "timezone" to timezone
        )
    }
}

/**
 * 备份信息
 */
@Serializable
data class BackupInfo(
    val backupId: String = "",          // 备份ID
    val name: String,                   // 备份名称
    val description: String = "",       // 备份描述
    val type: BackupType,               // 备份类型
    val filePath: String = "",          // 文件路径
    val fileSize: Long = 0L,            // 文件大小
    val checksum: String = "",          // 校验和
    val createTime: Long = System.currentTimeMillis(), // 创建时间
    val deviceId: String = "",          // 设备ID
    val appVersion: String = "",        // 应用版本
    val dataVersion: String = "",       // 数据版本
    val isEncrypted: Boolean = false,   // 是否加密
    val isCompressed: Boolean = true,   // 是否压缩
    val itemCount: Int = 0,             // 项目数量
    val categories: List<String> = emptyList(), // 备份分类
    val tags: List<String> = emptyList(), // 标签
    val userId: String = ""             // 用户ID
) {
    
    /**
     * 📊 获取备份摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "backup_id" to backupId,
            "name" to name,
            "type" to type.name,
            "file_size_mb" to String.format("%.2f", fileSize / 1024.0 / 1024.0),
            "item_count" to itemCount,
            "is_encrypted" to isEncrypted,
            "is_compressed" to isCompressed,
            "categories_count" to categories.size,
            "app_version" to appVersion,
            "create_time" to createTime
        )
    }
}

/**
 * 备份类型
 */
enum class BackupType {
    FULL,           // 完整备份
    SETTINGS,       // 设置备份
    FAVORITES,      // 收藏备份
    HISTORY,        // 历史备份
    SITES,          // 站点备份
    CUSTOM          // 自定义备份
}
