-- ============================================================
-- 离恨烟·烟雨阁（boonie）数据库初始化脚本
-- 项目：springBootTest（后端） + boonie-vue（前端）
-- 数据库：MySQL 8.0，账号 root / 123456
-- 用法：mysql -uroot -p123456 < boonie.sql
-- ============================================================

-- 创建数据库（utf8mb4 支持中文与 emoji）
CREATE DATABASE IF NOT EXISTS `boonie`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `boonie`;

-- ------------------------------------------------------------
-- 用户表 app_user
-- 对应实体类 com.fenglin.springboottest.entity.User (@TableName("app_user"))
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `app_user`;
CREATE TABLE `app_user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account`     VARCHAR(64)  NOT NULL                COMMENT '登录账号（邮箱或手机号）',
  `password`    VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt 加密存储）',
  `email`       VARCHAR(64)  DEFAULT NULL            COMMENT '邮箱',
  `phone`       VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
  `nickname`    VARCHAR(64)  DEFAULT NULL            COMMENT '昵称',
  `role`        VARCHAR(16)  NOT NULL DEFAULT 'user' COMMENT '角色：user=普通用户, admin=后台管理员',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account` (`account`),
  KEY `idx_email` (`email`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='用户表';

-- ------------------------------------------------------------
-- 种子数据：演示账号
-- 账号 demo@boonie.com，密码 123456（BCrypt 已加密）
-- ------------------------------------------------------------
INSERT INTO `app_user` (`account`, `password`, `email`, `nickname`, `role`) VALUES
('demo@boonie.com', '$2a$10$44/5E6LE8QMVfjA5plfxEOP34.sM80Vwga2SMvCYt0ucN2aUZY/Yy', 'demo@boonie.com', '烟雨阁少侠', 'user');

-- ------------------------------------------------------------
-- 后台管理员表 admin_user（与前台 app_user 完全隔离）
-- AuthServiceImpl.getRole/isAdmin 依据「账号是否存在于此表」判定管理员，
-- 缺失该表会导致每次登录/查询角色都报 Table 'boonie.admin_user' doesn't exist
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `admin_user`;
CREATE TABLE `admin_user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account`     VARCHAR(64)  NOT NULL                COMMENT '登录账号',
  `password`    VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt 加密存储）',
  `nickname`    VARCHAR(64)  DEFAULT NULL            COMMENT '昵称',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_account` (`account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='后台管理员表';

-- 默认管理员：admin@boonie.com / 123456（BCrypt 哈希，与演示账号同款密码）
INSERT INTO `admin_user` (`account`, `password`, `nickname`) VALUES
('admin@boonie.com', '$2a$10$44/5E6LE8QMVfjA5plfxEOP34.sM80Vwga2SMvCYt0ucN2aUZY/Yy', '阁主');

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
-- ------------------------------------------------------------
-- 详情正文种子（2026-10-14 追加）：为全部 22 条条目补充 detail 字段
-- 供详情页 /api/item/{id} 展示；段落之间用 \n 分隔
-- ------------------------------------------------------------

-- 我的纸伞
UPDATE `app_item` SET `detail`='每一把油纸伞都记录着主人的江湖。\n伞骨取自三年生楠竹，伞面覆以皮纸、刷足三遍桐油，方能撑得起江南一整季的烟雨。\n在这里查看你的纸伞品阶、伞面丹青进度与历次修缮记录——伞虽无言，伞下皆是故事。' WHERE `title`='我的油纸伞';
UPDATE `app_item` SET `detail`='纸伞如剑，须常拂拭。\n雨后须撑开阴干，切忌暴晒；桐油每岁一补，伞骨关节处薄施蜡油。\n小筑匠人总结了七条保养口诀，照做一把伞多用十年，江湖人称「伞养人，人养伞」。' WHERE `title`='纸伞保养';
UPDATE `app_item` SET `detail`='一方伞面，即一方山水。\n取青绿设色，以伞骨为皴、烟雨为染，泼墨于皮纸之上，画的是山水，落的是心境。\n每月丹青上品将悬于烟雨楼回廊，供四方侠客品评，获赞最多者可得鎏金伞骨一副。' WHERE `title`='伞面丹青';
UPDATE `app_item` SET `detail`='节气更迭，伞面随之。\n春分画桃花两枝，夏至点青荷一盏，秋分染桂香满伞，冬至留白等一场雪落。\n依节气更换伞面丹青，是烟雨阁延续百年的雅事——伞在手中，四季在眼中。' WHERE `title`='节气伞面';

-- 江湖动态 / 门派名录
UPDATE `app_item` SET `detail`='江湖每日都有新的传闻：谁家剑客雨夜过江，谁派掌门闭关练气。\n此处汇总四方侠客的行侠见闻与各门派快讯，一页阅尽江湖事。\n传闻七分真三分假，信与不信，全看阁下的江湖阅历。' WHERE `title`='江湖动态';
UPDATE `app_item` SET `detail`='今日烟雨指数：江南七成，塞北二成。\n出门宜带伞，宜访友，宜在檐下听一场雨；不宜赶路，不宜动刀兵。\n烟雨快报每日辰时更新，来自烟雨楼分布于各地的听雨郎。' WHERE `title`='烟雨快报';
UPDATE `app_item` SET `detail`='青绿山水之间，大小门派林立：有立派三百年的青山剑派，也有只医有缘人的听雨小筑。\n名录按剑、医、乐、画四艺分册，各派渊源、入门规矩、看家本事皆一一在册。\n择一派而栖，是初入江湖的头等大事，不妨多看几页再定。' WHERE `title`='门派名录';
UPDATE `app_item` SET `detail`='青山剑派立派三百年，剑法讲究「连绵不绝」。\n入门第一课不在剑上，而在檐下——听满一百场雨，方许摸剑。\n派中弟子个个惜伞如金，据说剑意第七重，是在伞下悟出来的。' WHERE `title`='青山剑派';
UPDATE `app_item` SET `detail`='烟雨楼，江南第一消息集散地。\n一壶清茶，可换半城传闻；三盏淡酒，能买一个名字的下落。\n楼中规矩只有一条：只谈江湖，不问出处。檐下听雨的，皆是过客，也皆是故人。' WHERE `title`='烟雨楼';
UPDATE `app_item` SET `detail`='隐世医馆，不悬壶，不挂牌，只医有缘人。\n听雨小筑的主人医术通神却常年闭门，据说只有雨夜叩门三声者，方能得见。\n檐下常备热茶与姜汤，路过的侠客哪怕不看病，也可歇脚避雨。' WHERE `title`='听雨小筑';

-- 徽章收集 / 朱砂印鉴
UPDATE `app_item` SET `detail`='行侠积德，徽章为证。\n救困、扶危、守约、义送，每桩善举皆镌一枚鎏金徽章。目前阁中已收录一十二枚，余下诸枚散落江湖各处。\n集齐一套者，烟雨楼将为其立传一页，青史留名。' WHERE `title`='江湖徽章';
UPDATE `app_item` SET `detail`='每一枚徽章背后，都有一段来历。\n「渡口」来自雨夜摆渡三百趟，「守约」来自十年之期风雨无阻，「义送」来自千里护送不取分文。\n图鉴收录每一枚的纹样、成色与故事，闲时翻阅，如会故人。' WHERE `title`='徽章图鉴';
UPDATE `app_item` SET `detail`='朱砂印鉴，只赠有奇缘者。\n或在山洞拾得失谱，或于江心救起故人，或在破庙躲雨时误入前朝秘境——凡此种种，皆可获印一方。\n朱砂浸骨，遇雨不褪，是江湖中最硬的信用。' WHERE `title`='朱砂印鉴';

-- 行侠日志 / 随笔小记
UPDATE `app_item` SET `detail`='以笔墨记伞下岁月。\n三月十七，渡口遇一蓑衣老翁，赠伞骨一根；四月初九，剑冢听雨半宿，无一剑出鞘。\n日志只记相逢与山水，不记恩怨——多年后重读，方知哪一笔是江湖，哪一笔是人生。' WHERE `title`='行侠日志';
UPDATE `app_item` SET `detail`='青绿山水间的行侠路线图。\n自烟雨楼出发，经桃花渡、青石巷、听雨桥，至青山剑派山门，全程标注歇脚茶铺、渡口与屋檐。\n每逢雨天，图中屋檐处会多出一个小小的墨点——那是为你预留的躲雨处。' WHERE `title`='行侠路线';
UPDATE `app_item` SET `detail`='灵光一现，随手记之。\n檐角雨声忽密忽疏，像谁在调一张无弦的琴；茶烟升到一半折了腰，大约也想歇歇。\n小记不过三五行，却是江湖里最真实的注脚。闲时回看，字字都带着那天的潮气。' WHERE `title`='随笔小记';

-- 侠客结识 / 飞鸽传书
UPDATE `app_item` SET `detail`='江湖路远，结伴方长。\n渡口同船、檐下同伞、灯下同席，皆是有缘。此处可结识四方侠客，互赠名帖，共赴山水之约。\n记住烟雨阁的规矩：伞可共撑，路各自行；相逢是缘，不必强留。' WHERE `title`='侠客同游';
UPDATE `app_item` SET `detail`='本月伞下听雨榜已开。\n不比剑法，不比轻功，只比谁在檐下听雨最久、心境最闲。榜首连庄三月者，江湖赠号「烟雨第一闲人」。\n上榜不难，难的是肯把赶路的时间，分给一场雨。' WHERE `title`='侠客排行';
UPDATE `app_item` SET `detail`='飞鸽传书，江湖旧信。\n一笔一墨皆是心意，一封一往皆见性情。信笺以桑皮纸制成，遇雨不洇，字迹历十年如新。\n此处收存你与好友的全部往来书信，伞下重读，如见故人执伞而来。' WHERE `title`='飞鸽传书';
UPDATE `app_item` SET `detail`='江湖险恶，遇险莫慌。\n点开急救飞信，放出一羽白鸽——十里之内，必有侠客来援。\n烟雨阁承诺：凡收信救援者，不论门派恩怨，风雨必至。此鸽一日千里，从无延误。' WHERE `title`='急救飞信';

-- 账号设置 / 消息提醒
UPDATE `app_item` SET `detail`='在此打理你在烟雨阁的名号。\n可改江湖名号、更换印鉴头像、修改口令，或绑定新的登录方式。\n名号是你在江湖的脸面，改之前不妨再读一遍烟雨楼的规矩：名号一改，旧帖作废。' WHERE `title`='账号设置';
UPDATE `app_item` SET `detail`='江湖消息多如雨点，取舍全凭本心。\n可分别设置：江湖动态（可关）、好友书信（建议开）、飞信救援（请务必开）。\n提醒方式可选檐下风铃或案头小灯，静音一时可以，静音一世不可。' WHERE `title`='消息提醒';

-- ------------------------------------------------------------
-- 让「伞面丹青」进入首页精选（阁中珍藏卷轴更丰满，幂等）
-- ------------------------------------------------------------
UPDATE `app_item` SET `featured` = 1 WHERE `title` = '伞面丹青';

-- ============================================================
-- 站点统计计数表 app_stat（听雨人次等）
-- 说明：start-backend.bat 仅在 boonie 库不存在时才导入本文件；
--       已存在的旧库由后端 DbInitRunner 的 CREATE TABLE IF NOT EXISTS 兜底，
--       此处一并写入以保证「全新库」与「已有库」最终状态一致。
-- ============================================================
DROP TABLE IF EXISTS `app_stat`;
CREATE TABLE `app_stat` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code`       VARCHAR(32)  NOT NULL                COMMENT '统计项编码（如 listen_rain）',
  `cnt`        BIGINT       NOT NULL DEFAULT 0      COMMENT '计数',
  `updated_at` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='站点统计计数';

INSERT INTO `app_stat` (`code`, `cnt`) VALUES ('listen_rain', 0);
