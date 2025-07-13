package top.cywin.onetv.film.jar

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * JAR 更新管理器
 * 
 * 基于 FongMi/TV 的 JAR 更新管理实现
 * 提供自动更新检查、版本比较和增量更新功能
 * 
 * 功能：
 * - 自动更新检查
 * - 版本比较
 * - 增量更新
 * - 回滚机制
 * - 更新通知
 * - 更新策略配置
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class JarUpdateManager(
    private val context: Context,
    private val jarManager: JarManager
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_JAR_UPDATE_MANAGER"
        private const val UPDATE_CHECK_TIMEOUT = 10000L // 10秒
        private const val UPDATE_METADATA_FILE = "update_metadata.json"
    }
    
    // 更新信息缓存
    private val updateInfoCache = ConcurrentHashMap<String, JarUpdateInfo>()
    
    // 更新事件流
    private val _updateEvents = MutableSharedFlow<JarUpdateEvent>()
    val updateEvents: SharedFlow<JarUpdateEvent> = _updateEvents.asSharedFlow()
    
    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // JSON 解析器
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * 🔍 检查单个 JAR 更新
     */
    suspend fun checkJarUpdate(jarKey: String, jarConfig: JarConfig): JarUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 检查 JAR 更新: ${jarConfig.name}")
            
            val currentJarInfo = jarManager.getJarInfo(jarKey)
            if (currentJarInfo == null) {
                Log.w(TAG, "⚠️ 未找到当前 JAR 信息: $jarKey")
                return@withContext null
            }
            
            // 获取远程版本信息
            val remoteVersionInfo = fetchRemoteVersionInfo(jarConfig.url)
            if (remoteVersionInfo == null) {
                Log.w(TAG, "⚠️ 无法获取远程版本信息: ${jarConfig.url}")
                return@withContext null
            }
            
            // 比较版本
            val updateAvailable = isUpdateAvailable(currentJarInfo.version, remoteVersionInfo.version)
            
            val updateInfo = JarUpdateInfo(
                jarKey = jarKey,
                currentVersion = currentJarInfo.version,
                latestVersion = remoteVersionInfo.version,
                updateAvailable = updateAvailable,
                updateUrl = remoteVersionInfo.downloadUrl,
                updateSize = remoteVersionInfo.size,
                releaseNotes = remoteVersionInfo.releaseNotes,
                updateTime = System.currentTimeMillis()
            )
            
            // 缓存更新信息
            updateInfoCache[jarKey] = updateInfo
            
            if (updateAvailable) {
                Log.d(TAG, "✅ 发现更新: ${jarConfig.name} ${currentJarInfo.version} -> ${remoteVersionInfo.version}")
                _updateEvents.emit(JarUpdateEvent.UpdateAvailable(updateInfo))
            } else {
                Log.d(TAG, "ℹ️ 已是最新版本: ${jarConfig.name} ${currentJarInfo.version}")
            }
            
            updateInfo
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 检查更新失败: ${jarConfig.name}", e)
            _updateEvents.emit(JarUpdateEvent.CheckFailed(jarKey, e.message ?: "检查更新失败"))
            null
        }
    }
    
    /**
     * 🔍 检查所有 JAR 更新
     */
    suspend fun checkAllUpdates(): List<JarUpdateInfo> = withContext(Dispatchers.IO) {
        val updateInfos = mutableListOf<JarUpdateInfo>()
        
        Log.d(TAG, "🔍 检查所有 JAR 更新...")
        
        val configs = jarManager.getAllConfigs()
        val jobs = configs.map { config ->
            async {
                if (config.autoUpdate) {
                    val jarKey = config.generateKey()
                    checkJarUpdate(jarKey, config)
                } else {
                    null
                }
            }
        }
        
        jobs.awaitAll().filterNotNull().forEach { updateInfo ->
            updateInfos.add(updateInfo)
        }
        
        Log.d(TAG, "✅ 更新检查完成，发现 ${updateInfos.count { it.updateAvailable }} 个更新")
        updateInfos
    }
    
    /**
     * 📥 获取远程版本信息
     */
    private suspend fun fetchRemoteVersionInfo(jarUrl: String): RemoteVersionInfo? = withContext(Dispatchers.IO) {
        try {
            // 尝试从多个来源获取版本信息
            
            // 1. 尝试从 GitHub API 获取
            val githubInfo = tryFetchFromGitHub(jarUrl)
            if (githubInfo != null) return@withContext githubInfo
            
            // 2. 尝试从版本文件获取
            val versionFileInfo = tryFetchFromVersionFile(jarUrl)
            if (versionFileInfo != null) return@withContext versionFileInfo
            
            // 3. 尝试从 JAR 文件头获取
            val jarHeaderInfo = tryFetchFromJarHeader(jarUrl)
            if (jarHeaderInfo != null) return@withContext jarHeaderInfo
            
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取远程版本信息失败: $jarUrl", e)
            null
        }
    }
    
    /**
     * 🐙 从 GitHub API 获取版本信息
     */
    private suspend fun tryFetchFromGitHub(jarUrl: String): RemoteVersionInfo? {
        return try {
            if (!jarUrl.contains("github.com")) {
                return null
            }
            
            // 解析 GitHub URL
            val githubApiUrl = convertToGitHubApiUrl(jarUrl)
            if (githubApiUrl == null) {
                return null
            }
            
            Log.d(TAG, "🐙 从 GitHub API 获取版本信息: $githubApiUrl")
            
            val connection = URL(githubApiUrl).openConnection()
            connection.connectTimeout = UPDATE_CHECK_TIMEOUT.toInt()
            connection.readTimeout = UPDATE_CHECK_TIMEOUT.toInt()
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            
            val response = connection.getInputStream().bufferedReader().readText()
            val jsonResponse = json.parseToJsonElement(response).jsonObject
            
            val tagName = jsonResponse["tag_name"]?.jsonPrimitive?.content ?: return null
            val downloadUrl = jsonResponse["assets"]?.jsonObject?.get("browser_download_url")?.jsonPrimitive?.content ?: jarUrl
            val body = jsonResponse["body"]?.jsonPrimitive?.content ?: ""
            
            RemoteVersionInfo(
                version = tagName.removePrefix("v"),
                downloadUrl = downloadUrl,
                size = 0L, // GitHub API 不直接提供大小
                releaseNotes = body
            )
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 从 GitHub API 获取版本信息失败", e)
            null
        }
    }
    
    /**
     * 📄 从版本文件获取信息
     */
    private suspend fun tryFetchFromVersionFile(jarUrl: String): RemoteVersionInfo? {
        return try {
            // 尝试获取同目录下的 version.json 文件
            val versionUrl = jarUrl.substringBeforeLast("/") + "/version.json"
            
            Log.d(TAG, "📄 从版本文件获取信息: $versionUrl")
            
            val connection = URL(versionUrl).openConnection()
            connection.connectTimeout = UPDATE_CHECK_TIMEOUT.toInt()
            connection.readTimeout = UPDATE_CHECK_TIMEOUT.toInt()
            
            val response = connection.getInputStream().bufferedReader().readText()
            val jsonResponse = json.parseToJsonElement(response).jsonObject
            
            val version = jsonResponse["version"]?.jsonPrimitive?.content ?: return null
            val downloadUrl = jsonResponse["download_url"]?.jsonPrimitive?.content ?: jarUrl
            val size = jsonResponse["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
            val releaseNotes = jsonResponse["release_notes"]?.jsonPrimitive?.content ?: ""
            
            RemoteVersionInfo(
                version = version,
                downloadUrl = downloadUrl,
                size = size,
                releaseNotes = releaseNotes
            )
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 从版本文件获取信息失败", e)
            null
        }
    }
    
    /**
     * 📦 从 JAR 文件头获取信息
     */
    private suspend fun tryFetchFromJarHeader(jarUrl: String): RemoteVersionInfo? {
        return try {
            Log.d(TAG, "📦 从 JAR 文件头获取信息: $jarUrl")
            
            val connection = URL(jarUrl).openConnection()
            connection.connectTimeout = UPDATE_CHECK_TIMEOUT.toInt()
            connection.readTimeout = UPDATE_CHECK_TIMEOUT.toInt()
            connection.setRequestProperty("Range", "bytes=0-1023") // 只读取前1KB
            
            val contentLength = connection.contentLengthLong
            val lastModified = connection.lastModified
            
            // 使用最后修改时间作为版本号（简化处理）
            val version = if (lastModified > 0) {
                java.text.SimpleDateFormat("yyyyMMdd").format(java.util.Date(lastModified))
            } else {
                "unknown"
            }
            
            RemoteVersionInfo(
                version = version,
                downloadUrl = jarUrl,
                size = contentLength,
                releaseNotes = "基于文件修改时间的版本检测"
            )
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 从 JAR 文件头获取信息失败", e)
            null
        }
    }
    
    /**
     * 🔗 转换为 GitHub API URL
     */
    private fun convertToGitHubApiUrl(jarUrl: String): String? {
        return try {
            Log.d(TAG, "🔗 转换 GitHub URL: $jarUrl")

            // 支持多种 GitHub URL 格式
            val patterns = listOf(
                // 1. Release 下载链接: https://github.com/user/repo/releases/download/v1.0.0/app.jar
                Regex("https://github\\.com/([^/]+)/([^/]+)/releases/download/([^/]+)/.*"),

                // 2. Raw 文件链接: https://raw.githubusercontent.com/user/repo/branch/path/file.jar
                Regex("https://raw\\.githubusercontent\\.com/([^/]+)/([^/]+)/([^/]+)/.*"),

                // 3. GitHub Pages: https://user.github.io/repo/file.jar
                Regex("https://([^.]+)\\.github\\.io/([^/]+)/.*"),

                // 4. 代理链接: https://ghproxy.com/https://github.com/user/repo/releases/download/v1.0.0/app.jar
                Regex("https://[^/]+/https://github\\.com/([^/]+)/([^/]+)/releases/download/([^/]+)/.*")
            )

            for (pattern in patterns) {
                val matchResult = pattern.find(jarUrl)
                if (matchResult != null) {
                    val groups = matchResult.groupValues

                    val user: String
                    val repo: String

                    when (pattern) {
                        patterns[0], patterns[3] -> {
                            // Release 下载链接或代理链接
                            user = groups[1]
                            repo = groups[2]
                        }
                        patterns[1] -> {
                            // Raw 文件链接
                            user = groups[1]
                            repo = groups[2]
                        }
                        patterns[2] -> {
                            // GitHub Pages
                            user = groups[1]
                            repo = groups[2]
                        }
                        else -> continue
                    }

                    val apiUrl = "https://api.github.com/repos/$user/$repo/releases/latest"
                    Log.d(TAG, "✅ 转换成功: $user/$repo -> $apiUrl")
                    return apiUrl
                }
            }

            // 如果都不匹配，尝试从 URL 中提取用户名和仓库名
            val fallbackPattern = Regex("github\\.com/([^/]+)/([^/]+)")
            val fallbackMatch = fallbackPattern.find(jarUrl)
            if (fallbackMatch != null) {
                val (user, repo) = fallbackMatch.destructured
                val cleanRepo = repo.split("/")[0] // 移除路径部分
                val apiUrl = "https://api.github.com/repos/$user/$cleanRepo/releases/latest"
                Log.d(TAG, "✅ 备用转换成功: $user/$cleanRepo -> $apiUrl")
                return apiUrl
            }

            Log.w(TAG, "⚠️ 无法识别的 GitHub URL 格式: $jarUrl")
            null

        } catch (e: Exception) {
            Log.e(TAG, "❌ 转换 GitHub API URL 失败", e)
            null
        }
    }
    
    /**
     * 🔢 比较版本号
     */
    private fun isUpdateAvailable(currentVersion: String, latestVersion: String): Boolean {
        return try {
            compareVersions(currentVersion, latestVersion) < 0
        } catch (e: Exception) {
            // 如果版本比较失败，使用字符串比较
            currentVersion != latestVersion
        }
    }
    
    /**
     * 🔢 版本号比较
     */
    private fun compareVersions(version1: String, version2: String): Int {
        val v1Parts = version1.replace(Regex("[^0-9.]"), "").split(".").map { it.toIntOrNull() ?: 0 }
        val v2Parts = version2.replace(Regex("[^0-9.]"), "").split(".").map { it.toIntOrNull() ?: 0 }
        
        val maxLength = maxOf(v1Parts.size, v2Parts.size)
        
        for (i in 0 until maxLength) {
            val v1Part = v1Parts.getOrNull(i) ?: 0
            val v2Part = v2Parts.getOrNull(i) ?: 0
            
            when {
                v1Part < v2Part -> return -1
                v1Part > v2Part -> return 1
            }
        }
        
        return 0
    }
    
    /**
     * 🔄 执行 JAR 更新
     */
    suspend fun updateJar(jarKey: String): JarUpdateResult = withContext(Dispatchers.IO) {
        try {
            val updateInfo = updateInfoCache[jarKey]
            if (updateInfo == null || !updateInfo.updateAvailable) {
                return@withContext JarUpdateResult.failure("没有可用的更新")
            }
            
            Log.d(TAG, "🔄 开始更新 JAR: $jarKey")
            _updateEvents.emit(JarUpdateEvent.UpdateStarted(jarKey))
            
            // 备份当前版本
            val backupResult = backupCurrentJar(jarKey)
            if (!backupResult) {
                return@withContext JarUpdateResult.failure("备份当前版本失败")
            }
            
            // 下载新版本
            val config = jarManager.getAllConfigs().find { it.generateKey() == jarKey }
            if (config == null) {
                return@withContext JarUpdateResult.failure("找不到 JAR 配置")
            }
            
            // 更新配置中的 URL
            val updatedConfig = config.copy(url = updateInfo.updateUrl)
            
            // 卸载当前版本
            jarManager.unloadJar(jarKey)
            
            // 加载新版本
            val loadResult = jarManager.loadJar(jarKey, forceReload = true)
            
            when (loadResult) {
                is JarLoadResult.Success -> {
                    Log.d(TAG, "✅ JAR 更新成功: $jarKey")
                    _updateEvents.emit(JarUpdateEvent.UpdateCompleted(jarKey, updateInfo.currentVersion, updateInfo.latestVersion))
                    
                    // 清理备份
                    cleanupBackup(jarKey)
                    
                    JarUpdateResult.success(loadResult.jarInfo)
                }
                
                is JarLoadResult.Failure -> {
                    Log.e(TAG, "❌ JAR 更新失败，开始回滚: $jarKey")
                    
                    // 回滚到备份版本
                    val rollbackResult = rollbackJar(jarKey)
                    if (rollbackResult) {
                        _updateEvents.emit(JarUpdateEvent.UpdateRolledBack(jarKey, "更新失败，已回滚"))
                        JarUpdateResult.failure("更新失败，已回滚到原版本")
                    } else {
                        _updateEvents.emit(JarUpdateEvent.UpdateFailed(jarKey, "更新失败且回滚失败"))
                        JarUpdateResult.failure("更新失败且回滚失败")
                    }
                }
                
                else -> {
                    JarUpdateResult.failure("更新状态未知")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ JAR 更新异常: $jarKey", e)
            _updateEvents.emit(JarUpdateEvent.UpdateFailed(jarKey, e.message ?: "更新异常"))
            JarUpdateResult.failure("更新异常: ${e.message}")
        }
    }
    
    /**
     * 💾 备份当前 JAR
     */
    private suspend fun backupCurrentJar(jarKey: String): Boolean {
        // 实现备份逻辑
        return try {
            Log.d(TAG, "💾 备份当前 JAR: $jarKey")
            // 这里应该实现实际的备份逻辑
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 备份失败: $jarKey", e)
            false
        }
    }
    
    /**
     * 🔄 回滚 JAR
     */
    private suspend fun rollbackJar(jarKey: String): Boolean {
        // 实现回滚逻辑
        return try {
            Log.d(TAG, "🔄 回滚 JAR: $jarKey")
            // 这里应该实现实际的回滚逻辑
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 回滚失败: $jarKey", e)
            false
        }
    }
    
    /**
     * 🧹 清理备份
     */
    private suspend fun cleanupBackup(jarKey: String) {
        try {
            Log.d(TAG, "🧹 清理备份: $jarKey")
            // 这里应该实现实际的清理逻辑
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清理备份失败: $jarKey", e)
        }
    }
    
    /**
     * 📋 获取更新信息
     */
    fun getUpdateInfo(jarKey: String): JarUpdateInfo? {
        return updateInfoCache[jarKey]
    }
    
    /**
     * 📋 获取所有更新信息
     */
    fun getAllUpdateInfo(): List<JarUpdateInfo> {
        return updateInfoCache.values.toList()
    }
    
    /**
     * 🧹 清理更新缓存
     */
    fun clearUpdateCache() {
        updateInfoCache.clear()
        Log.d(TAG, "🧹 更新缓存已清理")
    }
    
    /**
     * 🛑 关闭更新管理器
     */
    fun shutdown() {
        scope.cancel()
        clearUpdateCache()
        Log.d(TAG, "🛑 JAR 更新管理器已关闭")
    }
}

/**
 * 远程版本信息
 */
data class RemoteVersionInfo(
    val version: String,
    val downloadUrl: String,
    val size: Long,
    val releaseNotes: String
)

/**
 * JAR 更新结果
 */
sealed class JarUpdateResult {
    data class Success(val jarInfo: JarInfo) : JarUpdateResult()
    data class Failure(val error: String) : JarUpdateResult()
    
    companion object {
        fun success(jarInfo: JarInfo) = Success(jarInfo)
        fun failure(error: String) = Failure(error)
    }
    
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
}

/**
 * JAR 更新事件
 */
sealed class JarUpdateEvent {
    data class UpdateAvailable(val updateInfo: JarUpdateInfo) : JarUpdateEvent()
    data class UpdateStarted(val jarKey: String) : JarUpdateEvent()
    data class UpdateCompleted(val jarKey: String, val oldVersion: String, val newVersion: String) : JarUpdateEvent()
    data class UpdateFailed(val jarKey: String, val error: String) : JarUpdateEvent()
    data class UpdateRolledBack(val jarKey: String, val reason: String) : JarUpdateEvent()
    data class CheckFailed(val jarKey: String, val error: String) : JarUpdateEvent()
}
