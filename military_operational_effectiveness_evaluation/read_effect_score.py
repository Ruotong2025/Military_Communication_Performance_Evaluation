"""
从本地MySQL数据库读取 military_operation_effect_score 表结构
"""

import mysql.connector
from mysql.connector import Error


def read_effect_score_structure():
    """读取 military_operation_effect_score 表结构"""
    try:
        connection = mysql.connector.connect(
            host='localhost',
            database='military_operational_effectiveness_evaluation',
            user='root',
            password='root'
        )
        
        if connection.is_connected():
            print("成功连接到数据库!")
            
            cursor = connection.cursor(dictionary=True)
            
            # 查看表是否存在
            cursor.execute("SHOW TABLES LIKE 'military_operation_effect_score'")
            if not cursor.fetchone():
                print("表 military_operation_effect_score 不存在!")
                return None
            
            # 查看表结构
            print("\n=== military_operation_effect_score 表结构 ===")
            cursor.execute("DESCRIBE military_operation_effect_score")
            columns = cursor.fetchall()
            for col in columns:
                print(f"  {col['Field']}: {col['Type']} (Null: {col['Null']}, Key: {col['Key']})")
            
            # 读取所有数据
            print("\n=== military_operation_effect_score 表数据 ===")
            cursor.execute("SELECT * FROM military_operation_effect_score")
            rows = cursor.fetchall()
            
            print(f"共读取 {len(rows)} 条记录")
            
            if rows:
                print("\n前3条记录示例:")
                for i, row in enumerate(rows[:3], 1):
                    print(f"\n记录 {i}:")
                    for key, value in row.items():
                        print(f"  {key}: {value}")
            
            return columns
            
    except Error as e:
        print(f"数据库错误: {e}")
        return None
    finally:
        if connection and connection.is_connected():
            connection.close()
            print("\n数据库连接已关闭")


if __name__ == "__main__":
    read_effect_score_structure()
