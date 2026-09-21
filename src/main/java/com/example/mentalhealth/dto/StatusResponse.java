package com.example.mentalhealth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 心理状态，由 emotion_log 里 source='chat' 的记录聚合而来。
 *
 * 前端期望：
 * {
 *   level: 'L2',
 *   dimensions: [{ name: '情绪', score: 76 }, ...],
 *   trend7:  { dates: ['09/09', ...], levels: [2, 2, 3, ...] },
 *   trend30: { dates: [...], levels: [...] }
 * }
 *
 * 没有数据时 dimensions 为空数组、level 为 null，前端据此渲染空态。
 * 趋势里某天没有记录时该点为 null，折线图上表现为断点。
 */
@Data
public class StatusResponse {

    /** L1-L5，无数据时为 null */
    private String level;
    private List<Dimension> dimensions = new ArrayList<>();
    private Trend trend7 = new Trend();
    private Trend trend30 = new Trend();

    @Data
    @AllArgsConstructor
    public static class Dimension {
        private String name;
        private Integer score;
    }

    @Data
    public static class Trend {
        private List<String> dates = new ArrayList<>();
        /** 与 dates 一一对应，取值为 1-5；该天无数据则为 null */
        private List<Integer> levels = new ArrayList<>();
    }
}
