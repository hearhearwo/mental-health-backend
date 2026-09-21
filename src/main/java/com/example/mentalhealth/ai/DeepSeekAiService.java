package com.example.mentalhealth.ai;

import com.example.mentalhealth.entity.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek 实现，@Primary 优先于 RuleBasedAiService。
 *
 * 降级策略：没配 api-key、或者调用/解析任何一步失败，都回退到 RulesBasedAiService，
 * 保证聊天功能不会因为外部服务不可用而挂掉。
 */
@Service
@Primary
public class DeepSeekAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekAiService.class);

    /** 最多带上最近多少条历史消息作为上下文 */
    private static final int HISTORY_LIMIT = 20;

    private static final String CHAT_SYSTEM_PROMPT = """
            你是一位心理健康自助辅助助手，不是医生。请严格只输出一个 JSON 对象，不要输出任何解释性文字或 Markdown 代码块。

            输出格式：
            {"content":"给用户的回复正文","type":"text 或 card","crisis":false,"card":{"title":"标题","items":["条目1","条目2"]}}

            规则：
            1. 如果用户表达自杀、自伤、不想活等危机倾向，crisis 必须为 true，content 中必须明确给出：
               心理援助热线 12355、24 小时危机干预热线 400-161-9995，并建议紧急时拨打 120 或前往最近医院急诊。
            2. 需要给建议清单时用 type=card，card 形如 {"title":"标题","items":["条目1","条目2"]}。
            3. 推荐文章时用 type=card，card 形如 {"title":"为你推荐","articleRefs":[{"id":1,"title":"文章标题"}]}。
            4. 其余情况 type=text，并且不要返回 card 字段。
            5. 不做医学诊断，不推荐药物，语气温和共情，回复控制在 300 字以内。""";

    private static final String METRIC_SYSTEM_PROMPT = """
            你是心理状态评估助手。请根据用户的一段话，严格只输出一个 JSON 对象，不要输出任何解释性文字。

            输出格式：
            {"dimEmotion":0-100,"dimStress":0-100,"dimSleep":0-100,"dimSocial":0-100,"dimCrisis":0-100,"level":"L2"}

            取值含义：
            - dimEmotion：情绪维度，越高表示情绪越平稳
            - dimStress：压力维度，越高表示压力越大
            - dimSleep：睡眠维度，越高表示睡眠越好
            - dimSocial：社交维度，越高表示社交状况越好
            - dimCrisis：危机指数，越高表示风险越大（出现自杀、自伤等表述时应给 80 以上）
            - level：综合等级，L1 状态良好 / L2 轻度波动 / L3 中度压力 / L4 需关注 / L5 高风险预警

            信息不足时按中性值约 60 估计，不要拒绝输出。""";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RuleBasedAiService fallback;

    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public DeepSeekAiService(RestTemplate aiRestTemplate,
                             ObjectMapper objectMapper,
                             RuleBasedAiService fallback,
                             @Value("${deepseek.api-key:}") String apiKey,
                             @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
                             @Value("${deepseek.model:deepseek-chat}") String model) {
        this.restTemplate = aiRestTemplate;
        this.objectMapper = objectMapper;
        this.fallback = fallback;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        if (!StringUtils.hasText(apiKey)) {
            log.warn("未配置 DEEPSEEK_API_KEY，AI 回复将使用本地规则兜底");
        }
    }

    @Override
    public AiReply reply(List<ChatMessage> history, String userMessage) {
        if (!available()) {
            return fallback.reply(history, userMessage);
        }
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", CHAT_SYSTEM_PROMPT));
            for (ChatMessage message : tail(history)) {
                // DeepSeek 只认 user / assistant，历史里的 role 恰好就是这两个
                messages.add(Map.of("role", message.getRole(), "content", message.getContent()));
            }
            messages.add(Map.of("role", "user", "content", userMessage == null ? "" : userMessage));

            String raw = call(messages, 0.8);
            AiReply reply = objectMapper.readValue(raw, AiReply.class);
            if (!StringUtils.hasText(reply.getContent())) {
                throw new IllegalStateException("模型返回内容为空");
            }
            return applyCrisisSafetyNet(reply, userMessage);
        } catch (Exception e) {
            log.warn("DeepSeek 回复失败，回退本地规则：{}", e.getMessage());
            return fallback.reply(history, userMessage);
        }
    }

    @Override
    public EmotionMetrics extractMetrics(String userMessage) {
        if (!available()) {
            return fallback.extractMetrics(userMessage);
        }
        try {
            String raw = call(List.of(
                    Map.of("role", "system", "content", METRIC_SYSTEM_PROMPT),
                    Map.of("role", "user", "content", userMessage == null ? "" : userMessage)), 0.2);

            EmotionMetrics metrics = objectMapper.readValue(raw, EmotionMetrics.class);
            normalize(metrics);
            return metrics;
        } catch (Exception e) {
            log.warn("DeepSeek 指标提取失败，回退本地规则：{}", e.getMessage());
            return fallback.extractMetrics(userMessage);
        }
    }

    /**
     * 危机识别双保险：模型判定的结果与关键词规则取「或」。
     * 模型漏判但关键词命中时，直接换成标准的危机干预回复，确保热线信息一定出现。
     */
    private AiReply applyCrisisSafetyNet(AiReply reply, String userMessage) {
        boolean byKeyword = CrisisDetector.matches(userMessage);
        boolean byModel = Boolean.TRUE.equals(reply.getCrisis());

        if (byKeyword && !byModel) {
            log.info("关键词命中危机语句但模型未标记，强制按危机处理");
            AiReply crisisReply = new AiReply();
            crisisReply.setType("text");
            crisisReply.setCrisis(Boolean.TRUE);
            crisisReply.setContent("听到你这样说，我非常担心你。你此刻的痛苦是真实的，但请相信，你并不孤单，也永远有求助的出口。\n\n"
                    + "请立即联系专业人士：\n心理援助热线：12355\n24 小时危机干预热线：400-161-9995\n\n"
                    + "如情况紧急，请立刻拨打 120 或前往最近的医院急诊。你的生命非常重要。");
            return crisisReply;
        }
        if (byKeyword || byModel) {
            // 任一方判定为危机，就一定是危机；内容以模型给的为准（已要求包含热线）
            reply.setCrisis(Boolean.TRUE);
            reply.setType("text");
            reply.setCard(null);
        }
        return reply;
    }

    /** 模型可能给出越界值或漏字段，这里统一收敛 */
    private void normalize(EmotionMetrics metrics) {
        metrics.setDimEmotion(clamp(metrics.getDimEmotion()));
        metrics.setDimStress(clamp(metrics.getDimStress()));
        metrics.setDimSleep(clamp(metrics.getDimSleep()));
        metrics.setDimSocial(clamp(metrics.getDimSocial()));
        metrics.setDimCrisis(clamp(metrics.getDimCrisis()));
        if (!List.of("L1", "L2", "L3", "L4", "L5").contains(metrics.getLevel())) {
            metrics.setLevel(RuleBasedAiService.levelOf(metrics));
        }
    }

    private static Integer clamp(Integer value) {
        if (value == null) {
            return 60;
        }
        return Math.max(0, Math.min(100, value));
    }

    private List<ChatMessage> tail(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        int from = Math.max(0, history.size() - HISTORY_LIMIT);
        return history.subList(from, history.size());
    }

    private boolean available() {
        return StringUtils.hasText(apiKey);
    }

    /** 调用 chat/completions，返回 message.content 里的 JSON 文本 */
    private String call(List<Map<String, String>> messages, double temperature) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        // 要求模型输出 JSON，避免它包一层 ```json 代码块
        body.put("response_format", Map.of("type", "json_object"));
        body.put("temperature", temperature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/chat/completions",
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
                String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
            throw new IllegalStateException("响应结构异常：" + response.getBody());
        }
        return stripCodeFence(contentNode.asText());
    }

    /** 少数情况下模型仍会包 ```json，这里兜一层 */
    private String stripCodeFence(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}
