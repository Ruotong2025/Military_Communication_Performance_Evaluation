#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
语义相似度计算脚本
使用 paraphrase-multilingual-MiniLM-L12-v2 模型计算中文指标的语义相似度

用法：
    相似度模式（默认）:
        通过 stdin 接收 JSON 数据
        输入格式: {"query": "查询词", "candidates": ["候选词1", "候选词2", ...]}
        输出格式: [{"name": "候选词", "similarity": 0.95}, ...] (按相似度降序)

    --encode 模式:
        通过 stdin 接收 JSON 数据
        输入格式: {"texts": ["文本1", "文本2", ...]}
        输出格式: {"vectors": [[0.1, -0.2, ...], [0.3, -0.4, ...]]}

    --warmup:
        预加载模型（不等待输入，用于服务启动时预热）
"""

import sys
import json
import warnings
import os
import numpy as np

warnings.filterwarnings('ignore')

_model = None

def load_model():
    """加载模型，使用缓存避免重复加载"""
    global _model
    if _model is None:
        try:
            import os
            # 使用国内镜像站
            os.environ.setdefault('HF_ENDPOINT', 'https://hf-mirror.com')
            
            from sentence_transformers import SentenceTransformer
            
            model_name = 'paraphrase-multilingual-MiniLM-L12-v2'
            full_model_name = f'sentence-transformers/{model_name}'
            cache_dir = os.path.join(os.path.expanduser("~"), ".cache", "huggingface", "hub")
            
            print(f"检查模型缓存目录: {cache_dir}", file=sys.stderr)
            
            # 检查模型是否已缓存（sentence-transformers模型实际路径前缀）
            model_cache_path = os.path.join(cache_dir, f"models--{full_model_name.replace('/', '--')}")
            if not os.path.exists(model_cache_path):
                print(f"模型未缓存，正在从镜像站下载（约400MB）...", file=sys.stderr)
            
            print(f"正在加载模型: {model_name}", file=sys.stderr)
            _model = SentenceTransformer(model_name)
            print("模型加载成功", file=sys.stderr)
        except Exception as e:
            print(f"模型加载失败: {e}", file=sys.stderr)
            raise
    return _model

def calculate_similarity(query, candidates):
    """计算查询词与候选词的语义相似度"""
    if not query or not candidates:
        print(f"[DEBUG] 参数检查失败: query={query}, candidates={candidates}", file=sys.stderr)
        return []

    model = load_model()

    # 计算嵌入向量
    query_embedding = model.encode(query, convert_to_numpy=True)
    candidate_embeddings = model.encode(candidates, convert_to_numpy=True)

    print(f"[DEBUG] 嵌入向量计算完成: query_embedding.shape={query_embedding.shape}, candidate_embeddings.shape={candidate_embeddings.shape}", file=sys.stderr)

    # 计算余弦相似度
    query_norm = query_embedding / (np.linalg.norm(query_embedding) + 1e-8)
    candidate_norms = candidate_embeddings / (np.linalg.norm(candidate_embeddings, axis=1, keepdims=True) + 1e-8)
    similarities = (candidate_norms @ query_norm).tolist()

    # 构建结果
    results = [
        {'name': name, 'similarity': sim}
        for name, sim in zip(candidates, similarities)
    ]

    # 按相似度降序排列
    results.sort(key=lambda x: x['similarity'], reverse=True)

    print(f"[DEBUG] 计算完成，返回 {len(results)} 条结果", file=sys.stderr)

    return results

def warmup():
    """预热：加载模型但不执行计算"""
    print("开始预加载模型...", file=sys.stderr)
    load_model()
    print("模型预加载完成", file=sys.stderr)

def encode_texts(texts):
    """计算单个或多个文本的向量"""
    if not texts:
        return {"vectors": []}

    model = load_model()
    embeddings = model.encode(texts, convert_to_numpy=True)
    vectors = embeddings.tolist()

    print(f"[DEBUG] 向量计算完成: texts数量={len(texts)}, 向量维度={len(vectors[0]) if vectors else 0}", file=sys.stderr)

    return {"vectors": vectors}

def persistent_mode():
    """持久化模式：保持进程运行，处理多轮请求"""
    model = load_model()
    print("READY", flush=True)  # 通知 Java 进程已就绪

    while True:
        try:
            # 使用缓冲读取一行
            line = sys.stdin.readline()
            if not line or line.strip() == "":
                break

            input_data = line.strip()

            # 解析 JSON
            data = json.loads(input_data)
            texts = data.get('texts', [])

            if not texts:
                print(json.dumps({"vectors": []}), flush=True)
                print("DONE", flush=True)
                continue

            # 计算向量
            embeddings = model.encode(texts, convert_to_numpy=True)
            vectors = embeddings.tolist()

            result = {"vectors": vectors}
            output = json.dumps(result, ensure_ascii=False)
            print(output, flush=True)
            print("DONE", flush=True)

        except json.JSONDecodeError as e:
            print(f"JSON解析错误: {e}", file=sys.stderr)
            print(json.dumps({"vectors": []}), flush=True)
            print("DONE", flush=True)
        except Exception as e:
            import traceback
            traceback.print_exc()
            print(f"计算错误: {e}", file=sys.stderr)
            print(json.dumps({"vectors": []}), flush=True)
            print("DONE", flush=True)


def main():
    # 检查是否为持久化模式
    if len(sys.argv) > 1 and sys.argv[1] == '--persistent':
        persistent_mode()
        return

    # 检查是否为预热模式
    if len(sys.argv) > 1 and sys.argv[1] == '--warmup':
        warmup()
        return

    # 检查是否为 encode 模式
    if len(sys.argv) > 1 and sys.argv[1] == '--encode':
        try:
            # 从 stdin 读取输入
            if sys.stdin.buffer is not None:
                raw_input = sys.stdin.buffer.read()
                input_data = raw_input.decode('utf-8', errors='replace').strip()
            else:
                input_data = sys.stdin.read().strip()

            if not input_data:
                print(json.dumps({"vectors": []}))
                return

            # 解析 JSON
            data = json.loads(input_data)
            texts = data.get('texts', [])

            if not texts:
                print(json.dumps({"vectors": []}))
                return

            # 计算向量
            result = encode_texts(texts)

            # 输出结果
            output = json.dumps(result, ensure_ascii=False)
            if sys.stdout.buffer is not None:
                sys.stdout.buffer.write(output.encode('utf-8'))
                sys.stdout.buffer.flush()
            else:
                sys.stdout.write(output)
                sys.stdout.flush()

        except json.JSONDecodeError as e:
            print(f"JSON解析错误: {e}", file=sys.stderr)
            print(json.dumps({"vectors": []}))
        except Exception as e:
            import traceback
            traceback.print_exc()
            print(f"计算错误: {e}", file=sys.stderr)
            print(json.dumps({"vectors": []}))
        return

    try:
        # 从 stdin 读取输入（直接用 buffer + UTF-8，避免 Windows 默认编码问题）
        if sys.stdin.buffer is not None:
            raw_input = sys.stdin.buffer.read()
            input_data = raw_input.decode('utf-8', errors='replace').strip()
        else:
            input_data = sys.stdin.read().strip()

        if not input_data:
            print('[]')
            return

        # 解析 JSON
        data = json.loads(input_data)
        query = data.get('query', '')
        candidates = data.get('candidates', [])

        print(f"[DEBUG] 收到请求: query='{query}', candidates数量={len(candidates)}", file=sys.stderr)

        if not query:
            print('[]')
            return

        # 计算相似度
        results = calculate_similarity(query, candidates)

        # 输出结果（直接写 buffer + UTF-8）
        output = json.dumps(results, ensure_ascii=False)
        if sys.stdout.buffer is not None:
            sys.stdout.buffer.write(output.encode('utf-8'))
            sys.stdout.buffer.flush()
        else:
            sys.stdout.write(output)
            sys.stdout.flush()
        print(f"[DEBUG] 计算完成，返回 {len(results)} 条结果", file=sys.stderr)

    except json.JSONDecodeError as e:
        print(f"JSON解析错误: {e}", file=sys.stderr)
        print('[]')
    except Exception as e:
        import traceback
        traceback.print_exc()
        print(f"计算错误: {e}", file=sys.stderr)
        # 关键错误也输出到stdout，方便Java端捕获
        print(f"ERROR: {e}", file=sys.stdout)
        print('[]')

if __name__ == "__main__":
    main()
