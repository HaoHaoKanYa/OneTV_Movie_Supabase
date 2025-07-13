package top.cywin.onetv.film.utils

import android.util.Log
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * JSON 工具类
 * 
 * 基于 FongMi/TV 的 JSON 处理工具实现
 * 提供 JSON 解析、生成、查询等功能
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
object JsonUtils {
    
    private const val TAG = "ONETV_FILM_JSON_UTILS"
    
    // JSON 配置
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
        coerceInputValues = true
    }
    
    // 美化输出的 JSON 配置
    private val prettyJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
        coerceInputValues = true
    }
    
    /**
     * 🔍 检查字符串是否为有效的 JSON
     */
    fun isValidJson(jsonStr: String?): Boolean {
        if (StringUtils.isEmpty(jsonStr)) return false
        
        return try {
            json.parseToJsonElement(jsonStr!!)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 📋 解析 JSON 字符串为 JsonElement
     */
    fun parseToJsonElement(jsonStr: String?): JsonElement? {
        if (StringUtils.isEmpty(jsonStr)) return null
        
        return try {
            json.parseToJsonElement(jsonStr!!)
        } catch (e: Exception) {
            Log.e(TAG, "❌ JSON 解析失败", e)
            null
        }
    }
    
    /**
     * 📋 解析 JSON 字符串为 JsonObject
     */
    fun parseToJsonObject(jsonStr: String?): JsonObject? {
        val element = parseToJsonElement(jsonStr)
        return if (element is JsonObject) element else null
    }
    
    /**
     * 📋 解析 JSON 字符串为 JsonArray
     */
    fun parseToJsonArray(jsonStr: String?): JsonArray? {
        val element = parseToJsonElement(jsonStr)
        return if (element is JsonArray) element else null
    }
    
    /**
     * 🔍 获取 JSON 对象中的字符串值
     */
    fun getString(jsonObject: JsonObject?, key: String, defaultValue: String = ""): String {
        if (jsonObject == null) return defaultValue
        
        return try {
            jsonObject[key]?.jsonPrimitive?.contentOrNull ?: defaultValue
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 获取字符串值失败: $key", e)
            defaultValue
        }
    }
    
    /**
     * 🔍 获取 JSON 对象中的整数值
     */
    fun getInt(jsonObject: JsonObject?, key: String, defaultValue: Int = 0): Int {
        if (jsonObject == null) return defaultValue
        
        return try {
            jsonObject[key]?.jsonPrimitive?.intOrNull ?: defaultValue
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 获取整数值失败: $key", e)
            defaultValue
        }
    }
    
    /**
     * 🔍 获取 JSON 对象中的长整数值
     */
    fun getLong(jsonObject: JsonObject?, key: String, defaultValue: Long = 0L): Long {
        if (jsonObject == null) return defaultValue
        
        return try {
            jsonObject[key]?.jsonPrimitive?.longOrNull ?: defaultValue
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 获取长整数值失败: $key", e)
            defaultValue
        }
    }
    
    /**
     * 🔍 获取 JSON 对象中的浮点数值
     */
    fun getDouble(jsonObject: JsonObject?, key: String, defaultValue: Double = 0.0): Double {
        if (jsonObject == null) return defaultValue
        
        return try {
            jsonObject[key]?.jsonPrimitive?.doubleOrNull ?: defaultValue
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 获取浮点数值失败: $key", e)
            defaultValue
        }
    }
    
    /**
     * 🔍 获取 JSON 对象中的布尔值
     */
    fun getBoolean(jsonObject: JsonObject?, key: String, defaultValue: Boolean = false): Boolean {
        if (jsonObject == null) return defaultValue
        
        return try {
            jsonObject[key]?.jsonPrimitive?.booleanOrNull ?: defaultValue
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 获取布尔值失败: $key", e)
            defaultValue
        }
    }
    
    /**
     * 🔍 获取 JSON 对象中的对象值
     */
    fun getJsonObject(jsonObject: JsonObject?, key: String): JsonObject? {
        if (jsonObject == null) return null
        
        return try {
            jsonObject[key]?.jsonObject
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 获取对象值失败: $key", e)
            null
        }
    }
    
    /**
     * 🔍 获取 JSON 对象中的数组值
     */
    fun getJsonArray(jsonObject: JsonObject?, key: String): JsonArray? {
        if (jsonObject == null) return null
        
        return try {
            jsonObject[key]?.jsonArray
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 获取数组值失败: $key", e)
            null
        }
    }
    
    /**
     * 🔍 使用路径获取值（支持嵌套访问，如 "data.list.0.name"）
     */
    fun getValueByPath(jsonElement: JsonElement?, path: String, defaultValue: String = ""): String {
        if (jsonElement == null || StringUtils.isEmpty(path)) return defaultValue
        
        return try {
            val keys = path.split(".")
            var current: JsonElement? = jsonElement
            
            for (key in keys) {
                current = when (current) {
                    is JsonObject -> {
                        current[key]
                    }
                    is JsonArray -> {
                        val index = key.toIntOrNull()
                        if (index != null && index >= 0 && index < current.size) {
                            current[index]
                        } else {
                            null
                        }
                    }
                    else -> null
                }
                
                if (current == null) break
            }
            
            current?.jsonPrimitive?.contentOrNull ?: defaultValue
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 路径获取值失败: $path", e)
            defaultValue
        }
    }
    
    /**
     * 🔍 获取数组中的所有字符串值
     */
    fun getStringList(jsonArray: JsonArray?): List<String> {
        if (jsonArray == null) return emptyList()
        
        return try {
            jsonArray.mapNotNull { element ->
                element.jsonPrimitive.contentOrNull
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 获取字符串列表失败", e)
            emptyList()
        }
    }
    
    /**
     * 🔍 获取数组中的所有对象
     */
    fun getObjectList(jsonArray: JsonArray?): List<JsonObject> {
        if (jsonArray == null) return emptyList()
        
        return try {
            jsonArray.mapNotNull { element ->
                if (element is JsonObject) element else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 获取对象列表失败", e)
            emptyList()
        }
    }
    
    /**
     * 🏗️ 创建 JSON 对象
     */
    fun buildJsonObject(builder: JsonObjectBuilder.() -> Unit): JsonObject {
        return buildJsonObject(builder)
    }
    
    /**
     * 🏗️ 创建 JSON 数组
     */
    fun buildJsonArray(builder: JsonArrayBuilder.() -> Unit): JsonArray {
        return buildJsonArray(builder)
    }
    
    /**
     * 📝 将对象转换为 JSON 字符串
     */
    inline fun <reified T> toJsonString(obj: T, pretty: Boolean = false): String {
        return try {
            if (pretty) {
                prettyJson.encodeToString(obj)
            } else {
                json.encodeToString(obj)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 对象转 JSON 失败", e)
            ""
        }
    }
    
    /**
     * 📋 将 JSON 字符串转换为对象
     */
    inline fun <reified T> fromJsonString(jsonStr: String?): T? {
        if (StringUtils.isEmpty(jsonStr)) return null
        
        return try {
            json.decodeFromString<T>(jsonStr!!)
        } catch (e: Exception) {
            Log.e(TAG, "❌ JSON 转对象失败", e)
            null
        }
    }
    
    /**
     * 🔄 合并两个 JSON 对象
     */
    fun mergeJsonObjects(obj1: JsonObject?, obj2: JsonObject?): JsonObject {
        if (obj1 == null && obj2 == null) return buildJsonObject { }
        if (obj1 == null) return obj2!!
        if (obj2 == null) return obj1
        
        return buildJsonObject {
            // 添加第一个对象的所有键值对
            obj1.forEach { (key, value) ->
                put(key, value)
            }
            
            // 添加第二个对象的键值对（会覆盖重复的键）
            obj2.forEach { (key, value) ->
                put(key, value)
            }
        }
    }
    
    /**
     * 🔍 查找 JSON 中包含指定值的所有路径
     */
    fun findPaths(jsonElement: JsonElement?, targetValue: String): List<String> {
        if (jsonElement == null || StringUtils.isEmpty(targetValue)) return emptyList()
        
        val paths = mutableListOf<String>()
        findPathsRecursive(jsonElement, targetValue!!, "", paths)
        return paths
    }
    
    /**
     * 🔍 递归查找路径
     */
    private fun findPathsRecursive(
        element: JsonElement,
        targetValue: String,
        currentPath: String,
        paths: MutableList<String>
    ) {
        when (element) {
            is JsonObject -> {
                element.forEach { (key, value) ->
                    val newPath = if (currentPath.isEmpty()) key else "$currentPath.$key"
                    
                    if (value is JsonPrimitive && value.contentOrNull == targetValue) {
                        paths.add(newPath)
                    } else {
                        findPathsRecursive(value, targetValue, newPath, paths)
                    }
                }
            }
            is JsonArray -> {
                element.forEachIndexed { index, value ->
                    val newPath = if (currentPath.isEmpty()) index.toString() else "$currentPath.$index"
                    
                    if (value is JsonPrimitive && value.contentOrNull == targetValue) {
                        paths.add(newPath)
                    } else {
                        findPathsRecursive(value, targetValue, newPath, paths)
                    }
                }
            }
            is JsonPrimitive -> {
                if (element.contentOrNull == targetValue) {
                    paths.add(currentPath)
                }
            }
        }
    }
    
    /**
     * 🔍 过滤 JSON 数组
     */
    fun filterJsonArray(
        jsonArray: JsonArray?,
        predicate: (JsonElement) -> Boolean
    ): JsonArray {
        if (jsonArray == null) return buildJsonArray { }
        
        return buildJsonArray {
            jsonArray.forEach { element ->
                if (predicate(element)) {
                    add(element)
                }
            }
        }
    }
    
    /**
     * 🔄 转换 JSON 数组
     */
    fun <T> mapJsonArray(
        jsonArray: JsonArray?,
        transform: (JsonElement) -> T
    ): List<T> {
        if (jsonArray == null) return emptyList()
        
        return try {
            jsonArray.map(transform)
        } catch (e: Exception) {
            Log.e(TAG, "❌ JSON 数组转换失败", e)
            emptyList()
        }
    }
    
    /**
     * 📊 获取 JSON 统计信息
     */
    fun getJsonStats(jsonElement: JsonElement?): Map<String, Any> {
        if (jsonElement == null) {
            return mapOf(
                "type" to "null",
                "size" to 0,
                "depth" to 0
            )
        }
        
        return when (jsonElement) {
            is JsonObject -> {
                mapOf(
                    "type" to "object",
                    "size" to jsonElement.size,
                    "keys" to jsonElement.keys.toList(),
                    "depth" to calculateDepth(jsonElement)
                )
            }
            is JsonArray -> {
                mapOf(
                    "type" to "array",
                    "size" to jsonElement.size,
                    "depth" to calculateDepth(jsonElement)
                )
            }
            is JsonPrimitive -> {
                mapOf(
                    "type" to "primitive",
                    "value_type" to when {
                        jsonElement.isString -> "string"
                        jsonElement.booleanOrNull != null -> "boolean"
                        jsonElement.intOrNull != null -> "int"
                        jsonElement.longOrNull != null -> "long"
                        jsonElement.doubleOrNull != null -> "double"
                        else -> "unknown"
                    },
                    "value" to jsonElement.contentOrNull,
                    "depth" to 1
                )
            }
            else -> {
                mapOf(
                    "type" to "unknown",
                    "depth" to 0
                )
            }
        }
    }
    
    /**
     * 📏 计算 JSON 深度
     */
    private fun calculateDepth(jsonElement: JsonElement): Int {
        return when (jsonElement) {
            is JsonObject -> {
                if (jsonElement.isEmpty()) 1
                else 1 + (jsonElement.values.maxOfOrNull { calculateDepth(it) } ?: 0)
            }
            is JsonArray -> {
                if (jsonElement.isEmpty()) 1
                else 1 + (jsonElement.maxOfOrNull { calculateDepth(it) } ?: 0)
            }
            else -> 1
        }
    }
    
    /**
     * 🧹 清理 JSON（移除空值和空对象）
     */
    fun cleanJson(jsonElement: JsonElement?): JsonElement? {
        if (jsonElement == null) return null
        
        return when (jsonElement) {
            is JsonObject -> {
                val cleanedObject = buildJsonObject {
                    jsonElement.forEach { (key, value) ->
                        val cleanedValue = cleanJson(value)
                        if (cleanedValue != null && !isEmptyJsonElement(cleanedValue)) {
                            put(key, cleanedValue)
                        }
                    }
                }
                if (cleanedObject.isEmpty()) null else cleanedObject
            }
            is JsonArray -> {
                val cleanedArray = buildJsonArray {
                    jsonElement.forEach { element ->
                        val cleanedElement = cleanJson(element)
                        if (cleanedElement != null && !isEmptyJsonElement(cleanedElement)) {
                            add(cleanedElement)
                        }
                    }
                }
                if (cleanedArray.isEmpty()) null else cleanedArray
            }
            is JsonPrimitive -> {
                if (jsonElement.isString && jsonElement.content.isBlank()) null else jsonElement
            }
            else -> jsonElement
        }
    }
    
    /**
     * 🔍 检查 JSON 元素是否为空
     */
    private fun isEmptyJsonElement(jsonElement: JsonElement): Boolean {
        return when (jsonElement) {
            is JsonObject -> jsonElement.isEmpty()
            is JsonArray -> jsonElement.isEmpty()
            is JsonPrimitive -> jsonElement.isString && jsonElement.content.isBlank()
            else -> false
        }
    }
}
