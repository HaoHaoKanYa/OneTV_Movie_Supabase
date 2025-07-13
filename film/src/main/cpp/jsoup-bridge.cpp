/**
 * Jsoup HTML 解析桥接
 * 
 * 基于 FongMi/TV 的 HTML 解析实现
 * 提供高性能的 XPath 和 CSS 选择器解析
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */

#include <jni.h>
#include <string>
#include <vector>
#include <map>
#include <regex>
#include <android/log.h>

#define LOG_TAG "ONETV_JSOUP_BRIDGE"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// HTML 解析结果结构
struct ParseResult {
    std::string text;
    std::string html;
    std::map<std::string, std::string> attributes;
    std::vector<ParseResult> children;
};

// 简单的 HTML 标签解析器
class SimpleHtmlParser {
private:
    std::string html;
    size_t position;
    
public:
    SimpleHtmlParser(const std::string& htmlContent) : html(htmlContent), position(0) {}
    
    /**
     * 解析 HTML 标签
     */
    ParseResult parseTag(const std::string& tagName) {
        ParseResult result;
        
        // 查找开始标签
        std::string startTag = "<" + tagName;
        size_t startPos = html.find(startTag, position);
        if (startPos == std::string::npos) {
            return result;
        }
        
        // 查找标签结束位置
        size_t tagEndPos = html.find(">", startPos);
        if (tagEndPos == std::string::npos) {
            return result;
        }
        
        // 解析属性
        std::string tagContent = html.substr(startPos + 1, tagEndPos - startPos - 1);
        parseAttributes(tagContent, result.attributes);
        
        // 查找结束标签
        std::string endTag = "</" + tagName + ">";
        size_t endPos = html.find(endTag, tagEndPos);
        if (endPos == std::string::npos) {
            // 自闭合标签
            result.html = html.substr(startPos, tagEndPos - startPos + 1);
            return result;
        }
        
        // 提取内容
        size_t contentStart = tagEndPos + 1;
        size_t contentLength = endPos - contentStart;
        result.html = html.substr(startPos, endPos - startPos + endTag.length());
        result.text = html.substr(contentStart, contentLength);
        
        // 移除 HTML 标签获取纯文本
        result.text = stripHtmlTags(result.text);
        
        return result;
    }
    
    /**
     * 通过 CSS 选择器查找元素
     */
    std::vector<ParseResult> selectByCss(const std::string& selector) {
        std::vector<ParseResult> results;
        
        // 简单的 CSS 选择器解析
        if (selector.find("#") == 0) {
            // ID 选择器
            std::string id = selector.substr(1);
            ParseResult result = findById(id);
            if (!result.html.empty()) {
                results.push_back(result);
            }
        } else if (selector.find(".") == 0) {
            // Class 选择器
            std::string className = selector.substr(1);
            results = findByClass(className);
        } else {
            // 标签选择器
            results = findByTag(selector);
        }
        
        return results;
    }
    
    /**
     * 通过 XPath 查找元素（简化实现）
     */
    std::vector<ParseResult> selectByXPath(const std::string& xpath) {
        std::vector<ParseResult> results;
        
        // 简化的 XPath 解析
        if (xpath.find("//") == 0) {
            // 查找所有匹配的标签
            std::string tagName = xpath.substr(2);
            
            // 移除可能的属性条件
            size_t bracketPos = tagName.find("[");
            if (bracketPos != std::string::npos) {
                tagName = tagName.substr(0, bracketPos);
            }
            
            results = findByTag(tagName);
        }
        
        return results;
    }

private:
    /**
     * 解析标签属性
     */
    void parseAttributes(const std::string& tagContent, std::map<std::string, std::string>& attributes) {
        std::regex attrRegex(R"((\w+)=["']([^"']*)["'])");
        std::sregex_iterator iter(tagContent.begin(), tagContent.end(), attrRegex);
        std::sregex_iterator end;
        
        for (; iter != end; ++iter) {
            std::smatch match = *iter;
            attributes[match[1].str()] = match[2].str();
        }
    }
    
    /**
     * 移除 HTML 标签
     */
    std::string stripHtmlTags(const std::string& html) {
        std::regex tagRegex("<[^>]*>");
        return std::regex_replace(html, tagRegex, "");
    }
    
    /**
     * 通过 ID 查找元素
     */
    ParseResult findById(const std::string& id) {
        std::regex idRegex(R"(<[^>]*id=["'])" + id + R"(["'][^>]*>)");
        std::smatch match;
        
        if (std::regex_search(html, match, idRegex)) {
            size_t startPos = match.position();
            std::string tagName = extractTagName(match.str());
            
            SimpleHtmlParser parser(html.substr(startPos));
            return parser.parseTag(tagName);
        }
        
        return ParseResult();
    }
    
    /**
     * 通过 Class 查找元素
     */
    std::vector<ParseResult> findByClass(const std::string& className) {
        std::vector<ParseResult> results;
        std::regex classRegex(R"(<[^>]*class=["'][^"']*)" + className + R"([^"']*["'][^>]*>)");
        std::sregex_iterator iter(html.begin(), html.end(), classRegex);
        std::sregex_iterator end;
        
        for (; iter != end; ++iter) {
            std::smatch match = *iter;
            size_t startPos = match.position();
            std::string tagName = extractTagName(match.str());
            
            SimpleHtmlParser parser(html.substr(startPos));
            ParseResult result = parser.parseTag(tagName);
            if (!result.html.empty()) {
                results.push_back(result);
            }
        }
        
        return results;
    }
    
    /**
     * 通过标签名查找元素
     */
    std::vector<ParseResult> findByTag(const std::string& tagName) {
        std::vector<ParseResult> results;
        std::regex tagRegex("<" + tagName + R"([^>]*>)");
        std::sregex_iterator iter(html.begin(), html.end(), tagRegex);
        std::sregex_iterator end;
        
        for (; iter != end; ++iter) {
            std::smatch match = *iter;
            size_t startPos = match.position();
            
            SimpleHtmlParser parser(html.substr(startPos));
            ParseResult result = parser.parseTag(tagName);
            if (!result.html.empty()) {
                results.push_back(result);
            }
        }
        
        return results;
    }
    
    /**
     * 从标签字符串中提取标签名
     */
    std::string extractTagName(const std::string& tag) {
        std::regex nameRegex(R"(<(\w+))");
        std::smatch match;
        
        if (std::regex_search(tag, match, nameRegex)) {
            return match[1].str();
        }
        
        return "";
    }
};

/**
 * 将 ParseResult 转换为 JSON 字符串
 */
std::string parseResultToJson(const ParseResult& result) {
    std::string json = "{";
    json += "\"text\":\"" + result.text + "\",";
    json += "\"html\":\"" + result.html + "\",";
    json += "\"attributes\":{";
    
    bool first = true;
    for (const auto& attr : result.attributes) {
        if (!first) json += ",";
        json += "\"" + attr.first + "\":\"" + attr.second + "\"";
        first = false;
    }
    
    json += "}}";
    return json;
}

/**
 * 将 ParseResult 数组转换为 JSON 数组字符串
 */
std::string parseResultsToJson(const std::vector<ParseResult>& results) {
    std::string json = "[";
    
    for (size_t i = 0; i < results.size(); ++i) {
        if (i > 0) json += ",";
        json += parseResultToJson(results[i]);
    }
    
    json += "]";
    return json;
}

extern "C" {

/**
 * XPath 解析接口
 */
JNIEXPORT jstring JNICALL
Java_top_cywin_onetv_film_engine_XPathEngine_parseHtml(
    JNIEnv *env, jobject thiz, jstring html, jstring xpath) {
    
    const char* htmlStr = env->GetStringUTFChars(html, nullptr);
    const char* xpathStr = env->GetStringUTFChars(xpath, nullptr);
    
    LOGD("解析 HTML: XPath=%s", xpathStr);
    
    try {
        SimpleHtmlParser parser(htmlStr);
        std::vector<ParseResult> results = parser.selectByXPath(xpathStr);
        
        std::string jsonResult = parseResultsToJson(results);
        
        env->ReleaseStringUTFChars(html, htmlStr);
        env->ReleaseStringUTFChars(xpath, xpathStr);
        
        LOGD("解析完成，结果数量: %zu", results.size());
        return env->NewStringUTF(jsonResult.c_str());
        
    } catch (const std::exception& e) {
        LOGE("HTML 解析失败: %s", e.what());
        
        env->ReleaseStringUTFChars(html, htmlStr);
        env->ReleaseStringUTFChars(xpath, xpathStr);
        
        return env->NewStringUTF("[]");
    }
}

/**
 * CSS 选择器解析接口
 */
JNIEXPORT jstring JNICALL
Java_top_cywin_onetv_film_engine_XPathEngine_parseHtmlByCss(
    JNIEnv *env, jobject thiz, jstring html, jstring selector) {
    
    const char* htmlStr = env->GetStringUTFChars(html, nullptr);
    const char* selectorStr = env->GetStringUTFChars(selector, nullptr);
    
    LOGD("解析 HTML: CSS=%s", selectorStr);
    
    try {
        SimpleHtmlParser parser(htmlStr);
        std::vector<ParseResult> results = parser.selectByCss(selectorStr);
        
        std::string jsonResult = parseResultsToJson(results);
        
        env->ReleaseStringUTFChars(html, htmlStr);
        env->ReleaseStringUTFChars(selector, selectorStr);
        
        LOGD("解析完成，结果数量: %zu", results.size());
        return env->NewStringUTF(jsonResult.c_str());
        
    } catch (const std::exception& e) {
        LOGE("HTML 解析失败: %s", e.what());
        
        env->ReleaseStringUTFChars(html, htmlStr);
        env->ReleaseStringUTFChars(selector, selectorStr);
        
        return env->NewStringUTF("[]");
    }
}

/**
 * 提取文本内容
 */
JNIEXPORT jstring JNICALL
Java_top_cywin_onetv_film_engine_XPathEngine_extractText(
    JNIEnv *env, jobject thiz, jstring html) {
    
    const char* htmlStr = env->GetStringUTFChars(html, nullptr);
    
    try {
        SimpleHtmlParser parser(htmlStr);
        std::string text = parser.stripHtmlTags(htmlStr);
        
        env->ReleaseStringUTFChars(html, htmlStr);
        
        return env->NewStringUTF(text.c_str());
        
    } catch (const std::exception& e) {
        LOGE("文本提取失败: %s", e.what());
        
        env->ReleaseStringUTFChars(html, htmlStr);
        
        return env->NewStringUTF("");
    }
}

} // extern "C"
