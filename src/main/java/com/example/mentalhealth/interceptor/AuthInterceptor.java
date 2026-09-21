package com.example.mentalhealth.interceptor;

import com.example.mentalhealth.common.BusinessException;
import com.example.mentalhealth.common.ResultCode;
import com.example.mentalhealth.common.UserContext;
import com.example.mentalhealth.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 校验 Authorization: Bearer &lt;token&gt;，通过后把 userId 放进 UserContext。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 跨域预检请求不带 Authorization，直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        UserContext.set(jwtUtil.parseUserId(token));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 线程会被复用，必须清理，否则可能串到下一个请求
        UserContext.clear();
    }
}
