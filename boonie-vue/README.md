# 熊出没 · 狗熊岭（Vue 3 版）

由 `html-main` 的 4 个单文件 HTML 改造而来的 **Vue 3 + Vite 工程化项目**。
纯前端演示，登录逻辑为前端模拟，无后端。

## 与原项目的对应关系

| 原 HTML 文件 | Vue 中的访问路径 | 风格 |
|---|---|---|
| `index.html` | `/` | 森林高级风（毛玻璃 + 流动渐变 + 极光光斑 + 飘落树叶 + 3D 倾斜 + 昼夜切换） |
| `login-cartoon.html` | `/cartoon` | 卡通明亮风（蓝天白云、圆润按钮、emoji 迸发） |
| `login-neon.html` | `/neon` | 暗夜霓虹风（深色网格、青/品红霓虹辉光） |
| `login-minimal.html` | `/minimal` | 极简风（米白底、大量留白、单一绿色强调） |

> 现在**一个工程包含全部 4 种风格**，通过页面底部的「风格」切换器或直接改 URL 即可换肤，
> 不再需要打开 4 个不同的 HTML 文件。

## 快速开始

```bash
npm install
npm run dev      # 开发服务器，默认 http://localhost:5173
npm run build    # 产物输出到 dist/
npm run preview  # 预览构建产物
```

## 目录结构

```
boonie-vue/
├─ index.html                 # Vite 入口 HTML（挂载点 #app）
├─ vite.config.js             # Vite 配置（含 @ → src 别名）
├─ package.json
└─ src/
   ├─ main.js                 # 应用入口：注册 Pinia、Router、全局样式
   ├─ App.vue                 # 布局外壳：环境层 + 导航 + 路由出口 + 页脚
   ├─ router/index.js         # 路由表（4 风格 × 首页/登录/个人中心）
   ├─ stores/
   │   ├─ user.js             # 登录态 + 「记住我」持久化
   │   └─ theme.js            # 当前风格与昼夜状态
   ├─ composables/            # 从原内联脚本抽出的可复用逻辑
   │   ├─ useLoginForm.js     # 账号/密码校验
   │   ├─ useTilt.js          # 3D 鼠标倾斜
   │   ├─ useReveal.js        # 滚动揭示动画
   │   ├─ useScrolled.js      # 导航栏滚动收缩
   │   └─ useCelebrate.js     # 彩带 / emoji 庆祝特效
   ├─ components/
   │   ├─ AppNav.vue          # 顶部导航（随风格换品牌文案）
   │   ├─ StyleSwitcher.vue   # 风格切换器
   │   ├─ ForestBackdrop.vue  # 森林风：渐变背景 + 光斑
   │   ├─ CartoonBackdrop.vue # 卡通风：太阳 + 云朵
   │   ├─ NeonBackdrop.vue    # 霓虹风：透视网格
   │   └─ FallingLeaves.vue   # 森林风：飘落树叶
   ├─ views/
   │   ├─ LandingView.vue     # 落地页（4 套文案 + 同一模板）
   │   ├─ LoginView.vue       # 登录页
   │   └─ HomeView.vue        # 个人中心
   └─ styles/
       ├─ base.css            # 重置 + 视图动画 + 特效层 + 无障碍偏好
       ├─ forest.css          # 森林风（html[data-style='forest'] 作用域）
       ├─ cartoon.css         # 卡通风
       ├─ neon.css            # 霓虹风
       └─ minimal.css         # 极简风
```

## 关键改造点

1. **多文件 → 单工程多路由**
   4 个 HTML 合并为一个 SPA，`vue-router` 管理「首页 / 登录 / 个人中心」，
   `meta.style` 标记该路由属于哪种视觉风格。

2. **DOM 直操 → 响应式状态**
   原先的 `document.getElementById(...)`、`classList.toggle` 全部替换为
   `ref` / `computed` / Pinia store。例如登录后的问候语由 `user.greeting` 计算得出。

3. **CSS 全局污染 → 风格命名空间**
   每个风格文件的选择器统一加 `html[data-style='xxx']` 前缀，
   避免 4 套同名类（`.card`、`.btn`、`.input`）互相覆盖。
   `App.vue` 用 `watchEffect` 把当前风格同步到 `<html data-style>`。

4. **重复逻辑 → 组合式函数**
   账号校验、3D 倾斜、滚动揭示、庆祝特效等原先在多处重复的内联脚本，
   收敛到 `src/composables/` 下，按需引入。

5. **增强项**
   - 路由切换过渡动画、页面标题随路由自动更新
   - 风格切换器（替代原来手动打开不同文件）
   - 尊重系统 `prefers-reduced-motion` 设置
   - 使用 hash 路由，`npm run build` 后双击 `dist/index.html` 也能直接打开

## 通用功能

- 账号 / 密码输入，实时格式校验（邮箱 / 手机号 / 用户名）
- 密码显示 / 隐藏切换
- 「记住我」（`localStorage` 回填）
- 登录成功彩带 / emoji 庆祝，自动跳转个人中心
- 顶部导航在「首页 / 登录 / 个人中心」间切换
- 第三方登录（微信 / Apple / QQ）占位入口

---

> 演示作品，登录逻辑为前端模拟，未接入真实后端。
