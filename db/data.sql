-- ============================================================
-- 智能题库管理系统 数据脚本(演示数据)
-- 环境: MySQL 8.0+ / utf8mb4
-- 使用: mysql -uroot -p < db/data.sql (需先执行 db/schema.sql)
-- 默认账号: admin/123456(管理员)  demo/123456(普通用户)
-- 说明: 采用「先按种子主键/唯一标识删除, 再插入」的幂等方式,
--       可安全重复执行; 只重置种子行, 不影响用户自建的数据;
--       如需完全重置全部数据, 请删除库后重新导入 schema.sql
-- ============================================================
SET NAMES utf8mb4;
USE question_bank;

-- ============ 1. 清除种子数据(可重复执行) ============
DELETE FROM share WHERE (from_user_id = 1 AND share_type = 2 AND question_id IN (9, 10))
   OR (from_user_id = 1 AND share_type = 1 AND question_id = 1 AND to_user_id = 2)
   OR (from_user_id = 1 AND share_type = 3 AND bank_id = 2 AND to_user_id = 2);
DELETE FROM favorite WHERE user_id = 2 AND question_id IN (7, 9);
DELETE FROM question WHERE id BETWEEN 1 AND 14;
DELETE FROM bank WHERE id BETWEEN 1 AND 5;
DELETE FROM category WHERE id BETWEEN 1 AND 10;
DELETE FROM user WHERE id BETWEEN 1 AND 2;

-- ============ 2. 插入种子数据 ============

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
(7, 1, 1, 7, 4, 'Java 是一种面向对象的编程语言。', NULL, '对', 'Java 以类和对象为核心，支持封装、继承、多态。', 1, 'Java概述', '自编'),
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
