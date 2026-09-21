package com.example.mentalhealth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentalhealth.dto.StatusResponse;
import com.example.mentalhealth.entity.EmotionLog;
import com.example.mentalhealth.mapper.EmotionLogMapper;
import com.example.mentalhealth.util.DateFormats;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 心理状态聚合。数据全部来自 emotion_log 里 source='chat' 的记录，
 * 不在展示时实时调用大模型——指标在用户发消息时就已经异步提取好了。
 */
@Service
public class StatusService {

    private static final DateTimeFormatter AXIS_DATE = DateTimeFormatter.ofPattern("MM/dd");
    /** 维度均值取最近多少条记录，避免久远数据拖住当前状态 */
    private static final int RECENT_WINDOW = 20;
    private static final int TREND_DAYS_MAX = 30;

    private final EmotionLogMapper emotionLogMapper;

    public StatusService(EmotionLogMapper emotionLogMapper) {
        this.emotionLogMapper = emotionLogMapper;
    }

    public StatusResponse getStatus(Long userId) {
        Date since = Date.from(LocalDate.now().minusDays(TREND_DAYS_MAX - 1L)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());

        List<EmotionLog> records = emotionLogMapper.selectList(
                new LambdaQueryWrapper<EmotionLog>()
                        .eq(EmotionLog::getUserId, userId)
                        .eq(EmotionLog::getSource, EmotionLog.SOURCE_CHAT)
                        .ge(EmotionLog::getCreateTime, since)
                        .orderByDesc(EmotionLog::getCreateTime));

        StatusResponse response = new StatusResponse();
        response.setTrend7(buildTrend(records, 7));
        response.setTrend30(buildTrend(records, TREND_DAYS_MAX));

        if (records.isEmpty()) {
            // dimensions 保持空、level 保持 null，前端据此渲染空态
            return response;
        }

        // 最近一条即当前等级。取最新而非平均，是为了让危机情况立刻反映出来
        response.setLevel(records.get(0).getLevel());

        List<EmotionLog> recent = records.size() > RECENT_WINDOW
                ? records.subList(0, RECENT_WINDOW)
                : records;
        response.setDimensions(List.of(
                dimension("情绪", recent, EmotionLog::getDimEmotion),
                dimension("压力", recent, EmotionLog::getDimStress),
                dimension("睡眠", recent, EmotionLog::getDimSleep),
                dimension("社交", recent, EmotionLog::getDimSocial),
                dimension("危机指数", recent, EmotionLog::getDimCrisis)));
        return response;
    }

    private StatusResponse.Dimension dimension(String name, List<EmotionLog> records,
                                               Function<EmotionLog, Integer> getter) {
        double average = records.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(50);   // 该维度全空时给中性值，保证雷达图能画出形状
        return new StatusResponse.Dimension(name, (int) Math.round(average));
    }

    private StatusResponse.Trend buildTrend(List<EmotionLog> records, int days) {
        Map<LocalDate, List<Integer>> levelsByDay = new HashMap<>();
        for (EmotionLog record : records) {
            if (record.getCreateTime() == null) {
                continue;
            }
            int level = parseLevel(record.getLevel());
            if (level > 0) {
                levelsByDay.computeIfAbsent(DateFormats.toLocalDate(record.getCreateTime()),
                        key -> new ArrayList<>()).add(level);
            }
        }

        StatusResponse.Trend trend = new StatusResponse.Trend();
        LocalDate today = LocalDate.now();
        for (int offset = days - 1; offset >= 0; offset--) {
            LocalDate day = today.minusDays(offset);
            trend.getDates().add(day.format(AXIS_DATE));
            List<Integer> levels = levelsByDay.get(day);
            if (levels == null || levels.isEmpty()) {
                // 该天没有对话记录，留空点，折线上表现为断点
                trend.getLevels().add(null);
            } else {
                trend.getLevels().add((int) Math.round(
                        levels.stream().mapToInt(Integer::intValue).average().orElse(0)));
            }
        }
        return trend;
    }

    /** "L3" -> 3，无法解析返回 0 */
    private static int parseLevel(String level) {
        if (level == null || level.length() < 2) {
            return 0;
        }
        try {
            return Integer.parseInt(level.substring(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
