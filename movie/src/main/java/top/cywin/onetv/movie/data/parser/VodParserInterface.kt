package top.cywin.onetv.movie.data.parser

import top.cywin.onetv.movie.data.models.ParseResult
import top.cywin.onetv.movie.data.models.VodSite

/**
 * VOD解析器接口
 * 定义所有解析器必须实现的方法
 */
interface VodParserInterface {
    /**
     * 解析视频链接
     */
    suspend fun parse(url: String, site: VodSite, vararg flags: String): ParseResult
    
    /**
     * 验证URL是否有效
     */
    fun validate(url: String): Boolean
    
    /**
     * 获取请求头
     */
    fun getHeaders(): Map<String, String>
    
    /**
     * 获取超时时间
     */
    fun getTimeout(): Long = 30000L
    
    /**
     * 清理资源
     */
    fun cleanup() {}
}
