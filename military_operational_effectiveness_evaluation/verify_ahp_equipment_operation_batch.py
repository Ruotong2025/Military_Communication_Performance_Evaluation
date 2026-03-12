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
        cur.execute(
            "SELECT batch_id, COUNT(*) cnt, "
            "MIN(personnel_dim_weight) min_w, MAX(personnel_dim_weight) max_w "
            "FROM ahp_equipment_operation_weights GROUP BY batch_id"
        )
        print(cur.fetchall())
    finally:
        conn.close()


if __name__ == "__main__":
    main()

