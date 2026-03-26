# -*- coding: utf-8 -*-
"""
查询数据库表结构的脚本
"""
import mysql.connector
from mysql.connector import Error

def analyze_tables():
    """分析数据库表结构"""
    try:
        # 连接数据库
        connection = mysql.connector.connect(
            host='localhost',
            database='military_operational_effectiveness_evaluation',
            user='root',
            password='root'
        )

        if connection.is_connected():
            cursor = connection.cursor(dictionary=True)

            # 1. 查看所有表
            print("=" * 80)
            print("数据库中的所有表：")
            print("=" * 80)
            cursor.execute("SHOW TABLES")
            tables = cursor.fetchall()
            for t in tables:
                print(f"  - {list(t.values())[0]}")

            print()

            # 2. 查找相关表
            target_tables = [
                'metrics_military_comm_effect',
                'records_link_maintenance_events',
                'records_military_communication_info',
                'records_military_operation_info'
            ]

            for table_name in target_tables:
                try:
                    print("=" * 80)
                    print(f"表结构: {table_name}")
                    print("=" * 80)
                    cursor.execute(f"DESCRIBE {table_name}")
                    columns = cursor.fetchall()
                    for col in columns:
                        # 尝试获取注释（某些MySQL版本可能没有）
                        comment = ''
                        try:
                            comment = col.get('Comment', '')
                        except:
                            pass
                        print(f"  {col['Field']}: {col['Type']} ({col['Null']}, {col['Key']}) - {comment if comment else '无注释'}")

                    # 查询记录数
                    cursor.execute(f"SELECT COUNT(*) as cnt FROM {table_name}")
                    count = cursor.fetchone()
                    print(f"  >> 记录数: {count['cnt']}")

                    print()
                except Error as e:
                    print(f"  表 {table_name} 不存在: {e}")
                    print()

            # 3. 查看数据样本
            print("=" * 80)
            print("数据样本查询")
            print("=" * 80)

            # 查询各表的样本数据
            for table_name in target_tables:
                try:
                    print(f"\n[{table_name}] 样本数据:")
                    cursor.execute(f"SELECT * FROM {table_name} LIMIT 2")
                    rows = cursor.fetchall()
                    if rows:
                        for row in rows:
                            for key, value in row.items():
                                print(f"  {key}: {value}")
                            print("  ---")
                    else:
                        print("  (无数据)")
                except Error as e:
                    print(f"  表 {table_name} 不存在: {e}")

            cursor.close()
            connection.close()

    except Error as e:
        print(f"数据库连接错误: {e}")


if __name__ == "__main__":
    analyze_tables()
