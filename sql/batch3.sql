-- ============================================================================
-- 心理健康 AI 助手 —— 第三批数据库变更（邮箱注册）
--
-- 前提：已执行过 batch1.sql、batch2.sql。
-- 执行：
--   mysql -uroot -p mental_health_assistant < sql/batch3.sql
--
-- 说明：本批不需要新建表，只给 user 加一列。
--       验证码放内存缓存（Caffeine），不入库。
-- ============================================================================

USE mental_health_assistant;

-- ---------------------------------------------------------------------------
-- 1) user 增加邮箱列
--
--    与 username 的关系：
--      username —— 登录名，注册时直接填邮箱，是身份标识
--      email    —— 联系方式，将来做找回密码、邮件通知用
--    两者现在值相同，但语义不同、将来会分叉（比如用户改邮箱时不该动登录名）。
--
--    允许 NULL：存量用户没有邮箱，MySQL 的唯一索引允许多个 NULL，不冲突。
-- ---------------------------------------------------------------------------
ALTER TABLE `user`
  ADD COLUMN `email` varchar(100) DEFAULT NULL COMMENT '邮箱，注册与登录用' AFTER `phone`;

ALTER TABLE `user`
  ADD UNIQUE KEY `uk_email` (`email`);


-- ---------------------------------------------------------------------------
-- 2) 校验
-- ---------------------------------------------------------------------------
SELECT 'email 列' AS item, COUNT(*) AS cnt FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'mental_health_assistant' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'email'
UNION ALL
SELECT 'uk_email 唯一索引', COUNT(*) FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = 'mental_health_assistant' AND TABLE_NAME = 'user' AND INDEX_NAME = 'uk_email';
