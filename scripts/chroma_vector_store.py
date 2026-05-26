#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Chroma 向量存储核心类
管理指标和数据源的向量存储与搜索
"""

import chromadb
from chromadb.config import Settings
from typing import List, Dict, Any, Optional
import logging

# 禁用 INFO 日志，避免干扰 stdout JSON 协议
logging.basicConfig(level=logging.WARNING)
logger = logging.getLogger(__name__)


class ChromaVectorStore:
    """
    Chroma 向量存储管理器

    提供指标和数据源向量的存储、搜索、管理功能
    """

    # 集合名称常量
    COLLECTION_INDICATOR = "indicators"
    COLLECTION_SOURCE_DATA = "source_data"

    # 向量维度（与 sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2 一致）
    DIMENSION = 384

    def __init__(self, persist_dir: str = "./chroma_storage"):
        """
        初始化 Chroma 客户端

        Args:
            persist_dir: 持久化存储目录
        """
        self.persist_dir = persist_dir
        logger.info(f"初始化 Chroma 客户端，存储目录: {persist_dir}")

        # 使用 PersistentClient 实现本地磁盘持久化
        self.client = chromadb.PersistentClient(path=persist_dir)

        # 初始化集合
        self._init_collections()

    def _init_collections(self):
        """初始化指标和数据源集合"""
        try:
            # 获取或创建指标集合（使用余弦相似度）
            self.indicator_collection = self.client.get_or_create_collection(
                name=self.COLLECTION_INDICATOR,
                metadata={
                    "dimension": self.DIMENSION,
                    "hnsw:space": "cosine",
                    "description": "指标向量集合"
                }
            )
            logger.info(f"指标集合初始化完成，当前数量: {self.indicator_collection.count()}，距离函数: cosine")

            # 获取或创建数据源集合（使用余弦相似度）
            self.source_data_collection = self.client.get_or_create_collection(
                name=self.COLLECTION_SOURCE_DATA,
                metadata={
                    "dimension": self.DIMENSION,
                    "hnsw:space": "cosine",
                    "description": "数据源向量集合"
                }
            )
            logger.info(f"数据源集合初始化完成，当前数量: {self.source_data_collection.count()}，距离函数: cosine")

        except Exception as e:
            logger.error(f"初始化集合失败: {e}")
            raise

    def warmup(self):
        """预热连接 - 触发 Chroma 库的懒加载"""
        try:
            # 执行简单查询来预热 HNSW 索引
            self.indicator_collection.count()
            self.source_data_collection.count()

            # 执行一次真实的搜索来预热搜索索引
            if self.indicator_collection.count() > 0:
                dummy_vector = [0.0] * self.DIMENSION
                self.indicator_collection.query(
                    query_embeddings=[dummy_vector],
                    n_results=1
                )

            logger.info("Chroma 连接预热完成")
        except Exception as e:
            logger.warn(f"预热时出现警告（非致命）: {e}")

    # ==================== 指标集合操作 ====================

    def add_indicator(self, id: int, name: str, vector: List[float], metadata: Dict = None):
        """
        添加单个指标向量

        Args:
            id: 指标ID（对应MySQL主键）
            name: 指标名称
            vector: 向量（384维）
            metadata: 额外元数据
        """
        try:
            self.indicator_collection.add(
                ids=[str(id)],
                embeddings=[vector],
                metadatas=[{
                    "name": name,
                    **(metadata or {})
                }]
            )
            logger.debug(f"添加指标向量成功: id={id}, name={name}")
        except Exception as e:
            logger.error(f"添加指标向量失败: id={id}, error={e}")
            raise

    def add_indicators_batch(self, ids: List[int], names: List[str],
                            vectors: List[List[float]], metadatas: List[Dict] = None):
        """
        批量添加指标向量

        Args:
            ids: 指标ID列表
            names: 指标名称列表
            vectors: 向量列表
            metadatas: 额外元数据列表
        """
        if len(ids) != len(names) or len(ids) != len(vectors):
            raise ValueError("ids, names, vectors 长度必须一致")

        try:
            self.indicator_collection.add(
                ids=[str(id) for id in ids],
                embeddings=vectors,
                metadatas=metadatas or [{"name": name} for name in names]
            )
            logger.info(f"批量添加指标向量成功: {len(ids)} 条")
        except Exception as e:
            logger.error(f"批量添加指标向量失败: {e}")
            raise

    def search_indicators(self, query_vector: List[float], top_k: int = 5) -> List[Dict]:
        """
        搜索相似指标

        Args:
            query_vector: 查询向量
            top_k: 返回数量

        Returns:
            搜索结果列表，每项包含 id, distance, metadata
        """
        try:
            results = self.indicator_collection.query(
                query_embeddings=[query_vector],
                n_results=top_k
            )
            return self._format_search_results(results)
        except Exception as e:
            logger.error(f"搜索指标失败: {e}")
            return []

    def delete_indicator(self, id: int):
        """
        删除单个指标向量

        Args:
            id: 指标ID
        """
        try:
            self.indicator_collection.delete(ids=[str(id)])
            logger.debug(f"删除指标向量成功: id={id}")
        except Exception as e:
            logger.error(f"删除指标向量失败: id={id}, error={e}")

    def get_indicator_count(self) -> int:
        """
        获取指标数量

        Returns:
            指标总数
        """
        return self.indicator_collection.count()

    def clear_indicators(self):
        """清空指标集合"""
        try:
            self.client.delete_collection(self.COLLECTION_INDICATOR)
            self.indicator_collection = self.client.get_or_create_collection(
                name=self.COLLECTION_INDICATOR,
                metadata={"dimension": self.DIMENSION, "hnsw:space": "cosine"}
            )
            logger.info("指标集合已清空并重建")
        except Exception as e:
            logger.error(f"清空指标集合失败: {e}")
            raise

    def get_all_indicators(self) -> List[Dict]:
        """
        获取所有指标向量

        Returns:
            所有指标数据列表
        """
        try:
            results = self.indicator_collection.get()
            items = []
            if results and results['ids']:
                for i, id_val in enumerate(results['ids']):
                    items.append({
                        "id": int(id_val),
                        "vector": results['embeddings'][i] if results['embeddings'] else None,
                        "metadata": results['metadatas'][i] if results['metadatas'] else None
                    })
            return items
        except Exception as e:
            logger.error(f"获取所有指标失败: {e}")
            return []

    # ==================== 数据源集合操作 ====================

    def add_source_data(self, id: int, name: str, vector: List[float], metadata: Dict = None):
        """
        添加单个数据源向量

        Args:
            id: 数据源ID
            name: 数据源名称
            vector: 向量（384维）
            metadata: 额外元数据
        """
        try:
            self.source_data_collection.add(
                ids=[str(id)],
                embeddings=[vector],
                metadatas=[{
                    "name": name,
                    **(metadata or {})
                }]
            )
            logger.debug(f"添加数据源向量成功: id={id}, name={name}")
        except Exception as e:
            logger.error(f"添加数据源向量失败: id={id}, error={e}")
            raise

    def add_source_data_batch(self, ids: List[int], names: List[str],
                              vectors: List[List[float]], metadatas: List[Dict] = None):
        """
        批量添加数据源向量

        Args:
            ids: 数据源ID列表
            names: 数据源名称列表
            vectors: 向量列表
            metadatas: 额外元数据列表
        """
        if len(ids) != len(names) or len(ids) != len(vectors):
            raise ValueError("ids, names, vectors 长度必须一致")

        try:
            self.source_data_collection.add(
                ids=[str(id) for id in ids],
                embeddings=vectors,
                metadatas=metadatas or [{"name": name} for name in names]
            )
            logger.info(f"批量添加数据源向量成功: {len(ids)} 条")
        except Exception as e:
            logger.error(f"批量添加数据源向量失败: {e}")
            raise

    def search_source_data(self, query_vector: List[float], top_k: int = 5) -> List[Dict]:
        """
        搜索相似数据源

        Args:
            query_vector: 查询向量
            top_k: 返回数量

        Returns:
            搜索结果列表
        """
        try:
            results = self.source_data_collection.query(
                query_embeddings=[query_vector],
                n_results=top_k
            )
            return self._format_search_results(results)
        except Exception as e:
            logger.error(f"搜索数据源失败: {e}")
            return []

    def delete_source_data(self, id: int):
        """
        删除单个数据源向量

        Args:
            id: 数据源ID
        """
        try:
            self.source_data_collection.delete(ids=[str(id)])
            logger.debug(f"删除数据源向量成功: id={id}")
        except Exception as e:
            logger.error(f"删除数据源向量失败: id={id}, error={e}")

    def get_source_data_count(self) -> int:
        """
        获取数据源数量

        Returns:
            数据源总数
        """
        return self.source_data_collection.count()

    def clear_source_data(self):
        """清空数据源集合"""
        try:
            self.client.delete_collection(self.COLLECTION_SOURCE_DATA)
            self.source_data_collection = self.client.get_or_create_collection(
                name=self.COLLECTION_SOURCE_DATA,
                metadata={"dimension": self.DIMENSION, "hnsw:space": "cosine"}
            )
            logger.info("数据源集合已清空并重建")
        except Exception as e:
            logger.error(f"清空数据源集合失败: {e}")
            raise

    def get_all_source_data(self) -> List[Dict]:
        """
        获取所有数据源向量

        Returns:
            所有数据源数据列表
        """
        try:
            results = self.source_data_collection.get()
            items = []
            if results and results['ids']:
                for i, id_val in enumerate(results['ids']):
                    items.append({
                        "id": int(id_val),
                        "vector": results['embeddings'][i] if results['embeddings'] else None,
                        "metadata": results['metadatas'][i] if results['metadatas'] else None
                    })
            return items
        except Exception as e:
            logger.error(f"获取所有数据源失败: {e}")
            return []

    # ==================== 工具方法 ====================

    def _format_search_results(self, results) -> List[Dict]:
        """
        格式化搜索结果

        Args:
            results: Chroma 查询结果

        Returns:
            格式化后的结果列表
        """
        formatted = []
        if results and results['ids'] and len(results['ids']) > 0:
            for i, id_val in enumerate(results['ids'][0]):
                item = {
                    "id": int(id_val),
                    "distance": float(results['distances'][0][i]) if results['distances'] else None,
                    "metadata": results['metadatas'][0][i] if results.get('metadatas') and results['metadatas'] else None
                }
                formatted.append(item)
        return formatted

    def get_stats(self) -> Dict[str, int]:
        """
        获取统计信息

        Returns:
            统计信息字典
        """
        return {
            "indicator_count": self.get_indicator_count(),
            "source_data_count": self.get_source_data_count(),
            "persist_dir": self.persist_dir,
            "dimension": self.DIMENSION
        }

    def reset(self):
        """重置所有数据"""
        self.clear_indicators()
        self.clear_source_data()
        logger.info("Chroma 数据已全部重置")


# 全局实例（用于脚本模式）
_store = None


def get_store(persist_dir: str = "./chroma_storage") -> ChromaVectorStore:
    """获取全局 ChromaVectorStore 实例"""
    global _store
    if _store is None:
        _store = ChromaVectorStore(persist_dir)
    return _store


if __name__ == "__main__":
    # 测试代码
    store = ChromaVectorStore()

    # 输出统计信息
    stats = store.get_stats()
    print(f"Chroma 统计信息: {stats}")

    # 测试搜索
    test_vector = [0.1] * 384
    results = store.search_indicators(test_vector, top_k=3)
    print(f"搜索结果: {results}")
