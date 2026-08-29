-- ============================================================
-- v4 升级脚本: 索引补充 + 共享表防重唯一键
-- 使用: mysql -uroot -p < db/upgrade-v4.sql
-- 说明: 指定用户共享(share_type 1/3)由唯一键防重;
--       公开共享(share_type 2/4, to_user_id 为 NULL)由后端条件插入防重
-- ============================================================
USE question_bank;

-- 练习筛选高频查询复合索引
ALTER TABLE question ADD KEY idx_user_type (user_id, type), ADD KEY idx_user_difficulty (user_id, difficulty);

-- 共享表: 题目维度查询索引 + 防重唯一键
ALTER TABLE share ADD KEY idx_question (question_id),
    ADD UNIQUE KEY uk_share_question_target (from_user_id, question_id, to_user_id, share_type),
    ADD UNIQUE KEY uk_share_bank_target (from_user_id, bank_id, to_user_id, share_type);

-- 答题明细按题目查询索引
ALTER TABLE practice_answer ADD KEY idx_question (question_id);
