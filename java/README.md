# 觉醒伪神 · 登录注册项目（启动备忘）

> 本文件是「快速启动备忘录」，写给未来的自己 / AI 助手看，避免每次重新摸索。
> 一眼看懂：项目结构、端口、数据库、怎么一键启动。

## 一句话说明
海绵宝宝/觉醒伪神主题的登录注册网站。前端 Vue3 + Vite，后端 Spring Boot + MyBatis-Plus + MySQL。

## 目录结构
```
java/
├── start-all.bat        ← 一键启动前后端（双击）
├── stop-all.bat         ← 一键停止前后端（双击）
├── false-god-backend/   ← 后端（Spring Boot 3.2.5 + MyBatis-Plus + MySQL）
│   ├── start-backend.bat   ← 后端启动脚本
│   ├── pom.xml
│   └── sql/false_god.sql   ← 建表脚本（含测试账号）
└── html/                ← 前端（Vue3 + Vite）
    ├── start-frontend.bat  ← 前端启动脚本
    ├── package.json
    └── vite.config.js
```

## 关键信息（写死，别改）
| 项目 | 值 |
|------|-----|
| 后端端口 | 8080 |
| 前端端口 | 5173 |
| 前端代理 | `/api` → `http://localhost:8080` |
| 数据库 | MySQL `false_god`，root / 123456 |
| 测试账号 | chenhaoyu / 123456 |
| Java | Zulu 21（`C:\Program Files\Zulu\zulu-21`） |
| Maven | `~/.m2/wrapper/dists/apache-maven-3.9.9-bin/.../apache-maven-3.9.9` |
| 前端登录逻辑 | `html/src/api/auth.js`，`USE_MOCK=false`（已对接真实后端） |

## 启动步骤（每次启动前 node_modules、target 都会被删）
1. 双击 `start-all.bat`（或分别在两个目录双击各自脚本）。
2. 脚本会自动：前端 `npm install`（若无 node_modules）、后端 `mvn clean package`（若无 target）。
3. 打开 http://localhost:5173 访问页面。

## 停止步骤
双击 `stop-all.bat`（按端口 8080/5173 结束进程）。

## 环境注意
- Maven 不是装在系统 PATH，而是 Maven Wrapper 缓存里（`~/.m2/wrapper/dists`）。
  后端脚本已自动适配，若换了机器需检查该路径是否存在。
- 后端启动脚本里 Maven 路径有两处回退（3.9.9 → 3.9.16），改环境时同步更新。

## 常见问题
- **页面白屏 / 接口无响应（重要教训）**：多半是上次没关干净的旧进程还占着 5173/8080 端口，且已腐化（占着端口但不干活，Vite 会被迫跳到 5174）。
  解决：先双击 `stop-all.bat` 清理端口，再 `start-all.bat`。2026-11-02 就遇到过：5173 被旧 vite 占用导致白屏、8080 被旧 java 占用导致启动失败。
- 后端起不来：先确认 MySQL 已启动、`false_god` 库已建（执行 `sql/false_god.sql`）。
- 前端连不上后端：确认后端 8080 已启动，vite 代理 target 正确。
- 想临时用 Mock 模式：把 `html/src/api/auth.js` 里 `USE_MOCK` 改成 `true`。
