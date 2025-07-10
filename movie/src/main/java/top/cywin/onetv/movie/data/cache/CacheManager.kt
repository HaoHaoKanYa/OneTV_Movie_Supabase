package top.cywin.onetv.movie.data.cache

/**
 * 缓存管理器接口
 * 定义缓存操作的标准接口
 */
interface CacheManager {
    /**
     * 获取缓存
     */
    suspend fun get(key: String): String?
    
    /**
     * 设置缓存
     */
    suspend fun put(key: String, value: String, vararg ttl: Long)
    
    /**
     * 清除缓存
     */
    suspend fun clear()
    
    /**
     * 获取缓存大小
     */
    suspend fun getSize(): Long
}
