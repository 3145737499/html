-- ============================================================
-- 离恨烟·烟雨阁 —— 首页搜索/展示功能建表脚本（2026-10-14）
-- 表：app_category（分级分类，parent_id 构成两级树） + app_item（内容条目）
-- 用法：mysql -uroot -p123456 --default-character-set=utf8mb4 boonie < search.sql
-- 说明：本脚本同时已追加到 sql\boonie.sql 末尾，冷启动自动初始化
-- ============================================================

USE `boonie`;

-- ------------------------------------------------------------
-- 分级分类表 app_category
-- parent_id = 0 为顶级分类；子分类通过 parent_id 挂到父级
-- code 与前端 HomeView 导航项 key 一致（jianghu/umbrella/...）
-- 对应实体 com.fenglin.springboottest.entity.Category
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `app_item`;
DROP TABLE IF EXISTS `app_category`;
CREATE TABLE `app_category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id`   BIGINT       NOT NULL DEFAULT 0      COMMENT '父分类ID（0=顶级）',
  `code`        VARCHAR(32)  NOT NULL                COMMENT '分类编码（前端导航 key）',
  `name`        VARCHAR(64)  NOT NULL                COMMENT '分类名称',
  `sort_order`  INT          NOT NULL DEFAULT 0      COMMENT '排序（小在前）',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='内容分类表（分级）';

-- ------------------------------------------------------------
-- 内容条目表 app_item
-- category_id 指向 app_category.id（可挂任意层级，搜索时按层级联查）
-- hot=1 进热门搜索词；featured=1 进首页精选
-- 对应实体 com.fenglin.springboottest.entity.AppItem
-- ------------------------------------------------------------
CREATE TABLE `app_item` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_id` BIGINT       NOT NULL                COMMENT '所属分类ID（app_category.id）',
  `icon`        VARCHAR(16)  DEFAULT '🌂'            COMMENT '图标（emoji）',
  `title`       VARCHAR(128) NOT NULL                COMMENT '标题',
  `summary`     VARCHAR(512) DEFAULT NULL            COMMENT '简介（前端展示用 desc）',
  `detail`      TEXT                                 COMMENT '详情（预留）',
  `hot`         TINYINT      NOT NULL DEFAULT 0      COMMENT '热门搜索词 0/1',
  `featured`    TINYINT      NOT NULL DEFAULT 0      COMMENT '首页精选 0/1',
  `sort_order`  INT          NOT NULL DEFAULT 0      COMMENT '排序（小在前）',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='内容条目表';

-- ------------------------------------------------------------
-- 种子数据：6 个顶级分类 + 12 个子分类
-- ------------------------------------------------------------
INSERT INTO `app_category` (`id`, `parent_id`, `code`, `name`, `sort_order`) VALUES
(1,  0, 'jianghu', '江湖',     1),
(2,  0, 'umbrella', '油纸伞',   2),
(3,  0, 'badge',   '江湖徽章', 3),
(4,  0, 'log',     '行侠日志', 4),
(5,  0, 'friend',  '侠客同游', 5),
(6,  0, 'setting', '账号设置', 6),
(7,  1, 'news',    '江湖动态', 1),
(8,  1, 'sect',    '门派名录', 2),
(9,  2, 'mine',    '我的纸伞', 1),
(10, 2, 'art',     '伞面丹青', 2),
(11, 3, 'collect', '徽章收集', 1),
(12, 3, 'seal',    '朱砂印鉴', 2),
(13, 4, 'diary',   '行侠日志', 1),
(14, 4, 'note',    '随笔小记', 2),
(15, 5, 'mate',    '侠客结识', 1),
(16, 5, 'letter',  '飞鸽传书', 2),
(17, 6, 'account', '账号设置', 1),
(18, 6, 'notify',  '消息提醒', 2);

-- ------------------------------------------------------------
-- 种子数据：内容条目（原前端本地 12 条 + 扩充子类条目）
-- 扩充条目用于体现「分级分类」：按顶级分类搜索会连带子类条目一起返回
-- ------------------------------------------------------------
INSERT INTO `app_item` (`category_id`, `icon`, `title`, `summary`, `hot`, `featured`, `sort_order`) VALUES
-- 油纸伞 / 我的纸伞
(9,  '🌂', '我的油纸伞', '查看你的纸伞与伞面丹青进度，撑一把属于自己的烟雨。', 1, 0, 1),
(9,  '🧴', '纸伞保养', '桐油三遍、伞骨轻抚，让你的纸伞多用十年。', 0, 0, 2),
-- 油纸伞 / 伞面丹青
(10, '🎨', '伞面丹青', '在伞面上挥毫泼墨，绘制专属江湖山水。', 1, 0, 1),
(10, '🌸', '节气伞面', '依二十四节气更换伞面丹青，春分桃花、夏至青荷。', 0, 1, 2),
-- 江湖 / 江湖动态
(7,  '⚔️', '江湖动态', '阅尽四方侠客的行侠见闻与门派快讯。', 1, 0, 1),
(7,  '🌦️', '烟雨快报', '每日烟雨指数播报，出门带伞提醒。', 0, 0, 2),
-- 江湖 / 门派名录
(8,  '🏯', '门派名录', '青绿山水间的大小门派，择一而栖。', 1, 0, 1),
(8,  '⛰️', '青山剑派', '立派三百年，剑法如青山连绵不绝，入门先学听雨。', 0, 1, 2),
(8,  '🏮', '烟雨楼', '江南第一消息集散地，一壶清茶换半城传闻。', 0, 1, 3),
(8,  '🍵', '听雨小筑', '隐世医馆，只医有缘人，檐下常备热茶。', 0, 0, 4),
-- 江湖徽章 / 徽章收集
(11, '🏅', '江湖徽章', '已收集 12 枚，继续行侠解锁更多鎏金徽章。', 0, 0, 1),
(11, '📜', '徽章图鉴', '鎏金徽章全图鉴，收录每一枚的来历故事。', 0, 0, 2),
-- 江湖徽章 / 朱砂印鉴
(12, '🔖', '朱砂印鉴', '完成奇遇获得的朱砂印鉴，皆收于此处。', 0, 0, 1),
-- 行侠日志 / 行侠日志
(13, '📖', '行侠日志', '以笔墨记录每一次伞下奇遇与山水相逢。', 1, 0, 1),
(13, '🗺️', '行侠路线', '青绿山水间的行侠路线图，标注歇脚茶铺与渡口。', 0, 1, 2),
-- 行侠日志 / 随笔小记
(14, '✍️', '随笔小记', '随手记下烟雨中的灵光一现。', 0, 0, 1),
-- 侠客同游 / 侠客结识
(15, '👫', '侠客同游', '结识四方侠客，共赏青绿山水，同赴惊鸿之约。', 0, 0, 1),
(15, '🏆', '侠客排行', '本月伞下听雨榜，看谁是烟雨第一闲人。', 1, 1, 2),
-- 侠客同游 / 飞鸽传书
(16, '💌', '飞鸽传书', '与江湖好友往来书信，传递伞下温情。', 0, 0, 1),
(16, '🕊️', '急救飞信', '江湖遇险一键飞鸽，十里之内必有侠客来援。', 0, 0, 2),
-- 账号设置 / 账号设置
(17, '⚙️', '账号设置', '管理名号、密码与登录方式。', 0, 0, 1),
-- 账号设置 / 消息提醒
(18, '🔔', '消息提醒', '配置江湖动态与好友消息的提醒偏好。', 0, 0, 1);
