#!/bin/bash

# OneTV Film QuickJS 依赖安装脚本
# 
# 自动下载和配置 QuickJS 和 libcurl 依赖
#
# @author OneTV Team
# @since 2025-07-12

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

echo -e "${BLUE}=== OneTV Film QuickJS 依赖安装脚本 ===${NC}"
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

# 函数：检查命令是否存在
check_command() {
    if command -v "$1" >/dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# 函数：下载 QuickJS
download_quickjs() {
    print_status "下载 QuickJS..."
    
    QUICKJS_DIR="$CPP_DIR/quickjs"
    
    if [ -d "$QUICKJS_DIR" ]; then
        print_warning "QuickJS 目录已存在: $QUICKJS_DIR"
        read -p "是否重新下载? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_status "跳过 QuickJS 下载"
            return 0
        fi
        rm -rf "$QUICKJS_DIR"
    fi
    
    if check_command git; then
        print_status "使用 git 克隆 QuickJS..."
        git clone https://github.com/bellard/quickjs.git "$QUICKJS_DIR"
        
        if [ $? -eq 0 ]; then
            print_status "✅ QuickJS 下载成功"
            
            # 检查关键文件
            if [ -f "$QUICKJS_DIR/quickjs.c" ]; then
                print_status "✅ QuickJS 源文件验证成功"
            else
                print_error "❌ QuickJS 源文件验证失败"
                return 1
            fi
        else
            print_error "❌ QuickJS 下载失败"
            return 1
        fi
    else
        print_error "❌ git 命令不可用，无法下载 QuickJS"
        print_status "请手动下载 QuickJS:"
        print_status "1. 访问: https://github.com/bellard/quickjs"
        print_status "2. 下载源码到: $QUICKJS_DIR"
        return 1
    fi
}

# 函数：下载 libcurl for Android
download_curl_android() {
    print_status "下载 libcurl for Android..."
    
    CURL_DIR="$CPP_DIR/curl"
    CURL_TEMP_DIR="/tmp/curl-android-ios"
    
    if [ -d "$CURL_DIR" ]; then
        print_warning "libcurl 目录已存在: $CURL_DIR"
        read -p "是否重新下载? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_status "跳过 libcurl 下载"
            return 0
        fi
        rm -rf "$CURL_DIR"
    fi
    
    if check_command git; then
        print_status "使用 git 克隆 libcurl for Android..."
        
        # 清理临时目录
        rm -rf "$CURL_TEMP_DIR"
        
        git clone https://github.com/gcesarmza/curl-android-ios.git "$CURL_TEMP_DIR"
        
        if [ $? -eq 0 ]; then
            print_status "✅ libcurl 下载成功"
            
            # 复制 Android 相关文件
            if [ -d "$CURL_TEMP_DIR/prebuilt-with-ssl/android" ]; then
                cp -r "$CURL_TEMP_DIR/prebuilt-with-ssl/android" "$CURL_DIR"
                print_status "✅ libcurl Android 库复制成功"
            else
                print_warning "⚠️ 未找到预编译的 Android 库"
            fi
            
            # 复制头文件
            if [ -d "$CURL_TEMP_DIR/prebuilt-with-ssl/include" ]; then
                mkdir -p "$CURL_DIR/include"
                cp -r "$CURL_TEMP_DIR/prebuilt-with-ssl/include/"* "$CURL_DIR/include/"
                print_status "✅ libcurl 头文件复制成功"
            else
                print_warning "⚠️ 未找到头文件"
            fi
            
            # 清理临时目录
            rm -rf "$CURL_TEMP_DIR"
            
        else
            print_error "❌ libcurl 下载失败"
            return 1
        fi
    else
        print_error "❌ git 命令不可用，无法下载 libcurl"
        print_status "请手动下载 libcurl:"
        print_status "1. 访问: https://github.com/gcesarmza/curl-android-ios"
        print_status "2. 下载预编译库到: $CURL_DIR"
        return 1
    fi
}

# 函数：安装系统 libcurl（Linux/macOS）
install_system_curl() {
    print_status "尝试安装系统 libcurl..."
    
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux
        if check_command apt-get; then
            print_status "使用 apt-get 安装 libcurl..."
            sudo apt-get update
            sudo apt-get install -y libcurl4-openssl-dev
        elif check_command yum; then
            print_status "使用 yum 安装 libcurl..."
            sudo yum install -y libcurl-devel
        elif check_command dnf; then
            print_status "使用 dnf 安装 libcurl..."
            sudo dnf install -y libcurl-devel
        elif check_command pacman; then
            print_status "使用 pacman 安装 libcurl..."
            sudo pacman -S curl
        else
            print_warning "⚠️ 未找到支持的包管理器"
            return 1
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        if check_command brew; then
            print_status "使用 Homebrew 安装 libcurl..."
            brew install curl
        elif check_command port; then
            print_status "使用 MacPorts 安装 libcurl..."
            sudo port install curl
        else
            print_warning "⚠️ 未找到 Homebrew 或 MacPorts"
            return 1
        fi
    else
        print_warning "⚠️ 不支持的操作系统: $OSTYPE"
        return 1
    fi
    
    print_status "✅ 系统 libcurl 安装完成"
}

# 函数：验证安装
verify_installation() {
    print_status "验证安装..."
    
    # 检查 QuickJS
    QUICKJS_DIR="$CPP_DIR/quickjs"
    if [ -f "$QUICKJS_DIR/quickjs.c" ]; then
        print_status "✅ QuickJS 验证成功"
    else
        print_warning "⚠️ QuickJS 验证失败"
    fi
    
    # 检查 libcurl
    CURL_DIR="$CPP_DIR/curl"
    if [ -d "$CURL_DIR" ] || check_command curl-config; then
        print_status "✅ libcurl 验证成功"
    else
        print_warning "⚠️ libcurl 验证失败"
    fi
    
    print_status "验证完成"
}

# 函数：显示帮助
show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -q, --quickjs-only    仅下载 QuickJS"
    echo "  -c, --curl-only       仅下载 libcurl"
    echo "  -s, --system-curl     安装系统 libcurl"
    echo "  -h, --help           显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0                    # 下载所有依赖"
    echo "  $0 --quickjs-only     # 仅下载 QuickJS"
    echo "  $0 --curl-only        # 仅下载 libcurl"
    echo "  $0 --system-curl      # 安装系统 libcurl"
}

# 主函数
main() {
    local quickjs_only=false
    local curl_only=false
    local system_curl=false
    
    # 解析命令行参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -q|--quickjs-only)
                quickjs_only=true
                shift
                ;;
            -c|--curl-only)
                curl_only=true
                shift
                ;;
            -s|--system-curl)
                system_curl=true
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                print_error "未知选项: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # 执行安装
    if [ "$quickjs_only" = true ]; then
        download_quickjs
    elif [ "$curl_only" = true ]; then
        download_curl_android
    elif [ "$system_curl" = true ]; then
        install_system_curl
    else
        # 默认：安装所有依赖
        download_quickjs
        
        print_status "选择 libcurl 安装方式:"
        echo "1) 下载 Android 预编译库（推荐用于 Android 开发）"
        echo "2) 安装系统 libcurl（推荐用于桌面开发）"
        echo "3) 跳过 libcurl 安装"
        read -p "请选择 (1-3): " -n 1 -r
        echo
        
        case $REPLY in
            1)
                download_curl_android
                ;;
            2)
                install_system_curl
                ;;
            3)
                print_status "跳过 libcurl 安装"
                ;;
            *)
                print_warning "无效选择，跳过 libcurl 安装"
                ;;
        esac
    fi
    
    verify_installation
    
    echo ""
    print_status "🎉 依赖安装完成！"
    print_status "现在可以编译 OneTV Film QuickJS 原生库了"
    echo ""
}

# 运行主函数
main "$@"
