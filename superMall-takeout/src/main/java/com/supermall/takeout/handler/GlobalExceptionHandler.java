package com.supermall.takeout.handler;

import com.supermall.exception.BaseException;
import com.supermall.result.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** multipart 单文件上限（用于给用户一个准确的提示） */
    @Value("${spring.servlet.multipart.max-file-size:5MB}")
    private String maxFileSize;

    /** 业务异常 */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex) {
        log.error("业务异常：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /** 唯一键约束冲突 */
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex) {
        String message = ex.getMessage();
        if (message != null && message.contains("Duplicate entry")) {
            String[] parts = message.split(" ");
            String key = parts.length > 2 ? parts[2] : "";
            return Result.error(key + "已存在");
        }
        return Result.error("数据约束异常，请检查输入");
    }

    /** 参数校验失败（@RequestBody 绑定对象） */
    @ExceptionHandler
    public Result exceptionHandler(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("参数校验失败");
        log.error("参数校验失败：{}", message);
        return Result.error(message);
    }

    /** 参数校验失败（方法参数 / 查询参数） */
    @ExceptionHandler
    public Result exceptionHandler(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("参数校验失败");
        log.error("参数校验失败：{}", message);
        return Result.error(message);
    }

    /** 缺少必填请求参数 */
    @ExceptionHandler
    public Result exceptionHandler(MissingServletRequestParameterException ex) {
        return Result.error("缺少必填参数：" + ex.getParameterName());
    }

    /** 请求体格式错误或为空 */
    @ExceptionHandler
    public Result exceptionHandler(HttpMessageNotReadableException ex) {
        log.error("请求体解析失败：{}", ex.getMessage());
        return Result.error("请求体格式错误或为空");
    }

    /** 文件上传过大 */
    @ExceptionHandler
    public Result exceptionHandler(MaxUploadSizeExceededException ex) {
        log.error("文件上传失败：{}", ex.getMessage());
        return Result.error("文件大小超过限制，单个文件最大 " + maxFileSize);
    }

    /** 兜底 */
    @ExceptionHandler(Exception.class)
    public Result exceptionHandler(Exception ex) {
        log.error("未知错误:", ex);
        return Result.error("系统异常，请稍后再试");
    }
}
