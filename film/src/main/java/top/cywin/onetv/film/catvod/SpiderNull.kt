package top.cywin.onetv.film.catvod

import android.content.Context
import android.util.Log

/**
 * 空 Spider 实现
 * 
 * 基于 FongMi/TV 的 SpiderNull 实现
 * 用于处理无效或不支持的解析器
 * 
 * 特性：
 * - 提供默认的空实现
 * - 避免空指针异常
 * - 记录调用日志
 * - 返回标准格式的空数据
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class SpiderNull : Spider() {
    
    companion object {
        private const val TAG = "ONETV_FILM_SPIDER_NULL"
    }
    
    override fun init(context: Context, extend: String) {
        super.init(context, extend)
        Log.w(TAG, "⚠️ 使用空 Spider 实现，可能是解析器配置错误")
    }
    
    override suspend fun homeContent(filter: Boolean): String {
        Log.d(TAG, "🏠 SpiderNull.homeContent called with filter=$filter")
        
        return buildJsonResponse {
            put("class", buildJsonArray {
                // 返回空分类列表
            })
        }
    }
    
    override suspend fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String {
        Log.d(TAG, "📋 SpiderNull.categoryContent called with tid=$tid, pg=$pg, filter=$filter")
        
        return buildJsonResponse {
            put("list", buildJsonArray {
                // 返回空内容列表
            })
            put("page", pg.toIntOrNull() ?: 1)
            put("pagecount", 0)
            put("limit", 20)
            put("total", 0)
        }
    }
    
    override suspend fun detailContent(ids: List<String>): String {
        Log.d(TAG, "🎭 SpiderNull.detailContent called with ids=$ids")
        
        return buildJsonResponse {
            put("list", buildJsonArray {
                // 返回空详情列表
            })
        }
    }
    
    override suspend fun searchContent(key: String, quick: Boolean): String {
        Log.d(TAG, "🔍 SpiderNull.searchContent called with key=$key, quick=$quick")
        
        return buildJsonResponse {
            put("list", buildJsonArray {
                // 返回空搜索结果
            })
        }
    }
    
    override suspend fun playerContent(flag: String, id: String, vipFlags: List<String>): String {
        Log.d(TAG, "▶️ SpiderNull.playerContent called with flag=$flag, id=$id")
        
        return buildJsonResponse {
            put("parse", 0)
            put("playUrl", "")
            put("url", "")
            put("header", buildJsonObject {
                // 返回空请求头
            })
        }
    }
    
    override fun manualVideoCheck(): Boolean {
        return false
    }
    
    override fun isVideoFormat(url: String): Boolean {
        return false
    }
    
    override suspend fun getRealUrl(url: String): String {
        Log.d(TAG, "🔗 SpiderNull.getRealUrl called with url=$url")
        return url
    }
    
    override fun getSpiderInfo(): Map<String, Any> {
        return super.getSpiderInfo() + mapOf(
            "type" to "null",
            "description" to "空 Spider 实现，用于处理无效解析器"
        )
    }
}
