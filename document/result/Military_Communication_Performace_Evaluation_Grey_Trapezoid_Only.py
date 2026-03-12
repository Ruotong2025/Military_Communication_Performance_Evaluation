import numpy as np
import matplotlib.pyplot as plt

# 设置中文字体 - 使用系统默认支持中文的字体
import platform
system = platform.system()

if system == 'Windows':
    plt.rcParams['font.sans-serif'] = ['Microsoft YaHei', 'SimHei', 'Arial Unicode MS']
elif system == 'Darwin':  # macOS
    plt.rcParams['font.sans-serif'] = ['PingFang SC', 'Heiti SC', 'Apple Color Emoji']
else:  # Linux
    plt.rcParams['font.sans-serif'] = ['WenQuanYi Micro Hei', 'Noto Sans CJK SC']
    
plt.rcParams['axes.unicode_minus'] = False

print("="*80)
print("端点梯形白化权函数（5灰类，调整后的端点）")
print("="*80)

# 端点定义
a = {
    0: -2,   # 左延拓
    1: 0,    # 第1个端点
    2: 2,    # 第2个端点
    3: 4,    # 第3个端点
    4: 6,    # 第4个端点
    5: 8,    # 第5个端点
    6: 10,   # 第6个端点
    7: 12    # 右延拓
}

print("\n端点定义：")
print("-"*80)
for i in range(8):
    print(f"a_{i} = {a[i]}")
print("-"*80)

# 灰类定义（从劣到优，k=1到5）
print("灰类区间定义（按公式 [a_{k-1}, a_{k+2}]）：")
print("-"*80)
grey_names = {1: "劣", 2: "差", 3: "中", 4: "良", 5: "优"}
for k in [1, 2, 3, 4, 5]:
    a_k_minus_1 = a[k-1]
    a_k_plus_2 = a[k+2]
    print(f"灰类{k}（{grey_names[k]}）：[a_{k-1}, a_{k+2}] = [{a_k_minus_1}, {a_k_plus_2}]")
print("-"*80)

def calculate_grey_params(k):
    """计算灰类k的参数"""
    a_k = a[k]
    a_k_plus_1 = a[k+1]
    
    # λ_k = (a_k + a_{k+1}) / 2
    lambda_k = (a_k + a_k_plus_1) / 2
    
    # 平台起点：(a_k + λ_k) / 2
    c_k = (a_k + lambda_k) / 2
    
    # 平台终点：(λ_k + a_{k+1}) / 2
    d_k = (lambda_k + a_k_plus_1) / 2
    
    # 区间边界
    a_k_minus_1 = a[k-1]
    a_k_plus_2 = a[k+2]
    
    return {
        'a_k': a_k,
        'a_k_plus_1': a_k_plus_1,
        'lambda_k': lambda_k,
        'c_k': c_k,
        'd_k': d_k,
        'a_k_minus_1': a_k_minus_1,
        'a_k_plus_2': a_k_plus_2
    }

def whitening_function_k(x, k):
    """通用的第k个灰类梯形白化权函数"""
    params = calculate_grey_params(k)
    
    a_k_minus_1 = params['a_k_minus_1']
    c_k = params['c_k']
    d_k = params['d_k']
    a_k_plus_2 = params['a_k_plus_2']
    
    if x < a_k_minus_1 or x > a_k_plus_2:
        return 0
    elif a_k_minus_1 <= x < c_k:
        return (x - a_k_minus_1) / (c_k - a_k_minus_1)
    elif c_k <= x <= d_k:
        return 1
    elif d_k < x <= a_k_plus_2:
        return (a_k_plus_2 - x) / (a_k_plus_2 - d_k)
    else:
        return 0

# 创建5个白化权函数
def whitening_function_1(x):
    return whitening_function_k(x, 1)

def whitening_function_2(x):
    return whitening_function_k(x, 2)

def whitening_function_3(x):
    return whitening_function_k(x, 3)

def whitening_function_4(x):
    return whitening_function_k(x, 4)

def whitening_function_5(x):
    return whitening_function_k(x, 5)

WHITENING_FUNCTIONS = [
    whitening_function_1,
    whitening_function_2,
    whitening_function_3,
    whitening_function_4,
    whitening_function_5
]

# 计算灰类代表值
GREY_CLASS_VALUES = [
    calculate_grey_params(1)['lambda_k'],  # 劣
    calculate_grey_params(2)['lambda_k'],  # 差
    calculate_grey_params(3)['lambda_k'],  # 中
    calculate_grey_params(4)['lambda_k'],  # 良
    calculate_grey_params(5)['lambda_k'],  # 优
]

# 打印详细参数
print("\n灰类参数详细计算：")
print("="*80)
for k in [1, 2, 3, 4, 5]:
    params = calculate_grey_params(k)
    print(f"\n灰类{k}（{grey_names[k]}）：")
    print(f"  a_k = a_{k} = {params['a_k']}")
    print(f"  a_{k+1} = {params['a_k_plus_1']}")
    print(f"  λ{k} = ({params['a_k']} + {params['a_k_plus_1']})/2 = {params['lambda_k']:.1f}")
    print(f"  平台起点 c{k} = ({params['a_k']} + {params['lambda_k']:.1f})/2 = {params['c_k']:.2f}")
    print(f"  平台终点 d{k} = ({params['lambda_k']:.1f} + {params['a_k_plus_1']})/2 = {params['d_k']:.2f}")
    print(f"  区间范围：[{params['a_k_minus_1']}, {params['a_k_plus_2']}]")
    print(f"  平台区域：[{params['c_k']:.2f}, {params['d_k']:.2f}]")

print("\n" + "="*80)

print("\n灰类代表值（λ值）：")
print(f"  劣: λ1 = {GREY_CLASS_VALUES[0]:.1f}")
print(f"  差: λ2 = {GREY_CLASS_VALUES[1]:.1f}")
print(f"  中: λ3 = {GREY_CLASS_VALUES[2]:.1f}")
print(f"  良: λ4 = {GREY_CLASS_VALUES[3]:.1f}")
print(f"  优: λ5 = {GREY_CLASS_VALUES[4]:.1f}")
print("="*80)

# ==================== 绘图部分 ====================
x = np.linspace(-2, 12, 700)
y1 = np.array([whitening_function_1(xi) for xi in x])
y2 = np.array([whitening_function_2(xi) for xi in x])
y3 = np.array([whitening_function_3(xi) for xi in x])
y4 = np.array([whitening_function_4(xi) for xi in x])
y5 = np.array([whitening_function_5(xi) for xi in x])

plt.figure(figsize=(16, 9))

# 颜色和名称定义
colors = ['purple', 'orange', 'blue', 'green', 'red']
class_names_en = ['Poor', 'Bad', 'Medium', 'Good', 'Excellent']
class_names_cn = ['劣', '差', '中', '良', '优']
lambda_values = [f'{v:.1f}' for v in GREY_CLASS_VALUES]

# 绘制函数曲线
plt.plot(x, y1, color=colors[0], linewidth=2.5, 
         label=f'Class 1 ({class_names_en[0]}) [-2,2] λ={lambda_values[0]}')
plt.plot(x, y2, color=colors[1], linewidth=2.5, 
         label=f'Class 2 ({class_names_en[1]}) [0,4] λ={lambda_values[1]}')
plt.plot(x, y3, color=colors[2], linewidth=2.5, 
         label=f'Class 3 ({class_names_en[2]}) [2,6] λ={lambda_values[2]}')
plt.plot(x, y4, color=colors[3], linewidth=2.5, 
         label=f'Class 4 ({class_names_en[3]}) [4,8] λ={lambda_values[3]}')
plt.plot(x, y5, color=colors[4], linewidth=2.5, 
         label=f'Class 5 ({class_names_en[4]}) [6,10] λ={lambda_values[4]}')

# 标注端点
for i in range(8):
    plt.axvline(a[i], color='gray', linestyle=':', alpha=0.5, linewidth=1)
    plt.text(a[i], -0.08, f'a{i}={a[i]}', ha='center', fontsize=9, color='gray')

# 标注中心点和平台
for k in [1, 2, 3, 4, 5]:
    params = calculate_grey_params(k)
    color = colors[k-1]
    
    # 标注λ_k
    plt.scatter([params['lambda_k']], [1], color=color, s=120, zorder=5, 
                marker='o', edgecolors='black', linewidths=1.5)
    plt.axvline(params['lambda_k'], color=color, linestyle='--', alpha=0.3, linewidth=1)
    
    # 添加λ标注
    plt.text(params['lambda_k'], 1.03, f'λ={params["lambda_k"]:.1f}', 
             ha='center', fontsize=9, fontweight='bold', color=color)
    
    # 标注平台区域
    plt.axvspan(params['c_k'], params['d_k'], alpha=0.08, color=color)
    
    # 添加平台区域标注
    platform_center = (params['c_k'] + params['d_k']) / 2
    plt.text(platform_center, 0.92, f'平台\n[{params["c_k"]:.2f},{params["d_k"]:.2f}]', 
             ha='center', fontsize=8, color=color, 
             bbox=dict(boxstyle='round,pad=0.2', facecolor='white', alpha=0.7, edgecolor=color))

plt.title('端点梯形白化权函数 (End-point Trapezoidal Whitenization Functions)', fontsize=15, fontweight='bold')
plt.xlabel('指标得分 (Index Score x)', fontsize=13)
plt.ylabel('白化权函数值 (Whitenization Value)', fontsize=13)
plt.grid(alpha=0.25, linestyle='--')
plt.legend(loc='upper left', fontsize=10, framealpha=0.9)
plt.ylim(-0.2, 1.15)
plt.xlim(-2, 12)

# 添加函数公式说明
formula_text = 'f(x) = 0 (x < a_{k-1} or x > a_{k+2})\n' \
               'f(x) = (x - a_{k-1})/(c_k - a_{k-1}) (a_{k-1} ≤ x < c_k)\n' \
               'f(x) = 1 (c_k ≤ x ≤ d_k)\n' \
               'f(x) = (a_{k+2} - x)/(a_{k+2} - d_k) (d_k < x ≤ a_{k+2})'
plt.text(11.5, 0.4, formula_text, fontsize=10, 
         bbox=dict(boxstyle='round,pad=0.5', facecolor='lightyellow', alpha=0.8),
         verticalalignment='center', horizontalalignment='right')

# 添加重叠区域说明
plt.text(1, 0.2, '重叠区域\n[0,2]', ha='center', fontsize=9, 
         bbox=dict(boxstyle='round,pad=0.3', facecolor='gray', alpha=0.1))
plt.text(3, 0.2, '重叠区域\n[2,4]', ha='center', fontsize=9,
         bbox=dict(boxstyle='round,pad=0.3', facecolor='gray', alpha=0.1))
plt.text(5, 0.2, '重叠区域\n[4,6]', ha='center', fontsize=9,
         bbox=dict(boxstyle='round,pad=0.3', facecolor='gray', alpha=0.1))
plt.text(7, 0.2, '重叠区域\n[6,8]', ha='center', fontsize=9,
         bbox=dict(boxstyle='round,pad=0.3', facecolor='gray', alpha=0.1))

plt.tight_layout()
plt.show()

# ==================== 测试验证 ====================
print("\n白化权函数值测试 (归一化结果):")
print("="*80)
test_values = [-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
print(f"{'x':<6} {'Poor':<10} {'Bad':<10} {'Medium':<10} {'Good':<10} {'Excellent':<10} {'AuxScore':<10}")
print("-"*80)

for val in test_values:
    f_values = [f(val) for f in WHITENING_FUNCTIONS]
    total = sum(f_values)
    
    if total > 0:
        f_norm = [f / total for f in f_values]
    else:
        f_norm = [0.2] * 5
    
    aux_score = sum(f_norm[i] * GREY_CLASS_VALUES[i] for i in range(5))
    
    print(f"{val:<6.1f} {f_norm[0]:<10.3f} {f_norm[1]:<10.3f} {f_norm[2]:<10.3f} "
          f"{f_norm[3]:<10.3f} {f_norm[4]:<10.3f} {aux_score:<10.2f}")

print("="*80)

# ==================== 汇总表格 ====================
print("\n灰类参数汇总表:")
print("="*100)
print(f"{'灰类':<6} {'Class':<10} {'k':<4} {'区间':<14} {'a_k':<6} {'a_{k+1}':<8} {'λ值':<6} {'平台区域':<16} {'上升段长':<8} {'下降段长':<8}")
print("-"*100)
for k in [1, 2, 3, 4, 5]:
    params = calculate_grey_params(k)
    interval = f"[{params['a_k_minus_1']},{params['a_k_plus_2']}]"
    platform = f"[{params['c_k']:.2f},{params['d_k']:.2f}]"
    class_name_en = class_names_en[k-1]
    
    # 计算上升段和下降段长度
    rise_length = params['c_k'] - params['a_k_minus_1']
    fall_length = params['a_k_plus_2'] - params['d_k']
    
    print(f"{grey_names[k]:<6} {class_name_en:<10} {k:<4} {interval:<14} {params['a_k']:<6} "
          f"{params['a_k_plus_1']:<8} {params['lambda_k']:<6.1f} {platform:<16} {rise_length:<8.2f} {fall_length:<8.2f}")
print("="*100)

# ==================== 对称性验证 ====================
print("\n等腰梯形对称性验证:")
print("-"*100)
for k in [1, 2, 3, 4, 5]:
    params = calculate_grey_params(k)
    rise_length = params['c_k'] - params['a_k_minus_1']
    fall_length = params['a_k_plus_2'] - params['d_k']
    is_isosceles = abs(rise_length - fall_length) < 0.001
    
    print(f"灰类{k} ({grey_names[k]}): 上升段={rise_length:.2f}, 下降段={fall_length:.2f} - " + 
          f"{'✓ 等腰梯形' if is_isosceles else '✗ 非等腰'}")

# ==================== 重叠区域分析 ====================
print("\n" + "="*100)
print("重叠区域分析:")
print("-"*100)
overlaps = [
    ("劣与差 (Poor-Bad)", [0, 2]),
    ("差与中 (Bad-Medium)", [2, 4]),
    ("中与良 (Medium-Good)", [4, 6]),
    ("良与优 (Good-Excellent)", [6, 8]),
]
for name, region in overlaps:
    print(f"{name}: {region}")
print("="*100)

print("\n✅ 程序执行完成！")
print("📊 图形已显示，梯形白化权函数（带平台区域）")
