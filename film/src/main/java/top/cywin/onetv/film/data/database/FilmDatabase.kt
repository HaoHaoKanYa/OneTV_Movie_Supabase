package top.cywin.onetv.film.data.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import top.cywin.onetv.film.data.database.dao.*
import top.cywin.onetv.film.data.database.entities.*

/**
 * Film 数据库
 * 
 * 基于 FongMi/TV 标准的 Room 数据库
 * 管理影视相关的本地数据存储
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */
@Database(
    entities = [
        VodEntity::class,
        VodHistoryEntity::class,
        VodFavoriteEntity::class,
        VodDownloadEntity::class,
        ConfigEntity::class,
        SiteEntity::class,
        ParseEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class FilmDatabase : RoomDatabase() {
    
    // DAO 接口
    abstract fun vodDao(): VodDao
    abstract fun vodHistoryDao(): VodHistoryDao
    abstract fun vodFavoriteDao(): VodFavoriteDao
    abstract fun vodDownloadDao(): VodDownloadDao
    abstract fun configDao(): ConfigDao
    abstract fun siteDao(): SiteDao
    abstract fun parseDao(): ParseDao
    
    companion object {
        private const val DATABASE_NAME = "film_database"
        
        @Volatile
        private var INSTANCE: FilmDatabase? = null
        
        /**
         * 获取数据库实例
         */
        fun getInstance(context: Context): FilmDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        /**
         * 构建数据库
         */
        private fun buildDatabase(context: Context): FilmDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                FilmDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(*getAllMigrations())
                .addCallback(DatabaseCallback())
                .build()
        }
        
        /**
         * 获取所有数据库迁移
         */
        private fun getAllMigrations(): Array<Migration> {
            return arrayOf(
                // 未来的数据库迁移将在这里添加
            )
        }
    }
    
    /**
     * 数据库回调
     */
    private class DatabaseCallback : RoomDatabase.Callback() {
        
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // 数据库创建时的初始化操作
        }
        
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // 数据库打开时的操作
        }
    }
}

/**
 * 数据库类型转换器
 * 
 * 用于复杂类型的序列化和反序列化
 */
class DatabaseConverters {
    
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }
    
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotEmpty() }
    }
    
    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? {
        return value?.map { "${it.key}=${it.value}" }?.joinToString(";")
    }
    
    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? {
        return value?.split(";")
            ?.filter { it.isNotEmpty() && it.contains("=") }
            ?.associate { 
                val parts = it.split("=", limit = 2)
                parts[0] to (parts.getOrNull(1) ?: "")
            }
    }
}
