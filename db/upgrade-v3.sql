-- ============================================================
-- v3 升级脚本: 练习会题目快照表 (交卷完整性校验)
-- 使用: mysql -uroot -p < db/upgrade-v3.sql
-- ============================================================
USE question_bank;

-- 练习会题目快照表
CREATE TABLE IF NOT EXISTS practice_question (
  id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '会题目ID',
  record_id   BIGINT   NOT NULL COMMENT '练习记录ID',
  question_id BIGINT   NOT NULL COMMENT '题目ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_record_question (record_id, question_id),
  KEY idx_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练习会题目快照表';
