package com.example.mentalhealth.dto;

import com.example.mentalhealth.entity.ChatMessage;
import com.example.mentalhealth.util.DateFormats;
import lombok.Data;

import java.util.Map;

/** 前端期望：{ id, role, content, type, crisis, time, card } */
@Data
public class MessageResponse {

    private Long id;
    private String role;
    private String content;
    private String type;
    private Boolean crisis;
    private String time;
    /** type=card 时才有值 */
    private Map<String, Object> card;

    public static MessageResponse from(ChatMessage message) {
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        response.setRole(message.getRole());
        response.setContent(message.getContent());
        response.setType(message.getType() == null ? "text" : message.getType());
        response.setCrisis(Boolean.TRUE.equals(message.getCrisis()));
        response.setTime(DateFormats.toTime(message.getCreateTime()));
        response.setCard(message.getCardData());
        return response;
    }
}
