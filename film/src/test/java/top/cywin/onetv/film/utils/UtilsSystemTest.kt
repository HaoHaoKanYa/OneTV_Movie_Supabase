package top.cywin.onetv.film.utils

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.util.*

/**
 * 工具类系统完整测试
 * 
 * 测试第八阶段实现的所有工具类功能
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class UtilsSystemTest {
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = mockk<Context>(relaxed = true)
    }
    
    @Test
    fun testStringUtils() {
        // 测试空字符串检查
        assertTrue(StringUtils.isEmpty(null))
        assertTrue(StringUtils.isEmpty(""))
        assertTrue(StringUtils.isEmpty("   "))
        assertFalse(StringUtils.isEmpty("test"))
        
        assertTrue(StringUtils.isNotEmpty("test"))
        assertFalse(StringUtils.isNotEmpty(null))
        assertFalse(StringUtils.isNotEmpty(""))
        
        // 测试安全获取
        assertEquals("", StringUtils.safe(null))
        assertEquals("test", StringUtils.safe("test"))
        
        // 测试字符串截取
        assertEquals("", StringUtils.substring(null, 0, 5))
        assertEquals("test", StringUtils.substring("testing", 0, 4))
        assertEquals("ing", StringUtils.substring("testing", 4))
        assertEquals("", StringUtils.substring("test", 10, 20))
        
        // 测试字符串查找
        assertEquals("world", StringUtils.substringBetween("hello world test", "hello ", " test"))
        assertEquals("", StringUtils.substringBetween("hello world", "hi", "bye"))
        
        val results = StringUtils.substringBetweenAll("a[1]b[2]c[3]d", "[", "]")
        assertEquals(3, results.size)
        assertEquals("1", results[0])
        assertEquals("2", results[1])
        assertEquals("3", results[2])
        
        // 测试正则表达式
        assertEquals("123", StringUtils.findByRegex("abc123def", "\\d+"))
        assertEquals("", StringUtils.findByRegex("abcdef", "\\d+"))
        
        val numbers = StringUtils.findAllByRegex("a1b2c3d", "\\d")
        assertEquals(3, numbers.size)
        assertEquals("1", numbers[0])
        assertEquals("2", numbers[1])
        assertEquals("3", numbers[2])
        
        // 测试字符串替换
        assertEquals("hello world", StringUtils.replace("hello test", "test", "world"))
        assertEquals("hello 123", StringUtils.replaceByRegex("hello abc", "[a-z]+$", "123"))
        
        // 测试字符串清理
        assertEquals("hello world", StringUtils.clean("  <p>hello</p> &nbsp; world  "))
        
        // 测试编码解码
        assertEquals("hello%20world", StringUtils.urlEncode("hello world"))
        assertEquals("hello world", StringUtils.urlDecode("hello%20world"))
        
        assertFalse(StringUtils.base64Encode("hello").isEmpty())
        assertEquals("hello", StringUtils.base64Decode(StringUtils.base64Encode("hello")))
        
        // 测试哈希
        assertFalse(StringUtils.md5("hello").isEmpty())
        assertEquals(32, StringUtils.md5("hello").length)
        assertFalse(StringUtils.sha256("hello").isEmpty())
        assertEquals(64, StringUtils.sha256("hello").length)
        
        // 测试 AES 加密解密
        val key = "1234567890123456" // 16字节密钥
        val encrypted = StringUtils.aesEncrypt("hello", key)
        assertFalse(encrypted.isEmpty())
        assertEquals("hello", StringUtils.aesDecrypt(encrypted, key))
        
        // 测试时间格式化
        val timestamp = System.currentTimeMillis()
        val formatted = StringUtils.formatTimestamp(timestamp)
        assertFalse(formatted.isEmpty())
        assertTrue(formatted.contains("-"))
        assertTrue(formatted.contains(":"))
        
        // 测试时间解析
        val parsed = StringUtils.parseTimestamp("2023-01-01 12:00:00")
        assertTrue(parsed > 0)
        
        // 测试文件大小格式化
        assertEquals("1.0KB", StringUtils.formatFileSize(1024))
        assertEquals("1.0MB", StringUtils.formatFileSize(1024 * 1024))
        assertEquals("1.0GB", StringUtils.formatFileSize(1024 * 1024 * 1024))
        
        // 测试持续时间格式化
        assertEquals("1:00", StringUtils.formatDuration(60000))
        assertEquals("1:01:00", StringUtils.formatDuration(3660000))
        
        // 测试 URL 验证
        assertTrue(StringUtils.isValidUrl("https://example.com"))
        assertTrue(StringUtils.isValidUrl("http://test.org/path"))
        assertFalse(StringUtils.isValidUrl("not-a-url"))
        assertFalse(StringUtils.isValidUrl("ftp://example.com"))
        
        // 测试邮箱验证
        assertTrue(StringUtils.isValidEmail("test@example.com"))
        assertTrue(StringUtils.isValidEmail("user.name+tag@domain.co.uk"))
        assertFalse(StringUtils.isValidEmail("invalid-email"))
        assertFalse(StringUtils.isValidEmail("@example.com"))
        
        // 测试数字检查
        assertTrue(StringUtils.isNumeric("123"))
        assertTrue(StringUtils.isNumeric("123.45"))
        assertTrue(StringUtils.isNumeric("-123"))
        assertFalse(StringUtils.isNumeric("abc"))
        assertFalse(StringUtils.isNumeric("12a3"))
        
        // 测试安全转换
        assertEquals(123, StringUtils.toInt("123"))
        assertEquals(0, StringUtils.toInt("abc"))
        assertEquals(999, StringUtils.toInt("abc", 999))
        
        assertEquals(123L, StringUtils.toLong("123"))
        assertEquals(0L, StringUtils.toLong("abc"))
        assertEquals(999L, StringUtils.toLong("abc", 999L))
        
        assertEquals(123.45, StringUtils.toDouble("123.45"), 0.01)
        assertEquals(0.0, StringUtils.toDouble("abc"), 0.01)
        assertEquals(999.0, StringUtils.toDouble("abc", 999.0), 0.01)
        
        // 测试随机字符串
        val random1 = StringUtils.randomString(10)
        val random2 = StringUtils.randomString(10)
        assertEquals(10, random1.length)
        assertEquals(10, random2.length)
        assertNotEquals(random1, random2)
        
        // 测试字符串相似度
        assertEquals(1.0, StringUtils.similarity("hello", "hello"), 0.01)
        assertEquals(0.0, StringUtils.similarity("hello", "world"), 0.01)
        assertTrue(StringUtils.similarity("hello", "hallo") > 0.5)
    }
    
    @Test
    fun testJsonUtils() {
        // 测试 JSON 验证
        assertTrue(JsonUtils.isValidJson("{\"key\": \"value\"}"))
        assertTrue(JsonUtils.isValidJson("[1, 2, 3]"))
        assertFalse(JsonUtils.isValidJson("invalid json"))
        assertFalse(JsonUtils.isValidJson(null))
        
        // 测试 JSON 解析
        val jsonStr = """{"name": "test", "age": 25, "active": true}"""
        val jsonObject = JsonUtils.parseToJsonObject(jsonStr)
        assertNotNull(jsonObject)
        
        assertEquals("test", JsonUtils.getString(jsonObject, "name"))
        assertEquals(25, JsonUtils.getInt(jsonObject, "age"))
        assertTrue(JsonUtils.getBoolean(jsonObject, "active"))
        assertEquals("", JsonUtils.getString(jsonObject, "missing"))
        assertEquals(0, JsonUtils.getInt(jsonObject, "missing"))
        assertEquals(false, JsonUtils.getBoolean(jsonObject, "missing"))
        
        // 测试 JSON 数组解析
        val arrayStr = """["item1", "item2", "item3"]"""
        val jsonArray = JsonUtils.parseToJsonArray(arrayStr)
        assertNotNull(jsonArray)
        
        val stringList = JsonUtils.getStringList(jsonArray)
        assertEquals(3, stringList.size)
        assertEquals("item1", stringList[0])
        assertEquals("item2", stringList[1])
        assertEquals("item3", stringList[2])
        
        // 测试路径获取值
        val nestedJson = """{"data": {"list": [{"name": "test1"}, {"name": "test2"}]}}"""
        val nestedElement = JsonUtils.parseToJsonElement(nestedJson)
        assertEquals("test1", JsonUtils.getValueByPath(nestedElement, "data.list.0.name"))
        assertEquals("test2", JsonUtils.getValueByPath(nestedElement, "data.list.1.name"))
        assertEquals("", JsonUtils.getValueByPath(nestedElement, "data.missing.path"))
        
        // 测试 JSON 构建
        val builtObject = JsonUtils.buildJsonObject {
            put("name", "test")
            put("age", 25)
            put("active", true)
        }
        assertEquals("test", JsonUtils.getString(builtObject, "name"))
        assertEquals(25, JsonUtils.getInt(builtObject, "age"))
        assertTrue(JsonUtils.getBoolean(builtObject, "active"))
        
        val builtArray = JsonUtils.buildJsonArray {
            add("item1")
            add("item2")
            add("item3")
        }
        assertEquals(3, builtArray.size)
        
        // 测试 JSON 合并
        val obj1 = JsonUtils.buildJsonObject {
            put("a", 1)
            put("b", 2)
        }
        val obj2 = JsonUtils.buildJsonObject {
            put("b", 3)
            put("c", 4)
        }
        val merged = JsonUtils.mergeJsonObjects(obj1, obj2)
        assertEquals(1, JsonUtils.getInt(merged, "a"))
        assertEquals(3, JsonUtils.getInt(merged, "b")) // obj2 覆盖 obj1
        assertEquals(4, JsonUtils.getInt(merged, "c"))
        
        // 测试路径查找
        val searchJson = """{"users": [{"name": "john"}, {"name": "jane"}], "admin": {"name": "john"}}"""
        val searchElement = JsonUtils.parseToJsonElement(searchJson)
        val paths = JsonUtils.findPaths(searchElement, "john")
        assertTrue(paths.contains("users.0.name"))
        assertTrue(paths.contains("admin.name"))
        
        // 测试 JSON 统计
        val stats = JsonUtils.getJsonStats(jsonObject)
        assertEquals("object", stats["type"])
        assertTrue((stats["size"] as Int) > 0)
        assertTrue((stats["depth"] as Int) >= 1)
        
        // 测试 JSON 清理
        val dirtyJson = JsonUtils.buildJsonObject {
            put("valid", "data")
            put("empty", "")
            put("null_value", "")
        }
        val cleaned = JsonUtils.cleanJson(dirtyJson)
        assertNotNull(cleaned)
    }
    
    @Test
    fun testFileUtils() = runBlocking {
        // 创建临时测试文件
        val tempDir = File(System.getProperty("java.io.tmpdir"), "onetv_test")
        val testFile = File(tempDir, "test.txt")
        val testContent = "Hello, World!"
        
        try {
            // 测试目录创建
            assertTrue(FileUtils.createDir(tempDir))
            assertTrue(tempDir.exists())
            assertTrue(tempDir.isDirectory)
            
            // 测试文件存在检查
            assertFalse(FileUtils.exists(testFile))
            
            // 测试文件写入
            assertTrue(FileUtils.writeStringToFile(testFile, testContent))
            assertTrue(FileUtils.exists(testFile))
            
            // 测试文件读取
            val readContent = FileUtils.readFileToString(testFile)
            assertEquals(testContent, readContent)
            
            val readBytes = FileUtils.readFileToBytes(testFile)
            assertArrayEquals(testContent.toByteArray(), readBytes)
            
            // 测试文件大小
            val fileSize = FileUtils.getFileSize(testFile)
            assertEquals(testContent.length.toLong(), fileSize)
            
            // 测试文件复制
            val copyFile = File(tempDir, "copy.txt")
            assertTrue(FileUtils.copyFile(testFile, copyFile))
            assertTrue(FileUtils.exists(copyFile))
            assertEquals(testContent, FileUtils.readFileToString(copyFile))
            
            // 测试文件移动
            val moveFile = File(tempDir, "moved.txt")
            assertTrue(FileUtils.moveFile(copyFile, moveFile))
            assertFalse(FileUtils.exists(copyFile))
            assertTrue(FileUtils.exists(moveFile))
            
            // 测试文件列表
            val files = FileUtils.listFiles(tempDir)
            assertTrue(files.size >= 2) // testFile 和 moveFile
            
            val txtFiles = FileUtils.listFiles(tempDir, ".txt")
            assertTrue(txtFiles.size >= 2)
            
            // 测试文件哈希
            val hash = FileUtils.calculateFileHash(testFile)
            assertFalse(hash.isEmpty())
            assertEquals(32, hash.length) // MD5 长度
            
            val sha256Hash = FileUtils.calculateFileHash(testFile, "SHA-256")
            assertFalse(sha256Hash.isEmpty())
            assertEquals(64, sha256Hash.length) // SHA-256 长度
            
            // 测试目录大小
            val dirSize = FileUtils.getDirSize(tempDir)
            assertTrue(dirSize > 0)
            
            // 测试文件信息
            val fileInfo = FileUtils.getFileInfo(testFile)
            assertEquals(testFile.name, fileInfo["name"])
            assertEquals(testFile.absolutePath, fileInfo["path"])
            assertTrue(fileInfo["exists"] as Boolean)
            assertTrue(fileInfo["is_file"] as Boolean)
            assertFalse(fileInfo["is_directory"] as Boolean)
            assertEquals(testContent.length.toLong(), fileInfo["size"])
            
            // 测试文件查找
            val foundFiles = FileUtils.findFiles(tempDir, namePattern = "test")
            assertTrue(foundFiles.isNotEmpty())
            assertTrue(foundFiles.any { it.name.contains("test") })
            
        } finally {
            // 清理测试文件
            FileUtils.deleteDir(tempDir)
        }
    }
    
    @Test
    fun testDateTimeUtils() {
        // 测试当前时间
        val currentTimestamp = DateTimeUtils.getCurrentTimestamp()
        assertTrue(currentTimestamp > 0)
        
        val currentSeconds = DateTimeUtils.getCurrentTimestampSeconds()
        assertTrue(currentSeconds > 0)
        assertEquals(currentTimestamp / 1000, currentSeconds)
        
        // 测试时间格式化
        val timestamp = 1640995200000L // 2022-01-01 00:00:00 UTC
        val formatted = DateTimeUtils.formatTimestamp(timestamp)
        assertTrue(formatted.contains("2022") || formatted.contains("2021")) // 考虑时区差异
        
        val currentFormatted = DateTimeUtils.formatCurrentTime()
        assertFalse(currentFormatted.isEmpty())
        assertTrue(currentFormatted.contains("-"))
        assertTrue(currentFormatted.contains(":"))
        
        // 测试时间解析
        val parsed = DateTimeUtils.parseTimestamp("2022-01-01 12:00:00")
        assertTrue(parsed > 0)
        
        val smartParsed = DateTimeUtils.parseTimestampSmart("2022-01-01 12:00:00")
        assertTrue(smartParsed > 0)
        
        val timestampParsed = DateTimeUtils.parseTimestampSmart("1640995200")
        assertEquals(1640995200000L, timestampParsed)
        
        // 测试今天时间范围
        val todayStart = DateTimeUtils.getTodayStart()
        val todayEnd = DateTimeUtils.getTodayEnd()
        assertTrue(todayEnd > todayStart)
        assertTrue(DateTimeUtils.isToday(currentTimestamp))
        
        // 测试昨天时间范围
        val yesterdayStart = DateTimeUtils.getYesterdayStart()
        val yesterdayEnd = DateTimeUtils.getYesterdayEnd()
        assertTrue(yesterdayEnd > yesterdayStart)
        assertTrue(yesterdayEnd < todayStart)
        
        // 测试明天时间范围
        val tomorrowStart = DateTimeUtils.getTomorrowStart()
        val tomorrowEnd = DateTimeUtils.getTomorrowEnd()
        assertTrue(tomorrowEnd > tomorrowStart)
        assertTrue(tomorrowStart > todayEnd)
        
        // 测试本周时间范围
        val weekStart = DateTimeUtils.getThisWeekStart()
        val weekEnd = DateTimeUtils.getThisWeekEnd()
        assertTrue(weekEnd > weekStart)
        assertTrue(DateTimeUtils.isThisWeek(currentTimestamp))
        
        // 测试本月时间范围
        val monthStart = DateTimeUtils.getThisMonthStart()
        val monthEnd = DateTimeUtils.getThisMonthEnd()
        assertTrue(monthEnd > monthStart)
        assertTrue(DateTimeUtils.isThisMonth(currentTimestamp))
        
        // 测试时间差计算
        val timeDiff = DateTimeUtils.getTimeDifference(todayStart, todayEnd)
        assertTrue(timeDiff > 0)
        
        // 测试持续时间格式化
        val duration1 = DateTimeUtils.formatDuration(3661000L) // 1小时1分1秒
        assertTrue(duration1.contains("小时"))
        assertTrue(duration1.contains("分钟"))
        assertTrue(duration1.contains("秒"))
        
        val durationShort = DateTimeUtils.formatDurationShort(3661000L)
        assertTrue(durationShort.contains("h"))
        assertTrue(durationShort.contains("m"))
        
        val timeLength = DateTimeUtils.formatTimeLength(3661000L)
        assertEquals("01:01:01", timeLength)
        
        val shortTimeLength = DateTimeUtils.formatTimeLength(61000L)
        assertEquals("01:01", shortTimeLength)
        
        // 测试相对时间
        val relativeTime = DateTimeUtils.getRelativeTimeDescription(currentTimestamp - 60000L)
        assertTrue(relativeTime.contains("分钟前"))
        
        // 测试日期组件
        val year = DateTimeUtils.getYear(timestamp)
        assertTrue(year >= 2021 && year <= 2023)
        
        val month = DateTimeUtils.getMonth(timestamp)
        assertTrue(month >= 1 && month <= 12)
        
        val day = DateTimeUtils.getDayOfMonth(timestamp)
        assertTrue(day >= 1 && day <= 31)
        
        val dayOfWeek = DateTimeUtils.getDayOfWeek(timestamp)
        assertTrue(dayOfWeek >= 1 && dayOfWeek <= 7)
        
        val dayName = DateTimeUtils.getDayOfWeekName(timestamp)
        assertTrue(dayName.startsWith("周"))
        
        val hour = DateTimeUtils.getHour(timestamp)
        assertTrue(hour >= 0 && hour <= 23)
        
        val minute = DateTimeUtils.getMinute(timestamp)
        assertTrue(minute >= 0 && minute <= 59)
        
        val second = DateTimeUtils.getSecond(timestamp)
        assertTrue(second >= 0 && second <= 59)
        
        // 测试时间添加
        val addedDays = DateTimeUtils.addDays(timestamp, 1)
        assertEquals(timestamp + DateTimeUtils.DAY_MILLIS, addedDays)
        
        val addedHours = DateTimeUtils.addHours(timestamp, 1)
        assertEquals(timestamp + DateTimeUtils.HOUR_MILLIS, addedHours)
        
        val addedMinutes = DateTimeUtils.addMinutes(timestamp, 1)
        assertEquals(timestamp + DateTimeUtils.MINUTE_MILLIS, addedMinutes)
        
        val addedSeconds = DateTimeUtils.addSeconds(timestamp, 1)
        assertEquals(timestamp + DateTimeUtils.SECOND_MILLIS, addedSeconds)
        
        // 测试时间范围
        val rangeDesc = DateTimeUtils.getTimeRangeDescription(todayStart, todayEnd)
        assertFalse(rangeDesc.isEmpty())
        
        assertTrue(DateTimeUtils.isTimeInRange(currentTimestamp, todayStart, todayEnd))
        assertFalse(DateTimeUtils.isTimeInRange(currentTimestamp, yesterdayStart, yesterdayEnd))
        
        // 测试时间段描述
        val periodDesc = DateTimeUtils.getTimePeriodDescription(currentTimestamp)
        assertTrue(periodDesc in listOf("凌晨", "早晨", "上午", "中午", "下午", "傍晚", "晚上"))
        
        // 测试时间统计
        val timestamps = listOf(timestamp, timestamp + 3600000L, timestamp + 7200000L)
        val stats = DateTimeUtils.getTimeStats(timestamps)
        assertEquals(3, stats["count"])
        assertEquals(timestamp, stats["earliest"])
        assertEquals(timestamp + 7200000L, stats["latest"])
        assertEquals(7200000L, stats["span"])
    }
}
