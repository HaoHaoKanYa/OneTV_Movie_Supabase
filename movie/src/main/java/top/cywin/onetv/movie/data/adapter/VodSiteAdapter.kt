package top.cywin.onetv.movie.data.adapter

import top.cywin.onetv.movie.data.models.VodResponse

/**
 * VOD站点适配器接口
 * 定义所有站点适配器必须实现的方法
 */
interface VodSiteAdapter {
    /**
     * 搜索内容
     */
    suspend fun search(keyword: String, page: Int): VodResponse
    
    /**
     * 获取详情
     */
    suspend fun getDetail(vodId: String): VodResponse
    
    /**
     * 获取分类内容
     */
    suspend fun getCategory(typeId: String, page: Int): VodResponse
    
    /**
     * 获取首页内容
     */
    suspend fun getHomeContent(): VodResponse
}
