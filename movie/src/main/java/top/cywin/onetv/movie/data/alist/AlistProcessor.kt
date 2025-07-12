package top.cywin.onetv.movie.data.alist

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import top.cywin.onetv.movie.data.models.VodItem
import top.cywin.onetv.movie.data.models.VodClass
import top.cywin.onetv.movie.data.models.VodSite
import top.cywin.onetv.movie.data.network.TvboxNetworkConfig
import java.util.concurrent.TimeUnit

/**
 * Alist处理器 - 处理TVBOX Alist类型站点（type=4）
 * 支持Alist网盘系统、文件列表、视频播放等
 */
class AlistProcessor(private val context: Context) {
    
    private val httpClient = TvboxNetworkConfig.createTvboxHttpClient()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
    /**
     * Alist文件信息
     */
    @Serializable
    data class AlistFile(
        val name: String,
        val size: Long = 0,
        val is_dir: Boolean = false,
        val modified: String = "",
        val type: Int = 0,
        val thumb: String = "",
        val sign: String = ""
    )
    
    /**
     * Alist响应
     */
    @Serializable
    data class AlistResponse(
        val code: Int,
        val message: String = "",
        val data: AlistData? = null
    )
    
    /**
     * Alist数据
     */
    @Serializable
    data class AlistData(
        val content: List<AlistFile> = emptyList(),
        val total: Int = 0,
        val readme: String = "",
        val write: Boolean = false,
        val provider: String = ""
    )
    
    /**
     * 获取首页内容
     */
    suspend fun getHomeContent(site: VodSite): List<VodItem> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "💾 Alist站点获取首页内容: ${site.name}")
            
            if (site.api.isBlank()) {
                Log.w("ONETV_MOVIE", "⚠️ Alist站点API为空")
                return@withContext emptyList()
            }
            
            // 获取根目录文件列表
            val files = getFileList(site, "/")
            
            // 转换为VodItem
            convertFilesToVodItems(files, "首页推荐")
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ Alist站点获取首页内容失败", e)
            emptyList()
        }
    }
    
    /**
     * 获取分类列表
     */
    suspend fun getCategories(site: VodSite): List<VodClass> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "💾 Alist站点获取分类列表: ${site.name}")
            
            if (site.api.isBlank()) {
                Log.w("ONETV_MOVIE", "⚠️ Alist站点API为空")
                return@withContext emptyList()
            }
            
            // 获取根目录，根据文件夹创建分类
            val files = getFileList(site, "/")
            val categories = mutableListOf<VodClass>()
            
            // 根据文件夹添加动态分类
            files.filter { it.is_dir }.forEachIndexed { index, file ->
                categories.add(
                    VodClass(
                        typeId = (100 + index).toString(),
                        typeName = file.name
                    )
                )
            }
            
            Log.d("ONETV_MOVIE", "✅ Alist分类获取成功: ${categories.size}个分类")
            categories
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ Alist站点获取分类列表失败", e)
            emptyList()
        }
    }
    
    /**
     * 获取分类内容
     */
    suspend fun getCategoryContent(site: VodSite, typeId: String, page: Int): List<VodItem> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "💾 Alist站点获取分类内容: ${site.name}, 分类: $typeId, 页码: $page")
            
            if (site.api.isBlank()) {
                Log.w("ONETV_MOVIE", "⚠️ Alist站点API为空")
                return@withContext emptyList()
            }
            
            // 根据分类ID确定路径（只使用动态分类）
            val path = when {
                typeId.toIntOrNull() ?: 0 >= 100 -> {
                    // 动态分类，需要获取对应文件夹名称
                    getDynamicCategoryPath(site, typeId)
                }
                else -> "/"
            }
            
            // 获取文件列表
            val files = getFileList(site, path)
            
            // 转换为VodItem
            convertFilesToVodItems(files, "分类内容")
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ Alist站点获取分类内容失败", e)
            emptyList()
        }
    }
    
    /**
     * 搜索内容
     */
    suspend fun search(site: VodSite, keyword: String): List<VodItem> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "💾 Alist站点搜索: ${site.name}, 关键词: $keyword")
            
            if (site.api.isBlank()) {
                Log.w("ONETV_MOVIE", "⚠️ Alist站点API为空")
                return@withContext emptyList()
            }
            
            // Alist搜索请求
            val searchResult = performSearch(site, keyword)
            
            // 转换为VodItem
            convertFilesToVodItems(searchResult, "搜索结果")
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ Alist站点搜索失败", e)
            emptyList()
        }
    }
    
    /**
     * 获取文件列表
     */
    private suspend fun getFileList(site: VodSite, path: String): List<AlistFile> {
        return try {
            Log.d("ONETV_MOVIE", "🔄 获取Alist文件列表: $path")
            
            // 构建请求
            val requestBody = """
                {
                    "path": "$path",
                    "password": "",
                    "page": 1,
                    "per_page": 100,
                    "refresh": false
                }
            """.trimIndent()
            
            val request = Request.Builder()
                .url("${site.api}/api/fs/list")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", "OneTV/2.1.1")
                .header("Accept", "application/json")
                .build()
            
            // 执行请求
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("ONETV_MOVIE", "⚠️ Alist文件列表请求失败: HTTP ${response.code}")
                    return emptyList()
                }
                
                val responseBody = response.body?.string() ?: ""
                val alistResponse = json.decodeFromString<AlistResponse>(responseBody)
                
                if (alistResponse.code == 200) {
                    alistResponse.data?.content ?: emptyList()
                } else {
                    Log.w("ONETV_MOVIE", "⚠️ Alist响应错误: ${alistResponse.message}")
                    emptyList()
                }
            }
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 获取Alist文件列表失败", e)
            emptyList()
        }
    }
    
    /**
     * 执行搜索
     */
    private suspend fun performSearch(site: VodSite, keyword: String): List<AlistFile> {
        return try {
            Log.d("ONETV_MOVIE", "🔍 执行Alist搜索: $keyword")
            
            // 构建搜索请求
            val requestBody = """
                {
                    "parent": "/",
                    "keywords": "$keyword",
                    "scope": 0,
                    "page": 1,
                    "per_page": 100
                }
            """.trimIndent()
            
            val request = Request.Builder()
                .url("${site.api}/api/fs/search")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", "OneTV/2.1.1")
                .header("Accept", "application/json")
                .build()
            
            // 执行请求
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("ONETV_MOVIE", "⚠️ Alist搜索请求失败: HTTP ${response.code}")
                    return emptyList()
                }
                
                val responseBody = response.body?.string() ?: ""
                val alistResponse = json.decodeFromString<AlistResponse>(responseBody)
                
                if (alistResponse.code == 200) {
                    alistResponse.data?.content ?: emptyList()
                } else {
                    Log.w("ONETV_MOVIE", "⚠️ Alist搜索响应错误: ${alistResponse.message}")
                    emptyList()
                }
            }
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ Alist搜索失败", e)
            emptyList()
        }
    }
    
    /**
     * 转换文件为VodItem
     */
    private fun convertFilesToVodItems(files: List<AlistFile>, category: String): List<VodItem> {
        return files.filter { isVideoFile(it.name) }.map { file ->
            VodItem(
                vodId = file.name.hashCode().toString(),
                vodName = getVideoTitle(file.name),
                vodPic = file.thumb.ifEmpty { "" },
                vodRemarks = formatFileSize(file.size),
                typeId = "1",
                typeName = category,
                vodPlayUrl = file.name // 这里存储文件路径，后续需要转换为播放地址
            )
        }
    }
    
    /**
     * 判断是否为视频文件
     */
    private fun isVideoFile(fileName: String): Boolean {
        val videoExtensions = listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp", "ts", "m3u8")
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return videoExtensions.contains(extension)
    }
    
    /**
     * 获取视频标题
     */
    private fun getVideoTitle(fileName: String): String {
        return fileName.substringBeforeLast('.')
    }
    
    /**
     * 格式化文件大小
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
     * 获取动态分类路径
     */
    private suspend fun getDynamicCategoryPath(site: VodSite, typeId: String): String {
        return try {
            val index = typeId.toInt() - 100
            val files = getFileList(site, "/")
            val folders = files.filter { it.is_dir }
            if (index < folders.size) {
                "/${folders[index].name}"
            } else {
                "/"
            }
        } catch (e: Exception) {
            "/"
        }
    }
    

}
