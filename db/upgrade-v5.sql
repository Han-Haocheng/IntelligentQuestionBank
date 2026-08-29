-- ============================================================
-- v5 升级脚本: 共享权限 + 订阅状态 + 拷贝来源 (v1.1)
-- 使用: mysql -uroot -p < db/upgrade-v5.sql
-- ============================================================
USE question_bank;

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
