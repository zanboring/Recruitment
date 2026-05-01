package com.example.recruitment.exception;

import com.example.recruitment.common.Result;
import com.example.recruitment.common.ResultCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.failed(ResultCode.FAILED, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null
                ? fieldError.getDefaultMessage()
                : "参数校验失败";
        return Result.validateFailed(msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        return Result.validateFailed(e.getMessage());
    }

    /**
     * 请求体解析失败（含 UTF-8 乱码、JSON格式错误等）
     * 单独处理，方便定位请求编码问题
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("请求体解析失败（可能是编码问题或JSON格式错误）: {}", e.getMessage());
        String msg = e.getMessage();
        if (msg != null && msg.contains("UTF-8")) {
            return Result.failed(ResultCode.VALIDATE_FAILED, "请求体编码错误，请确保请求头包含 Content-Type: application/json;charset=UTF-8");
        }
        return Result.failed(ResultCode.VALIDATE_FAILED, "请求体格式错误：" + e.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("服务器未预期异常", e);
        return Result.failed(ResultCode.SERVER_ERROR, "服务器异常");
    }
}

