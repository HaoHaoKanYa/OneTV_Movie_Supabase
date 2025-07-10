package top.cywin.onetv.movie.data.adapter

import top.cywin.onetv.movie.data.models.VodResponse
import top.cywin.onetv.movie.data.models.VodSite

/**
 * 站点适配器管理器接口
 * 定义站点适配器管理器必须实现的方法
 */
interface SiteAdapterManager {
    /**
     * 获取适配器
     */
    fun getAdapter(siteKey: String): VodSiteAdapter?
    
    /**
     * 获取所有适配器
     */
    fun getAllAdapters(): List<VodSiteAdapter>
    
    /**
     * 搜索所有站点
     */
    suspend fun searchAllSites(keyword: String, page: Int): List<VodResponse>
    
    /**
     * 获取最佳站点
     */
    fun getBestSite(): VodSite?
    
    /**
     * 获取站点统计信息
     */
    fun getSiteStats(): Map<String, Any>
}
