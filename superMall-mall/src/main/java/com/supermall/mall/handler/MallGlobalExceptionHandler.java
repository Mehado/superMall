package com.supermall.mall.handler;

import com.supermall.exception.BaseException;
import com.supermall.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 电商板块全局异常处理。
 *
 * <p>和外卖的 GlobalExceptionHandler 逻辑一致，但必须是【独立的类】：
 * 两个服务是两个 Spring 容器，各自 @RestControllerAdvice；
 * 而且如果哪天电商要定制提示（比如返回给后台的错误更详细），不会影响外卖。
 */
@RestControllerAdvice
@Slf4j
public class MallGlobalExceptionHandler {

    /** multipart 单文件上限（用于给用户一个准确的提示） */
    @Value("${spring.servlet.multipart.max-file-size:5MB}")
    private String maxFileSize;

    /** 业务异常（BaseException 及其子类：验证码、账号、密码…） */
    @ExceptionHandler(BaseException.class)
    public Result<Void> handleBusiness(BaseException ex) {
        log.error("[mall] 业务异常：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /** 唯一键冲突 */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result<Void> handleDuplicateKey(SQLIntegrityConstraintViolationException ex) {
        String message = ex.getMessage();
        if (message != null && message.contains("Duplicate entry")) {
            return Result.error("数据已存在，请勿重复提交");
        }
        return Result.error("数据约束异常，请检查输入");
    }

    /** 参数校验失败（@RequestBody 对象） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("参数校验失败");
        log.error("[mall] 参数校验失败：{}", message);
        return Result.error(message);
    }

    /** 缺必填请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException ex) {
        return Result.error("缺少必填参数：" + ex.getParameterName());
    }

    /** 缺上传文件字段（multipart 里没带 file/files） */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public Result<Void> handleMissingPart(MissingServletRequestPartException ex) {
        return Result.error("缺少上传的文件：" + ex.getRequestPartName());
    }

    /** 请求体格式错误或为空 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleUnreadable(HttpMessageNotReadableException ex) {
        log.error("[mall] 请求体解析失败：{}", ex.getMessage());
        return Result.error("请求体格式错误或为空");
    }

    /** 上传文件超过 multipart 上限（Spring 在进 Controller 前就抛了，得在这里转成友好提示） */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        log.error("[mall] 上传文件过大：{}", ex.getMessage());
        return Result.error("文件大小超过限制，单个文件最大 " + maxFileSize);
    }

    /** 兜底 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception ex) {
        log.error("[mall] 未知错误:", ex);
        return Result.error("系统异常，请稍后再试");
    }
}
