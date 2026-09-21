package com.example.mentalhealth.mapper;

import com.example.mentalhealth.entity.Knowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeMapper {

    /**
     * 按分类 / 关键词查询文章，并带出当前用户是否已收藏。
     * category 和 keyword 传 null 或空串表示不过滤。
     */
    List<Knowledge> selectByCondition(@Param("category") String category,
                                      @Param("keyword") String keyword,
                                      @Param("userId") Long userId);

    /** 文章详情，同样带出 favorited */
    Knowledge selectById(@Param("id") Integer id, @Param("userId") Long userId);

    int increaseViewCount(@Param("id") Integer id);

    /** 按浏览量取前 N 篇，供 AI 推荐卡片使用 */
    List<Knowledge> selectTop(@Param("limit") int limit);

    int insert(Knowledge knowledge);

    int update(Knowledge knowledge);

    int deleteById(@Param("id") Integer id);
}
