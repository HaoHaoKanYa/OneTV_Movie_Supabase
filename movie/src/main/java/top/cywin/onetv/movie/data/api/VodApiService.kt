package top.cywin.onetv.movie.data.api

import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
//import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import top.cywin.onetv.movie.data.models.VodResponse
import top.cywin.onetv.movie.data.models.VodConfigResponse
import top.cywin.onetv.movie.data.models.VodConfigLinkResponse
import java.util.concurrent.TimeUnit

/**
 * 点播API服务 - 直接调用站点API (TVBOX标准)
 */
interface VodApiService {

    /**
     * 获取配置文件链接 (从Edge Function获取onetv-api-movie.json的访问链接)
     */
    @GET("vod-config")
    suspend fun getConfigLink(): VodConfigLinkResponse

    /**
     * 直接获取配置文件内容 (通过URL下载配置文件)
     */
    @GET
    suspend fun getConfigContent(@Url url: String): VodConfigResponse

    /**
     * 直接调用站点API - 获取首页内容 (TVBOX标准)
     */
    @GET
    suspend fun getHomeContent(@Url url: String): VodResponse

    /**
     * 直接调用站点API - 获取分类内容 (TVBOX标准)
     */
    @GET
    suspend fun getCategoryContent(
        @Url url: String,
        @Query("ac") ac: String = "list",
        @Query("t") typeId: String,
        @Query("pg") page: Int = 1,
        @Query("f") filters: String = ""
    ): VodResponse

    /**
     * 直接调用站点API - 搜索内容 (TVBOX标准)
     */
    @GET
    suspend fun searchContent(
        @Url url: String,
        @Query("ac") ac: String = "list",
        @Query("wd") keyword: String,
        @Query("pg") page: Int = 1
    ): VodResponse

    /**
     * 直接调用站点API - 获取详情 (TVBOX标准)
     */
    @GET
    suspend fun getContentDetail(
        @Url url: String,
        @Query("ac") ac: String = "detail",
        @Query("ids") ids: String
    ): VodResponse

    companion object {
        /**
         * 创建配置API服务 (用于获取配置文件)
         * 使用AppConfigManager获取真实的Supabase URL
         */
        fun createConfigService(appConfigManager: top.cywin.onetv.movie.data.config.AppConfigManager): VodApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("User-Agent", "OneTV/2.1.1")

                    // 安全获取Service Role Key，Edge Function需要service_role权限
                    Log.d("ONETV_MOVIE", "🔐 开始设置Edge Function认证头...")
                    try {
                        if (appConfigManager.isConfigInitialized()) {
                            Log.d("ONETV_MOVIE", "✅ AppConfigManager已初始化，获取Service Role Key...")
                            val serviceKey = appConfigManager.getServiceRoleKey()
                            val apiKey = appConfigManager.getApiKey()

                            // Edge Function需要Service Role Key来访问存储桶
                            request.addHeader("apikey", serviceKey)
                            request.addHeader("Authorization", "Bearer $serviceKey")

                            // 添加详细调试信息
                            Log.d("ONETV_MOVIE", "✅ 使用Service Role Key访问Edge Function")
                            Log.d("ONETV_MOVIE", "🔑 Service Key前缀: ${serviceKey.take(20)}...")
                            Log.d("ONETV_MOVIE", "🔑 Service Key长度: ${serviceKey.length}")
                            Log.d("ONETV_MOVIE", "🔗 请求URL: ${chain.request().url}")
                            Log.d("ONETV_MOVIE", "📋 请求头: apikey=${serviceKey.take(20)}..., Authorization=Bearer ${serviceKey.take(20)}...")
                        } else {
                            // 配置未初始化时使用临时头
                            request.addHeader("apikey", "temp-key")
                            request.addHeader("Authorization", "Bearer temp-key")
                            Log.w("ONETV_MOVIE", "⚠️ 配置未初始化，使用临时认证")
                        }
                    } catch (e: Exception) {
                        // 异常时使用临时头
                        request.addHeader("apikey", "temp-key")
                        request.addHeader("Authorization", "Bearer temp-key")
                        Log.e("ONETV_MOVIE", "❌ 获取Service Role Key失败，使用临时认证", e)
                        Log.e("ONETV_MOVIE", "❌ 异常详情: ${e.message}")
                        Log.e("ONETV_MOVIE", "❌ 异常类型: ${e.javaClass.simpleName}")
                    }

                    chain.proceed(request.build())
                }
                .build()

            // 安全获取Supabase URL
            val baseUrl = try {
                if (appConfigManager.isConfigInitialized()) {
                    appConfigManager.getSupabaseUrl()
                } else {
                    "https://temp.supabase.co"
                }
            } catch (e: Exception) {
                "https://temp.supabase.co" // 临时后备URL
            }

            val retrofit = Retrofit.Builder()
                .baseUrl("$baseUrl/functions/v1/")
                .client(client)
                .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
                .build()

            return retrofit.create(VodApiService::class.java)
        }

        /**
         * 创建站点API服务 (用于直接调用站点API)
         */
        fun createSiteService(): VodApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .addHeader("Referer", "https://www.baidu.com/")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.placeholder.com/") // 占位符，实际使用@Url
                .client(client)
                .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
                .build()

            return retrofit.create(VodApiService::class.java)
        }
    }
}

/**
 * 常量定义 - 仅包含不敏感的默认值
 * 所有敏感配置都从app_configs表动态读取
 */
object Constants {
    // 默认配置键名（用于从app_configs表查询）
    const val CONFIG_APP_ID = "onetv"

    // VOD相关默认配置
    const val DEFAULT_VOD_CONFIG_STORAGE_BUCKET = "vod-sources"
    const val DEFAULT_VOD_CONFIG_FILE_PATH = "onetv-api-movie.json"

    // API端点路径（相对路径）
    const val VOD_CONFIG_ENDPOINT_PATH = "/functions/v1/vod-config"
    const val VOD_CONTENT_ENDPOINT_PATH = "/functions/v1/vod-content"

    // 缓存相关
    const val CONFIG_CACHE_EXPIRE_TIME = 24 * 60 * 60 * 1000L // 24小时
    const val DEFAULT_REQUEST_TIMEOUT = 30000L // 30秒

    // 错误信息
    const val ERROR_CONFIG_NOT_INITIALIZED = "配置未初始化，请先调用AppConfigManager.initializeConfig()"
    const val ERROR_NETWORK_UNAVAILABLE = "网络连接不可用"
    const val ERROR_CONFIG_LOAD_FAILED = "配置加载失败"
}
