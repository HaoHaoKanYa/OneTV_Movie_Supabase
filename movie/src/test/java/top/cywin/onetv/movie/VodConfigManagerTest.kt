package top.cywin.onetv.movie

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import top.cywin.onetv.movie.data.VodConfigManager
import top.cywin.onetv.movie.data.cache.MovieCacheManager
import top.cywin.onetv.movie.data.models.*

/**
 * VodConfigManager单元测试
 */
class VodConfigManagerTest {

    @Mock
    private lateinit var mockCacheManager: MovieCacheManager

    private lateinit var configManager: VodConfigManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        configManager = VodConfigManager(mockCacheManager)
    }

    @Test
    fun `test load config success`() = runTest {
        // 准备测试数据
        val testConfig = VodConfigResponse(
            sites = listOf(
                VodSite(
                    key = "test_site",
                    name = "测试站点",
                    api = "https://test.com/api",
                    type = 1,
                    searchable = 1,
                    changeable = 1
                )
            ),
            parses = listOf(
                VodParse(
                    name = "测试解析器",
                    type = 1,
                    url = "https://test.com/parse"
                )
            )
        )

        // 执行测试
        val result = configManager.load(testConfig)

        // 验证结果
        assertTrue(result.isSuccess)
        assertEquals("配置加载成功", result.getOrNull())
        assertTrue(configManager.isConfigLoaded())
        assertEquals(1, configManager.getSites().size)
        assertEquals(2, configManager.getParses().size) // 包含神解析器
    }

    @Test
    fun `test get home site`() = runTest {
        // 准备测试数据
        val testSite = VodSite(
            key = "home_site",
            name = "首页站点",
            api = "https://home.com/api",
            type = 1
        )
        val testConfig = VodConfigResponse(sites = listOf(testSite))

        // 执行测试
        configManager.load(testConfig)

        // 验证结果
        val homeSite = configManager.getHomeSite()
        assertNotNull(homeSite)
        assertEquals("home_site", homeSite?.key)
        assertEquals("首页站点", homeSite?.name)
    }

    @Test
    fun `test get parse by name`() = runTest {
        // 准备测试数据
        val testParse = VodParse(
            name = "特定解析器",
            type = 2,
            url = "https://specific.com/parse"
        )
        val testConfig = VodConfigResponse(parses = listOf(testParse))

        // 执行测试
        configManager.load(testConfig)

        // 验证结果
        val foundParse = configManager.getParse("特定解析器")
        assertNotNull(foundParse)
        assertEquals("特定解析器", foundParse?.name)
        assertEquals(2, foundParse?.type)

        // 测试不存在的解析器
        val notFoundParse = configManager.getParse("不存在的解析器")
        assertNull(notFoundParse)
    }

    @Test
    fun `test switch site`() = runTest {
        // 准备测试数据
        val site1 = VodSite(key = "site1", name = "站点1", api = "https://site1.com", type = 1)
        val site2 = VodSite(key = "site2", name = "站点2", api = "https://site2.com", type = 1)
        val testConfig = VodConfigResponse(sites = listOf(site1, site2))

        // 执行测试
        configManager.load(testConfig)

        // 验证初始状态
        assertEquals("site1", configManager.getHomeSite()?.key)

        // 切换站点
        configManager.setHomeSite(site2)

        // 验证切换结果
        assertEquals("site2", configManager.getHomeSite()?.key)
    }

    @Test
    fun `test config summary`() = runTest {
        // 准备测试数据
        val testConfig = VodConfigResponse(
            sites = listOf(
                VodSite(key = "site1", name = "站点1", api = "https://site1.com", type = 1),
                VodSite(key = "site2", name = "站点2", api = "https://site2.com", type = 1)
            ),
            parses = listOf(
                VodParse(name = "解析器1", type = 1, url = "https://parse1.com"),
                VodParse(name = "解析器2", type = 2, url = "https://parse2.com")
            )
        )

        // 执行测试
        configManager.load(testConfig)

        // 验证结果
        val summary = configManager.getConfigSummary()
        assertEquals("站点: 2, 解析器: 3", summary) // 包含神解析器
    }

    @Test
    fun `test empty config`() = runTest {
        // 准备空配置
        val emptyConfig = VodConfigResponse()

        // 执行测试
        val result = configManager.load(emptyConfig)

        // 验证结果
        assertTrue(result.isSuccess)
        assertFalse(configManager.isConfigLoaded()) // 空配置不算已加载
        assertTrue(configManager.getSites().isEmpty())
        assertEquals(1, configManager.getParses().size) // 只有神解析器
    }
}
