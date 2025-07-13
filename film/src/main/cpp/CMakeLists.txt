cmake_minimum_required(VERSION 3.22.1)

project("film-native")

# 设置 C++ 标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 设置编译选项
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++17 -Wall -Wextra -O2")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g -DDEBUG")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -DNDEBUG")

# 查找 QuickJS 库
find_library(quickjs-lib quickjs)

# 设置 QuickJS 源码路径
set(QUICKJS_DIR ${CMAKE_CURRENT_SOURCE_DIR}/quickjs)

# 设置 libcurl 路径
set(CURL_DIR ${CMAKE_CURRENT_SOURCE_DIR}/curl)

# 查找预编译的 libcurl
find_library(CURL_LIBRARY
    NAMES curl libcurl
    PATHS
        ${CURL_DIR}/lib/${ANDROID_ABI}
        ${CMAKE_CURRENT_SOURCE_DIR}/libs/${ANDROID_ABI}
        /usr/lib
        /usr/local/lib
    NO_DEFAULT_PATH
)

# 如果找不到预编译的 libcurl，尝试系统库
if(NOT CURL_LIBRARY)
    find_library(CURL_LIBRARY curl)
endif()

# 设置 libcurl 头文件路径
set(CURL_INCLUDE_DIRS
    ${CURL_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    /usr/include/curl
    /usr/local/include/curl
)

# 创建共享库
add_library(film-native SHARED
    quickjs-android.cpp
    jsoup-bridge.cpp
    http-bridge.cpp
    spider-bridge.cpp
)

# 查找系统库
find_library(log-lib log)
find_library(android-lib android)

# 设置包含目录
target_include_directories(film-native PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${QUICKJS_DIR}
    ${CURL_INCLUDE_DIRS}
)

# 链接库
target_link_libraries(film-native
    ${log-lib}
    ${android-lib}
)

# 如果找到了 libcurl，则链接它
if(CURL_LIBRARY)
    target_link_libraries(film-native ${CURL_LIBRARY})
    target_compile_definitions(film-native PRIVATE HAVE_CURL)
    message(STATUS "HTTP support enabled with libcurl: ${CURL_LIBRARY}")
else()
    target_compile_definitions(film-native PRIVATE NO_CURL)
    message(WARNING "HTTP support disabled (libcurl not found)")
    message(STATUS "To enable HTTP support, install libcurl:")
    message(STATUS "  - Download from: https://github.com/gcesarmza/curl-android-ios")
    message(STATUS "  - Or install system package: sudo apt-get install libcurl4-openssl-dev")
endif()

# 如果使用本地 QuickJS 源码
if(EXISTS ${QUICKJS_DIR}/quickjs.c)
    target_link_libraries(film-native quickjs)
    target_compile_definitions(film-native PRIVATE HAVE_QUICKJS_SOURCE)
    message(STATUS "QuickJS source found: ${QUICKJS_DIR}")
else()
    message(WARNING "QuickJS source not found at: ${QUICKJS_DIR}")
    message(STATUS "To enable QuickJS support:")
    message(STATUS "  git clone https://github.com/bellard/quickjs.git ${QUICKJS_DIR}")
endif()

# 设置编译定义
target_compile_definitions(film-native PRIVATE
    ANDROID
    __ANDROID__
    ONETV_FILM_VERSION="2.1.1"
)

# 调试信息
message(STATUS "=== OneTV Film Native Build Configuration ===")
message(STATUS "Android ABI: ${ANDROID_ABI}")
message(STATUS "Android API Level: ${ANDROID_PLATFORM}")
message(STATUS "CMake Build Type: ${CMAKE_BUILD_TYPE}")
message(STATUS "QuickJS Directory: ${QUICKJS_DIR}")
message(STATUS "libcurl Library: ${CURL_LIBRARY}")
message(STATUS "libcurl Include: ${CURL_INCLUDE_DIRS}")
message(STATUS "C++ Standard: ${CMAKE_CXX_STANDARD}")
message(STATUS "==============================================")
