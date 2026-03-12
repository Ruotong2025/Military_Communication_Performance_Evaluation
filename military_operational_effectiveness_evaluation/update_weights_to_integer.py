import pymysql
import random


def main() -> None:
    conn = pymysql.connect(
        host="localhost",
        user="root",
        password="root",
        database="military_operational_effectiveness_evaluation",
        charset="utf8mb4",
    )
    try:
        cur = conn.cursor()

        # 获取所有 weight 列名
        cur.execute("SHOW COLUMNS FROM ahp_equipment_operation_weights")
        cols = [r[0] for r in cur.fetchall()]
        weight_cols = [c for c in cols if c.endswith("_weight")]

        print(f"Found {len(weight_cols)} weight columns")

        # 为每条记录重新生成 1~10 的随机整数权重
        cur.execute("SELECT id FROM ahp_equipment_operation_weights WHERE batch_id = 'AHP-2026-002'")
        record_ids = [r[0] for r in cur.fetchall()]

        for record_id in record_ids:
            # 为每个 weight 字段生成 1~10 的随机整数
            for wc in weight_cols:
                new_val = random.randint(1, 10)
                cur.execute(
                    f"UPDATE ahp_equipment_operation_weights SET `{wc}` = %s WHERE id = %s",
                    (new_val, record_id)
                )

        conn.commit()

        # 验证结果
        cur.execute(
            "SELECT batch_id, COUNT(*) cnt, "
            "MIN(personnel_dim_weight) min_w, MAX(personnel_dim_weight) max_w "
            "FROM ahp_equipment_operation_weights GROUP BY batch_id"
        )
        print("Result:", cur.fetchall())

    finally:
        conn.close()


if __name__ == "__main__":
    main()
