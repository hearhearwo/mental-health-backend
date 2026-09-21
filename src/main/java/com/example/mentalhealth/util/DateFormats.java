package com.example.mentalhealth.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 数据库用 java.util.Date，接口返回给前端用字符串，统一在这里转换。
 * DateTimeFormatter 是线程安全的，可以静态复用。
 */
public final class DateFormats {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 会话列表用：09-15 10:20 */
    private static final DateTimeFormatter MONTH_DAY_TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    /** 消息气泡用：10:18 */
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    /** 趋势图横轴用：09/09 */
    private static final DateTimeFormatter SLASH_DATE = DateTimeFormatter.ofPattern("MM/dd");

    private DateFormats() {
    }

    /** Date -> "yyyy-MM-dd"，null 安全 */
    public static String toDate(Date date) {
        return date == null ? null : toLocalDateTime(date).format(DATE);
    }

    /** Date -> "yyyy-MM-dd HH:mm:ss"，null 安全 */
    public static String toDateTime(Date date) {
        return date == null ? null : toLocalDateTime(date).format(DATE_TIME);
    }

    /** Date -> "MM-dd HH:mm"，会话列表用 */
    public static String toMonthDayTime(Date date) {
        return date == null ? null : toLocalDateTime(date).format(MONTH_DAY_TIME);
    }

    /** Date -> "HH:mm"，消息气泡用 */
    public static String toTime(Date date) {
        return date == null ? null : toLocalDateTime(date).format(TIME);
    }

    /** Date -> "MM/dd"，趋势图横轴用 */
    public static String toSlashDate(Date date) {
        return date == null ? null : toLocalDateTime(date).format(SLASH_DATE);
    }

    /** 取某个 Date 所在的自然日（用于按天聚合） */
    public static java.time.LocalDate toLocalDate(Date date) {
        return toLocalDateTime(date).toLocalDate();
    }

    /** "yyyy-MM-dd" -> Date（当天 00:00:00）；空串或 null 返回 null */
    public static Date parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        LocalDate localDate = LocalDate.parse(text.trim(), DATE);
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
