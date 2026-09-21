package com.example.mentalhealth.ai;

import com.example.mentalhealth.entity.ChatMessage;

import java.util.List;

/**
 * AI 能力抽象。目前两个实现：
 *   DeepSeekAiService —— 配了 DEEPSEEK_API_KEY 时走真实大模型（@Primary）
 *   RuleBasedAiService —— 本地规则，无 key 或模型调用失败时兜底
 */
public interface AiService {

    /** 根据历史消息与用户新消息，生成一条回复 */
    AiReply reply(List<ChatMessage> history, String userMessage);

    /** 从一条用户消息里提取 5 维度心理指标 */
    EmotionMetrics extractMetrics(String userMessage);
}
