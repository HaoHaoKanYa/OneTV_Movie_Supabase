package top.cywin.onetv.movie.data.config

import top.cywin.onetv.movie.data.models.*
import kotlinx.serialization.json.JsonPrimitive

/**
 * 默认配置提供器
 * 提供后备的视频源配置，仅在所有其他配置源都失败时使用
 *
 * 🔧 配置加载优先级：
 * 1. 用户自定义源 (用户设置的TVBOX兼容配置URL)
 * 2. 内置源 (通过Edge Function从vod-sources/onetv-api-movie.json获取)
 * 3. 默认配置 (本类提供的后备配置)
 */
object DefaultConfigProvider {
    
    /**
     * 获取默认配置（占位符）
     * 实际使用时会被Supabase存储桶中的配置覆盖
     */
    fun getDefaultConfig(): VodConfigResponse {
        return VodConfigResponse(
            spider = "", // 爬虫JAR包地址（从存储桶加载）
            wallpaper = "", // 壁纸地址（从存储桶加载）
            sites = getDefaultSites(),
            parses = getDefaultParses(),
            flags = getDefaultFlags(),
            ijk = emptyList(),
            ads = getDefaultAds(),
            notice = "默认配置已加载，正在从服务器获取最新配置..."
        )
    }
    
    /**
     * 默认站点列表（内置测试站点）
     * 提供一些可用的测试站点，确保在没有配置时也能正常显示内容
     */
    private fun getDefaultSites(): List<VodSite> {
        return listOf(
            VodSite(
                key = "default_demo",
                name = "默认站点",
                api = "https://demo.example.com/api.php/provide/vod/",
                ext = JsonPrimitive(""),
                jar = "",
                type = 1, // CMS类型
                searchable = 1,
                quickSearch = 1,
                filterable = 1,
                playerType = 1,
                changeable = 1,
                click = "",
                timeout = 15000, // 15秒超时
                header = null,
                style = null,
                categories = emptyList()
            ),
            VodSite(
                key = "placeholder_site_2", 
                name = "占位符站点2",
                api = "https://placeholder2.example.com/api.php/provide/vod/",
                ext = JsonPrimitive(""),
                jar = "",
                type = 1, // CMS类型
                searchable = 1,
                changeable = 1,
                timeout = 15000,
                header = null,
                style = null,
                categories = emptyList()
            )
        )
    }
    
    /**
     * 默认解析器列表（占位符）
     * 实际解析器配置从存储桶加载
     */
    private fun getDefaultParses(): List<VodParse> {
        return listOf(
            VodParse(
                name = "默认解析器1",
                type = 1, // JSON解析
                url = "https://placeholder-parser1.example.com/",
                ext = mapOf(),
                header = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                )
            ),
            VodParse(
                name = "默认解析器2",
                type = 0, // 嗅探解析
                url = "https://placeholder-parser2.example.com/",
                ext = mapOf(),
                header = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                )
            )
        )
    }
    
    /**
     * 默认标识列表
     */
    private fun getDefaultFlags(): List<String> {
        return listOf(
            "youku", "qq", "iqiyi", "qiyi", "letv", "sohu", "tudou", "pptv", 
            "mgtv", "wasu", "bilibili", "renrenmi", "xigua", "migu", "funshion",
            "优酷", "芒果", "腾讯", "爱奇艺", "奇艺", "哔哩哔哩", "哔哩"
        )
    }
    
    /**
     * 默认广告过滤列表
     */
    private fun getDefaultAds(): List<String> {
        return listOf(
            "mimg.0c1q0l.cn",
            "www.googletagmanager.com",
            "www.google-analytics.com",
            "mc.usihnbcq.cn",
            "mg.g1mm3d.cn",
            "mscs.svaeuzh.cn",
            "cnzz.hhurm.com",
            "tp.vinuxhome.com",
            "cnzz.mmstat.com",
            "www.baihuillq.com"
        )
    }
    
    /**
     * 获取Edge Function配置URL (已废弃，现在通过VodApiService调用)
     * @deprecated 使用VodRepository.loadConfigByPriority()替代
     */
    @Deprecated("使用VodRepository.loadConfigByPriority()替代")
    fun getConfigUrl(baseUrl: String): String {
        return "$baseUrl/functions/v1/vod-config"
    }
    
    /**
     * 验证配置是否为占位符配置
     */
    fun isPlaceholderConfig(config: VodConfigResponse): Boolean {
        return config.sites.any { it.key.startsWith("placeholder_") }
    }
    
    /**
     * 获取配置状态描述
     */
    fun getConfigStatusDescription(config: VodConfigResponse): String {
        return when {
            isPlaceholderConfig(config) -> "使用默认占位符配置，等待服务器配置加载"
            config.sites.isEmpty() -> "配置为空，请检查网络连接"
            else -> "配置加载成功：${config.sites.size} 个站点，${config.parses.size} 个解析器"
        }
    }
    
    /**
     * 创建测试用的TVBOX配置
     * 用于开发和测试阶段
     */
    fun createTestConfig(): VodConfigResponse {
        return VodConfigResponse(
            spider = "",
            wallpaper = "https://picsum.photos/1920/1080",
            sites = listOf(
                VodSite(
                    key = "test_site",
                    name = "测试站点",
                    api = "https://test.example.com/api.php/provide/vod/",
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
                    categories = listOf(
                        "电影",
                        "电视剧",
                        "综艺",
                        "动漫"
                    )
                )
            ),
            parses = listOf(
                VodParse(
                    name = "测试解析器",
                    type = 1,
                    url = "https://test-parser.example.com/",
                    ext = mapOf(),
                    header = mapOf("User-Agent" to "OneTV/2.0")
                )
            ),
            flags = listOf("test", "demo"),
            ijk = emptyList(),
            ads = emptyList(),
            notice = "这是测试配置，仅用于开发和测试"
        )
    }
    
    /**
     * 获取配置文件的示例URL列表
     * 用于用户参考和配置
     */
    fun getExampleConfigUrls(): List<String> {
        return listOf(
            "https://raw.githubusercontent.com/example/tvbox-config/main/config.json",
            "https://gitee.com/example/tvbox-config/raw/master/config.json",
            "https://cdn.jsdelivr.net/gh/example/tvbox-config@main/config.json"
        )
    }
}
