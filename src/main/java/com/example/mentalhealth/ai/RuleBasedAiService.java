package com.example.mentalhealth.ai;

import com.example.mentalhealth.entity.ChatMessage;
import com.example.mentalhealth.entity.Knowledge;
import com.example.mentalhealth.mapper.KnowledgeMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 本地规则实现。逻辑与前端 mock 的 generateReply 保持一致，
 * 用于「没有配 DEEPSEEK_API_KEY」和「模型调用失败」两种情况。
 */
@Service
public class RuleBasedAiService implements AiService {

    private static final Pattern SLEEP = Pattern.compile("睡|失眠|熬夜|睡不着");
    private static final Pattern STRESS = Pattern.compile("焦虑|压力|紧张|很累|烦躁");
    private static final Pattern ARTICLE = Pattern.compile("建议|方法|文章|知识|怎么办");

    private static final String CRISIS_CONTENT =
            "听到你这样说，我非常担心你。你此刻的痛苦是真实的，但请相信，你并不孤单，也永远有求助的出口。\n\n"
                    + "请立即联系专业人士：\n"
                    + "心理援助热线：12355\n"
                    + "24 小时危机干预热线：400-161-9995\n\n"
                    + "如情况紧急，请立刻拨打 120 或前往最近的医院急诊。你的生命非常重要。";

    private final KnowledgeMapper knowledgeMapper;

    public RuleBasedAiService(KnowledgeMapper knowledgeMapper) {
        this.knowledgeMapper = knowledgeMapper;
    }

    @Override
    public AiReply reply(List<ChatMessage> history, String userMessage) {
        String text = userMessage == null ? "" : userMessage;

        // 与前端一致：危机语句最优先，且不做淡化处理
        if (CrisisDetector.matches(text)) {
            AiReply reply = new AiReply();
            reply.setType("text");
            reply.setCrisis(Boolean.TRUE);
            reply.setContent(CRISIS_CONTENT);
            return reply;
        }

        if (SLEEP.matcher(text).find()) {
            return card("针对睡眠困扰，给你几个可以立刻尝试的建议：", "睡眠改善建议",
                    List.of("睡前 1 小时远离手机和电脑屏幕",
                            "保持固定的起床时间，即使是周末",
                            "下午之后避免摄入咖啡因",
                            "睡前做 5 分钟腹式呼吸放松"));
        }

        if (STRESS.matcher(text).find()) {
            return card("压力需要被温柔地对待，试试这些方法：", "压力缓解清单",
                    List.of("把担忧写下来，给情绪一个出口",
                            "每天留出 10 分钟只做一件喜欢的事",
                            "进行适度运动，释放身体紧张",
                            "和信任的人聊一聊现在的感受"));
        }

        if (ARTICLE.matcher(text).find()) {
            return articleCard();
        }

        AiReply reply = new AiReply();
        reply.setType("text");
        reply.setCrisis(Boolean.FALSE);
        reply.setContent("我理解你的感受，谢谢你愿意和我分享。可以再多说一点吗？"
                + "比如这种感受是什么时候开始的，以及它对你的日常生活带来了哪些影响？");
        return reply;
    }

    private AiReply card(String content, String title, List<String> items) {
        AiReply reply = new AiReply();
        reply.setType("card");
        reply.setCrisis(Boolean.FALSE);
        reply.setContent(content);
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("title", title);
        card.put("items", items);
        reply.setCard(card);
        return reply;
    }

    /** 推荐卡片用知识库里的真实文章，避免前端点进去是空链接 */
    private AiReply articleCard() {
        List<Knowledge> articles = knowledgeMapper.selectTop(3);

        List<Map<String, Object>> refs = new ArrayList<>();
        for (Knowledge article : articles) {
            Map<String, Object> ref = new LinkedHashMap<>();
            ref.put("id", article.getId());
            ref.put("title", article.getTitle());
            refs.add(ref);
        }

        AiReply reply = new AiReply();
        reply.setType("card");
        reply.setCrisis(Boolean.FALSE);
        reply.setContent("这里有几篇可能对你有帮助的文章：");
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("title", "为你推荐");
        card.put("articleRefs", refs);
        reply.setCard(card);
        return reply;
    }

    /**
     * 关键词启发式的粗略打分，仅作为无 key 时的占位，
     * 让状态图有数据可画。接入真实模型后这条路径基本不会走到。
     */
    @Override
    public EmotionMetrics extractMetrics(String userMessage) {
        String text = userMessage == null ? "" : userMessage;

        int emotion = 65;
        int stress = 45;
        int sleep = 70;
        int social = 65;
        int crisis = 15;

        if (CrisisDetector.matches(text)) {
            crisis = 95;
            emotion = 15;
            stress = 90;
        }
        if (SLEEP.matcher(text).find()) {
            sleep = 30;
            emotion -= 10;
        }
        if (STRESS.matcher(text).find()) {
            stress += 30;
            emotion -= 15;
        }
        if (Pattern.compile("孤单|孤独|没朋友|没人理解|一个人").matcher(text).find()) {
            social = 30;
            emotion -= 10;
        }
        if (Pattern.compile("开心|高兴|放松|不错|很好|谢谢").matcher(text).find()) {
            emotion += 20;
            stress -= 15;
        }

        EmotionMetrics metrics = new EmotionMetrics();
        metrics.setDimEmotion(clamp(emotion));
        metrics.setDimStress(clamp(stress));
        metrics.setDimSleep(clamp(sleep));
        metrics.setDimSocial(clamp(social));
        metrics.setDimCrisis(clamp(crisis));
        metrics.setLevel(levelOf(metrics));
        return metrics;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    /**
     * 综合等级：危机指数过高直接 L5/L4，否则看压力与情绪的加权。
     * 与前端 L1-L5 语义一致：L1 状态良好 → L5 高风险预警。
     */
    static String levelOf(EmotionMetrics m) {
        if (m.getDimCrisis() != null && m.getDimCrisis() >= 80) {
            return "L5";
        }
        if (m.getDimCrisis() != null && m.getDimCrisis() >= 60) {
            return "L4";
        }
        double bad = (100 - safe(m.getDimEmotion())) * 0.4
                + safe(m.getDimStress()) * 0.3
                + (100 - safe(m.getDimSleep())) * 0.2
                + (100 - safe(m.getDimSocial())) * 0.1;
        if (bad < 25) {
            return "L1";
        }
        if (bad < 40) {
            return "L2";
        }
        if (bad < 60) {
            return "L3";
        }
        return "L4";
    }

    private static int safe(Integer value) {
        return value == null ? 50 : value;
    }
}
