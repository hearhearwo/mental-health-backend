package com.example.mentalhealth.dto;

import lombok.Data;

/** 发送注册验证码：{ email } */
@Data
public class SendCodeRequest {
    private String email;
}
