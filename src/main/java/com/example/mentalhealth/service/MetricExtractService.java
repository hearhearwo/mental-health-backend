package com.example.mentalhealth.service;

import com.example.mentalhealth.ai.AiService;
import com.example.mentalhealth.ai.EmotionMetrics;
import com.example.mentalhealth.entity.EmotionLog;
import com.example.mentalhealth.mapper.EmotionLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 从用户消息里异步提取 5 维度心理指标，写入 emotion_log（source='chat'）。
 *
 * 两点注意：
 * 1. 必须是异步的——不能让用户发完消息还等模型跑完指标提取。
 * 2. 不能抛出任何异常——指标提取失败不该影响聊天本身。
 */
@Service
public class MetricExtractService {

    private static final Logger log = LoggerFactory.getLogger(MetricExtractService.class);

    /** 太短的消息（"嗯""好的"）没有分析价值，跳过以省调用成本 */
    private static final int MIN_LENGTH = 4;

    private final AiService aiService;
    private final EmotionLogMapper emotionLogMapper;

    public MetricExtractService(AiService aiService, EmotionLogMapper emotionLogMapper) {
        this.aiService = aiService;
        this.emotionLogMapper = emotionLogMapper;
    }

    /**
     * userId 必须作为参数传进来，不能靠 UserContext——
     * ThreadLocal 不会传播到异步线程。
     */
    @Async("metricExecutor")
    public void extractAndSave(Long userId, String userMessage) {
        if (userMessage == null || userMessage.trim().length() < MIN_LENGTH) {
            return;
        }
        try {
            EmotionMetrics metrics = aiService.extractMetrics(userMessage);

            EmotionLog record = new EmotionLog();
            record.setUserId(userId);
            record.setSource(EmotionLog.SOURCE_CHAT);
            record.setContent(userMessage);
            record.setDimEmotion(metrics.getDimEmotion());
            record.setDimStress(metrics.getDimStress());
            record.setDimSleep(metrics.getDimSleep());
            record.setDimSocial(metrics.getDimSocial());
            record.setDimCrisis(metrics.getDimCrisis());
            record.setLevel(metrics.getLevel());

            Date now = new Date();
            record.setCreateTime(now);
            record.setUpdateTime(now);

            emotionLogMapper.insert(record);
        } catch (Exception e) {
            // 吞掉异常，绝不影响聊天主流程
            log.warn("心理指标提取失败（不影响对话）：{}", e.getMessage());
        }
    }
}
