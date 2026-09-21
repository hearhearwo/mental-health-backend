package com.example.mentalhealth.dto;

import com.example.mentalhealth.entity.EmotionLog;
import com.example.mentalhealth.util.DateFormats;
import lombok.Data;

/** 前端期望：{ id, date, mood, score, content } */
@Data
public class DiaryResponse {

    private Long id;
    private String date;
    private String mood;
    private Integer score;
    private String content;

    public static DiaryResponse from(EmotionLog log) {
        DiaryResponse response = new DiaryResponse();
        response.setId(log.getId());
        response.setDate(DateFormats.toDate(log.getCreateTime()));
        response.setMood(log.getEmotionTag());
        response.setScore(log.getEmotionScore());
        response.setContent(log.getContent());
        return response;
    }
}
