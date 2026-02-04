package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 分页结果DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult {
    private List<Map<String, Object>> records;  // 数据记录
    private Long total;                         // 总记录数
    private Integer page;                       // 当前页
    private Integer size;                       // 每页大小
    private Integer totalPages;                 // 总页数
}
