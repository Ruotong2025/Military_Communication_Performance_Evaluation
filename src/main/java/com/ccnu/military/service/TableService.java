package com.ccnu.military.service;

import com.ccnu.military.dto.ColumnInfo;
import com.ccnu.military.dto.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Arrays;
import java.util.HashSet;

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
        "military_effectiveness_evaluation"
    ));

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
     * 获取表数据（分页）
     */
    public PageResult getTableData(String tableName, int page, int size) {
        validateTableName(tableName);
        
        // 计算偏移量
        int offset = (page - 1) * size;
        
        // 查询总记录数
        String countSql = "SELECT COUNT(*) FROM " + tableName;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class);
        
        // 查询数据
        String dataSql = String.format(
            "SELECT * FROM %s LIMIT %d OFFSET %d",
            tableName, size, offset
        );
        
        List<Map<String, Object>> records = jdbcTemplate.queryForList(dataSql);
        
        // 计算总页数
        int totalPages = (int) Math.ceil((double) total / size);
        
        return new PageResult(records, total, page, size, totalPages);
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
