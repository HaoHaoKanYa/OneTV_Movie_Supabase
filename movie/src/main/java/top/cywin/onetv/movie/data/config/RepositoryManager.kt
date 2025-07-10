package top.cywin.onetv.movie.data.config

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import top.cywin.onetv.movie.data.models.VodConfig
// KotlinPoet专业重构 - 移除Hilt相关import
// import javax.inject.Inject
// import javax.inject.Singleton

/**
 * 仓库配置管理器 - 基于OneMoVie架构的多配置源管理
 * 支持多个配置仓库、配置订阅、自动更新等功能
 * KotlinPoet专业重构 - 移除Hilt依赖，使用标准构造函数
 */
// @Singleton
class RepositoryManager(
    private val context: Context,
    private val appConfigManager: AppConfigManager
) {
    
    private val preferences: SharedPreferences = 
        context.getSharedPreferences("movie_repository_config", Context.MODE_PRIVATE)
    
    private val client = OkHttpClient()
    
    /**
     * 配置仓库信息
     */
    @Serializable
    data class ConfigRepository(
        val id: String,
        val name: String,
        val url: String,
        val type: RepositoryType,
        val isActive: Boolean = true,
        val lastUpdate: Long = 0L,
        val updateInterval: Long = 24 * 60 * 60 * 1000L, // 24小时
        val description: String = "",
        val author: String = "",
        val version: String = "1.0",
        val tags: List<String> = emptyList()
    )
    
    /**
     * 仓库类型枚举
     */
    @Serializable
    enum class RepositoryType(val displayName: String) {
        OFFICIAL("官方仓库"),
        COMMUNITY("社区仓库"),
        PERSONAL("个人仓库"),
        BACKUP("备份仓库")
    }
    
    /**
     * 配置订阅信息
     */
    @Serializable
    data class ConfigSubscription(
        val repositoryId: String,
        val configUrl: String,
        val autoUpdate: Boolean = true,
        val priority: Int = 0, // 优先级，数字越大优先级越高
        val lastSync: Long = 0L
    )
    
    private var repositories = mutableListOf<ConfigRepository>()
    private var subscriptions = mutableListOf<ConfigSubscription>()
    
    init {
        loadRepositories()
        loadSubscriptions()
    }
    
    /**
     * 初始化默认仓库
     */
    suspend fun initializeDefaultRepositories(): Result<String> {
        return try {
            // 添加官方仓库
            val officialRepo = ConfigRepository(
                id = "official_onetv",
                name = "OneTV官方仓库",
                url = "${appConfigManager.getStorageBucketUrl("vod-sources")}/config/",
                type = RepositoryType.OFFICIAL,
                description = "OneTV官方维护的配置仓库",
                author = "OneTV Team"
            )
            
            // 添加社区仓库示例（从配置中读取）
            val communityRepoUrl = getCommunityRepositoryUrl()
            val communityRepo = if (communityRepoUrl.isNotEmpty()) {
                ConfigRepository(
                    id = "community_tvbox",
                    name = "TVBOX社区仓库",
                    url = communityRepoUrl,
                    type = RepositoryType.COMMUNITY,
                    description = "TVBOX社区维护的配置仓库",
                    author = "TVBOX Community"
                )
            } else null
            
            addRepository(officialRepo)
            communityRepo?.let { addRepository(it) }
            
            // 添加默认订阅
            val defaultSubscription = ConfigSubscription(
                repositoryId = officialRepo.id,
                configUrl = "${officialRepo.url}onetv-api-movie.json",
                autoUpdate = true,
                priority = 100
            )
            
            addSubscription(defaultSubscription)
            
            Result.success("默认仓库初始化成功")
            
        } catch (e: Exception) {
            Result.failure(Exception("默认仓库初始化失败: ${e.message}"))
        }
    }
    
    /**
     * 添加配置仓库
     */
    fun addRepository(repository: ConfigRepository): Result<String> {
        return try {
            if (repositories.any { it.id == repository.id }) {
                return Result.failure(Exception("仓库ID已存在"))
            }
            
            repositories.add(repository)
            saveRepositories()
            
            Result.success("仓库添加成功")
        } catch (e: Exception) {
            Result.failure(Exception("仓库添加失败: ${e.message}"))
        }
    }
    
    /**
     * 删除配置仓库
     */
    fun removeRepository(repositoryId: String): Result<String> {
        return try {
            val removed = repositories.removeAll { it.id == repositoryId }
            if (removed) {
                // 同时删除相关订阅
                subscriptions.removeAll { it.repositoryId == repositoryId }
                saveRepositories()
                saveSubscriptions()
                Result.success("仓库删除成功")
            } else {
                Result.failure(Exception("仓库不存在"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("仓库删除失败: ${e.message}"))
        }
    }
    
    /**
     * 添加配置订阅
     */
    fun addSubscription(subscription: ConfigSubscription): Result<String> {
        return try {
            // 检查仓库是否存在
            val repository = repositories.find { it.id == subscription.repositoryId }
            if (repository == null) {
                return Result.failure(Exception("仓库不存在"))
            }
            
            // 检查订阅是否已存在
            val existingIndex = subscriptions.indexOfFirst { 
                it.repositoryId == subscription.repositoryId && it.configUrl == subscription.configUrl 
            }
            
            if (existingIndex >= 0) {
                subscriptions[existingIndex] = subscription
            } else {
                subscriptions.add(subscription)
            }
            
            saveSubscriptions()
            Result.success("订阅添加成功")
            
        } catch (e: Exception) {
            Result.failure(Exception("订阅添加失败: ${e.message}"))
        }
    }
    
    /**
     * 删除配置订阅
     */
    fun removeSubscription(repositoryId: String, configUrl: String): Result<String> {
        return try {
            val removed = subscriptions.removeAll { 
                it.repositoryId == repositoryId && it.configUrl == configUrl 
            }
            
            if (removed) {
                saveSubscriptions()
                Result.success("订阅删除成功")
            } else {
                Result.failure(Exception("订阅不存在"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("订阅删除失败: ${e.message}"))
        }
    }
    
    /**
     * 获取所有配置仓库
     */
    fun getAllRepositories(): List<ConfigRepository> = repositories.toList()
    
    /**
     * 获取活跃的配置仓库
     */
    fun getActiveRepositories(): List<ConfigRepository> = repositories.filter { it.isActive }
    
    /**
     * 获取所有订阅
     */
    fun getAllSubscriptions(): List<ConfigSubscription> = subscriptions.toList()
    
    /**
     * 获取活跃的订阅 (按优先级排序)
     */
    fun getActiveSubscriptions(): List<ConfigSubscription> {
        return subscriptions
            .filter { subscription ->
                repositories.any { it.id == subscription.repositoryId && it.isActive }
            }
            .sortedByDescending { it.priority }
    }
    
    /**
     * 同步所有订阅的配置
     */
    suspend fun syncAllConfigurations(): Result<List<VodConfig>> {
        return withContext(Dispatchers.IO) {
            try {
                val configs = mutableListOf<VodConfig>()
                val activeSubscriptions = getActiveSubscriptions()
                
                for (subscription in activeSubscriptions) {
                    try {
                        val config = fetchConfigFromSubscription(subscription)
                        if (config != null) {
                            configs.add(config)
                            
                            // 更新同步时间
                            val updatedSubscription = subscription.copy(lastSync = System.currentTimeMillis())
                            val index = subscriptions.indexOfFirst { 
                                it.repositoryId == subscription.repositoryId && it.configUrl == subscription.configUrl 
                            }
                            if (index >= 0) {
                                subscriptions[index] = updatedSubscription
                            }
                        }
                    } catch (e: Exception) {
                        // 单个订阅失败不影响其他订阅
                        continue
                    }
                }
                
                saveSubscriptions()
                Result.success(configs)
                
            } catch (e: Exception) {
                Result.failure(Exception("配置同步失败: ${e.message}"))
            }
        }
    }
    
    /**
     * 从订阅获取配置
     */
    private suspend fun fetchConfigFromSubscription(subscription: ConfigSubscription): VodConfig? {
        return try {
            val request = Request.Builder()
                .url(subscription.configUrl)
                .header("User-Agent", "OneTV/1.0")
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val configJson = response.body?.string()
                if (!configJson.isNullOrEmpty()) {
                    Json.decodeFromString<VodConfig>(configJson)
                } else null
            } else null
            
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 检查是否需要更新
     */
    fun needsUpdate(subscription: ConfigSubscription): Boolean {
        val repository = repositories.find { it.id == subscription.repositoryId } ?: return false
        val currentTime = System.currentTimeMillis()
        return (currentTime - subscription.lastSync) > repository.updateInterval
    }
    
    /**
     * 获取仓库统计信息
     */
    fun getRepositoryStats(): Map<String, Any> {
        return mapOf(
            "total_repositories" to repositories.size,
            "active_repositories" to repositories.count { it.isActive },
            "total_subscriptions" to subscriptions.size,
            "active_subscriptions" to getActiveSubscriptions().size,
            "repository_types" to repositories.groupBy { it.type }.mapValues { it.value.size }
        )
    }
    
    /**
     * 保存仓库配置
     */
    private fun saveRepositories() {
        try {
            val json = Json.encodeToString(repositories)
            preferences.edit().putString("repositories", json).apply()
        } catch (e: Exception) {
            // 保存失败不影响主流程
        }
    }
    
    /**
     * 加载仓库配置
     */
    private fun loadRepositories() {
        try {
            val json = preferences.getString("repositories", null)
            if (!json.isNullOrEmpty()) {
                repositories = Json.decodeFromString<MutableList<ConfigRepository>>(json)
            }
        } catch (e: Exception) {
            repositories = mutableListOf()
        }
    }
    
    /**
     * 保存订阅配置
     */
    private fun saveSubscriptions() {
        try {
            val json = Json.encodeToString(subscriptions)
            preferences.edit().putString("subscriptions", json).apply()
        } catch (e: Exception) {
            // 保存失败不影响主流程
        }
    }
    
    /**
     * 加载订阅配置
     */
    private fun loadSubscriptions() {
        try {
            val json = preferences.getString("subscriptions", null)
            if (!json.isNullOrEmpty()) {
                subscriptions = Json.decodeFromString<MutableList<ConfigSubscription>>(json)
            }
        } catch (e: Exception) {
            subscriptions = mutableListOf()
        }
    }

    /**
     * 获取社区仓库URL（从配置或用户设置中读取）
     */
    private fun getCommunityRepositoryUrl(): String {
        return try {
            // 从用户设置中读取社区仓库URL
            preferences.getString("community_repo_url", "") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 设置社区仓库URL
     */
    fun setCommunityRepositoryUrl(url: String): Result<String> {
        return try {
            // 验证URL格式
            if (url.isNotEmpty() && !url.startsWith("http")) {
                return Result.failure(Exception("无效的URL格式"))
            }

            preferences.edit()
                .putString("community_repo_url", url)
                .apply()

            Result.success("社区仓库URL设置成功")
        } catch (e: Exception) {
            Result.failure(Exception("设置失败: ${e.message}"))
        }
    }
}
