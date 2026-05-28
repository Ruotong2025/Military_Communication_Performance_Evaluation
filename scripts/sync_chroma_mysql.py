#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Chroma 与 MySQL 数据同步校验脚本

功能：
    1. 对比 MySQL 和 Chroma 中的数据
    2. 找出差异（Chroma 多余或缺失的数据）
    3. 以 MySQL 为主进行同步

用法：
    # 仅检查差异（不修改数据）
    python sync_chroma_mysql.py --check-only

    # 执行同步
    python sync_chroma_mysql.py --sync

    # 指定配置文件
    python sync_chroma_mysql.py --config path/to/application.yml
"""

import sys
import os
import json
import argparse
import logging
import subprocess
from typing import List, Dict, Set, Tuple, Optional

# 添加脚本目录到路径
script_dir = os.path.dirname(os.path.abspath(__file__))
if script_dir not in sys.path:
    sys.path.insert(0, script_dir)

from chroma_vector_store import ChromaVectorStore

# 禁用 INFO 日志
logging.basicConfig(level=logging.WARNING)
logger = logging.getLogger(__name__)

# 项目根目录
PROJECT_ROOT = os.path.dirname(script_dir)
DEFAULT_CHROMA_PATH = os.path.join(PROJECT_ROOT, "chroma_storage")
DEFAULT_CONFIG_PATH = os.path.join(PROJECT_ROOT, "src/main/resources/application.yml")


def parse_mysql_config(config_path: str) -> Dict[str, str]:
    """解析 application.yml 获取 MySQL 配置"""
    import re

    config = {
        'host': 'localhost',
        'port': 3306,
        'database': 'military_operational_effectiveness_evaluation',
        'user': 'root',
        'password': 'root'
    }

    if not os.path.exists(config_path):
        logger.warning(f"配置文件不存在: {config_path}，使用默认配置")
        return config

    try:
        with open(config_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # 解析 datasource.url
        url_match = re.search(r'url:\s*jdbc:mysql://([^:]+):(\d+)/([^?]+)', content)
        if url_match:
            config['host'] = url_match.group(1)
            config['port'] = int(url_match.group(2))
            config['database'] = url_match.group(3)

        # 解析用户名
        user_match = re.search(r'username:\s*(\S+)', content)
        if user_match:
            config['user'] = user_match.group(1)

        # 解析密码
        password_match = re.search(r'password:\s*(\S+)', content)
        if password_match:
            config['password'] = password_match.group(1)

        logger.info(f"已读取 MySQL 配置: {config['host']}:{config['port']}/{config['database']}")

    except Exception as e:
        logger.warning(f"解析配置文件失败: {e}，使用默认配置")

    return config


def connect_mysql(config: Dict[str, str]):
    """连接 MySQL 数据库"""
    try:
        import pymysql
        conn = pymysql.connect(
            host=config['host'],
            port=config['port'],
            user=config['user'],
            password=config['password'],
            database=config['database'],
            charset='utf8mb4',
            cursorclass=pymysql.cursors.DictCursor
        )
        return conn
    except ImportError:
        logger.error("请安装 pymysql: pip install pymysql")
        sys.exit(1)
    except Exception as e:
        logger.error(f"MySQL 连接失败: {e}")
        sys.exit(1)


def get_indicators_from_mysql(conn) -> List[Dict]:
    """从 MySQL 读取所有指标定义"""
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT id, indicator_name FROM indicator_definition")
            results = cursor.fetchall()
            logger.info(f"从 MySQL 读取到 {len(results)} 条指标定义")
            return results
    except Exception as e:
        logger.error(f"查询 indicator_definition 表失败: {e}")
        return []


def get_source_data_from_mysql(conn) -> List[Dict]:
    """从 MySQL 读取所有数据源定义"""
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT id, source_data_name FROM indicator_source_data")
            results = cursor.fetchall()
            logger.info(f"从 MySQL 读取到 {len(results)} 条数据源定义")
            return results
    except Exception as e:
        logger.error(f"查询 indicator_source_data 表失败: {e}")
        return []


def get_indicators_from_chroma(store: ChromaVectorStore) -> Set[int]:
    """从 Chroma 读取所有指标 ID"""
    try:
        all_indicators = store.get_all_indicators()
        ids = {item['id'] for item in all_indicators}
        logger.info(f"从 Chroma 读取到 {len(ids)} 个指标")
        return ids
    except Exception as e:
        logger.error(f"读取 Chroma 指标失败: {e}")
        return set()


def get_source_data_from_chroma(store: ChromaVectorStore) -> Set[int]:
    """从 Chroma 读取所有数据源 ID"""
    try:
        all_source_data = store.get_all_source_data()
        ids = {item['id'] for item in all_source_data}
        logger.info(f"从 Chroma 读取到 {len(ids)} 个数据源")
        return ids
    except Exception as e:
        logger.error(f"读取 Chroma 数据源失败: {e}")
        return set()


def encode_texts(texts: List[str]) -> List[List[float]]:
    """直接调用 similarity_calculator 模块生成向量"""
    if not texts:
        return []

    try:
        # 动态导入 similarity_calculator 模块
        import importlib.util
        spec = importlib.util.spec_from_file_location(
            "similarity_calculator",
            os.path.join(script_dir, 'similarity_calculator.py')
        )
        calculator = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(calculator)

        # 使用模块的 encode_texts 函数
        result = calculator.encode_texts(texts)
        vectors = result.get('vectors', [])
        logger.info(f"成功生成 {len(vectors)} 个向量")
        return vectors

    except Exception as e:
        logger.error(f"向量生成失败: {e}")
        return []


def compare_data(
    mysql_indicators: List[Dict],
    chroma_indicator_ids: Set[int],
    mysql_source_data: List[Dict],
    chroma_source_data_ids: Set[int]
) -> Dict:
    """比对 MySQL 和 Chroma 的数据差异"""

    # 转换为集合
    mysql_indicator_ids = {item['id'] for item in mysql_indicators}
    mysql_source_data_ids = {item['id'] for item in mysql_source_data}

    # 指标差异
    indicators_to_delete = chroma_indicator_ids - mysql_indicator_ids  # Chroma 有, MySQL 没有
    indicators_to_add = mysql_indicator_ids - chroma_indicator_ids      # MySQL 有, Chroma 没有

    # 数据源差异
    source_data_to_delete = chroma_source_data_ids - mysql_source_data_ids
    source_data_to_add = mysql_source_data_ids - chroma_source_data_ids

    # 需要添加的完整数据
    indicators_to_add_data = [
        {'id': item['id'], 'name': item['indicator_name']}
        for item in mysql_indicators if item['id'] in indicators_to_add
    ]
    source_data_to_add_data = [
        {'id': item['id'], 'name': item['source_data_name']}
        for item in mysql_source_data if item['id'] in source_data_to_add
    ]

    return {
        'indicators': {
            'mysql_count': len(mysql_indicator_ids),
            'chroma_count': len(chroma_indicator_ids),
            'to_delete': sorted(indicators_to_delete),
            'to_add': indicators_to_add_data,
            'to_delete_count': len(indicators_to_delete),
            'to_add_count': len(indicators_to_add)
        },
        'source_data': {
            'mysql_count': len(mysql_source_data_ids),
            'chroma_count': len(chroma_source_data_ids),
            'to_delete': sorted(source_data_to_delete),
            'to_add': source_data_to_add_data,
            'to_delete_count': len(source_data_to_delete),
            'to_add_count': len(source_data_to_add)
        }
    }


def delete_from_chroma(store: ChromaVectorStore, indicator_ids: List[int], source_data_ids: List[int]) -> Dict:
    """从 Chroma 删除多余的数据"""
    deleted_indicators = 0
    deleted_source_data = 0

    for id in indicator_ids:
        try:
            store.delete_indicator(id)
            deleted_indicators += 1
            logger.debug(f"已删除指标: {id}")
        except Exception as e:
            logger.warning(f"删除指标 {id} 失败: {e}")

    for id in source_data_ids:
        try:
            store.delete_source_data(id)
            deleted_source_data += 1
            logger.debug(f"已删除数据源: {id}")
        except Exception as e:
            logger.warning(f"删除数据源 {id} 失败: {e}")

    return {
        'deleted_indicators': deleted_indicators,
        'deleted_source_data': deleted_source_data
    }


def add_to_chroma(store: ChromaVectorStore, indicators: List[Dict], source_data: List[Dict]) -> Dict:
    """向 Chroma 添加缺失的数据"""
    added_indicators = 0
    added_source_data = 0

    # 处理指标
    if indicators:
        logger.info(f"开始为 {len(indicators)} 个指标生成向量...")
        names = [item['name'] for item in indicators]
        vectors = encode_texts(names)

        for i, item in enumerate(indicators):
            if i < len(vectors) and vectors[i]:
                try:
                    store.add_indicator(item['id'], item['name'], vectors[i])
                    added_indicators += 1
                    logger.debug(f"已添加指标: {item['id']} - {item['name']}")
                except Exception as e:
                    logger.warning(f"添加指标 {item['id']} 失败: {e}")

    # 处理数据源
    if source_data:
        logger.info(f"开始为 {len(source_data)} 个数据源生成向量...")
        names = [item['name'] for item in source_data]
        vectors = encode_texts(names)

        for i, item in enumerate(source_data):
            if i < len(vectors) and vectors[i]:
                try:
                    store.add_source_data(item['id'], item['name'], vectors[i])
                    added_source_data += 1
                    logger.debug(f"已添加数据源: {item['id']} - {item['name']}")
                except Exception as e:
                    logger.warning(f"添加数据源 {item['id']} 失败: {e}")

    return {
        'added_indicators': added_indicators,
        'added_source_data': added_source_data
    }


def print_report(diff_result: Dict, sync_result: Optional[Dict] = None):
    """打印报告"""
    print("\n" + "=" * 60)
    print("Chroma 与 MySQL 数据校验报告")
    print("=" * 60)

    # 指标统计
    print("\n【指标 (indicators)】")
    print(f"  MySQL 数量: {diff_result['indicators']['mysql_count']}")
    print(f"  Chroma 数量: {diff_result['indicators']['chroma_count']}")
    print(f"  Chroma 多余 (需删除): {diff_result['indicators']['to_delete_count']}")
    print(f"  Chroma 缺失 (需添加): {diff_result['indicators']['to_add_count']}")

    if diff_result['indicators']['to_delete']:
        print(f"  删除列表: {diff_result['indicators']['to_delete'][:10]}")
        if len(diff_result['indicators']['to_delete']) > 10:
            print(f"           ... 还有 {len(diff_result['indicators']['to_delete']) - 10} 条")

    if diff_result['indicators']['to_add']:
        print(f"  添加列表 (前5条):")
        for item in diff_result['indicators']['to_add'][:5]:
            print(f"    - ID:{item['id']} | {item['name']}")
        if len(diff_result['indicators']['to_add']) > 5:
            print(f"    ... 还有 {len(diff_result['indicators']['to_add']) - 5} 条")

    # 数据源统计
    print("\n【数据源 (source_data)】")
    print(f"  MySQL 数量: {diff_result['source_data']['mysql_count']}")
    print(f"  Chroma 数量: {diff_result['source_data']['chroma_count']}")
    print(f"  Chroma 多余 (需删除): {diff_result['source_data']['to_delete_count']}")
    print(f"  Chroma 缺失 (需添加): {diff_result['source_data']['to_add_count']}")

    if diff_result['source_data']['to_delete']:
        print(f"  删除列表: {diff_result['source_data']['to_delete'][:10]}")
        if len(diff_result['source_data']['to_delete']) > 10:
            print(f"           ... 还有 {len(diff_result['source_data']['to_delete']) - 10} 条")

    if diff_result['source_data']['to_add']:
        print(f"  添加列表 (前5条):")
        for item in diff_result['source_data']['to_add'][:5]:
            print(f"    - ID:{item['id']} | {item['name']}")
        if len(diff_result['source_data']['to_add']) > 5:
            print(f"    ... 还有 {len(diff_result['source_data']['to_add']) - 5} 条")

    # 同步结果
    if sync_result:
        print("\n" + "-" * 60)
        print("同步结果")
        print("-" * 60)
        print(f"  删除指标数: {sync_result['deleted_indicators']}")
        print(f"  删除数据源数: {sync_result['deleted_source_data']}")
        print(f"  添加指标数: {sync_result['added_indicators']}")
        print(f"  添加数据源数: {sync_result['added_source_data']}")

        total_delete = sync_result['deleted_indicators'] + sync_result['deleted_source_data']
        total_add = sync_result['added_indicators'] + sync_result['added_source_data']

        if total_delete == 0 and total_add == 0:
            print("\n  状态: 数据已完全同步，无需修改")
        else:
            print(f"\n  状态: 同步完成 (删除 {total_delete} 条, 添加 {total_add} 条)")

    print("\n" + "=" * 60)


def main():
    parser = argparse.ArgumentParser(
        description='Chroma 与 MySQL 数据同步校验工具',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  %(prog)s --check-only        # 仅检查差异，不修改
  %(prog)s --sync             # 执行同步
  %(prog)s --config /path/to/application.yml  # 指定配置文件
        """
    )

    parser.add_argument('--check-only', action='store_true',
                        help='仅检查差异，不执行同步')
    parser.add_argument('--sync', action='store_true',
                        help='执行同步操作')
    parser.add_argument('--config', default=DEFAULT_CONFIG_PATH,
                        help=f'配置文件路径 (默认: {DEFAULT_CONFIG_PATH})')
    parser.add_argument('--chroma-path', default=DEFAULT_CHROMA_PATH,
                        help=f'Chroma 存储路径 (默认: {DEFAULT_CHROMA_PATH})')

    args = parser.parse_args()

    if not args.check_only and not args.sync:
        parser.print_help()
        print("\n请指定 --check-only 或 --sync")
        sys.exit(1)

    print("开始校验 Chroma 与 MySQL 数据...")
    print(f"Chroma 路径: {args.chroma_path}")
    print(f"配置文件: {args.config}")

    # 1. 解析 MySQL 配置
    mysql_config = parse_mysql_config(args.config)

    # 2. 连接 MySQL
    print("\n正在连接 MySQL...")
    mysql_conn = connect_mysql(mysql_config)

    # 3. 初始化 Chroma
    print("正在初始化 Chroma...")
    chroma_store = ChromaVectorStore(args.chroma_path)

    # 4. 读取数据
    print("\n正在读取数据...")

    # MySQL 数据
    mysql_indicators = get_indicators_from_mysql(mysql_conn)
    mysql_source_data = get_source_data_from_mysql(mysql_conn)

    # Chroma 数据
    chroma_indicator_ids = get_indicators_from_chroma(chroma_store)
    chroma_source_data_ids = get_source_data_from_chroma(chroma_store)

    # 5. 比对差异
    diff_result = compare_data(
        mysql_indicators, chroma_indicator_ids,
        mysql_source_data, chroma_source_data_ids
    )

    # 6. 打印报告
    print_report(diff_result)

    # 7. 执行同步
    sync_result = None
    if args.sync:
        print("\n正在执行同步...")

        # 删除多余数据
        delete_result = delete_from_chroma(
            chroma_store,
            diff_result['indicators']['to_delete'],
            diff_result['source_data']['to_delete']
        )

        # 添加缺失数据
        add_result = add_to_chroma(
            chroma_store,
            diff_result['indicators']['to_add'],
            diff_result['source_data']['to_add']
        )

        sync_result = {**delete_result, **add_result}

        # 打印更新后的报告
        print_report(diff_result, sync_result)

    # 关闭连接
    mysql_conn.close()

    # 返回状态码
    if args.sync:
        total_changes = 0
        if sync_result:
            total_changes = (
                sync_result.get('deleted_indicators', 0) +
                sync_result.get('deleted_source_data', 0) +
                sync_result.get('added_indicators', 0) +
                sync_result.get('added_source_data', 0)
            )
        return 0 if total_changes == 0 else 1

    return 0


if __name__ == "__main__":
    sys.exit(main())
