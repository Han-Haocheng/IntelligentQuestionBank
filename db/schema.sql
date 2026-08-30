-- ============================================================
-- 智能题库管理系统 数据库结构脚本(schema)
-- 环境: MySQL 8.0+ / utf8mb4
-- 使用: mysql -uroot -p < db/schema.sql
-- 说明: 仅创建库与表结构(IF NOT EXISTS, 可重复执行), 不含数据
--       演示数据请导入 db/data.sql
-- ============================================================
CREATE DATABASE IF NOT EXISTS question_bank DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE question_bank;

-- 用户表 (role: 0-管理员 1-普通用户; status: 1-正常 0-禁用)
CREATE TABLE IF NOT EXISTS user (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username    VARCHAR(50)  NOT NULL COMMENT '用户名',
  password    VARCHAR(128) NOT NULL COMMENT '密码(BCrypt 哈希)',
  -- 存量 v1 账号仍为 "盐:sha256" 格式, 登录成功后会由后端自动升级为 BCrypt
  nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
  email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  role        TINYINT      NOT NULL DEFAULT 1 COMMENT '角色 0管理员 1用户',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1正常 0禁用',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 分类表 (parent_id=0 为顶级, 支持二级树)
CREATE TABLE IF NOT EXISTS category (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  name        VARCHAR(50) NOT NULL COMMENT '分类名称',
  parent_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '父分类ID, 0为顶级',
  sort        INT         NOT NULL DEFAULT 0 COMMENT '排序号',
  user_id     BIGINT      NOT NULL COMMENT '所属用户',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_user (user_id),
  KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目分类表';

-- 题库表 (题目分组, 支持整库共享)
CREATE TABLE IF NOT EXISTS bank (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题库ID',
  name        VARCHAR(100) NOT NULL COMMENT '题库名称',
  description VARCHAR(500) DEFAULT NULL COMMENT '题库描述',
  origin_bank_id BIGINT    DEFAULT NULL COMMENT '拷贝来源题库ID',
  user_id     BIGINT       NOT NULL COMMENT '所属用户',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_name (user_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库表';

-- 题目表 (type: 1单选 2多选 3填空 4判断 5简答; difficulty: 1~5)
CREATE TABLE IF NOT EXISTS question (
  id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '题目ID',
  user_id     BIGINT        NOT NULL COMMENT '所属用户',
  category_id BIGINT        DEFAULT NULL COMMENT '分类ID',
  bank_id     BIGINT        DEFAULT NULL COMMENT '所属题库',
  type        TINYINT       NOT NULL DEFAULT 1 COMMENT '题型 1单选 2多选 3填空 4判断 5简答',
  title       VARCHAR(2000) NOT NULL COMMENT '题干',
  options     TEXT          DEFAULT NULL COMMENT '选项(JSON数组,选择类题型使用)',
  answer      VARCHAR(500)  DEFAULT NULL COMMENT '参考答案',
  analysis    TEXT          DEFAULT NULL COMMENT '答案解析',
  difficulty  TINYINT       NOT NULL DEFAULT 3 COMMENT '难度 1~5',
  tags        VARCHAR(255)  DEFAULT NULL COMMENT '知识点标签,逗号分隔',
  source      VARCHAR(100)  DEFAULT NULL COMMENT '题目来源',
  origin_question_id BIGINT DEFAULT NULL COMMENT '拷贝来源题目ID',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_user (user_id),
  KEY idx_user_type (user_id, type),
  KEY idx_user_difficulty (user_id, difficulty),
  KEY idx_category (category_id),
  KEY idx_bank (bank_id),
  KEY idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- 收藏表
CREATE TABLE IF NOT EXISTS favorite (
  id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  user_id     BIGINT   NOT NULL COMMENT '用户ID',
  question_id BIGINT   NOT NULL COMMENT '题目ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_question (user_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- 共享表 (share_type: 1指定用户-题目 2公开-题目 3指定用户-题库 4公开-题库)
CREATE TABLE IF NOT EXISTS share (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '共享ID',
  question_id  BIGINT       DEFAULT NULL COMMENT '题目ID(题库共享时为NULL)',
  bank_id      BIGINT       DEFAULT NULL COMMENT '题库ID(题目共享时为NULL)',
  from_user_id BIGINT       NOT NULL COMMENT '共享人ID',
  to_user_id   BIGINT       DEFAULT NULL COMMENT '接收人ID(公开共享为NULL)',
  share_type   TINYINT      NOT NULL DEFAULT 1 COMMENT '1指定用户-题目 2公开-题目 3指定用户-题库 4公开-题库',
  permission   TINYINT      NOT NULL DEFAULT 1 COMMENT '1只读 2可编辑(公开共享固定只读)',
  message      VARCHAR(200) DEFAULT NULL COMMENT '共享留言',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '共享时间',
  PRIMARY KEY (id),
  KEY idx_to_user (to_user_id),
  KEY idx_from_user (from_user_id),
  KEY idx_question (question_id),
  KEY idx_bank (bank_id),
  UNIQUE KEY uk_share_question_target (from_user_id, question_id, to_user_id, share_type),
  UNIQUE KEY uk_share_bank_target (from_user_id, bank_id, to_user_id, share_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='共享表';

-- 共享订阅状态表 (收件人侧: 订阅/退订; 公开共享也按用户记录)
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

-- 练习记录表 (mode: 1-顺序 2-随机 3-错题重做)
CREATE TABLE IF NOT EXISTS practice_record (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '练习ID',
  user_id     BIGINT       NOT NULL COMMENT '用户ID',
  name        VARCHAR(100) DEFAULT NULL COMMENT '练习名称',
  mode        TINYINT      NOT NULL DEFAULT 1 COMMENT '模式 1顺序 2随机 3错题重做',
  category_id BIGINT       DEFAULT NULL COMMENT '限定分类',
  total       INT          NOT NULL DEFAULT 0 COMMENT '题目总数',
  correct     INT          NOT NULL DEFAULT 0 COMMENT '答对数量',
  duration    INT          NOT NULL DEFAULT 0 COMMENT '用时(秒)',
  status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0进行中 1已完成',
  start_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  finish_time DATETIME     DEFAULT NULL COMMENT '交卷时间',
  PRIMARY KEY (id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练习记录表';

-- 练习答题明细表
CREATE TABLE IF NOT EXISTS practice_answer (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  record_id   BIGINT       NOT NULL COMMENT '练习ID',
  question_id BIGINT       NOT NULL COMMENT '题目ID',
  user_id     BIGINT       NOT NULL COMMENT '用户ID',
  user_answer VARCHAR(500) DEFAULT NULL COMMENT '用户答案',
  is_correct  TINYINT      NOT NULL DEFAULT 0 COMMENT '0答错 1答对',
  answer_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '答题时间',
  PRIMARY KEY (id),
  KEY idx_record (record_id),
  KEY idx_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练习答题明细表';

-- 练习会题目快照表 (记录每次练习包含的题目, 交卷时校验归属, 防止提交无关题目)
CREATE TABLE IF NOT EXISTS practice_question (
  id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '会题目ID',
  record_id   BIGINT   NOT NULL COMMENT '练习记录ID',
  question_id BIGINT   NOT NULL COMMENT '题目ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_record_question (record_id, question_id),
  KEY idx_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练习会题目快照表';

-- 错题本
CREATE TABLE IF NOT EXISTS wrong_question (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '错题ID',
  user_id         BIGINT       NOT NULL COMMENT '用户ID',
  question_id     BIGINT       NOT NULL COMMENT '题目ID',
  wrong_count     INT          NOT NULL DEFAULT 1 COMMENT '错误次数',
  last_answer     VARCHAR(500) DEFAULT NULL COMMENT '最近一次错误答案',
  mastered        TINYINT      NOT NULL DEFAULT 0 COMMENT '0未掌握 1已掌握',
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次加入时间',
  last_wrong_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近错误时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_question (user_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本';

-- v2 起 AI 分析在前端本地完成, 后端不再存储 AI 记录
