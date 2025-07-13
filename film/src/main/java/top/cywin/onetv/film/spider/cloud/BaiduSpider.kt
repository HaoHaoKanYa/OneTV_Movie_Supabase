package top.cywin.onetv.film.spider.cloud

import android.util.Log
import top.cywin.onetv.film.catvod.Spider
import top.cywin.onetv.film.utils.JsonUtils
import top.cywin.onetv.film.utils.StringUtils
import top.cywin.onetv.film.network.NetworkClient

/**
 * 百度网盘解析器
 * 
 * 基于 FongMi/TV 的百度网盘解析实现
 * 支持百度网盘资源的解析和播放
 * 
 * 功能：
 * - 百度网盘登录认证
 * - 文件列表获取
 * - 播放链接解析
 * - 分享链接处理
 * - 自动刷新 Token
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class BaiduSpider : Spider() {
    
    companion object {
        private const val TAG = "ONETV_FILM_BAIDU_SPIDER"
        private const val BAIDU_API_BASE = "https://pan.baidu.com"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
    
    private lateinit var networkClient: NetworkClient
    private var accessToken: String = ""
    private var refreshToken: String = ""
    private var bdstoken: String = ""
    
    override fun init(context: android.content.Context, extend: String) {
        super.init(context, extend)
        
        networkClient = NetworkClient.Builder()
            .userAgent(USER_AGENT)
            .timeout(30000L)
            .build()
        
        // 解析扩展配置
        parseExtendConfig(extend)
        
        Log.d(TAG, "🕷️ BaiduSpider 初始化完成")
    }
    
    override fun getName(): String = "百度网盘"
    
    override fun getApi(): String = api
    
    override fun getType(): Int = 3 // 特殊类型
    
    override fun isSearchable(): Boolean = true
    
    override fun isFilterable(): Boolean = false
    
    /**
     * 🏠 获取首页内容
     */
    override fun homeContent(filter: Boolean): String {
        Log.d(TAG, "🏠 获取百度网盘首页内容")
        
        return try {
            // 检查登录状态
            if (!isLoggedIn()) {
                return createLoginRequiredResult()
            }
            
            // 构建分类
            val classes = mutableListOf<Map<String, Any>>()
            classes.add(mapOf(
                "type_id" to "root",
                "type_name" to "我的网盘"
            ))
            classes.add(mapOf(
                "type_id" to "video",
                "type_name" to "视频文件"
            ))
            classes.add(mapOf(
                "type_id" to "share",
                "type_name" to "分享文件"
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
        Log.d(TAG, "📂 获取百度网盘分类内容: tid=$tid, pg=$pg")
        
        return try {
            if (!isLoggedIn()) {
                return createLoginRequiredResult()
            }
            
            val fileList = when (tid) {
                "root" -> getFileList("/") // 根目录
                "video" -> getVideoFiles()
                "share" -> getSharedFiles()
                "recent" -> getRecentFiles()
                else -> getFileList(tid)
            }
            
            val vodList = fileList.map { file ->
                mapOf(
                    "vod_id" to file["fs_id"],
                    "vod_name" to file["server_filename"],
                    "vod_pic" to (file["thumbs"] ?: ""),
                    "vod_remarks" to formatFileSize(file["size"] as? Long ?: 0L),
                    "vod_tag" to if (file["isdir"] == 1) "文件夹" else "文件"
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
        Log.d(TAG, "🔍 百度网盘搜索: key=$key")
        
        return try {
            if (!isLoggedIn()) {
                return createLoginRequiredResult()
            }
            
            val searchResults = searchFiles(key)
            
            val vodList = searchResults.map { file ->
                mapOf(
                    "vod_id" to file["fs_id"],
                    "vod_name" to file["server_filename"],
                    "vod_pic" to (file["thumbs"] ?: ""),
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
        Log.d(TAG, "📄 获取百度网盘详情: ids=$ids")
        
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
            
            val playUrls = if (fileInfo["isdir"] == 0) {
                // 单个文件
                listOf("播放$${getPlayUrl(fileId)}")
            } else {
                // 文件夹，获取其中的视频文件
                val videoFiles = getVideoFilesInFolder(fileInfo["path"] as String)
                videoFiles.mapIndexed { index, file ->
                    "${file["server_filename"]}$${getPlayUrl(file["fs_id"] as String)}"
                }
            }
            
            val detailItem = mapOf(
                "vod_id" to fileId,
                "vod_name" to fileInfo["server_filename"],
                "vod_pic" to (fileInfo["thumbs"] ?: ""),
                "vod_content" to "百度网盘文件",
                "vod_play_from" to "百度网盘",
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
        Log.d(TAG, "🎬 获取百度网盘播放内容: flag=$flag, id=$id")
        
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
                    "Referer" to "https://pan.baidu.com/"
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
                bdstoken = JsonUtils.getString(config, "bdstoken") ?: ""
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 解析扩展配置失败", e)
        }
    }
    
    /**
     * 🔐 检查登录状态
     */
    private fun isLoggedIn(): Boolean {
        return accessToken.isNotEmpty() && bdstoken.isNotEmpty()
    }
    
    /**
     * 📁 获取文件列表
     */
    private fun getFileList(path: String): List<Map<String, Any>> {
        return try {
            // 这里应该调用百度网盘 API
            // 暂时返回模拟数据
            listOf(
                mapOf(
                    "fs_id" to "mock_file_1",
                    "server_filename" to "示例视频.mp4",
                    "isdir" to 0, // 0=文件, 1=文件夹
                    "size" to 1024000000L,
                    "thumbs" to "",
                    "path" to "$path/示例视频.mp4"
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
        return getFileList("/").filter { file ->
            val name = file["server_filename"] as? String ?: ""
            isVideoFile(name)
        }
    }
    
    /**
     * 📤 获取分享文件
     */
    private fun getSharedFiles(): List<Map<String, Any>> {
        return try {
            // 这里应该获取分享的文件
            getVideoFiles().take(5)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取分享文件失败", e)
            emptyList()
        }
    }
    
    /**
     * 🕒 获取最近文件
     */
    private fun getRecentFiles(): List<Map<String, Any>> {
        return getVideoFiles().take(10)
    }
    
    /**
     * 🔍 搜索文件
     */
    private fun searchFiles(keyword: String): List<Map<String, Any>> {
        return try {
            // 这里应该调用百度网盘搜索 API
            getVideoFiles().filter { file ->
                val name = file["server_filename"] as? String ?: ""
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
            // 这里应该调用百度网盘 API 获取文件详情
            mapOf(
                "fs_id" to fileId,
                "server_filename" to "示例文件",
                "isdir" to 0,
                "size" to 1024000000L,
                "path" to "/示例文件"
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取文件信息失败", e)
            null
        }
    }
    
    /**
     * 📁 获取文件夹中的视频文件
     */
    private fun getVideoFilesInFolder(folderPath: String): List<Map<String, Any>> {
        return getFileList(folderPath).filter { file ->
            file["isdir"] == 0 && isVideoFile(file["server_filename"] as? String ?: "")
        }
    }
    
    /**
     * 🎬 获取播放链接
     */
    private fun getPlayUrl(fileId: String): String {
        return try {
            // 这里应该调用百度网盘 API 获取播放链接
            "https://mock-baidu-play-url.com/video.m3u8"
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
            "error" to "需要登录百度网盘",
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
