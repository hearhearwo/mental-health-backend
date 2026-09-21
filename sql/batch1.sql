-- ============================================================================
-- 心理健康 AI 助手 —— 第一批数据库变更
--
-- 执行前请确认：数据库为 mental_health_assistant，且已备份。
-- 注意：MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，本脚本重复执行会在第 2 段报错
--      （表已存在），属于正常现象。
--
-- 用法：
--   mysql -uroot -p mental_health_assistant < sql/batch1.sql
-- ============================================================================

USE mental_health_assistant;

-- ---------------------------------------------------------------------------
-- 1) 文章收藏表
--    前端每篇文章都要返回 favorited 字段，原来没有存储的地方，新建此表。
--    uk_user_article 唯一索引同时起到「防止重复收藏」和「加速联表查询」两个作用。
-- ---------------------------------------------------------------------------
CREATE TABLE `article_favorite` (
  `id`          bigint   NOT NULL AUTO_INCREMENT,
  `user_id`     bigint   NOT NULL COMMENT '用户 id',
  `article_id`  bigint   NOT NULL COMMENT '文章 id（对应 knowledge.id）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_article` (`user_id`, `article_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_article` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章收藏表';


-- ---------------------------------------------------------------------------
-- 2) knowledge 补充文章来源字段
--    前端文章详情页脚要展示来源，原表没有这个列。
-- ---------------------------------------------------------------------------
ALTER TABLE `knowledge`
  ADD COLUMN `source` varchar(255) DEFAULT NULL COMMENT '文章来源' AFTER `category`;


-- ---------------------------------------------------------------------------
-- 3) 存量用户密码明文 -> BCrypt（可选，按需执行）
--
--    公开仓库里不放真实哈希：原密码是很弱的 6 位数字，
--    哈希公开后等于把密码也公开了。请自己生成后替换下面的占位符。
--
--    生成方式（任选其一）：
--      a) 用本项目的 BCryptPasswordEncoder 写几行代码 encode 一下
--      b) 用任意在线 BCrypt 工具，cost 设 10
--
--    拿到哈希后，取消下面两行的注释并替换 <BCRYPT_HASH> 再执行。
-- ---------------------------------------------------------------------------
-- UPDATE `user`
-- SET `password` = '<BCRYPT_HASH>'
-- WHERE `username` = 'admin';


-- ---------------------------------------------------------------------------
-- 4) 校验
-- ---------------------------------------------------------------------------
SELECT '文章收藏表' AS item, COUNT(*) AS cnt FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = 'mental_health_assistant' AND TABLE_NAME = 'article_favorite'
UNION ALL
SELECT 'knowledge.source 列', COUNT(*) FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'mental_health_assistant' AND TABLE_NAME = 'knowledge' AND COLUMN_NAME = 'source'
UNION ALL
SELECT '密码已是 BCrypt', COUNT(*) FROM `user`
 WHERE `username` = 'admin' AND `password` LIKE '$2a$%';
