#!/bin/bash

# OneTV Film 原生代码构建测试脚本
# 
# 用于测试 C++ 桥接文件的编译
#
# @author OneTV Team
# @since 2025-07-13

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CPP_DIR="$SCRIPT_DIR"

echo -e "${BLUE}=== OneTV Film 原生代码构建测试 ===${NC}"
echo -e "${BLUE}脚本目录: $SCRIPT_DIR${NC}"
echo ""

# 函数：打印状态
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 函数：检查文件是否存在
check_file() {
    if [ -f "$1" ]; then
        print_status "✅ 找到文件: $1"
        return 0
    else
        print_error "❌ 缺少文件: $1"
        return 1
    fi
}

# 函数：检查目录是否存在
check_directory() {
    if [ -d "$1" ]; then
        print_status "✅ 找到目录: $1"
        return 0
    else
        print_warning "⚠️ 缺少目录: $1"
        return 1
    fi
}

# 函数：检查必要文件
check_required_files() {
    print_status "检查必要文件..."
    
    local all_files_exist=true
    
    # 检查 C++ 源文件
    if ! check_file "$CPP_DIR/quickjs-android.cpp"; then
        all_files_exist=false
    fi
    
    if ! check_file "$CPP_DIR/jsoup-bridge.cpp"; then
        all_files_exist=false
    fi
    
    if ! check_file "$CPP_DIR/http-bridge.cpp"; then
        all_files_exist=false
    fi
    
    if ! check_file "$CPP_DIR/spider-bridge.cpp"; then
        all_files_exist=false
    fi
    
    # 检查 CMakeLists.txt
    if ! check_file "$CPP_DIR/CMakeLists.txt"; then
        all_files_exist=false
    fi
    
    # 检查依赖
    if ! check_directory "$CPP_DIR/quickjs"; then
        print_warning "QuickJS 源码未找到，运行 ./setup-deps.sh 安装依赖"
    fi
    
    if ! check_directory "$CPP_DIR/curl"; then
        print_warning "libcurl 未找到，HTTP 功能将被禁用"
    fi
    
    if [ "$all_files_exist" = true ]; then
        print_status "✅ 所有必要文件检查通过"
        return 0
    else
        print_error "❌ 缺少必要文件"
        return 1
    fi
}

# 函数：语法检查
syntax_check() {
    print_status "执行语法检查..."
    
    # 检查是否有 g++ 编译器
    if ! command -v g++ >/dev/null 2>&1; then
        print_warning "⚠️ g++ 编译器未找到，跳过语法检查"
        return 0
    fi
    
    local syntax_ok=true
    
    # 检查每个 C++ 文件的语法
    for cpp_file in "$CPP_DIR"/*.cpp; do
        if [ -f "$cpp_file" ]; then
            filename=$(basename "$cpp_file")
            print_status "检查语法: $filename"
            
            # 基本语法检查（不链接）
            if g++ -std=c++17 -fsyntax-only -I"$CPP_DIR" "$cpp_file" 2>/dev/null; then
                print_status "✅ $filename 语法检查通过"
            else
                print_error "❌ $filename 语法检查失败"
                syntax_ok=false
            fi
        fi
    done
    
    if [ "$syntax_ok" = true ]; then
        print_status "✅ 所有文件语法检查通过"
        return 0
    else
        print_error "❌ 语法检查失败"
        return 1
    fi
}

# 函数：CMake 配置检查
cmake_check() {
    print_status "检查 CMake 配置..."
    
    # 检查是否有 cmake
    if ! command -v cmake >/dev/null 2>&1; then
        print_warning "⚠️ cmake 未找到，跳过 CMake 检查"
        return 0
    fi
    
    # 创建临时构建目录
    BUILD_DIR="$CPP_DIR/build-test"
    rm -rf "$BUILD_DIR"
    mkdir -p "$BUILD_DIR"
    
    cd "$BUILD_DIR"
    
    print_status "运行 CMake 配置..."
    if cmake .. -DCMAKE_BUILD_TYPE=Debug 2>/dev/null; then
        print_status "✅ CMake 配置成功"
        
        # 尝试生成 Makefile
        if [ -f "Makefile" ]; then
            print_status "✅ Makefile 生成成功"
        else
            print_warning "⚠️ Makefile 未生成"
        fi
        
        cd "$CPP_DIR"
        rm -rf "$BUILD_DIR"
        return 0
    else
        print_error "❌ CMake 配置失败"
        cd "$CPP_DIR"
        rm -rf "$BUILD_DIR"
        return 1
    fi
}

# 函数：代码质量检查
code_quality_check() {
    print_status "执行代码质量检查..."
    
    local issues_found=false
    
    # 检查文件编码
    for cpp_file in "$CPP_DIR"/*.cpp; do
        if [ -f "$cpp_file" ]; then
            filename=$(basename "$cpp_file")
            
            # 检查是否包含中文注释（UTF-8 编码）
            if file "$cpp_file" | grep -q "UTF-8"; then
                print_status "✅ $filename 使用 UTF-8 编码"
            else
                print_warning "⚠️ $filename 可能不是 UTF-8 编码"
            fi
            
            # 检查行尾格式
            if file "$cpp_file" | grep -q "CRLF"; then
                print_warning "⚠️ $filename 使用 Windows 行尾格式 (CRLF)"
                issues_found=true
            fi
        fi
    done
    
    # 检查头文件包含
    print_status "检查头文件包含..."
    for cpp_file in "$CPP_DIR"/*.cpp; do
        if [ -f "$cpp_file" ]; then
            filename=$(basename "$cpp_file")
            
            # 检查是否包含必要的头文件
            if grep -q "#include <jni.h>" "$cpp_file"; then
                print_status "✅ $filename 包含 JNI 头文件"
            else
                print_error "❌ $filename 缺少 JNI 头文件"
                issues_found=true
            fi
            
            if grep -q "#include <android/log.h>" "$cpp_file"; then
                print_status "✅ $filename 包含 Android 日志头文件"
            else
                print_warning "⚠️ $filename 缺少 Android 日志头文件"
            fi
        fi
    done
    
    if [ "$issues_found" = false ]; then
        print_status "✅ 代码质量检查通过"
        return 0
    else
        print_warning "⚠️ 发现代码质量问题"
        return 1
    fi
}

# 函数：生成构建报告
generate_report() {
    print_status "生成构建报告..."
    
    REPORT_FILE="$CPP_DIR/build-report.txt"
    
    cat > "$REPORT_FILE" << EOF
OneTV Film 原生代码构建报告
生成时间: $(date)
脚本目录: $CPP_DIR

=== 文件清单 ===
EOF
    
    # 列出所有 C++ 文件
    for cpp_file in "$CPP_DIR"/*.cpp; do
        if [ -f "$cpp_file" ]; then
            filename=$(basename "$cpp_file")
            filesize=$(stat -f%z "$cpp_file" 2>/dev/null || stat -c%s "$cpp_file" 2>/dev/null || echo "unknown")
            echo "$filename ($filesize bytes)" >> "$REPORT_FILE"
        fi
    done
    
    cat >> "$REPORT_FILE" << EOF

=== 依赖状态 ===
EOF
    
    # 检查依赖状态
    if [ -d "$CPP_DIR/quickjs" ]; then
        echo "QuickJS: 已安装" >> "$REPORT_FILE"
    else
        echo "QuickJS: 未安装" >> "$REPORT_FILE"
    fi
    
    if [ -d "$CPP_DIR/curl" ]; then
        echo "libcurl: 已安装 (Android)" >> "$REPORT_FILE"
    elif command -v curl-config >/dev/null 2>&1; then
        echo "libcurl: 已安装 (系统)" >> "$REPORT_FILE"
    else
        echo "libcurl: 未安装" >> "$REPORT_FILE"
    fi
    
    cat >> "$REPORT_FILE" << EOF

=== 构建环境 ===
操作系统: $OSTYPE
EOF
    
    if command -v g++ >/dev/null 2>&1; then
        echo "g++ 版本: $(g++ --version | head -n1)" >> "$REPORT_FILE"
    else
        echo "g++: 未安装" >> "$REPORT_FILE"
    fi
    
    if command -v cmake >/dev/null 2>&1; then
        echo "cmake 版本: $(cmake --version | head -n1)" >> "$REPORT_FILE"
    else
        echo "cmake: 未安装" >> "$REPORT_FILE"
    fi
    
    print_status "✅ 构建报告已生成: $REPORT_FILE"
}

# 主函数
main() {
    local exit_code=0
    
    print_status "开始构建测试..."
    echo ""
    
    # 执行各项检查
    if ! check_required_files; then
        exit_code=1
    fi
    
    echo ""
    
    if ! syntax_check; then
        exit_code=1
    fi
    
    echo ""
    
    if ! cmake_check; then
        exit_code=1
    fi
    
    echo ""
    
    if ! code_quality_check; then
        # 代码质量问题不影响构建
        print_warning "代码质量检查发现问题，但不影响构建"
    fi
    
    echo ""
    
    generate_report
    
    echo ""
    
    if [ $exit_code -eq 0 ]; then
        print_status "🎉 构建测试通过！"
        print_status "原生代码已准备就绪，可以进行 Android 构建"
    else
        print_error "❌ 构建测试失败"
        print_status "请修复上述问题后重新测试"
    fi
    
    echo ""
    print_status "提示："
    print_status "- 运行 ./setup-deps.sh 安装依赖"
    print_status "- 在 Android Studio 中构建项目"
    print_status "- 查看 build-report.txt 了解详细信息"
    
    exit $exit_code
}

# 运行主函数
main "$@"
