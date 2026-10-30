# 离恨烟 · 烟雨阁（Vue 3 + Spring Boot 全栈）

以王者荣耀「离恨烟」（公孙离水墨皮肤）为视觉主题的**水墨丹青风登录演示项目**。
前端 Vue 3 + Vite，后端 Spring Boot 3 + MyBatis-Plus + MySQL，登录 / 注册均对接真实后端。

## 页面一览

| 路径 | 页面 | 说明 |
|---|---|---|
| `/` | 首页 | 水墨英雄区：立绘图框 + 竖排诗题 + 朱砂印章 + 统计数据 + 伞下江湖四宫格 |
| `/login` | 登录 / 注册 | 宣纸质感卡片（金线双框 + 四角金饰 + 印章）、**图形验证码**注册 |
| `/home` | 个人中心 | 需登录，卷首语 / 分类检索 / 阁中珍藏 / 数字速览 / 听雨入口 |
| `/item/:id` | 条目详情 | 无需登录，卷宗正文 + 朱砂落款 + 同分类推荐 |
| `/game` | 伞下听雨 | 无需登录，Canvas 小游戏（接雨珠/飞花/竹叶/朱砂印） |
| `/admin/login` | 后台登录 | 仅登录无注册，登录后校验管理员权限 |
| `/admin` | 后台管理 | 需管理员，分类与藏品的增删改查 |

## 快速开始

### 方式一：一键启动（推荐）

双击项目根目录 `start-all.bat`（自动初始化数据库 → 启动后端 9090 → 启动前端 5173）。

测试账号：`demo@boonie.com` / `123456`

### 方式二：手动启动

```bash
# 后端（需 MySQL，账号 root/123456，首次自动执行 sql/boonie.sql）
cd springBootTest
mvnw.cmd spring-boot:run        # http://localhost:9090

# 前端
cd boonie-vue
npm install
npm run dev                     # http://localhost:5173（/api 代理到 9090）
```

## 后端接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/auth/captcha` | 获取图形验证码 → `{ key, image(base64 PNG) }`，5 分钟有效、一次性使用 |
| POST | `/api/auth/register` | `{ account, password, code, captchaKey }` 注册 |
| POST | `/api/auth/login` | `{ account, password }` 登录，BCrypt 校验，返回 token |
| POST | `/api/auth/logout` | `{ token }` 登出 |

图形验证码由后端 AWT 绘制：宣纸底色 + 淡墨噪点 + 干扰弧线 + 楷体旋转字符，
4 位（去除易混淆的 I / O / 0 / 1），验证后立即作废。

## 目录结构（前端）

```
boonie-vue/
├─ index.html                 # Vite 入口 HTML（挂载点 #app）
├─ vite.config.js             # Vite 配置（@ 别名 + /api 代理 9090）
├─ public/*.webp              # 立绘 + AI 水墨横幅，统一 WebP（共约 690KB）
└─ src/
   ├─ main.js                 # 应用入口：注册 Pinia、Router、全局样式
   ├─ App.vue                 # 布局外壳：水墨背景 + 导航 + 路由出口 + 页脚
   ├─ router/index.js         # 路由表（首页 / 登录 / 个人中心）
   ├─ stores/
   │   └─ user.js             # 登录态（以 token 为判据）+ /api/auth/me 角色校验 + 「记住我」
   ├─ composables/
   │   ├─ useLoginForm.js     # 账号/密码校验
   │   ├─ useReveal.js        # 滚动揭示动画
   │   ├─ useScrolled.js      # 导航栏滚动收缩
   │   └─ useCelebrate.js     # 水墨彩带庆祝特效
   ├─ components/
   │   ├─ AppNav.vue          # 顶部导航（RouterLink + <768px 汉堡菜单）
   │   ├─ InkBackdrop.vue     # 水墨背景（宣纸肌理 + 五叠远山 + 水面小舟）
   │   ├─ InkPetals.vue       # 飘落花瓣
   │   └─ InkMotif.vue        # 分类水墨线描母题
   ├─ views/
   │   ├─ LandingView.vue     # 首页英雄区 + 特性区
   │   ├─ LoginView.vue       # 登录 / 注册（图形验证码）
   │   ├─ HomeView.vue        # 个人中心
   │   ├─ DetailView.vue      # 条目详情
   │   ├─ GameView.vue        # 伞下听雨（Canvas 小游戏）
   │   ├─ AdminLoginView.vue  # 后台登录
   │   └─ AdminView.vue       # 后台管理
   └─ styles/
       ├─ base.css            # 重置 + 视图动画 + 特效层 + 无障碍偏好
       └─ ink.css             # 水墨丹青设计系统（青绿/朱砂/鎏金/墨）
```

## 功能清单

- 登录：BCrypt 密码校验、token 持久化（刷新不掉线）、记住我回填
- 注册：图形验证码（点击图片刷新、提交失败自动换图）、账号唯一性校验
- 退出登录：前后端联动作废 token
- 权限：后台 `/admin` 需后端核实的管理员角色，仅凭本地缓存的角色进不去
- 水墨视觉细节：竖排诗题、朱砂印章、金线勾边、飘落花瓣、登录成功水墨彩带
- 响应式：`<768px` 导航折叠为汉堡菜单
- 尊重系统 `prefers-reduced-motion` 设置
- 使用 hash 路由，`npm run build` 后双击 `dist/index.html` 也能直接打开（接口需后端在线）

## 性能约定

- `public/` 下的图片统一为 WebP（共约 690KB）。新增图片请先压成 WebP 再放进来，
  上一版 4 张原图合计 6.4MB，转 WebP 后省了约 5.7MB。

---

> 演示作品。王者荣耀「离恨烟」形象版权归腾讯所有，仅作学习演示用途。
