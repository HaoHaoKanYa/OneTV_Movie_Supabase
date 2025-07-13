/**
 * HTTP 请求桥接
 * 
 * 基于 FongMi/TV 的 HTTP 请求实现
 * 提供高性能的网络请求和响应处理
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */

#include <jni.h>
#include <string>
#include <map>
#include <vector>
#include <sstream>
#include <chrono>
#include <android/log.h>

#ifdef HAVE_CURL
#include <curl/curl.h>
#endif

#define LOG_TAG "ONETV_HTTP_BRIDGE"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// HTTP 响应结构
struct HttpResponse {
    long statusCode;
    std::string body;
    std::map<std::string, std::string> headers;
    std::string error;
    long responseTime;
};

#ifdef HAVE_CURL

// libcurl 写入回调函数
static size_t WriteCallback(void* contents, size_t size, size_t nmemb, std::string* userp) {
    size_t totalSize = size * nmemb;
    userp->append((char*)contents, totalSize);
    return totalSize;
}

// libcurl 头部回调函数
static size_t HeaderCallback(char* buffer, size_t size, size_t nitems, std::map<std::string, std::string>* headers) {
    size_t totalSize = size * nitems;
    std::string header(buffer, totalSize);
    
    // 解析头部
    size_t colonPos = header.find(':');
    if (colonPos != std::string::npos) {
        std::string key = header.substr(0, colonPos);
        std::string value = header.substr(colonPos + 1);
        
        // 去除空白字符
        key.erase(0, key.find_first_not_of(" \t\r\n"));
        key.erase(key.find_last_not_of(" \t\r\n") + 1);
        value.erase(0, value.find_first_not_of(" \t\r\n"));
        value.erase(value.find_last_not_of(" \t\r\n") + 1);
        
        (*headers)[key] = value;
    }
    
    return totalSize;
}

/**
 * 使用 libcurl 执行 HTTP 请求
 */
HttpResponse performHttpRequest(const std::string& url, const std::string& method, 
                               const std::map<std::string, std::string>& headers,
                               const std::string& body, long timeout) {
    HttpResponse response;
    response.statusCode = 0;
    response.responseTime = 0;
    
    auto startTime = std::chrono::high_resolution_clock::now();
    
    CURL* curl = curl_easy_init();
    if (!curl) {
        response.error = "Failed to initialize libcurl";
        return response;
    }
    
    try {
        // 设置 URL
        curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
        
        // 设置超时
        curl_easy_setopt(curl, CURLOPT_TIMEOUT, timeout);
        curl_easy_setopt(curl, CURLOPT_CONNECTTIMEOUT, 10L);
        
        // 设置写入回调
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, &response.body);
        
        // 设置头部回调
        curl_easy_setopt(curl, CURLOPT_HEADERFUNCTION, HeaderCallback);
        curl_easy_setopt(curl, CURLOPT_HEADERDATA, &response.headers);
        
        // 设置 User-Agent
        curl_easy_setopt(curl, CURLOPT_USERAGENT, "OneTV/2.1.1 (Android; Film-Module)");
        
        // 设置 SSL 选项
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0L);
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 0L);
        
        // 跟随重定向
        curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);
        curl_easy_setopt(curl, CURLOPT_MAXREDIRS, 5L);
        
        // 设置请求头
        struct curl_slist* headerList = nullptr;
        for (const auto& header : headers) {
            std::string headerStr = header.first + ": " + header.second;
            headerList = curl_slist_append(headerList, headerStr.c_str());
        }
        if (headerList) {
            curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headerList);
        }
        
        // 设置请求方法和数据
        if (method == "POST") {
            curl_easy_setopt(curl, CURLOPT_POST, 1L);
            if (!body.empty()) {
                curl_easy_setopt(curl, CURLOPT_POSTFIELDS, body.c_str());
                curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, body.length());
            }
        } else if (method == "PUT") {
            curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, "PUT");
            if (!body.empty()) {
                curl_easy_setopt(curl, CURLOPT_POSTFIELDS, body.c_str());
                curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, body.length());
            }
        } else if (method == "DELETE") {
            curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, "DELETE");
        } else if (method == "HEAD") {
            curl_easy_setopt(curl, CURLOPT_NOBODY, 1L);
        }
        // GET 是默认方法
        
        // 执行请求
        CURLcode res = curl_easy_perform(curl);
        
        if (res != CURLE_OK) {
            response.error = curl_easy_strerror(res);
        } else {
            // 获取响应状态码
            curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &response.statusCode);
        }
        
        // 清理头部列表
        if (headerList) {
            curl_slist_free_all(headerList);
        }
        
    } catch (const std::exception& e) {
        response.error = e.what();
    }
    
    curl_easy_cleanup(curl);
    
    auto endTime = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(endTime - startTime);
    response.responseTime = duration.count();
    
    return response;
}

#else

/**
 * 简化的 HTTP 请求实现（无 libcurl）
 */
HttpResponse performHttpRequest(const std::string& url, const std::string& method, 
                               const std::map<std::string, std::string>& headers,
                               const std::string& body, long timeout) {
    HttpResponse response;
    response.statusCode = 200;
    response.body = "{\"error\":\"HTTP support not available (libcurl not found)\"}";
    response.error = "libcurl not available";
    response.responseTime = 0;
    
    LOGE("HTTP 请求失败: libcurl 不可用");
    return response;
}

#endif

/**
 * 解析请求头字符串
 */
std::map<std::string, std::string> parseHeaders(const std::string& headersStr) {
    std::map<std::string, std::string> headers;
    
    if (headersStr.empty()) {
        return headers;
    }
    
    std::istringstream stream(headersStr);
    std::string line;
    
    while (std::getline(stream, line)) {
        size_t colonPos = line.find(':');
        if (colonPos != std::string::npos) {
            std::string key = line.substr(0, colonPos);
            std::string value = line.substr(colonPos + 1);
            
            // 去除空白字符
            key.erase(0, key.find_first_not_of(" \t\r\n"));
            key.erase(key.find_last_not_of(" \t\r\n") + 1);
            value.erase(0, value.find_first_not_of(" \t\r\n"));
            value.erase(value.find_last_not_of(" \t\r\n") + 1);
            
            headers[key] = value;
        }
    }
    
    return headers;
}

/**
 * 将 HttpResponse 转换为 JSON 字符串
 */
std::string httpResponseToJson(const HttpResponse& response) {
    std::ostringstream json;
    json << "{";
    json << "\"statusCode\":" << response.statusCode << ",";
    json << "\"body\":\"" << response.body << "\",";
    json << "\"responseTime\":" << response.responseTime << ",";
    json << "\"error\":\"" << response.error << "\",";
    json << "\"headers\":{";
    
    bool first = true;
    for (const auto& header : response.headers) {
        if (!first) json << ",";
        json << "\"" << header.first << "\":\"" << header.second << "\"";
        first = false;
    }
    
    json << "}}";
    return json.str();
}

extern "C" {

/**
 * 执行 HTTP GET 请求
 */
JNIEXPORT jstring JNICALL
Java_top_cywin_onetv_film_network_EnhancedOkHttpManager_nativeGet(
    JNIEnv *env, jobject thiz, jstring url, jstring headers) {
    
    const char* urlStr = env->GetStringUTFChars(url, nullptr);
    const char* headersStr = env->GetStringUTFChars(headers, nullptr);
    
    LOGD("执行 GET 请求: %s", urlStr);
    
    try {
        std::map<std::string, std::string> headerMap = parseHeaders(headersStr);
        HttpResponse response = performHttpRequest(urlStr, "GET", headerMap, "", 30);
        
        std::string jsonResult = httpResponseToJson(response);
        
        env->ReleaseStringUTFChars(url, urlStr);
        env->ReleaseStringUTFChars(headers, headersStr);
        
        LOGD("GET 请求完成: 状态码=%ld, 耗时=%ldms", response.statusCode, response.responseTime);
        return env->NewStringUTF(jsonResult.c_str());
        
    } catch (const std::exception& e) {
        LOGE("GET 请求失败: %s", e.what());
        
        env->ReleaseStringUTFChars(url, urlStr);
        env->ReleaseStringUTFChars(headers, headersStr);
        
        HttpResponse errorResponse;
        errorResponse.statusCode = 0;
        errorResponse.error = e.what();
        std::string jsonResult = httpResponseToJson(errorResponse);
        
        return env->NewStringUTF(jsonResult.c_str());
    }
}

/**
 * 执行 HTTP POST 请求
 */
JNIEXPORT jstring JNICALL
Java_top_cywin_onetv_film_network_EnhancedOkHttpManager_nativePost(
    JNIEnv *env, jobject thiz, jstring url, jstring headers, jstring body) {
    
    const char* urlStr = env->GetStringUTFChars(url, nullptr);
    const char* headersStr = env->GetStringUTFChars(headers, nullptr);
    const char* bodyStr = env->GetStringUTFChars(body, nullptr);
    
    LOGD("执行 POST 请求: %s", urlStr);
    
    try {
        std::map<std::string, std::string> headerMap = parseHeaders(headersStr);
        HttpResponse response = performHttpRequest(urlStr, "POST", headerMap, bodyStr, 30);
        
        std::string jsonResult = httpResponseToJson(response);
        
        env->ReleaseStringUTFChars(url, urlStr);
        env->ReleaseStringUTFChars(headers, headersStr);
        env->ReleaseStringUTFChars(body, bodyStr);
        
        LOGD("POST 请求完成: 状态码=%ld, 耗时=%ldms", response.statusCode, response.responseTime);
        return env->NewStringUTF(jsonResult.c_str());
        
    } catch (const std::exception& e) {
        LOGE("POST 请求失败: %s", e.what());
        
        env->ReleaseStringUTFChars(url, urlStr);
        env->ReleaseStringUTFChars(headers, headersStr);
        env->ReleaseStringUTFChars(body, bodyStr);
        
        HttpResponse errorResponse;
        errorResponse.statusCode = 0;
        errorResponse.error = e.what();
        std::string jsonResult = httpResponseToJson(errorResponse);
        
        return env->NewStringUTF(jsonResult.c_str());
    }
}

/**
 * 通用 HTTP 请求接口
 */
JNIEXPORT jstring JNICALL
Java_top_cywin_onetv_film_network_EnhancedOkHttpManager_nativeRequest(
    JNIEnv *env, jobject thiz, jstring method, jstring url, jstring headers, jstring body, jint timeout) {
    
    const char* methodStr = env->GetStringUTFChars(method, nullptr);
    const char* urlStr = env->GetStringUTFChars(url, nullptr);
    const char* headersStr = env->GetStringUTFChars(headers, nullptr);
    const char* bodyStr = env->GetStringUTFChars(body, nullptr);
    
    LOGD("执行 %s 请求: %s", methodStr, urlStr);
    
    try {
        std::map<std::string, std::string> headerMap = parseHeaders(headersStr);
        HttpResponse response = performHttpRequest(urlStr, methodStr, headerMap, bodyStr, timeout);
        
        std::string jsonResult = httpResponseToJson(response);
        
        env->ReleaseStringUTFChars(method, methodStr);
        env->ReleaseStringUTFChars(url, urlStr);
        env->ReleaseStringUTFChars(headers, headersStr);
        env->ReleaseStringUTFChars(body, bodyStr);
        
        LOGD("%s 请求完成: 状态码=%ld, 耗时=%ldms", methodStr, response.statusCode, response.responseTime);
        return env->NewStringUTF(jsonResult.c_str());
        
    } catch (const std::exception& e) {
        LOGE("%s 请求失败: %s", methodStr, e.what());
        
        env->ReleaseStringUTFChars(method, methodStr);
        env->ReleaseStringUTFChars(url, urlStr);
        env->ReleaseStringUTFChars(headers, headersStr);
        env->ReleaseStringUTFChars(body, bodyStr);
        
        HttpResponse errorResponse;
        errorResponse.statusCode = 0;
        errorResponse.error = e.what();
        std::string jsonResult = httpResponseToJson(errorResponse);
        
        return env->NewStringUTF(jsonResult.c_str());
    }
}

} // extern "C"
