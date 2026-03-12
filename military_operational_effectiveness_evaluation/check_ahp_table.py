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

# 查看ahp_equipment_operation_weights表结构
cursor.execute("DESCRIBE ahp_equipment_operation_weights")
print('=== ahp_equipment_operation_weights 表结构 ===')
cols = []
for col in cursor.fetchall():
    cols.append(col[0])
    print(col[0], col[1])

conn.close()
