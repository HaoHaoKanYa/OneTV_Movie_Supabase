package top.cywin.onetv.movie.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import top.cywin.onetv.movie.data.VodConfigManager
import top.cywin.onetv.movie.data.api.VodApiService
import top.cywin.onetv.movie.data.cache.MovieCacheManager
import top.cywin.onetv.movie.data.cache.VodCacheManager
import top.cywin.onetv.movie.data.config.AppConfigManager
import top.cywin.onetv.movie.data.models.*
import top.cywin.onetv.movie.data.parser.ParseManager
import top.cywin.onetv.movie.data.parser.ParseResult
import top.cywin.onetv.movie.data.parser.TvboxConfigParser
import top.cywin.onetv.movie.data.network.TvboxNetworkConfig
// KotlinPoet专业重构 - 移除Hilt相关import
// import top.cywin.onetv.movie.di.SiteApiService
// import javax.inject.Inject
// import javax.inject.Singleton
import android.util.Log

/**
 * 点播数据仓库 (参考OneMoVie Repository模式)
 * KotlinPoet专业重构 - 移除Hilt依赖，使用标准构造函数
 */
// @Singleton
class VodRepository(
    private val context: Context,
    private val appConfigManager: AppConfigManager,
    private val cacheManager: MovieCacheManager,
    private val vodCacheManager: VodCacheManager,
    private val configManager: VodConfigManager,
    private val parseManager: ParseManager,
    private val apiService: VodApiService, // 配置API服务
    private val siteApiService: VodApiService, // 站点API服务
    private val tvboxParser: TvboxConfigParser = TvboxConfigParser.create() // TVBOX配置解析器
) {

    // 各种站点处理器 - 延迟初始化
    private lateinit var spiderProcessor: top.cywin.onetv.movie.data.spider.SpiderProcessor
    private lateinit var appProcessor: top.cywin.onetv.movie.data.app.AppSiteProcessor
    private lateinit var alistProcessor: top.cywin.onetv.movie.data.alist.AlistProcessor

    /**
     * 获取配置文件并初始化站点
     */
    suspend fun loadConfig(): Result<VodConfigResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "🔄 开始加载点播配置 - Edge Function + 缓存策略")

            // 0. 确保AppConfigManager已初始化
            ensureAppConfigInitialized()

            // 1. TVBOX标准：强制清除缓存，重新检测仓库索引
            Log.d("ONETV_MOVIE", "🗑️ 强制清除缓存以检测仓库索引")
            vodCacheManager.clearAll()
            configManager.clear()

            Log.d("ONETV_MOVIE", "📦 重新解析配置文件以检测仓库索引")

            // 2. 缓存未命中，根据优先级获取配置
            Log.d("ONETV_MOVIE", "🌐 缓存未命中，根据优先级获取配置")
            val vodConfigResult = loadConfigByPriority()

            if (vodConfigResult.isSuccess) {
                val vodConfig = vodConfigResult.getOrNull()!!
                Log.d("ONETV_MOVIE", "✅ Edge Function获取配置成功")
                Log.d("ONETV_MOVIE", "📊 解析配置成功: 站点数=${vodConfig.sites.size}, 解析器数=${vodConfig.parses.size}")

                // 3. 保存到缓存 (24小时有效期)
                Log.d("ONETV_MOVIE", "💾 保存配置到缓存")
                val vodConfigForCache = convertToVodConfig(vodConfig)
                vodCacheManager.saveConfig(vodConfigForCache)

                // 4. 加载到配置管理器
                val loadResult = configManager.load(vodConfig)
                if (loadResult.isSuccess) {
                    Log.d("ONETV_MOVIE", "🎉 内置源配置加载成功")
                    return@withContext Result.success(vodConfig)
                } else {
                    Log.e("ONETV_MOVIE", "❌ 配置加载到管理器失败")
                }
            } else {
                Log.w("ONETV_MOVIE", "⚠️ Edge Function获取配置失败: ${vodConfigResult.exceptionOrNull()?.message}")
            }

            // 5. Edge Function失败，尝试从配置管理器获取已有配置
            val existingConfig = configManager.getCurrentConfig()
            if (existingConfig != null) {
                Log.d("ONETV_MOVIE", "📋 使用配置管理器中的现有配置")
                return@withContext Result.success(existingConfig)
            }

            // 6. 最后使用默认配置
            Log.w("ONETV_MOVIE", "🔧 使用默认配置作为后备方案")
            val defaultConfigResponse = createDefaultConfigResponse()

            // 7. 加载到配置管理器
            val loadResult = configManager.load(defaultConfigResponse)
            if (loadResult.isSuccess) {
                Log.d("ONETV_MOVIE", "✅ 默认配置加载成功")
                Result.success(defaultConfigResponse)
            } else {
                Log.e("ONETV_MOVIE", "❌ 默认配置加载失败")
                Result.failure(Exception("所有配置加载方式都失败"))
            }

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "💥 配置加载异常", e)
            // 即使异常也尝试提供默认配置
            try {
                val defaultConfigResponse = createDefaultConfigResponse()
                Result.success(defaultConfigResponse)
            } catch (ex: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 确保AppConfigManager已初始化
     */
    private suspend fun ensureAppConfigInitialized() = withContext(Dispatchers.IO) {
        try {
            if (!appConfigManager.isConfigInitialized()) {
                Log.d("ONETV_MOVIE", "🔧 AppConfigManager未初始化，开始初始化...")
                val result = appConfigManager.initializeConfig()
                if (result.isSuccess) {
                    Log.d("ONETV_MOVIE", "✅ AppConfigManager初始化成功")
                } else {
                    Log.w("ONETV_MOVIE", "⚠️ AppConfigManager初始化失败: ${result.exceptionOrNull()?.message}")
                }
            } else {
                Log.d("ONETV_MOVIE", "✅ AppConfigManager已初始化")
            }
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ AppConfigManager初始化异常", e)
        }
    }

    /**
     * 按优先级加载配置
     * 优先级: 1. 用户自定义源 2. 内置源(Edge Function)
     */
    private suspend fun loadConfigByPriority(): Result<VodConfigResponse> = withContext(Dispatchers.IO) {
        try {
            // 1. 检查是否有用户自定义源
            val customConfigUrl = getUserCustomConfigUrl()
            if (!customConfigUrl.isNullOrBlank()) {
                Log.d("ONETV_MOVIE", "🎯 优先级1: 使用用户自定义源: $customConfigUrl")
                val customResult = loadVodConfigFromCustomUrl(customConfigUrl)
                if (customResult.isSuccess) {
                    Log.d("ONETV_MOVIE", "✅ 用户自定义源加载成功")
                    return@withContext customResult
                } else {
                    Log.w("ONETV_MOVIE", "⚠️ 用户自定义源加载失败，降级到内置源")
                }
            }

            // 2. 使用内置源 (Edge Function)
            Log.d("ONETV_MOVIE", "🏠 优先级2: 使用内置源(Edge Function)")
            return@withContext loadVodConfigFromEdgeFunction()

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "💥 按优先级加载配置失败", e)
            Result.failure(e)
        }
    }

    /**
     * 获取用户自定义配置URL
     */
    private suspend fun getUserCustomConfigUrl(): String? = withContext(Dispatchers.IO) {
        try {
            // 从SharedPreferences或数据库获取用户设置的自定义源URL
            val preferences = context.getSharedPreferences("movie_settings", Context.MODE_PRIVATE)
            val customUrl = preferences.getString("custom_vod_config_url", null)

            if (!customUrl.isNullOrBlank()) {
                Log.d("ONETV_MOVIE", "📋 发现用户自定义源: ${customUrl.take(50)}...")
            } else {
                Log.d("ONETV_MOVIE", "📋 未设置用户自定义源，使用内置源")
            }

            return@withContext customUrl
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 获取用户自定义源失败", e)
            null
        }
    }

    /**
     * 从用户自定义URL加载配置
     */
    private suspend fun loadVodConfigFromCustomUrl(url: String): Result<VodConfigResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "🔗 从用户自定义URL获取配置: $url")

            // 使用OkHttp直接请求用户提供的URL
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "OneTV/2.1.1")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }

            val configJson = response.body?.string() ?: throw Exception("响应体为空")
            Log.d("ONETV_MOVIE", "📄 自定义源配置下载成功，大小: ${configJson.length} 字符")

            // 解析JSON配置
            val vodConfig = Json.decodeFromString<VodConfigResponse>(configJson)

            Log.d("ONETV_MOVIE", "✅ 自定义源JSON解析成功")
            Log.d("ONETV_MOVIE", "📊 自定义源配置统计: 站点=${vodConfig.sites.size}个, 解析器=${vodConfig.parses.size}个")

            // 验证配置有效性
            if (vodConfig.sites.isEmpty()) {
                throw Exception("自定义源配置中没有可用站点")
            }

            Result.success(vodConfig)

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "💥 从自定义URL加载配置失败", e)
            Result.failure(e)
        }
    }

    /**
     * 通过Edge Function获取VOD配置 (内置源)
     */
    private suspend fun loadVodConfigFromEdgeFunction(): Result<VodConfigResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "🔗 通过Edge Function获取TVBOX配置链接")

            // 记录请求详情
            val supabaseUrl = try {
                appConfigManager.getSupabaseUrl()
            } catch (e: Exception) {
                "未知URL"
            }
            Log.d("ONETV_MOVIE", "📡 请求URL: $supabaseUrl/functions/v1/vod-config")

            // 调用Edge Function获取配置文件链接
            val linkResponse = apiService.getConfigLink()

            val configUrl = linkResponse.config_url
            if (configUrl.isBlank()) {
                Log.e("ONETV_MOVIE", "❌ Edge Function返回空链接: ${linkResponse.message}")
                return@withContext Result.failure(Exception("获取配置链接失败: ${linkResponse.message}"))
            }

            Log.d("ONETV_MOVIE", "✅ 获取TVBOX配置链接成功: $configUrl")

            // 按TVBOX标准：返回配置文件URL供前端解析（不下载配置文件）
            Log.d("ONETV_MOVIE", "📋 返回TVBOX配置文件URL供前端解析...")

            // 创建包含配置URL的VodConfigResponse，前端将直接从URL加载影视接口
            val vodConfig = VodConfigResponse(
                sites = emptyList(), // 前端将从URL加载
                parses = emptyList(), // 前端将从URL加载
                urls = listOf(
                    VodConfigUrl(
                        name = "OneTV主配置",
                        url = configUrl
                    )
                ),
                spider = "",
                wallpaper = "",
                logo = "",
                lives = emptyList(),
                doh = emptyList(),
                flags = emptyList(),
                ijk = emptyList(),
                ads = emptyList(),
                rules = emptyList()
            )

            Log.d("ONETV_MOVIE", "✅ 配置URL准备完成，供前端解析")
            Log.d("ONETV_MOVIE", "🔗 配置URL: $configUrl")
            Log.d("ONETV_MOVIE", "� 前端将直接从URL加载影视接口，无需后端下载配置文件")

            // 按TVBOX标准：客户端解析配置URL（完全符合TVBOX逻辑）
            Log.d("ONETV_MOVIE", "🔄 使用TVBOX解析器解析配置URL...")

            // 使用TVBOX解析器解析配置URL
            val parseResult = tvboxParser.parseConfigUrl(configUrl)
            if (parseResult.isFailure) {
                Log.e("ONETV_MOVIE", "� TVBOX配置解析失败", parseResult.exceptionOrNull())
                return@withContext parseResult
            }

            val actualConfig = parseResult.getOrThrow()
            Log.d("ONETV_MOVIE", "🎉 TVBOX配置解析成功")
            Log.d("ONETV_MOVIE", "📊 解析结果: 站点=${actualConfig.sites.size}个, 解析器=${actualConfig.parses.size}个")

            Result.success(actualConfig)

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "💥 Edge Function获取配置失败", e)
            Result.failure(e)
        }
    }

    /**
     * 解析配置文件URL获取TVBOX配置
     * 使用正确的认证方式访问私密存储桶
     *
     * ⚠️ 已废弃：根据用户要求，客户端不应下载配置文件，应直接返回URL供前端解析
     */
    @Deprecated("客户端不应下载配置文件，应直接返回URL供前端解析")
    private suspend fun parseConfigFromUrl(configUrl: String): VodConfigResponse = withContext(Dispatchers.IO) {
        try {
            // 判断是否为私密存储桶URL
            val isPrivateStorage = configUrl.contains("supabase.co/storage/v1/object/sign/")

            // 创建HTTP客户端
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS) // 连接超时15秒
                .readTimeout(30, TimeUnit.SECONDS) // 读取超时30秒
                .writeTimeout(15, TimeUnit.SECONDS) // 写入超时15秒
                .build()

            val request = if (isPrivateStorage) {
                Log.d("ONETV_MOVIE", "🔐 使用Service Role Key访问私密存储桶配置文件")

                // 确保AppConfigManager已初始化
                ensureAppConfigInitialized()

                // 获取Service Role Key
                val serviceRoleKey = appConfigManager.getServiceRoleKey()
                Log.d("ONETV_MOVIE", "🔑 Service Role Key: ${serviceRoleKey.take(20)}...")

                Request.Builder()
                    .url(configUrl)
                    .addHeader("Authorization", "Bearer $serviceRoleKey")
                    .addHeader("apikey", serviceRoleKey)
                    .addHeader("User-Agent", "OneTV/2.1.1")
                    .addHeader("Accept", "application/json")
                    .build()
            } else {
                Log.d("ONETV_MOVIE", "🌐 访问公开配置文件（无需认证）")

                Request.Builder()
                    .url(configUrl)
                    .addHeader("User-Agent", "OneTV/2.1.1")
                    .addHeader("Accept", "application/json")
                    .build()
            }

            Log.d("ONETV_MOVIE", "📡 请求配置文件: $configUrl")

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "无响应内容"
                Log.e("ONETV_MOVIE", "❌ 配置文件访问失败: ${response.code} - $errorBody")
                throw Exception("配置文件访问失败: ${response.code} - $errorBody")
            }

            val configJson = response.body?.string() ?: throw Exception("配置文件内容为空")
            Log.d("ONETV_MOVIE", "✅ 配置文件读取成功，大小: ${configJson.length} 字符")

            // 解析JSON配置 - 支持TVBOX标准字段（包括logo等扩展字段）
            val vodConfig = Json.decodeFromString<VodConfigResponse>(configJson)
            Log.d("ONETV_MOVIE", "✅ TVBOX配置JSON解析成功")

            vodConfig

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "💥 解析配置文件失败", e)
            throw e
        }
    }

    /**
     * 转换VodConfigResponse为VodConfig用于缓存
     */
    private fun convertToVodConfig(response: VodConfigResponse): VodConfig {
        return VodConfig(
            id = 0,
            type = 0,
            url = "", // 仓库URL
            name = "TVBOX配置",
            logo = response.logo,
            home = response.sites.firstOrNull()?.key ?: "",
            parse = response.parses.firstOrNull()?.name ?: "",
            json = "", // 原始JSON
            time = System.currentTimeMillis(),
            sites = response.sites,
            parses = response.parses,
            spider = response.spider,
            wallpaper = response.wallpaper,
            notice = response.notice
        )
    }

    /**
     * 强制刷新配置 (清除缓存，重新从网络获取)
     */
    suspend fun refreshConfig(): Result<VodConfigResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "🔄 强制刷新配置，清除缓存")

            // 1. 清除缓存
            vodCacheManager.clearAll()

            // 2. 重新加载配置
            return@withContext loadConfig()

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "💥 配置刷新失败", e)
            Result.failure(e)
        }
    }

    /**
     * 检查配置是否需要更新 (检查缓存是否过期)
     */
    suspend fun isConfigUpdateNeeded(): Boolean = withContext(Dispatchers.IO) {
        try {
            val cachedConfig = vodCacheManager.getConfig()
            // 需要更新的条件：缓存为空 或 站点数为0
            val needUpdate = cachedConfig == null || cachedConfig.sites.isEmpty()

            Log.d("ONETV_MOVIE", "🔍 检查配置更新需求: ${if (needUpdate) "需要更新" else "缓存有效"}")
            if (cachedConfig != null) {
                Log.d("ONETV_MOVIE", "📊 当前缓存站点数: ${cachedConfig.sites.size}")
            }
            return@withContext needUpdate

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 检查配置更新失败", e)
            true // 出错时默认需要更新
        }
    }

    /**
     * 设置用户自定义配置源
     */
    suspend fun setCustomConfigUrl(url: String?): Result<String> = withContext(Dispatchers.IO) {
        try {
            val preferences = context.getSharedPreferences("movie_settings", Context.MODE_PRIVATE)

            if (url.isNullOrBlank()) {
                // 清除自定义源，使用内置源
                preferences.edit().remove("custom_vod_config_url").apply()
                Log.d("ONETV_MOVIE", "🏠 已清除自定义源，将使用内置源")
                Result.success("已切换到内置源")
            } else {
                // 验证自定义源是否有效
                val testResult = loadVodConfigFromCustomUrl(url)
                if (testResult.isSuccess) {
                    preferences.edit().putString("custom_vod_config_url", url).apply()
                    Log.d("ONETV_MOVIE", "✅ 自定义源设置成功: $url")

                    // 清除缓存，强制重新加载
                    vodCacheManager.clearAll()

                    Result.success("自定义源设置成功")
                } else {
                    Log.e("ONETV_MOVIE", "❌ 自定义源验证失败: ${testResult.exceptionOrNull()?.message}")
                    Result.failure(testResult.exceptionOrNull() ?: Exception("自定义源验证失败"))
                }
            }
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "💥 设置自定义源失败", e)
            Result.failure(e)
        }
    }

    /**
     * 获取当前配置源信息
     */
    suspend fun getCurrentConfigSource(): String = withContext(Dispatchers.IO) {
        try {
            val customUrl = getUserCustomConfigUrl()
            if (!customUrl.isNullOrBlank()) {
                "用户自定义源: ${customUrl.take(50)}..."
            } else {
                "内置源 (Edge Function)"
            }
        } catch (e: Exception) {
            "未知源"
        }
    }

    /**
     * 清除配置缓存，强制重新解析（用于TVBOX仓库索引检测）
     */
    suspend fun clearConfigCache(): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "🗑️ 清除配置缓存，强制重新解析")
            vodCacheManager.clearAll()
            configManager.clear()
            Result.success("缓存清除成功")
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "清除缓存失败", e)
            Result.failure(e)
        }
    }

    /**
     * 将VodConfig转换为VodConfigResponse
     */
    private fun convertToVodConfigResponse(vodConfig: VodConfig): VodConfigResponse {
        return VodConfigResponse(
            spider = vodConfig.spider,
            wallpaper = vodConfig.wallpaper,
            sites = vodConfig.sites,
            parses = vodConfig.parses,
            flags = emptyList(), // VodConfig没有flags字段
            ijk = emptyList(), // VodConfig没有ijk字段
            ads = emptyList(), // VodConfig没有ads字段
            notice = "从缓存加载的配置"
        )
    }



    /**
     * 创建默认配置响应（临时解决方案）
     */
    private fun createDefaultConfigResponse(): VodConfigResponse {
        val defaultSite = VodSite(
            key = "default",
            name = "默认站点",
            api = "https://example.com/api.php/provide/vod/",
            ext = JsonPrimitive(""),
            jar = "",
            type = 1,
            searchable = 1,
            quickSearch = 1,
            filterable = 1,
            playerType = 1,
            changeable = 1,
            click = "",
            timeout = 15000, // 15秒超时
            header = null,
            style = null,
            categories = emptyList() // 不提供硬编码分类
        )

        val defaultParse = VodParse(
            name = "默认解析",
            type = 1,
            url = "https://example.com/parse?url=",
            ext = emptyMap(),
            header = emptyMap()
        )

        return VodConfigResponse(
            spider = "",
            wallpaper = "",
            sites = listOf(defaultSite),
            parses = listOf(defaultParse),
            flags = listOf("qiyi", "qq", "youku", "mgtv"),
            ijk = emptyList(),
            ads = emptyList(),
            notice = "这是临时默认配置，请配置正确的服务器信息"
        )
    }

    /**
     * 获取站点分类
     */
    suspend fun getCategories(siteKey: String): Result<List<VodClass>> = withContext(Dispatchers.IO) {
        try {
            val site = configManager.getSite(siteKey)
                ?: return@withContext Result.failure(Exception("未找到站点: $siteKey"))

            // 检查缓存
            val cacheKey = "categories_$siteKey"
            val cached = cacheManager.getCache(cacheKey, Array<VodClass>::class.java)?.toList()
            if (!cached.isNullOrEmpty()) {
                return@withContext Result.success(cached)
            }

            // TVBOX智能容错处理：尝试多种方式直到成功
            val categories = getCategoriesWithFallback(site)

            // 缓存结果
            cacheManager.putCache(cacheKey, categories.toTypedArray(), 24 * 60 * 60 * 1000L)

            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取Spider站点分类
     */
    private suspend fun getSpiderCategories(site: VodSite): List<VodClass> {
        return try {
            Log.d("ONETV_MOVIE", "🕷️ 获取Spider站点分类: ${site.name}")

            // 初始化Spider处理器（如果尚未初始化）
            if (!::spiderProcessor.isInitialized) {
                spiderProcessor = top.cywin.onetv.movie.data.spider.SpiderProcessor(context)
                val initResult = spiderProcessor.initialize()
                if (!initResult) {
                    Log.w("ONETV_MOVIE", "⚠️ Spider处理器初始化失败，返回空分类")
                    return emptyList()
                }
            }

            // 使用Spider处理器获取分类
            spiderProcessor.getCategories(site)

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ Spider站点分类获取失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 获取CMS站点分类
     */
    private suspend fun getCmsCategories(site: VodSite): List<VodClass> {
        return try {
            // 检查是否为真正的CMS站点
            if (!isTrueCmsSite(site)) {
                Log.d("ONETV_MOVIE", "⚠️ 站点 ${site.name} 不是真正的CMS站点，跳过CMS处理")
                return emptyList()
            }

            if (site.api.isBlank() || !site.api.startsWith("http")) {
                Log.w("ONETV_MOVIE", "⚠️ CMS站点API格式无效，返回空分类")
                return emptyList()
            }

            Log.d("ONETV_MOVIE", "🌐 调用CMS站点API获取分类...")

            // 构建获取分类的API URL
            val apiUrl = buildCmsApiUrl(site.api, "list")
            Log.d("ONETV_MOVIE", "🔗 分类API URL: $apiUrl")

            val response = siteApiService.getHomeContent(apiUrl)
            val categories = response.classes

            Log.d("ONETV_MOVIE", "✅ CMS站点分类获取成功: ${categories.size}个分类")
            categories.forEach { category ->
                Log.d("ONETV_MOVIE", "📂 分类: ${category.typeName} (${category.typeId})")
            }

            categories

        } catch (e: Exception) {
            Log.w("ONETV_MOVIE", "⚠️ CMS站点API调用失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 检查是否为真正的CMS站点
     */
    private fun isTrueCmsSite(site: VodSite): Boolean {
        val api = site.api.lowercase()

        // 如果是JavaScript文件，不是CMS站点
        if (api.endsWith(".js") || api.contains("drpy") || api.contains("hipy")) {
            return false
        }

        // 如果API包含csp_前缀，通常是Spider站点
        if (api.startsWith("csp_") || site.name.contains("csp_")) {
            return false
        }

        // 如果有JAR包，通常是Spider站点
        if (site.jar.isNotEmpty()) {
            return false
        }

        // 真正的CMS站点应该是HTTP API
        return api.startsWith("http") && (api.contains("/api/") || api.contains("?ac="))
    }

    /**
     * 获取APP站点分类
     */
    private suspend fun getAppCategories(site: VodSite): List<VodClass> {
        return try {
            Log.d("ONETV_MOVIE", "📱 获取APP站点分类: ${site.name}")

            // 初始化APP处理器（如果尚未初始化）
            if (!::appProcessor.isInitialized) {
                appProcessor = top.cywin.onetv.movie.data.app.AppSiteProcessor(context)
            }

            // 使用APP处理器获取分类
            appProcessor.getCategories(site)

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ APP站点分类获取失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 获取Alist站点分类
     */
    private suspend fun getAlistCategories(site: VodSite): List<VodClass> {
        return try {
            Log.d("ONETV_MOVIE", "💾 获取Alist站点分类: ${site.name}")

            // 初始化Alist处理器（如果尚未初始化）
            if (!::alistProcessor.isInitialized) {
                alistProcessor = top.cywin.onetv.movie.data.alist.AlistProcessor(context)
            }

            // 使用Alist处理器获取分类
            alistProcessor.getCategories(site)

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ Alist站点分类获取失败: ${e.message}")
            emptyList()
        }
    }



    /**
     * 获取内容列表
     */
    suspend fun getContentList(
        typeId: String,
        page: Int = 1,
        siteKey: String = "",
        filters: Map<String, String> = emptyMap()
    ): Result<VodResponse> = withContext(Dispatchers.IO) {
        try {
            val site = configManager.getSite(siteKey)
                ?: return@withContext Result.failure(Exception("未找到站点"))

            // 构建缓存键
            val filterString = filters.entries.joinToString("&") { "${it.key}=${it.value}" }
            val cacheKey = "content_${siteKey}_${typeId}_${page}_${filterString.hashCode()}"

            // 检查缓存
            val cached = cacheManager.getCache(cacheKey, VodResponse::class.java)
            if (cached != null) {
                return@withContext Result.success(cached)
            }

            // 根据站点类型获取内容
            val response = when {
                // Spider站点 - 使用Spider处理器
                site.isSpider() -> {
                    Log.d("ONETV_MOVIE", "🕷️ Spider站点获取分类内容")
                    val items = getSpiderCategoryContent(site, typeId, page)
                    VodResponse(
                        code = 1,
                        msg = "success",
                        page = page,
                        pagecount = if (items.isEmpty()) 0 else 1,
                        limit = 20,
                        total = items.size,
                        list = items
                    )
                }

                // CMS站点 - 使用标准API
                site.isCms() -> {
                    Log.d("ONETV_MOVIE", "🌐 CMS站点获取分类内容")
                    getCmsCategoryContent(site, typeId, page)
                }

                // APP站点 - 使用APP处理器
                site.isApp() -> {
                    Log.d("ONETV_MOVIE", "📱 APP站点获取分类内容")
                    val items = getAppCategoryContent(site, typeId, page)
                    VodResponse(
                        code = 1,
                        msg = "success",
                        page = page,
                        pagecount = if (items.isEmpty()) 0 else 1,
                        limit = 20,
                        total = items.size,
                        list = items
                    )
                }

                // Alist站点 - 使用Alist处理器
                site.isAlist() -> {
                    Log.d("ONETV_MOVIE", "💾 Alist站点获取分类内容")
                    val items = getAlistCategoryContent(site, typeId, page)
                    VodResponse(
                        code = 1,
                        msg = "success",
                        page = page,
                        pagecount = if (items.isEmpty()) 0 else 1,
                        limit = 20,
                        total = items.size,
                        list = items
                    )
                }

                else -> {
                    Log.w("ONETV_MOVIE", "⚠️ 未知站点类型，返回空结果")
                    VodResponse(
                        code = 1,
                        msg = "success",
                        page = page,
                        pagecount = 0,
                        limit = 20,
                        total = 0,
                        list = emptyList()
                    )
                }
            }

            // 缓存结果
            cacheManager.putCache(cacheKey, response, 24 * 60 * 60 * 1000L)

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取Spider站点分类内容
     */
    private suspend fun getSpiderCategoryContent(site: VodSite, typeId: String, page: Int): List<VodItem> {
        return try {
            // 初始化Spider处理器（如果尚未初始化）
            if (!::spiderProcessor.isInitialized) {
                spiderProcessor = top.cywin.onetv.movie.data.spider.SpiderProcessor(context)
                val initResult = spiderProcessor.initialize()
                if (!initResult) {
                    Log.w("ONETV_MOVIE", "⚠️ Spider处理器初始化失败")
                    return emptyList()
                }
            }

            spiderProcessor.getCategoryContent(site, typeId, page)
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ Spider站点分类内容获取失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 获取CMS站点分类内容
     */
    private suspend fun getCmsCategoryContent(site: VodSite, typeId: String, page: Int): VodResponse {
        return try {
            if (site.api.isBlank() || !site.api.startsWith("http")) {
                Log.w("ONETV_MOVIE", "⚠️ CMS站点API格式无效")
                return VodResponse(code = 1, msg = "success", page = page, pagecount = 0, limit = 20, total = 0, list = emptyList())
            }

            Log.d("ONETV_MOVIE", "🌐 调用CMS站点API获取分类内容...")
            val apiUrl = buildCmsApiUrl(site.api, "list", mapOf("t" to typeId, "pg" to page.toString()))
            siteApiService.getCategoryContent(url = apiUrl, typeId = typeId, page = page)
        } catch (e: Exception) {
            Log.w("ONETV_MOVIE", "⚠️ CMS站点API调用失败: ${e.message}")
            VodResponse(code = 1, msg = "success", page = page, pagecount = 0, limit = 20, total = 0, list = emptyList())
        }
    }

    /**
     * 获取APP站点分类内容
     */
    private suspend fun getAppCategoryContent(site: VodSite, typeId: String, page: Int): List<VodItem> {
        return try {
            // 初始化APP处理器（如果尚未初始化）
            if (!::appProcessor.isInitialized) {
                appProcessor = top.cywin.onetv.movie.data.app.AppSiteProcessor(context)
            }

            appProcessor.getCategoryContent(site, typeId, page)
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ APP站点分类内容获取失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 获取Alist站点分类内容
     */
    private suspend fun getAlistCategoryContent(site: VodSite, typeId: String, page: Int): List<VodItem> {
        return try {
            // 初始化Alist处理器（如果尚未初始化）
            if (!::alistProcessor.isInitialized) {
                alistProcessor = top.cywin.onetv.movie.data.alist.AlistProcessor(context)
            }

            alistProcessor.getCategoryContent(site, typeId, page)
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ Alist站点分类内容获取失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 搜索内容
     */
    suspend fun searchContent(
        keyword: String,
        page: Int = 1,
        siteKey: String = ""
    ): Result<VodResponse> = withContext(Dispatchers.IO) {
        try {
            val site = configManager.getSite(siteKey)
                ?: return@withContext Result.failure(Exception("未找到站点"))

            if (!site.isSearchable()) {
                return@withContext Result.failure(Exception("该站点不支持搜索"))
            }

            // 构建缓存键
            val cacheKey = "search_${siteKey}_${keyword}_$page"

            // 检查缓存
            val cached = cacheManager.getCache(cacheKey, VodResponse::class.java)
            if (cached != null) {
                return@withContext Result.success(cached)
            }

            // 从网络获取
            val response = siteApiService.searchContent(
                url = site.api,
                keyword = keyword,
                page = page
            )

            // 缓存结果
            cacheManager.putCache(cacheKey, response, 5 * 60 * 1000) // 5分钟

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取内容详情
     */
    suspend fun getContentDetail(
        vodId: String,
        siteKey: String = ""
    ): Result<VodItem> = withContext(Dispatchers.IO) {
        try {
            val site = configManager.getSite(siteKey)
                ?: return@withContext Result.failure(Exception("未找到站点"))

            // 构建缓存键
            val cacheKey = "detail_${siteKey}_$vodId"

            // 检查缓存
            val cached = cacheManager.getCache(cacheKey, VodItem::class.java)
            if (cached != null) {
                return@withContext Result.success(cached)
            }

            // 从网络获取
            val response = siteApiService.getContentDetail(
                url = site.api,
                ids = vodId
            )

            val item = response.list?.firstOrNull()
                ?: return@withContext Result.failure(Exception("未找到内容详情"))

            // 设置站点信息
            val itemWithSite = item.copy(siteKey = siteKey)

            // 缓存结果
            cacheManager.putCache(cacheKey, itemWithSite, 60 * 60 * 1000) // 1小时

            Result.success(itemWithSite)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取推荐内容 - 按TVBOX标准处理不同类型站点
     */
    suspend fun getRecommendContent(siteKey: String = ""): Result<List<VodItem>> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "🔍 开始获取推荐内容，站点: $siteKey")

            val site = configManager.getSite(siteKey)
                ?: return@withContext Result.failure(Exception("未找到站点"))

            Log.d("ONETV_MOVIE", "📺 站点信息: ${site.name}, 类型: ${site.getTypeDescription()}, API: ${site.api}")

            // 构建缓存键
            val cacheKey = "recommend_$siteKey"

            // 检查缓存
            val cached = cacheManager.getCache(cacheKey, Array<VodItem>::class.java)?.toList()
            if (cached != null) {
                Log.d("ONETV_MOVIE", "✅ 从缓存获取推荐内容: ${cached.size}部影片")
                return@withContext Result.success(cached)
            }

            // TVBOX智能容错处理：尝试多种方式直到成功
            val recommendList = getRecommendContentWithFallback(site)

            Log.d("ONETV_MOVIE", "✅ 推荐内容获取完成: ${recommendList.size}部影片")

            // 缓存结果
            if (recommendList.isNotEmpty()) {
                cacheManager.putCache(cacheKey, recommendList, 30 * 60 * 1000) // 30分钟
            }

            Result.success(recommendList)

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 推荐内容获取失败", e)
            Result.failure(e)
        }
    }

    /**
     * 处理Spider站点 - 使用Spider处理器
     */
    private suspend fun handleSpiderSite(site: VodSite): List<VodItem> {
        return try {
            Log.d("ONETV_MOVIE", "🕷️ 处理Spider站点: ${site.name}")

            // 初始化Spider处理器（如果尚未初始化）
            if (!::spiderProcessor.isInitialized) {
                spiderProcessor = top.cywin.onetv.movie.data.spider.SpiderProcessor(context)
                val initResult = spiderProcessor.initialize()
                if (!initResult) {
                    Log.w("ONETV_MOVIE", "⚠️ Spider处理器初始化失败")
                    return emptyList()
                }
            }

            // 使用Spider处理器获取首页内容
            spiderProcessor.getHomeContent(site)

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ Spider站点处理失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 处理CMS站点 - 标准TVBOX API
     */
    private suspend fun handleCmsSite(site: VodSite): List<VodItem> {
        return try {
            if (site.api.isBlank() || !site.api.startsWith("http")) {
                Log.w("ONETV_MOVIE", "⚠️ CMS站点API格式无效: ${site.api}")
                return emptyList()
            }

            Log.d("ONETV_MOVIE", "🌐 调用CMS站点API: ${site.api}")

            // 构建符合TVBOX标准的API请求
            val apiUrl = buildCmsApiUrl(site.api, "list")
            Log.d("ONETV_MOVIE", "🔗 构建的API URL: $apiUrl")

            val response = siteApiService.getHomeContent(apiUrl)
            val items = response.list?.take(20) ?: emptyList()

            Log.d("ONETV_MOVIE", "✅ CMS站点API调用成功，获得${items.size}个项目")
            items

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ CMS站点API调用失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 构建CMS API URL - 符合TVBOX标准
     */
    private fun buildCmsApiUrl(baseApi: String, action: String, params: Map<String, String> = emptyMap()): String {
        return try {
            val url = StringBuilder(baseApi)

            // 确保API以正确的格式结尾
            if (!baseApi.endsWith("/")) {
                url.append("/")
            }

            // 添加标准参数
            val queryParams = mutableMapOf<String, String>()
            queryParams["ac"] = action

            // 添加额外参数
            queryParams.putAll(params)

            // 构建查询字符串
            if (queryParams.isNotEmpty()) {
                if (!baseApi.contains("?")) {
                    url.append("?")
                } else {
                    url.append("&")
                }

                queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }.let {
                    url.append(it)
                }
            }

            url.toString()

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 构建CMS API URL失败", e)
            baseApi
        }
    }

    /**
     * 处理APP站点 - 使用APP站点处理器
     */
    private suspend fun handleAppSite(site: VodSite): List<VodItem> {
        return try {
            Log.d("ONETV_MOVIE", "📱 处理APP站点: ${site.name}")

            // 初始化APP处理器（如果尚未初始化）
            if (!::appProcessor.isInitialized) {
                appProcessor = top.cywin.onetv.movie.data.app.AppSiteProcessor(context)
            }

            // 使用APP处理器获取首页内容
            appProcessor.getHomeContent(site)

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ APP站点处理失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 处理Alist站点 - 使用Alist处理器
     */
    private suspend fun handleAlistSite(site: VodSite): List<VodItem> {
        return try {
            Log.d("ONETV_MOVIE", "💾 处理Alist站点: ${site.name}")

            // 初始化Alist处理器（如果尚未初始化）
            if (!::alistProcessor.isInitialized) {
                alistProcessor = top.cywin.onetv.movie.data.alist.AlistProcessor(context)
            }

            // 使用Alist处理器获取首页内容
            alistProcessor.getHomeContent(site)

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ Alist站点处理失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 清除缓存
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cacheManager.clearAllCache()
    }

    /**
     * 解析线路配置
     */
    suspend fun parseRouteConfig(routeUrl: String): Result<VodConfigResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "🔗 开始解析线路配置: $routeUrl")

            // 使用TVBOX解析器解析线路配置
            val parseResult = tvboxParser.parseConfigUrl(routeUrl)
            if (parseResult.isFailure) {
                Log.e("ONETV_MOVIE", "线路配置解析失败", parseResult.exceptionOrNull())
                return@withContext parseResult
            }

            val config = parseResult.getOrThrow()
            Log.d("ONETV_MOVIE", "✅ 线路配置解析成功: 站点=${config.sites.size}个, 解析器=${config.parses.size}个")

            // 缓存配置
            val vodConfig = convertToVodConfig(config)
            vodCacheManager.saveConfig(vodConfig)

            Result.success(config)

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "线路配置解析异常", e)
            Result.failure(e)
        }
    }

    /**
     * 解析播放地址 (集成解析器系统)
     */
    suspend fun parsePlayUrl(
        url: String,
        siteKey: String,
        flag: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val site = configManager.getSite(siteKey)
                ?: return@withContext Result.failure(Exception("未找到站点"))

            // 获取解析器配置
            val parse = configManager.getParseByFlag(flag)

            // 使用解析器管理器解析
            val parseResult = parseManager.parsePlayUrl(url, parse, flag)

            if (parseResult.isNotEmpty()) {
                Result.success(parseResult)
            } else {
                Result.failure(Exception("解析失败"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取解析结果详情 (包含headers等信息)
     */
    suspend fun parsePlayUrlDetailed(
        url: String,
        siteKey: String,
        flag: String = ""
    ): Result<ParseResult> = withContext(Dispatchers.IO) {
        try {
            val site = configManager.getSite(siteKey)
                ?: return@withContext Result.failure(Exception("未找到站点"))

            val parse = configManager.getParseByFlag(flag)
            val playUrl = parseManager.parsePlayUrl(url, parse, flag)

            val parseResult = ParseResult(
                success = playUrl.isNotEmpty(),
                playUrl = playUrl,
                error = if (playUrl.isEmpty()) "解析失败" else null
            )
            Result.success(parseResult)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取可用解析器列表
     */
    suspend fun getAvailableParsers(siteKey: String): List<VodParse> = withContext(Dispatchers.IO) {
        try {
            val config = configManager.getCurrentConfig()
            config?.parses ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取站点支持的解析器
     */
    suspend fun getSiteParsers(siteKey: String): List<VodParse> = withContext(Dispatchers.IO) {
        try {
            val site = configManager.getSite(siteKey)
            val config = configManager.getCurrentConfig()

            if (site != null && config != null) {
                // 根据站点配置筛选适合的解析器
                config.parses.filter { parse ->
                    // 这里可以添加更复杂的匹配逻辑
                    parse.type in listOf(0, 1, 2) // 支持的解析器类型
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 测试解析器可用性
     */
    suspend fun testParser(parse: VodParse, testUrl: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val playUrl = parseManager.parsePlayUrl(testUrl, parse, "")
            Result.success(playUrl.isNotEmpty())
        } catch (e: Exception) {
            Result.success(false)
        }
    }

    /**
     * 获取缓存大小
     */
    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        cacheManager.getCacheSize()
    }

    // ==================== TVBOX智能容错处理机制 ====================

    /**
     * TVBOX智能容错处理：获取分类时尝试多种方式直到成功
     */
    private suspend fun getCategoriesWithFallback(site: VodSite): List<VodClass> {
        Log.d("ONETV_MOVIE", "🧠 TVBOX智能容错: 开始获取站点分类 - ${site.name}")

        // 方式1：根据智能识别的类型处理
        val primaryResult = tryGetCategoriesByIntelligentType(site)
        if (primaryResult.isNotEmpty()) {
            Log.d("ONETV_MOVIE", "✅ 智能识别成功: ${primaryResult.size}个分类")
            return primaryResult
        }

        // 方式2：如果智能识别失败，尝试所有可能的处理器
        Log.d("ONETV_MOVIE", "🔄 智能识别失败，尝试所有处理器...")

        val fallbackResults = listOf(
            suspend { getSpiderCategories(site) },
            suspend { getCmsCategories(site) },
            suspend { getAppCategories(site) },
            suspend { getAlistCategories(site) }
        )

        for ((index, processor) in fallbackResults.withIndex()) {
            try {
                val result = processor()
                if (result.isNotEmpty()) {
                    val processorName = when(index) {
                        0 -> "Spider"
                        1 -> "CMS"
                        2 -> "APP"
                        3 -> "Alist"
                        else -> "Unknown"
                    }
                    Log.d("ONETV_MOVIE", "✅ TVBOX容错成功: $processorName 处理器获得${result.size}个分类")
                    return result
                }
            } catch (e: Exception) {
                Log.d("ONETV_MOVIE", "⚠️ 处理器${index + 1}失败: ${e.message}")
            }
        }

        // 方式3：所有处理器都失败，返回空分类
        Log.d("ONETV_MOVIE", "🔧 所有处理器都失败，返回空分类（不显示硬编码分类）")
        return emptyList()
    }

    /**
     * 根据智能识别的类型获取分类
     */
    private suspend fun tryGetCategoriesByIntelligentType(site: VodSite): List<VodClass> {
        return try {
            when {
                site.isSpider() -> {
                    Log.d("ONETV_MOVIE", "🕷️ 智能识别: Spider站点")
                    getSpiderCategories(site)
                }
                site.isCms() -> {
                    Log.d("ONETV_MOVIE", "🌐 智能识别: CMS站点")
                    getCmsCategories(site)
                }
                site.isApp() -> {
                    Log.d("ONETV_MOVIE", "📱 智能识别: APP站点")
                    getAppCategories(site)
                }
                site.isAlist() -> {
                    Log.d("ONETV_MOVIE", "💾 智能识别: Alist站点")
                    getAlistCategories(site)
                }
                else -> {
                    Log.w("ONETV_MOVIE", "⚠️ 未知站点类型: ${site.type}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.w("ONETV_MOVIE", "⚠️ 智能识别处理失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * TVBOX智能容错处理：获取推荐内容时尝试多种方式
     */
    private suspend fun getRecommendContentWithFallback(site: VodSite): List<VodItem> {
        Log.d("ONETV_MOVIE", "🧠 TVBOX智能容错: 开始获取推荐内容 - ${site.name}")

        // 方式1：根据智能识别的类型处理
        val primaryResult = tryGetRecommendByIntelligentType(site)
        if (primaryResult.isNotEmpty()) {
            Log.d("ONETV_MOVIE", "✅ 智能识别成功: ${primaryResult.size}部影片")
            return primaryResult
        }

        // 方式2：如果智能识别失败，尝试所有可能的处理器
        Log.d("ONETV_MOVIE", "🔄 智能识别失败，尝试所有处理器...")

        val fallbackResults = listOf(
            suspend { handleSpiderSite(site) },
            suspend { handleCmsSite(site) },
            suspend { handleAppSite(site) },
            suspend { handleAlistSite(site) }
        )

        for ((index, processor) in fallbackResults.withIndex()) {
            try {
                val result = processor()
                if (result.isNotEmpty()) {
                    val processorName = when(index) {
                        0 -> "Spider"
                        1 -> "CMS"
                        2 -> "APP"
                        3 -> "Alist"
                        else -> "Unknown"
                    }
                    Log.d("ONETV_MOVIE", "✅ TVBOX容错成功: $processorName 处理器获得${result.size}部影片")
                    return result
                }
            } catch (e: Exception) {
                Log.d("ONETV_MOVIE", "⚠️ 处理器${index + 1}失败: ${e.message}")
            }
        }

        // 方式3：返回空列表
        Log.d("ONETV_MOVIE", "🔧 所有处理器都失败，返回空列表")
        return emptyList()
    }

    /**
     * 根据智能识别的类型获取推荐内容
     */
    private suspend fun tryGetRecommendByIntelligentType(site: VodSite): List<VodItem> {
        return try {
            when {
                site.isSpider() -> {
                    Log.d("ONETV_MOVIE", "🕷️ 智能识别: Spider站点")
                    handleSpiderSite(site)
                }
                site.isCms() -> {
                    Log.d("ONETV_MOVIE", "🌐 智能识别: CMS站点")
                    handleCmsSite(site)
                }
                site.isApp() -> {
                    Log.d("ONETV_MOVIE", "📱 智能识别: APP站点")
                    handleAppSite(site)
                }
                site.isAlist() -> {
                    Log.d("ONETV_MOVIE", "💾 智能识别: Alist站点")
                    handleAlistSite(site)
                }
                else -> {
                    Log.w("ONETV_MOVIE", "⚠️ 未知站点类型: ${site.type}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.w("ONETV_MOVIE", "⚠️ 智能识别处理失败: ${e.message}")
            emptyList()
        }
    }
}
