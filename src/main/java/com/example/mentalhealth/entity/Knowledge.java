package com.example.mentalhealth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

@Data
public class Knowledge {

    private Integer id;
    private String title;
    private String summary;
    private String content;
    private String cover;
    private String category;
    private String source;
    private Integer viewCount;
    private Integer status;
    private Date createTime;
    private Date updateTime;

    /**
     * 当前用户是否已收藏。不是 knowledge 表的列，
     * 由 selectByCondition / selectById 联表 article_favorite 带出。
     */
    @TableField(exist = false)
    private Boolean favorited;
}
