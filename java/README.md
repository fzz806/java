# 觉醒伪神 · 登录注册项目（启动备忘）

> 本文件是「快速启动备忘录」，写给未来的自己 / AI 助手看，避免每次重新摸索。
> 一句话：**双击 `start-all.bat` 就行**，其余都是它帮你做的事。

## 一句话说明
「觉醒伪神：神明弃我，我自为神」主题的登录注册网站。
前端 Vue3 + Vite，后端 Spring Boot 3.2.5 + MyBatis-Plus + MySQL。

## 目录结构
```
java/
├── start-all.bat        ← 一键启动（智能检测环境，双击即可）
├── stop-all.bat         ← 一键停止（按端口 5173/5174/8080 清理）
├── false-god-backend/   ← 后端（Spring Boot 3.2.5 + MyBatis-Plus + MySQL）
│   ├── sql/false_god.sql   ← 建表脚本（含测试账号）
│   └── pom.xml
└── html/                ← 前端（Vue3 + Vite）
    ├── src/api/auth.js     ← 登录/注册接口
    ├── src/api/session.js  ← 登录态本地存储（新增）
    ├── src/components/ThroneCard.vue ← 登录后神位页（新增）
    ├── package.json
    └── vite.config.js
```

## 关键信息（配置基准）
| 项目 | 值 |
|------|-----|
| 后端端口 | 8080 |
| 前端端口 | 5173 |
| 前端代理 | `/api` → `http://localhost:8080` |
| 数据库 | MySQL `false_god`，root / 123456 |
| 测试账号 | chenhaoyu / 123456 |
| 后端接口 | `POST /api/login`、`POST /api/register` |
| Java | Zulu 21（`C:\Program Files\Zulu\zulu-21`） |
| Maven | `~/.m2/wrapper/dists/apache-maven-3.9.9-bin/<hash>/apache-maven-3.9.9` |

## 启动步骤（推荐）
**双击 `start-all.bat`**，脚本会自动完成 6 步：
1. 自动查找 Java（JAVA_HOME → Zulu 21 → jdk-17 → PATH，多路回退）
2. 清理被占用的 5173 / 8080 端口（解决白屏、启动失败的根因）
3. 检测 MySQL 是否运行；没跑就尝试 `net start MySQL80` 拉起
4. 检测 `false_god` 库；不存在则自动建库并导入 `sql/false_god.sql`
5. 缺 jar 才 Maven 构建，然后启动后端，**健康检查通过后才继续**
6. 缺 `node_modules` 才 `npm install`，启动前端，最后自动打开浏览器

启动完成后控制台会打印：
- 前端 http://localhost:5173
- 后端 http://localhost:8080
- 测试账号 chenhaoyu / 123456

## 停止步骤
双击 `stop-all.bat`，按端口（5173 / 5174 / 8080）结束对应进程。

## 环境注意
- **Maven 不在系统 PATH**，在 Maven Wrapper 缓存里。路径是三层：
  `~/.m2/wrapper/dists/apache-maven-X-bin/<hash>/apache-maven-X/bin/mvn.cmd`
  `start-all.bat` 已按三层结构自动查找，换机器无需改脚本。
- `.bat` 文件是 **GBK 编码**，不要加 `chcp 65001`，否则中文会乱码。

## 常见问题
- **页面白屏 / 接口无响应**：多半是旧进程占着 5173/8080 且已腐化（占端口但不干活，Vite 被迫跳 5174）。
  `start-all.bat` 启动时会自动清理，也可先双击 `stop-all.bat`。
- **登录报 500 / Unknown database 'false_god'**：数据库没建。脚本会自动处理；手动修复：
  ```
  mysql -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS false_god DEFAULT CHARACTER SET utf8mb4;"
  mysql -uroot -p123456 --default-character-set=utf8mb4 false_god < sql/false_god.sql
  ```
  注意**必须加 `--default-character-set=utf8mb4`**，否则中文报 `ERROR 1366`。
- **前端连不上后端**：确认 8080 已启动，检查 `vite.config.js` 代理 target。
- **想临时用 Mock 模式**：把 `html/src/api/auth.js` 里 `USE_MOCK` 改成 `true`。

## 已知待完善（下次可做）
1. **token 是假的**：后端返回 `fake-jwt-<时间戳>`，没有真实签名与校验，也没有鉴权拦截器。
2. **无鉴权接口**：缺少 `/api/me` 之类的 el 接口，前端刷新只能靠 localStorage 恢复登录态。
3. **密码未加盐哈希**：见 `common/PasswordUtil.java`，需确认是否用了 BCrypt/MD5+盐。
4. **无后端单元测试**：`src/test` 目录不存在。
5. **无前端路由**：单页状态切换（`view` ref），未引入 vue-router，刷新会回登录页。
6. 生产环境 **CORS 配置**（`config/CorsConfig.java`）需收紧，目前可能允许全部来源。
