#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
VOD模块完整资源前缀处理脚本
处理遗漏的资源文件和目录
"""

import os
import re
import shutil
import datetime
from pathlib import Path
from typing import List, Dict, Set

class CompleteVodResourceProcessor:
    def __init__(self, vod_path: str = "vod"):
        self.vod_path = Path(vod_path)
        self.prefix = "vod_"
        self.processed_files = []
        self.updated_references = []
        self.errors = []
        
    def log_info(self, message: str):
        print(f"ℹ️  {message}")
    
    def log_success(self, message: str):
        print(f"✅ {message}")
    
    def log_error(self, message: str):
        print(f"❌ {message}")
        self.errors.append(message)
    
    def process_mipmap_anydpi_v26(self):
        """处理自适应图标"""
        print("\n=== 处理自适应图标 (mipmap-anydpi-v26) ===")
        
        anydpi_path = self.vod_path / "src" / "main" / "res" / "mipmap-anydpi-v26"
        if not anydpi_path.exists():
            self.log_info("mipmap-anydpi-v26目录不存在")
            return
        
        for xml_file in anydpi_path.glob("*.xml"):
            if not xml_file.name.startswith(self.prefix):
                new_name = self.prefix + xml_file.name
                new_path = xml_file.parent / new_name
                
                try:
                    shutil.move(str(xml_file), str(new_path))
                    self.processed_files.append(f"重命名: {xml_file.name} -> {new_name}")
                    self.log_success(f"重命名: {xml_file.name} -> {new_name}")
                except Exception as e:
                    self.log_error(f"重命名失败 {xml_file}: {e}")
    
    def process_xml_directory(self):
        """处理xml配置目录"""
        print("\n=== 处理XML配置目录 ===")
        
        xml_path = self.vod_path / "src" / "main" / "res" / "xml"
        if not xml_path.exists():
            self.log_info("xml目录不存在")
            return
        
        for xml_file in xml_path.glob("*.xml"):
            # file_paths.xml是特殊文件，不重命名
            if xml_file.name == "file_paths.xml":
                self.log_info(f"跳过特殊文件: {xml_file.name}")
                continue
                
            if not xml_file.name.startswith(self.prefix):
                new_name = self.prefix + xml_file.name
                new_path = xml_file.parent / new_name
                
                try:
                    shutil.move(str(xml_file), str(new_path))
                    self.processed_files.append(f"重命名: {xml_file.name} -> {new_name}")
                    self.log_success(f"重命名: {xml_file.name} -> {new_name}")
                except Exception as e:
                    self.log_error(f"重命名失败 {xml_file}: {e}")
    
    def process_assets_images(self):
        """处理assets/images目录"""
        print("\n=== 处理Assets图片目录 ===")
        
        images_path = self.vod_path / "src" / "main" / "assets" / "images"
        if not images_path.exists():
            self.log_info("assets/images目录不存在")
            return
        
        for image_file in images_path.glob("*"):
            if image_file.is_file() and not image_file.name.startswith(self.prefix):
                new_name = self.prefix + image_file.name
                new_path = image_file.parent / new_name
                
                try:
                    shutil.move(str(image_file), str(new_path))
                    self.processed_files.append(f"重命名: {image_file.name} -> {new_name}")
                    self.log_success(f"重命名: {image_file.name} -> {new_name}")
                    
                    # 更新HTML文件中的引用
                    self.update_html_image_references(image_file.name, new_name)
                    
                except Exception as e:
                    self.log_error(f"重命名失败 {image_file}: {e}")
    
    def process_assets_files(self):
        """处理assets根目录文件"""
        print("\n=== 处理Assets根目录文件 ===")
        
        assets_path = self.vod_path / "src" / "main" / "assets"
        if not assets_path.exists():
            self.log_info("assets目录不存在")
            return
        
        # 处理favicon.ico
        favicon_file = assets_path / "favicon.ico"
        if favicon_file.exists() and not favicon_file.name.startswith(self.prefix):
            new_name = self.prefix + favicon_file.name
            new_path = favicon_file.parent / new_name
            
            try:
                shutil.move(str(favicon_file), str(new_path))
                self.processed_files.append(f"重命名: {favicon_file.name} -> {new_name}")
                self.log_success(f"重命名: {favicon_file.name} -> {new_name}")
                
                # 更新HTML文件中的favicon引用
                self.update_html_favicon_reference(new_name)
                
            except Exception as e:
                self.log_error(f"重命名失败 {favicon_file}: {e}")
    
    def process_other_images(self):
        """处理other/image目录"""
        print("\n=== 处理Other图片目录 ===")
        
        other_images_path = self.vod_path / "other" / "image"
        if not other_images_path.exists():
            self.log_info("other/image目录不存在")
            return
        
        for image_file in other_images_path.glob("*"):
            if image_file.is_file() and not image_file.name.startswith(self.prefix):
                new_name = self.prefix + image_file.name
                new_path = image_file.parent / new_name
                
                try:
                    shutil.move(str(image_file), str(new_path))
                    self.processed_files.append(f"重命名: {image_file.name} -> {new_name}")
                    self.log_success(f"重命名: {image_file.name} -> {new_name}")
                except Exception as e:
                    self.log_error(f"重命名失败 {image_file}: {e}")
    
    def process_values_resources(self):
        """处理values目录中的资源定义"""
        print("\n=== 处理Values资源定义 ===")
        
        values_dirs = [
            self.vod_path / "src" / "main" / "res" / "values",
            self.vod_path / "src" / "main" / "res" / "values-zh-rCN",
            self.vod_path / "src" / "main" / "res" / "values-zh-rTW",
            self.vod_path / "src" / "mobile" / "res" / "values",
            self.vod_path / "src" / "mobile" / "res" / "values-night",
            self.vod_path / "src" / "mobile" / "res" / "values-v27",
            self.vod_path / "src" / "mobile" / "res" / "values-zh-rCN",
            self.vod_path / "src" / "mobile" / "res" / "values-zh-rTW",
            self.vod_path / "catvod" / "src" / "main" / "res" / "values",
            self.vod_path / "catvod" / "src" / "main" / "res" / "values-zh-rCN",
            self.vod_path / "catvod" / "src" / "main" / "res" / "values-zh-rTW",
        ]
        
        for values_dir in values_dirs:
            if values_dir.exists():
                self.log_info(f"处理: {values_dir.relative_to(self.vod_path)}")
                for xml_file in values_dir.glob("*.xml"):
                    self.update_values_file_content(xml_file)
    
    def update_values_file_content(self, xml_file: Path):
        """更新values文件中的资源定义"""
        try:
            with open(xml_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            original_content = content
            
            # 更新资源定义的name属性（只更新那些没有前缀的）
            patterns = [
                # string资源
                (r'<string\s+name="(?!vod_)([^"]+)"', r'<string name="vod_\1"'),
                # color资源
                (r'<color\s+name="(?!vod_)([^"]+)"', r'<color name="vod_\1"'),
                # style资源
                (r'<style\s+name="(?!vod_)([^"]+)"', r'<style name="vod_\1"'),
                # attr资源
                (r'<attr\s+name="(?!vod_)([^"]+)"', r'<attr name="vod_\1"'),
                # dimen资源
                (r'<dimen\s+name="(?!vod_)([^"]+)"', r'<dimen name="vod_\1"'),
                # integer资源
                (r'<integer\s+name="(?!vod_)([^"]+)"', r'<integer name="vod_\1"'),
                # bool资源
                (r'<bool\s+name="(?!vod_)([^"]+)"', r'<bool name="vod_\1"'),
                # array资源
                (r'<string-array\s+name="(?!vod_)([^"]+)"', r'<string-array name="vod_\1"'),
                (r'<integer-array\s+name="(?!vod_)([^"]+)"', r'<integer-array name="vod_\1"'),
                # declare-styleable
                (r'<declare-styleable\s+name="(?!vod_)([^"]+)"', r'<declare-styleable name="vod_\1"'),
            ]
            
            updated = False
            for pattern, replacement in patterns:
                new_content = re.sub(pattern, replacement, content)
                if new_content != content:
                    content = new_content
                    updated = True
            
            if updated:
                with open(xml_file, 'w', encoding='utf-8') as f:
                    f.write(content)
                self.updated_references.append(str(xml_file))
                self.log_success(f"更新values文件: {xml_file.name}")
            
        except Exception as e:
            self.log_error(f"更新values文件失败 {xml_file}: {e}")
    
    def update_html_image_references(self, old_name: str, new_name: str):
        """更新HTML文件中的图片引用"""
        html_files = [
            self.vod_path / "src" / "main" / "assets" / "index.html",
            self.vod_path / "src" / "main" / "assets" / "parse.html"
        ]
        
        for html_file in html_files:
            if html_file.exists():
                try:
                    with open(html_file, 'r', encoding='utf-8') as f:
                        content = f.read()
                    
                    # 更新图片路径引用
                    old_path = f"images/{old_name}"
                    new_path = f"images/{new_name}"
                    
                    if old_path in content:
                        content = content.replace(old_path, new_path)
                        
                        with open(html_file, 'w', encoding='utf-8') as f:
                            f.write(content)
                        
                        self.updated_references.append(str(html_file))
                        self.log_success(f"更新HTML引用: {html_file.name}")
                        
                except Exception as e:
                    self.log_error(f"更新HTML文件失败 {html_file}: {e}")
    
    def update_html_favicon_reference(self, new_favicon_name: str):
        """更新HTML文件中的favicon引用"""
        html_files = [
            self.vod_path / "src" / "main" / "assets" / "index.html",
            self.vod_path / "src" / "main" / "assets" / "parse.html"
        ]
        
        for html_file in html_files:
            if html_file.exists():
                try:
                    with open(html_file, 'r', encoding='utf-8') as f:
                        content = f.read()
                    
                    # 更新favicon引用
                    content = re.sub(r'href="favicon\.ico"', f'href="{new_favicon_name}"', content)
                    
                    with open(html_file, 'w', encoding='utf-8') as f:
                        f.write(content)
                    
                    self.updated_references.append(str(html_file))
                    self.log_success(f"更新HTML favicon引用: {html_file.name}")
                    
                except Exception as e:
                    self.log_error(f"更新HTML favicon失败 {html_file}: {e}")
    
    def run_complete_processing(self):
        """运行完整的资源处理"""
        print("VOD模块完整资源前缀处理")
        print("=" * 50)
        
        # 处理各种资源
        self.process_mipmap_anydpi_v26()
        self.process_xml_directory()
        self.process_assets_images()
        self.process_assets_files()
        self.process_other_images()
        self.process_values_resources()
        
        # 生成报告
        self.generate_report()
        
        return len(self.errors) == 0
    
    def generate_report(self):
        """生成处理报告"""
        print(f"\n{'='*50}")
        print("处理完成报告")
        print(f"{'='*50}")
        
        print(f"处理文件数: {len(self.processed_files)}")
        print(f"更新引用数: {len(self.updated_references)}")
        print(f"错误数量: {len(self.errors)}")
        
        if self.processed_files:
            print(f"\n处理的文件:")
            for file_info in self.processed_files:
                print(f"  {file_info}")
        
        if self.updated_references:
            print(f"\n更新的引用:")
            for ref_file in self.updated_references:
                print(f"  {ref_file}")
        
        if self.errors:
            print(f"\n错误信息:")
            for error in self.errors:
                print(f"  {error}")
        
        # 保存报告
        report_content = f"""
VOD模块完整资源前缀处理报告
============================

处理时间: {datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

统计信息:
- 处理文件数: {len(self.processed_files)}
- 更新引用数: {len(self.updated_references)}
- 错误数量: {len(self.errors)}

处理的文件:
{chr(10).join(f"  {file_info}" for file_info in self.processed_files)}

更新的引用:
{chr(10).join(f"  {ref_file}" for ref_file in self.updated_references)}

错误信息:
{chr(10).join(f"  {error}" for error in self.errors)}
"""
        
        report_file = f"vod_complete_resource_report_{datetime.datetime.now().strftime('%Y%m%d_%H%M%S')}.txt"
        with open(report_file, "w", encoding="utf-8") as f:
            f.write(report_content)
        
        print(f"\n报告已保存到: {report_file}")

def main():
    processor = CompleteVodResourceProcessor()
    success = processor.run_complete_processing()
    
    if success:
        print("\n🎉 所有资源处理完成！")
        print("\n建议下一步操作:")
        print("1. 检查Git差异: git diff")
        print("2. 运行编译测试: ./gradlew :vod:assembleDebug")
        print("3. 提交更改: git add . && git commit -m 'feat: 完成VOD模块遗漏资源的前缀处理'")
    else:
        print("\n❌ 处理过程中出现错误，请检查上述错误信息")
    
    return success

if __name__ == "__main__":
    import sys
    success = main()
    sys.exit(0 if success else 1)
