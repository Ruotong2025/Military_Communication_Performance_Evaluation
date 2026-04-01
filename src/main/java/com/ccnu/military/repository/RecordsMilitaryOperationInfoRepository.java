package com.ccnu.military.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 作战基础信息 Repository
 * <p>使用 JdbcTemplate 进行原生 SQL 查询，返回 Map 类型数据
 */
@Repository
@RequiredArgsConstructor
public class RecordsMilitaryOperationInfoRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 根据作战ID查询原始数据（按 operation_id 字段查询）
     *
     * @param operationId 作战ID（operation_id 字段，如 20260001）
     * @return 包含作战基础信息的 Map
     */
    public Optional<Map<String, Object>> findRawDataByOperationId(int operationId) {
        String sql = "SELECT " +
                "id, operation_id, " +
                "command_personnel_count, operator_personnel_count, maintenance_personnel_count, " +
                "avg_experience_years, annual_maintenance_hours, avg_training_frequency_per_year, " +
                "total_equipment_count, damaged_equipment_count, new_equipment_ratio, " +
                "total_power_consumption_kw, annual_electricity_consumption_kwh, annual_fuel_consumption_liters, " +
                "spectrum_reserve_mhz, spare_parts_satisfaction_rate, total_transport_distance_km, " +
                "avg_altitude_m, temperature_celsius, electromagnetic_interference_level " +
                "FROM records_military_operation_info WHERE operation_id = ?";

        return jdbcTemplate.queryForList(sql, operationId)
                .stream()
                .findFirst()
                .map(row -> {
                    Map<String, Object> result = new HashMap<>();
                    result.putAll(row);
                    return result;
                });
    }

    /**
     * 根据作战ID列表批量查询数据
     *
     * @param operationIds 作战ID列表（对应 operation_id 字段）
     * @return 作战ID(int)到数据的映射
     */
    public Map<Integer, Map<String, Object>> findByOperationIds(List<Integer> operationIds) {
        if (operationIds == null || operationIds.isEmpty()) {
            return new HashMap<>();
        }
        String placeholders = String.join(",", operationIds.stream().map(id -> "?").toArray(String[]::new));
        String sql = "SELECT " +
                "id, operation_id, " +
                "command_personnel_count, operator_personnel_count, maintenance_personnel_count, " +
                "avg_experience_years, annual_maintenance_hours, avg_training_frequency_per_year, " +
                "total_equipment_count, damaged_equipment_count, new_equipment_ratio, " +
                "total_power_consumption_kw, annual_electricity_consumption_kwh, annual_fuel_consumption_liters, " +
                "spectrum_reserve_mhz, spare_parts_satisfaction_rate, total_transport_distance_km, " +
                "avg_altitude_m, temperature_celsius, electromagnetic_interference_level " +
                "FROM records_military_operation_info WHERE operation_id IN (" + placeholders + ")";

        Map<Integer, Map<String, Object>> results = new HashMap<>();
        jdbcTemplate.queryForList(sql, operationIds.toArray())
                .forEach(row -> {
                    Map<String, Object> data = new HashMap<>();
                    data.putAll(row);
                    results.put((Integer) row.get("operation_id"), data);
                });

        return results;
    }
}
