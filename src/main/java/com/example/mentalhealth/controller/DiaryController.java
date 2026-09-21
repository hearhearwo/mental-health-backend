package com.example.mentalhealth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentalhealth.common.BusinessException;
import com.example.mentalhealth.common.Result;
import com.example.mentalhealth.common.ResultCode;
import com.example.mentalhealth.common.UserContext;
import com.example.mentalhealth.dto.DiaryRequest;
import com.example.mentalhealth.dto.DiaryResponse;
import com.example.mentalhealth.entity.EmotionLog;
import com.example.mentalhealth.mapper.EmotionLogMapper;
import com.example.mentalhealth.util.DateFormats;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/diaries")
public class DiaryController {

    private final EmotionLogMapper emotionLogMapper;

    public DiaryController(EmotionLogMapper emotionLogMapper) {
        this.emotionLogMapper = emotionLogMapper;
    }

    /**
     * 只返回当前登录用户手写的日记。
     * source 过滤不能少：emotion_log 里还存着聊天消息异步提取的指标记录，
     * 不过滤的话日记页会混进用户从没写过的"日记"。
     */
    @GetMapping
    public Result<List<DiaryResponse>> list() {
        Long userId = UserContext.require();
        List<DiaryResponse> data = emotionLogMapper.selectList(
                        new LambdaQueryWrapper<EmotionLog>()
                                .eq(EmotionLog::getUserId, userId)
                                .eq(EmotionLog::getSource, EmotionLog.SOURCE_DIARY)
                                .orderByDesc(EmotionLog::getCreateTime))
                .stream()
                .map(DiaryResponse::from)
                .toList();
        return Result.ok(data);
    }

    @PostMapping
    public Result<DiaryResponse> create(@RequestBody DiaryRequest request) {
        Long userId = UserContext.require();
        if (!StringUtils.hasText(request.getContent())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "日记内容不能为空");
        }
        if (request.getScore() != null && (request.getScore() < 1 || request.getScore() > 5)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "情绪评分需要在 1-5 之间");
        }

        EmotionLog log = new EmotionLog();
        log.setUserId(userId);
        // 显式写死，不依赖数据库默认值
        log.setSource(EmotionLog.SOURCE_DIARY);
        log.setContent(request.getContent());
        log.setEmotionTag(request.getMood());
        log.setEmotionScore(request.getScore());

        // 前端可以补记过去的日期；不传就用当前时间
        Date diaryDate = DateFormats.parseDate(request.getDate());
        Date now = new Date();
        log.setCreateTime(diaryDate != null ? diaryDate : now);
        log.setUpdateTime(now);

        emotionLogMapper.insert(log);
        return Result.ok(DiaryResponse.from(log));
    }
}
