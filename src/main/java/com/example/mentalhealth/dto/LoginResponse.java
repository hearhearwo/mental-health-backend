package com.example.mentalhealth.dto;

import lombok.Data;

/** 前端期望：{ token, user: { name } } */
@Data
public class LoginResponse {

    private String token;
    private UserInfo user;

    public LoginResponse(String token, String name) {
        this.token = token;
        this.user = new UserInfo(name);
    }

    @Data
    public static class UserInfo {
        private String name;

        public UserInfo(String name) {
            this.name = name;
        }
    }
}
