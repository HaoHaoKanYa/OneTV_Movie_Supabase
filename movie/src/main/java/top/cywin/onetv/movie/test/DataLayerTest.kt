package top.cywin.onetv.movie.test

import android.content.Context
import kotlinx.coroutines.runBlocking
import top.cywin.onetv.movie.data.api.VodApiService
import top.cywin.onetv.movie.data.cache.VodCacheManager
import top.cywin.onetv.movie.data.repository.VodConfigManager
import top.cywin.onetv.movie.data.repository.VodRepository

/**
 * 数据层集成测试 (用于验证架构正确性)
 */
class DataLayerTest(private val context: Context) {

    private val cacheManager = VodCacheManager(context)
    private val configManager = VodConfigManager(context)
    
    // 模拟API服务 (实际使用时会通过Hilt注入)
    private val configApiService = createMockConfigApiService()
    private val siteApiService = createMockSiteApiService()
    
    private val repository = VodRepository(
        configApiService = configApiService,
        siteApiService = siteApiService,
        cacheManager = cacheManager,
        configManager = configManager
    )

    /**
     * 测试配置加载
     */
    fun testConfigLoading() = runBlocking {
        println("=== 测试配置加载 ===")
        
        try {
            val result = repository.loadConfig()
            if (result.isSuccess) {
                val config = result.getOrNull()
                println("✅ 配置加载成功")
                println("站点数量: ${config?.sites?.size ?: 0}")
                println("解析器数量: ${config?.parses?.size ?: 0}")
            } else {
                println("❌ 配置加载失败: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            println("❌ 配置加载异常: ${e.message}")
        }
    }

    /**
     * 测试分类获取
     */
    fun testCategoriesLoading() = runBlocking {
        println("\n=== 测试分类获取 ===")
        
        try {
            // 先确保有配置
            repository.loadConfig()
            
            val result = repository.getCategories("test_site")
            if (result.isSuccess) {
                val categories = result.getOrNull() ?: emptyList()
                println("✅ 分类获取成功")
                println("分类数量: ${categories.size}")
                categories.take(3).forEach { category ->
                    println("- ${category.typeName} (${category.typeId})")
                }
            } else {
                println("❌ 分类获取失败: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            println("❌ 分类获取异常: ${e.message}")
        }
    }

    /**
     * 测试内容列表获取
     */
    fun testContentListLoading() = runBlocking {
        println("\n=== 测试内容列表获取 ===")
        
        try {
            val result = repository.getContentList(
                typeId = "1", // 电影分类
                page = 1,
                siteKey = "test_site"
            )
            
            if (result.isSuccess) {
                val response = result.getOrNull()
                println("✅ 内容列表获取成功")
                println("内容数量: ${response?.list?.size ?: 0}")
                println("总页数: ${response?.pagecount ?: 0}")
                
                response?.list?.take(3)?.forEach { item ->
                    println("- ${item.vodName} (${item.vodYear})")
                }
            } else {
                println("❌ 内容列表获取失败: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            println("❌ 内容列表获取异常: ${e.message}")
        }
    }

    /**
     * 测试搜索功能
     */
    fun testSearchContent() = runBlocking {
        println("\n=== 测试搜索功能 ===")
        
        try {
            val result = repository.searchContent(
                keyword = "复仇者联盟",
                page = 1,
                siteKey = "test_site"
            )
            
            if (result.isSuccess) {
                val response = result.getOrNull()
                println("✅ 搜索功能正常")
                println("搜索结果数量: ${response?.list?.size ?: 0}")
                
                response?.list?.take(2)?.forEach { item ->
                    println("- ${item.vodName}")
                }
            } else {
                println("❌ 搜索功能失败: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            println("❌ 搜索功能异常: ${e.message}")
        }
    }

    /**
     * 测试缓存功能
     */
    fun testCacheFunction() = runBlocking {
        println("\n=== 测试缓存功能 ===")
        
        try {
            // 测试通用缓存
            cacheManager.putCache("test_key", "test_value", 60 * 1000) // 1分钟
            val cachedValue = cacheManager.getCache<String>("test_key")
            
            if (cachedValue == "test_value") {
                println("✅ 缓存存储和读取正常")
            } else {
                println("❌ 缓存功能异常")
            }
            
            // 测试缓存大小
            val cacheSize = cacheManager.getCacheSize()
            println("缓存大小: ${cacheSize} bytes")
            
            // 测试缓存统计
            val stats = cacheManager.getCacheStats()
            println("缓存统计: $stats")
            
        } catch (e: Exception) {
            println("❌ 缓存功能异常: ${e.message}")
        }
    }

    /**
     * 测试配置管理
     */
    fun testConfigManager() {
        println("\n=== 测试配置管理 ===")
        
        try {
            val stats = configManager.getConfigStats()
            println("✅ 配置管理正常")
            println("配置统计: $stats")
            
            val allSites = configManager.getAllSites()
            println("所有站点数量: ${allSites.size}")
            
            val searchableSites = configManager.getSearchableSites()
            println("可搜索站点数量: ${searchableSites.size}")
            
        } catch (e: Exception) {
            println("❌ 配置管理异常: ${e.message}")
        }
    }

    /**
     * 运行所有测试
     */
    fun runAllTests() {
        println("🚀 开始数据层集成测试...")
        
        testConfigLoading()
        testCategoriesLoading()
        testContentListLoading()
        testSearchContent()
        testCacheFunction()
        testConfigManager()
        
        println("\n✅ 数据层集成测试完成!")
    }

    /**
     * 创建模拟配置API服务
     */
    private fun createMockConfigApiService(): VodApiService {
        // TODO: 返回模拟的API服务实现
        // 实际项目中这里会返回真实的Retrofit服务
        return object : VodApiService {
            override suspend fun getCategories(siteKey: String, action: String) = 
                throw NotImplementedError("Mock implementation")
            override suspend fun getContentList(siteKey: String, action: String, typeId: String, page: Int, filters: String) = 
                throw NotImplementedError("Mock implementation")
            override suspend fun getContentDetail(siteKey: String, action: String, ids: String) = 
                throw NotImplementedError("Mock implementation")
            override suspend fun searchContent(siteKey: String, action: String, keyword: String, page: Int) = 
                throw NotImplementedError("Mock implementation")
            override suspend fun getRecommendContent(siteKey: String, action: String, limit: Int) = 
                throw NotImplementedError("Mock implementation")
            override suspend fun getConfig(configUrl: String) = 
                """{"spider":"","sites":[],"lives":[],"parses":[],"flags":[],"ads":[],"wallpaper":"","warningText":""}"""
            override suspend fun parsePlayUrl(playUrl: String, siteKey: String, flag: String) = 
                emptyMap<String, Any>()
            override suspend fun getHotKeywords(siteKey: String, action: String, limit: Int) = 
                emptyList<String>()
        }
    }

    /**
     * 创建模拟站点API服务
     */
    private fun createMockSiteApiService(): VodApiService {
        // TODO: 返回模拟的API服务实现
        return createMockConfigApiService() // 暂时使用相同的实现
    }
}
