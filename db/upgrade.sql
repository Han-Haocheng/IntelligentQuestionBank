-- ============================================================
-- 智能题库管理系统 存量库升级脚本(v1 -> 最新结构, 合并 v2-v5)
-- 使用: mysql -uroot -p < db/upgrade.sql
-- 说明: 适用于已导入过 v1 结构的存量库, 一次性升级到当前结构;
--       升级语句为非幂等 DDL, 请勿重复执行; 新环境无需本脚本
-- ============================================================

-- ======================= v2: 题库分组 =======================

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
-- ======================= v3: 练习快照 =======================

-- 练习会题目快照表
CREATE TABLE IF NOT EXISTS practice_question (
  id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '会题目ID',
  record_id   BIGINT   NOT NULL COMMENT '练习记录ID',
  question_id BIGINT   NOT NULL COMMENT '题目ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_record_question (record_id, question_id),
  KEY idx_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练习会题目快照表';

-- ======================= v4: 索引/唯一键 =======================
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

-- ======================= v5: 权限/订阅 =======================

-- 共享权限: 1只读 2可编辑 (公开共享固定只读)
ALTER TABLE share ADD COLUMN permission TINYINT NOT NULL DEFAULT 1 COMMENT '1只读 2可编辑' AFTER share_type;

-- 收件人订阅状态表 (公开共享也按用户记录订阅态)
CREATE TABLE IF NOT EXISTS share_member (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '订阅ID',
  share_id    BIGINT      NOT NULL COMMENT '共享ID',
  user_id     BIGINT      NOT NULL COMMENT '收件人ID',
  subscribed  TINYINT     NOT NULL DEFAULT 1 COMMENT '1订阅中 0已退订',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_share_user (share_id, user_id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='共享订阅状态(收件人侧)';

-- 拷贝来源记录 (收件人"拷贝"生成的副本)
ALTER TABLE question ADD COLUMN origin_question_id BIGINT DEFAULT NULL COMMENT '拷贝来源题目ID' AFTER source;
ALTER TABLE bank ADD COLUMN origin_bank_id BIGINT DEFAULT NULL COMMENT '拷贝来源题库ID' AFTER description;
