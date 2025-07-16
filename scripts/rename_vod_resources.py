#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
VOD模块资源重命名脚本
自动为所有资源文件添加vod_前缀并更新引用
"""

import os
import re
import shutil
import argparse
import datetime
from pathlib import Path
from typing import List, Dict, Tuple, Set

class VodResourceRenamer:
    def __init__(self, vod_path: str):
        self.vod_path = Path(vod_path)
        self.prefix = "vod_"
        self.renamed_files = {}  # old_name -> new_name
        self.updated_references = []
        self.skipped_files = []
        
    def scan_resources(self, module_path: Path) -> Dict[str, List[Path]]:
        """扫描模块中的所有资源文件"""
        resources = {
            'layout': [],
            'drawable': [],
            'mipmap': [],
            'anim': [],
            'color': [],
            'menu': [],
            'xml': [],
            'values': []
        }
        
        res_path = module_path / "res"
        if not res_path.exists():
            print(f"资源目录不存在: {res_path}")
            return resources
            
        print(f"扫描资源目录: {res_path}")
        
        for res_type in resources.keys():
            # 扫描各种密度的资源目录
            for res_dir in res_path.glob(f"{res_type}*"):
                if res_dir.is_dir():
                    print(f"  扫描 {res_dir.name}/")
                    for file_path in res_dir.glob("*"):
                        if file_path.is_file() and not file_path.name.startswith('.'):
                            resources[res_type].append(file_path)
                            print(f"    发现: {file_path.name}")
                            
        return resources
    
    def should_rename_file(self, file_path: Path) -> bool:
        """判断文件是否需要重命名"""
        # 已经有前缀的跳过
        if file_path.name.startswith(self.prefix):
            return False
            
        # values目录下的文件不重命名文件名，只更新内容
        if "values" in str(file_path.parent):
            return False
            
        # 特殊文件跳过
        skip_files = {
            'AndroidManifest.xml',
            'file_paths.xml',
            'ic_launcher.xml',
            'ic_launcher_round.xml'
        }
        
        if file_path.name in skip_files:
            self.skipped_files.append(str(file_path))
            return False
            
        return True
    
    def rename_resource_file(self, file_path: Path) -> Path:
        """重命名单个资源文件"""
        if not self.should_rename_file(file_path):
            return file_path
            
        new_name = self.prefix + file_path.name
        new_path = file_path.parent / new_name
        
        # 检查目标文件是否已存在
        if new_path.exists():
            print(f"警告: 目标文件已存在，跳过重命名: {new_path}")
            return file_path
        
        try:
            # 重命名文件
            shutil.move(str(file_path), str(new_path))
            
            # 记录重命名
            old_name = file_path.stem
            new_name_stem = new_path.stem
            self.renamed_files[old_name] = new_name_stem
            
            print(f"✓ 重命名: {file_path.name} -> {new_path.name}")
            return new_path
            
        except Exception as e:
            print(f"✗ 重命名失败 {file_path}: {e}")
            return file_path
    
    def update_xml_references(self, file_path: Path):
        """更新XML文件中的资源引用"""
        if not file_path.suffix == '.xml':
            return
            
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                
            original_content = content
            updated_count = 0
            
            # 更新各种资源引用
            for old_name, new_name in self.renamed_files.items():
                # 资源引用模式
                patterns = [
                    (rf'@drawable/{re.escape(old_name)}(?=\W|$)', f'@drawable/{new_name}'),
                    (rf'@layout/{re.escape(old_name)}(?=\W|$)', f'@layout/{new_name}'),
                    (rf'@mipmap/{re.escape(old_name)}(?=\W|$)', f'@mipmap/{new_name}'),
                    (rf'@anim/{re.escape(old_name)}(?=\W|$)', f'@anim/{new_name}'),
                    (rf'@color/{re.escape(old_name)}(?=\W|$)', f'@color/{new_name}'),
                    (rf'@menu/{re.escape(old_name)}(?=\W|$)', f'@menu/{new_name}'),
                    (rf'@xml/{re.escape(old_name)}(?=\W|$)', f'@xml/{new_name}'),
                    (rf'@style/{re.escape(old_name)}(?=\W|$)', f'@style/{new_name}'),
                    (rf'@string/{re.escape(old_name)}(?=\W|$)', f'@string/{new_name}'),
                ]
                
                for pattern, replacement in patterns:
                    new_content = re.sub(pattern, replacement, content)
                    if new_content != content:
                        updated_count += 1
                        content = new_content
            
            # 如果内容有变化，写回文件
            if content != original_content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(content)
                self.updated_references.append(str(file_path))
                print(f"✓ 更新XML引用: {file_path.name} ({updated_count} 处)")
                
        except Exception as e:
            print(f"✗ 更新XML引用失败 {file_path}: {e}")
    
    def update_code_references(self, module_path: Path):
        """更新Java/Kotlin代码中的资源引用"""
        java_path = module_path / "java"
        if not java_path.exists():
            print(f"代码目录不存在: {java_path}")
            return
            
        print(f"更新代码引用: {java_path}")
        
        # 处理Java文件
        for java_file in java_path.rglob("*.java"):
            self._update_code_file(java_file)
            
        # 处理Kotlin文件
        for kotlin_file in java_path.rglob("*.kt"):
            self._update_code_file(kotlin_file)
    
    def _update_code_file(self, file_path: Path):
        """更新单个代码文件中的资源引用"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                
            original_content = content
            updated_count = 0
            
            # 更新R.资源引用
            for old_name, new_name in self.renamed_files.items():
                patterns = [
                    (rf'R\.drawable\.{re.escape(old_name)}(?=\W|$)', f'R.drawable.{new_name}'),
                    (rf'R\.layout\.{re.escape(old_name)}(?=\W|$)', f'R.layout.{new_name}'),
                    (rf'R\.mipmap\.{re.escape(old_name)}(?=\W|$)', f'R.mipmap.{new_name}'),
                    (rf'R\.anim\.{re.escape(old_name)}(?=\W|$)', f'R.anim.{new_name}'),
                    (rf'R\.color\.{re.escape(old_name)}(?=\W|$)', f'R.color.{new_name}'),
                    (rf'R\.menu\.{re.escape(old_name)}(?=\W|$)', f'R.menu.{new_name}'),
                    (rf'R\.xml\.{re.escape(old_name)}(?=\W|$)', f'R.xml.{new_name}'),
                    (rf'R\.style\.{re.escape(old_name)}(?=\W|$)', f'R.style.{new_name}'),
                    (rf'R\.string\.{re.escape(old_name)}(?=\W|$)', f'R.string.{new_name}'),
                ]
                
                for pattern, replacement in patterns:
                    new_content = re.sub(pattern, replacement, content)
                    if new_content != content:
                        updated_count += 1
                        content = new_content
            
            # 如果内容有变化，写回文件
            if content != original_content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(content)
                self.updated_references.append(str(file_path))
                print(f"✓ 更新代码引用: {file_path.name} ({updated_count} 处)")
                
        except Exception as e:
            print(f"✗ 更新代码引用失败 {file_path}: {e}")
    
    def update_values_resources(self, module_path: Path):
        """更新values资源文件中的内容引用"""
        res_path = module_path / "res"
        if not res_path.exists():
            return
            
        # 处理所有values目录
        for values_dir in res_path.glob("values*"):
            if values_dir.is_dir():
                for xml_file in values_dir.glob("*.xml"):
                    self._update_values_file(xml_file)
    
    def _update_values_file(self, file_path: Path):
        """更新values文件中的资源定义"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                
            original_content = content
            
            # 更新style、string、color等资源的name属性
            for old_name, new_name in self.renamed_files.items():
                # 更新name属性
                patterns = [
                    (rf'name="{re.escape(old_name)}"', f'name="{new_name}"'),
                    (rf"name='{re.escape(old_name)}'", f"name='{new_name}'"),
                ]
                
                for pattern, replacement in patterns:
                    content = re.sub(pattern, replacement, content)
            
            # 如果内容有变化，写回文件
            if content != original_content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(content)
                self.updated_references.append(str(file_path))
                print(f"✓ 更新values文件: {file_path.name}")
                
        except Exception as e:
            print(f"✗ 更新values文件失败 {file_path}: {e}")
    
    def process_module(self, module_name: str) -> bool:
        """处理单个模块"""
        print(f"\n{'='*50}")
        print(f"处理模块: {module_name}")
        print(f"{'='*50}")
        
        # 确定模块路径
        if module_name == "main":
            module_path = self.vod_path / "src" / "main"
        elif module_name in ["leanback", "mobile"]:
            module_path = self.vod_path / "src" / module_name
        else:
            module_path = self.vod_path / module_name / "src" / "main"
        
        if not module_path.exists():
            print(f"✗ 模块路径不存在: {module_path}")
            return False
        
        print(f"模块路径: {module_path}")
        
        # 重置计数器
        initial_renamed_count = len(self.renamed_files)
        initial_updated_count = len(self.updated_references)
        
        # 扫描资源
        resources = self.scan_resources(module_path)
        total_files = sum(len(files) for files in resources.values())
        print(f"\n发现 {total_files} 个资源文件")
        
        if total_files == 0:
            print("无资源文件需要处理")
            return True
        
        # 重命名资源文件
        print(f"\n--- 重命名资源文件 ---")
        for res_type, files in resources.items():
            if files:
                print(f"\n处理 {res_type} 资源:")
                for file_path in files:
                    self.rename_resource_file(file_path)
        
        # 更新XML引用
        print(f"\n--- 更新XML引用 ---")
        res_path = module_path / "res"
        if res_path.exists():
            for xml_file in res_path.rglob("*.xml"):
                self.update_xml_references(xml_file)
        
        # 更新values资源
        print(f"\n--- 更新values资源 ---")
        self.update_values_resources(module_path)
        
        # 更新代码引用
        print(f"\n--- 更新代码引用 ---")
        self.update_code_references(module_path)
        
        # 统计本模块处理结果
        renamed_count = len(self.renamed_files) - initial_renamed_count
        updated_count = len(self.updated_references) - initial_updated_count
        
        print(f"\n--- 模块 {module_name} 处理完成 ---")
        print(f"重命名文件: {renamed_count}")
        print(f"更新引用: {updated_count}")
        print(f"跳过文件: {len(self.skipped_files)}")
        
        return True
    
    def generate_report(self) -> str:
        """生成处理报告"""
        report = f"""
VOD模块资源前缀处理报告
========================

处理时间: {datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
前缀: {self.prefix}

处理统计:
- 重命名文件: {len(self.renamed_files)} 个
- 更新引用: {len(self.updated_references)} 个文件
- 跳过文件: {len(self.skipped_files)} 个

重命名详情:
"""
        for old_name, new_name in sorted(self.renamed_files.items()):
            report += f"  {old_name} -> {new_name}\n"
        
        if self.skipped_files:
            report += "\n跳过文件:\n"
            for skipped_file in sorted(self.skipped_files):
                report += f"  {skipped_file}\n"
        
        report += "\n更新引用文件:\n"
        for ref_file in sorted(self.updated_references):
            report += f"  {ref_file}\n"
        
        return report

def main():
    parser = argparse.ArgumentParser(description='VOD模块资源重命名工具')
    parser.add_argument('--module', 
                       choices=['main', 'leanback', 'mobile', 'catvod', 'all'],
                       default='all',
                       help='要处理的模块 (默认: all)')
    parser.add_argument('--vod-path', 
                       default='vod',
                       help='VOD模块路径 (默认: vod)')
    parser.add_argument('--dry-run', 
                       action='store_true',
                       help='仅预览，不实际修改文件')
    
    args = parser.parse_args()
    
    if args.dry_run:
        print("⚠️  DRY RUN 模式 - 不会实际修改文件")
    
    vod_path = args.vod_path
    if not Path(vod_path).exists():
        print(f"✗ VOD模块路径不存在: {vod_path}")
        return False
    
    renamer = VodResourceRenamer(vod_path)
    
    # 确定要处理的模块
    if args.module == 'all':
        modules = ["main", "leanback", "mobile", "catvod"]
    else:
        modules = [args.module]
    
    print(f"开始处理VOD模块资源前缀")
    print(f"目标路径: {Path(vod_path).absolute()}")
    print(f"处理模块: {', '.join(modules)}")
    print(f"前缀: {renamer.prefix}")
    
    # 处理各个模块
    success_count = 0
    for module in modules:
        if not args.dry_run:
            # 非dry-run模式，每个模块处理完后暂停确认
            input(f"\n按Enter开始处理模块 {module}...")
        
        success = renamer.process_module(module)
        if success:
            success_count += 1
            if not args.dry_run:
                input(f"模块 {module} 处理完成，按Enter继续...")
        else:
            print(f"✗ 模块 {module} 处理失败")
            if not args.dry_run:
                choice = input("继续处理下一个模块? (y/N): ")
                if choice.lower() != 'y':
                    break
    
    # 生成报告
    report = renamer.generate_report()
    report_file = f"vod_resource_rename_report_{datetime.datetime.now().strftime('%Y%m%d_%H%M%S')}.txt"
    
    if not args.dry_run:
        with open(report_file, "w", encoding="utf-8") as f:
            f.write(report)
        print(f"\n报告已保存到: {report_file}")
    else:
        print("\n=== 预览报告 ===")
        print(report)
    
    print(f"\n处理完成！成功处理 {success_count}/{len(modules)} 个模块")
    
    if success_count == len(modules):
        print("🎉 所有模块处理成功！")
        if not args.dry_run:
            print("\n建议下一步操作:")
            print("1. 运行编译测试: ./gradlew :vod:assembleDebug")
            print("2. 检查Git差异: git diff")
            print("3. 提交变更: git add . && git commit -m 'feat: 为VOD模块资源添加vod_前缀'")
        return True
    else:
        print("❌ 部分模块处理失败")
        return False

if __name__ == "__main__":
    success = main()
    exit(0 if success else 1)
