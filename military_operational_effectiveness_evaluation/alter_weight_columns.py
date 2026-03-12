import pymysql


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

        # 修改每个 weight 列的类型为 decimal(5,0) 以存储整数
        for wc in weight_cols:
            cur.execute(
                f"ALTER TABLE ahp_equipment_operation_weights MODIFY COLUMN `{wc}` DECIMAL(5,0)"
            )
            print(f"Modified {wc} to DECIMAL(5,0)")

        conn.commit()
        print("\nAll weight columns modified to DECIMAL(5,0)")

    finally:
        conn.close()


if __name__ == "__main__":
    main()
