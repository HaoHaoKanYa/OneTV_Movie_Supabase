package top.cywin.onetv.film.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 播放器结果数据模型
 * 
 * 基于 FongMi/TV 的标准播放器结果格式
 * 兼容 TVBOX 标准播放器响应
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */
@Serializable
data class PlayerResult(
    val url: String = "",                              // 播放地址
    val header: Map<String, String> = emptyMap(),      // 请求头
    val parse: Int = 0,                                // 是否需要解析 (0=直链, 1=需要解析)
    val playType: Int = 0,                             // 播放类型
    val subt: String = "",                             // 字幕地址
    val jx: Int = 0,                                   // 是否需要嗅探 (0=不需要, 1=需要)
    val danmaku: String = "",                          // 弹幕地址
    
    // 扩展字段
    val siteKey: String = "",                          // 站点标识
    val flag: String = "",                             // 播放标识
    val id: String = "",                               // 播放 ID
    val format: String = "",                           // 播放格式
    val click: String = "",                            // 点击地址
    val key: String = "",                              // 播放密钥
    val desc: String = "",                             // 描述信息
    val jxFrom: String = "",                           // 解析来源
    val drm: PlayerDrm? = null,                        // DRM 配置
    val parseTime: Long = 0L,                          // 解析耗时
    val createTime: Long = System.currentTimeMillis()  // 创建时间
) {
    
    /**
     * 🔍 是否为有效播放地址
     */
    fun isValid(): Boolean = url.isNotEmpty()
    
    /**
     * 🔍 是否需要解析
     */
    fun needParse(): Boolean = parse == 1
    
    /**
     * 🔍 是否需要嗅探
     */
    fun needSniffer(): Boolean = jx == 1
    
    /**
     * 🔍 是否有字幕
     */
    fun hasSubtitle(): Boolean = subt.isNotEmpty()
    
    /**
     * 🔍 是否有弹幕
     */
    fun hasDanmaku(): Boolean = danmaku.isNotEmpty()
    
    /**
     * 🔍 是否有 DRM
     */
    fun hasDrm(): Boolean = drm != null
    
    /**
     * 🔍 是否为直播流
     */
    fun isLiveStream(): Boolean {
        return url.contains("m3u8") || url.contains("flv") || url.contains("rtmp")
    }
    
    /**
     * 🔍 获取真实播放地址
     */
    fun getRealUrl(): String {
        return if (url.isNotEmpty()) {
            url + if (key.isNotEmpty()) "?key=$key" else ""
        } else {
            ""
        }
    }
    
    /**
     * 🔍 获取用户代理
     */
    fun getUserAgent(): String {
        return header["User-Agent"] ?: header["user-agent"] ?: ""
    }
    
    /**
     * 🔍 获取 Referer
     */
    fun getReferer(): String {
        return header["Referer"] ?: header["referer"] ?: ""
    }
    
    /**
     * 📊 获取统计信息
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "is_valid" to isValid(),
            "need_parse" to needParse(),
            "need_sniffer" to needSniffer(),
            "has_subtitle" to hasSubtitle(),
            "has_danmaku" to hasDanmaku(),
            "has_drm" to hasDrm(),
            "is_live_stream" to isLiveStream(),
            "header_count" to header.size,
            "site_key" to siteKey,
            "flag" to flag,
            "parse_time" to parseTime,
            "create_time" to createTime
        )
    }
    
    companion object {
        
        /**
         * 🏭 创建成功结果
         */
        fun success(
            url: String,
            header: Map<String, String> = emptyMap(),
            parse: Int = 0,
            siteKey: String = "",
            flag: String = "",
            id: String = "",
            parseTime: Long = 0L
        ): PlayerResult {
            return PlayerResult(
                url = url,
                header = header,
                parse = parse,
                siteKey = siteKey,
                flag = flag,
                id = id,
                parseTime = parseTime
            )
        }
        
        /**
         * 🏭 创建失败结果
         */
        fun failure(
            desc: String,
            siteKey: String = "",
            flag: String = "",
            id: String = "",
            parseTime: Long = 0L
        ): PlayerResult {
            return PlayerResult(
                url = "",
                desc = desc,
                siteKey = siteKey,
                flag = flag,
                id = id,
                parseTime = parseTime
            )
        }
        
        /**
         * 🏭 创建需要解析的结果
         */
        fun needParse(
            url: String,
            header: Map<String, String> = emptyMap(),
            siteKey: String = "",
            flag: String = "",
            id: String = ""
        ): PlayerResult {
            return PlayerResult(
                url = url,
                header = header,
                parse = 1,
                siteKey = siteKey,
                flag = flag,
                id = id
            )
        }
        
        /**
         * 🏭 创建需要嗅探的结果
         */
        fun needSniffer(
            url: String,
            header: Map<String, String> = emptyMap(),
            siteKey: String = "",
            flag: String = "",
            id: String = ""
        ): PlayerResult {
            return PlayerResult(
                url = url,
                header = header,
                jx = 1,
                siteKey = siteKey,
                flag = flag,
                id = id
            )
        }
        
        /**
         * 🏭 创建带字幕的结果
         */
        fun withSubtitle(
            url: String,
            subt: String,
            header: Map<String, String> = emptyMap(),
            siteKey: String = "",
            flag: String = "",
            id: String = ""
        ): PlayerResult {
            return PlayerResult(
                url = url,
                header = header,
                subt = subt,
                siteKey = siteKey,
                flag = flag,
                id = id
            )
        }
        
        /**
         * 🏭 创建带弹幕的结果
         */
        fun withDanmaku(
            url: String,
            danmaku: String,
            header: Map<String, String> = emptyMap(),
            siteKey: String = "",
            flag: String = "",
            id: String = ""
        ): PlayerResult {
            return PlayerResult(
                url = url,
                header = header,
                danmaku = danmaku,
                siteKey = siteKey,
                flag = flag,
                id = id
            )
        }
    }
}

/**
 * 播放器 DRM 配置
 */
@Serializable
data class PlayerDrm(
    val type: String = "",                             // DRM 类型 (widevine, playready, clearkey)
    val licenseUrl: String = "",                       // 许可证地址
    val keyId: String = "",                            // 密钥 ID
    val key: String = "",                              // 密钥
    val headers: Map<String, String> = emptyMap()      // DRM 请求头
) {
    
    /**
     * 🔍 是否为有效 DRM
     */
    fun isValid(): Boolean = type.isNotEmpty() && (licenseUrl.isNotEmpty() || key.isNotEmpty())
    
    /**
     * 🔍 是否为 Widevine DRM
     */
    fun isWidevine(): Boolean = type.equals("widevine", ignoreCase = true)
    
    /**
     * 🔍 是否为 PlayReady DRM
     */
    fun isPlayReady(): Boolean = type.equals("playready", ignoreCase = true)
    
    /**
     * 🔍 是否为 ClearKey DRM
     */
    fun isClearKey(): Boolean = type.equals("clearkey", ignoreCase = true)
    
    companion object {
        
        /**
         * 🏭 创建 Widevine DRM
         */
        fun widevine(
            licenseUrl: String,
            headers: Map<String, String> = emptyMap()
        ): PlayerDrm {
            return PlayerDrm(
                type = "widevine",
                licenseUrl = licenseUrl,
                headers = headers
            )
        }
        
        /**
         * 🏭 创建 ClearKey DRM
         */
        fun clearKey(
            keyId: String,
            key: String
        ): PlayerDrm {
            return PlayerDrm(
                type = "clearkey",
                keyId = keyId,
                key = key
            )
        }
    }
}
