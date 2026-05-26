#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Chroma 向量存储管理脚本
供 Java 调用，执行向量操作

用法:
    # 单次命令模式
    python chroma_manager.py <command> [args]

    # 持久化模式（供进程池调用）
    python chroma_manager.py --persistent

持久化模式协议:
    1. 启动后输出 "READY" 表示准备就绪
    2. 接收 JSON 行，每行包含 {"command": "...", "payload": {...}}
    3. 输出 JSON 结果行
    4. 收到空行或 "SHUTDOWN" 命令时退出
"""

import sys
import json
import os
import signal
import logging

# 禁用 INFO 日志，避免干扰 stdout JSON 协议
logging.basicConfig(level=logging.WARNING)

# 添加脚本目录到路径
script_dir = os.path.dirname(os.path.abspath(__file__))
if script_dir not in sys.path:
    sys.path.insert(0, script_dir)

from chroma_vector_store import ChromaVectorStore, get_store

# 默认存储目录
DEFAULT_PERSIST_DIR = "./chroma_storage"


def cmd_add_indicators(store: ChromaVectorStore, data: dict) -> dict:
    """添加指标向量"""
    try:
        ids = data['ids']
        names = data['names']
        vectors = data['vectors']

        store.add_indicators_batch(ids, names, vectors)

        return {
            "success": True,
            "message": f"成功添加 {len(ids)} 个指标向量",
            "count": len(ids),
            "total": store.get_indicator_count()
        }
    except Exception as e:
        return {"success": False, "error": str(e)}


def cmd_search_indicators(store: ChromaVectorStore, data: dict) -> dict:
    """搜索相似指标"""
    try:
        query = data['query']
        top_k = data.get('top_k', 5)

        results = store.search_indicators(query, top_k)

        return {
            "success": True,
            "message": f"找到 {len(results)} 个相似指标",
            "results": results,
            "count": len(results)
        }
    except Exception as e:
        return {"success": False, "error": str(e)}


def cmd_add_source_data(store: ChromaVectorStore, data: dict) -> dict:
    """添加数据源向量"""
    try:
        items = data['items']

        ids = [item['id'] for item in items]
        names = [item['name'] for item in items]
        vectors = [item['vector'] for item in items]

        store.add_source_data_batch(ids, names, vectors)

        return {
            "success": True,
            "message": f"成功添加 {len(items)} 个数据源向量",
            "count": len(items),
            "total": store.get_source_data_count()
        }
    except Exception as e:
        return {"success": False, "error": str(e)}


def cmd_search_source_data(store: ChromaVectorStore, data: dict) -> dict:
    """搜索相似数据源"""
    try:
        query = data['query']
        top_k = data.get('top_k', 5)

        results = store.search_source_data(query, top_k)

        return {
            "success": True,
            "message": f"找到 {len(results)} 个相似数据源",
            "results": results,
            "count": len(results)
        }
    except Exception as e:
        return {"success": False, "error": str(e)}


def cmd_delete_indicator(store: ChromaVectorStore, data: dict) -> dict:
    """删除指标向量"""
    try:
        id = data['id']
        store.delete_indicator(id)
        return {
            "success": True,
            "message": f"成功删除指标向量: {id}",
            "total": store.get_indicator_count()
        }
    except Exception as e:
        return {"success": False, "error": str(e)}


def cmd_delete_source_data(store: ChromaVectorStore, data: dict) -> dict:
    """删除数据源向量"""
    try:
        id = data['id']
        store.delete_source_data(id)
        return {
            "success": True,
            "message": f"成功删除数据源向量: {id}",
            "total": store.get_source_data_count()
        }
    except Exception as e:
        return {"success": False, "error": str(e)}


def cmd_clear_indicators(store: ChromaVectorStore, data: dict) -> dict:
    """清空指标集合"""
    try:
        store.clear_indicators()
        return {"success": True, "message": "指标集合已清空"}
    except Exception as e:
        return {"success": False, "error": str(e)}


def cmd_clear_source_data(store: ChromaVectorStore, data: dict) -> dict:
    """清空数据源集合"""
    try:
        store.clear_source_data()
        return {"success": True, "message": "数据源集合已清空"}
    except Exception as e:
        return {"success": False, "error": str(e)}


def cmd_stats(store: ChromaVectorStore, data: dict) -> dict:
    """获取统计信息"""
    try:
        stats = store.get_stats()
        return {"success": True, **stats}
    except Exception as e:
        return {"success": False, "error": str(e)}


def cmd_warmup(store: ChromaVectorStore, data: dict) -> dict:
    """预热命令 - 触发 Chroma 连接初始化"""
    try:
        # 执行一次空查询来预热
        store.indicator_collection.count()
        store.source_data_collection.count()
        return {"success": True, "message": "预热完成"}
    except Exception as e:
        return {"success": False, "error": str(e)}


def cmd_reset(store: ChromaVectorStore, data: dict) -> dict:
    """重置所有数据"""
    try:
        store.reset()
        return {"success": True, "message": "所有数据已重置"}
    except Exception as e:
        return {"success": False, "error": str(e)}


def cmd_rebuild_index(store: ChromaVectorStore, data: dict) -> dict:
    """
    重建索引

    删除并重新创建集合，应用新的距离函数配置
    """
    try:
        collection_name = data.get('collection', 'indicators')

        if collection_name == 'indicators':
            # 删除旧集合
            try:
                store.client.delete_collection(store.COLLECTION_INDICATOR)
            except Exception:
                pass
            # 重新创建（会使用新的配置）
            store._init_collections()
            return {"success": True, "message": "指标索引已重建"}

        elif collection_name == 'source_data':
            try:
                store.client.delete_collection(store.COLLECTION_SOURCE_DATA)
            except Exception:
                pass
            store._init_collections()
            return {"success": True, "message": "数据源索引已重建"}

        elif collection_name == 'all':
            # 重建所有索引
            try:
                store.client.delete_collection(store.COLLECTION_INDICATOR)
                store.client.delete_collection(store.COLLECTION_SOURCE_DATA)
            except Exception:
                pass
            store._init_collections()
            return {"success": True, "message": "所有索引已重建"}

        else:
            return {"success": False, "error": f"未知集合: {collection_name}"}

    except Exception as e:
        return {"success": False, "error": str(e)}


# 命令路由表
COMMAND_HANDLERS = {
    "add-indicators": cmd_add_indicators,
    "search-indicators": cmd_search_indicators,
    "add-source-data": cmd_add_source_data,
    "search-source-data": cmd_search_source_data,
    "delete-indicator": cmd_delete_indicator,
    "delete-source-data": cmd_delete_source_data,
    "clear-indicators": cmd_clear_indicators,
    "clear-source-data": cmd_clear_source_data,
    "stats": cmd_stats,
    "reset": cmd_reset,
    "rebuild-index": cmd_rebuild_index,
    "warmup": cmd_warmup,
}


def persistent_mode(persist_dir: str):
    """
    持久化模式主循环

    保持进程运行，处理多轮请求
    """
    # 初始化 Chroma 存储
    store = ChromaVectorStore(persist_dir)

    # 通知 Java 进程已就绪
    print("READY", flush=True)

    running = True

    def signal_handler(signum, frame):
        nonlocal running
        running = False

    # 注册信号处理
    signal.signal(signal.SIGTERM, signal_handler)
    signal.signal(signal.SIGINT, signal_handler)

    while running:
        try:
            # 读取一行命令
            line = sys.stdin.readline()
            if not line:
                # EOF，退出
                break

            line = line.strip()
            if not line:
                continue

            # 解析命令
            try:
                request = json.loads(line)
            except json.JSONDecodeError as e:
                print(json.dumps({
                    "success": False,
                    "error": f"JSON 解析失败: {e}"
                }, ensure_ascii=False), flush=True)
                continue

            command = request.get('command')
            payload = request.get('payload', {})

            # 处理 SHUTDOWN 命令
            if command == "SHUTDOWN":
                print(json.dumps({"success": True, "message": "关闭中..."}, ensure_ascii=False), flush=True)
                break

            # 查找处理器
            handler = COMMAND_HANDLERS.get(command)
            if handler is None:
                print(json.dumps({
                    "success": False,
                    "error": f"未知命令: {command}"
                }, ensure_ascii=False), flush=True)
                continue

            # 执行命令
            result = handler(store, payload)

            # 输出结果
            print(json.dumps(result, ensure_ascii=False), flush=True)

        except Exception as e:
            import traceback
            print(json.dumps({
                "success": False,
                "error": str(e),
                "type": type(e).__name__
            }, ensure_ascii=False), flush=True)


def single_command_mode(command: str, persist_dir: str, stdin_data: str):
    """
    单次命令模式

    每次调用执行一个命令后退出
    """
    store = get_store(persist_dir)

    # 解析 payload
    try:
        payload = json.loads(stdin_data) if stdin_data else {}
    except json.JSONDecodeError as e:
        print(json.dumps({
            "success": False,
            "error": f"JSON 解析失败: {e}"
        }, ensure_ascii=False))
        sys.exit(1)

    # 查找处理器
    handler = COMMAND_HANDLERS.get(command)
    if handler is None:
        print(json.dumps({
            "success": False,
            "error": f"未知命令: {command}"
        }, ensure_ascii=False))
        sys.exit(1)

    # 执行命令
    try:
        result = handler(store, payload)
        print(json.dumps(result, ensure_ascii=False))
    except Exception as e:
        import traceback
        print(json.dumps({
            "success": False,
            "error": str(e),
            "type": type(e).__name__
        }, ensure_ascii=False))
        sys.exit(1)


def main():
    """主入口"""
    # 解析参数
    args = sys.argv[1:]

    # 持久化模式
    if '--persistent' in args or '-p' in args:
        persist_dir = os.environ.get('CHROMA_PERSIST_DIR', DEFAULT_PERSIST_DIR)
        persistent_mode(persist_dir)
        return

    # 单次命令模式
    if len(args) < 1:
        print(json.dumps({
            "success": False,
            "error": "用法: python chroma_manager.py <command> [--persistent|-p]"
        }, ensure_ascii=False))
        sys.exit(1)

    command = args[0]
    persist_dir = os.environ.get('CHROMA_PERSIST_DIR', DEFAULT_PERSIST_DIR)

    # 从 stdin 读取数据
    stdin_data = ""
    if not sys.stdin.isatty():
        stdin_data = sys.stdin.read()

    single_command_mode(command, persist_dir, stdin_data)


if __name__ == "__main__":
    main()
