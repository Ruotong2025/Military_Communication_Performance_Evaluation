package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 统一捕获并处理所有异常，返回友好的错误信息给前端
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 IllegalArgumentException（业务参数校验异常）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("业务参数错误: {}", e.getMessage());
        return ApiResponse.error(400, e.getMessage());
    }

    /**
     * 处理 IllegalStateException（业务状态异常）
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalState(IllegalStateException e) {
        log.warn("业务状态错误: {}", e.getMessage());
        return ApiResponse.error(400, e.getMessage());
    }

    /**
     * 处理数据库访问异常
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleDataAccess(DataAccessException e) {
        log.error("数据库访问异常: {}", e.getMessage(), e);
        String message = e.getMessage();
        if (message != null && message.length() > 100) {
            message = message.substring(0, 100) + "...";
        }
        return ApiResponse.error(500, "数据库操作失败: " + message);
    }

    /**
     * 处理运行时异常（兜底）
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: {}", e.getMessage(), e);
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = "系统内部错误";
        }
        return ApiResponse.error(500, message);
    }

    /**
     * 处理所有其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGenericException(Exception e) {
        log.error("未处理的异常: {}", e.getMessage(), e);
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = "系统发生未知错误，请联系管理员";
        }
        return ApiResponse.error(500, message);
    }
}
