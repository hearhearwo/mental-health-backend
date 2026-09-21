package com.example.mentalhealth.dto;

import com.example.mentalhealth.entity.ChatSession;
import com.example.mentalhealth.util.DateFormats;
import lombok.Data;

/** 前端期望：{ id, title, updatedAt } */
@Data
public class ConversationResponse {

    private Long id;
    private String title;
    private String updatedAt;

    public static ConversationResponse from(ChatSession session) {
        ConversationResponse response = new ConversationResponse();
        response.setId(session.getId());
        response.setTitle(session.getSessionName());
        response.setUpdatedAt(DateFormats.toMonthDayTime(session.getUpdateTime()));
        return response;
    }
}
