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
     * 默认站点列表（空列表）
     * 生产环境不提供默认站点，必须从服务器加载
     */
    private fun getDefaultSites(): List<VodSite> {
        return emptyList()
    }
    
    /**
     * 默认解析器列表（空列表）
     * 生产环境不提供默认解析器，必须从服务器加载
     */
    private fun getDefaultParses(): List<VodParse> {
        return emptyList()
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
    

}
