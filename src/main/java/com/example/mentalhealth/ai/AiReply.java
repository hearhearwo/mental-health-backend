package com.example.mentalhealth.ai;

import lombok.Data;

import java.util.Map;

/** AI 对一条用户消息的回复，字段与前端契约对应 */
@Data
public class AiReply {

    /** 给用户看的正文 */
    private String content;
    /** text / card */
    private String type = "text";
    /** 是否危机干预 */
    private Boolean crisis = Boolean.FALSE;
    /** type=card 时的卡片内容：{title, items} 或 {title, articleRefs} */
    private Map<String, Object> card;

    public boolean isCard() {
        return "card".equals(type);
    }
}
