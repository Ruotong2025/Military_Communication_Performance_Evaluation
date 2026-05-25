#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
查询 Chroma 存储统计信息
"""

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from chroma_vector_store import ChromaVectorStore


def main():
    # 获取项目根目录（向上两级）
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    
    persist_dir = os.environ.get('CHROMA_PERSIST_DIR', os.path.join(project_root, 'chroma_storage'))

    print(f"Chroma 存储目录: {persist_dir}")
    print("=" * 50)

    try:
        store = ChromaVectorStore(persist_dir)

        # 获取统计信息
        stats = store.get_stats()
        print(f"\n统计信息:")
        print(f"  指标集合 (indicators) 数量: {stats.get('indicator_count', 0)}")
        print(f"  数据源集合 (source_data) 数量: {stats.get('source_data_count', 0)}")
        print(f"  向量维度: {stats.get('dimension', 'N/A')}")

        # 获取所有指标
        indicators = store.get_all_indicators()
        print(f"\n指标列表 (共 {len(indicators)} 条):")
        if indicators:
            for ind in indicators[:20]:
                meta = ind.get('metadata', {})
                name = meta.get('name', 'N/A') if meta else 'N/A'
                print(f"  - ID: {ind['id']}, 名称: {name}")
            if len(indicators) > 20:
                print(f"  ... 还有 {len(indicators) - 20} 条")
        else:
            print("  (空)")

        # 获取所有数据源
        source_data = store.get_all_source_data()
        print(f"\n数据源列表 (共 {len(source_data)} 条):")
        if source_data:
            for sd in source_data[:20]:
                meta = sd.get('metadata', {})
                name = meta.get('name', 'N/A') if meta else 'N/A'
                print(f"  - ID: {sd['id']}, 名称: {name}")
            if len(source_data) > 20:
                print(f"  ... 还有 {len(source_data) - 20} 条")
        else:
            print("  (空)")

    except Exception as e:
        print(f"错误: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    main()
