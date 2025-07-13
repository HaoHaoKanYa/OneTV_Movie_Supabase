package top.cywin.onetv.film.utils

import android.util.Log
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

/**
 * Jsoup HTML 解析工具类
 * 
 * 基于 FongMi/TV 的 Jsoup 工具实现
 * 提供完整的 HTML 解析和 XPath 规则处理功能
 * 
 * 功能：
 * - HTML 文档解析
 * - CSS 选择器支持
 * - 属性提取
 * - 文本内容提取
 * - 链接处理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
object JsoupUtils {
    
    private const val TAG = "ONETV_FILM_JSOUP_UTILS"
    
    /**
     * 📄 解析 HTML 文档
     */
    fun parseDocument(html: String, baseUrl: String = ""): Document {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            Log.d(TAG, "📄 HTML 文档解析成功，元素数量: ${doc.allElements.size}")
            doc
        } catch (e: Exception) {
            Log.e(TAG, "❌ HTML 文档解析失败", e)
            Jsoup.parse("<html><body></body></html>")
        }
    }
    
    /**
     * 🔍 选择元素列表
     */
    fun selectElements(html: String, rule: String): Elements {
        return try {
            val doc = parseDocument(html)
            selectElements(doc, rule)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 元素选择失败: rule=$rule", e)
            Elements()
        }
    }
    
    /**
     * 🔍 选择元素列表（从文档）
     */
    fun selectElements(doc: Document, rule: String): Elements {
        return try {
            val elements = doc.select(rule)
            Log.d(TAG, "🔍 选择元素成功: rule=$rule, count=${elements.size}")
            elements
        } catch (e: Exception) {
            Log.e(TAG, "❌ 元素选择失败: rule=$rule", e)
            Elements()
        }
    }
    
    /**
     * 🔍 选择单个元素
     */
    fun selectElement(html: String, rule: String): Element? {
        return try {
            val doc = parseDocument(html)
            selectElement(doc, rule)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 单元素选择失败: rule=$rule", e)
            null
        }
    }
    
    /**
     * 🔍 选择单个元素（从文档）
     */
    fun selectElement(doc: Document, rule: String): Element? {
        return try {
            val element = doc.selectFirst(rule)
            Log.d(TAG, "🔍 选择单元素: rule=$rule, found=${element != null}")
            element
        } catch (e: Exception) {
            Log.e(TAG, "❌ 单元素选择失败: rule=$rule", e)
            null
        }
    }
    
    /**
     * 📝 解析规则获取文本
     */
    fun parseRule(html: String, rule: String): String {
        return try {
            val doc = parseDocument(html)
            parseRule(doc, rule)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 规则解析失败: rule=$rule", e)
            ""
        }
    }
    
    /**
     * 📝 解析规则获取文本（从文档）
     */
    fun parseRule(doc: Document, rule: String): String {
        return try {
            parseRuleInternal(doc, rule)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 规则解析失败: rule=$rule", e)
            ""
        }
    }
    
    /**
     * 📝 解析规则获取文本（从元素）
     */
    fun parseRule(element: Element, rule: String): String {
        return try {
            parseElementByRule(element, rule)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 元素规则解析失败: rule=$rule", e)
            ""
        }
    }
    
    /**
     * 📝 解析规则获取文本数组
     */
    fun parseRuleArray(html: String, rule: String): List<String> {
        return try {
            val doc = parseDocument(html)
            parseRuleArray(doc, rule)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 规则数组解析失败: rule=$rule", e)
            emptyList()
        }
    }
    
    /**
     * 📝 解析规则获取文本数组（从文档）
     */
    fun parseRuleArray(doc: Document, rule: String): List<String> {
        return try {
            val elements = selectElements(doc, extractSelector(rule))
            elements.map { element ->
                parseElementByRule(element, rule)
            }.filter { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 规则数组解析失败: rule=$rule", e)
            emptyList()
        }
    }
    
    /**
     * 🔧 内部规则解析实现
     */
    private fun parseRuleInternal(doc: Document, rule: String): String {
        if (rule.isEmpty()) return ""
        
        // 解析规则格式：selector@attribute 或 selector@text
        val parts = rule.split("@")
        val selector = parts[0].trim()
        val attribute = if (parts.size > 1) parts[1].trim() else "text"
        
        val element = selectElement(doc, selector) ?: return ""
        
        return when (attribute.lowercase()) {
            "text" -> element.text()
            "html" -> element.html()
            "outerhtml" -> element.outerHtml()
            "href" -> element.attr("href")
            "src" -> element.attr("src")
            "class" -> element.attr("class")
            "id" -> element.attr("id")
            else -> element.attr(attribute)
        }.trim()
    }
    
    /**
     * 🔧 元素规则解析实现
     */
    private fun parseElementByRule(element: Element, rule: String): String {
        if (rule.isEmpty()) return ""
        
        // 解析规则格式：selector@attribute 或 selector@text
        val parts = rule.split("@")
        val selector = parts[0].trim()
        val attribute = if (parts.size > 1) parts[1].trim() else "text"
        
        // 如果选择器为空或为"."，直接使用当前元素
        val targetElement = if (selector.isEmpty() || selector == ".") {
            element
        } else {
            element.selectFirst(selector) ?: return ""
        }
        
        return when (attribute.lowercase()) {
            "text" -> targetElement.text()
            "html" -> targetElement.html()
            "outerhtml" -> targetElement.outerHtml()
            "href" -> targetElement.attr("href")
            "src" -> targetElement.attr("src")
            "class" -> targetElement.attr("class")
            "id" -> targetElement.attr("id")
            else -> targetElement.attr(attribute)
        }.trim()
    }
    
    /**
     * 🔧 提取选择器部分
     */
    private fun extractSelector(rule: String): String {
        return rule.split("@")[0].trim()
    }
    
    /**
     * 🔗 解析链接
     */
    fun parseLinks(html: String, rule: String, baseUrl: String = ""): List<String> {
        return try {
            val doc = parseDocument(html, baseUrl)
            val elements = selectElements(doc, rule)
            
            elements.map { element ->
                val href = element.attr("abs:href").ifEmpty { element.attr("href") }
                if (href.isNotEmpty() && baseUrl.isNotEmpty()) {
                    UrlUtils.resolveUrl(baseUrl, href)
                } else {
                    href
                }
            }.filter { it.isNotEmpty() }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 链接解析失败: rule=$rule", e)
            emptyList()
        }
    }
    
    /**
     * 🖼️ 解析图片链接
     */
    fun parseImages(html: String, rule: String, baseUrl: String = ""): List<String> {
        return try {
            val doc = parseDocument(html, baseUrl)
            val elements = selectElements(doc, rule)
            
            elements.map { element ->
                val src = element.attr("abs:src").ifEmpty { element.attr("src") }
                if (src.isNotEmpty() && baseUrl.isNotEmpty()) {
                    UrlUtils.resolveUrl(baseUrl, src)
                } else {
                    src
                }
            }.filter { it.isNotEmpty() }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 图片解析失败: rule=$rule", e)
            emptyList()
        }
    }
    
    /**
     * 📊 获取元素属性
     */
    fun getElementAttributes(html: String, rule: String): Map<String, String> {
        return try {
            val element = selectElement(html, rule) ?: return emptyMap()
            
            val attributes = mutableMapOf<String, String>()
            element.attributes().forEach { attr ->
                attributes[attr.key] = attr.value
            }
            
            Log.d(TAG, "📊 元素属性获取成功: rule=$rule, count=${attributes.size}")
            attributes
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 元素属性获取失败: rule=$rule", e)
            emptyMap()
        }
    }
    
    /**
     * 🧹 清理 HTML 内容
     */
    fun cleanHtml(html: String): String {
        return try {
            val doc = parseDocument(html)
            doc.text().trim()
        } catch (e: Exception) {
            Log.e(TAG, "❌ HTML 清理失败", e)
            html
        }
    }
    
    /**
     * 🔍 检查元素是否存在
     */
    fun hasElement(html: String, rule: String): Boolean {
        return try {
            val doc = parseDocument(html)
            selectElement(doc, rule) != null
        } catch (e: Exception) {
            Log.e(TAG, "❌ 元素存在性检查失败: rule=$rule", e)
            false
        }
    }
    
    /**
     * 📏 获取元素数量
     */
    fun getElementCount(html: String, rule: String): Int {
        return try {
            val elements = selectElements(html, rule)
            elements.size
        } catch (e: Exception) {
            Log.e(TAG, "❌ 元素数量获取失败: rule=$rule", e)
            0
        }
    }
}
