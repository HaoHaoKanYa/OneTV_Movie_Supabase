#include <jni.h>
#include <string>
#include <android/log.h>
#include <memory>
#include <map>
#include <mutex>
#include <thread>
#include <future>
#include <sstream>
#include <vector>

// 条件编译 libcurl 支持
#ifdef HAVE_CURL
#include <curl/curl.h>
#define HTTP_SUPPORT_ENABLED 1
#else
#define HTTP_SUPPORT_ENABLED 0
#endif

// QuickJS 头文件 - 生产环境版本
extern "C" {
#include "quickjs.h"
#include "quickjs-libc.h"
}

#define LOG_TAG "ONETV_FILM_QUICKJS_NATIVE"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

/**
 * HTTP 响应结构
 */
struct HttpResponse {
    std::string data;
    long responseCode;
    std::string contentType;
    std::map<std::string, std::string> headers;

    HttpResponse() : responseCode(0) {}
};

#if HTTP_SUPPORT_ENABLED

/**
 * HTTP 请求结构
 */
struct HttpRequest {
    std::string url;
    std::string method;
    std::string data;
    std::map<std::string, std::string> headers;
    long timeout;

    HttpRequest() : method("GET"), timeout(15000) {}
};

/**
 * libcurl 写入回调函数
 */
static size_t WriteCallback(void* contents, size_t size, size_t nmemb, HttpResponse* response) {
    size_t totalSize = size * nmemb;
    response->data.append(static_cast<char*>(contents), totalSize);
    return totalSize;
}

/**
 * libcurl 头部回调函数
 */
static size_t HeaderCallback(char* buffer, size_t size, size_t nitems, HttpResponse* response) {
    size_t totalSize = size * nitems;
    std::string header(buffer, totalSize);

    // 解析头部
    size_t colonPos = header.find(':');
    if (colonPos != std::string::npos) {
        std::string key = header.substr(0, colonPos);
        std::string value = header.substr(colonPos + 1);

        // 去除空白字符
        key.erase(0, key.find_first_not_of(" \t"));
        key.erase(key.find_last_not_of(" \t\r\n") + 1);
        value.erase(0, value.find_first_not_of(" \t"));
        value.erase(value.find_last_not_of(" \t\r\n") + 1);

        response->headers[key] = value;

        // 特殊处理 Content-Type
        if (key == "Content-Type" || key == "content-type") {
            response->contentType = value;
        }
    }

    return totalSize;
}

/**
 * 执行真实的 HTTP 请求
 */
static HttpResponse performHttpRequest(const HttpRequest& request) {
    HttpResponse response;

    CURL* curl = curl_easy_init();
    if (!curl) {
        LOGE("❌ 初始化 CURL 失败");
        response.responseCode = -1;
        response.data = "ERROR: Failed to initialize CURL";
        return response;
    }

    try {
        // 设置 URL
        curl_easy_setopt(curl, CURLOPT_URL, request.url.c_str());

        // 设置写入回调
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, &response);

        // 设置头部回调
        curl_easy_setopt(curl, CURLOPT_HEADERFUNCTION, HeaderCallback);
        curl_easy_setopt(curl, CURLOPT_HEADERDATA, &response);

        // 设置超时
        curl_easy_setopt(curl, CURLOPT_TIMEOUT_MS, request.timeout);
        curl_easy_setopt(curl, CURLOPT_CONNECTTIMEOUT_MS, 10000L); // 10秒连接超时

        // 设置 User-Agent
        curl_easy_setopt(curl, CURLOPT_USERAGENT, "OneTV-QuickJS/1.0.0 (Android)");

        // 跟随重定向
        curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);
        curl_easy_setopt(curl, CURLOPT_MAXREDIRS, 5L);

        // SSL 设置
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 1L);
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 2L);

        // 设置请求方法
        if (request.method == "POST") {
            curl_easy_setopt(curl, CURLOPT_POST, 1L);
            if (!request.data.empty()) {
                curl_easy_setopt(curl, CURLOPT_POSTFIELDS, request.data.c_str());
                curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, request.data.length());
            }
        } else if (request.method == "PUT") {
            curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, "PUT");
            if (!request.data.empty()) {
                curl_easy_setopt(curl, CURLOPT_POSTFIELDS, request.data.c_str());
                curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, request.data.length());
            }
        } else if (request.method == "DELETE") {
            curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, "DELETE");
        } else if (request.method == "HEAD") {
            curl_easy_setopt(curl, CURLOPT_NOBODY, 1L);
        }

        // 设置自定义头部
        struct curl_slist* headers = nullptr;
        for (const auto& header : request.headers) {
            std::string headerStr = header.first + ": " + header.second;
            headers = curl_slist_append(headers, headerStr.c_str());
        }

        if (headers) {
            curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
        }

        // 执行请求
        LOGD("🌐 执行 HTTP 请求: %s %s", request.method.c_str(), request.url.c_str());
        CURLcode res = curl_easy_perform(curl);

        if (res != CURLE_OK) {
            LOGE("❌ HTTP 请求失败: %s", curl_easy_strerror(res));
            response.responseCode = -1;
            response.data = "ERROR: " + std::string(curl_easy_strerror(res));
        } else {
            // 获取响应码
            curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &response.responseCode);
            LOGD("✅ HTTP 请求成功: %ld, 数据长度: %zu", response.responseCode, response.data.length());
        }

        // 清理
        if (headers) {
            curl_slist_free_all(headers);
        }

    } catch (const std::exception& e) {
        LOGE("❌ HTTP 请求异常: %s", e.what());
        response.responseCode = -1;
        response.data = "ERROR: " + std::string(e.what());
    }

    curl_easy_cleanup(curl);
    return response;
}

/**
 * 异步执行 HTTP 请求
 */
static std::future<HttpResponse> performHttpRequestAsync(const HttpRequest& request) {
    return std::async(std::launch::async, [request]() {
        return performHttpRequest(request);
    });
}

#else // !HTTP_SUPPORT_ENABLED

/**
 * 模拟 HTTP 请求（当 libcurl 不可用时）
 */
static HttpResponse performHttpRequest(const HttpRequest& request) {
    HttpResponse response;
    response.responseCode = 200;
    response.contentType = "application/json";

    // 返回模拟响应，说明 HTTP 功能不可用
    response.data = R"({
        "error": "HTTP_NOT_SUPPORTED",
        "message": "libcurl not available. Please install libcurl to enable HTTP functionality.",
        "url": ")" + request.url + R"(",
        "method": ")" + request.method + R"("
    })";

    LOGE("❌ HTTP 功能不可用: libcurl 未安装");
    return response;
}

/**
 * 模拟异步 HTTP 请求
 */
static std::future<HttpResponse> performHttpRequestAsync(const HttpRequest& request) {
    return std::async(std::launch::async, [request]() {
        return performHttpRequest(request);
    });
}

#endif // HTTP_SUPPORT_ENABLED

// 真实的 QuickJS 上下文包装器
struct QuickJSContextWrapper {
    JSRuntime* runtime;
    JSContext* context;
    std::map<std::string, std::string> globals;
    std::mutex mutex;
    bool isValid;

    QuickJSContextWrapper() : runtime(nullptr), context(nullptr), isValid(false) {
#if HTTP_SUPPORT_ENABLED
        // 初始化 CURL（全局初始化，线程安全）
        static std::once_flag curlInitFlag;
        std::call_once(curlInitFlag, []() {
            CURLcode res = curl_global_init(CURL_GLOBAL_DEFAULT);
            if (res != CURLE_OK) {
                LOGE("❌ 初始化 CURL 全局环境失败: %s", curl_easy_strerror(res));
            } else {
                LOGD("✅ CURL 全局环境初始化成功");
            }
        });
#else
        LOGI("ℹ️ HTTP 功能已禁用（libcurl 不可用）");
#endif

        // 创建 QuickJS 运行时
        runtime = JS_NewRuntime();
        if (!runtime) {
            LOGE("❌ 创建 QuickJS 运行时失败");
            return;
        }

        // 设置内存限制 (32MB)
        JS_SetMemoryLimit(runtime, 32 * 1024 * 1024);

        // 设置最大栈大小 (512KB)
        JS_SetMaxStackSize(runtime, 512 * 1024);

        // 创建 QuickJS 上下文
        context = JS_NewContext(runtime);
        if (!context) {
            LOGE("❌ 创建 QuickJS 上下文失败");
            JS_FreeRuntime(runtime);
            runtime = nullptr;
            return;
        }

        // 初始化标准库
        js_init_module_std(context, "std");
        js_init_module_os(context, "os");

        // 添加控制台支持
        initConsole();

        // 添加 HTTP 支持
        initHttp();

        isValid = true;
        LOGD("✅ QuickJS 上下文初始化成功");
    }

    ~QuickJSContextWrapper() {
        if (context) {
            JS_FreeContext(context);
            context = nullptr;
        }
        if (runtime) {
            JS_FreeRuntime(runtime);
            runtime = nullptr;
        }
        isValid = false;
        LOGD("🗑️ QuickJS 上下文已销毁");
    }

private:
    void initConsole() {
        if (!context) return;

        JSValue global = JS_GetGlobalObject(context);
        JSValue console = JS_NewObject(context);

        // 添加 console 方法
        JS_SetPropertyStr(context, console, "log",
            JS_NewCFunction(context, js_console_log, "log", 1));
        JS_SetPropertyStr(context, console, "error",
            JS_NewCFunction(context, js_console_error, "error", 1));
        JS_SetPropertyStr(context, console, "warn",
            JS_NewCFunction(context, js_console_warn, "warn", 1));
        JS_SetPropertyStr(context, console, "info",
            JS_NewCFunction(context, js_console_info, "info", 1));

        JS_SetPropertyStr(context, global, "console", console);
        JS_FreeValue(context, global);
    }

    void initHttp() {
        if (!context) return;

        JSValue global = JS_GetGlobalObject(context);

        // 添加完整的 HTTP 请求函数
        JS_SetPropertyStr(context, global, "httpGet",
            JS_NewCFunction(context, js_http_get, "httpGet", 2));
        JS_SetPropertyStr(context, global, "httpPost",
            JS_NewCFunction(context, js_http_post, "httpPost", 3));
        JS_SetPropertyStr(context, global, "httpPut",
            JS_NewCFunction(context, js_http_put, "httpPut", 3));
        JS_SetPropertyStr(context, global, "httpDelete",
            JS_NewCFunction(context, js_http_delete, "httpDelete", 2));
        JS_SetPropertyStr(context, global, "httpHead",
            JS_NewCFunction(context, js_http_head, "httpHead", 2));

        // 添加通用的 HTTP 请求函数
        JS_SetPropertyStr(context, global, "httpRequest",
            JS_NewCFunction(context, js_http_request, "httpRequest", 1));

        // 添加异步 HTTP 请求函数
        JS_SetPropertyStr(context, global, "httpGetAsync",
            JS_NewCFunction(context, js_http_get_async, "httpGetAsync", 2));
        JS_SetPropertyStr(context, global, "httpPostAsync",
            JS_NewCFunction(context, js_http_post_async, "httpPostAsync", 3));

        JS_FreeValue(context, global);
    }

    // Console 方法实现
    static JSValue js_console_log(JSContext *ctx, JSValueConst this_val, int argc, JSValueConst *argv) {
        return js_console_output(ctx, argc, argv, ANDROID_LOG_INFO, "LOG");
    }

    static JSValue js_console_error(JSContext *ctx, JSValueConst this_val, int argc, JSValueConst *argv) {
        return js_console_output(ctx, argc, argv, ANDROID_LOG_ERROR, "ERROR");
    }

    static JSValue js_console_warn(JSContext *ctx, JSValueConst this_val, int argc, JSValueConst *argv) {
        return js_console_output(ctx, argc, argv, ANDROID_LOG_WARN, "WARN");
    }

    static JSValue js_console_info(JSContext *ctx, JSValueConst this_val, int argc, JSValueConst *argv) {
        return js_console_output(ctx, argc, argv, ANDROID_LOG_INFO, "INFO");
    }

    static JSValue js_console_output(JSContext *ctx, int argc, JSValueConst *argv, int level, const char* prefix) {
        std::string output = std::string("[") + prefix + "] ";
        for (int i = 0; i < argc; i++) {
            if (i > 0) output += " ";

            const char* str = JS_ToCString(ctx, argv[i]);
            if (str) {
                output += str;
                JS_FreeCString(ctx, str);
            }
        }

        __android_log_print(level, "QuickJS-Console", "%s", output.c_str());
        return JS_UNDEFINED;
    }

    // HTTP GET 方法实现（生产版）
    static JSValue js_http_get(JSContext *ctx, JSValueConst this_val, int argc, JSValueConst *argv) {
        if (argc < 1) {
            return JS_ThrowTypeError(ctx, "httpGet requires at least 1 argument");
        }

        const char* url = JS_ToCString(ctx, argv[0]);
        if (!url) {
            return JS_ThrowTypeError(ctx, "URL must be a string");
        }

        try {
            // 构建 HTTP 请求
            HttpRequest request;
            request.url = url;
            request.method = "GET";
            request.timeout = 15000L; // 15秒超时

            // 添加默认头部
            request.headers["Accept"] = "application/json, text/plain, */*";
            request.headers["Accept-Language"] = "zh-CN,zh;q=0.9,en;q=0.8";

            // 解析可选的头部参数
            if (argc >= 2 && JS_IsObject(argv[1])) {
                JSPropertyEnum *props;
                uint32_t propCount;

                if (JS_GetOwnPropertyNames(ctx, &props, &propCount, argv[1], JS_GPN_STRING_MASK) == 0) {
                    for (uint32_t i = 0; i < propCount; i++) {
                        JSValue key = JS_AtomToString(ctx, props[i].atom);
                        JSValue value = JS_GetProperty(ctx, argv[1], props[i].atom);

                        const char* keyStr = JS_ToCString(ctx, key);
                        const char* valueStr = JS_ToCString(ctx, value);

                        if (keyStr && valueStr) {
                            request.headers[keyStr] = valueStr;
                        }

                        if (keyStr) JS_FreeCString(ctx, keyStr);
                        if (valueStr) JS_FreeCString(ctx, valueStr);

                        JS_FreeValue(ctx, key);
                        JS_FreeValue(ctx, value);
                    }
                    js_free(ctx, props);
                }
            }

            LOGD("🌐 执行 HTTP GET: %s", url);

            // 执行真实的 HTTP 请求
            HttpResponse response = performHttpRequest(request);

            JS_FreeCString(ctx, url);

            // 构建响应对象
            JSValue responseObj = JS_NewObject(ctx);
            JS_SetPropertyStr(ctx, responseObj, "status", JS_NewInt32(ctx, response.responseCode));
            JS_SetPropertyStr(ctx, responseObj, "data", JS_NewString(ctx, response.data.c_str()));
            JS_SetPropertyStr(ctx, responseObj, "contentType", JS_NewString(ctx, response.contentType.c_str()));

            // 添加响应头
            JSValue headersObj = JS_NewObject(ctx);
            for (const auto& header : response.headers) {
                JS_SetPropertyStr(ctx, headersObj, header.first.c_str(), JS_NewString(ctx, header.second.c_str()));
            }
            JS_SetPropertyStr(ctx, responseObj, "headers", headersObj);

            return responseObj;

        } catch (const std::exception& e) {
            JS_FreeCString(ctx, url);
            LOGE("❌ HTTP GET 请求异常: %s", e.what());
            return JS_ThrowInternalError(ctx, "HTTP request failed: %s", e.what());
        }
    }

    static JSValue js_http_post(JSContext *ctx, JSValueConst this_val, int argc, JSValueConst *argv) {
        if (argc < 2) {
            return JS_ThrowTypeError(ctx, "httpPost requires at least 2 arguments");
        }

        const char* url = JS_ToCString(ctx, argv[0]);
        const char* data = JS_ToCString(ctx, argv[1]);

        if (!url || !data) {
            if (url) JS_FreeCString(ctx, url);
            if (data) JS_FreeCString(ctx, data);
            return JS_ThrowTypeError(ctx, "URL and data must be strings");
        }

        try {
            // 构建 HTTP POST 请求
            HttpRequest request;
            request.url = url;
            request.method = "POST";
            request.data = data;
            request.timeout = 15000L; // 15秒超时

            // 添加默认头部
            request.headers["Accept"] = "application/json, text/plain, */*";
            request.headers["Content-Type"] = "application/json; charset=UTF-8";
            request.headers["Accept-Language"] = "zh-CN,zh;q=0.9,en;q=0.8";

            // 解析可选的头部参数
            if (argc >= 3 && JS_IsObject(argv[2])) {
                JSPropertyEnum *props;
                uint32_t propCount;

                if (JS_GetOwnPropertyNames(ctx, &props, &propCount, argv[2], JS_GPN_STRING_MASK) == 0) {
                    for (uint32_t i = 0; i < propCount; i++) {
                        JSValue key = JS_AtomToString(ctx, props[i].atom);
                        JSValue value = JS_GetProperty(ctx, argv[2], props[i].atom);

                        const char* keyStr = JS_ToCString(ctx, key);
                        const char* valueStr = JS_ToCString(ctx, value);

                        if (keyStr && valueStr) {
                            request.headers[keyStr] = valueStr;
                        }

                        if (keyStr) JS_FreeCString(ctx, keyStr);
                        if (valueStr) JS_FreeCString(ctx, valueStr);

                        JS_FreeValue(ctx, key);
                        JS_FreeValue(ctx, value);
                    }
                    js_free(ctx, props);
                }
            }

            LOGD("🌐 执行 HTTP POST: %s, 数据长度: %zu", url, strlen(data));

            // 执行真实的 HTTP POST 请求
            HttpResponse response = performHttpRequest(request);

            JS_FreeCString(ctx, url);
            JS_FreeCString(ctx, data);

            // 构建响应对象
            JSValue responseObj = JS_NewObject(ctx);
            JS_SetPropertyStr(ctx, responseObj, "status", JS_NewInt32(ctx, response.responseCode));
            JS_SetPropertyStr(ctx, responseObj, "data", JS_NewString(ctx, response.data.c_str()));
            JS_SetPropertyStr(ctx, responseObj, "contentType", JS_NewString(ctx, response.contentType.c_str()));

            // 添加响应头
            JSValue headersObj = JS_NewObject(ctx);
            for (const auto& header : response.headers) {
                JS_SetPropertyStr(ctx, headersObj, header.first.c_str(), JS_NewString(ctx, header.second.c_str()));
            }
            JS_SetPropertyStr(ctx, responseObj, "headers", headersObj);

            return responseObj;

        } catch (const std::exception& e) {
            JS_FreeCString(ctx, url);
            JS_FreeCString(ctx, data);
            LOGE("❌ HTTP POST 请求异常: %s", e.what());
            return JS_ThrowInternalError(ctx, "HTTP POST request failed: %s", e.what());
        }
    }

    // HTTP PUT 方法实现
    static JSValue js_http_put(JSContext *ctx, JSValueConst this_val, int argc, JSValueConst *argv) {
        if (argc < 2) {
            return JS_ThrowTypeError(ctx, "httpPut requires at least 2 arguments");
        }

        const char* url = JS_ToCString(ctx, argv[0]);
        const char* data = JS_ToCString(ctx, argv[1]);

        if (!url || !data) {
            if (url) JS_FreeCString(ctx, url);
            if (data) JS_FreeCString(ctx, data);
            return JS_ThrowTypeError(ctx, "URL and data must be strings");
        }

        try {
            HttpRequest request;
            request.url = url;
            request.method = "PUT";
            request.data = data;
            request.timeout = 15000L;

            request.headers["Accept"] = "application/json, text/plain, */*";
            request.headers["Content-Type"] = "application/json; charset=UTF-8";

            HttpResponse response = performHttpRequest(request);

            JS_FreeCString(ctx, url);
            JS_FreeCString(ctx, data);

            JSValue responseObj = JS_NewObject(ctx);
            JS_SetPropertyStr(ctx, responseObj, "status", JS_NewInt32(ctx, response.responseCode));
            JS_SetPropertyStr(ctx, responseObj, "data", JS_NewString(ctx, response.data.c_str()));

            return responseObj;

        } catch (const std::exception& e) {
            JS_FreeCString(ctx, url);
            JS_FreeCString(ctx, data);
            return JS_ThrowInternalError(ctx, "HTTP PUT request failed: %s", e.what());
        }
    }

    // HTTP DELETE 方法实现
    static JSValue js_http_delete(JSContext *ctx, JSValueConst this_val, int argc, JSValueConst *argv) {
        if (argc < 1) {
            return JS_ThrowTypeError(ctx, "httpDelete requires at least 1 argument");
        }

        const char* url = JS_ToCString(ctx, argv[0]);
        if (!url) {
            return JS_ThrowTypeError(ctx, "URL must be a string");
        }

        try {
            HttpRequest request;
            request.url = url;
            request.method = "DELETE";
            request.timeout = 15000L;

            HttpResponse response = performHttpRequest(request);

            JS_FreeCString(ctx, url);

            JSValue responseObj = JS_NewObject(ctx);
            JS_SetPropertyStr(ctx, responseObj, "status", JS_NewInt32(ctx, response.responseCode));
            JS_SetPropertyStr(ctx, responseObj, "data", JS_NewString(ctx, response.data.c_str()));

            return responseObj;

        } catch (const std::exception& e) {
            JS_FreeCString(ctx, url);
            return JS_ThrowInternalError(ctx, "HTTP DELETE request failed: %s", e.what());
        }
    }

    // HTTP HEAD 方法实现
    static JSValue js_http_head(JSContext *ctx, JSValueConst this_val, int argc, JSValueConst *argv) {
        if (argc < 1) {
            return JS_ThrowTypeError(ctx, "httpHead requires at least 1 argument");
        }

        const char* url = JS_ToCString(ctx, argv[0]);
        if (!url) {
            return JS_ThrowTypeError(ctx, "URL must be a string");
        }

        try {
            HttpRequest request;
            request.url = url;
            request.method = "HEAD";
            request.timeout = 15000L;

            HttpResponse response = performHttpRequest(request);

            JS_FreeCString(ctx, url);

            JSValue responseObj = JS_NewObject(ctx);
            JS_SetPropertyStr(ctx, responseObj, "status", JS_NewInt32(ctx, response.responseCode));

            // HEAD 请求只返回头部信息
            JSValue headersObj = JS_NewObject(ctx);
            for (const auto& header : response.headers) {
                JS_SetPropertyStr(ctx, headersObj, header.first.c_str(), JS_NewString(ctx, header.second.c_str()));
            }
            JS_SetPropertyStr(ctx, responseObj, "headers", headersObj);

            return responseObj;

        } catch (const std::exception& e) {
            JS_FreeCString(ctx, url);
            return JS_ThrowInternalError(ctx, "HTTP HEAD request failed: %s", e.what());
        }
    }

    // 通用 HTTP 请求方法实现
    static JSValue js_http_request(JSContext *ctx, JSValueConst this_val, int argc, JSValueConst *argv) {
        if (argc < 1 || !JS_IsObject(argv[0])) {
            return JS_ThrowTypeError(ctx, "httpRequest requires an options object");
        }

        try {
            HttpRequest request;

            // 解析请求选项
            JSValue urlVal = JS_GetPropertyStr(ctx, argv[0], "url");
            JSValue methodVal = JS_GetPropertyStr(ctx, argv[0], "method");
            JSValue dataVal = JS_GetPropertyStr(ctx, argv[0], "data");
            JSValue headersVal = JS_GetPropertyStr(ctx, argv[0], "headers");
            JSValue timeoutVal = JS_GetPropertyStr(ctx, argv[0], "timeout");

            // URL（必需）
            const char* url = JS_ToCString(ctx, urlVal);
            if (!url) {
                JS_FreeValue(ctx, urlVal);
                JS_FreeValue(ctx, methodVal);
                JS_FreeValue(ctx, dataVal);
                JS_FreeValue(ctx, headersVal);
                JS_FreeValue(ctx, timeoutVal);
                return JS_ThrowTypeError(ctx, "URL is required");
            }
            request.url = url;
            JS_FreeCString(ctx, url);

            // 方法（可选，默认GET）
            const char* method = JS_ToCString(ctx, methodVal);
            if (method) {
                request.method = method;
                JS_FreeCString(ctx, method);
            }

            // 数据（可选）
            const char* data = JS_ToCString(ctx, dataVal);
            if (data) {
                request.data = data;
                JS_FreeCString(ctx, data);
            }

            // 超时（可选）
            int32_t timeout;
            if (JS_ToInt32(ctx, &timeout, timeoutVal) == 0 && timeout > 0) {
                request.timeout = timeout;
            }

            // 头部（可选）
            if (JS_IsObject(headersVal)) {
                JSPropertyEnum *props;
                uint32_t propCount;

                if (JS_GetOwnPropertyNames(ctx, &props, &propCount, headersVal, JS_GPN_STRING_MASK) == 0) {
                    for (uint32_t i = 0; i < propCount; i++) {
                        JSValue key = JS_AtomToString(ctx, props[i].atom);
                        JSValue value = JS_GetProperty(ctx, headersVal, props[i].atom);

                        const char* keyStr = JS_ToCString(ctx, key);
                        const char* valueStr = JS_ToCString(ctx, value);

                        if (keyStr && valueStr) {
                            request.headers[keyStr] = valueStr;
                        }

                        if (keyStr) JS_FreeCString(ctx, keyStr);
                        if (valueStr) JS_FreeCString(ctx, valueStr);

                        JS_FreeValue(ctx, key);
                        JS_FreeValue(ctx, value);
                    }
                    js_free(ctx, props);
                }
            }

            // 清理
            JS_FreeValue(ctx, urlVal);
            JS_FreeValue(ctx, methodVal);
            JS_FreeValue(ctx, dataVal);
            JS_FreeValue(ctx, headersVal);
            JS_FreeValue(ctx, timeoutVal);

            // 执行请求
            HttpResponse response = performHttpRequest(request);

            // 构建响应对象
            JSValue responseObj = JS_NewObject(ctx);
            JS_SetPropertyStr(ctx, responseObj, "status", JS_NewInt32(ctx, response.responseCode));
            JS_SetPropertyStr(ctx, responseObj, "data", JS_NewString(ctx, response.data.c_str()));
            JS_SetPropertyStr(ctx, responseObj, "contentType", JS_NewString(ctx, response.contentType.c_str()));

            JSValue headersObj = JS_NewObject(ctx);
            for (const auto& header : response.headers) {
                JS_SetPropertyStr(ctx, headersObj, header.first.c_str(), JS_NewString(ctx, header.second.c_str()));
            }
            JS_SetPropertyStr(ctx, responseObj, "headers", headersObj);

            return responseObj;

        } catch (const std::exception& e) {
            return JS_ThrowInternalError(ctx, "HTTP request failed: %s", e.what());
        }
    }

    // 异步 HTTP GET 方法实现（简化版，返回 Promise）
    static JSValue js_http_get_async(JSContext *ctx, JSValueConst this_val, int argc, JSValueConst *argv) {
        // 注意：这里需要实现 Promise 支持，暂时返回同步结果
        return js_http_get(ctx, this_val, argc, argv);
    }

    // 异步 HTTP POST 方法实现（简化版，返回 Promise）
    static JSValue js_http_post_async(JSContext *ctx, JSValueConst this_val, int argc, JSValueConst *argv) {
        // 注意：这里需要实现 Promise 支持，暂时返回同步结果
        return js_http_post(ctx, this_val, argc, argv);
    }
};

// 全局上下文管理
static std::map<jlong, std::unique_ptr<QuickJSContextWrapper>> g_contexts;
static std::mutex g_contexts_mutex;
static jlong g_next_context_id = 1;

extern "C" {

/**
 * 创建 JavaScript 上下文
 */
JNIEXPORT jlong JNICALL
Java_top_cywin_onetv_film_engine_QuickJSEngine_createJSContext(JNIEnv *env, jobject thiz) {
    LOGD("🔧 创建 JavaScript 上下文");
    
    try {
        std::lock_guard<std::mutex> lock(g_contexts_mutex);

        // 创建真实的 QuickJS 上下文
        auto context = std::make_unique<QuickJSContextWrapper>();
        if (!context->isValid) {
            LOGE("❌ QuickJS 上下文初始化失败");
            return 0;
        }

        jlong contextId = g_next_context_id++;
        g_contexts[contextId] = std::move(context);

        LOGD("✅ JavaScript 上下文创建成功: %lld", contextId);
        return contextId;

    } catch (const std::exception& e) {
        LOGE("❌ 创建 JavaScript 上下文失败: %s", e.what());
        return 0;
    }
}

/**
 * 销毁 JavaScript 上下文
 */
JNIEXPORT void JNICALL
Java_top_cywin_onetv_film_engine_QuickJSEngine_destroyJSContext(JNIEnv *env, jobject thiz, jlong context_id) {
    LOGD("🗑️ 销毁 JavaScript 上下文: %lld", context_id);
    
    try {
        std::lock_guard<std::mutex> lock(g_contexts_mutex);
        
        auto it = g_contexts.find(context_id);
        if (it != g_contexts.end()) {
            // QuickJSContextWrapper 的析构函数会自动清理资源
            g_contexts.erase(it);

            LOGD("✅ JavaScript 上下文销毁成功: %lld", context_id);
        } else {
            LOGE("⚠️ 未找到 JavaScript 上下文: %lld", context_id);
        }
        
    } catch (const std::exception& e) {
        LOGE("❌ 销毁 JavaScript 上下文失败: %s", e.what());
    }
}

/**
 * 执行 JavaScript 代码
 */
JNIEXPORT jstring JNICALL
Java_top_cywin_onetv_film_engine_QuickJSEngine_nativeEvaluateScript(JNIEnv *env, jobject thiz, jlong context_id, jstring script) {
    const char* scriptStr = env->GetStringUTFChars(script, nullptr);
    LOGD("📜 执行 JavaScript 代码: %s", scriptStr);
    
    try {
        std::lock_guard<std::mutex> lock(g_contexts_mutex);
        
        auto it = g_contexts.find(context_id);
        if (it == g_contexts.end()) {
            env->ReleaseStringUTFChars(script, scriptStr);
            return env->NewStringUTF("ERROR: Context not found");
        }
        
        auto& contextWrapper = it->second;
        std::lock_guard<std::mutex> ctxLock(contextWrapper->mutex);

        if (!contextWrapper->isValid) {
            env->ReleaseStringUTFChars(script, scriptStr);
            return env->NewStringUTF("ERROR: Context is invalid");
        }

        // 使用真实的 QuickJS 执行 JavaScript 代码
        JSValue result = JS_Eval(contextWrapper->context, scriptStr, strlen(scriptStr), "<eval>", JS_EVAL_TYPE_GLOBAL);

        std::string resultStr;
        if (JS_IsException(result)) {
            // 处理异常
            JSValue exception = JS_GetException(contextWrapper->context);
            const char* errorStr = JS_ToCString(contextWrapper->context, exception);
            if (errorStr) {
                resultStr = std::string("ERROR: ") + errorStr;
                JS_FreeCString(contextWrapper->context, errorStr);
                LOGE("JavaScript 执行错误: %s", resultStr.c_str());
            } else {
                resultStr = "ERROR: Unknown JavaScript error";
                LOGE("未知 JavaScript 错误");
            }
            JS_FreeValue(contextWrapper->context, exception);
        } else {
            // 获取结果
            if (JS_IsUndefined(result)) {
                resultStr = "";
            } else {
                const char* resultCStr = JS_ToCString(contextWrapper->context, result);
                if (resultCStr) {
                    resultStr = resultCStr;
                    JS_FreeCString(contextWrapper->context, resultCStr);
                } else {
                    resultStr = "";
                }
            }
        }

        JS_FreeValue(contextWrapper->context, result);
        
        env->ReleaseStringUTFChars(script, scriptStr);

        LOGD("✅ JavaScript 代码执行成功");
        return env->NewStringUTF(resultStr.c_str());
        
    } catch (const std::exception& e) {
        env->ReleaseStringUTFChars(script, scriptStr);
        LOGE("❌ JavaScript 代码执行失败: %s", e.what());
        
        std::string errorMsg = "ERROR: " + std::string(e.what());
        return env->NewStringUTF(errorMsg.c_str());
    }
}

/**
 * 调用 JavaScript 函数
 */
JNIEXPORT jstring JNICALL
Java_top_cywin_onetv_film_engine_QuickJSEngine_nativeCallFunction(JNIEnv *env, jobject thiz, jlong context_id, jstring function_name, jstring args_json) {
    const char* funcName = env->GetStringUTFChars(function_name, nullptr);
    const char* argsStr = env->GetStringUTFChars(args_json, nullptr);
    
    LOGD("🔧 调用 JavaScript 函数: %s", funcName);
    
    try {
        std::lock_guard<std::mutex> lock(g_contexts_mutex);
        
        auto it = g_contexts.find(context_id);
        if (it == g_contexts.end()) {
            env->ReleaseStringUTFChars(function_name, funcName);
            env->ReleaseStringUTFChars(args_json, argsStr);
            return env->NewStringUTF("ERROR: Context not found");
        }
        
        auto& contextWrapper = it->second;
        std::lock_guard<std::mutex> ctxLock(contextWrapper->mutex);

        if (!contextWrapper->isValid) {
            env->ReleaseStringUTFChars(function_name, funcName);
            env->ReleaseStringUTFChars(args_json, argsStr);
            return env->NewStringUTF("ERROR: Context is invalid");
        }

        // 获取全局对象
        JSValue global = JS_GetGlobalObject(contextWrapper->context);

        // 获取函数
        JSValue func = JS_GetPropertyStr(contextWrapper->context, global, funcName);
        JS_FreeValue(contextWrapper->context, global);

        if (JS_IsUndefined(func)) {
            JS_FreeValue(contextWrapper->context, func);
            env->ReleaseStringUTFChars(function_name, funcName);
            env->ReleaseStringUTFChars(args_json, argsStr);

            std::string errorMsg = "ERROR: Function not found: " + std::string(funcName);
            return env->NewStringUTF(errorMsg.c_str());
        }

        // 解析参数 JSON
        JSValue args = JS_ParseJSON(contextWrapper->context, argsStr, strlen(argsStr), "<args>");
        if (JS_IsException(args)) {
            JS_FreeValue(contextWrapper->context, func);
            JS_FreeValue(contextWrapper->context, args);
            env->ReleaseStringUTFChars(function_name, funcName);
            env->ReleaseStringUTFChars(args_json, argsStr);
            return env->NewStringUTF("ERROR: Invalid JSON arguments");
        }

        // 调用函数
        JSValue result = JS_Call(contextWrapper->context, func, JS_UNDEFINED, 1, &args);

        std::string resultStr;
        if (JS_IsException(result)) {
            // 处理异常
            JSValue exception = JS_GetException(contextWrapper->context);
            const char* errorStr = JS_ToCString(contextWrapper->context, exception);
            if (errorStr) {
                resultStr = std::string("ERROR: ") + errorStr;
                JS_FreeCString(contextWrapper->context, errorStr);
            } else {
                resultStr = "ERROR: Unknown function call error";
            }
            JS_FreeValue(contextWrapper->context, exception);
        } else {
            // 获取结果
            const char* resultCStr = JS_ToCString(contextWrapper->context, result);
            if (resultCStr) {
                resultStr = resultCStr;
                JS_FreeCString(contextWrapper->context, resultCStr);
            } else {
                resultStr = "";
            }
        }

        // 清理资源
        JS_FreeValue(contextWrapper->context, func);
        JS_FreeValue(contextWrapper->context, args);
        JS_FreeValue(contextWrapper->context, result);
        
        env->ReleaseStringUTFChars(function_name, funcName);
        env->ReleaseStringUTFChars(args_json, argsStr);
        
        LOGD("✅ JavaScript 函数调用成功: %s", funcName);
        return env->NewStringUTF(resultStr.c_str());
        
    } catch (const std::exception& e) {
        env->ReleaseStringUTFChars(function_name, funcName);
        env->ReleaseStringUTFChars(args_json, argsStr);
        LOGE("❌ JavaScript 函数调用失败: %s", e.what());
        
        std::string errorMsg = "ERROR: " + std::string(e.what());
        return env->NewStringUTF(errorMsg.c_str());
    }
}

/**
 * 检查函数是否存在
 */
JNIEXPORT jboolean JNICALL
Java_top_cywin_onetv_film_engine_QuickJSEngine_nativeHasFunction(JNIEnv *env, jobject thiz, jlong context_id, jstring function_name) {
    const char* funcName = env->GetStringUTFChars(function_name, nullptr);
    LOGD("🔍 检查函数是否存在: %s", funcName);

    try {
        std::lock_guard<std::mutex> lock(g_contexts_mutex);

        auto it = g_contexts.find(context_id);
        if (it == g_contexts.end()) {
            env->ReleaseStringUTFChars(function_name, funcName);
            return JNI_FALSE;
        }

        auto& contextWrapper = it->second;
        std::lock_guard<std::mutex> ctxLock(contextWrapper->mutex);

        if (!contextWrapper->isValid) {
            env->ReleaseStringUTFChars(function_name, funcName);
            return JNI_FALSE;
        }

        // 获取全局对象
        JSValue global = JS_GetGlobalObject(contextWrapper->context);

        // 检查函数是否存在
        JSValue func = JS_GetPropertyStr(contextWrapper->context, global, funcName);
        bool exists = !JS_IsUndefined(func);

        // 清理资源
        JS_FreeValue(contextWrapper->context, global);
        JS_FreeValue(contextWrapper->context, func);

        env->ReleaseStringUTFChars(function_name, funcName);

        LOGD("🔍 函数存在性检查结果: %s = %s", funcName, exists ? "true" : "false");
        return exists ? JNI_TRUE : JNI_FALSE;

    } catch (const std::exception& e) {
        env->ReleaseStringUTFChars(function_name, funcName);
        LOGE("❌ 检查函数存在性失败: %s", e.what());
        return JNI_FALSE;
    }
}

/**
 * 获取内存使用情况
 */
JNIEXPORT jlong JNICALL
Java_top_cywin_onetv_film_engine_QuickJSEngine_nativeGetMemoryUsage(JNIEnv *env, jobject thiz, jlong context_id) {
    LOGD("💾 获取内存使用情况: %lld", context_id);

    try {
        std::lock_guard<std::mutex> lock(g_contexts_mutex);

        auto it = g_contexts.find(context_id);
        if (it == g_contexts.end()) {
            return -1;
        }

        auto& contextWrapper = it->second;
        std::lock_guard<std::mutex> ctxLock(contextWrapper->mutex);

        if (!contextWrapper->isValid) {
            return -1;
        }

        // 获取 QuickJS 运行时的内存使用情况
        JSMemoryUsage usage;
        JS_ComputeMemoryUsage(contextWrapper->runtime, &usage);
        jlong memoryUsage = static_cast<jlong>(usage.memory_used_size);

        LOGD("💾 内存使用情况: %lld bytes", memoryUsage);
        return memoryUsage;

    } catch (const std::exception& e) {
        LOGE("❌ 获取内存使用情况失败: %s", e.what());
        return -1;
    }
}

} // extern "C"
