"""
从本地MySQL数据库读取 cost_evaluation 表数据
"""

import mysql.connector
from mysql.connector import Error


def read_cost_evaluation_from_db():
    """从MySQL数据库读取 cost_evaluation 表"""
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
            
            # 查看表结构
            print("\n=== cost_evaluation 表结构 ===")
            cursor.execute("DESCRIBE cost_evaluation")
            columns = cursor.fetchall()
            for col in columns:
                print(f"  {col['Field']}: {col['Type']} (Null: {col['Null']}, Key: {col['Key']})")
            
            # 读取所有数据
            print("\n=== cost_evaluation 表数据 ===")
            cursor.execute("SELECT * FROM cost_evaluation")
            rows = cursor.fetchall()
            
            print(f"共读取 {len(rows)} 条记录")
            print()
            
            for i, row in enumerate(rows, 1):
                print(f"记录 {i}:")
                for key, value in row.items():
                    print(f"  {key}: {value}")
                print()
            
            return rows
            
    except Error as e:
        print(f"数据库错误: {e}")
        return None
    finally:
        if connection and connection.is_connected():
            connection.close()
            print("\n数据库连接已关闭")


if __name__ == "__main__":
    read_cost_evaluation_from_db()
