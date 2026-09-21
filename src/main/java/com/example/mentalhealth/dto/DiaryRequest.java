package com.example.mentalhealth.dto;

import lombok.Data;

/** 前端提交：{ date, mood, score, content } */
@Data
public class DiaryRequest {
    /** yyyy-MM-dd，不传则用当前时间 */
    private String date;
    /** 情绪标签：开心 / 平静 / 焦虑 / 难过 / 愤怒 */
    private String mood;
    /** 1-5 分 */
    private Integer score;
    private String content;
}
