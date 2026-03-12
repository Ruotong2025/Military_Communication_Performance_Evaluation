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

        # 当前库里该表字段名已是 batch_id（无需再 rename 列）
        old_batch_id = "AHP-EOP-20260312-8051"
        new_batch_id = "AHP-2026-002"

        cur.execute("SHOW COLUMNS FROM ahp_equipment_operation_weights")
        cols = [r[0] for r in cur.fetchall()]
        weight_cols = [c for c in cols if c.endswith("_weight")]

        # 将 0~1 的权重映射到 1~9：new = 1 + old * 8
        set_parts = ["batch_id=%s"] + [
            f"`{c}`=ROUND(1+`{c}`*8,4)" for c in weight_cols
        ]
        sql = (
            "UPDATE ahp_equipment_operation_weights SET "
            + ", ".join(set_parts)
            + " WHERE batch_id=%s"
        )
        cur.execute(sql, (new_batch_id, old_batch_id))
        affected = cur.rowcount
        conn.commit()

        cur.execute(
            "SELECT batch_id, COUNT(*) cnt, "
            "MIN(personnel_dim_weight) min_w, MAX(personnel_dim_weight) max_w "
            "FROM ahp_equipment_operation_weights GROUP BY batch_id"
        )
        print(cur.fetchall())
        print("affected_rows:", affected)
    finally:
        conn.close()


if __name__ == "__main__":
    main()

