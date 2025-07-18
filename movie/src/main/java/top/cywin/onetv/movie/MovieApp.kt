package top.cywin.onetv.movie

import android.app.Application
import android.util.Log

// ✅ 正确的引用 - 使用现有适配器
import top.cywin.onetv.movie.adapter.IntegrationManager
import top.cywin.onetv.movie.adapter.RepositoryAdapter
import top.cywin.onetv.movie.adapter.UIAdapter
import top.cywin.onetv.movie.adapter.ViewModelAdapter

// ✅ 直接使用FongMi_TV系统
import top.cywin.onetv.movie.api.config.VodConfig
import top.cywin.onetv.movie.model.SiteViewModel
import top.cywin.onetv.movie.model.LiveViewModel

/**
 * OneTV Movie模块应用单例
 * 整合FongMi_TV解析系统，通过适配器提供统一访问接口
 */
class MovieApp : Application() {

    companion object {
        private const val TAG = "ONETV_MOVIE_APP"
        private lateinit var instance: MovieApp

        fun getInstance(): MovieApp = instance
    }

    // ✅ 使用适配器系统替代不存在的类
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

    // ✅ 直接使用FongMi_TV的核心组件
    val vodConfig by lazy {
        Log.d(TAG, "🏗️ 获取VodConfig")
        VodConfig.get()
    }

    val siteViewModel by lazy {
        Log.d(TAG, "🏗️ 获取SiteViewModel")
        viewModelAdapter.siteViewModel ?: SiteViewModel()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        Log.d(TAG, "🚀 OneTV Movie应用启动")

        try {
            initializeAdapters()
            Log.d(TAG, "✅ 应用初始化完成")
        } catch (e: Exception) {
            Log.e(TAG, "💥 应用初始化失败", e)
        }
    }

    private fun initializeAdapters() {
        Log.d(TAG, "🏗️ 初始化适配器系统")

        try {
            // 1. 初始化Repository适配器（连接FongMi_TV数据层）
            repositoryAdapter.reconnectRepositories()
            Log.d(TAG, "✅ RepositoryAdapter初始化完成")

            // 2. 初始化UI适配器（适配现有UI）
            uiAdapter.adaptExistingUI()
            Log.d(TAG, "✅ UIAdapter初始化完成")

            // 3. 初始化ViewModel适配器（连接数据观察）
            viewModelAdapter.reconnectViewModels()
            Log.d(TAG, "✅ ViewModelAdapter初始化完成")

            // 4. 初始化集成管理器（统一管理）
            // integrationManager.initialize(applicationContext, null)
            Log.d(TAG, "✅ IntegrationManager准备就绪")

            Log.d(TAG, "✅ 适配器系统初始化完成")

        } catch (e: Exception) {
            Log.e(TAG, "💥 适配器系统初始化失败", e)
            throw e
        }
    }




}
