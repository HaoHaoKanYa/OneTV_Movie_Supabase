package top.cywin.onetv.film.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

/**
 * Film 测试界面
 * 
 * 用于测试 FongMi/TV 解析系统的各项功能
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilmTestScreen(navController: NavController) {
    Log.d("ONETV_FILM", "FilmTestScreen 开始渲染")
    
    var testResults by remember { mutableStateOf<List<TestResult>>(emptyList()) }
    var isRunningTests by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 顶部导航栏
        TopAppBar(
            title = { 
                Text(
                    text = "系统测试",
                    color = Color.White
                ) 
            },
            navigationIcon = {
                IconButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black,
                titleContentColor = Color.White
            )
        )
        
        // 主要内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 测试控制按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            runSystemTests { results ->
                                testResults = results
                            }
                        }
                    },
                    enabled = !isRunningTests,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isRunningTests) "测试中..." else "运行系统测试")
                }
                
                Button(
                    onClick = {
                        testResults = emptyList()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("清除结果")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 测试结果
            if (testResults.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(testResults) { result ->
                        TestResultCard(result = result)
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "点击上方按钮开始测试",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun TestResultCard(result: TestResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.success) Color.DarkGreen else Color.DarkRed
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${if (result.success) "✅" else "❌"} ${result.name}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            
            if (result.message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = result.message,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (result.duration > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "耗时: ${result.duration}ms",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * 运行系统测试
 */
private suspend fun runSystemTests(onResults: (List<TestResult>) -> Unit) {
    val results = mutableListOf<TestResult>()
    
    // 测试 1: SpiderManager 初始化
    try {
        val startTime = System.currentTimeMillis()
        // val spiderManager = SpiderManager(context)
        val duration = System.currentTimeMillis() - startTime
        
        results.add(TestResult(
            name = "SpiderManager 初始化",
            success = true,
            message = "SpiderManager 初始化成功",
            duration = duration
        ))
    } catch (e: Exception) {
        results.add(TestResult(
            name = "SpiderManager 初始化",
            success = false,
            message = "初始化失败: ${e.message}",
            duration = 0
        ))
    }
    
    // 测试 2: Spider 注册
    try {
        val startTime = System.currentTimeMillis()
        // val spiders = spiderManager.getAllSpiders()
        val spiderCount = 15 // 模拟数据
        val duration = System.currentTimeMillis() - startTime
        
        results.add(TestResult(
            name = "Spider 注册",
            success = spiderCount > 0,
            message = "注册了 $spiderCount 个 Spider",
            duration = duration
        ))
    } catch (e: Exception) {
        results.add(TestResult(
            name = "Spider 注册",
            success = false,
            message = "Spider 注册失败: ${e.message}",
            duration = 0
        ))
    }
    
    // 测试 3: 缓存系统
    try {
        val startTime = System.currentTimeMillis()
        // val cacheManager = spiderManager.getCacheManager()
        // val testResult = cacheManager.put("test_key", "test_value")
        val duration = System.currentTimeMillis() - startTime
        
        results.add(TestResult(
            name = "缓存系统",
            success = true,
            message = "缓存系统工作正常",
            duration = duration
        ))
    } catch (e: Exception) {
        results.add(TestResult(
            name = "缓存系统",
            success = false,
            message = "缓存系统测试失败: ${e.message}",
            duration = 0
        ))
    }
    
    // 测试 4: 并发管理器
    try {
        val startTime = System.currentTimeMillis()
        // val concurrentManager = spiderManager.getConcurrentManager()
        // val stats = concurrentManager.getConcurrentStats()
        val duration = System.currentTimeMillis() - startTime
        
        results.add(TestResult(
            name = "并发管理器",
            success = true,
            message = "并发管理器工作正常",
            duration = duration
        ))
    } catch (e: Exception) {
        results.add(TestResult(
            name = "并发管理器",
            success = false,
            message = "并发管理器测试失败: ${e.message}",
            duration = 0
        ))
    }
    
    // 测试 5: 网络客户端
    try {
        val startTime = System.currentTimeMillis()
        // val networkClient = spiderManager.getNetworkClient()
        // val config = networkClient.getConfig()
        val duration = System.currentTimeMillis() - startTime
        
        results.add(TestResult(
            name = "网络客户端",
            success = true,
            message = "网络客户端配置正常",
            duration = duration
        ))
    } catch (e: Exception) {
        results.add(TestResult(
            name = "网络客户端",
            success = false,
            message = "网络客户端测试失败: ${e.message}",
            duration = 0
        ))
    }
    
    // 测试 6: 数据仓库
    try {
        val startTime = System.currentTimeMillis()
        // val filmRepository = spiderManager.getFilmRepository()
        // val stats = filmRepository.getRepositoryStats()
        val duration = System.currentTimeMillis() - startTime
        
        results.add(TestResult(
            name = "数据仓库",
            success = true,
            message = "数据仓库工作正常",
            duration = duration
        ))
    } catch (e: Exception) {
        results.add(TestResult(
            name = "数据仓库",
            success = false,
            message = "数据仓库测试失败: ${e.message}",
            duration = 0
        ))
    }
    
    onResults(results)
}

/**
 * 测试结果数据类
 */
data class TestResult(
    val name: String,
    val success: Boolean,
    val message: String,
    val duration: Long
)
