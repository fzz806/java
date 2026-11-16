package com.falsegod.config;

import com.falsegod.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：把 Spring 的默认错误 JSON（timestamp/status/error/path）
 * 统一成项目约定的 {@code { code, message, data }} 结构。
 *
 * <p>为什么需要：前端 api/*.js 只认 `code === 0` 为成功，
 * 其余一律取 `message` 弹提示。若不统一，前端拿到的是
 * `{"timestamp":...}` 这种没有 message 的对象，提示会变成空字符串。
 *
 * <p>注意：401 由 {@link AuthInterceptor} 直接写响应，不经过这里。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 路径不存在（Spring 6.1 由静态资源处理器抛出） */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(NoResourceFoundException e) {
        return Result.error(404, "神堂无此通路：" + e.getResourcePath());
    }

    /** 请求方法不支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return Result.error(405, "神堂不纳此式（" + e.getMethod() + "），请换个请求方式");
    }

    /** 请求体缺失或 JSON 格式错误 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBadBody(HttpMessageNotReadableException e) {
        return Result.error(400, "请求内容无法解析，请检查 JSON 格式");
    }

    /** 缺少必填参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error(400, "缺少必要参数：" + e.getParameterName());
    }

    /** 参数类型不匹配 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.error(400, "参数类型不符：" + e.getName());
    }

    /** 兜底：未预期的异常。内部细节只写日志，不回传给前端 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleAnything(Exception e) {
        log.error("未处理异常: {}", e.getMessage(), e);
        return Result.error(500, "神堂内部异动，请查看后端日志");
    }
}
