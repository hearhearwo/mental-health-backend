package com.example.mentalhealth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 前端期望：{ user: 用户消息, reply: AI 回复 } */
@Data
@AllArgsConstructor
public class SendMessageResponse {
    private MessageResponse user;
    private MessageResponse reply;
}
