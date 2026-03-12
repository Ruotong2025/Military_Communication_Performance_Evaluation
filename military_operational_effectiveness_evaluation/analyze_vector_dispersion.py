import numpy as np
import sqlite3

# 连接数据库
conn = sqlite3.connect('military_operational_effectiveness_evaluation.db')
cursor = conn.cursor()

# 读取数据
cursor.execute('SELECT expert_name, weight FROM experts_ahp ORDER BY expert_id')
data = cursor.fetchall()

expert_names = [row[0] for row in data]
weights = [eval(row[1]) for row in data]
weight_matrix = np.array(weights)

# 计算平均值
avg_weights = np.mean(weight_matrix, axis=0)

print("=" * 80)
print("向量公式 vs CV 对比分析")
print("=" * 80)

print(f"\n{'专家':<10} {'向量离散度':<15} {'CV(%)':<12} {'差异说明':<20}")
print("-" * 80)

vector_dispersions = []
cvs = []

for i, name in enumerate(expert_names):
    expert_weights = weight_matrix[i]
    
    # 向量相对离散度计算
    numerator = np.sqrt(np.sum((expert_weights - avg_weights) ** 2))
    denominator = np.sqrt(np.sum(expert_weights ** 2) + np.sum(avg_weights ** 2))
    vector_disp = numerator / denominator if denominator > 0 else 0
    
    # CV计算（相对指标均值偏差CV，与你的客观可信度口径一致）
    deviations = expert_weights - avg_weights
    std_of_deviations = np.std(deviations, ddof=1) if deviations.size > 1 else 0.0
    overall_mean = float(np.mean(weight_matrix))
    cv = (std_of_deviations / overall_mean) * 100 if overall_mean > 0 else 0.0
    
    vector_dispersions.append(vector_disp)
    cvs.append(cv)
    
    # 判断差异
    if cv > 35:
        level = "严重分歧(CV>35%)"
    elif cv > 25:
        level = "轻度分歧"
    else:
        level = "较为一致"
    
    print(f"{name:<10} {vector_disp:<15.6f} {cv:<12.2f} {level:<20}")

print("\n" + "=" * 80)
print("统计总结")
print("=" * 80)

print(f"\n向量离散度:")
print(f"  平均值: {np.mean(vector_dispersions):.6f}")
print(f"  范围: {min(vector_dispersions):.6f} ~ {max(vector_dispersions):.6f}")
print(f"  区间跨度: {max(vector_dispersions) - min(vector_dispersions):.6f}")

print(f"\nCV变异系数:")
print(f"  平均值: {np.mean(cvs):.2f}%")
print(f"  范围: {min(cvs):.2f}% ~ {max(cvs):.2f}%")
print(f"  区间跨度: {max(cvs) - min(cvs):.2f}%")

print("\n" + "=" * 80)
print("为什么向量离散度值这么低？")
print("=" * 80)

print("""
1. 【分母效应】向量公式的分母包含两个向量的模的平方和：
   分母 = √(Σ(ωⁱⱼ)² + Σ(ω̄ⱼ)²)
   
   当评分值较大时（如1-10分），分母会非常大：
   例如：ωⁱⱼ = [5,6,7,8,9,...] 
        Σ(ωⁱⱼ)² = 5² + 6² + 7² + ... = 很大的数
   
   大分母会"压缩"最终结果，使其接近0

2. 【归一化特性】向量相对离散度的理论最大值约为0.707（当两个向量正交时）
   实际数据中很难达到这个值

3. 【CV不受量纲影响】CV = (标准差/平均值) × 100%
   - 相对度量，不受评分范围影响
   - 直接反映"相对波动程度"

4. 【数值示例】
   假设专家评分: [3, 5, 7, 9]
   平均评分: [6, 6, 6, 6]
   
   向量分子: √((3-6)² + (5-6)² + (7-6)² + (9-6)²) = √(9+1+1+9) = √20 ≈ 4.47
   向量分母: √(3²+5²+7²+9² + 6²×4) = √(164 + 144) = √308 ≈ 17.55
   向量离散度: 4.47 / 17.55 ≈ 0.255 ← 看起来很小！
   
   CV: std([3,5,7,9]) / mean([3,5,7,9]) × 100% = 2.58 / 6 × 100% ≈ 43% ← 更直观！
""")

print("\n" + "=" * 80)
print("结论")
print("=" * 80)
print("""
向量离散度不是"太低"，而是：
1. 它的取值范围本身就在 0~0.707 之间（理论最大值）
2. 当前数据的向量离散度 0.2~0.3 实际上已经是"中等偏高"水平
3. CV的百分比表示（0~100%+）更符合人类直觉

这就是为什么我们选择用CV作为客观可信度的计算依据！
""")

conn.close()
