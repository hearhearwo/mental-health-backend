package com.example.mentalhealth.dto;

import com.example.mentalhealth.entity.Knowledge;
import com.example.mentalhealth.util.DateFormats;
import lombok.Data;

/**
 * 前端期望：{ id, title, category, summary, views, favorited, source, updatedAt, content }
 * 注意 views / updatedAt 与数据库列名 view_count / update_time 不一致，所以需要这层转换。
 */
@Data
public class ArticleResponse {

    private Integer id;
    private String title;
    private String category;
    private String summary;
    private Integer views;
    private Boolean favorited;
    private String source;
    private String updatedAt;
    private String content;

    public static ArticleResponse from(Knowledge knowledge) {
        ArticleResponse response = new ArticleResponse();
        response.setId(knowledge.getId());
        response.setTitle(knowledge.getTitle());
        response.setCategory(knowledge.getCategory());
        response.setSummary(knowledge.getSummary());
        response.setViews(knowledge.getViewCount());
        response.setFavorited(Boolean.TRUE.equals(knowledge.getFavorited()));
        response.setSource(knowledge.getSource());
        // 前端契约里文章日期是纯日期（如 "2026-09-12"），不带时分秒
        response.setUpdatedAt(DateFormats.toDate(knowledge.getUpdateTime()));
        response.setContent(knowledge.getContent());
        return response;
    }
}
