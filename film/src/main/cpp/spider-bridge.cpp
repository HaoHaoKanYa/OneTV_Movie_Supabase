/**
 * Spider 爬虫桥接
 * 
 * 基于 FongMi/TV 的爬虫引擎实现
 * 提供高性能的网页解析和数据提取
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */

#include <jni.h>
#include <string>
#include <vector>
#include <map>
#include <regex>
#include <sstream>
#include <chrono>
#include <algorithm>
#include <android/log.h>

#define LOG_TAG "ONETV_SPIDER_BRIDGE"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// 爬虫结果结构
struct SpiderResult {
    std::string url;
    std::string title;
    std::string content;
    std::string thumbnail;
    std::map<std::string, std::string> metadata;
    std::vector<std::string> playUrls;
    std::vector<std::string> downloadUrls;
    long parseTime;
    std::string error;
};

// 简单的正则表达式爬虫引擎
class SimpleSpiderEngine {
private:
    std::string html;
    std::string baseUrl;
    
public:
    SimpleSpiderEngine(const std::string& htmlContent, const std::string& url) 
        : html(htmlContent), baseUrl(url) {}
    
    /**
     * 提取视频播放地址
     */
    std::vector<std::string> extractPlayUrls() {
        std::vector<std::string> urls;
        
        // 常见的视频 URL 模式
        std::vector<std::regex> patterns = {
            std::regex(R"((https?://[^"'\s]+\.m3u8[^"'\s]*))"),
            std::regex(R"((https?://[^"'\s]+\.mp4[^"'\s]*))"),
            std::regex(R"((https?://[^"'\s]+\.flv[^"'\s]*))"),
            std::regex(R"((https?://[^"'\s]+\.avi[^"'\s]*))"),
            std::regex(R"((https?://[^"'\s]+\.mkv[^"'\s]*))"),
            std::regex(R"(src=["']([^"']+\.m3u8[^"']*))"),
            std::regex(R"(src=["']([^"']+\.mp4[^"']*))"),
            std::regex(R"(url["\s]*[:=]["\s]*["']([^"']+\.m3u8[^"']*))"),
            std::regex(R"(url["\s]*[:=]["\s]*["']([^"']+\.mp4[^"']*))"),
        };
        
        for (const auto& pattern : patterns) {
            std::sregex_iterator iter(html.begin(), html.end(), pattern);
            std::sregex_iterator end;
            
            for (; iter != end; ++iter) {
                std::smatch match = *iter;
                std::string url = match[1].str();
                
                // 去重
                if (std::find(urls.begin(), urls.end(), url) == urls.end()) {
                    urls.push_back(url);
                }
            }
        }
        
        return urls;
    }
    
    /**
     * 提取标题
     */
    std::string extractTitle() {
        // 尝试多种标题提取模式
        std::vector<std::regex> patterns = {
            std::regex(R"(<title[^>]*>([^<]+)</title>)", std::regex_constants::icase),
            std::regex(R"(<h1[^>]*>([^<]+)</h1>)", std::regex_constants::icase),
            std::regex(R"(<h2[^>]*>([^<]+)</h2>)", std::regex_constants::icase),
            std::regex(R"(title["\s]*[:=]["\s]*["']([^"']+))", std::regex_constants::icase),
            std::regex(R"(name["\s]*[:=]["\s]*["']([^"']+))", std::regex_constants::icase),
        };
        
        for (const auto& pattern : patterns) {
            std::smatch match;
            if (std::regex_search(html, match, pattern)) {
                std::string title = match[1].str();
                title = trim(stripHtmlTags(title));
                if (!title.empty() && title.length() > 2) {
                    return title;
                }
            }
        }
        
        return "未知标题";
    }
    
    /**
     * 提取缩略图
     */
    std::string extractThumbnail() {
        std::vector<std::regex> patterns = {
            std::regex(R"(<img[^>]+src=["']([^"']+))", std::regex_constants::icase),
            std::regex(R"(poster=["']([^"']+))", std::regex_constants::icase),
            std::regex(R"(thumbnail["\s]*[:=]["\s]*["']([^"']+))", std::regex_constants::icase),
            std::regex(R"(cover["\s]*[:=]["\s]*["']([^"']+))", std::regex_constants::icase),
        };
        
        for (const auto& pattern : patterns) {
            std::smatch match;
            if (std::regex_search(html, match, pattern)) {
                std::string thumbnail = match[1].str();
                if (isImageUrl(thumbnail)) {
                    return makeAbsoluteUrl(thumbnail);
                }
            }
        }
        
        return "";
    }
    
    /**
     * 提取元数据
     */
    std::map<std::string, std::string> extractMetadata() {
        std::map<std::string, std::string> metadata;
        
        // 提取常见的元数据
        std::vector<std::pair<std::string, std::regex>> patterns = {
            {"duration", std::regex(R"(duration["\s]*[:=]["\s]*["']?([^"',\s]+))", std::regex_constants::icase)},
            {"quality", std::regex(R"(quality["\s]*[:=]["\s]*["']?([^"',\s]+))", std::regex_constants::icase)},
            {"format", std::regex(R"(format["\s]*[:=]["\s]*["']?([^"',\s]+))", std::regex_constants::icase)},
            {"size", std::regex(R"(size["\s]*[:=]["\s]*["']?([^"',\s]+))", std::regex_constants::icase)},
            {"bitrate", std::regex(R"(bitrate["\s]*[:=]["\s]*["']?([^"',\s]+))", std::regex_constants::icase)},
            {"fps", std::regex(R"(fps["\s]*[:=]["\s]*["']?([^"',\s]+))", std::regex_constants::icase)},
        };
        
        for (const auto& pattern : patterns) {
            std::smatch match;
            if (std::regex_search(html, match, pattern.second)) {
                metadata[pattern.first] = match[1].str();
            }
        }
        
        return metadata;
    }
    
    /**
     * 提取下载地址
     */
    std::vector<std::string> extractDownloadUrls() {
        std::vector<std::string> urls;
        
        std::vector<std::regex> patterns = {
            std::regex(R"(download["\s]*[:=]["\s]*["']([^"']+))", std::regex_constants::icase),
            std::regex(R"(href=["']([^"']+\.mp4[^"']*)", std::regex_constants::icase),
            std::regex(R"(href=["']([^"']+\.avi[^"']*)", std::regex_constants::icase),
            std::regex(R"(href=["']([^"']+\.mkv[^"']*)", std::regex_constants::icase),
        };
        
        for (const auto& pattern : patterns) {
            std::sregex_iterator iter(html.begin(), html.end(), pattern);
            std::sregex_iterator end;
            
            for (; iter != end; ++iter) {
                std::smatch match = *iter;
                std::string url = match[1].str();
                
                if (std::find(urls.begin(), urls.end(), url) == urls.end()) {
                    urls.push_back(makeAbsoluteUrl(url));
                }
            }
        }
        
        return urls;
    }

private:
    /**
     * 移除 HTML 标签
     */
    std::string stripHtmlTags(const std::string& html) {
        std::regex tagRegex("<[^>]*>");
        return std::regex_replace(html, tagRegex, "");
    }
    
    /**
     * 去除首尾空白字符
     */
    std::string trim(const std::string& str) {
        size_t start = str.find_first_not_of(" \t\r\n");
        if (start == std::string::npos) return "";
        
        size_t end = str.find_last_not_of(" \t\r\n");
        return str.substr(start, end - start + 1);
    }
    
    /**
     * 检查是否为图片 URL
     */
    bool isImageUrl(const std::string& url) {
        std::regex imageRegex(R"(\.(jpg|jpeg|png|gif|bmp|webp)(\?|$))", std::regex_constants::icase);
        return std::regex_search(url, imageRegex);
    }
    
    /**
     * 转换为绝对 URL
     */
    std::string makeAbsoluteUrl(const std::string& url) {
        if (url.find("http") == 0) {
            return url;
        }
        
        if (url.find("//") == 0) {
            return "https:" + url;
        }
        
        if (url.find("/") == 0) {
            // 提取基础域名
            std::regex domainRegex(R"(https?://[^/]+)");
            std::smatch match;
            if (std::regex_search(baseUrl, match, domainRegex)) {
                return match[0].str() + url;
            }
        }
        
        // 相对路径
        size_t lastSlash = baseUrl.find_last_of('/');
        if (lastSlash != std::string::npos) {
            return baseUrl.substr(0, lastSlash + 1) + url;
        }
        
        return url;
    }
};

/**
 * 将 SpiderResult 转换为 JSON 字符串
 */
std::string spiderResultToJson(const SpiderResult& result) {
    std::ostringstream json;
    json << "{";
    json << "\"url\":\"" << result.url << "\",";
    json << "\"title\":\"" << result.title << "\",";
    json << "\"content\":\"" << result.content << "\",";
    json << "\"thumbnail\":\"" << result.thumbnail << "\",";
    json << "\"parseTime\":" << result.parseTime << ",";
    json << "\"error\":\"" << result.error << "\",";
    
    // 播放地址数组
    json << "\"playUrls\":[";
    for (size_t i = 0; i < result.playUrls.size(); ++i) {
        if (i > 0) json << ",";
        json << "\"" << result.playUrls[i] << "\"";
    }
    json << "],";
    
    // 下载地址数组
    json << "\"downloadUrls\":[";
    for (size_t i = 0; i < result.downloadUrls.size(); ++i) {
        if (i > 0) json << ",";
        json << "\"" << result.downloadUrls[i] << "\"";
    }
    json << "],";
    
    // 元数据对象
    json << "\"metadata\":{";
    bool first = true;
    for (const auto& meta : result.metadata) {
        if (!first) json << ",";
        json << "\"" << meta.first << "\":\"" << meta.second << "\"";
        first = false;
    }
    json << "}";
    
    json << "}";
    return json.str();
}

extern "C" {

/**
 * 解析网页内容
 */
JNIEXPORT jstring JNICALL
Java_top_cywin_onetv_film_spider_SpiderManager_nativeParse(
    JNIEnv *env, jobject thiz, jstring url, jstring html) {
    
    const char* urlStr = env->GetStringUTFChars(url, nullptr);
    const char* htmlStr = env->GetStringUTFChars(html, nullptr);
    
    LOGD("解析网页内容: %s", urlStr);
    
    auto startTime = std::chrono::high_resolution_clock::now();
    
    try {
        SimpleSpiderEngine engine(htmlStr, urlStr);
        
        SpiderResult result;
        result.url = urlStr;
        result.title = engine.extractTitle();
        result.content = htmlStr;
        result.thumbnail = engine.extractThumbnail();
        result.playUrls = engine.extractPlayUrls();
        result.downloadUrls = engine.extractDownloadUrls();
        result.metadata = engine.extractMetadata();
        
        auto endTime = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(endTime - startTime);
        result.parseTime = duration.count();
        
        std::string jsonResult = spiderResultToJson(result);
        
        env->ReleaseStringUTFChars(url, urlStr);
        env->ReleaseStringUTFChars(html, htmlStr);
        
        LOGD("网页解析完成: 标题=%s, 播放地址=%zu个, 耗时=%ldms", 
             result.title.c_str(), result.playUrls.size(), result.parseTime);
        
        return env->NewStringUTF(jsonResult.c_str());
        
    } catch (const std::exception& e) {
        LOGE("网页解析失败: %s", e.what());
        
        env->ReleaseStringUTFChars(url, urlStr);
        env->ReleaseStringUTFChars(html, htmlStr);
        
        SpiderResult errorResult;
        errorResult.url = urlStr;
        errorResult.error = e.what();
        
        auto endTime = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(endTime - startTime);
        errorResult.parseTime = duration.count();
        
        std::string jsonResult = spiderResultToJson(errorResult);
        return env->NewStringUTF(jsonResult.c_str());
    }
}

/**
 * 提取播放地址
 */
JNIEXPORT jstring JNICALL
Java_top_cywin_onetv_film_spider_SpiderManager_nativeExtractPlayUrls(
    JNIEnv *env, jobject thiz, jstring url, jstring html) {
    
    const char* urlStr = env->GetStringUTFChars(url, nullptr);
    const char* htmlStr = env->GetStringUTFChars(html, nullptr);
    
    LOGD("提取播放地址: %s", urlStr);
    
    try {
        SimpleSpiderEngine engine(htmlStr, urlStr);
        std::vector<std::string> playUrls = engine.extractPlayUrls();
        
        // 构建 JSON 数组
        std::ostringstream json;
        json << "[";
        for (size_t i = 0; i < playUrls.size(); ++i) {
            if (i > 0) json << ",";
            json << "\"" << playUrls[i] << "\"";
        }
        json << "]";
        
        env->ReleaseStringUTFChars(url, urlStr);
        env->ReleaseStringUTFChars(html, htmlStr);
        
        LOGD("播放地址提取完成: %zu个", playUrls.size());
        return env->NewStringUTF(json.str().c_str());
        
    } catch (const std::exception& e) {
        LOGE("播放地址提取失败: %s", e.what());
        
        env->ReleaseStringUTFChars(url, urlStr);
        env->ReleaseStringUTFChars(html, htmlStr);
        
        return env->NewStringUTF("[]");
    }
}

/**
 * 验证播放地址
 */
JNIEXPORT jboolean JNICALL
Java_top_cywin_onetv_film_spider_SpiderManager_nativeValidatePlayUrl(
    JNIEnv *env, jobject thiz, jstring url) {
    
    const char* urlStr = env->GetStringUTFChars(url, nullptr);
    
    LOGD("验证播放地址: %s", urlStr);
    
    try {
        std::string urlString(urlStr);
        
        // 简单的 URL 验证
        bool isValid = (urlString.find("http") == 0) && 
                      (urlString.find(".m3u8") != std::string::npos || 
                       urlString.find(".mp4") != std::string::npos ||
                       urlString.find(".flv") != std::string::npos ||
                       urlString.find(".avi") != std::string::npos ||
                       urlString.find(".mkv") != std::string::npos);
        
        env->ReleaseStringUTFChars(url, urlStr);
        
        LOGD("播放地址验证结果: %s", isValid ? "有效" : "无效");
        return isValid ? JNI_TRUE : JNI_FALSE;
        
    } catch (const std::exception& e) {
        LOGE("播放地址验证失败: %s", e.what());
        
        env->ReleaseStringUTFChars(url, urlStr);
        return JNI_FALSE;
    }
}

} // extern "C"
