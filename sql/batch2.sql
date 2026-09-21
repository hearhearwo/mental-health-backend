   -- ============================================================================
-- 心理健康 AI 助手 —— 第二批数据库变更
--
-- 前提：已执行过 batch1.sql。
-- 执行：
--   mysql -uroot -p mental_health_assistant < sql/batch2.sql
-- ============================================================================

USE mental_health_assistant;

-- ---------------------------------------------------------------------------
-- 1) chat_message 增加结构化字段
--    业务元数据（消息类型、危机标记、卡片内容）单独存列，不塞进 content，
--    content 只放给用户看的正文。
--    MySQL 8.0.46 支持 JSON 类型；若在 5.7 以下，把 json 改成 text。
-- ---------------------------------------------------------------------------
ALTER TABLE `chat_message`
  ADD COLUMN `type`      varchar(20) NOT NULL DEFAULT 'text' COMMENT 'text=普通文本 / card=卡片消息' AFTER `content`,
  ADD COLUMN `crisis`    tinyint     NOT NULL DEFAULT 0      COMMENT '1=危机干预回复，前端红色高亮' AFTER `type`,
  ADD COLUMN `card_data` json                 DEFAULT NULL   COMMENT '卡片内容，type=card 时有效' AFTER `crisis`;


-- ---------------------------------------------------------------------------
-- 2) emotion_log 增加来源区分 + 5 维度指标
--
--    source 是关键：日记接口只查 'diary'，状态聚合只查 'chat'，
--    否则聊天提取的记录会污染日记页的日历与趋势图。
--
--    量纲也分开：
--      emotion_score / emotion_tag  -> 用户手写日记的 1-5 分与情绪标签
--      dim_*                        -> AI 从聊天中提取的 0-100 分
--    两套互不干扰，聚合统计不会失真。
-- ---------------------------------------------------------------------------
ALTER TABLE `emotion_log`
  ADD COLUMN `source`      varchar(10) NOT NULL DEFAULT 'diary' COMMENT 'diary=用户手写日记 / chat=聊天异步提取' AFTER `user_id`,
  ADD COLUMN `dim_emotion` int DEFAULT NULL COMMENT '情绪维度 0-100' AFTER `emotion_score`,
  ADD COLUMN `dim_stress`  int DEFAULT NULL COMMENT '压力维度 0-100，越高压力越大' AFTER `dim_emotion`,
  ADD COLUMN `dim_sleep`   int DEFAULT NULL COMMENT '睡眠维度 0-100，越高睡眠越好' AFTER `dim_stress`,
  ADD COLUMN `dim_social`  int DEFAULT NULL COMMENT '社交维度 0-100' AFTER `dim_sleep`,
  ADD COLUMN `dim_crisis`  int DEFAULT NULL COMMENT '危机指数 0-100，越高风险越大' AFTER `dim_social`,
  ADD COLUMN `level`       varchar(4) DEFAULT NULL COMMENT '综合等级 L1-L5' AFTER `dim_crisis`;


-- ---------------------------------------------------------------------------
-- 3) 索引：状态聚合按 (user_id, source, create_time) 扫描
-- ---------------------------------------------------------------------------
ALTER TABLE `emotion_log`
  ADD KEY `idx_user_source_time` (`user_id`, `source`, `create_time`);


-- ---------------------------------------------------------------------------
-- 4) 校验
-- ---------------------------------------------------------------------------
SELECT 'chat_message 新列' AS item, COUNT(*) AS cnt FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'mental_health_assistant' AND TABLE_NAME = 'chat_message'
   AND COLUMN_NAME IN ('type','crisis','card_data')
UNION ALL
SELECT 'emotion_log 新列', COUNT(*) FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'mental_health_assistant' AND TABLE_NAME = 'emotion_log'
   AND COLUMN_NAME IN ('source','dim_emotion','dim_stress','dim_sleep','dim_social','dim_crisis','level');
