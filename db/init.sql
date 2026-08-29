-- ============================================================
-- 智能题库管理系统 数据库初始化脚本
-- 环境: MySQL 8.0+ / utf8mb4
-- 使用: mysql -uroot -p < db/init.sql
-- 默认账号: admin/123456(管理员)  demo/123456(普通用户)
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
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_user (user_id),
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
  message      VARCHAR(200) DEFAULT NULL COMMENT '共享留言',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '共享时间',
  PRIMARY KEY (id),
  KEY idx_to_user (to_user_id),
  KEY idx_from_user (from_user_id),
  KEY idx_bank (bank_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='共享表';

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
  KEY idx_record (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练习答题明细表';

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

-- ============================================================
-- 种子数据
-- ============================================================

-- 账号 (密码均为 123456, BCrypt)
INSERT INTO user (id, username, password, nickname, role) VALUES
(1, 'admin', '$2a$10$.IAH.dIaEFQhU.DNoJz4Ze0clebnLDZywCmqeZtvLbXGLBpfop36S', '管理员', 0),
(2, 'demo',  '$2a$10$gykpJdkTMxe/PUXewnV5EuqO75oA3FSJC76aNJjc/QWdp0N9fDxWu', '演示用户', 1);

-- 分类 (admin: 1-8, demo: 9-10)
INSERT INTO category (id, name, parent_id, sort, user_id) VALUES
(1, '数学',     0, 1, 1),
(2, '代数',     1, 1, 1),
(3, '几何',     1, 2, 1),
(4, '英语',     0, 2, 1),
(5, '语法',     4, 1, 1),
(6, '计算机',   0, 3, 1),
(7, 'Java基础', 6, 1, 1),
(8, '数据结构', 6, 2, 1),
(9, '学习',     0, 1, 2),
(10, '网络',    9, 1, 2);

-- 题库 (admin: 1-4, demo: 5)
INSERT INTO bank (id, name, description, user_id) VALUES
(1, 'Java 入门题库', 'Java 基础入门题目', 1),
(2, '数据结构题库', '栈、队列、顺序表等', 1),
(3, '数学题库', '代数与几何', 1),
(4, '英语题库', '语法与时态练习', 1),
(5, '网络基础题库', 'HTTP/TCP 入门', 2);

-- 题目 (admin: 1-12, demo: 13-14) 多空答案用 ||| 分隔
INSERT INTO question (id, user_id, bank_id, category_id, type, title, options, answer, analysis, difficulty, tags, source) VALUES
(1, 1, 1, 7, 1, 'Java 中，下列哪个关键字用于定义类？', '["class","interface","struct","package"]', 'A', 'class 用于定义类；interface 定义接口；Java 中没有 struct；package 用于声明包。', 1, 'Java基础,关键字', '自编'),
(2, 1, 3, 2, 1, '已知 x + 3 = 7，则 x 的值为？', '["3","4","10","-4"]', 'B', '两边同时减 3，x = 7 - 3 = 4。', 1, '一元一次方程', '自编'),
(3, 1, 1, 7, 2, '下列哪些是 Java 的基本数据类型？', '["int","String","boolean","Integer"]', 'AC', 'int 和 boolean 是 8 种基本数据类型；String 是引用类型；Integer 是包装类。', 2, '数据类型', '自编'),
(4, 1, 3, 3, 2, '下列属于平面图形性质的是？', '["三角形内角和为180度","圆的周长为2πr","正方体有6个面","勾股定理"]', 'ABD', '正方体是立体图形，不属于平面图形性质；A、B、D 均为平面几何结论。', 2, '平面几何', '自编'),
(5, 1, 1, 7, 3, 'Java 中用于定义常量的关键字是____，用于类继承的关键字是____。', NULL, 'final|||extends', 'final 修饰的变量为常量；extends 用于类继承。', 2, '关键字', '自编'),
(6, 1, 4, 5, 3, '用所给动词的适当形式填空: He ____ (go) to school every day.', NULL, 'goes', '主语 He 是第三人称单数，一般现在时动词加 -es。', 1, '一般现在时', '教材'),
(7, 1, 1, 7, 4, 'Java 是一种面向对象的编程语言。', NULL, '对', 'Java 以类和对象为核心，支持封装、继承、多态。', 1, 'Java概述', '自编');

(8, 1, 3, 2, 4, '所有的质数都是奇数。', NULL, '错', '2 是质数，但 2 是偶数，所以命题不成立。', 2, '质数,奇偶性', '自编'),
(9, 1, 2, 8, 5, '简述栈和队列的区别。', NULL, '栈是后进先出(LIFO)的线性表，队列是先进先出(FIFO)的线性表。', '栈只能在一端(栈顶)操作；队列一端入队、另一端出队。', 3, '栈,队列', '自编'),
(10, 1, 2, 8, 1, '在长度为 n 的顺序表中第 i 个位置前插入一个元素，平均需要移动的元素个数为？', '["n","n/2","log n","n*n"]', 'B', '等概率下平均移动次数为 n/2。', 3, '顺序表,插入', '教材'),
(11, 1, 4, 5, 1, 'She ____ to the party last night.', '["go","goes","went","gone"]', 'C', 'last night 表示过去时间，用一般过去时 went。', 1, '一般过去时', '教材'),
(12, 1, 3, 3, 5, '简述三角形全等的判定方法（至少写出三种）。', NULL, 'SSS、SAS、ASA、AAS、HL(直角三角形)。', '任选三种作答即可，判定方法共五种。', 3, '全等三角形', '教材'),
(13, 2, 5, 10, 1, 'HTTP 协议默认使用的端口号是？', '["21","80","443","8080"]', 'B', 'HTTP 默认 80，HTTPS 默认 443，21 是 FTP。', 1, '网络协议', '自编'),
(14, 2, 5, 10, 4, 'TCP 是一种无连接的传输层协议。', NULL, '错', 'TCP 是面向连接的可靠传输协议，UDP 才是无连接的。', 1, 'TCP,UDP', '自编');

-- 收藏演示 (demo 收藏 admin 的题目)
INSERT INTO favorite (user_id, question_id) VALUES
(2, 7),
(2, 9);

-- 共享演示 (题目共享 + 题库共享)
INSERT INTO share (question_id, bank_id, from_user_id, to_user_id, share_type, message) VALUES
(9,    NULL, 1, NULL, 2, '经典面试题, 大家参考'),
(10,   NULL, 1, NULL, 2, NULL),
(1,    NULL, 1, 2,    1, '入门题, 做做看'),
(NULL, 2,    1, 2,    3, '数据结构题库, 一起学习!');

