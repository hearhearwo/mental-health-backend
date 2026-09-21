package com.example.mentalhealth.dto;

import lombok.Data;

/** 邮箱注册：{ email, password, code } */
@Data
public class RegisterRequest {
    private String email;
    private String password;
    /** 邮箱收到的 6 位验证码 */
    private String code;
}
