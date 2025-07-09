package top.cywin.onetv.movie.data.cloud

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 网盘管理器 - 基于OneMoVie架构的网盘功能实现
 * 支持阿里云盘、百度网盘、夸克网盘等多种网盘服务
 */
@Singleton
class CloudDriveManager @Inject constructor() {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * 网盘类型枚举
     */
    enum class CloudDriveType(val displayName: String, val identifier: String) {
        ALI_DRIVE("阿里云盘", "ali"),
        BAIDU_PAN("百度网盘", "baidu"),
        QUARK_PAN("夸克网盘", "quark"),
        ONEDRIVE("OneDrive", "onedrive"),
        GOOGLE_DRIVE("Google Drive", "google"),
        ALIST("AList", "alist")
    }
    
    /**
     * 网盘配置信息
     */
    @Serializable
    data class CloudDriveConfig(
        val id: String,
        val name: String,
        val type: CloudDriveType,
        val baseUrl: String,
        val token: String = "",
        val refreshToken: String = "",
        val username: String = "",
        val password: String = "",
        val isActive: Boolean = true,
        val lastSync: Long = 0L,
        val settings: Map<String, String> = emptyMap()
    )
    
    /**
     * 网盘文件信息
     */
    @Serializable
    data class CloudFile(
        val id: String,
        val name: String,
        val path: String,
        val size: Long = 0L,
        val isDirectory: Boolean = false,
        val mimeType: String = "",
        val downloadUrl: String = "",
        val thumbnailUrl: String = "",
        val createTime: Long = 0L,
        val updateTime: Long = 0L,
        val parentId: String = ""
    ) {
        /**
         * 是否为视频文件
         */
        fun isVideoFile(): Boolean {
            val videoExtensions = listOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v")
            val extension = name.substringAfterLast('.', "").lowercase()
            return videoExtensions.contains(extension)
        }
        
        /**
         * 获取文件扩展名
         */
        fun getExtension(): String = name.substringAfterLast('.', "")
        
        /**
         * 格式化文件大小
         */
        fun getFormattedSize(): String {
            return when {
                size < 1024 -> "${size}B"
                size < 1024 * 1024 -> "${size / 1024}KB"
                size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)}MB"
                else -> "${size / (1024 * 1024 * 1024)}GB"
            }
        }
    }
    
    /**
     * 网盘操作结果
     */
    @Serializable
    data class CloudResult<T>(
        val success: Boolean,
        val data: T? = null,
        val error: String? = null,
        val code: Int = 0
    ) {
        companion object {
            fun <T> success(data: T): CloudResult<T> = CloudResult(true, data)
            fun <T> failure(error: String, code: Int = -1): CloudResult<T> = CloudResult(false, null, error, code)
        }
    }
    
    /**
     * 获取网盘文件列表
     */
    suspend fun getFileList(
        config: CloudDriveConfig,
        parentId: String = "root",
        path: String = "/"
    ): CloudResult<List<CloudFile>> {
        return withContext(Dispatchers.IO) {
            try {
                when (config.type) {
                    CloudDriveType.ALI_DRIVE -> getAliDriveFiles(config, parentId)
                    CloudDriveType.BAIDU_PAN -> getBaiduPanFiles(config, path)
                    CloudDriveType.QUARK_PAN -> getQuarkPanFiles(config, parentId)
                    CloudDriveType.ALIST -> getAListFiles(config, path)
                    else -> CloudResult.failure("暂不支持该网盘类型")
                }
            } catch (e: Exception) {
                Log.e("CloudDriveManager", "获取文件列表失败: ${e.message}")
                CloudResult.failure("获取文件列表失败: ${e.message}")
            }
        }
    }
    
    /**
     * 获取阿里云盘文件列表
     */
    private suspend fun getAliDriveFiles(config: CloudDriveConfig, parentId: String): CloudResult<List<CloudFile>> {
        try {
            val url = "${config.baseUrl}/adrive/v3/file/list"
            val requestBody = """
                {
                    "drive_id": "${config.settings["drive_id"] ?: ""}",
                    "parent_file_id": "$parentId",
                    "limit": 100,
                    "all": false,
                    "url_expire_sec": 1600,
                    "image_thumbnail_process": "image/resize,w_400/format,jpeg",
                    "image_url_process": "image/resize,w_1920/format,jpeg",
                    "video_thumbnail_process": "video/snapshot,t_0,f_jpg,ar_auto,w_300",
                    "fields": "*"
                }
            """.trimIndent()
            
            val request = Request.Builder()
                .url(url)
                .post(okhttp3.RequestBody.create(
                    okhttp3.MediaType.Companion.get("application/json"), 
                    requestBody
                ))
                .header("Authorization", "Bearer ${config.token}")
                .header("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val jsonResponse = Json.parseToJsonElement(responseBody).jsonObject
                val items = jsonResponse["items"]?.jsonArray ?: return CloudResult.failure("响应格式错误")
                
                val files = items.map { item ->
                    val fileObj = item.jsonObject
                    CloudFile(
                        id = fileObj["file_id"]?.jsonPrimitive?.content ?: "",
                        name = fileObj["name"]?.jsonPrimitive?.content ?: "",
                        path = fileObj["file_path"]?.jsonPrimitive?.content ?: "",
                        size = fileObj["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                        isDirectory = fileObj["type"]?.jsonPrimitive?.content == "folder",
                        mimeType = fileObj["mime_type"]?.jsonPrimitive?.content ?: "",
                        downloadUrl = fileObj["download_url"]?.jsonPrimitive?.content ?: "",
                        thumbnailUrl = fileObj["thumbnail"]?.jsonPrimitive?.content ?: "",
                        createTime = parseTimeString(fileObj["created_at"]?.jsonPrimitive?.content),
                        updateTime = parseTimeString(fileObj["updated_at"]?.jsonPrimitive?.content),
                        parentId = parentId
                    )
                }
                
                CloudResult.success(files)
            } else {
                CloudResult.failure("请求失败: ${response.code}")
            }
            
        } catch (e: Exception) {
            CloudResult.failure("阿里云盘请求失败: ${e.message}")
        }
    }
    
    /**
     * 获取百度网盘文件列表
     */
    private suspend fun getBaiduPanFiles(config: CloudDriveConfig, path: String): CloudResult<List<CloudFile>> {
        try {
            val encodedPath = URLEncoder.encode(path, "UTF-8")
            val url = "${config.baseUrl}/rest/2.0/xpan/file?method=list&dir=$encodedPath&access_token=${config.token}"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "pan.baidu.com")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val jsonResponse = Json.parseToJsonElement(responseBody).jsonObject
                val list = jsonResponse["list"]?.jsonArray ?: return CloudResult.failure("响应格式错误")
                
                val files = list.map { item ->
                    val fileObj = item.jsonObject
                    CloudFile(
                        id = fileObj["fs_id"]?.jsonPrimitive?.content ?: "",
                        name = fileObj["server_filename"]?.jsonPrimitive?.content ?: "",
                        path = fileObj["path"]?.jsonPrimitive?.content ?: "",
                        size = fileObj["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                        isDirectory = fileObj["isdir"]?.jsonPrimitive?.content == "1",
                        mimeType = fileObj["category"]?.jsonPrimitive?.content ?: "",
                        createTime = fileObj["server_ctime"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                        updateTime = fileObj["server_mtime"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                    )
                }
                
                CloudResult.success(files)
            } else {
                CloudResult.failure("请求失败: ${response.code}")
            }
            
        } catch (e: Exception) {
            CloudResult.failure("百度网盘请求失败: ${e.message}")
        }
    }
    
    /**
     * 获取夸克网盘文件列表
     */
    private suspend fun getQuarkPanFiles(config: CloudDriveConfig, parentId: String): CloudResult<List<CloudFile>> {
        try {
            val url = "${config.baseUrl}/1/clouddrive/file/sort?pr=ucpro&fr=pc&pdir_fid=$parentId&_page=1&_size=50&_fetch_total=1&_fetch_sub_dirs=0&_sort=file_type:asc,updated_at:desc"
            
            val request = Request.Builder()
                .url(url)
                .header("Cookie", config.token)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val jsonResponse = Json.parseToJsonElement(responseBody).jsonObject
                val data = jsonResponse["data"]?.jsonObject
                val list = data?.get("list")?.jsonArray ?: return CloudResult.failure("响应格式错误")
                
                val files = list.map { item ->
                    val fileObj = item.jsonObject
                    CloudFile(
                        id = fileObj["fid"]?.jsonPrimitive?.content ?: "",
                        name = fileObj["file_name"]?.jsonPrimitive?.content ?: "",
                        path = fileObj["file_name"]?.jsonPrimitive?.content ?: "",
                        size = fileObj["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                        isDirectory = fileObj["dir"]?.jsonPrimitive?.content == "true",
                        mimeType = fileObj["obj_category"]?.jsonPrimitive?.content ?: "",
                        createTime = fileObj["created_at"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                        updateTime = fileObj["updated_at"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                        parentId = parentId
                    )
                }
                
                CloudResult.success(files)
            } else {
                CloudResult.failure("请求失败: ${response.code}")
            }
            
        } catch (e: Exception) {
            CloudResult.failure("夸克网盘请求失败: ${e.message}")
        }
    }
    
    /**
     * 获取AList文件列表
     */
    private suspend fun getAListFiles(config: CloudDriveConfig, path: String): CloudResult<List<CloudFile>> {
        try {
            val url = "${config.baseUrl}/api/fs/list"
            val requestBody = """{"path": "$path", "password": "", "page": 1, "per_page": 0, "refresh": false}"""
            
            val request = Request.Builder()
                .url(url)
                .post(okhttp3.RequestBody.create(
                    okhttp3.MediaType.Companion.get("application/json"), 
                    requestBody
                ))
                .header("Authorization", "Bearer ${config.token}")
                .header("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val jsonResponse = Json.parseToJsonElement(responseBody).jsonObject
                val data = jsonResponse["data"]?.jsonObject
                val content = data?.get("content")?.jsonArray ?: return CloudResult.failure("响应格式错误")
                
                val files = content.map { item ->
                    val fileObj = item.jsonObject
                    CloudFile(
                        id = fileObj["name"]?.jsonPrimitive?.content ?: "",
                        name = fileObj["name"]?.jsonPrimitive?.content ?: "",
                        path = "$path/${fileObj["name"]?.jsonPrimitive?.content ?: ""}",
                        size = fileObj["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                        isDirectory = fileObj["is_dir"]?.jsonPrimitive?.content?.toBoolean() ?: false,
                        mimeType = fileObj["type"]?.jsonPrimitive?.content ?: "",
                        createTime = parseTimeString(fileObj["created"]?.jsonPrimitive?.content),
                        updateTime = parseTimeString(fileObj["modified"]?.jsonPrimitive?.content)
                    )
                }
                
                CloudResult.success(files)
            } else {
                CloudResult.failure("请求失败: ${response.code}")
            }
            
        } catch (e: Exception) {
            CloudResult.failure("AList请求失败: ${e.message}")
        }
    }
    
    /**
     * 获取文件下载链接
     */
    suspend fun getDownloadUrl(config: CloudDriveConfig, file: CloudFile): CloudResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                when (config.type) {
                    CloudDriveType.ALI_DRIVE -> getAliDriveDownloadUrl(config, file)
                    CloudDriveType.BAIDU_PAN -> getBaiduPanDownloadUrl(config, file)
                    CloudDriveType.ALIST -> getAListDownloadUrl(config, file)
                    else -> CloudResult.failure("暂不支持该网盘类型的下载")
                }
            } catch (e: Exception) {
                CloudResult.failure("获取下载链接失败: ${e.message}")
            }
        }
    }
    
    /**
     * 获取阿里云盘下载链接
     */
    private suspend fun getAliDriveDownloadUrl(config: CloudDriveConfig, file: CloudFile): CloudResult<String> {
        // 阿里云盘下载链接获取逻辑
        return if (file.downloadUrl.isNotEmpty()) {
            CloudResult.success(file.downloadUrl)
        } else {
            CloudResult.failure("无法获取下载链接")
        }
    }
    
    /**
     * 获取百度网盘下载链接
     */
    private suspend fun getBaiduPanDownloadUrl(config: CloudDriveConfig, file: CloudFile): CloudResult<String> {
        // 百度网盘下载链接获取逻辑
        return CloudResult.failure("百度网盘下载需要特殊处理")
    }
    
    /**
     * 获取AList下载链接
     */
    private suspend fun getAListDownloadUrl(config: CloudDriveConfig, file: CloudFile): CloudResult<String> {
        return CloudResult.success("${config.baseUrl}/d${file.path}")
    }
    
    /**
     * 解析时间字符串
     */
    private fun parseTimeString(timeString: String?): Long {
        return try {
            timeString?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
