package top.cywin.onetv.movie.data.parser

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import top.cywin.onetv.movie.data.models.VodConfigResponse
import top.cywin.onetv.movie.data.network.TvboxNetworkConfig
import java.util.concurrent.TimeUnit

/**
 * TVBOX配置解析器 - 按TVBOX标准客户端解析配置URL
 * 
 * 符合TVBOX设计原则：
 * 1. 用户配置接口URL
 * 2. 客户端解析URL
 * 3. 解析影视资源
 * 4. 更新UI界面
 */
class TvboxConfigParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // 使用TVBOX标准网络配置，解决连接问题
    private val httpClient = TvboxNetworkConfig.createTvboxHttpClient()

    /**
     * 按TVBOX标准解析配置URL - 流式解析，不下载文件
     *
     * @param configUrl 配置文件URL
     * @return 解析后的配置数据
     */
    suspend fun parseConfigUrl(configUrl: String): Result<VodConfigResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "🔄 TVBOX客户端开始流式解析配置URL")
            Log.d("ONETV_MOVIE", "🌐 配置URL: $configUrl")
            Log.d("ONETV_MOVIE", "📋 按TVBOX标准：直接解析URL，不下载文件")

            // 1. 客户端直接从URL流式解析JSON配置（不下载文件）
            val config = parseConfigFromStream(configUrl)
            Log.d("ONETV_MOVIE", "✅ JSON流式解析成功")
            Log.d("ONETV_MOVIE", "📊 解析结果: 站点=${config.sites.size}个, 解析器=${config.parses.size}个")

        // 🧪 详细调试信息
        if (config.sites.isNotEmpty()) {
            Log.d("ONETV_MOVIE", "🧪 第一个站点: ${config.sites.first().name} (${config.sites.first().key})")
        }
        if (config.parses.isNotEmpty()) {
            Log.d("ONETV_MOVIE", "🧪 第一个解析器: ${config.parses.first().name} (类型=${config.parses.first().type})")
            val firstParse = config.parses.first()
            Log.d("ONETV_MOVIE", "🧪 解析器ext字段: ${firstParse.ext}")
            Log.d("ONETV_MOVIE", "🧪 解析器flag字段: ${firstParse.getFlagList()}")
        }

            // 2. 检查是否为仓库索引文件
            if (config.sites.isEmpty() && config.urls.isNotEmpty()) {
                Log.d("ONETV_MOVIE", "🏪 检测到仓库索引文件，共${config.urls.size}个线路")

                // 按顺序尝试所有线路，直到找到可用的配置
                for ((index, urlConfig) in config.urls.withIndex()) {
                    Log.d("ONETV_MOVIE", "🔗 尝试线路${index + 1}/${config.urls.size}: ${urlConfig.name}")
                    Log.d("ONETV_MOVIE", "🌐 线路URL: ${urlConfig.url}")

                    try {
                        // 递归解析仓库中的配置
                        val result = parseConfigUrl(urlConfig.url)
                        if (result.isSuccess) {
                            val parsedConfig = result.getOrNull()
                            if (parsedConfig != null && parsedConfig.sites.isNotEmpty()) {
                                Log.d("ONETV_MOVIE", "✅ 线路${index + 1}解析成功: 站点=${parsedConfig.sites.size}个")
                                return@withContext result
                            }
                        }
                        Log.w("ONETV_MOVIE", "⚠️ 线路${index + 1}解析失败或无有效站点")
                    } catch (e: Exception) {
                        Log.w("ONETV_MOVIE", "⚠️ 线路${index + 1}解析异常: ${e.message}")
                    }
                }

                Log.e("ONETV_MOVIE", "💥 所有线路解析失败")
                return@withContext Result.failure(Exception("所有线路解析失败"))
            }

            // 3. 验证配置有效性
            if (config.sites.isEmpty()) {
                Log.w("ONETV_MOVIE", "⚠️ 配置中没有可用站点")
                return@withContext Result.failure(Exception("配置中没有可用站点"))
            }

            // 4. 输出站点信息用于调试
            config.sites.take(5).forEachIndexed { index, site ->
                Log.d("ONETV_MOVIE", "📺 站点${index + 1}: ${site.name} (${site.key}) - ${site.api}")
            }

            Log.d("ONETV_MOVIE", "🎉 TVBOX配置流式解析完成")
            Result.success(config)

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "💥 TVBOX配置解析失败", e)
            Result.failure(e)
        }
    }

    /**
     * 从URL流式解析配置 - 按TVBOX标准，不下载文件，支持重试
     */
    private suspend fun parseConfigFromStream(url: String): VodConfigResponse = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        // 重试3次，模仿OneMoVie的重试机制
        repeat(3) { attempt ->
            try {
                Log.d("ONETV_MOVIE", "🔄 尝试连接 (第${attempt + 1}次): $url")

                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .addHeader("Cache-Control", "no-cache")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}: ${response.message}")
                }

                // 直接从响应流解析JSON，不存储到内存
                response.body?.use { responseBody ->
                    val inputStream = responseBody.byteStream()
                    val jsonString = inputStream.bufferedReader().use { it.readText() }

                    Log.d("ONETV_MOVIE", "✅ 网络请求成功，JSON大小: ${jsonString.length} 字符")

                    // 解析JSON但不存储文件
                    return@withContext json.decodeFromString<VodConfigResponse>(jsonString)
                } ?: throw Exception("响应内容为空")

            } catch (e: Exception) {
                lastException = e
                Log.w("ONETV_MOVIE", "⚠️ 第${attempt + 1}次尝试失败: ${e.message}")

                if (attempt < 2) {
                    // 等待后重试
                    delay(1000L * (attempt + 1))
                }
            }
        }

        throw lastException ?: Exception("网络连接失败")
    }

    companion object {
        /**
         * 创建解析器实例
         */
        fun create(): TvboxConfigParser {
            return TvboxConfigParser()
        }
    }
}
