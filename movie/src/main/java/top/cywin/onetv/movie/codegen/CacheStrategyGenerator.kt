package top.cywin.onetv.movie.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import top.cywin.onetv.movie.data.models.VodConfigResponse
import java.io.File

/**
 * 智能缓存策略生成器
 * 根据数据类型和使用模式生成优化的缓存管理器
 * 
 * 支持的缓存策略：
 * - 配置缓存: 24小时TTL，高优先级
 * - 内容缓存: 30分钟TTL，中等优先级
 * - 搜索缓存: 5分钟TTL，低优先级
 * - 详情缓存: 1小时TTL，中等优先级
 * - 播放缓存: 10分钟TTL，高优先级
 */
object CacheStrategyGenerator {
    private const val GENERATED_PACKAGE = "top.cywin.onetv.movie.generated.cache"
    
    /**
     * 生成智能缓存管理器
     */
    fun generateCacheManager(config: VodConfigResponse): FileSpec {
        val cacheClass = TypeSpec.classBuilder("GeneratedCacheManager")
            .addSuperinterface(ClassName("top.cywin.onetv.movie.data.cache", "CacheManager"))
            .addProperty(generateCacheConfigProperty())
            .addProperty(generateCacheStatsProperty())
            .addProperty(generateMemoryCacheProperty())
            .addProperty(generateDiskCacheProperty())
            .addFunction(generateGetFunction())
            .addFunction(generatePutFunction())
            .addFunction(generateClearFunction())
            .addFunction(generateGetSizeFunction())
            .addFunction(generateCleanupExpiredFunction())
            .addFunction(generateGetCacheStatsFunction())
            .apply {
                // 为每种数据类型生成专门的缓存方法
                addFunction(generateConfigCacheFunction())
                addFunction(generateContentCacheFunction())
                addFunction(generateSearchCacheFunction())
                addFunction(generateDetailCacheFunction())
                addFunction(generatePlayUrlCacheFunction())
                addFunction(generateImageCacheFunction())
            }
            .build()
        
        return FileSpec.builder(GENERATED_PACKAGE, "GeneratedCacheManager")
            .addImport("android.util", "Log", "LruCache")
            .addImport("kotlinx.coroutines", "withContext", "Dispatchers", "sync.Mutex", "sync.withLock")
            .addImport("kotlinx.serialization.json", "Json")
            .addImport("top.cywin.onetv.movie.data.cache", "CacheManager")
            .addImport("top.cywin.onetv.movie.data.models", "*")
            .addImport("java.io", "File")
            .addImport("java.util.concurrent", "ConcurrentHashMap")
            .addType(cacheClass)
            .addFileComment("🤖 自动生成的智能缓存管理器")
            .addFileComment("支持多级缓存策略和智能过期管理")
            .addFileComment("配置站点数: ${config.sites.size}")
            .addFileComment("解析器数: ${config.parses.size}")
            .addFileComment("生成时间: ${java.time.LocalDateTime.now()}")
            .addFileComment("⚠️ 此文件由KotlinPoet自动生成，请勿手动修改！")
            .build()
    }
    
    /**
     * 生成缓存配置属性
     */
    private fun generateCacheConfigProperty(): PropertySpec {
        return PropertySpec.builder("cacheConfig", 
            ClassName("kotlin.collections", "Map")
                .parameterizedBy(String::class.asClassName(), Long::class.asClassName()))
            .addModifiers(KModifier.PRIVATE)
            .initializer(
                CodeBlock.builder()
                    .add("mapOf(\n")
                    .indent()
                    .add("\"config\" to 24 * 60 * 60 * 1000L, // 24小时\n")
                    .add("\"content\" to 15 * 60 * 1000L, // 15分钟\n")
                    .add("\"search\" to 5 * 60 * 1000L, // 5分钟\n")
                    .add("\"detail\" to 60 * 60 * 1000L, // 1小时\n")
                    .add("\"playurl\" to 10 * 60 * 1000L, // 10分钟\n")
                    .add("\"image\" to 2 * 60 * 60 * 1000L // 2小时\n")
                    .unindent()
                    .add(")")
                    .build()
            )
            .build()
    }
    
    /**
     * 生成缓存统计属性
     */
    private fun generateCacheStatsProperty(): PropertySpec {
        return PropertySpec.builder("cacheStats", 
            ClassName("java.util.concurrent", "ConcurrentHashMap")
                .parameterizedBy(String::class.asClassName(), Long::class.asClassName()))
            .addModifiers(KModifier.PRIVATE)
            .initializer("ConcurrentHashMap()")
            .build()
    }
    
    /**
     * 生成内存缓存属性
     */
    private fun generateMemoryCacheProperty(): PropertySpec {
        return PropertySpec.builder("memoryCache", 
            ClassName("android.util", "LruCache")
                .parameterizedBy(String::class.asClassName(), String::class.asClassName()))
            .addModifiers(KModifier.PRIVATE)
            .initializer(
                CodeBlock.builder()
                    .add("object : LruCache<String, String>(1024 * 1024) { // 1MB\n")
                    .indent()
                    .add("override fun sizeOf(key: String, value: String): Int {\n")
                    .indent()
                    .add("return value.toByteArray().size\n")
                    .unindent()
                    .add("}\n")
                    .unindent()
                    .add("}")
                    .build()
            )
            .build()
    }
    
    /**
     * 生成磁盘缓存属性
     */
    private fun generateDiskCacheProperty(): PropertySpec {
        return PropertySpec.builder("diskCacheDir", File::class)
            .addModifiers(KModifier.PRIVATE)
            .initializer("File(context.cacheDir, \"movie_cache\")")
            .build()
    }
    
    /**
     * 生成通用Get函数
     */
    private fun generateGetFunction(): FunSpec {
        return FunSpec.builder("get")
            .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
            .addParameter("key", String::class)
            .returns(String::class.asClassName().copy(nullable = true))
            .addCode(
                CodeBlock.builder()
                    .addStatement("return withContext(Dispatchers.IO) {")
                    .indent()
                    .addStatement("try {")
                    .indent()
                    .addStatement("// 先从内存缓存获取")
                    .addStatement("val memoryValue = memoryCache.get(key)")
                    .addStatement("if (memoryValue != null) {")
                    .indent()
                    .addStatement("cacheStats[\"memory_hit\"] = (cacheStats[\"memory_hit\"] ?: 0) + 1")
                    .addStatement("Log.v(\"ONETV_MOVIE\", \"💾 内存缓存命中: \$key\")")
                    .addStatement("return@withContext memoryValue")
                    .unindent()
                    .addStatement("}")
                    .addStatement("")
                    .addStatement("// 从磁盘缓存获取")
                    .addStatement("val diskFile = File(diskCacheDir, key.hashCode().toString())")
                    .addStatement("if (diskFile.exists() && !isCacheExpired(diskFile, key)) {")
                    .indent()
                    .addStatement("val diskValue = diskFile.readText()")
                    .addStatement("// 回写到内存缓存")
                    .addStatement("memoryCache.put(key, diskValue)")
                    .addStatement("cacheStats[\"disk_hit\"] = (cacheStats[\"disk_hit\"] ?: 0) + 1")
                    .addStatement("Log.v(\"ONETV_MOVIE\", \"💿 磁盘缓存命中: \$key\")")
                    .addStatement("return@withContext diskValue")
                    .unindent()
                    .addStatement("}")
                    .addStatement("")
                    .addStatement("cacheStats[\"cache_miss\"] = (cacheStats[\"cache_miss\"] ?: 0) + 1")
                    .addStatement("Log.v(\"ONETV_MOVIE\", \"❌ 缓存未命中: \$key\")")
                    .addStatement("null")
                    .unindent()
                    .addStatement("} catch (e: Exception) {")
                    .indent()
                    .addStatement("Log.e(\"ONETV_MOVIE\", \"❌ 缓存读取失败: \$key\", e)")
                    .addStatement("null")
                    .unindent()
                    .addStatement("}")
                    .unindent()
                    .addStatement("}")
                    .build()
            )
            .build()
    }
    
    /**
     * 生成通用Put函数
     */
    private fun generatePutFunction(): FunSpec {
        return FunSpec.builder("put")
            .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
            .addParameter("key", String::class)
            .addParameter("value", String::class)
            .addParameter("ttl", Long::class, KModifier.VARARG)
            .addCode(
                CodeBlock.builder()
                    .addStatement("withContext(Dispatchers.IO) {")
                    .indent()
                    .addStatement("try {")
                    .indent()
                    .addStatement("// 写入内存缓存")
                    .addStatement("memoryCache.put(key, value)")
                    .addStatement("")
                    .addStatement("// 写入磁盘缓存")
                    .addStatement("if (!diskCacheDir.exists()) {")
                    .indent()
                    .addStatement("diskCacheDir.mkdirs()")
                    .unindent()
                    .addStatement("}")
                    .addStatement("")
                    .addStatement("val diskFile = File(diskCacheDir, key.hashCode().toString())")
                    .addStatement("diskFile.writeText(value)")
                    .addStatement("")
                    .addStatement("cacheStats[\"cache_write\"] = (cacheStats[\"cache_write\"] ?: 0) + 1")
                    .addStatement("Log.v(\"ONETV_MOVIE\", \"💾 缓存写入成功: \$key\")")
                    .unindent()
                    .addStatement("} catch (e: Exception) {")
                    .indent()
                    .addStatement("Log.e(\"ONETV_MOVIE\", \"❌ 缓存写入失败: \$key\", e)")
                    .unindent()
                    .addStatement("}")
                    .unindent()
                    .addStatement("}")
                    .build()
            )
            .build()
    }
    
    /**
     * 生成配置缓存函数
     */
    private fun generateConfigCacheFunction(): FunSpec {
        return FunSpec.builder("cacheConfig")
            .addModifiers(KModifier.SUSPEND)
            .addParameter("key", String::class)
            .addParameter("config", ClassName("top.cywin.onetv.movie.data.models", "VodConfigResponse"))
            .addCode(
                CodeBlock.builder()
                    .addStatement("val cacheKey = \"config_\$key\"")
                    .addStatement("val configJson = Json.encodeToString(VodConfigResponse.serializer(), config)")
                    .addStatement("put(cacheKey, configJson)")
                    .addStatement("Log.d(\"ONETV_MOVIE\", \"📋 配置缓存成功: \$key\")")
                    .build()
            )
            .build()
    }
    
    /**
     * 生成内容缓存函数
     */
    private fun generateContentCacheFunction(): FunSpec {
        return FunSpec.builder("cacheContent")
            .addModifiers(KModifier.SUSPEND)
            .addParameter("siteKey", String::class)
            .addParameter("typeId", String::class)
            .addParameter("page", Int::class)
            .addParameter("content", ClassName("top.cywin.onetv.movie.data.models", "VodResponse"))
            .addCode(
                CodeBlock.builder()
                    .addStatement("val cacheKey = \"content_\${siteKey}_\${typeId}_\$page\"")
                    .addStatement("val contentJson = Json.encodeToString(VodResponse.serializer(), content)")
                    .addStatement("put(cacheKey, contentJson)")
                    .addStatement("Log.d(\"ONETV_MOVIE\", \"📄 内容缓存成功: \$cacheKey\")")
                    .build()
            )
            .build()
    }
    
    /**
     * 生成搜索缓存函数
     */
    private fun generateSearchCacheFunction(): FunSpec {
        return FunSpec.builder("cacheSearch")
            .addModifiers(KModifier.SUSPEND)
            .addParameter("keyword", String::class)
            .addParameter("page", Int::class)
            .addParameter("results", ClassName("top.cywin.onetv.movie.data.models", "VodResponse"))
            .addCode(
                CodeBlock.builder()
                    .addStatement("val cacheKey = \"search_\${keyword.hashCode()}_\$page\"")
                    .addStatement("val resultsJson = Json.encodeToString(VodResponse.serializer(), results)")
                    .addStatement("put(cacheKey, resultsJson)")
                    .addStatement("Log.d(\"ONETV_MOVIE\", \"🔍 搜索缓存成功: \$keyword\")")
                    .build()
            )
            .build()
    }
    
    /**
     * 生成详情缓存函数
     */
    private fun generateDetailCacheFunction(): FunSpec {
        return FunSpec.builder("cacheDetail")
            .addModifiers(KModifier.SUSPEND)
            .addParameter("vodId", String::class)
            .addParameter("detail", ClassName("top.cywin.onetv.movie.data.models", "VodResponse"))
            .addCode(
                CodeBlock.builder()
                    .addStatement("val cacheKey = \"detail_\$vodId\"")
                    .addStatement("val detailJson = Json.encodeToString(VodResponse.serializer(), detail)")
                    .addStatement("put(cacheKey, detailJson)")
                    .addStatement("Log.d(\"ONETV_MOVIE\", \"📋 详情缓存成功: \$vodId\")")
                    .build()
            )
            .build()
    }
    
    /**
     * 生成播放链接缓存函数
     */
    private fun generatePlayUrlCacheFunction(): FunSpec {
        return FunSpec.builder("cachePlayUrl")
            .addModifiers(KModifier.SUSPEND)
            .addParameter("url", String::class)
            .addParameter("playUrl", String::class)
            .addCode(
                CodeBlock.builder()
                    .addStatement("val cacheKey = \"playurl_\${url.hashCode()}\"")
                    .addStatement("put(cacheKey, playUrl)")
                    .addStatement("Log.d(\"ONETV_MOVIE\", \"▶️ 播放链接缓存成功\")")
                    .build()
            )
            .build()
    }
    
    /**
     * 生成图片缓存函数
     */
    private fun generateImageCacheFunction(): FunSpec {
        return FunSpec.builder("cacheImage")
            .addModifiers(KModifier.SUSPEND)
            .addParameter("imageUrl", String::class)
            .addParameter("imageData", ByteArray::class)
            .addCode(
                CodeBlock.builder()
                    .addStatement("val cacheKey = \"image_\${imageUrl.hashCode()}\"")
                    .addStatement("val imageBase64 = android.util.Base64.encodeToString(imageData, android.util.Base64.DEFAULT)")
                    .addStatement("put(cacheKey, imageBase64)")
                    .addStatement("Log.d(\"ONETV_MOVIE\", \"🖼️ 图片缓存成功\")")
                    .build()
            )
            .build()
    }
    
    // 其他辅助方法的生成...
    private fun generateClearFunction(): FunSpec = FunSpec.builder("clear").build()
    private fun generateGetSizeFunction(): FunSpec = FunSpec.builder("getSize").build()
    private fun generateCleanupExpiredFunction(): FunSpec = FunSpec.builder("cleanupExpired").build()
    private fun generateGetCacheStatsFunction(): FunSpec = FunSpec.builder("getCacheStats").build()
}
