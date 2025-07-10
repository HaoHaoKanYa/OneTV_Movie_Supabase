package top.cywin.onetv.movie.codegen

import android.util.Log
import com.squareup.kotlinpoet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.cywin.onetv.movie.data.models.VodConfigResponse
import top.cywin.onetv.movie.data.models.VodParse
import top.cywin.onetv.movie.data.models.VodSite
import java.io.File

/**
 * OneTV Movie模块专业代码生成器
 * 基于KotlinPoet实现动态代码生成，提升系统专业性和扩展能力
 * 
 * 核心功能：
 * 1. 动态解析器生成 - 根据配置生成高性能解析器
 * 2. 站点适配器生成 - 自动适配不同TVBOX站点
 * 3. 智能缓存策略生成 - 优化数据缓存管理
 * 4. API接口生成 - 类型安全的网络接口
 * 5. 组件注册中心 - 统一管理生成的组件
 */
object MovieCodeGenerator {
    private const val TAG = "ONETV_MOVIE_CODEGEN"
    private const val GENERATED_PACKAGE = "top.cywin.onetv.movie.generated"
    
    /**
     * 主代码生成入口
     * 根据VOD配置生成所有必要的组件代码
     */
    suspend fun generateAll(config: VodConfigResponse, outputDir: File) = withContext(Dispatchers.IO) {
        Log.i(TAG, "🚀 开始KotlinPoet专业代码生成")
        Log.i(TAG, "📊 配置统计: ${config.sites.size} 个站点，${config.parses.size} 个解析器")
        
        try {
            // 确保输出目录存在
            ensureOutputDirectory(outputDir)
            
            // 1. 生成解析器系统
            Log.i(TAG, "🔧 生成动态解析器系统...")
            generateParsers(config.parses, outputDir)
            
            // 2. 生成站点适配器
            Log.i(TAG, "🌐 生成站点适配器系统...")
            generateSiteAdapters(config.sites, outputDir)
            
            // 3. 生成智能缓存策略
            Log.i(TAG, "💾 生成智能缓存管理器...")
            generateCacheStrategies(config, outputDir)
            
            // 4. 生成API接口
            Log.i(TAG, "🔗 生成API接口系统...")
            generateApiInterfaces(config.sites, outputDir)
            
            // 5. 生成组件注册中心
            Log.i(TAG, "📋 生成组件注册中心...")
            generateComponentRegistry(config, outputDir)
            
            // 6. 生成性能监控组件
            Log.i(TAG, "📈 生成性能监控系统...")
            generatePerformanceMonitor(outputDir)
            
            Log.i(TAG, "✅ KotlinPoet专业代码生成完成！")
            Log.i(TAG, "📁 生成文件位置: ${outputDir.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 代码生成失败", e)
            throw MovieCodeGenerationException("代码生成失败: ${e.message}", e)
        }
    }
    
    /**
     * 确保输出目录结构存在
     */
    private fun ensureOutputDirectory(outputDir: File) {
        val subDirs = listOf("parser", "adapter", "cache", "api", "registry", "monitor")
        subDirs.forEach { subDir ->
            File(outputDir, subDir).mkdirs()
        }
        Log.d(TAG, "📁 输出目录结构创建完成")
    }
    
    /**
     * 生成动态解析器类
     */
    private fun generateParsers(parses: List<VodParse>, outputDir: File) {
        Log.d(TAG, "🔧 开始生成 ${parses.size} 个解析器...")
        
        parses.forEach { parse ->
            try {
                val parserFile = ParserGenerator.generateParser(parse)
                writeFileToOutput(parserFile, outputDir)
                Log.d(TAG, "✅ 解析器生成成功: ${parse.name}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ 解析器生成失败: ${parse.name}", e)
            }
        }
        
        // 生成解析器管理器
        val parserManagerFile = ParserGenerator.generateParserManager(parses)
        writeFileToOutput(parserManagerFile, outputDir)
        Log.d(TAG, "✅ 解析器管理器生成完成")
    }
    
    /**
     * 生成站点适配器
     */
    private fun generateSiteAdapters(sites: List<VodSite>, outputDir: File) {
        Log.d(TAG, "🌐 开始生成 ${sites.size} 个站点适配器...")
        
        sites.forEach { site ->
            try {
                val adapterFile = SiteAdapterGenerator.generateSiteAdapter(site)
                writeFileToOutput(adapterFile, outputDir)
                Log.d(TAG, "✅ 站点适配器生成成功: ${site.name}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ 站点适配器生成失败: ${site.name}", e)
            }
        }
        
        // 生成站点适配器管理器
        val adapterManagerFile = SiteAdapterGenerator.generateAdapterManager(sites)
        writeFileToOutput(adapterManagerFile, outputDir)
        Log.d(TAG, "✅ 站点适配器管理器生成完成")
    }
    
    /**
     * 生成智能缓存策略
     */
    private fun generateCacheStrategies(config: VodConfigResponse, outputDir: File) {
        Log.d(TAG, "💾 生成智能缓存管理器...")
        
        val cacheManagerFile = CacheStrategyGenerator.generateCacheManager(config)
        writeFileToOutput(cacheManagerFile, outputDir)
        Log.d(TAG, "✅ 智能缓存管理器生成完成")
    }
    
    /**
     * 生成API接口
     */
    private fun generateApiInterfaces(sites: List<VodSite>, outputDir: File) {
        Log.d(TAG, "🔗 开始生成API接口...")
        
        sites.forEach { site ->
            try {
                val apiFile = ApiInterfaceGenerator.generateApiInterface(site)
                writeFileToOutput(apiFile, outputDir)
                Log.d(TAG, "✅ API接口生成成功: ${site.name}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ API接口生成失败: ${site.name}", e)
            }
        }
        
        // 生成统一API管理器
        val apiManagerFile = ApiInterfaceGenerator.generateApiManager(sites)
        writeFileToOutput(apiManagerFile, outputDir)
        Log.d(TAG, "✅ API管理器生成完成")
    }
    
    /**
     * 生成组件注册中心
     */
    private fun generateComponentRegistry(config: VodConfigResponse, outputDir: File) {
        Log.d(TAG, "📋 生成组件注册中心...")
        
        val registryFile = ComponentRegistryGenerator.generateRegistry(config)
        writeFileToOutput(registryFile, outputDir)
        Log.d(TAG, "✅ 组件注册中心生成完成")
    }
    
    /**
     * 生成性能监控组件
     */
    private fun generatePerformanceMonitor(outputDir: File) {
        Log.d(TAG, "📈 生成性能监控系统...")
        
        val monitorFile = PerformanceMonitorGenerator.generateMonitor()
        writeFileToOutput(monitorFile, outputDir)
        Log.d(TAG, "✅ 性能监控系统生成完成")
    }
    
    /**
     * 写入文件到输出目录
     */
    private fun writeFileToOutput(fileSpec: FileSpec, outputDir: File) {
        try {
            fileSpec.writeTo(outputDir)
            Log.v(TAG, "📝 文件写入成功: ${fileSpec.name}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 文件写入失败: ${fileSpec.name}", e)
            throw e
        }
    }
    
    /**
     * 验证生成的代码质量
     */
    fun validateGeneratedCode(outputDir: File): CodeQualityReport {
        Log.i(TAG, "🔍 开始代码质量验证...")
        
        val report = CodeQualityValidator.validate(outputDir)
        
        Log.i(TAG, "📊 代码质量报告:")
        Log.i(TAG, "  ✅ 生成文件数: ${report.totalFiles}")
        Log.i(TAG, "  ⚠️ 警告数: ${report.warnings.size}")
        Log.i(TAG, "  ❌ 错误数: ${report.errors.size}")
        
        return report
    }
}

/**
 * 代码生成异常
 */
class MovieCodeGenerationException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * 代码质量报告
 */
data class CodeQualityReport(
    val totalFiles: Int,
    val warnings: List<String>,
    val errors: List<String>,
    val suggestions: List<String>
)
