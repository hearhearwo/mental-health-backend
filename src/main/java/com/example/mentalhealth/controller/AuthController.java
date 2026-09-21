package com.example.mentalhealth.controller;

import com.example.mentalhealth.common.Result;
import com.example.mentalhealth.dto.LoginRequest;
import com.example.mentalhealth.dto.LoginResponse;
import com.example.mentalhealth.dto.RegisterRequest;
import com.example.mentalhealth.dto.SendCodeRequest;
import com.example.mentalhealth.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.ok(authService.login(request.getAccount(), request.getPassword()));
    }

    /** 发送注册验证码到邮箱。同一邮箱 60 秒内只能请求一次 */
    @PostMapping("/email-code")
    public Result<Void> sendEmailCode(@RequestBody SendCodeRequest request) {
        authService.sendRegisterCode(request.getEmail());
        return Result.ok();
    }

    /** 邮箱注册。成功直接返回 token，前端不必再调一次登录 */
    @PostMapping("/register")
    public Result<LoginResponse> register(@RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request.getEmail(), request.getPassword(), request.getCode()));
    }

    /**
     * JWT 是无状态的，服务端没有会话可销毁，前端清掉本地 token 即可。
     * 保留这个接口是为了满足前端契约，也让以后换成有状态 token 时有地方扩展。
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }
}
