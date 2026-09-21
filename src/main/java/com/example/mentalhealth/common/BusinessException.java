package com.example.mentalhealth.common;

/**
 * 业务异常。抛出后由 GlobalExceptionHandler 统一转成 Result，不再产生 Whitelabel 错误页。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(ResultCode.ERROR, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
