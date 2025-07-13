package top.cywin.onetv.film.data.database.entities

import androidx.room.*

/**
 * 配置实体
 * 
 * 基于 FongMi/TV 的配置存储设计
 * 用于本地存储配置信息
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */
@Entity(
    tableName = "config",
    indices = [
        Index(value = ["configUrl"], unique = true),
        Index(value = ["configType"]),
        Index(value = ["isActive"])
    ]
)
data class ConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "configUrl") val configUrl: String,
    @ColumnInfo(name = "configName") val configName: String = "",
    @ColumnInfo(name = "configType") val configType: Int = 0, // 0=TVBOX配置, 1=仓库索引
    @ColumnInfo(name = "configContent") val configContent: String = "",
    @ColumnInfo(name = "configVersion") val configVersion: String = "",
    @ColumnInfo(name = "configSource") val configSource: String = "",
    @ColumnInfo(name = "isActive") val isActive: Boolean = false,
    @ColumnInfo(name = "sitesCount") val sitesCount: Int = 0,
    @ColumnInfo(name = "parsesCount") val parsesCount: Int = 0,
    @ColumnInfo(name = "spider") val spider: String = "",
    @ColumnInfo(name = "wallpaper") val wallpaper: String = "",
    @ColumnInfo(name = "warningText") val warningText: String = "",
    @ColumnInfo(name = "notice") val notice: String = "",
    
    // 时间信息
    @ColumnInfo(name = "createTime") val createTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updateTime") val updateTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "lastLoadTime") val lastLoadTime: Long = 0L
) {
    
    /**
     * 配置类型枚举
     */
    object Type {
        const val TVBOX_CONFIG = 0  // TVBOX 配置
        const val STORE_INDEX = 1   // 仓库索引
    }
    
    /**
     * 🔍 是否为 TVBOX 配置
     */
    fun isTvboxConfig(): Boolean = configType == Type.TVBOX_CONFIG
    
    /**
     * 🔍 是否为仓库索引
     */
    fun isStoreIndex(): Boolean = configType == Type.STORE_INDEX
    
    /**
     * 🔍 是否有站点
     */
    fun hasSites(): Boolean = sitesCount > 0
    
    /**
     * 🔍 是否有解析器
     */
    fun hasParses(): Boolean = parsesCount > 0
    
    /**
     * 🔍 是否有爬虫
     */
    fun hasSpider(): Boolean = spider.isNotEmpty()
    
    /**
     * 📊 获取配置摘要
     */
    fun getSummary(): String {
        return "站点: $sitesCount, 解析器: $parsesCount"
    }
    
    companion object {
        
        /**
         * 🏭 创建 TVBOX 配置
         */
        fun createTvboxConfig(
            configUrl: String,
            configName: String = "",
            configContent: String = "",
            sitesCount: Int = 0,
            parsesCount: Int = 0,
            spider: String = ""
        ): ConfigEntity {
            return ConfigEntity(
                configUrl = configUrl,
                configName = configName,
                configType = Type.TVBOX_CONFIG,
                configContent = configContent,
                sitesCount = sitesCount,
                parsesCount = parsesCount,
                spider = spider
            )
        }
        
        /**
         * 🏭 创建仓库索引配置
         */
        fun createStoreIndex(
            configUrl: String,
            configName: String = "",
            configContent: String = ""
        ): ConfigEntity {
            return ConfigEntity(
                configUrl = configUrl,
                configName = configName,
                configType = Type.STORE_INDEX,
                configContent = configContent
            )
        }
    }
}

/**
 * 站点实体
 */
@Entity(
    tableName = "site",
    indices = [
        Index(value = ["siteKey"], unique = true),
        Index(value = ["configId"]),
        Index(value = ["siteType"]),
        Index(value = ["isActivated"])
    ]
)
data class SiteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "configId") val configId: Long,
    @ColumnInfo(name = "siteKey") val siteKey: String,
    @ColumnInfo(name = "siteName") val siteName: String,
    @ColumnInfo(name = "siteType") val siteType: Int,
    @ColumnInfo(name = "siteApi") val siteApi: String,
    @ColumnInfo(name = "sitePlayUrl") val sitePlayUrl: String = "",
    @ColumnInfo(name = "searchable") val searchable: Int = 0,
    @ColumnInfo(name = "quickSearch") val quickSearch: Int = 0,
    @ColumnInfo(name = "filterable") val filterable: Int = 0,
    @ColumnInfo(name = "ext") val ext: String = "",
    @ColumnInfo(name = "jar") val jar: String = "",
    @ColumnInfo(name = "categories") val categories: String = "",
    @ColumnInfo(name = "header") val header: String = "",
    @ColumnInfo(name = "playerType") val playerType: Int = 0,
    @ColumnInfo(name = "isActivated") val isActivated: Boolean = true,
    
    // 时间信息
    @ColumnInfo(name = "createTime") val createTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updateTime") val updateTime: Long = System.currentTimeMillis()
)

/**
 * 解析器实体
 */
@Entity(
    tableName = "parse",
    indices = [
        Index(value = ["parseName"], unique = true),
        Index(value = ["configId"]),
        Index(value = ["parseType"]),
        Index(value = ["isActivated"])
    ]
)
data class ParseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "configId") val configId: Long,
    @ColumnInfo(name = "parseName") val parseName: String,
    @ColumnInfo(name = "parseType") val parseType: Int,
    @ColumnInfo(name = "parseUrl") val parseUrl: String,
    @ColumnInfo(name = "ext") val ext: String = "",
    @ColumnInfo(name = "header") val header: String = "",
    @ColumnInfo(name = "flag") val flag: String = "",
    @ColumnInfo(name = "isActivated") val isActivated: Boolean = true,
    
    // 时间信息
    @ColumnInfo(name = "createTime") val createTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updateTime") val updateTime: Long = System.currentTimeMillis()
)
