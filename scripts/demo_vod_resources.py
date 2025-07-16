#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
VOD模块资源前缀演示脚本
展示当前VOD模块的资源状况和需要处理的内容
"""

import os
import sys
from pathlib import Path
from collections import defaultdict

def analyze_vod_resources():
    """分析VOD模块资源"""
    print("VOD模块资源分析")
    print("=" * 50)
    
    vod_path = Path("vod")
    if not vod_path.exists():
        print("✗ 找不到VOD模块")
        return False
    
    # 分析各个模块
    modules = {
        "主模块": vod_path / "src" / "main",
        "Leanback模块": vod_path / "src" / "leanback", 
        "Mobile模块": vod_path / "src" / "mobile",
        "CatVOD模块": vod_path / "catvod" / "src" / "main"
    }
    
    total_stats = defaultdict(int)
    prefix_stats = defaultdict(int)
    
    for module_name, module_path in modules.items():
        print(f"\n--- {module_name} ---")
        
        res_path = module_path / "res"
        if not res_path.exists():
            print("  无资源目录")
            continue
        
        # 统计资源文件
        stats = analyze_module_resources(res_path)
        
        print(f"  资源统计:")
        for res_type, count in stats.items():
            if count > 0:
                print(f"    {res_type}: {count} 个文件")
                total_stats[res_type] += count
        
        # 检查前缀情况
        prefix_info = check_prefix_status(res_path)
        print(f"  前缀状况:")
        print(f"    有前缀: {prefix_info['with_prefix']} 个")
        print(f"    无前缀: {prefix_info['without_prefix']} 个")
        print(f"    跳过文件: {prefix_info['skipped']} 个")
        
        prefix_stats['with_prefix'] += prefix_info['with_prefix']
        prefix_stats['without_prefix'] += prefix_info['without_prefix']
        prefix_stats['skipped'] += prefix_info['skipped']
    
    # 总体统计
    print(f"\n{'='*50}")
    print("总体统计:")
    print(f"{'='*50}")
    
    total_files = sum(total_stats.values())
    print(f"资源文件总数: {total_files}")
    
    print(f"\n按类型统计:")
    for res_type, count in sorted(total_stats.items()):
        print(f"  {res_type}: {count} 个")
    
    print(f"\n前缀状况:")
    print(f"  有vod_前缀: {prefix_stats['with_prefix']} 个")
    print(f"  无vod_前缀: {prefix_stats['without_prefix']} 个")
    print(f"  跳过处理: {prefix_stats['skipped']} 个")
    
    # 处理建议
    print(f"\n{'='*50}")
    print("处理建议:")
    print(f"{'='*50}")
    
    if prefix_stats['without_prefix'] > 0:
        print(f"🔧 需要添加前缀的文件: {prefix_stats['without_prefix']} 个")
        print("   建议执行: python scripts/vod_resource_setup.py")
    else:
        print("✅ 所有资源文件都已有正确前缀")
    
    return True

def analyze_module_resources(res_path: Path):
    """分析模块资源"""
    stats = defaultdict(int)
    
    resource_types = ['layout', 'drawable', 'mipmap', 'anim', 'color', 'menu', 'xml', 'values']
    
    for res_type in resource_types:
        for res_dir in res_path.glob(f"{res_type}*"):
            if res_dir.is_dir():
                for res_file in res_dir.glob("*"):
                    if res_file.is_file() and not res_file.name.startswith('.'):
                        stats[res_type] += 1
    
    return stats

def check_prefix_status(res_path: Path):
    """检查前缀状况"""
    with_prefix = 0
    without_prefix = 0
    skipped = 0
    
    skip_files = {
        'AndroidManifest.xml',
        'file_paths.xml', 
        'ic_launcher.xml',
        'ic_launcher_round.xml'
    }
    
    for res_file in res_path.rglob("*"):
        if res_file.is_file() and not res_file.name.startswith('.'):
            # 跳过values目录
            if "values" in str(res_file.parent):
                skipped += 1
                continue
            
            # 跳过特殊文件
            if res_file.name in skip_files:
                skipped += 1
                continue
            
            # 检查前缀
            if res_file.name.startswith("vod_"):
                with_prefix += 1
            else:
                without_prefix += 1
    
    return {
        'with_prefix': with_prefix,
        'without_prefix': without_prefix,
        'skipped': skipped
    }

def show_sample_files():
    """显示示例文件"""
    print(f"\n{'='*50}")
    print("示例文件预览:")
    print(f"{'='*50}")
    
    vod_path = Path("vod")
    
    # 显示一些示例文件
    sample_paths = [
        vod_path / "src" / "main" / "res" / "drawable",
        vod_path / "src" / "leanback" / "res" / "layout",
        vod_path / "src" / "mobile" / "res" / "drawable"
    ]
    
    for sample_path in sample_paths:
        if sample_path.exists():
            print(f"\n{sample_path.relative_to(vod_path)}:")
            files = list(sample_path.glob("*"))[:5]  # 只显示前5个文件
            for file in files:
                if file.is_file():
                    prefix_status = "✅" if file.name.startswith("vod_") else "❌"
                    print(f"  {prefix_status} {file.name}")
            
            if len(list(sample_path.glob("*"))) > 5:
                print(f"  ... 还有 {len(list(sample_path.glob('*'))) - 5} 个文件")

def check_gradle_config():
    """检查Gradle配置"""
    print(f"\n{'='*50}")
    print("Gradle配置检查:")
    print(f"{'='*50}")
    
    build_file = Path("vod/build.gradle.kts")
    if not build_file.exists():
        print("✗ 找不到build.gradle.kts文件")
        return False
    
    try:
        with open(build_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 检查resourcePrefix
        if 'resourcePrefix = "vod_"' in content:
            print("✅ resourcePrefix配置已存在")
        else:
            print("❌ 缺少resourcePrefix配置")
            print("   建议添加: resourcePrefix = \"vod_\"")
        
        # 检查验证任务
        if 'validateResourcePrefix' in content:
            print("✅ 资源验证任务已配置")
        else:
            print("❌ 缺少资源验证任务")
            print("   建议添加验证任务")
        
        return True
        
    except Exception as e:
        print(f"✗ 读取配置文件失败: {e}")
        return False

def show_next_steps():
    """显示下一步操作"""
    print(f"\n{'='*50}")
    print("下一步操作:")
    print(f"{'='*50}")
    
    print("1. 🔍 预览模式 (推荐先执行)")
    print("   python scripts/rename_vod_resources.py --dry-run")
    
    print("\n2. 🚀 一键执行 (自动化处理)")
    print("   python scripts/vod_resource_setup.py")
    
    print("\n3. 🔧 分步执行 (手动控制)")
    print("   python scripts/rename_vod_resources.py --module main")
    print("   python scripts/rename_vod_resources.py --module leanback")
    print("   python scripts/rename_vod_resources.py --module mobile")
    print("   python scripts/rename_vod_resources.py --module catvod")
    
    print("\n4. ✅ 验证测试")
    print("   python scripts/test_vod_resources.py")
    
    print("\n5. 🏗️ 编译测试")
    print("   ./gradlew :vod:assembleDebug")

def main():
    """主函数"""
    print("OneTV VOD模块资源前缀演示")
    print("当前时间:", __import__('datetime').datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
    
    # 检查环境
    if not Path("settings.gradle.kts").exists():
        print("✗ 请在OneTV项目根目录运行此脚本")
        return False
    
    # 分析资源
    if not analyze_vod_resources():
        return False
    
    # 显示示例文件
    show_sample_files()
    
    # 检查Gradle配置
    check_gradle_config()
    
    # 显示下一步操作
    show_next_steps()
    
    print(f"\n{'='*50}")
    print("演示完成！")
    print("详细文档请查看: vodMD/23_VOD资源前缀执行指南_20250116.md")
    
    return True

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
