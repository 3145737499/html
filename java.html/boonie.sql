/*
 Navicat Premium Dump SQL

 Source Server         : test
 Source Server Type    : MySQL
 Source Server Version : 80039 (8.0.39)
 Source Host           : localhost:3306
 Source Schema         : boonie

 Target Server Type    : MySQL
 Target Server Version : 80039 (8.0.39)
 File Encoding         : 65001

 Date: 28/10/2026 09:48:20
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin_user
-- ----------------------------
DROP TABLE IF EXISTS `admin_user`;
CREATE TABLE `admin_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_account`(`account` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '鍚庡彴绠＄悊鍛樿〃' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of admin_user
-- ----------------------------
INSERT INTO `admin_user` VALUES (1, '314573', '$2a$10$Ufc93A8HLepLWO/fPtrKdut0AVfUPi3MI0umjFA4nK1w/YQp7.aiG', 'admin314573', '2026-10-23 16:50:01', '2026-10-23 16:50:01');
INSERT INTO `admin_user` VALUES (2, 'admin@boonie.com', '$2a$10$44/5E6LE8QMVfjA5plfxEOP34.sM80Vwga2SMvCYt0ucN2aUZY/Yy', '阁主', '2026-10-28 08:59:05', '2026-10-28 08:59:05');

-- ----------------------------
-- Table structure for app_category
-- ----------------------------
DROP TABLE IF EXISTS `app_category`;
CREATE TABLE `app_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父分类ID（0=顶级）',
  `code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类编码（前端导航 key）',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序（小在前）',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE,
  INDEX `idx_parent`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '内容分类表（分级）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of app_category
-- ----------------------------
INSERT INTO `app_category` VALUES (1, 0, 'jianghu', '江湖', 1, '2026-10-14 08:58:22', '2026-10-14 08:58:22');
INSERT INTO `app_category` VALUES (2, 0, 'umbrella', '油纸伞', 2, '2026-10-14 08:58:22', '2026-10-14 08:58:22');
INSERT INTO `app_category` VALUES (3, 0, 'badge', '江湖徽章', 3, '2026-10-14 08:58:22', '2026-10-14 08:58:22');
INSERT INTO `app_category` VALUES (4, 0, 'log', '行侠日志', 4, '2026-10-14 08:58:22', '2026-10-14 08:58:22');
INSERT INTO `app_category` VALUES (5, 0, 'friend', '侠客同游', 5, '2026-10-14 08:58:22', '2026-10-14 08:58:22');
INSERT INTO `app_category` VALUES (6, 0, 'setting', '账号设置', 6, '2026-10-14 08:58:22', '2026-10-14 08:58:22');
INSERT INTO `app_category` VALUES (7, 1, 'news', '江湖动态', 1, '2026-10-14 08:58:22', '2026-10-14 08:58:22');
INSERT INTO `app_category` VALUES (8, 1, 'sect', '门派名录', 2, '2026-10-14 08:58:22', '2026-10-14 08:58:22');
INSERT INTO `app_category` VALUES (9, 2, 'mine', '我的纸伞', 1, '2026-10-14 08:58:22', '2026-10-14 08:58:22');
INSERT INTO `app_category` VALUES (10, 2, 'art', '伞面丹青', 2, '2026-10-14 08:58:22', '2026-10-14 08:58:22');
INSERT INTO `app_category` VALUES (11, 3, 'collect', '徽章收集', 1, '2026-10-14 08:58:22', '2026-10-14 08:58:22');
INSERT INTO `app_category` VALUES (12, 3, 'seal', '朱砂印鉴', 2, '2026-10-14 08:58:22', '2026-10-14 08:58:22');
INSERT INTO `app_category` VALUES (13, 4, 'diary', '行侠日志', 1, '2026-10-14 08:58:22', '2026-10-14 08:58:22');
INSERT INTO `app_category` VALUES (14, 4, 'note', '随笔小记', 2, '2026-10-14 08:58:22', '2026-10-14 08:58:22');
INSERT INTO `app_category` VALUES (15, 5, 'mate', '侠客结识', 1, '2026-10-14 08:58:22', '2026-10-14 08:58:22');
INSERT INTO `app_category` VALUES (16, 5, 'letter', '飞鸽传书', 2, '2026-10-14 08:58:22', '2026-10-14 08:58:22');
INSERT INTO `app_category` VALUES (17, 6, 'account', '账号设置', 1, '2026-10-14 08:58:22', '2026-10-14 08:58:22');
INSERT INTO `app_category` VALUES (18, 6, 'notify', '消息提醒', 2, '2026-10-14 08:58:22', '2026-10-14 08:58:22');
INSERT INTO `app_category` VALUES (19, 4, 'poem_bridge', '桥上诗', 3, '2026-10-28 08:59:05', '2026-10-28 08:59:05');
INSERT INTO `app_category` VALUES (20, 4, 'poem_sun', '岁晚诗', 4, '2026-10-28 08:59:05', '2026-10-28 08:59:05');
INSERT INTO `app_category` VALUES (21, 4, 'poem_mirror', '镜花诗', 5, '2026-10-28 09:37:24', '2026-10-28 09:37:24');

-- ----------------------------
-- Table structure for app_item
-- ----------------------------
DROP TABLE IF EXISTS `app_item`;
CREATE TABLE `app_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_id` bigint NOT NULL COMMENT '所属分类ID（app_category.id）',
  `icon` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '?' COMMENT '图标（emoji）',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题',
  `summary` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '简介（前端展示用 desc）',
  `detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '详情（预留）',
  `hot` tinyint NOT NULL DEFAULT 0 COMMENT '热门搜索词 0/1',
  `featured` tinyint NOT NULL DEFAULT 0 COMMENT '首页精选 0/1',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序（小在前）',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category_id` ASC) USING BTREE,
  INDEX `idx_title`(`title` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '内容条目表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of app_item
-- ----------------------------
INSERT INTO `app_item` VALUES (1, 9, '🌂', '我的油纸伞', '查看你的纸伞与伞面丹青进度，撑一把属于自己的烟雨。', '每一把油纸伞都记录着主人的江湖。\n伞骨取自三年生楠竹，伞面覆以皮纸、刷足三遍桐油，方能撑得起江南一整季的烟雨。\n在这里查看你的纸伞品阶、伞面丹青进度与历次修缮记录——伞虽无言，伞下皆是故事。', 1, 0, 1, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (2, 9, '🧴', '纸伞保养', '桐油三遍、伞骨轻抚，让你的纸伞多用十年。', '纸伞如剑，须常拂拭。\n雨后须撑开阴干，切忌暴晒；桐油每岁一补，伞骨关节处薄施蜡油。\n小筑匠人总结了七条保养口诀，照做一把伞多用十年，江湖人称「伞养人，人养伞」。', 0, 0, 2, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (3, 10, '🎨', '伞面丹青', '在伞面上挥毫泼墨，绘制专属江湖山水。', '一方伞面，即一方山水。\n取青绿设色，以伞骨为皴、烟雨为染，泼墨于皮纸之上，画的是山水，落的是心境。\n每月丹青上品将悬于烟雨楼回廊，供四方侠客品评，获赞最多者可得鎏金伞骨一副。', 1, 1, 1, '2026-10-14 08:58:22', '2026-10-23 16:26:23');
INSERT INTO `app_item` VALUES (4, 10, '🌸', '节气伞面', '依二十四节气更换伞面丹青，春分桃花、夏至青荷。', '节气更迭，伞面随之。\n春分画桃花两枝，夏至点青荷一盏，秋分染桂香满伞，冬至留白等一场雪落。\n依节气更换伞面丹青，是烟雨阁延续百年的雅事——伞在手中，四季在眼中。', 0, 1, 2, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (5, 7, '⚔️', '江湖动态', '阅尽四方侠客的行侠见闻与门派快讯。', '江湖每日都有新的传闻：谁家剑客雨夜过江，谁派掌门闭关练气。\n此处汇总四方侠客的行侠见闻与各门派快讯，一页阅尽江湖事。\n传闻七分真三分假，信与不信，全看阁下的江湖阅历。', 1, 0, 1, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (6, 7, '🌦️', '烟雨快报', '每日烟雨指数播报，出门带伞提醒。', '今日烟雨指数：江南七成，塞北二成。\n出门宜带伞，宜访友，宜在檐下听一场雨；不宜赶路，不宜动刀兵。\n烟雨快报每日辰时更新，来自烟雨楼分布于各地的听雨郎。', 0, 0, 2, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (7, 8, '🏯', '门派名录', '青绿山水间的大小门派，择一而栖。', '青绿山水之间，大小门派林立：有立派三百年的青山剑派，也有只医有缘人的听雨小筑。\n名录按剑、医、乐、画四艺分册，各派渊源、入门规矩、看家本事皆一一在册。\n择一派而栖，是初入江湖的头等大事，不妨多看几页再定。', 1, 0, 1, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (8, 8, '⛰️', '青山剑派', '立派三百年，剑法如青山连绵不绝，入门先学听雨。', '青山剑派立派三百年，剑法讲究「连绵不绝」。\n入门第一课不在剑上，而在檐下——听满一百场雨，方许摸剑。\n派中弟子个个惜伞如金，据说剑意第七重，是在伞下悟出来的。', 0, 1, 2, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (9, 8, '🏮', '烟雨楼', '江南第一消息集散地，一壶清茶换半城传闻。', '烟雨楼，江南第一消息集散地。\n一壶清茶，可换半城传闻；三盏淡酒，能买一个名字的下落。\n楼中规矩只有一条：只谈江湖，不问出处。檐下听雨的，皆是过客，也皆是故人。', 0, 1, 3, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (10, 8, '🍵', '听雨小筑', '隐世医馆，只医有缘人，檐下常备热茶。', '隐世医馆，不悬壶，不挂牌，只医有缘人。\n听雨小筑的主人医术通神却常年闭门，据说只有雨夜叩门三声者，方能得见。\n檐下常备热茶与姜汤，路过的侠客哪怕不看病，也可歇脚避雨。', 0, 0, 4, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (11, 11, '🏅', '江湖徽章', '已收集 12 枚，继续行侠解锁更多鎏金徽章。', '行侠积德，徽章为证。\n救困、扶危、守约、义送，每桩善举皆镌一枚鎏金徽章。目前阁中已收录一十二枚，余下诸枚散落江湖各处。\n集齐一套者，烟雨楼将为其立传一页，青史留名。', 0, 0, 1, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (12, 11, '📜', '徽章图鉴', '鎏金徽章全图鉴，收录每一枚的来历故事。', '每一枚徽章背后，都有一段来历。\n「渡口」来自雨夜摆渡三百趟，「守约」来自十年之期风雨无阻，「义送」来自千里护送不取分文。\n图鉴收录每一枚的纹样、成色与故事，闲时翻阅，如会故人。', 0, 0, 2, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (13, 12, '🔖', '朱砂印鉴', '完成奇遇获得的朱砂印鉴，皆收于此处。', '朱砂印鉴，只赠有奇缘者。\n或在山洞拾得失谱，或于江心救起故人，或在破庙躲雨时误入前朝秘境——凡此种种，皆可获印一方。\n朱砂浸骨，遇雨不褪，是江湖中最硬的信用。', 0, 0, 1, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (14, 13, '📖', '行侠日志', '以笔墨记录每一次伞下奇遇与山水相逢。', '以笔墨记伞下岁月。\n三月十七，渡口遇一蓑衣老翁，赠伞骨一根；四月初九，剑冢听雨半宿，无一剑出鞘。\n日志只记相逢与山水，不记恩怨——多年后重读，方知哪一笔是江湖，哪一笔是人生。', 1, 0, 1, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (15, 13, '🗺️', '行侠路线', '青绿山水间的行侠路线图，标注歇脚茶铺与渡口。', '青绿山水间的行侠路线图。\n自烟雨楼出发，经桃花渡、青石巷、听雨桥，至青山剑派山门，全程标注歇脚茶铺、渡口与屋檐。\n每逢雨天，图中屋檐处会多出一个小小的墨点——那是为你预留的躲雨处。', 0, 1, 2, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (16, 14, '✍️', '随笔小记', '随手记下烟雨中的灵光一现。', '灵光一现，随手记之。\n檐角雨声忽密忽疏，像谁在调一张无弦的琴；茶烟升到一半折了腰，大约也想歇歇。\n小记不过三五行，却是江湖里最真实的注脚。闲时回看，字字都带着那天的潮气。', 0, 0, 1, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (17, 15, '👫', '侠客同游', '结识四方侠客，共赏青绿山水，同赴惊鸿之约。', '江湖路远，结伴方长。\n渡口同船、檐下同伞、灯下同席，皆是有缘。此处可结识四方侠客，互赠名帖，共赴山水之约。\n记住烟雨阁的规矩：伞可共撑，路各自行；相逢是缘，不必强留。', 0, 0, 1, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (18, 15, '🏆', '侠客排行', '本月伞下听雨榜，看谁是烟雨第一闲人。', '本月伞下听雨榜已开。\n不比剑法，不比轻功，只比谁在檐下听雨最久、心境最闲。榜首连庄三月者，江湖赠号「烟雨第一闲人」。\n上榜不难，难的是肯把赶路的时间，分给一场雨。', 1, 1, 2, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (19, 16, '💌', '飞鸽传书', '与江湖好友往来书信，传递伞下温情。', '飞鸽传书，江湖旧信。\n一笔一墨皆是心意，一封一往皆见性情。信笺以桑皮纸制成，遇雨不洇，字迹历十年如新。\n此处收存你与好友的全部往来书信，伞下重读，如见故人执伞而来。', 0, 0, 1, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (20, 16, '🕊️', '急救飞信', '江湖遇险一键飞鸽，十里之内必有侠客来援。', '江湖险恶，遇险莫慌。\n点开急救飞信，放出一羽白鸽——十里之内，必有侠客来援。\n烟雨阁承诺：凡收信救援者，不论门派恩怨，风雨必至。此鸽一日千里，从无延误。', 0, 0, 2, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (21, 17, '⚙️', '账号设置', '管理名号、密码与登录方式。', '在此打理你在烟雨阁的名号。\n可改江湖名号、更换印鉴头像、修改口令，或绑定新的登录方式。\n名号是你在江湖的脸面，改之前不妨再读一遍烟雨楼的规矩：名号一改，旧帖作废。', 0, 0, 1, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (22, 18, '🔔', '消息提醒', '配置江湖动态与好友消息的提醒偏好。', '江湖消息多如雨点，取舍全凭本心。\n可分别设置：江湖动态（可关）、好友书信（建议开）、飞信救援（请务必开）。\n提醒方式可选檐下风铃或案头小灯，静音一时可以，静音一世不可。', 0, 0, 1, '2026-10-14 08:58:22', '2026-10-14 09:35:35');
INSERT INTO `app_item` VALUES (23, 19, '🌉', '桥上看景', '你站在桥上看风景，看风景的人在楼上看你。', '桥上看景，楼上看人。\n此笺取自卞之琳《断章》——你以为在看风景，却不知自己也成了别人的风景。\n烟雨阁藏此一联，提醒过客：江湖之大，人人皆是观者，亦皆是被观之人。', 0, 1, 1, '2026-10-28 08:59:05', '2026-10-28 08:59:05');
INSERT INTO `app_item` VALUES (24, 20, '☀️', '岁晚风暖', '风清日暖，正是人间岁晚时。', '风清日暖，岁晚情长。\n一年将尽，檐角风铃轻响，案头热茶正温。此笺写给每一个在江湖奔波后，仍愿慢下来的人。\n岁晚不是终点，是又一程烟雨的起点。', 0, 1, 2, '2026-10-28 08:59:05', '2026-10-28 08:59:05');
INSERT INTO `app_item` VALUES (25, 21, '🌸', '镜花辞树', '最是人间留不住，朱颜辞镜花辞树。', '最是人间留不住。朱颜辞镜，是少年容颜终要从镜中老去；花辞树，是枝头繁花终要离枝零落。王国维以一句道尽无常——人间最留不住的，从来是春光与少年。烟雨阁藏此一笺，愿你惜取眼前人与当下景。', 0, 1, 3, '2026-10-28 09:37:24', '2026-10-28 09:37:24');

-- ----------------------------
-- Table structure for app_stat
-- ----------------------------
DROP TABLE IF EXISTS `app_stat`;
CREATE TABLE `app_stat`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '统计项编码（如 listen_rain）',
  `cnt` bigint NOT NULL DEFAULT 0 COMMENT '计数',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '站点统计计数' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of app_stat
-- ----------------------------
INSERT INTO `app_stat` VALUES (1, 'listen_rain', 0, '2026-10-21 00:00:00');

-- ----------------------------
-- Table structure for app_user
-- ----------------------------
DROP TABLE IF EXISTS `app_user`;
CREATE TABLE `app_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录账号（邮箱或手机号）',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码（BCrypt 加密存储）',
  `email` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'user' COMMENT '瑙掕壊锛歶ser=鏅??鐢ㄦ埛, admin=鍚庡彴绠＄悊鍛',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_account`(`account` ASC) USING BTREE,
  INDEX `idx_email`(`email` ASC) USING BTREE,
  INDEX `idx_phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of app_user
-- ----------------------------
INSERT INTO `app_user` VALUES (1, 'demo@boonie.com', '$2a$10$44/5E6LE8QMVfjA5plfxEOP34.sM80Vwga2SMvCYt0ucN2aUZY/Yy', 'demo@boonie.com', NULL, '烟雨阁少侠', 'user', '2026-10-14 08:34:13', '2026-10-14 08:34:13');
INSERT INTO `app_user` VALUES (2, '123456789', '$2a$10$4z3PxKW0a3eNIn8zaiXtCechoilkmsLRLU.mFL/NkoIx94SlCB4Ki', NULL, '123456789', '123456789', 'user', '2026-10-14 08:35:03', '2026-10-14 08:35:03');
INSERT INTO `app_user` VALUES (4, '314573', '$2a$10$7r3qDAQtU.K1CYfpztkAFOPKJDEz3aWkW31nvSaJ.AiS1sthGA4im', NULL, '314573', '314573', 'user', '2026-10-23 16:53:30', '2026-10-23 16:53:30');

SET FOREIGN_KEY_CHECKS = 1;
