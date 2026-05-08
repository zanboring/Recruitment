package com.example.recruitment.exception;

import com.example.recruitment.common.Result;
import com.example.recruitment.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 优化要点：
 * 1. 增强日志记录，包含请求信息
 * 2. 添加更多异常类型处理
 * 3. 统一错误响应格式
 * 4. 提供详细的错误信息便于排查
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("[业务异常] {} {} - {}", 
            request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.failed(ResultCode.FAILED, e.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid注解）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        
        log.warn("[参数校验失败] {} {} - {}", 
            request.getMethod(), request.getRequestURI(), errors);
        
        return Result.validateFailed(errors);
    }

    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(
            ConstraintViolationException e, HttpServletRequest request) {
        
        String errors = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        
        log.warn("[约束违反] {} {} - {}", 
            request.getMethod(), request.getRequestURI(), errors);
        
        return Result.validateFailed(errors);
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        
        String msg = String.format("缺少必需参数: %s (%s)", e.getParameterName(), e.getParameterType());
        log.warn("[缺少参数] {} {} - {}", 
            request.getMethod(), request.getRequestURI(), msg);
        
        return Result.validateFailed(msg);
    }

    /**
     * 处理参数类型转换异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        
        String msg = String.format("参数类型错误: %s 期望类型: %s", 
            e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown");
        log.warn("[参数类型错误] {} {} - {}", 
            request.getMethod(), request.getRequestURI(), msg);
        
        return Result.validateFailed(msg);
    }

    /**
     * 请求体解析失败（含 UTF-8 乱码、JSON格式错误等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        
        String msg = e.getMessage();
        String detailMsg;
        
        if (msg != null && msg.contains("UTF-8")) {
            detailMsg = "请求体编码错误，请确保请求头包含 Content-Type: application/json;charset=UTF-8";
        } else if (msg != null && msg.contains("JSON parse error")) {
            detailMsg = "JSON格式错误: " + e.getMostSpecificCause().getMessage();
        } else {
            detailMsg = "请求体格式错误: " + e.getMostSpecificCause().getMessage();
        }
        
        log.warn("[请求体解析失败] {} {} - {}", 
            request.getMethod(), request.getRequestURI(), detailMsg);
        
        return Result.failed(ResultCode.VALIDATE_FAILED, detailMsg);
    }

    /**
     * 处理HTTP方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        
        String msg = String.format("不支持的HTTP方法: %s，支持的方法: %s", 
            e.getMethod(), e.getSupportedHttpMethods());
        log.warn("[HTTP方法不支持] {} {} - {}", 
            request.getMethod(), request.getRequestURI(), msg);
        
        return Result.failed(ResultCode.FAILED, msg);
    }

    /**
     * 处理媒体类型不支持异常
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        
        String msg = String.format("不支持的媒体类型: %s，支持的类型: %s", 
            e.getContentType(), e.getSupportedMediaTypes());
        log.warn("[媒体类型不支持] {} {} - {}", 
            request.getMethod(), request.getRequestURI(), msg);
        
        return Result.failed(ResultCode.FAILED, msg);
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNoHandlerFoundException(
            NoHandlerFoundException e, HttpServletRequest request) {
        
        String msg = String.format("请求路径不存在: %s %s", e.getHttpMethod(), e.getRequestURL());
        log.warn("[路径不存在] {} {} - {}", 
            request.getMethod(), request.getRequestURI(), msg);
        
        return Result.failed(ResultCode.NOT_FOUND, msg);
    }

    /**
     * 处理数据库访问异常
     */
    @ExceptionHandler(DataAccessException.class)
    public Result<Void> handleDataAccessException(
            DataAccessException e, HttpServletRequest request) {
        
        String msg = "数据库操作失败";
        log.error("[数据库异常] {} {} - {} - {}", 
            request.getMethod(), request.getRequestURI(), msg, e.getMessage());
        
        return Result.failed(ResultCode.SERVER_ERROR, msg);
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        // 记录详细日志
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String clientIp = getClientIp(request);
        
        log.error("""
            ============================================
            [服务器异常]
            时间: {}
            IP: {}
            方法: {}
            URL: {}
            参数: {}
            异常类型: {}
            异常信息: {}
            ============================================
            """, timestamp, clientIp, request.getMethod(), 
            request.getRequestURI(), request.getQueryString(),
            e.getClass().getName(), e.getMessage(), e);
        
        // 返回通用错误信息，不暴露内部细节
        return Result.failed(ResultCode.SERVER_ERROR, "服务器内部错误，请稍后重试");
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多个代理，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}