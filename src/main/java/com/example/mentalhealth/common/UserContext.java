package com.example.mentalhealth.common;

/**
 * 当前登录用户。由 AuthInterceptor 在请求进入时写入、请求结束时清理。
 * 业务代码通过 {@link #get()} 取 userId，不必再解析 token。
 */
public final class UserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Long get() {
        return CURRENT_USER_ID.get();
    }

    /** 取当前用户 id，取不到直接抛 401（正常流程下拦截器已保证有值） */
    public static Long require() {
        Long userId = CURRENT_USER_ID.get();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }
        return userId;
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
