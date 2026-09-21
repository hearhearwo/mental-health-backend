package com.example.mentalhealth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户手写日记与聊天异步提取的指标共用这张表，靠 source 区分：
 *   source='diary' —— 用户手写，用 emotionTag / emotionScore（1-5 分）
 *   source='chat'  —— AI 从聊天提取，用 dim* 五个维度（0-100 分）与 level
 * 两套量纲各占各的列，聚合统计时不会互相污染。
 */
@Data
@TableName("emotion_log")
public class EmotionLog {

    /** source 取值 */
    public static final String SOURCE_DIARY = "diary";
    public static final String SOURCE_CHAT = "chat";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** diary / chat */
    private String source;
    private String content;
    /** 日记的情绪标签：开心 / 平静 / 焦虑 / 难过 / 愤怒 */
    private String emotionTag;
    /** 日记的 1-5 分自评 */
    private Integer emotionScore;

    /** 以下 5 个维度为 AI 从聊天中提取，0-100 */
    private Integer dimEmotion;
    private Integer dimStress;
    private Integer dimSleep;
    private Integer dimSocial;
    private Integer dimCrisis;
    /** 综合等级 L1-L5 */
    private String level;

    private Date createTime;
    private Date updateTime;
}
