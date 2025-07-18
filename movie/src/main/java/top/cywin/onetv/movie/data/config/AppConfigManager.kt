package top.cywin.onetv.movie.data.config

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import top.cywin.onetv.movie.Constants
// KotlinPoet专业重构 - 移除Hilt相关import
// import javax.inject.Inject
// import javax.inject.Singleton
import android.util.Log
import top.cywin.onetv.core.data.repositories.supabase.SupabaseClient

/**
 * 应用配置管理器 - 从app_configs表读取配置
 * 基于OneMoVie架构，支持动态配置加载
 * KotlinPoet专业重构 - 移除Hilt依赖，使用标准构造函数
 */
// @Singleton
class AppConfigManager(
    private val context: Context
) {
    
    private val preferences: SharedPreferences = 
        context.getSharedPreferences("movie_app_config", Context.MODE_PRIVATE)
    
    private var currentConfig: AppConfig? = null
    
    /**
     * 应用配置数据模型
     */
    @Serializable
    data class AppConfig(
        val id: Int,
        val appId: String,
        val projectName: String,
        val projectUrl: String,
        val projectId: String,
        val apiKey: String,
        val accessToken: String,
        val isActive: Boolean,
        val createdAt: String,
        val updatedAt: String,
        val jwtSecret: String,
        val serviceRole: String? = null // Service Role Key字段
    )
    
    /**
     * 初始化配置
     */
    suspend fun initializeConfig(): Result<AppConfig> {
        return try {
            Log.d("ONETV_MOVIE", "开始初始化配置")

            // 检查是否需要强制重新加载
            val forceReload = preferences.getBoolean("force_reload_config", false)
            if (forceReload) {
                Log.d("ONETV_MOVIE", "🔄 检测到强制重新加载标记，清除缓存")
                preferences.edit().remove("cached_config").remove("force_reload_config").apply()
            }

            // 检查serviceRole字段问题，如果缓存中的配置serviceRole为空，强制重新加载
            val cachedConfig = getCachedConfig()
            val needForceReload = cachedConfig?.serviceRole.isNullOrEmpty()

            if (needForceReload) {
                Log.w("ONETV_MOVIE", "⚠️ 检测到缓存配置serviceRole字段为空，强制重新加载")
                Log.w("ONETV_MOVIE", "🗑️ 清除所有缓存数据，强制从数据库重新获取")
                preferences.edit().clear().apply() // 清除所有缓存
            }

            // 临时强制重新加载策略：总是清除缓存，直到serviceRole问题解决
            Log.w("ONETV_MOVIE", "🔧 临时策略：强制清除缓存，确保从数据库获取最新配置")
            preferences.edit().clear().apply()

            // 1. 尝试从缓存获取（除非强制重新加载或serviceRole为空）
            if (!forceReload && !needForceReload) {
                if (cachedConfig != null && isConfigValid(cachedConfig)) {
                    Log.d("ONETV_MOVIE", "使用缓存配置")
                    currentConfig = cachedConfig
                    return Result.success(cachedConfig)
                }
            }

            // 2. 从服务器获取最新配置
            Log.d("ONETV_MOVIE", "从服务器获取配置")
            val serverConfig = fetchConfigFromServer()
            if (serverConfig != null) {
                Log.d("ONETV_MOVIE", "服务器配置获取成功")
                currentConfig = serverConfig
                cacheConfig(serverConfig)
                return Result.success(serverConfig)
            }

            // 3. 使用临时默认配置，避免应用崩溃
            Log.w("ONETV_MOVIE", "无法获取服务器配置，使用临时默认配置")
            val defaultConfig = getTemporaryDefaultConfig()
            currentConfig = defaultConfig
            Result.success(defaultConfig)

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "配置初始化失败", e)
            // 即使失败也提供默认配置，避免应用崩溃
            val defaultConfig = getTemporaryDefaultConfig()
            currentConfig = defaultConfig
            Result.success(defaultConfig)
        }
    }
    
    /**
     * 从服务器获取配置
     * 使用现有的 Supabase 配置系统
     */
    private suspend fun fetchConfigFromServer(): AppConfig? {
        return withContext(Dispatchers.IO) {
            try {
                // 使用现有的 SupabaseClient 获取配置
                val supabaseClient = top.cywin.onetv.core.data.repositories.supabase.SupabaseClient
                val appConfig = supabaseClient.getAppConfig(Constants.CONFIG_APP_ID)

                // 转换为本地 AppConfig 格式
                appConfig?.let { config ->
                    AppConfig(
                        id = config.id,
                        appId = config.appId,
                        projectName = config.projectName,
                        projectUrl = config.projectUrl,
                        projectId = config.projectId,
                        apiKey = config.apiKey,
                        accessToken = config.accessToken ?: "",
                        isActive = config.isActive,
                        createdAt = config.createdAt ?: "",
                        updatedAt = config.updatedAt ?: "",
                        jwtSecret = config.jwtSecret ?: "",
                        serviceRole = config.serviceRole // Service Role Key
                    )
                }

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "从服务器获取配置失败", e)
                null
            }
        }
    }
    
    /**
     * 获取缓存的配置
     */
    private fun getCachedConfig(): AppConfig? {
        return try {
            val configJson = preferences.getString("cached_config", null)
            if (!configJson.isNullOrEmpty()) {
                Json.decodeFromString<AppConfig>(configJson)
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 缓存配置
     */
    private fun cacheConfig(config: AppConfig) {
        try {
            val configJson = Json.encodeToString(AppConfig.serializer(), config)
            preferences.edit()
                .putString("cached_config", configJson)
                .putLong("config_cache_time", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            // 缓存失败不影响主流程
        }
    }
    
    /**
     * 检查配置是否有效
     */
    private fun isConfigValid(config: AppConfig): Boolean {
        val cacheTime = preferences.getLong("config_cache_time", 0)
        val currentTime = System.currentTimeMillis()
        val cacheExpireTime = 24 * 60 * 60 * 1000L // 24小时
        
        return (currentTime - cacheTime) < cacheExpireTime && 
               config.projectUrl.isNotEmpty() && 
               config.apiKey.isNotEmpty()
    }
    
    /**
     * 获取初始配置（已废弃，现在使用 SupabaseClient）
     * 保留此方法以防兼容性问题
     */
    @Deprecated("使用 SupabaseClient 替代")
    private fun getInitialConfig(): AppConfig? {
        return null // 不再使用本地配置文件
    }

    /**
     * 从URL提取项目ID
     */
    private fun extractProjectIdFromUrl(url: String): String {
        return try {
            // 从 https://xxx.supabase.co 提取 xxx
            val regex = "https://([^.]+)\\.supabase\\.co".toRegex()
            regex.find(url)?.groupValues?.get(1) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 获取临时默认配置（避免应用崩溃）
     */
    private fun getTemporaryDefaultConfig(): AppConfig {
        return AppConfig(
            id = 0,
            appId = Constants.CONFIG_APP_ID,
            projectName = "Temporary Config",
            projectUrl = "https://temp.supabase.co",
            projectId = "temp",
            apiKey = "temp-key",
            accessToken = "",
            isActive = true,
            createdAt = "",
            updatedAt = "",
            jwtSecret = "",
            serviceRole = null // 临时配置没有Service Role Key
        )
    }

    /**
     * 获取默认配置（仅在无法获取服务器配置时使用）
     */
    private fun getDefaultConfig(): AppConfig? {
        // 不再提供硬编码的默认配置
        // 如果无法从服务器获取配置，返回null，要求用户手动配置
        return null
    }
    
    /**
     * 获取当前配置
     */
    fun getCurrentConfig(): AppConfig? = currentConfig
    
    /**
     * 获取Supabase URL
     */
    fun getSupabaseUrl(): String {
        return currentConfig?.projectUrl
            ?: throw IllegalStateException(Constants.ERROR_CONFIG_NOT_INITIALIZED)
    }

    /**
     * 获取API Key
     */
    fun getApiKey(): String {
        return currentConfig?.apiKey
            ?: throw IllegalStateException(Constants.ERROR_CONFIG_NOT_INITIALIZED)
    }

    /**
     * 获取Service Role Key (优先使用supabaseService字段)
     */
    fun getServiceRoleKey(): String {
        val config = currentConfig ?: throw IllegalStateException(Constants.ERROR_CONFIG_NOT_INITIALIZED)

        Log.d("ONETV_MOVIE", "🔍 检查Service Role Key配置...")
        Log.d("ONETV_MOVIE", "📋 配置来源: ${if (getCachedConfig() != null) "缓存" else "数据库"}")
        Log.d("ONETV_MOVIE", "📋 配置ID: ${config.id}")
        Log.d("ONETV_MOVIE", "📋 配置更新时间: ${config.updatedAt}")
        Log.d("ONETV_MOVIE", "📋 serviceRole字段: ${if (config.serviceRole.isNullOrEmpty()) "空" else "已设置(${config.serviceRole!!.take(30)}...)"}")
        Log.d("ONETV_MOVIE", "📋 accessToken字段: ${if (config.accessToken.isEmpty()) "空" else "已设置(${config.accessToken.take(30)}...)"}")

        // 如果serviceRole为空，打印完整的配置信息用于调试
        if (config.serviceRole.isNullOrEmpty()) {
            Log.e("ONETV_MOVIE", "🔍 完整配置调试信息:")
            Log.e("ONETV_MOVIE", "   ID: ${config.id}")
            Log.e("ONETV_MOVIE", "   AppID: ${config.appId}")
            Log.e("ONETV_MOVIE", "   ProjectName: ${config.projectName}")
            Log.e("ONETV_MOVIE", "   CreatedAt: ${config.createdAt}")
            Log.e("ONETV_MOVIE", "   UpdatedAt: ${config.updatedAt}")
            Log.e("ONETV_MOVIE", "   ServiceRole: ${config.serviceRole}")
            Log.e("ONETV_MOVIE", "   AccessToken: ${config.accessToken.take(50)}...")
        }

        // 如果serviceRole为空，这是一个严重问题，尝试强制重新加载
        if (config.serviceRole.isNullOrEmpty()) {
            Log.e("ONETV_MOVIE", "❌ CRITICAL: serviceRole字段为空！")
            Log.e("ONETV_MOVIE", "❌ 数据库应该包含: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            Log.e("ONETV_MOVIE", "❌ 可能原因: 1) 应用缓存了旧数据 2) 数据库查询问题")
            Log.e("ONETV_MOVIE", "🔄 尝试强制清除缓存并重新加载...")

            // 强制清除缓存
            try {
                preferences.edit().clear().apply()
                Log.d("ONETV_MOVIE", "✅ 缓存已清除")

                // 标记需要重新加载（在下次初始化时会自动重新加载）
                Log.d("ONETV_MOVIE", "🔄 标记需要重新加载配置...")
                preferences.edit().putBoolean("force_reload_config", true).apply()
            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "❌ 强制重新加载失败", e)
            }
        }

        // 优先使用新的serviceRole字段，如果没有则使用accessToken作为后备
        val serviceKey = config.serviceRole?.takeIf { it.isNotEmpty() }
            ?: config.accessToken.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("Service Role Key未配置")

        Log.d("ONETV_MOVIE", "✅ 使用Service Role Key: ${serviceKey.take(30)}... (长度: ${serviceKey.length})")

        // 验证Service Role Key格式
        if (serviceKey.startsWith("eyJ")) {
            Log.d("ONETV_MOVIE", "✅ Service Role Key格式正确 (JWT)")
        } else {
            Log.e("ONETV_MOVIE", "❌ Service Role Key格式错误！")
            Log.e("ONETV_MOVIE", "❌ 期望格式: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            Log.e("ONETV_MOVIE", "❌ 实际格式: ${serviceKey.take(30)}...")
            Log.e("ONETV_MOVIE", "❌ 这将导致HTTP 401认证失败")
        }

        return serviceKey
    }

    /**
     * 获取Project ID
     */
    fun getProjectId(): String {
        return currentConfig?.projectId
            ?: throw IllegalStateException(Constants.ERROR_CONFIG_NOT_INITIALIZED)
    }
    
    /**
     * 强制刷新配置
     */
    suspend fun refreshConfig(): Result<AppConfig> {
        // 清除缓存
        preferences.edit().remove("cached_config").apply()

        // 重新初始化
        return initializeConfig()
    }


    
    /**
     * 检查配置是否已初始化
     */
    fun isConfigInitialized(): Boolean = currentConfig != null
    
    /**
     * 获取VOD配置URL
     */
    fun getVodConfigUrl(): String {
        val baseUrl = getSupabaseUrl()
        return "$baseUrl/functions/v1/vod-config"
    }

    /**
     * 获取VOD内容API URL
     */
    fun getVodContentUrl(): String {
        val baseUrl = getSupabaseUrl()
        return "$baseUrl/functions/v1/vod-content"
    }
    
    /**
     * 获取存储桶URL
     */
    fun getStorageBucketUrl(bucketName: String): String {
        val baseUrl = getSupabaseUrl()
        return "$baseUrl/storage/v1/object/public/$bucketName"
    }
    
    /**
     * 手动设置初始配置（用于首次设置或配置更新）
     */
    fun setInitialConfig(projectUrl: String, apiKey: String): Result<String> {
        return try {
            // 验证URL格式
            if (!projectUrl.matches("https://[a-zA-Z0-9-]+\\.supabase\\.co".toRegex())) {
                return Result.failure(Exception("无效的Supabase URL格式"))
            }

            // 验证API Key格式（基本验证）
            if (apiKey.length < 50) {
                return Result.failure(Exception("API Key格式不正确"))
            }

            // 保存到本地存储
            preferences.edit()
                .putString("initial_project_url", projectUrl)
                .putString("initial_api_key", apiKey)
                .apply()

            Result.success("初始配置保存成功")

        } catch (e: Exception) {
            Result.failure(Exception("保存配置失败: ${e.message}"))
        }
    }

    /**
     * 清除本地配置（用于重置）
     */
    fun clearLocalConfig() {
        preferences.edit()
            .remove("initial_project_url")
            .remove("initial_api_key")
            .remove("cached_config")
            .remove("config_cache_time")
            .apply()

        currentConfig = null
    }

    /**
     * 检查是否有初始配置
     */
    fun hasInitialConfig(): Boolean {
        val savedUrl = preferences.getString("initial_project_url", null)
        val savedKey = preferences.getString("initial_api_key", null)
        return !savedUrl.isNullOrEmpty() && !savedKey.isNullOrEmpty()
    }

    /**
     * 获取配置摘要信息
     */
    fun getConfigSummary(): String {
        val config = currentConfig
        return if (config != null) {
            "项目: ${config.projectName}, ID: ${config.projectId}, 状态: ${if (config.isActive) "激活" else "未激活"}"
        } else {
            "配置未初始化"
        }
    }
}
