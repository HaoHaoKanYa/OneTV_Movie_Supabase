package top.cywin.onetv.movie.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import top.cywin.onetv.movie.data.models.VodParse
import top.cywin.onetv.movie.data.models.VodSite
import top.cywin.onetv.movie.data.models.ParseResult
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 动态解析器生成器
 * 根据VodParse配置生成高性能、类型安全的解析器类
 * 
 * 支持的解析器类型：
 * - 0: 嗅探解析器 (SniffingParser)
 * - 1: JSON解析器 (JsonParser) 
 * - 2: WebView解析器 (WebViewParser)
 * - 3: 自定义解析器 (CustomParser)
 */
object ParserGenerator {
    private const val GENERATED_PACKAGE = "top.cywin.onetv.movie.generated.parser"
    
    /**
     * 生成单个解析器类
     */
    fun generateParser(parse: VodParse): FileSpec {
        val className = generateClassName(parse.name)
        
        val parserClass = TypeSpec.classBuilder(className)
            .addSuperinterface(ClassName("top.cywin.onetv.movie.data.parser", "VodParserInterface"))
            .addProperty(generateConfigProperty(parse))
            .addProperty(generatePerformanceProperty())
            .addFunction(generateParseFunction(parse))
            .addFunction(generateValidateFunction(parse))
            .addFunction(generateGetHeadersFunction(parse))
            .addFunction(generateGetTimeoutFunction(parse))
            .addFunction(generateCleanupFunction())
            .apply {
                // 根据解析器类型添加特定方法
                when (parse.type) {
                    0 -> addFunction(generateSniffingMethods())
                    1 -> addFunction(generateJsonMethods())
                    2 -> addFunction(generateWebViewMethods())
                    3 -> addFunction(generateCustomMethods(parse))
                }
            }
            .build()
        
        return FileSpec.builder(GENERATED_PACKAGE, className)
            .addImport("android.util", "Log")
            .addImport("kotlinx.coroutines", "withContext", "Dispatchers", "withTimeoutOrNull")
            .addImport("okhttp3", "OkHttpClient", "Request", "Response")
            .addImport("top.cywin.onetv.movie.data.models", "ParseResult", "VodSite", "VodParse")
            .addImport("java.net", "URLEncoder")
            .addImport("java.util.concurrent", "TimeUnit")
            .addType(parserClass)
            .addFileComment("🤖 自动生成的${getParserTypeName(parse.type)}解析器")
            .addFileComment("解析器名称: ${parse.name}")
            .addFileComment("解析器类型: ${parse.type}")
            .addFileComment("生成时间: ${java.time.LocalDateTime.now()}")
            .addFileComment("⚠️ 此文件由KotlinPoet自动生成，请勿手动修改！")
            .build()
    }
    
    /**
     * 生成解析器管理器
     */
    fun generateParserManager(parses: List<VodParse>): FileSpec {
        val managerClass = TypeSpec.classBuilder("GeneratedParserManager")
            .addSuperinterface(ClassName("top.cywin.onetv.movie.data.parser", "ParseManager"))
            .addProperty(generateParsersMapProperty(parses))
            .addFunction(generateGetParserFunction())
            .addFunction(generateGetAllParsersFunction())
            .addFunction(generateParseWithBestParserFunction())
            .addFunction(generateGetParserStatsFunction())
            .build()
        
        return FileSpec.builder(GENERATED_PACKAGE, "GeneratedParserManager")
            .addImport("android.util", "Log")
            .addImport("top.cywin.onetv.movie.data.parser", "ParseManager", "VodParserInterface")
            .addImport("top.cywin.onetv.movie.data.models", "VodParse", "ParseResult")
            .addType(managerClass)
            .addFileComment("🤖 自动生成的解析器管理器")
            .addFileComment("管理 ${parses.size} 个解析器")
            .build()
    }
    
    /**
     * 生成类名（清理特殊字符）
     */
    private fun generateClassName(name: String): String {
        return name.replace(Regex("[^A-Za-z0-9]"), "") + "Parser"
    }
    
    /**
     * 生成配置属性
     */
    private fun generateConfigProperty(parse: VodParse): PropertySpec {
        return PropertySpec.builder("config", ClassName("top.cywin.onetv.movie.data.models", "VodParse"))
            .addModifiers(KModifier.PRIVATE)
            .initializer(
                CodeBlock.builder()
                    .add("VodParse(\n")
                    .indent()
                    .add("name = %S,\n", parse.name)
                    .add("type = %L,\n", parse.type)
                    .add("url = %S,\n", parse.url)
                    .add("ext = mapOf(${generateMapEntries(parse.ext)}),\n")
                    .add("header = mapOf(${generateMapEntries(parse.header)})\n")
                    .unindent()
                    .add(")")
                    .build()
            )
            .build()
    }
    
    /**
     * 生成性能监控属性
     */
    private fun generatePerformanceProperty(): PropertySpec {
        return PropertySpec.builder("performanceStats", 
            ClassName("kotlin.collections", "MutableMap")
                .parameterizedBy(String::class.asClassName(), Long::class.asClassName()))
            .addModifiers(KModifier.PRIVATE)
            .initializer("mutableMapOf()")
            .build()
    }
    
    /**
     * 生成主解析函数
     */
    private fun generateParseFunction(parse: VodParse): FunSpec {
        return FunSpec.builder("parse")
            .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
            .addParameter("url", String::class)
            .addParameter("site", ClassName("top.cywin.onetv.movie.data.models", "VodSite"))
            .addParameter("flags", String::class, KModifier.VARARG)
            .returns(ClassName("top.cywin.onetv.movie.data.models", "ParseResult"))
            .addCode(generateParseLogic(parse))
            .build()
    }
    
    /**
     * 生成解析逻辑
     */
    private fun generateParseLogic(parse: VodParse): CodeBlock {
        return CodeBlock.builder()
            .addStatement("val startTime = System.currentTimeMillis()")
            .addStatement("Log.d(\"ONETV_MOVIE\", \"🔧 开始解析: \${config.name} - \$url\")")
            .addStatement("")
            .addStatement("return withContext(Dispatchers.IO) {")
            .indent()
            .addStatement("try {")
            .indent()
            .add(generateSpecificParseLogic(parse))
            .unindent()
            .addStatement("} catch (e: Exception) {")
            .indent()
            .addStatement("val parseTime = System.currentTimeMillis() - startTime")
            .addStatement("Log.e(\"ONETV_MOVIE\", \"❌ 解析失败: \${config.name} - \${e.message}\")")
            .addStatement("ParseResult.failure(\"解析异常: \${e.message}\", parseTime)")
            .unindent()
            .addStatement("}")
            .unindent()
            .addStatement("}")
            .build()
    }
    
    /**
     * 根据解析器类型生成特定解析逻辑
     */
    private fun generateSpecificParseLogic(parse: VodParse): CodeBlock {
        return when (parse.type) {
            0 -> generateSniffingLogic(parse)
            1 -> generateJsonLogic(parse)
            2 -> generateWebViewLogic(parse)
            3 -> generateCustomLogic(parse)
            else -> CodeBlock.of("ParseResult.failure(\"未知解析器类型: ${parse.type}\", 0)")
        }
    }
    
    /**
     * 生成嗅探解析逻辑
     */
    private fun generateSniffingLogic(parse: VodParse): CodeBlock {
        return CodeBlock.builder()
            .addStatement("// 🔍 嗅探解析逻辑")
            .addStatement("val client = createHttpClient()")
            .addStatement("val request = buildRequest(url, site)")
            .addStatement("")
            .addStatement("val response = withTimeoutOrNull(30000) {")
            .indent()
            .addStatement("client.newCall(request).execute()")
            .unindent()
            .addStatement("}")
            .addStatement("")
            .addStatement("if (response?.isSuccessful == true) {")
            .indent()
            .addStatement("val content = response.body?.string() ?: \"\"")
            .addStatement("val videoUrl = sniffVideoUrl(content)")
            .addStatement("val parseTime = System.currentTimeMillis() - startTime")
            .addStatement("")
            .addStatement("if (videoUrl.isNotEmpty()) {")
            .indent()
            .addStatement("Log.d(\"ONETV_MOVIE\", \"✅ 嗅探解析成功: \$videoUrl\")")
            .addStatement("ParseResult.success(videoUrl, parseTime)")
            .unindent()
            .addStatement("} else {")
            .indent()
            .addStatement("ParseResult.failure(\"嗅探解析未找到视频链接\", parseTime)")
            .unindent()
            .addStatement("}")
            .unindent()
            .addStatement("} else {")
            .indent()
            .addStatement("val parseTime = System.currentTimeMillis() - startTime")
            .addStatement("ParseResult.failure(\"网络请求失败: \${response?.code}\", parseTime)")
            .unindent()
            .addStatement("}")
            .build()
    }
    
    /**
     * 生成JSON解析逻辑
     */
    private fun generateJsonLogic(parse: VodParse): CodeBlock {
        return CodeBlock.builder()
            .addStatement("// 📄 JSON解析逻辑")
            .addStatement("val parseUrl = if (config.url.isNotEmpty()) {")
            .indent()
            .addStatement("val encodedUrl = URLEncoder.encode(url, \"UTF-8\")")
            .addStatement("if (config.url.contains(\"?\")) {")
            .indent()
            .addStatement("config.url + \"&url=\" + encodedUrl")
            .unindent()
            .addStatement("} else {")
            .indent()
            .addStatement("config.url + \"?url=\" + encodedUrl")
            .unindent()
            .addStatement("}")
            .unindent()
            .addStatement("} else url")
            .addStatement("")
            .addStatement("val client = createHttpClient()")
            .addStatement("val request = buildRequest(parseUrl, site)")
            .addStatement("")
            .addStatement("val response = withTimeoutOrNull(30000) {")
            .indent()
            .addStatement("client.newCall(request).execute()")
            .unindent()
            .addStatement("}")
            .addStatement("")
            .addStatement("if (response?.isSuccessful == true) {")
            .indent()
            .addStatement("val jsonContent = response.body?.string() ?: \"\"")
            .addStatement("val playUrl = parseJsonForPlayUrl(jsonContent)")
            .addStatement("val parseTime = System.currentTimeMillis() - startTime")
            .addStatement("")
            .addStatement("if (playUrl.isNotEmpty()) {")
            .indent()
            .addStatement("Log.d(\"ONETV_MOVIE\", \"✅ JSON解析成功: \$playUrl\")")
            .addStatement("ParseResult.success(playUrl, parseTime)")
            .unindent()
            .addStatement("} else {")
            .indent()
            .addStatement("ParseResult.failure(\"JSON解析未找到播放地址\", parseTime)")
            .unindent()
            .addStatement("}")
            .unindent()
            .addStatement("} else {")
            .indent()
            .addStatement("val parseTime = System.currentTimeMillis() - startTime")
            .addStatement("ParseResult.failure(\"网络请求失败: \${response?.code}\", parseTime)")
            .unindent()
            .addStatement("}")
            .build()
    }
    
    /**
     * 生成WebView解析逻辑
     */
    private fun generateWebViewLogic(parse: VodParse): CodeBlock {
        return CodeBlock.builder()
            .addStatement("// 🌐 WebView解析逻辑")
            .addStatement("Log.w(\"ONETV_MOVIE\", \"⚠️ WebView解析需要在主线程中执行\")")
            .addStatement("val parseTime = System.currentTimeMillis() - startTime")
            .addStatement("ParseResult.failure(\"WebView解析暂不支持\", parseTime)")
            .build()
    }
    
    /**
     * 生成自定义解析逻辑
     */
    private fun generateCustomLogic(parse: VodParse): CodeBlock {
        return CodeBlock.builder()
            .addStatement("// 🔧 自定义解析逻辑")
            .addStatement("val customLogic = config.ext[\"logic\"] ?: \"\"")
            .addStatement("val parseTime = System.currentTimeMillis() - startTime")
            .addStatement("ParseResult.failure(\"自定义解析逻辑待实现: \$customLogic\", parseTime)")
            .build()
    }
    
    /**
     * 生成Map条目字符串
     */
    private fun generateMapEntries(map: Map<String, String>): String {
        return map.entries.joinToString(", ") { "\"${it.key}\" to \"${it.value}\"" }
    }
    
    /**
     * 获取解析器类型名称
     */
    private fun getParserTypeName(type: Int): String {
        return when (type) {
            0 -> "嗅探"
            1 -> "JSON"
            2 -> "WebView"
            3 -> "自定义"
            else -> "未知"
        }
    }
    
    // 其他辅助方法的生成...
    private fun generateSniffingMethods(): FunSpec = FunSpec.builder("sniffVideoUrl").build()
    private fun generateJsonMethods(): FunSpec = FunSpec.builder("parseJsonForPlayUrl").build()
    private fun generateWebViewMethods(): FunSpec = FunSpec.builder("webViewParse").build()
    private fun generateCustomMethods(parse: VodParse): FunSpec = FunSpec.builder("customParse").build()
    private fun generateValidateFunction(parse: VodParse): FunSpec = FunSpec.builder("validate").build()
    private fun generateGetHeadersFunction(parse: VodParse): FunSpec = FunSpec.builder("getHeaders").build()
    private fun generateGetTimeoutFunction(parse: VodParse): FunSpec = FunSpec.builder("getTimeout").build()
    private fun generateCleanupFunction(): FunSpec = FunSpec.builder("cleanup").build()
    private fun generateParsersMapProperty(parses: List<VodParse>): PropertySpec = PropertySpec.builder("parsers", Map::class).build()
    private fun generateGetParserFunction(): FunSpec = FunSpec.builder("getParser").build()
    private fun generateGetAllParsersFunction(): FunSpec = FunSpec.builder("getAllParsers").build()
    private fun generateParseWithBestParserFunction(): FunSpec = FunSpec.builder("parseWithBestParser").build()
    private fun generateGetParserStatsFunction(): FunSpec = FunSpec.builder("getParserStats").build()
}
