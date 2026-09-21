package com.example.mentalhealth.common;

/**
 * 业务状态码。与 HTTP 状态码数字一致仅为便于理解，
 * 实际都通过 HTTP 200 + 响应体里的 code 返回。
 */
public final class ResultCode {

    /** 成功 */
    public static final int SUCCESS = 0;
    /** 参数错误 */
    public static final int BAD_REQUEST = 400;
    /** 未登录 / token 失效，前端据此跳回登录页 */
    public static final int UNAUTHORIZED = 401;
    /** 资源不存在 */
    public static final int NOT_FOUND = 404;
    /** 服务器内部错误 */
    public static final int ERROR = 500;

    private ResultCode() {
    }
}
