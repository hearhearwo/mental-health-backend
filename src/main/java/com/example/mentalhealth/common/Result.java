package com.example.mentalhealth.common;

import lombok.Data;

/**
 * 统一响应体，格式与前端约定一致：{ "code": 0, "data": ..., "message": "ok" }
 * code 为 0 表示成功，非 0 表示业务错误（见 ResultCode）。
 */
@Data
public class Result<T> {

    private Integer code;
    private T data;
    private String message;

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS);
        result.setData(data);
        result.setMessage("ok");
        return result;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setData(null);
        result.setMessage(message);
        return result;
    }
}
