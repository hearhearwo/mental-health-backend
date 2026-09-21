package com.example.mentalhealth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * autoResultMap = true 是 JacksonTypeHandler 生效的前提，
 * 否则查询时不会走这个 typeHandler，cardData 会拿不到值。
 */
@Data
@TableName(value = "chat_message", autoResultMap = true)
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    /** user / assistant */
    private String role;
    /** 只放给用户看的正文，业务元数据不放这里 */
    private String content;
    /** text / card */
    private String type;
    /** 危机干预回复，前端红色高亮 */
    private Boolean crisis;
    /** 卡片内容，type=card 时有效。数据库是 JSON 列 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> cardData;
    private Date createTime;
}
