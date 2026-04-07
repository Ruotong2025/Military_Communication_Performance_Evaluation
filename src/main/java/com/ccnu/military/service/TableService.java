package com.ccnu.military.service;

import com.ccnu.military.dto.ColumnInfo;
import com.ccnu.military.dto.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * 动态表查询服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableService {

    private final JdbcTemplate jdbcTemplate;
    
    // 允许查询的表白名单
    private static final Set<String> ALLOWED_TABLES = new HashSet<>(Arrays.asList(
        "communication_network_lifecycle",
        "during_battle_communications",
        "military_effectiveness_evaluation",
        "records_military_operation_info",
        "records_military_communication_info",
        "records_link_maintenance_events",
        "records_security_events",
        "records_comm_attack_operation",
        "records_comm_defense_operation",
        "equipment_qt_indicator_def",
        "equipment_ql_indicator_def",
        "equipment_qt_evaluation_record",
        "equipment_ql_evaluation_record",
        "expert_ahp_comparison_score",
        "expert_ahp_individual_weights"
    ));

    // 业务要求：基础表不允许按行删除
    private static final String BASE_TABLE = "records_military_operation_info";

    /** 支持按 operation_id 筛选的模拟表 */
    private static final Set<String> OPERATION_FILTERABLE_TABLES = new HashSet<>(Arrays.asList(
            "records_military_operation_info",
            "records_military_communication_info",
            "records_link_maintenance_events",
            "records_security_events",
            "records_comm_attack_operation",
            "records_comm_defense_operation"
    ));

    /**
     * 作战基础信息表中出现的作战 ID（降序），供下拉筛选
     */
    public List<Long> listDistinctOperationIds() {
        String sql = "SELECT DISTINCT operation_id FROM records_military_operation_info "
                + "WHERE operation_id IS NOT NULL ORDER BY operation_id DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("operation_id"));
    }

    /**
     * 获取表结构信息
     */
    public List<ColumnInfo> getTableStructure(String tableName) {
        validateTableName(tableName);
        
        String sql = "SELECT " +
                "COLUMN_NAME as columnName, " +
                "DATA_TYPE as dataType, " +
                "COLUMN_COMMENT as columnComment, " +
                "CHARACTER_MAXIMUM_LENGTH as columnLength, " +
                "IS_NULLABLE as nullable, " +
                "COLUMN_KEY as columnKey " +
                "FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "AND TABLE_NAME = ? " +
                "ORDER BY ORDINAL_POSITION";
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ColumnInfo info = new ColumnInfo();
            info.setColumnName(rs.getString("columnName"));
            info.setDataType(rs.getString("dataType"));
            info.setColumnComment(rs.getString("columnComment"));
            info.setColumnLength(rs.getInt("columnLength"));
            info.setNullable("YES".equals(rs.getString("nullable")));
            info.setColumnKey(rs.getString("columnKey"));
            return info;
        }, tableName);
    }

    /**
     * 获取表数据（分页）；可选按作战 ID（operation_id）筛选（仅白名单内模拟表）
     */
    public PageResult getTableData(String tableName, int page, int size, String operationId) {
        validateTableName(tableName);

        int offset = (page - 1) * size;
        boolean filter = StringUtils.hasText(operationId);
        if (filter && !OPERATION_FILTERABLE_TABLES.contains(tableName)) {
            throw new IllegalArgumentException("该表不支持按作战ID筛选: " + tableName);
        }

        Object opParam = null;
        if (filter) {
            String trimmed = operationId.trim();
            try {
                opParam = Long.parseLong(trimmed);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("作战ID须为数字: " + trimmed);
            }
        }

        String countSql = "SELECT COUNT(*) FROM `" + tableName + "`";
        String dataSql = "SELECT * FROM `" + tableName + "`";
        Long total;
        List<Map<String, Object>> records;

        if (filter) {
            countSql += " WHERE operation_id = ?";
            dataSql += " WHERE operation_id = ? LIMIT ? OFFSET ?";
            total = jdbcTemplate.queryForObject(countSql, Long.class, opParam);
            records = jdbcTemplate.queryForList(dataSql, opParam, size, offset);
        } else {
            dataSql += " LIMIT ? OFFSET ?";
            total = jdbcTemplate.queryForObject(countSql, Long.class);
            records = jdbcTemplate.queryForList(dataSql, size, offset);
        }

        int totalPages = total == null || total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResult(records, total != null ? total : 0L, page, size, totalPages);
    }

    /**
     * 按主键删除一行（仅允许非基础 records 表）
     */
    public int deleteTableRow(String tableName, Map<String, Object> row) {
        validateTableName(tableName);
        if (BASE_TABLE.equals(tableName)) {
            throw new IllegalArgumentException("基础表不支持删除行: " + tableName);
        }
        if (row == null || row.isEmpty()) {
            throw new IllegalArgumentException("删除失败：缺少行数据");
        }

        String pkSql = "SELECT COLUMN_NAME FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_KEY = 'PRI' " +
                "ORDER BY ORDINAL_POSITION";
        List<String> pkColumns = jdbcTemplate.queryForList(pkSql, String.class, tableName);
        if (pkColumns == null || pkColumns.isEmpty()) {
            throw new IllegalArgumentException("删除失败：表未定义主键，禁止删除");
        }

        for (String pk : pkColumns) {
            if (!row.containsKey(pk) || row.get(pk) == null) {
                throw new IllegalArgumentException("删除失败：缺少主键字段 " + pk);
            }
        }

        String whereClause = pkColumns.stream()
                .map(pk -> "`" + pk + "` = ?")
                .collect(Collectors.joining(" AND "));
        Object[] args = pkColumns.stream().map(row::get).toArray();
        String sql = "DELETE FROM `" + tableName + "` WHERE " + whereClause + " LIMIT 1";
        return jdbcTemplate.update(sql, args);
    }

    /**
     * 验证表名（防止SQL注入）
     */
    private void validateTableName(String tableName) {
        if (!ALLOWED_TABLES.contains(tableName)) {
            throw new IllegalArgumentException("不允许查询的表: " + tableName);
        }
    }

    /**
     * 获取所有允许查询的表
     */
    public Set<String> getAllowedTables() {
        return ALLOWED_TABLES;
    }
}
