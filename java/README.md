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
| 本机 IP | `192.168.68.49`（以太网） |
| 后端端口 | 8080 |
| 前端端口 | 5173 |
| 前端代理 | `/api` → `http://192.168.68.49:8080` |
| 数据库 | MySQL `false_god`，`192.168.68.49:3306`，root / 123456 |
| 本机访问 | http://192.168.68.49:5173 |
| 局域网访问 | 同一 WiFi 下手机/其他电脑打开 http://192.168.68.49:5173 |
| 测试账号 | chenhaoyu / 123456 |
| 后端接口 | `POST /api/login`、`POST /api/register` |
| Java | Zulu 21（`C:\Program Files\Zulu\zulu-21`） |
| Maven | `~/.m2/wrapper/dists/apache-maven-3.9.9-bin/<hash>/apache-maven-3.9.9` |

## 境界等级体系（战力模块）
两轴模型：**大境界**（`t_power.realm_index`，0~9）+ **小境界**（`t_power.stage`，0~3）。

| 大境界（realm_index） | 小境界（stage 0~3） |
|---|---|
| 一印(0) ~ 七印(6) | 初印 → 中印 → 圆满 → 巅峰 |
| 八印(7)【天锁八印】 | 初印 → 中印 → 圆满 → 巅峰 |
| 半神(8) | 初阶 → 中阶 → 圆满 → 巅峰 |
| 真神(9)（自封之神） | 终局位格，不再细分 |

- 小境界臻至**巅峰**才能冲击大境界壁垒；真神为终点，精进与突破都会被拒绝。
- 综合战力不落库，实时推导：`realmIndex*800 + stage*120 + skills*140 + essence*40`
  （小境界 +120，大境界突破净 +440）。
- 初始档案：四印·圆满 → 战力 3940（高于榜首沈砚 3860）。
- **战力榜联动**：`GET /api/rank` 会把当前玩家按真实战力并入榜单（那条带 `self: true`），
  战力/境界取自战力档案，所以修行后名次立刻变化（初始 3940 已居榜首）。
- 动作键 `seal` 语义为「小境界精进」，`realm` 为「大境界突破」；键名保持不变以兼容 `t_task.action`。
- 旧库升级：`false_god_power.sql` 内含幂等迁移块，会把历史的 `seal`（红印 0~9 重）改名为 `stage` 并折算进 0~3 档。

## 神格任务与每日额度
- **日课 10 条**（每天重置）+ **主线 4 条**（一次性功业，终身各领一次）。
- **每日上限 10 个**：`TaskService.DAILY_CLAIM_LIMIT = 10`，恰好等于日课池条数，
  做满 10 条即额度用尽，次日零点按 `claimed_at` 自然归零。
- **额度只约束日课**：主线不占额度、也不受 `limitReached` 限制。区分方式是 `task_date`——
  日课恒为当天，主线固定 `1970-01-01`，所以统计时 `task_date = 今天` 就能把两类分开，无需联表。
- 日课全清 **660 经验/天**，主线一次性共 1150；阶位门槛 `100 + (n-1)*80`，一到九阶累计 3780 点封顶。
- **阶位 = 大境界**：Lv.N 就是第 N 个大境界（Lv.4 = 四印），阶梯统一在 `common/RealmLadder.java`，
  修行页与任务页共用同一份，两处数字永远一致。
- **静修每日 5 次**：`PowerService.REST_DAILY_LIMIT = 5`，每次回灵能 240。次数按当天
  `t_power_log` 里以「静修」开头的记录计数（成功必写日志、失败不写，不必另建计数字段），
  自然日 00:00 自动归零；超额直接拒绝，不回灵能、也不推进静修类日课。
- **单日可支配灵能 = 上限 2000 + 5×240 = 3200**。日课里要花灵能的是磨印双锤 1200、
  吞噬本源 400、本源三噬 1200、夺技于野 1200，合计 4000，所以**一天做满 10 条需要前一天
  留下余粮**，不再能一口气清空；灵能紧时优先做奖励高的夺技/吞噬。
- 静修类日课最高只设 5 次（`daily_rest6` 已改名「五转周天」，target 6 → 5），
  **不能超过每日上限**，否则该任务永远完成不了。
- 领取 10 条得 660 经验（其中 540 点是四印的境界折算），阶位 4 → 5，境界同步到「五印 · 初印」。
- 日课刻意不放「大境界突破」（1600 灵能一次，且真神后永久做不了）。
  唯一例外是 `daily_seal`（精进两档）：若当天起始正好在「巅峰」档，第二次精进会被拦，需额外突破一次（+1600 灵能）。
- 注意：**改日课条数时必须同步改 `DAILY_CLAIM_LIMIT`**，否则额度会提前卡住或用不完。

## 启动步骤（推荐）
**双击 `start-all.bat`**，脚本会自动完成 6 步：
1. 自动查找 Java（JAVA_HOME → Zulu 21 → jdk-17 → PATH，多路回退）
2. 清理被占用的 5173 / 8080 端口（解决白屏、启动失败的根因）
3. 检测 MySQL 是否运行；没跑就尝试 `net start MySQL80` 拉起
4. 检测 `false_god` 库；不存在则自动建库并导入 `sql/false_god.sql`
5. 缺 jar 才 Maven 构建，然后启动后端，**健康检查通过后才继续**
6. 缺 `node_modules` 才 `npm install`，启动前端，最后自动打开浏览器

启动完成后控制台会打印：
- 前端 http://192.168.68.49:5173
- 后端 http://192.168.68.49:8080
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
- **前端连不上后端**：确认 8080 已启动，检查 `vite.config.js` 代理 target 是否为 `192.168.68.49:8080`。
- **手机打不开页面**：① 手机与电脑连同一 WiFi；② Windows 防火墙放行 5173/8080 入站（首次可临时关闭防火墙验证）；③ 确认 IP 没变（换网/重连路由会变，`ipconfig` 查）。
- **想临时用 Mock 模式**：把 `html/src/api/auth.js` 里 `USE_MOCK` 改成 `true`。

## 已知待完善（下次可做）
> 2026-11-18 复核：按代码实测逐条核对过，1/2/3/4 项均已解决，保留记录便于对照。

1. ~~token 是假的~~ **已完成**：`common/JwtUtil.java` 用 HS256 真实签发与校验，`config/AuthInterceptor.java` 拦截 `/api/**`；密钥取自 `falsegod.jwt.secret`。
2. ~~无鉴权接口~~ **已完成**：`GET /api/me` 已实现（`AuthController`），`App.vue` 挂载时用 `fetchMe` 校验 token；无 token 访问返回 401，失效则静默清本地会话。
3. ~~密码未加盐哈希~~ **已完成**：`common/PasswordUtil.java` 使用 BCrypt（随机盐），并兼容历史 MD5 数据、登录时自动升级。
4. ~~无后端单元测试~~ **已完成**：`src/test` 下有 4 个测试类（JwtUtil / PasswordUtil / PowerService / TaskService），共 57 个用例。
5. ~~子页面不进 URL~~ **已改进（轻量方案）**：子页面 key 会同步到 URL hash（如 `#power`、`#task`），刷新与分享可直达，
   浏览器前进/后退也能还原；未引入 vue-router，实现见 `HomePage.vue` 的 `page` ref + `popstate`。
6. 生产环境 **CORS 配置**（`config/CorsConfig.java`）已支持按配置项收紧来源，上线前记得改默认值。
7. **战力境界与神格阶位是同一条阶梯**：统一由 `common/RealmLadder` 定义（一印~真神），Lv.N = 第 N 个大境界。
   经验跨阶会把 `t_power.realm_index` 一起推上去，修行突破也会把经验补到该阶起点；「弃子」只是神堂判定，不是阶位名。
8. ~~公会名册未联动~~ **已完成**：`/api/guild` 与战力榜同一套做法，把当前玩家并入名册（该条 `self=true`）并返回
   `memberCount`；简报里「在册 N 人」由后端按真实人数改写（实测 7 → 8 人），不再是写死文案。
   `t_guild_info.members` 的「神弃者占六」相应改成「神弃者过半」，加人后依然成立。
9. **真神终局后 `daily_seal` 做不完**：真神不能再精进/突破，磨印双锤会永久停在不足 target 的状态，
   日课最多只能做满 9 条。当前没有真神账号，先记录；要修得给真神换一条不依赖境界的替代日课。
