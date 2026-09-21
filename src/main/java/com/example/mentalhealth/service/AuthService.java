package com.example.mentalhealth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentalhealth.common.BusinessException;
import com.example.mentalhealth.common.ResultCode;
import com.example.mentalhealth.dto.LoginResponse;
import com.example.mentalhealth.entity.User;
import com.example.mentalhealth.mapper.UserMapper;
import com.example.mentalhealth.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final int PASSWORD_MAX_LENGTH = 64;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailCodeService emailCodeService;
    private final MailService mailService;

    public AuthService(UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       EmailCodeService emailCodeService,
                       MailService mailService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailCodeService = emailCodeService;
        this.mailService = mailService;
    }

    public LoginResponse login(String account, String password) {
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号和密码不能为空");
        }

        // account 允许是用户名、手机号或邮箱
        List<User> matched = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, account)
                .or()
                .eq(User::getPhone, account)
                .or()
                .eq(User::getEmail, account));
        User user = matched.isEmpty() ? null : matched.get(0);

        // 账号不存在与密码错误返回同一条提示，避免暴露某个账号是否已注册
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号已被禁用");
        }

        String token = jwtUtil.generate(user.getId(), user.getUsername());
        String displayName = StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
        return new LoginResponse(token, displayName);
    }

    /** 发送注册验证码 */
    public void sendRegisterCode(String email) {
        String normalized = normalizeEmail(email);
        // 注意：这里不校验邮箱是否已注册。如果校验了，任何人都能拿这个接口
        // 逐个试出哪些邮箱已注册。查重放到 register 里，且必须在验证码校验之后。
        String code = emailCodeService.generate(normalized);
        try {
            mailService.sendCode(normalized, code);
        } catch (RuntimeException e) {
            // 邮件没发出去，就把已经放进缓存的验证码作废掉。
            // 否则 60 秒冷却已经生效，用户重试只会得到「请求过于频繁」，
            // 而他根本没收到任何邮件——两条提示自相矛盾，很难排查。
            emailCodeService.invalidate(normalized);
            throw e;
        }
    }

    /**
     * 邮箱注册。注册成功后直接签发 token，前端不必再调一次登录。
     */
    public LoginResponse register(String email, String password, String code) {
        String normalized = normalizeEmail(email);
        validatePassword(password);

        // 顺序很重要：先校验验证码，再查重。
        // 反过来的话，没拿到验证码的人也能用这个接口探测邮箱是否已注册。
        emailCodeService.verify(normalized, code);

        boolean exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, normalized)) > 0
                || userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, normalized)) > 0;
        if (exists) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该邮箱已注册，请直接登录");
        }

        Date now = new Date();
        User user = new User();
        // 登录名直接用邮箱，这样现有的登录流程不需要额外改动
        user.setUsername(normalized);
        user.setEmail(normalized);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(normalized.substring(0, normalized.indexOf('@')));
        user.setRole("user");
        user.setStatus(1);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        userMapper.insert(user);

        String token = jwtUtil.generate(user.getId(), user.getUsername());
        return new LoginResponse(token, user.getNickname());
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邮箱不能为空");
        }
        String normalized = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邮箱格式不正确");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码不能为空");
        }
        if (password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "密码长度需在 " + PASSWORD_MIN_LENGTH + "-" + PASSWORD_MAX_LENGTH + " 位之间");
        }
    }
}
