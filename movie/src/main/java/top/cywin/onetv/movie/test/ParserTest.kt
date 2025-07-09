package top.cywin.onetv.movie.test

import kotlinx.coroutines.runBlocking
import top.cywin.onetv.movie.data.parser.*
import top.cywin.onetv.movie.data.models.VodSite
import top.cywin.onetv.movie.data.models.VodParse

/**
 * 播放地址解析器测试
 */
class ParserTest {

    private val parserManager = VodParserManager()
    
    // 测试站点
    private val testSite = VodSite(
        key = "test_site",
        name = "测试站点",
        type = 0,
        api = "https://example.com/api.php/provide/vod/",
        searchable = 1,
        changeable = 1,
        ext = "",
        jar = "",
        categories = emptyList(),
        header = emptyMap()
    )

    /**
     * 测试嗅探解析器
     */
    fun testSniffParser() = runBlocking {
        println("=== 测试嗅探解析器 ===")
        
        val testUrls = listOf(
            "https://example.com/video.m3u8",
            "https://example.com/video.mp4",
            "https://example.com/play.php?url=test",
            "https://example.com/stream/video.flv"
        )
        
        val sniffParser = SniffParser()
        
        testUrls.forEach { url ->
            try {
                println("测试URL: $url")
                val result = sniffParser.parse(url, testSite)
                
                if (result.success) {
                    println("✅ 解析成功: ${result.playUrl}")
                    println("   解析时间: ${result.parseTime}ms")
                } else {
                    println("❌ 解析失败: ${result.error}")
                }
                
                println("   支持该URL: ${sniffParser.canParse(url, testSite)}")
                println()
                
            } catch (e: Exception) {
                println("❌ 解析异常: ${e.message}")
                println()
            }
        }
    }

    /**
     * 测试JSON解析器
     */
    fun testJsonParser() = runBlocking {
        println("=== 测试JSON解析器 ===")
        
        val testUrls = listOf(
            "https://api.example.com/parse.json?url=test",
            "https://example.com/api/video.json",
            "https://parse.example.com/json?v=123"
        )
        
        val jsonParser = JsonParser()
        
        testUrls.forEach { url ->
            try {
                println("测试URL: $url")
                val result = jsonParser.parse(url, testSite)
                
                if (result.success) {
                    println("✅ 解析成功: ${result.playUrl}")
                } else {
                    println("❌ 解析失败: ${result.error}")
                }
                
                println("   支持该URL: ${jsonParser.canParse(url, testSite)}")
                println()
                
            } catch (e: Exception) {
                println("❌ 解析异常: ${e.message}")
                println()
            }
        }
    }

    /**
     * 测试WebView解析器
     */
    fun testWebViewParser() = runBlocking {
        println("=== 测试WebView解析器 ===")
        
        val testUrls = listOf(
            "https://example.com/player.html?v=123",
            "https://player.example.com/embed/456",
            "https://example.com/watch?id=789"
        )
        
        val webViewParser = WebViewParser()
        
        testUrls.forEach { url ->
            try {
                println("测试URL: $url")
                val result = webViewParser.parse(url, testSite)
                
                if (result.success) {
                    println("✅ 解析成功: ${result.playUrl}")
                } else {
                    println("❌ 解析失败: ${result.error}")
                }
                
                println("   支持该URL: ${webViewParser.canParse(url, testSite)}")
                println()
                
            } catch (e: Exception) {
                println("❌ 解析异常: ${e.message}")
                println()
            }
        }
    }

    /**
     * 测试自定义解析器
     */
    fun testCustomParser() = runBlocking {
        println("=== 测试自定义解析器 ===")
        
        val testUrls = listOf(
            "https://v.qq.com/x/page/123.html",
            "https://www.iqiyi.com/v_456.html",
            "https://v.youku.com/v_show/id_789.html",
            "https://www.bilibili.com/video/BV123456"
        )
        
        val customParser = CustomParser()
        
        testUrls.forEach { url ->
            try {
                println("测试URL: $url")
                val result = customParser.parse(url, testSite)
                
                if (result.success) {
                    println("✅ 解析成功: ${result.playUrl}")
                } else {
                    println("❌ 解析失败: ${result.error}")
                }
                
                println("   支持该URL: ${customParser.canParse(url, testSite)}")
                println()
                
            } catch (e: Exception) {
                println("❌ 解析异常: ${e.message}")
                println()
            }
        }
    }

    /**
     * 测试解析器管理器
     */
    fun testParserManager() = runBlocking {
        println("=== 测试解析器管理器 ===")
        
        val testUrls = listOf(
            "https://example.com/video.m3u8",
            "https://api.example.com/parse.json?url=test",
            "https://player.example.com/embed/123",
            "https://v.qq.com/x/page/456.html"
        )
        
        testUrls.forEach { url ->
            try {
                println("测试URL: $url")
                
                // 获取支持的解析器
                val supportedParsers = parserManager.getSupportedParsers(url, testSite)
                println("   支持的解析器: ${supportedParsers.map { it.getType().name }}")
                
                // 尝试解析
                val result = parserManager.parsePlayUrl(url, testSite)
                
                if (result.success) {
                    println("✅ 解析成功: ${result.playUrl}")
                    println("   解析时间: ${result.parseTime}ms")
                    if (result.headers.isNotEmpty()) {
                        println("   请求头: ${result.headers}")
                    }
                } else {
                    println("❌ 解析失败: ${result.error}")
                }
                println()
                
            } catch (e: Exception) {
                println("❌ 解析异常: ${e.message}")
                println()
            }
        }
    }

    /**
     * 测试解析器性能
     */
    fun testParserPerformance() = runBlocking {
        println("=== 测试解析器性能 ===")
        
        val testUrl = "https://example.com/video.m3u8"
        val iterations = 10
        
        val parsers = listOf(
            SniffParser(),
            JsonParser(),
            WebViewParser(),
            CustomParser()
        )
        
        parsers.forEach { parser ->
            if (parser.canParse(testUrl, testSite)) {
                val startTime = System.currentTimeMillis()
                
                repeat(iterations) {
                    parser.parse(testUrl, testSite)
                }
                
                val totalTime = System.currentTimeMillis() - startTime
                val avgTime = totalTime / iterations
                
                println("${parser.getType().name} 解析器:")
                println("   总时间: ${totalTime}ms")
                println("   平均时间: ${avgTime}ms")
                println()
            }
        }
    }

    /**
     * 运行所有测试
     */
    fun runAllTests() {
        println("🚀 开始播放地址解析器测试...")
        println()
        
        testSniffParser()
        testJsonParser()
        testWebViewParser()
        testCustomParser()
        testParserManager()
        testParserPerformance()
        
        println("✅ 播放地址解析器测试完成!")
    }
}
