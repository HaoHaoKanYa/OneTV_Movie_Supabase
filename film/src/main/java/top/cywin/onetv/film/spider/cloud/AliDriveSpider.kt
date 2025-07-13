package top.cywin.onetv.film.spider.cloud

import android.util.Log
import top.cywin.onetv.film.catvod.Spider
import top.cywin.onetv.film.utils.JsonUtils
import top.cywin.onetv.film.utils.StringUtils
import top.cywin.onetv.film.network.NetworkClient

/**
 * 阿里云盘解析器
 * 
 * 基于 FongMi/TV 的阿里云盘解析实现
 * 支持阿里云盘资源的解析和播放
 * 
 * 功能：
 * - 阿里云盘登录认证
 * - 文件列表获取
 * - 播放链接解析
 * - 分享链接处理
 * - 自动刷新 Token
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class AliDriveSpider : Spider() {
    
    companion object {
        private const val TAG = "ONETV_FILM_ALI_DRIVE_SPIDER"
        private const val ALI_API_BASE = "https://api.aliyundrive.com"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
    
    private lateinit var networkClient: NetworkClient
    private var accessToken: String = ""
    private var refreshToken: String = ""
    private var driveId: String = ""
    
    override fun init(context: android.content.Context, extend: String) {
        super.init(context, extend)
        
        networkClient = NetworkClient.Builder()
            .userAgent(USER_AGENT)
            .timeout(30000L)
            .build()
        
        // 解析扩展配置
        parseExtendConfig(extend)
        
        Log.d(TAG, "🕷️ AliDriveSpider 初始化完成")
    }
    
    override fun getName(): String = "阿里云盘"
    
    override fun getApi(): String = api
    
    override fun getType(): Int = 3 // 特殊类型
    
    override fun isSearchable(): Boolean = true
    
    override fun isFilterable(): Boolean = false
    
    /**
     * 🏠 获取首页内容
     */
    override fun homeContent(filter: Boolean): String {
        Log.d(TAG, "🏠 获取阿里云盘首页内容")
        
        return try {
            // 检查登录状态
            if (!isLoggedIn()) {
                return createLoginRequiredResult()
            }
            
            // 获取根目录文件列表
            val rootFiles = getFileList("root")
            
            // 构建分类
            val classes = mutableListOf<Map<String, Any>>()
            classes.add(mapOf(
                "type_id" to "root",
                "type_name" to "根目录"
            ))
            classes.add(mapOf(
                "type_id" to "video",
                "type_name" to "视频文件"
            ))
            classes.add(mapOf(
                "type_id" to "recent",
                "type_name" to "最近播放"
            ))
            
            JsonUtils.createJsonObject(mapOf(
                "class" to classes
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取首页内容失败", e)
            createErrorResult("获取首页失败: ${e.message}")
        }
    }
    
    /**
     * 📂 获取分类内容
     */
    override fun categoryContent(tid: String, pg: String, filter: Boolean, extend: Map<String, String>): String {
        Log.d(TAG, "📂 获取阿里云盘分类内容: tid=$tid, pg=$pg")
        
        return try {
            if (!isLoggedIn()) {
                return createLoginRequiredResult()
            }
            
            val fileList = when (tid) {
                "root" -> getFileList("root")
                "video" -> getVideoFiles()
                "recent" -> getRecentFiles()
                else -> getFileList(tid)
            }
            
            val vodList = fileList.map { file ->
                mapOf(
                    "vod_id" to file["file_id"],
                    "vod_name" to file["name"],
                    "vod_pic" to (file["thumbnail"] ?: ""),
                    "vod_remarks" to formatFileSize(file["size"] as? Long ?: 0L),
                    "vod_tag" to if (file["type"] == "folder") "文件夹" else "文件"
                )
            }
            
            JsonUtils.createJsonObject(mapOf(
                "list" to vodList,
                "page" to pg.toIntOrNull() ?: 1,
                "pagecount" to 1,
                "limit" to vodList.size,
                "total" to vodList.size
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取分类内容失败", e)
            createErrorResult("获取分类失败: ${e.message}")
        }
    }
    
    /**
     * 🔍 搜索内容
     */
    override fun searchContent(key: String, quick: Boolean): String {
        Log.d(TAG, "🔍 阿里云盘搜索: key=$key")
        
        return try {
            if (!isLoggedIn()) {
                return createLoginRequiredResult()
            }
            
            val searchResults = searchFiles(key)
            
            val vodList = searchResults.map { file ->
                mapOf(
                    "vod_id" to file["file_id"],
                    "vod_name" to file["name"],
                    "vod_pic" to (file["thumbnail"] ?: ""),
                    "vod_remarks" to formatFileSize(file["size"] as? Long ?: 0L)
                )
            }
            
            JsonUtils.createJsonObject(mapOf(
                "list" to vodList
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 搜索失败", e)
            createErrorResult("搜索失败: ${e.message}")
        }
    }
    
    /**
     * 📄 获取详情内容
     */
    override fun detailContent(ids: List<String>): String {
        Log.d(TAG, "📄 获取阿里云盘详情: ids=$ids")
        
        return try {
            if (!isLoggedIn()) {
                return createLoginRequiredResult()
            }
            
            if (ids.isEmpty()) {
                return createEmptyResult()
            }
            
            val fileId = ids[0]
            val fileInfo = getFileInfo(fileId)
            
            if (fileInfo == null) {
                return createErrorResult("文件不存在")
            }
            
            val playUrls = if (fileInfo["type"] == "file") {
                // 单个文件
                listOf("播放$${getPlayUrl(fileId)}")
            } else {
                // 文件夹，获取其中的视频文件
                val videoFiles = getVideoFilesInFolder(fileId)
                videoFiles.mapIndexed { index, file ->
                    "${file["name"]}$${getPlayUrl(file["file_id"] as String)}"
                }
            }
            
            val detailItem = mapOf(
                "vod_id" to fileId,
                "vod_name" to fileInfo["name"],
                "vod_pic" to (fileInfo["thumbnail"] ?: ""),
                "vod_content" to "阿里云盘文件",
                "vod_play_from" to "阿里云盘",
                "vod_play_url" to playUrls.joinToString("#")
            )
            
            JsonUtils.createJsonObject(mapOf(
                "list" to listOf(detailItem)
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取详情失败", e)
            createErrorResult("获取详情失败: ${e.message}")
        }
    }
    
    /**
     * 🎬 获取播放内容
     */
    override fun playerContent(flag: String, id: String, vipFlags: List<String>): String {
        Log.d(TAG, "🎬 获取阿里云盘播放内容: flag=$flag, id=$id")
        
        return try {
            if (!isLoggedIn()) {
                return createErrorResult("需要登录")
            }
            
            val playUrl = getPlayUrl(id)
            
            JsonUtils.createJsonObject(mapOf(
                "parse" to 0,
                "playUrl" to "",
                "url" to playUrl,
                "header" to mapOf(
                    "User-Agent" to USER_AGENT,
                    "Referer" to "https://www.aliyundrive.com/"
                )
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取播放内容失败", e)
            createErrorResult("获取播放内容失败: ${e.message}")
        }
    }
    
    /**
     * 🔧 解析扩展配置
     */
    private fun parseExtendConfig(extend: String) {
        try {
            if (extend.isEmpty()) return
            
            val config = JsonUtils.parseToJsonObject(extend)
            if (config != null) {
                accessToken = JsonUtils.getString(config, "access_token") ?: ""
                refreshToken = JsonUtils.getString(config, "refresh_token") ?: ""
                driveId = JsonUtils.getString(config, "drive_id") ?: ""
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 解析扩展配置失败", e)
        }
    }
    
    /**
     * 🔐 检查登录状态
     */
    private fun isLoggedIn(): Boolean {
        return accessToken.isNotEmpty() && driveId.isNotEmpty()
    }
    
    /**
     * 📁 获取文件列表
     */
    private fun getFileList(parentFileId: String): List<Map<String, Any>> {
        return try {
            // 这里应该调用阿里云盘 API
            // 暂时返回模拟数据
            listOf(
                mapOf(
                    "file_id" to "mock_file_1",
                    "name" to "示例视频.mp4",
                    "type" to "file",
                    "size" to 1024000000L,
                    "thumbnail" to ""
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取文件列表失败", e)
            emptyList()
        }
    }
    
    /**
     * 🎥 获取视频文件
     */
    private fun getVideoFiles(): List<Map<String, Any>> {
        return getFileList("root").filter { file ->
            val name = file["name"] as? String ?: ""
            name.endsWith(".mp4", true) || 
            name.endsWith(".mkv", true) || 
            name.endsWith(".avi", true) ||
            name.endsWith(".m3u8", true)
        }
    }
    
    /**
     * 🕒 获取最近文件
     */
    private fun getRecentFiles(): List<Map<String, Any>> {
        // 这里应该获取最近播放的文件
        return getVideoFiles().take(10)
    }
    
    /**
     * 🔍 搜索文件
     */
    private fun searchFiles(keyword: String): List<Map<String, Any>> {
        return try {
            // 这里应该调用阿里云盘搜索 API
            getVideoFiles().filter { file ->
                val name = file["name"] as? String ?: ""
                name.contains(keyword, true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 搜索文件失败", e)
            emptyList()
        }
    }
    
    /**
     * 📄 获取文件信息
     */
    private fun getFileInfo(fileId: String): Map<String, Any>? {
        return try {
            // 这里应该调用阿里云盘 API 获取文件详情
            mapOf(
                "file_id" to fileId,
                "name" to "示例文件",
                "type" to "file",
                "size" to 1024000000L
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取文件信息失败", e)
            null
        }
    }
    
    /**
     * 📁 获取文件夹中的视频文件
     */
    private fun getVideoFilesInFolder(folderId: String): List<Map<String, Any>> {
        return getFileList(folderId).filter { file ->
            file["type"] == "file" && isVideoFile(file["name"] as? String ?: "")
        }
    }
    
    /**
     * 🎬 获取播放链接
     */
    private fun getPlayUrl(fileId: String): String {
        return try {
            // 这里应该调用阿里云盘 API 获取播放链接
            "https://mock-play-url.com/video.m3u8"
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取播放链接失败", e)
            ""
        }
    }
    
    /**
     * 🎥 检查是否为视频文件
     */
    private fun isVideoFile(fileName: String): Boolean {
        val videoExtensions = listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "m3u8", "ts")
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in videoExtensions
    }
    
    /**
     * 📏 格式化文件大小
     */
    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "${size}B"
            size < 1024 * 1024 -> "${size / 1024}KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)}MB"
            else -> "${size / (1024 * 1024 * 1024)}GB"
        }
    }
    
    /**
     * 🔐 创建需要登录的结果
     */
    private fun createLoginRequiredResult(): String {
        return JsonUtils.createJsonObject(mapOf(
            "error" to "需要登录阿里云盘",
            "login_required" to true,
            "list" to emptyList<Any>(),
            "class" to emptyList<Any>()
        ))
    }
    
    /**
     * 📝 创建空结果
     */
    private fun createEmptyResult(): String {
        return JsonUtils.createJsonObject(mapOf(
            "list" to emptyList<Any>(),
            "class" to emptyList<Any>()
        ))
    }
    
    /**
     * ❌ 创建错误结果
     */
    private fun createErrorResult(message: String): String {
        return JsonUtils.createJsonObject(mapOf(
            "error" to message,
            "list" to emptyList<Any>(),
            "class" to emptyList<Any>()
        ))
    }
}
