import pymysql
import random

# 连接数据库
conn = pymysql.connect(
    host='localhost',
    user='root',
    password='root',
    database='military_operational_effectiveness_evaluation',
    charset='utf8mb4'
)
cursor = conn.cursor()

# 检查表是否存在
cursor.execute("SHOW TABLES LIKE 'ahp_equipment_operation_weights'")
result = cursor.fetchall()
print('ahp_equipment_operation_weights 表是否存在:', result)

cursor.execute("SHOW TABLES LIKE 'equipment_operation_score'")
result2 = cursor.fetchall()
print('equipment_operation_score 表是否存在:', result2)

# 查看ahp_expert_military_operation_effect_weights的结构作为参考
cursor.execute("DESCRIBE ahp_expert_military_operation_effect_weights")
print('\n=== 参考表 ahp_expert_military_operation_effect_weights 结构 ===')
cols = []
for col in cursor.fetchall():
    cols.append(col[0])
    print(col[0], col[1])

conn.close()
