-- ============================================================
-- v2 升级脚本: 题库分组功能 (已导入过 v1 库的环境执行本文件)
-- 使用: mysql -uroot -p < db/upgrade-v2.sql
-- ============================================================
USE question_bank;

-- 题库表
CREATE TABLE IF NOT EXISTS bank (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题库ID',
  name        VARCHAR(100) NOT NULL COMMENT '题库名称',
  description VARCHAR(500) DEFAULT NULL COMMENT '题库描述',
  user_id     BIGINT       NOT NULL COMMENT '所属用户',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_name (user_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库表';

-- 题目归属题库
ALTER TABLE question ADD COLUMN bank_id BIGINT DEFAULT NULL COMMENT '所属题库' AFTER category_id;
ALTER TABLE question ADD KEY idx_bank (bank_id);

-- 共享表支持题库共享 (share_type: 1指定用户-题目 2公开-题目 3指定用户-题库 4公开-题库)
ALTER TABLE share MODIFY question_id BIGINT DEFAULT NULL COMMENT '题目ID(题库共享时为NULL)';
ALTER TABLE share ADD COLUMN bank_id BIGINT DEFAULT NULL COMMENT '题库ID(题库共享时)';
ALTER TABLE share ADD KEY idx_bank (bank_id);
ALTER TABLE share MODIFY share_type TINYINT NOT NULL DEFAULT 1 COMMENT '1指定用户-题目 2公开-题目 3指定用户-题库 4公开-题库';

-- v2 起 AI 分析迁移到前端本地, 后端不再存储 AI 记录
DROP TABLE IF EXISTS ai_analysis;