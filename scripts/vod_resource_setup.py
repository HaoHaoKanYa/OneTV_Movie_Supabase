#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
VOD模块资源前缀设置脚本
简化版本，用于实际执行
"""

import os
import sys
import subprocess
from pathlib import Path

def check_environment():
    """检查环境"""
    print("=== 环境检查 ===")
    
    # 检查是否在项目根目录
    if not Path("settings.gradle.kts").exists():
        print("✗ 请在项目根目录运行此脚本")
        return False
    
    # 检查VOD模块
    if not Path("vod").exists():
        print("✗ 找不到VOD模块")
        return False
    
    # 检查Python版本
    if sys.version_info < (3, 6):
        print("✗ 需要Python 3.6或更高版本")
        return False
    
    # 检查Git状态
    try:
        result = subprocess.run(["git", "status", "--porcelain"], 
                              capture_output=True, text=True)
        if result.stdout.strip():
            print("⚠️  工作目录有未提交的更改")
            choice = input("继续? (y/N): ")
            if choice.lower() != 'y':
                return False
    except:
        print("⚠️  无法检查Git状态")
    
    print("✓ 环境检查通过")
    return True

def backup_vod_module():
    """备份VOD模块"""
    print("\n=== 创建备份 ===")
    
    import datetime
    backup_name = f"vod_backup_{datetime.datetime.now().strftime('%Y%m%d_%H%M%S')}"
    
    try:
        import shutil
        shutil.copytree("vod", backup_name)
        print(f"✓ 备份创建成功: {backup_name}")
        return backup_name
    except Exception as e:
        print(f"✗ 备份创建失败: {e}")
        return None

def update_gradle_config():
    """更新Gradle配置"""
    print("\n=== 更新Gradle配置 ===")
    
    build_file = Path("vod/build.gradle.kts")
    
    try:
        with open(build_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 检查是否已有resourcePrefix配置
        if 'resourcePrefix' in content:
            print("✓ resourcePrefix配置已存在")
            return True
        
        # 在android块中添加resourcePrefix
        android_pattern = r'(android\s*\{)'
        replacement = r'\1\n    resourcePrefix = "vod_"'
        
        import re
        new_content = re.sub(android_pattern, replacement, content)
        
        if new_content != content:
            with open(build_file, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print("✓ 已添加resourcePrefix配置")
            return True
        else:
            print("⚠️  无法自动添加resourcePrefix配置，请手动添加")
            return False
            
    except Exception as e:
        print(f"✗ 更新Gradle配置失败: {e}")
        return False

def run_resource_rename():
    """运行资源重命名"""
    print("\n=== 运行资源重命名 ===")
    
    script_path = Path("scripts/rename_vod_resources.py")
    if not script_path.exists():
        print("✗ 找不到重命名脚本")
        return False
    
    try:
        # 先运行dry-run预览
        print("预览模式运行...")
        result = subprocess.run([
            sys.executable, str(script_path), 
            "--module", "all", 
            "--dry-run"
        ], capture_output=True, text=True)
        
        if result.returncode != 0:
            print(f"✗ 预览失败: {result.stderr}")
            return False
        
        print("预览结果:")
        print(result.stdout)
        
        # 确认执行
        choice = input("\n确认执行资源重命名? (y/N): ")
        if choice.lower() != 'y':
            print("取消执行")
            return False
        
        # 实际执行
        print("\n开始执行资源重命名...")
        result = subprocess.run([
            sys.executable, str(script_path), 
            "--module", "all"
        ])
        
        return result.returncode == 0
        
    except Exception as e:
        print(f"✗ 执行重命名失败: {e}")
        return False

def test_compilation():
    """测试编译"""
    print("\n=== 测试编译 ===")
    
    try:
        # 清理构建
        print("清理构建...")
        subprocess.run(["./gradlew", "clean"], check=True, capture_output=True)
        
        # 编译VOD模块
        print("编译VOD模块...")
        result = subprocess.run(
            ["./gradlew", ":vod:assembleDebug"], 
            capture_output=True, text=True
        )
        
        if result.returncode == 0:
            print("✓ 编译成功")
            return True
        else:
            print(f"✗ 编译失败:")
            print(result.stderr)
            return False
            
    except Exception as e:
        print(f"✗ 编译测试失败: {e}")
        return False

def run_tests():
    """运行测试"""
    print("\n=== 运行测试 ===")
    
    test_script = Path("scripts/test_vod_resources.py")
    if not test_script.exists():
        print("⚠️  找不到测试脚本，跳过测试")
        return True
    
    try:
        result = subprocess.run([
            sys.executable, str(test_script)
        ])
        
        return result.returncode == 0
        
    except Exception as e:
        print(f"✗ 运行测试失败: {e}")
        return False

def commit_changes():
    """提交更改"""
    print("\n=== 提交更改 ===")
    
    try:
        # 检查更改
        result = subprocess.run(["git", "status", "--porcelain"], 
                              capture_output=True, text=True)
        
        if not result.stdout.strip():
            print("✓ 无更改需要提交")
            return True
        
        print("发现以下更改:")
        print(result.stdout)
        
        choice = input("提交这些更改? (y/N): ")
        if choice.lower() != 'y':
            print("跳过提交")
            return True
        
        # 添加更改
        subprocess.run(["git", "add", "vod/"], check=True)
        
        # 提交
        commit_msg = "feat: 为VOD模块资源添加vod_前缀，实现资源隔离"
        subprocess.run(["git", "commit", "-m", commit_msg], check=True)
        
        print("✓ 更改已提交")
        return True
        
    except Exception as e:
        print(f"✗ 提交失败: {e}")
        return False

def main():
    """主函数"""
    print("VOD模块资源前缀自动化设置")
    print("=" * 50)
    
    # 步骤列表
    steps = [
        ("环境检查", check_environment),
        ("创建备份", backup_vod_module),
        ("更新Gradle配置", update_gradle_config),
        ("资源重命名", run_resource_rename),
        ("测试编译", test_compilation),
        ("运行测试", run_tests),
        ("提交更改", commit_changes),
    ]
    
    backup_name = None
    
    for step_name, step_func in steps:
        print(f"\n{'='*20} {step_name} {'='*20}")
        
        try:
            if step_name == "创建备份":
                backup_name = step_func()
                success = backup_name is not None
            else:
                success = step_func()
            
            if not success:
                print(f"✗ {step_name} 失败")
                
                if backup_name:
                    choice = input(f"恢复备份 {backup_name}? (y/N): ")
                    if choice.lower() == 'y':
                        import shutil
                        shutil.rmtree("vod")
                        shutil.move(backup_name, "vod")
                        print("✓ 已恢复备份")
                
                return False
            
            print(f"✓ {step_name} 完成")
            
        except KeyboardInterrupt:
            print(f"\n用户中断，{step_name} 未完成")
            return False
        except Exception as e:
            print(f"✗ {step_name} 异常: {e}")
            return False
    
    print("\n" + "=" * 50)
    print("🎉 VOD模块资源前缀设置完成！")
    print("\n建议下一步操作:")
    print("1. 运行完整测试: python scripts/test_vod_resources.py")
    print("2. 测试TV应用: ./gradlew :tv:assembleDebug")
    print("3. 功能测试: 启动应用并测试影视点播功能")
    
    if backup_name:
        print(f"\n备份位置: {backup_name}")
        print("确认一切正常后可删除备份")
    
    return True

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
