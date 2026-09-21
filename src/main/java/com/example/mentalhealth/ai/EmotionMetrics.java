package com.example.mentalhealth.ai;

import lombok.Data;

/**
 * 从一条用户消息里提取的心理状态指标，0-100 分。
 * 写入 emotion_log（source='chat'），前端 /api/profile/status 从这个表聚合。
 */
@Data
public class EmotionMetrics {

    /** 情绪维度，越高越平稳 */
    private Integer dimEmotion;
    /** 压力维度，越高压力越大 */
    private Integer dimStress;
    /** 睡眠维度，越高睡眠越好 */
    private Integer dimSleep;
    /** 社交维度，越高越好 */
    private Integer dimSocial;
    /** 危机指数，越高风险越大 */
    private Integer dimCrisis;
    /** 综合等级 L1-L5 */
    private String level;
}
