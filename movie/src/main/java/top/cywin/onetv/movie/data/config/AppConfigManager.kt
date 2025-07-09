package top.cywin.onetv.movie.data.config

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import top.cywin.onetv.movie.data.api.Constants
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用配置管理器 - 从app_configs表读取配置
 * 基于OneMoVie架构，支持动态配置加载
 */
@Singleton
class AppConfigManager @Inject constructor(
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
        val jwtSecret: String
    )
    
    /**
     * 初始化配置
     */
    suspend fun initializeConfig(): Result<AppConfig> {
        return try {
            // 1. 尝试从缓存获取
            val cachedConfig = getCachedConfig()
            if (cachedConfig != null && isConfigValid(cachedConfig)) {
                currentConfig = cachedConfig
                return Result.success(cachedConfig)
            }
            
            // 2. 从服务器获取最新配置
            val serverConfig = fetchConfigFromServer()
            if (serverConfig != null) {
                currentConfig = serverConfig
                cacheConfig(serverConfig)
                return Result.success(serverConfig)
            }
            
            // 3. 无法获取配置，需要用户手动设置
            Result.failure(Exception("无法获取配置，请检查网络连接或手动配置服务器信息"))
            
        } catch (e: Exception) {
            Result.failure(Exception("配置初始化失败: ${e.message}"))
        }
    }
    
    /**
     * 从服务器获取配置
     * 注意：这里需要一个初始的配置来获取完整配置，通常通过以下方式之一：
     * 1. 从本地预置的最小配置文件读取
     * 2. 从应用启动参数获取
     * 3. 从环境变量获取
     * 4. 从用户输入获取
     */
    private suspend fun fetchConfigFromServer(): AppConfig? {
        return withContext(Dispatchers.IO) {
            try {
                // 尝试从本地预置配置获取初始连接信息
                val initialConfig = getInitialConfig()
                if (initialConfig == null) {
                    return@withContext null
                }

                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("${initialConfig.projectUrl}/rest/v1/app_configs?app_id=eq.${Constants.CONFIG_APP_ID}&is_active=eq.true")
                    .header("apikey", initialConfig.apiKey)
                    .header("Authorization", "Bearer ${initialConfig.apiKey}")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrEmpty()) {
                        val configs = Json.decodeFromString<List<AppConfig>>(responseBody)
                        configs.firstOrNull { it.isActive }
                    } else null
                } else null

            } catch (e: Exception) {
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
     * 获取初始配置（用于首次连接服务器）
     * 这里从安全的本地存储或用户输入获取最小必要信息
     */
    private fun getInitialConfig(): AppConfig? {
        return try {
            // 方案1: 从加密的本地配置文件读取
            val configFile = context.assets.open("initial_config.json")
            val configJson = configFile.bufferedReader().use { it.readText() }
            Json.decodeFromString<AppConfig>(configJson)
        } catch (e: Exception) {
            // 方案2: 从SharedPreferences读取用户设置的配置
            val savedUrl = preferences.getString("initial_project_url", null)
            val savedKey = preferences.getString("initial_api_key", null)

            if (!savedUrl.isNullOrEmpty() && !savedKey.isNullOrEmpty()) {
                AppConfig(
                    id = 0,
                    appId = Constants.CONFIG_APP_ID,
                    projectName = "Initial Config",
                    projectUrl = savedUrl,
                    projectId = extractProjectIdFromUrl(savedUrl),
                    apiKey = savedKey,
                    accessToken = "",
                    isActive = true,
                    createdAt = "",
                    updatedAt = "",
                    jwtSecret = ""
                )
            } else {
                null
            }
        }
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
     * 获取Service Role Key
     */
    fun getServiceRoleKey(): String {
        return currentConfig?.accessToken
            ?: throw IllegalStateException(Constants.ERROR_CONFIG_NOT_INITIALIZED)
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
        return "$baseUrl${Constants.VOD_CONFIG_ENDPOINT}"
    }
    
    /**
     * 获取VOD内容API URL
     */
    fun getVodContentUrl(): String {
        val baseUrl = getSupabaseUrl()
        return "$baseUrl${Constants.VOD_CONTENT_ENDPOINT}"
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
