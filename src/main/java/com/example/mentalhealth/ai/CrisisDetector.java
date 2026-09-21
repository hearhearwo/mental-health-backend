package com.example.mentalhealth.ai;

import java.util.regex.Pattern;

/**
 * 危机语句的关键词兜底规则。
 *
 * 与前端 mock 里 generateReply 的正则完全一致，保证前后端行为统一。
 * 这是「双保险」里的第二道：即使大模型没有判定为危机，
 * 只要命中这里的任一关键词，也强制按危机处理。
 */
public final class CrisisDetector {

    private static final Pattern CRISIS_PATTERN =
            Pattern.compile("自杀|不想活|轻生|绝望|活不下去|结束生命");

    private CrisisDetector() {
    }

    public static boolean matches(String text) {
        return text != null && CRISIS_PATTERN.matcher(text).find();
    }
}
