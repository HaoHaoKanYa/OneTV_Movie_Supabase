#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
VOD模块资源测试脚本
验证资源前缀添加是否正确
"""

import os
import re
import subprocess
import sys
from pathlib import Path
from typing import List, Set, Tuple

class VodResourceTester:
    def __init__(self, vod_path: str = "vod"):
        self.vod_path = Path(vod_path)
        self.prefix = "vod_"
        self.errors = []
        self.warnings = []
        
    def log_error(self, message: str):
        """记录错误"""
        self.errors.append(message)
        print(f"✗ 错误: {message}")
    
    def log_warning(self, message: str):
        """记录警告"""
        self.warnings.append(message)
        print(f"⚠️  警告: {message}")
    
    def log_success(self, message: str):
        """记录成功"""
        print(f"✓ {message}")
    
    def test_compilation(self) -> bool:
        """测试编译是否成功"""
        print("\n=== 测试编译 ===")
        
        try:
            # 清理构建
            result = subprocess.run(
                ["./gradlew", "clean", "--quiet"],
                capture_output=True, text=True, cwd="."
            )
            
            if result.returncode != 0:
                self.log_warning(f"清理构建失败: {result.stderr}")
            
            # 编译VOD模块
            result = subprocess.run(
                ["./gradlew", ":vod:assembleDebug", "--quiet"],
                capture_output=True, text=True, cwd="."
            )
            
            if result.returncode == 0:
                self.log_success("VOD模块编译成功")
                return True
            else:
                self.log_error(f"VOD模块编译失败:\n{result.stderr}")
                return False
                
        except FileNotFoundError:
            self.log_error("找不到gradlew命令，请确保在项目根目录运行")
            return False
        except Exception as e:
            self.log_error(f"编译测试异常: {e}")
            return False
    
    def test_resource_prefix(self) -> bool:
        """测试资源文件前缀"""
        print("\n=== 测试资源前缀 ===")
        
        violations = []
        
        # 检查各个模块的资源
        modules = [
            self.vod_path / "src" / "main",
            self.vod_path / "src" / "leanback", 
            self.vod_path / "src" / "mobile",
            self.vod_path / "catvod" / "src" / "main"
        ]
        
        for module_path in modules:
            res_path = module_path / "res"
            if not res_path.exists():
                continue
                
            print(f"检查模块: {module_path.relative_to(self.vod_path)}")
            
            # 检查资源文件
            for res_file in res_path.rglob("*"):
                if res_file.is_file() and not res_file.name.startswith('.'):
                    # 跳过values目录和特殊文件
                    if "values" in str(res_file.parent):
                        continue
                    
                    skip_files = {
                        'AndroidManifest.xml',
                        'file_paths.xml',
                        'ic_launcher.xml',
                        'ic_launcher_round.xml'
                    }
                    
                    if res_file.name in skip_files:
                        continue
                    
                    # 检查前缀
                    if not res_file.name.startswith(self.prefix):
                        violations.append(str(res_file.relative_to(self.vod_path)))
        
        if violations:
            for violation in violations:
                self.log_error(f"资源文件缺少前缀: {violation}")
            return False
        else:
            self.log_success("所有资源文件都有正确的前缀")
            return True
    
    def test_resource_conflicts(self) -> bool:
        """检测与主应用的资源冲突"""
        print("\n=== 测试资源冲突 ===")
        
        # 获取主应用资源
        tv_resources = set()
        tv_res_path = Path("tv/src/main/res")
        if tv_res_path.exists():
            for res_file in tv_res_path.rglob("*"):
                if res_file.is_file():
                    tv_resources.add(res_file.name)
        else:
            self.log_warning("找不到TV模块资源目录")
        
        # 获取VOD资源
        vod_resources = set()
        vod_res_paths = [
            self.vod_path / "src" / "main" / "res",
            self.vod_path / "src" / "leanback" / "res",
            self.vod_path / "src" / "mobile" / "res"
        ]
        
        for vod_res_path in vod_res_paths:
            if vod_res_path.exists():
                for res_file in vod_res_path.rglob("*"):
                    if res_file.is_file():
                        vod_resources.add(res_file.name)
        
        # 检查冲突
        conflicts = tv_resources.intersection(vod_resources)
        if conflicts:
            for conflict in sorted(conflicts):
                self.log_error(f"资源冲突: {conflict}")
            return False
        else:
            self.log_success("无资源冲突")
            return True
    
    def test_reference_integrity(self) -> bool:
        """测试引用完整性"""
        print("\n=== 测试引用完整性 ===")
        
        # 收集所有资源文件
        available_resources = set()
        modules = [
            self.vod_path / "src" / "main",
            self.vod_path / "src" / "leanback",
            self.vod_path / "src" / "mobile",
            self.vod_path / "catvod" / "src" / "main"
        ]
        
        for module_path in modules:
            res_path = module_path / "res"
            if not res_path.exists():
                continue
                
            # 收集各类资源
            resource_types = ['drawable', 'layout', 'mipmap', 'anim', 'color', 'menu', 'xml']
            for res_type in resource_types:
                for res_dir in res_path.glob(f"{res_type}*"):
                    if res_dir.is_dir():
                        for res_file in res_dir.glob("*"):
                            if res_file.is_file():
                                available_resources.add(f"{res_type}/{res_file.stem}")
            
            # 收集values资源
            for values_dir in res_path.glob("values*"):
                if values_dir.is_dir():
                    for xml_file in values_dir.glob("*.xml"):
                        self._extract_values_resources(xml_file, available_resources)
        
        print(f"发现 {len(available_resources)} 个可用资源")
        
        # 检查引用
        broken_refs = []
        for module_path in modules:
            broken_refs.extend(self._check_module_references(module_path, available_resources))
        
        if broken_refs:
            for ref in broken_refs:
                self.log_error(f"无效引用: {ref}")
            return False
        else:
            self.log_success("所有资源引用都有效")
            return True
    
    def _extract_values_resources(self, xml_file: Path, resources: Set[str]):
        """从values文件中提取资源名称"""
        try:
            with open(xml_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 提取各种资源定义
            patterns = [
                (r'<string\s+name="([^"]+)"', 'string'),
                (r'<color\s+name="([^"]+)"', 'color'),
                (r'<style\s+name="([^"]+)"', 'style'),
                (r'<attr\s+name="([^"]+)"', 'attr'),
                (r'<dimen\s+name="([^"]+)"', 'dimen'),
                (r'<integer\s+name="([^"]+)"', 'integer'),
                (r'<bool\s+name="([^"]+)"', 'bool'),
            ]
            
            for pattern, res_type in patterns:
                matches = re.findall(pattern, content)
                for match in matches:
                    resources.add(f"{res_type}/{match}")
                    
        except Exception as e:
            self.log_warning(f"解析values文件失败 {xml_file}: {e}")
    
    def _check_module_references(self, module_path: Path, available_resources: Set[str]) -> List[str]:
        """检查模块中的资源引用"""
        broken_refs = []
        
        # 检查XML文件中的引用
        res_path = module_path / "res"
        if res_path.exists():
            for xml_file in res_path.rglob("*.xml"):
                broken_refs.extend(self._check_xml_references(xml_file, available_resources))
        
        # 检查代码文件中的引用
        java_path = module_path / "java"
        if java_path.exists():
            for code_file in java_path.rglob("*"):
                if code_file.suffix in ['.java', '.kt']:
                    broken_refs.extend(self._check_code_references(code_file, available_resources))
        
        return broken_refs
    
    def _check_xml_references(self, xml_file: Path, available_resources: Set[str]) -> List[str]:
        """检查XML文件中的资源引用"""
        broken_refs = []
        
        try:
            with open(xml_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 查找资源引用
            ref_pattern = r'@(drawable|layout|mipmap|anim|color|menu|xml|style|string|dimen|attr|integer|bool)/([^"\s>]+)'
            matches = re.findall(ref_pattern, content)
            
            for res_type, res_name in matches:
                resource_ref = f"{res_type}/{res_name}"
                if resource_ref not in available_resources:
                    broken_refs.append(f"{xml_file}: {resource_ref}")
                    
        except Exception as e:
            self.log_warning(f"检查XML引用失败 {xml_file}: {e}")
        
        return broken_refs
    
    def _check_code_references(self, code_file: Path, available_resources: Set[str]) -> List[str]:
        """检查代码文件中的资源引用"""
        broken_refs = []
        
        try:
            with open(code_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 查找R.资源引用
            ref_pattern = r'R\.(drawable|layout|mipmap|anim|color|menu|xml|style|string|dimen|attr|integer|bool)\.([a-zA-Z_][a-zA-Z0-9_]*)'
            matches = re.findall(ref_pattern, content)
            
            for res_type, res_name in matches:
                resource_ref = f"{res_type}/{res_name}"
                if resource_ref not in available_resources:
                    broken_refs.append(f"{code_file}: {resource_ref}")
                    
        except Exception as e:
            self.log_warning(f"检查代码引用失败 {code_file}: {e}")
        
        return broken_refs
    
    def test_gradle_config(self) -> bool:
        """测试Gradle配置"""
        print("\n=== 测试Gradle配置 ===")
        
        build_file = self.vod_path / "build.gradle.kts"
        if not build_file.exists():
            self.log_error("找不到VOD模块的build.gradle.kts文件")
            return False
        
        try:
            with open(build_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 检查resourcePrefix配置
            if 'resourcePrefix' in content and 'vod_' in content:
                self.log_success("Gradle resourcePrefix配置正确")
                return True
            else:
                self.log_warning("未找到resourcePrefix配置，建议添加")
                return True  # 不是致命错误
                
        except Exception as e:
            self.log_error(f"读取Gradle配置失败: {e}")
            return False
    
    def run_all_tests(self) -> bool:
        """运行所有测试"""
        print("VOD模块资源测试开始")
        print("=" * 50)
        
        tests = [
            ("Gradle配置", self.test_gradle_config),
            ("资源前缀", self.test_resource_prefix),
            ("资源冲突", self.test_resource_conflicts),
            ("引用完整性", self.test_reference_integrity),
            ("编译测试", self.test_compilation),
        ]
        
        passed = 0
        total = len(tests)
        
        for test_name, test_func in tests:
            print(f"\n{'='*20} {test_name} {'='*20}")
            try:
                if test_func():
                    passed += 1
                    print(f"✓ {test_name} 通过")
                else:
                    print(f"✗ {test_name} 失败")
            except Exception as e:
                print(f"✗ {test_name} 异常: {e}")
        
        # 输出总结
        print(f"\n{'='*50}")
        print(f"测试总结: {passed}/{total} 通过")
        
        if self.warnings:
            print(f"\n警告 ({len(self.warnings)}):")
            for warning in self.warnings:
                print(f"  ⚠️  {warning}")
        
        if self.errors:
            print(f"\n错误 ({len(self.errors)}):")
            for error in self.errors:
                print(f"  ✗ {error}")
        
        if passed == total and not self.errors:
            print("\n🎉 所有测试通过！VOD模块资源配置正确")
            return True
        else:
            print(f"\n❌ 测试失败，请修复上述问题")
            return False

def main():
    import argparse
    
    parser = argparse.ArgumentParser(description='VOD模块资源测试工具')
    parser.add_argument('--vod-path', 
                       default='vod',
                       help='VOD模块路径 (默认: vod)')
    parser.add_argument('--test',
                       choices=['gradle', 'prefix', 'conflicts', 'references', 'compile', 'all'],
                       default='all',
                       help='要运行的测试 (默认: all)')
    
    args = parser.parse_args()
    
    if not Path(args.vod_path).exists():
        print(f"✗ VOD模块路径不存在: {args.vod_path}")
        return False
    
    tester = VodResourceTester(args.vod_path)
    
    if args.test == 'all':
        success = tester.run_all_tests()
    else:
        test_map = {
            'gradle': tester.test_gradle_config,
            'prefix': tester.test_resource_prefix,
            'conflicts': tester.test_resource_conflicts,
            'references': tester.test_reference_integrity,
            'compile': tester.test_compilation
        }
        
        test_func = test_map[args.test]
        success = test_func()
    
    return success

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
