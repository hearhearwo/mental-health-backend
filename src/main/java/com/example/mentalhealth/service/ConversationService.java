package com.example.mentalhealth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentalhealth.ai.AiReply;
import com.example.mentalhealth.ai.AiService;
import com.example.mentalhealth.common.BusinessException;
import com.example.mentalhealth.common.ResultCode;
import com.example.mentalhealth.dto.ConversationResponse;
import com.example.mentalhealth.dto.MessageResponse;
import com.example.mentalhealth.dto.SendMessageResponse;
import com.example.mentalhealth.entity.ChatMessage;
import com.example.mentalhealth.entity.ChatSession;
import com.example.mentalhealth.mapper.ChatMessageMapper;
import com.example.mentalhealth.mapper.ChatSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Service
public class ConversationService {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    /** 单条消息长度上限，防止超长输入拖垮模型调用 */
    private static final int MAX_CONTENT_LENGTH = 2000;

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final AiService aiService;
    private final MetricExtractService metricExtractService;

    public ConversationService(ChatSessionMapper chatSessionMapper,
                               ChatMessageMapper chatMessageMapper,
                               AiService aiService,
                               MetricExtractService metricExtractService) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.aiService = aiService;
        this.metricExtractService = metricExtractService;
    }

    public List<ConversationResponse> list(Long userId) {
        return chatSessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .orderByDesc(ChatSession::getUpdateTime))
                .stream()
                .map(ConversationResponse::from)
                .toList();
    }

    public ConversationResponse create(Long userId) {
        Date now = new Date();
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setSessionName("新的会话");
        session.setCreateTime(now);
        session.setUpdateTime(now);
        chatSessionMapper.insert(session);
        return ConversationResponse.from(session);
    }

    public List<MessageResponse> messages(Long userId, Long sessionId) {
        requireOwnedSession(userId, sessionId);
        return chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getId))
                .stream()
                .map(MessageResponse::from)
                .toList();
    }

    public SendMessageResponse send(Long userId, Long sessionId, String content) {
        ChatSession session = requireOwnedSession(userId, sessionId);

        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "消息内容不能为空");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "消息过长，请控制在 " + MAX_CONTENT_LENGTH + " 字以内");
        }

        // 取历史（不含本条）作为上下文
        List<ChatMessage> history = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getId));

        Date now = new Date();
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setRole(ROLE_USER);
        userMessage.setContent(content);
        userMessage.setType("text");
        userMessage.setCrisis(Boolean.FALSE);
        userMessage.setCreateTime(now);
        chatMessageMapper.insert(userMessage);

        // 同步等 AI 回复：前端发完消息就在转圈，这里必须拿到结果再返回
        AiReply aiReply = aiService.reply(history, content);

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setRole(ROLE_ASSISTANT);
        assistantMessage.setContent(aiReply.getContent());
        assistantMessage.setType(aiReply.isCard() ? "card" : "text");
        assistantMessage.setCrisis(Boolean.TRUE.equals(aiReply.getCrisis()));
        assistantMessage.setCardData(aiReply.getCard());
        assistantMessage.setCreateTime(new Date());
        chatMessageMapper.insert(assistantMessage);

        // 会话标题用首条用户消息，方便前端列表识别
        if (history.isEmpty()) {
            session.setSessionName(abbreviate(content));
        }
        session.setUpdateTime(new Date());
        chatSessionMapper.updateById(session);

        // 异步提取心理指标，不阻塞本次响应
        metricExtractService.extractAndSave(userId, content);

        return new SendMessageResponse(MessageResponse.from(userMessage), MessageResponse.from(assistantMessage));
    }

    public void delete(Long userId, Long sessionId) {
        requireOwnedSession(userId, sessionId);
        chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId));
        chatSessionMapper.deleteById(sessionId);
    }

    /**
     * 会话归属校验。所有涉及 sessionId 的操作都要先过这一关，
     * 否则换个 id 就能读到别人的对话。
     */
    private ChatSession requireOwnedSession(Long userId, Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        return session;
    }

    private static String abbreviate(String text) {
        String trimmed = text.trim();
        return trimmed.length() <= 20 ? trimmed : trimmed.substring(0, 20);
    }
}
