package com.example.mentalhealth.service;

import com.example.mentalhealth.common.BusinessException;
import com.example.mentalhealth.common.ResultCode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * 邮箱验证码的生成与校验。
 *
 * 用 Caffeine 内存缓存存放，到期自动淘汰，所以不需要为验证码建表。
 * 代价是服务重启后已发出的验证码会失效，用户重新获取即可。
 */
@Service
public class EmailCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Cache<String, CodeEntry> codes;
    private final long resendMillis;
    private final int maxAttempts;

    public EmailCodeService(@Value("${app.email-code.expire-minutes:5}") long expireMinutes,
                            @Value("${app.email-code.resend-seconds:60}") long resendSeconds,
                            @Value("${app.email-code.max-attempts:5}") int maxAttempts) {
        this.codes = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(expireMinutes))
                .maximumSize(10_000)
                .build();
        this.resendMillis = resendSeconds * 1000L;
        this.maxAttempts = maxAttempts;
    }

    /** 生成并缓存 6 位验证码。同一邮箱在冷却期内重复请求会直接拒绝 */
    public String generate(String email) {
        CodeEntry existing = codes.getIfPresent(email);
        if (existing != null) {
            long elapsed = System.currentTimeMillis() - existing.sentAt;
            if (elapsed < resendMillis) {
                long waitSeconds = (resendMillis - elapsed + 999) / 1000;
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "请求过于频繁，请 " + waitSeconds + " 秒后再试");
            }
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        codes.put(email, new CodeEntry(code));
        return code;
    }

    /**
     * 校验验证码。校验通过即作废（一次性使用）。
     * 试错次数超过上限时直接丢弃该验证码，防止 6 位数字被暴力枚举。
     */
    public void verify(String email, String inputCode) {
        if (inputCode == null || inputCode.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码不能为空");
        }
        CodeEntry entry = codes.getIfPresent(email);
        if (entry == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码已过期，请重新获取");
        }
        if (!entry.code.equals(inputCode.trim())) {
            entry.attempts++;
            if (entry.attempts >= maxAttempts) {
                codes.invalidate(email);
                throw new BusinessException(ResultCode.BAD_REQUEST, "错误次数过多，验证码已作废，请重新获取");
            }
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "验证码错误，还可尝试 " + (maxAttempts - entry.attempts) + " 次");
        }
        // 一次性使用，验证通过立即作废，避免同一验证码重复注册
        codes.invalidate(email);
    }

    /**
     * 作废某邮箱当前的验证码。
     * 发信失败时调用：否则验证码已进缓存、冷却计时已开始，
     * 用户一封邮件都没收到却被锁 60 秒不能重试。
     */
    public void invalidate(String email) {
        codes.invalidate(email);
    }

    private static final class CodeEntry {
        final String code;
        final long sentAt = System.currentTimeMillis();
        int attempts = 0;

        CodeEntry(String code) {
            this.code = code;
        }
    }
}
