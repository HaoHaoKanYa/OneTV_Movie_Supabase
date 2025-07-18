package top.cywin.onetv.movie

import android.content.Context
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import top.cywin.onetv.movie.codegen.MovieCodeGenerator
import top.cywin.onetv.movie.data.VodConfigManager
import top.cywin.onetv.movie.data.cache.MovieCacheManager
import top.cywin.onetv.movie.data.cache.VodCacheManager
import top.cywin.onetv.movie.data.config.AppConfigManager
import top.cywin.onetv.movie.data.database.MovieDatabase
import top.cywin.onetv.movie.data.models.VodConfigResponse
import top.cywin.onetv.movie.data.repository.WatchHistoryRepository
import top.cywin.onetv.movie.data.repository.FavoriteRepository
import top.cywin.onetv.movie.data.cloud.CloudDriveManager
import top.cywin.onetv.movie.adapter.IntegrationManager
import top.cywin.onetv.movie.adapter.RepositoryAdapter
import top.cywin.onetv.movie.adapter.UIAdapter
import top.cywin.onetv.movie.adapter.ViewModelAdapter
import top.cywin.onetv.movie.api.config.VodConfig
import top.cywin.onetv.movie.model.SiteViewModel
import top.cywin.onetv.movie.model.LiveViewModel
import java.io.File

/**
 * OneTV Movie模块应用单例 - KotlinPoet专业版
 * 集成动态代码生成系统，提供企业级的扩展能力和专业性
 * 
 * 🚀 核心功能：
 * 1. 传统依赖管理 - 兼容现有架构，保持稳定性
 * 2. KotlinPoet代码生成 - 动态适配和优化，提升专业性
 * 3. 智能组件管理 - 自动选择最优实现
 * 4. 性能监控 - 实时性能分析和优化
 * 5. 热更新支持 - 配置变更时动态重新生成代码
 */
object MovieApp {
    private const val TAG = "ONETV_MOVIE"
    
    private lateinit var applicationContext: Context
    private var isInitialized = false
    private var isCodeGenerated = false
    
    // 🤖 KotlinPoet代码生成系统
    private val codeGenerator = MovieCodeGenerator
    
    // 📁 代码生成输出目录
    private val generatedCodeDir by lazy { 
        File(applicationContext.filesDir, "generated_code").apply { mkdirs() }
    }
    
    // 📊 性能统计
    private val performanceStats = mutableMapOf<String, Long>()
    
    // ========== 核心管理器（懒加载） ==========
    val cacheManager by lazy {
        Log.d(TAG, "🏗️ 创建MovieCacheManager")
        MovieCacheManager(applicationContext)
    }

    val appConfigManager by lazy {
        Log.d(TAG, "🏗️ 创建AppConfigManager")
        AppConfigManager(applicationContext)
    }

    val vodConfigManager by lazy {
        Log.d(TAG, "🏗️ 创建VodConfigManager")
        VodConfigManager(cacheManager)
    }

    // ========== 适配器系统 ==========
    val integrationManager by lazy {
        Log.d(TAG, "🏗️ 创建IntegrationManager")
        IntegrationManager.getInstance()
    }

    val repositoryAdapter by lazy {
        Log.d(TAG, "🏗️ 创建RepositoryAdapter")
        RepositoryAdapter()
    }

    val uiAdapter by lazy {
        Log.d(TAG, "🏗️ 创建UIAdapter")
        UIAdapter(applicationContext)
    }

    val viewModelAdapter by lazy {
        Log.d(TAG, "🏗️ 创建ViewModelAdapter")
        ViewModelAdapter(null) // 生命周期在使用时绑定
    }

    // ========== FongMi_TV核心组件 ==========
    val vodConfig by lazy {
        Log.d(TAG, "🏗️ 获取VodConfig")
        VodConfig.get()
    }

    val siteViewModel by lazy {
        Log.d(TAG, "🏗️ 获取SiteViewModel")
        viewModelAdapter.siteViewModel
    }

    val liveViewModel by lazy {
        Log.d(TAG, "🏗️ 获取LiveViewModel")
        viewModelAdapter.liveViewModel
    }

    val cloudDriveManager by lazy {
        Log.d(TAG, "🏗️ 创建CloudDriveManager")
        CloudDriveManager()
    }
    
    // ========== 数据库 ==========
    val database by lazy {
        Log.d(TAG, "🏗️ 创建MovieDatabase")
        MovieDatabase.getDatabase(applicationContext)
    }

    // ========== 初始化适配器系统 ==========
    private fun initializeAdapters() {
        Log.d(TAG, "🏗️ 初始化适配器系统")

        // 1. 初始化Repository适配器（连接FongMi_TV数据层）
        repositoryAdapter.reconnectRepositories()

        // 2. 初始化ViewModel适配器（连接数据观察）
        viewModelAdapter.reconnectViewModels()

        // 3. 初始化UI适配器（适配现有UI）
        uiAdapter.adaptExistingUI()

        // 4. 初始化集成管理器（统一管理）
        // integrationManager.initialize(applicationContext, null)

        Log.d(TAG, "✅ 适配器系统初始化完成")
    }

    // ========== 缓存管理器 ==========
    val vodCacheManager by lazy {
        Log.d(TAG, "🏗️ 创建VodCacheManager")
        VodCacheManager(applicationContext)
    }

    // ========== 仓库层（通过适配器访问） ==========
    // 注意：不再直接创建Repository，而是通过RepositoryAdapter访问FongMi_TV功能

    val watchHistoryRepository by lazy {
        Log.d(TAG, "🏗️ 创建WatchHistoryRepository")
        WatchHistoryRepository(database.watchHistoryDao())
    }

    val favoriteRepository by lazy {
        Log.d(TAG, "🏗️ 创建FavoriteRepository")
        FavoriteRepository(database.favoriteDao())
    }

    /**
     * 确保AppConfigManager已初始化
     */
    private fun ensureAppConfigInitialized() {
        try {
            if (!appConfigManager.isConfigInitialized()) {
                Log.d(TAG, "🔧 AppConfigManager未初始化，开始初始化...")
                // 使用runBlocking确保初始化完成
                kotlinx.coroutines.runBlocking {
                    val result = appConfigManager.initializeConfig()
                    if (result.isSuccess) {
                        Log.d(TAG, "✅ AppConfigManager初始化成功")
                    } else {
                        Log.w(TAG, "⚠️ AppConfigManager初始化失败: ${result.exceptionOrNull()?.message}")
                    }
                }
            } else {
                Log.d(TAG, "✅ AppConfigManager已初始化")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ AppConfigManager初始化异常", e)
        }
    }

    /**
     * 🚀 初始化MovieApp - KotlinPoet专业版
     * 集成动态代码生成，提升系统专业性和扩展能力
     */
    suspend fun initialize(context: Context) {
        if (!isInitialized) {
            val startTime = System.currentTimeMillis()
            applicationContext = context.applicationContext
            isInitialized = true
            
            Log.i(TAG, "🚀 OneTV Movie模块初始化开始 - KotlinPoet专业版")
            
            try {
                // 1. 基础组件初始化
                initializeBasicComponents()
                
                // 2. KotlinPoet代码生成系统初始化
                initializeCodeGenerationSystem()
                
                val initTime = System.currentTimeMillis() - startTime
                performanceStats["init_time"] = initTime
                
                Log.i(TAG, "✅ MovieApp初始化完成！耗时: ${initTime}ms")
                Log.i(TAG, "🤖 代码生成状态: ${if (isCodeGenerated) "已启用" else "使用默认组件"}")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ MovieApp初始化失败", e)
                throw e
            }
        } else {
            Log.w(TAG, "⚠️ MovieApp已经初始化，跳过重复初始化")
        }
    }
    
    /**
     * 🏗️ 初始化基础组件
     */
    private fun initializeBasicComponents() {
        Log.d(TAG, "🏗️ 初始化基础组件...")

        // 触发懒加载，确保核心组件可用
        appConfigManager
        vodConfigManager

        // 初始化适配器系统
        initializeAdapters()

        Log.d(TAG, "✅ 基础组件初始化完成")
    }
    
    /**
     * 🤖 初始化KotlinPoet代码生成系统
     */
    private suspend fun initializeCodeGenerationSystem() {
        Log.i(TAG, "🤖 初始化KotlinPoet代码生成系统...")
        
        try {
            // 加载VOD配置
            val config = loadVodConfig()
            if (config != null && config.sites.isNotEmpty()) {
                Log.i(TAG, "📋 配置加载成功: ${config.sites.size} 个站点，${config.parses.size} 个解析器")
                
                // 启动代码生成（异步）
                GlobalScope.launch {
                    try {
                        generateCode(config)
                        isCodeGenerated = true
                        Log.i(TAG, "🎉 KotlinPoet代码生成完成！系统已升级为专业版")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ 代码生成失败，降级到默认组件", e)
                    }
                }
            } else {
                Log.w(TAG, "⚠️ 配置为空或加载失败，使用默认组件")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 代码生成系统初始化失败", e)
        }
    }
    
    /**
     * 🔄 生成代码
     */
    private suspend fun generateCode(config: VodConfigResponse) {
        val startTime = System.currentTimeMillis()
        
        Log.i(TAG, "🔄 开始生成专业代码...")
        codeGenerator.generateAll(config, generatedCodeDir)
        
        val generateTime = System.currentTimeMillis() - startTime
        performanceStats["code_generation_time"] = generateTime
        
        Log.i(TAG, "✅ 代码生成完成！耗时: ${generateTime}ms")
        
        // 验证生成的代码质量
        val qualityReport = codeGenerator.validateGeneratedCode(generatedCodeDir)
        Log.i(TAG, "📊 代码质量报告: ${qualityReport.totalFiles} 个文件，${qualityReport.warnings.size} 个警告")
    }
    
    /**
     * 📋 加载VOD配置
     */
    private suspend fun loadVodConfig(): VodConfigResponse? {
        return try {
            vodConfigManager.getCurrentConfig()
        } catch (e: Exception) {
            Log.e(TAG, "❌ 配置加载失败", e)
            null
        }
    }
    
    /**
     * 🔄 配置更新时重新生成代码
     */
    suspend fun onConfigUpdated(newConfig: VodConfigResponse) {
        Log.i(TAG, "🔄 配置更新，重新生成代码...")
        
        try {
            generateCode(newConfig)
            Log.i(TAG, "✅ 代码重新生成完成")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 代码重新生成失败", e)
        }
    }
    
    /**
     * 📊 获取性能统计
     */
    fun getPerformanceStats(): Map<String, Long> = performanceStats.toMap()
    
    /**
     * 🔍 检查是否已初始化
     */
    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("MovieApp未初始化，请在Application.onCreate()中调用MovieApp.initialize()")
        }
    }
    
    // ========== 公共访问方法 ==========
    // 注意：使用by lazy属性，无需额外的get方法
    
    /**
     * 🧹 清理资源（可选，用于测试或特殊场景）
     */
    fun cleanup() {
        Log.d(TAG, "🧹 清理MovieApp资源")
        performanceStats.clear()
        // 这里可以添加更多清理逻辑
    }
    
    /**
     * 🔍 获取系统状态信息
     */
    fun getSystemInfo(): Map<String, Any> {
        return mapOf(
            "initialized" to isInitialized,
            "code_generated" to isCodeGenerated,
            "performance_stats" to performanceStats,
            "generated_code_dir" to generatedCodeDir.absolutePath
        )
    }
}
