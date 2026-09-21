package com.example.mentalhealth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentalhealth.common.BusinessException;
import com.example.mentalhealth.common.Result;
import com.example.mentalhealth.common.ResultCode;
import com.example.mentalhealth.common.UserContext;
import com.example.mentalhealth.dto.ArticleResponse;
import com.example.mentalhealth.entity.ArticleFavorite;
import com.example.mentalhealth.entity.Knowledge;
import com.example.mentalhealth.mapper.ArticleFavoriteMapper;
import com.example.mentalhealth.mapper.KnowledgeMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final KnowledgeMapper knowledgeMapper;
    private final ArticleFavoriteMapper articleFavoriteMapper;

    public ArticleController(KnowledgeMapper knowledgeMapper,
                             ArticleFavoriteMapper articleFavoriteMapper) {
        this.knowledgeMapper = knowledgeMapper;
        this.articleFavoriteMapper = articleFavoriteMapper;
    }

    /** 文章列表，支持分类过滤与关键词搜索 */
    @GetMapping
    public Result<List<ArticleResponse>> list(@RequestParam(value = "category", required = false) String category,
                                              @RequestParam(value = "keyword", required = false) String keyword) {
        Long userId = UserContext.require();
        List<ArticleResponse> data = knowledgeMapper.selectByCondition(category, keyword, userId)
                .stream()
                .map(ArticleResponse::from)
                .toList();
        return Result.ok(data);
    }

    /** 文章详情，顺带累加浏览量 */
    @GetMapping("/{id}")
    public Result<ArticleResponse> detail(@PathVariable("id") Integer id) {
        Long userId = UserContext.require();
        Knowledge knowledge = knowledgeMapper.selectById(id, userId);
        if (knowledge == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        }
        knowledgeMapper.increaseViewCount(id);
        return Result.ok(ArticleResponse.from(knowledge));
    }

    /** 收藏 / 取消收藏，同一个接口来回切换，返回切换后的状态 */
    @PostMapping("/{id}/favorite")
    public Result<Map<String, Boolean>> toggleFavorite(@PathVariable("id") Integer id) {
        Long userId = UserContext.require();
        if (knowledgeMapper.selectById(id, userId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        }

        ArticleFavorite existing = articleFavoriteMapper.selectOne(
                new LambdaQueryWrapper<ArticleFavorite>()
                        .eq(ArticleFavorite::getUserId, userId)
                        .eq(ArticleFavorite::getArticleId, id));

        boolean favorited;
        if (existing == null) {
            ArticleFavorite favorite = new ArticleFavorite();
            favorite.setUserId(userId);
            favorite.setArticleId(id.longValue());
            favorite.setCreateTime(new Date());
            articleFavoriteMapper.insert(favorite);
            favorited = true;
        } else {
            articleFavoriteMapper.deleteById(existing.getId());
            favorited = false;
        }
        return Result.ok(Map.of("favorited", favorited));
    }

    // ------------------------------------------------------------------
    // 以下为管理端预留的文章维护接口。不在前端契约内（前端 admin 页面目前是
    // 硬编码数据），保留是因为功能已实现且验证过。参数用表单形式提交。
    // ------------------------------------------------------------------

    @PostMapping("/save")
    public Result<ArticleResponse> save(Knowledge knowledge) {
        if (knowledge.getTitle() == null || knowledge.getTitle().isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "title 不能为空");
        }
        if (knowledge.getContent() == null || knowledge.getContent().isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "content 不能为空");
        }
        Date now = new Date();
        knowledge.setCreateTime(now);
        knowledge.setUpdateTime(now);
        if (knowledge.getViewCount() == null) {
            knowledge.setViewCount(0);
        }
        if (knowledge.getStatus() == null) {
            knowledge.setStatus(1);
        }
        knowledgeMapper.insert(knowledge);
        return Result.ok(ArticleResponse.from(knowledgeMapper.selectById(knowledge.getId(), UserContext.get())));
    }

    @PostMapping("/update")
    public Result<ArticleResponse> update(Knowledge knowledge) {
        if (knowledge.getId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "id 不能为空");
        }
        if (knowledgeMapper.selectById(knowledge.getId(), UserContext.get()) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在：id=" + knowledge.getId());
        }
        knowledge.setUpdateTime(new Date());
        knowledgeMapper.update(knowledge);
        return Result.ok(ArticleResponse.from(knowledgeMapper.selectById(knowledge.getId(), UserContext.get())));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Integer id) {
        if (knowledgeMapper.deleteById(id) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在：id=" + id);
        }
        return Result.ok();
    }
}
