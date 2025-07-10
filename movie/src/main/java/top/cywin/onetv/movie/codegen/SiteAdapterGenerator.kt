package top.cywin.onetv.movie.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import top.cywin.onetv.movie.data.models.VodSite
import top.cywin.onetv.movie.data.models.VodResponse

/**
 * 站点适配器生成器
 * 为每个TVBOX站点生成专门的适配器，实现智能适配和优化
 * 
 * 支持的站点类型：
 * - 0: Spider站点 (爬虫类型)
 * - 1: CMS站点 (内容管理系统)
 * - 3: APP站点 (应用接口)
 * - 4: Alist站点 (网盘类型)
 */
object SiteAdapterGenerator {
    private const val GENERATED_PACKAGE = "top.cywin.onetv.movie.generated.adapter"
    
    /**
     * 生成单个站点适配器
     */
    fun generateSiteAdapter(site: VodSite): FileSpec {
        val className = generateClassName(site.key)
        
        val adapterClass = TypeSpec.classBuilder(className)
            .addSuperinterface(ClassName("top.cywin.onetv.movie.data.adapter", "VodSiteAdapter"))
            .addProperty(generateSiteConfigProperty(site))
            .addProperty(generatePerformanceStatsProperty())
            .addFunction(generateSearchFunction(site))
            .addFunction(generateDetailFunction(site))
            .addFunction(generateCategoryFunction(site))
            .addFunction(generateHomeContentFunction(site))
            .addFunction(generateBuildUrlFunction(site))
            .addFunction(generateCreateHttpClientFunction(site))
            .addFunction(generateHandleResponseFunction())
            .addFunction(generateGetSiteStatsFunction())
            .apply {
                // 根据站点类型添加特定方法
                when (site.type) {
                    0 -> addFunction(generateSpiderMethods(site))
                    1 -> addFunction(generateCmsMethods(site))
                    3 -> addFunction(generateAppMethods(site))
                    4 -> addFunction(generateAlistMethods(site))
                }
            }
            .build()
        
        return FileSpec.builder(GENERATED_PACKAGE, className)
            .addImport("android.util", "Log")
            .addImport("kotlinx.coroutines", "withContext", "Dispatchers", "withTimeoutOrNull")
            .addImport("kotlinx.serialization.json", "Json")
            .addImport("okhttp3", "OkHttpClient", "Request", "Response")
            .addImport("top.cywin.onetv.movie.data.models", "VodResponse", "VodSite", "VodItem")
            .addImport("top.cywin.onetv.movie.data.adapter", "VodSiteAdapter")
            .addImport("java.net", "URLEncoder")
            .addImport("java.util.concurrent", "TimeUnit")
            .addType(adapterClass)
            .addFileComment("🤖 自动生成的${getSiteTypeName(site.type)}站点适配器")
            .addFileComment("站点名称: ${site.name}")
            .addFileComment("站点类型: ${site.type}")
            .addFileComment("API地址: ${site.api}")
            .addFileComment("生成时间: ${java.time.LocalDateTime.now()}")
            .addFileComment("⚠️ 此文件由KotlinPoet自动生成，请勿手动修改！")
            .build()
    }
    
    /**
     * 生成站点适配器管理器
     */
    fun generateAdapterManager(sites: List<VodSite>): FileSpec {
        val managerClass = TypeSpec.classBuilder("GeneratedSiteAdapterManager")
            .addSuperinterface(ClassName("top.cywin.onetv.movie.data.adapter", "SiteAdapterManager"))
            .addProperty(generateAdaptersMapProperty(sites))
            .addFunction(generateGetAdapterFunction())
            .addFunction(generateGetAllAdaptersFunction())
            .addFunction(generateSearchAllSitesFunction(sites))
            .addFunction(generateGetBestSiteFunction())
            .addFunction(generateGetSiteStatsFunction())
            .build()
        
        return FileSpec.builder(GENERATED_PACKAGE, "GeneratedSiteAdapterManager")
            .addImport("android.util", "Log")
            .addImport("kotlinx.coroutines", "async", "awaitAll", "coroutineScope")
            .addImport("top.cywin.onetv.movie.data.adapter", "SiteAdapterManager", "VodSiteAdapter")
            .addImport("top.cywin.onetv.movie.data.models", "VodSite", "VodResponse")
            .addType(managerClass)
            .addFileComment("🤖 自动生成的站点适配器管理器")
            .addFileComment("管理 ${sites.size} 个站点适配器")
            .build()
    }
    
    /**
     * 生成类名
     */
    private fun generateClassName(key: String): String {
        return key.split("_", "-")
            .joinToString("") { it.replaceFirstChar { char -> char.uppercase() } } + "SiteAdapter"
    }
    
    /**
     * 生成站点配置属性
     */
    private fun generateSiteConfigProperty(site: VodSite): PropertySpec {
        return PropertySpec.builder("siteConfig", ClassName("top.cywin.onetv.movie.data.models", "VodSite"))
            .addModifiers(KModifier.PRIVATE)
            .initializer(generateSiteInitializer(site))
            .build()
    }
    
    /**
     * 生成站点初始化器
     */
    private fun generateSiteInitializer(site: VodSite): CodeBlock {
        return CodeBlock.builder()
            .add("VodSite(\n")
            .indent()
            .add("key = %S,\n", site.key)
            .add("name = %S,\n", site.name)
            .add("api = %S,\n", site.api)
            .add("ext = %S,\n", site.ext)
            .add("jar = %S,\n", site.jar)
            .add("type = %L,\n", site.type)
            .add("searchable = %L,\n", site.searchable)
            .add("changeable = %L,\n", site.changeable)
            .add("timeout = %L,\n", site.timeout)
            .add("header = mapOf(${generateMapEntries(site.header)})\n")
            .unindent()
            .add(")")
            .build()
    }
    
    /**
     * 生成性能统计属性
     */
    private fun generatePerformanceStatsProperty(): PropertySpec {
        return PropertySpec.builder("performanceStats", 
            ClassName("kotlin.collections", "MutableMap")
                .parameterizedBy(String::class.asClassName(), Long::class.asClassName()))
            .addModifiers(KModifier.PRIVATE)
            .initializer("mutableMapOf()")
            .build()
    }
    
    /**
     * 生成搜索函数
     */
    private fun generateSearchFunction(site: VodSite): FunSpec {
        return FunSpec.builder("search")
            .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
            .addParameter("keyword", String::class)
            .addParameter("page", Int::class)
            .returns(ClassName("top.cywin.onetv.movie.data.models", "VodResponse"))
            .addCode(generateSearchLogic(site))
            .build()
    }
    
    /**
     * 生成搜索逻辑
     */
    private fun generateSearchLogic(site: VodSite): CodeBlock {
        return if (site.searchable == 1) {
            CodeBlock.builder()
                .addStatement("val startTime = System.currentTimeMillis()")
                .addStatement("Log.d(\"ONETV_MOVIE\", \"🔍 开始搜索: \${siteConfig.name} - \$keyword\")")
                .addStatement("")
                .addStatement("return withContext(Dispatchers.IO) {")
                .indent()
                .addStatement("try {")
                .indent()
                .addStatement("val searchUrl = buildSearchUrl(keyword, page)")
                .addStatement("val client = createHttpClient()")
                .addStatement("val request = Request.Builder()")
                .addStatement("    .url(searchUrl)")
                .apply {
                    site.header.forEach { (key, value) ->
                        addStatement("    .addHeader(%S, %S)", key, value)
                    }
                }
                .addStatement("    .build()")
                .addStatement("")
                .addStatement("val response = withTimeoutOrNull(\${siteConfig.timeout.toLong()}) {")
                .indent()
                .addStatement("client.newCall(request).execute()")
                .unindent()
                .addStatement("}")
                .addStatement("")
                .addStatement("if (response?.isSuccessful == true) {")
                .indent()
                .addStatement("val jsonContent = response.body?.string() ?: \"\"")
                .addStatement("val result = Json.decodeFromString<VodResponse>(jsonContent)")
                .addStatement("val searchTime = System.currentTimeMillis() - startTime")
                .addStatement("performanceStats[\"search_time\"] = searchTime")
                .addStatement("Log.d(\"ONETV_MOVIE\", \"✅ 搜索成功: \${result.list?.size ?: 0} 个结果\")")
                .addStatement("result")
                .unindent()
                .addStatement("} else {")
                .indent()
                .addStatement("Log.e(\"ONETV_MOVIE\", \"❌ 搜索失败: \${response?.code}\")")
                .addStatement("VodResponse(code = 0, msg = \"搜索失败: \${response?.code}\")")
                .unindent()
                .addStatement("}")
                .unindent()
                .addStatement("} catch (e: Exception) {")
                .indent()
                .addStatement("Log.e(\"ONETV_MOVIE\", \"❌ 搜索异常: \${e.message}\")")
                .addStatement("VodResponse(code = 0, msg = \"搜索异常: \${e.message}\")")
                .unindent()
                .addStatement("}")
                .unindent()
                .addStatement("}")
                .build()
        } else {
            CodeBlock.builder()
                .addStatement("Log.w(\"ONETV_MOVIE\", \"⚠️ 站点 \${siteConfig.name} 不支持搜索\")")
                .addStatement("return VodResponse(code = 0, msg = \"站点不支持搜索\")")
                .build()
        }
    }
    
    /**
     * 生成详情函数
     */
    private fun generateDetailFunction(site: VodSite): FunSpec {
        return FunSpec.builder("getDetail")
            .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
            .addParameter("vodId", String::class)
            .returns(ClassName("top.cywin.onetv.movie.data.models", "VodResponse"))
            .addCode(generateDetailLogic(site))
            .build()
    }
    
    /**
     * 生成详情逻辑
     */
    private fun generateDetailLogic(site: VodSite): CodeBlock {
        return CodeBlock.builder()
            .addStatement("val startTime = System.currentTimeMillis()")
            .addStatement("Log.d(\"ONETV_MOVIE\", \"📄 获取详情: \${siteConfig.name} - \$vodId\")")
            .addStatement("")
            .addStatement("return withContext(Dispatchers.IO) {")
            .indent()
            .addStatement("try {")
            .indent()
            .addStatement("val detailUrl = buildDetailUrl(vodId)")
            .addStatement("val client = createHttpClient()")
            .addStatement("val request = Request.Builder()")
            .addStatement("    .url(detailUrl)")
            .apply {
                site.header.forEach { (key, value) ->
                    addStatement("    .addHeader(%S, %S)", key, value)
                }
            }
            .addStatement("    .build()")
            .addStatement("")
            .addStatement("val response = withTimeoutOrNull(\${siteConfig.timeout.toLong()}) {")
            .indent()
            .addStatement("client.newCall(request).execute()")
            .unindent()
            .addStatement("}")
            .addStatement("")
            .addStatement("if (response?.isSuccessful == true) {")
            .indent()
            .addStatement("val jsonContent = response.body?.string() ?: \"\"")
            .addStatement("val result = Json.decodeFromString<VodResponse>(jsonContent)")
            .addStatement("val detailTime = System.currentTimeMillis() - startTime")
            .addStatement("performanceStats[\"detail_time\"] = detailTime")
            .addStatement("Log.d(\"ONETV_MOVIE\", \"✅ 详情获取成功\")")
            .addStatement("result")
            .unindent()
            .addStatement("} else {")
            .indent()
            .addStatement("Log.e(\"ONETV_MOVIE\", \"❌ 详情获取失败: \${response?.code}\")")
            .addStatement("VodResponse(code = 0, msg = \"详情获取失败: \${response?.code}\")")
            .unindent()
            .addStatement("}")
            .unindent()
            .addStatement("} catch (e: Exception) {")
            .indent()
            .addStatement("Log.e(\"ONETV_MOVIE\", \"❌ 详情获取异常: \${e.message}\")")
            .addStatement("VodResponse(code = 0, msg = \"详情获取异常: \${e.message}\")")
            .unindent()
            .addStatement("}")
            .unindent()
            .addStatement("}")
            .build()
    }
    
    /**
     * 生成Map条目字符串
     */
    private fun generateMapEntries(map: Map<String, String>): String {
        return map.entries.joinToString(", ") { "\"${it.key}\" to \"${it.value}\"" }
    }
    
    /**
     * 获取站点类型名称
     */
    private fun getSiteTypeName(type: Int): String {
        return when (type) {
            0 -> "Spider"
            1 -> "CMS"
            3 -> "APP"
            4 -> "Alist"
            else -> "未知"
        }
    }
    
    // 其他辅助方法的生成...
    private fun generateCategoryFunction(site: VodSite): FunSpec = FunSpec.builder("getCategory").build()
    private fun generateHomeContentFunction(site: VodSite): FunSpec = FunSpec.builder("getHomeContent").build()
    private fun generateBuildUrlFunction(site: VodSite): FunSpec = FunSpec.builder("buildUrl").build()
    private fun generateCreateHttpClientFunction(site: VodSite): FunSpec = FunSpec.builder("createHttpClient").build()
    private fun generateHandleResponseFunction(): FunSpec = FunSpec.builder("handleResponse").build()
    private fun generateGetSiteStatsFunction(): FunSpec = FunSpec.builder("getSiteStats").build()
    private fun generateSpiderMethods(site: VodSite): FunSpec = FunSpec.builder("spiderMethod").build()
    private fun generateCmsMethods(site: VodSite): FunSpec = FunSpec.builder("cmsMethod").build()
    private fun generateAppMethods(site: VodSite): FunSpec = FunSpec.builder("appMethod").build()
    private fun generateAlistMethods(site: VodSite): FunSpec = FunSpec.builder("alistMethod").build()
    private fun generateAdaptersMapProperty(sites: List<VodSite>): PropertySpec = PropertySpec.builder("adapters", Map::class).build()
    private fun generateGetAdapterFunction(): FunSpec = FunSpec.builder("getAdapter").build()
    private fun generateGetAllAdaptersFunction(): FunSpec = FunSpec.builder("getAllAdapters").build()
    private fun generateSearchAllSitesFunction(sites: List<VodSite>): FunSpec = FunSpec.builder("searchAllSites").build()
    private fun generateGetBestSiteFunction(): FunSpec = FunSpec.builder("getBestSite").build()
}
