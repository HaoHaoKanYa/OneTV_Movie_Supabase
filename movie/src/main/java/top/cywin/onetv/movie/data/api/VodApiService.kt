package top.cywin.onetv.movie.data.api

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import top.cywin.onetv.movie.data.models.VodResponse
import top.cywin.onetv.movie.data.models.VodConfigResponse
import java.util.concurrent.TimeUnit

/**
 * 点播API服务 - 直接调用站点API (TVBOX标准)
 */
interface VodApiService {

    /**
     * 获取配置文件 (从Supabase获取onetv-api-movie.json)
     */
    @GET("vod-config")
    suspend fun getConfig(): VodConfigResponse

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
         */
        fun createConfigService(): VodApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("${Constants.SUPABASE_URL}/functions/v1/")
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
