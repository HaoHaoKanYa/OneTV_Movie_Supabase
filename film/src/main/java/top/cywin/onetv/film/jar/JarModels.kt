package top.cywin.onetv.film.jar

import kotlinx.serialization.Serializable

/**
 * JAR 相关数据模型
 * 
 * 基于 FongMi/TV 的 JAR 数据模型定义
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */

/**
 * JAR 信息
 */
@Serializable
data class JarInfo(
    val key: String,                    // JAR 唯一标识
    val name: String,                   // JAR 名称
    val version: String,                // 版本号
    val vendor: String,                 // 供应商
    val description: String,            // 描述
    val url: String,                    // 下载 URL
    val filePath: String,               // 本地文件路径
    val fileSize: Long,                 // 文件大小
    val spiderClasses: List<String>,    // Spider 类列表
    val loadTime: Long,                 // 加载时间
    val checksum: String                // 文件校验和
) {
    
    /**
     * 📋 获取摘要信息
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "key" to key,
            "name" to name,
            "version" to version,
            "vendor" to vendor,
            "spider_count" to spiderClasses.size,
            "file_size_mb" to String.format("%.2f", fileSize / 1024.0 / 1024.0),
            "load_time" to loadTime
        )
    }
    
    /**
     * ✅ 验证信息完整性
     */
    fun isValid(): Boolean {
        return key.isNotEmpty() && 
               name.isNotEmpty() && 
               version.isNotEmpty() && 
               url.isNotEmpty() && 
               filePath.isNotEmpty() && 
               fileSize > 0
    }
    
    /**
     * 🔄 是否需要更新
     */
    fun needsUpdate(other: JarInfo): Boolean {
        return this.checksum != other.checksum || 
               this.version != other.version
    }
}

/**
 * JAR 加载结果
 */
sealed class JarLoadResult {
    
    /**
     * ✅ 成功结果
     */
    data class Success(val jarInfo: JarInfo) : JarLoadResult()
    
    /**
     * ❌ 失败结果
     */
    data class Failure(val error: String, val exception: Exception? = null) : JarLoadResult()
    
    /**
     * ⏳ 进行中结果
     */
    data class Loading(val progress: Int, val message: String) : JarLoadResult()
    
    companion object {
        fun success(jarInfo: JarInfo) = Success(jarInfo)
        fun failure(error: String, exception: Exception? = null) = Failure(error, exception)
        fun loading(progress: Int, message: String) = Loading(progress, message)
    }
    
    /**
     * ✅ 是否成功
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * ❌ 是否失败
     */
    val isFailure: Boolean get() = this is Failure
    
    /**
     * ⏳ 是否加载中
     */
    val isLoading: Boolean get() = this is Loading
}

/**
 * JAR 配置
 */
@Serializable
data class JarConfig(
    val url: String,                    // JAR URL
    val name: String = "",              // 自定义名称
    val enabled: Boolean = true,        // 是否启用
    val autoUpdate: Boolean = false,    // 是否自动更新
    val updateInterval: Long = 86400000L, // 更新间隔（毫秒）
    val priority: Int = 100,            // 优先级
    val metadata: Map<String, String> = emptyMap() // 元数据
) {
    
    /**
     * ✅ 验证配置
     */
    fun isValid(): Boolean {
        return url.isNotEmpty() && 
               (url.startsWith("http://") || url.startsWith("https://")) &&
               priority >= 0
    }
    
    /**
     * 🔑 生成键
     */
    fun generateKey(): String {
        return url.hashCode().toString()
    }
}

/**
 * JAR 管理器配置
 */
@Serializable
data class JarManagerConfig(
    val maxCacheSize: Long = 500 * 1024 * 1024L, // 最大缓存大小 500MB
    val maxJarSize: Long = 50 * 1024 * 1024L,     // 单个 JAR 最大大小 50MB
    val downloadTimeout: Long = 30000L,           // 下载超时时间
    val enableAutoUpdate: Boolean = false,        // 是否启用自动更新
    val updateCheckInterval: Long = 3600000L,     // 更新检查间隔 1小时
    val enableSecurityCheck: Boolean = true,      // 是否启用安全检查
    val allowedDomains: Set<String> = emptySet(), // 允许的域名
    val enableMetrics: Boolean = true             // 是否启用指标收集
) {
    
    /**
     * ✅ 验证配置
     */
    fun isValid(): Boolean {
        return maxCacheSize > 0 && 
               maxJarSize > 0 && 
               downloadTimeout > 0 && 
               updateCheckInterval > 0
    }
}

/**
 * JAR 加载器统计
 */
data class JarLoaderStats(
    val loadedJars: Int,                // 已加载 JAR 数量
    val cachedClasses: Int,             // 缓存类数量
    val cacheSize: Long,                // 缓存大小
    val jarInfos: List<JarInfo>         // JAR 信息列表
) {
    
    /**
     * 📊 获取统计摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "loaded_jars" to loadedJars,
            "cached_classes" to cachedClasses,
            "cache_size_mb" to String.format("%.2f", cacheSize / 1024.0 / 1024.0),
            "total_spiders" to jarInfos.sumOf { it.spiderClasses.size }
        )
    }
}

/**
 * JAR 更新信息
 */
@Serializable
data class JarUpdateInfo(
    val jarKey: String,                 // JAR 键
    val currentVersion: String,         // 当前版本
    val latestVersion: String,          // 最新版本
    val updateAvailable: Boolean,       // 是否有更新
    val updateUrl: String,              // 更新 URL
    val updateSize: Long,               // 更新大小
    val releaseNotes: String,           // 发布说明
    val updateTime: Long                // 更新时间
) {
    
    /**
     * ✅ 是否需要更新
     */
    fun needsUpdate(): Boolean {
        return updateAvailable && currentVersion != latestVersion
    }
}

/**
 * JAR 依赖信息
 */
@Serializable
data class JarDependency(
    val name: String,                   // 依赖名称
    val version: String,                // 依赖版本
    val url: String,                    // 依赖 URL
    val required: Boolean = true,       // 是否必需
    val loaded: Boolean = false         // 是否已加载
) {
    
    /**
     * ✅ 验证依赖
     */
    fun isValid(): Boolean {
        return name.isNotEmpty() && 
               version.isNotEmpty() && 
               url.isNotEmpty()
    }
}

/**
 * JAR 安全信息
 */
@Serializable
data class JarSecurityInfo(
    val checksum: String,               // 文件校验和
    val signature: String,              // 数字签名
    val trusted: Boolean,               // 是否可信
    val permissions: List<String>,      // 权限列表
    val scanResult: SecurityScanResult  // 扫描结果
) {
    
    /**
     * ✅ 是否安全
     */
    fun isSafe(): Boolean {
        return trusted && scanResult == SecurityScanResult.SAFE
    }
}

/**
 * 安全扫描结果
 */
enum class SecurityScanResult {
    SAFE,           // 安全
    WARNING,        // 警告
    DANGEROUS,      // 危险
    UNKNOWN         // 未知
}

/**
 * JAR 性能指标
 */
data class JarPerformanceMetrics(
    val jarKey: String,                 // JAR 键
    val loadTime: Long,                 // 加载时间
    val classLoadTime: Long,            // 类加载时间
    val memoryUsage: Long,              // 内存使用
    val spiderCreateTime: Long,         // Spider 创建时间
    val executionCount: Long,           // 执行次数
    val averageExecutionTime: Long,     // 平均执行时间
    val errorCount: Long,               // 错误次数
    val lastExecutionTime: Long         // 最后执行时间
) {
    
    /**
     * 📊 获取性能摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "jar_key" to jarKey,
            "load_time_ms" to loadTime,
            "class_load_time_ms" to classLoadTime,
            "memory_usage_mb" to String.format("%.2f", memoryUsage / 1024.0 / 1024.0),
            "execution_count" to executionCount,
            "avg_execution_time_ms" to averageExecutionTime,
            "error_count" to errorCount,
            "error_rate" to if (executionCount > 0) errorCount.toDouble() / executionCount else 0.0
        )
    }
}

/**
 * JAR 事件
 */
sealed class JarEvent {
    
    /**
     * JAR 加载开始
     */
    data class LoadStarted(val jarKey: String, val url: String) : JarEvent()
    
    /**
     * JAR 加载成功
     */
    data class LoadSuccess(val jarInfo: JarInfo) : JarEvent()
    
    /**
     * JAR 加载失败
     */
    data class LoadFailure(val jarKey: String, val error: String, val exception: Exception?) : JarEvent()
    
    /**
     * JAR 卸载
     */
    data class Unloaded(val jarKey: String) : JarEvent()
    
    /**
     * JAR 更新可用
     */
    data class UpdateAvailable(val updateInfo: JarUpdateInfo) : JarEvent()
    
    /**
     * JAR 更新完成
     */
    data class UpdateCompleted(val jarKey: String, val oldVersion: String, val newVersion: String) : JarEvent()
    
    /**
     * Spider 创建
     */
    data class SpiderCreated(val jarKey: String, val className: String, val success: Boolean) : JarEvent()
    
    /**
     * 安全警告
     */
    data class SecurityWarning(val jarKey: String, val warning: String) : JarEvent()
}

/**
 * JAR 状态
 */
enum class JarStatus {
    UNKNOWN,        // 未知
    DOWNLOADING,    // 下载中
    VALIDATING,     // 验证中
    LOADING,        // 加载中
    LOADED,         // 已加载
    ERROR,          // 错误
    UNLOADED        // 已卸载
}
