package top.cywin.onetv.movie.data.config

import top.cywin.onetv.movie.data.models.*

/**
 * 默认配置提供器
 * 提供内置的视频源配置（占位符），实际配置从Supabase存储桶加载
 * 
 * 🔧 配置来源说明：
 * - 内置源：存储在 supabase/storage/vod-sources/onetv-api-movie.json
 * - 外置源：用户自定义的TVBOX兼容配置文件（如GitHub托管）
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
     * 默认站点列表（占位符）
     * 实际站点配置从 supabase/storage/vod-sources/onetv-api-movie.json 加载
     */
    private fun getDefaultSites(): List<VodSite> {
        return listOf(
            VodSite(
                key = "placeholder_site_1",
                name = "占位符站点1",
                api = "https://placeholder.example.com/api.php/provide/vod/",
                ext = "",
                jar = "",
                type = 1, // CMS类型
                searchable = 1,
                changeable = 1,
                timeout = 30000,
                header = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                ),
                style = null,
                categories = emptyList()
            ),
            VodSite(
                key = "placeholder_site_2", 
                name = "占位符站点2",
                api = "https://placeholder2.example.com/api.php/provide/vod/",
                ext = "",
                jar = "",
                type = 1, // CMS类型
                searchable = 1,
                changeable = 1,
                timeout = 30000,
                header = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                ),
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
     * 获取配置加载URL
     * 指向Supabase存储桶中的配置文件
     */
    fun getConfigUrl(baseUrl: String): String {
        return "$baseUrl/storage/v1/object/public/vod-sources/onetv-api-movie.json"
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
                    ext = "",
                    jar = "",
                    type = 1,
                    searchable = 1,
                    changeable = 1,
                    timeout = 30000,
                    header = mapOf("User-Agent" to "OneTV/2.0"),
                    style = null,
                    categories = listOf(
                        VodClass(
                            typeId = "1",
                            typeName = "电影",
                            typeFlag = "1"
                        ),
                        VodClass(
                            typeId = "2", 
                            typeName = "电视剧",
                            typeFlag = "1"
                        ),
                        VodClass(
                            typeId = "3",
                            typeName = "综艺",
                            typeFlag = "1"
                        ),
                        VodClass(
                            typeId = "4",
                            typeName = "动漫",
                            typeFlag = "1"
                        )
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
