package com.example.mentalhealth.dto;

import lombok.Data;

@Data
public class LoginRequest {
    /** 用户名或手机号 */
    private String account;
    private String password;
}
