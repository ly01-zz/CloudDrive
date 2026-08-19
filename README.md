# ☁️ CloudDrive 云盘

基于 **Spring Boot 3 + Vue 3 + MySQL + Redis + 腾讯云 COS** 的多人云盘系统，支持文件秒传、分享、回收站、流量限额与完整的管理后台。

## ✨ 功能特性

### 用户端
| 模块 | 说明 |
|---|---|
| 文件管理 | 上传（COS 直传 + 断点续传式 STS 临时密钥）、下载、新建文件夹、重命名目录浏览、列表/网格双视图 |
| **秒传** | 前端计算 SHA-256 → Redis 缓存命中即秒传；存储采用**内容寻址**（`user-files/sha/{sha256}`），同内容只存一份，多用户共享引用并带删除保护 |
| 分享 | 公开/私密（提取码）分享，支持有效期、访问/下载次数、下载流量限制，一键复制链接 |
| 回收站 | 逻辑删除可恢复，超 15 天定时自动清理 |
| 扩容申请 | 申请空间扩容，管理员审批后自动累加 |
| 月度流量 | 每月限额，跨月自动清零（定时任务 + 下载时懒重置双保险），侧边栏实时展示 |
| 系统公告 | 管理员发布广播公告，登录后弹窗通知 |

### 管理后台
| 模块 | 说明 |
|---|---|
| 数据看板 | 用户数/冻结数、存储占用、本月下载流量、有效分享数 + 近 7 天下载趋势 |
| 用户管理 | 禁用/启用、**重置流量**（记录原因）、**调整空间/流量配额** |
| 扩容审批 | 通过/拒绝，通过后自动增加用户空间 |
| 分享管理 | 全量分享列表（含创建者），可强制取消违规分享 |
| 文件治理 | 按文件名/手机号/回收站状态搜索所有文件，支持恢复、彻底删除（含级联） |
| 操作日志 | 管理员关键操作全程留痕（操作人/IP/原因） |
| 公告管理 | 发布/下架/删除公告 |
| 系统配置 | 在线修改注册上限、默认空间、月度流量等配置，实时生效 |

### 定时任务
- **每月 1 号 00:00**：批量清零所有用户月度下载流量
- **每天 01:00**：物理清理回收站中超 15 天的文件（含 COS 对象，秒传共享保护）

## 🛠 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17 / Spring Boot 3.4 / MyBatis-Plus 3.5 / Spring Security (JWT) |
| 存储 | MySQL 8（逻辑删除）/ Redis 7（秒传缓存）/ 腾讯云 COS（文件存储 + STS 临时密钥直传） |
| 前端 | Vue 3 (Composition API) / Element Plus / Pinia / Vite / cos-js-sdk-v5 |
| 部署 | Docker / docker-compose / Nginx（history 路由 + API 反向代理） |

## 📁 项目结构

```
cloud-drive/
├── src/main/java/cn/bvovd/clouddrive/
│   ├── config/        # 配置类（Redis/COS/Security/JWT 拦截器注册）
│   ├── controller/    # 接口层（用户端 + admin 管理端）
│   ├── service/       # 业务层
│   ├── mapper/        # MyBatis-Plus Mapper（含自定义 SQL）
│   ├── entity/ dto/ vo/
│   ├── task/          # 定时任务（月度流量清零、回收站清理）
│   ├── utils/         # 工具类（JWT/Redis/COS 清理）
│   └── exception/     # 全局异常处理
├── frontend/          # Vue3 前端
├── mysql/init.sql     # 数据库初始化脚本（Docker 首次启动自动执行）
├── docker-compose.yml # 一键部署编排
└── DEPLOY.md          # 部署指南（Docker 详细步骤、备份、HTTPS）
```

## 🚀 快速开始

### 方式一：本地开发

**环境要求**：JDK 17、Maven、MySQL 8、Redis、Node 18+、腾讯云 COS 存储桶

```bash
# 1. 初始化数据库
mysql -uroot -p < mysql/init.sql

# 2. 后端：配置环境变量后启动（IDEA Run Configuration → Environment variables）
#    COS_SECRET_ID=xxx;COS_SECRET_KEY=xxx;COS_BUCKET_NAME=你的存储桶名
mvnw spring-boot:run

# 3. 前端
cd frontend
npm install
npm run dev          # http://localhost:5173
```

### 方式二：Docker 一键部署

```bash
git clone https://github.com/ly01-zz/CloudDrive.git
cd CloudDrive
cp .env.example .env        # 填写 DB/Redis/JWT/COS 密钥
docker compose up -d --build
# 访问 http://服务器IP
```

详细步骤、数据迁移、备份、HTTPS 配置见 **[DEPLOY.md](DEPLOY.md)**。

## 👤 默认账号

| 角色 | 手机号 | 密码 |
|---|---|---|
| 管理员 | `13800000000` | `admin123`（**首次登录后请立即修改**）|

普通用户通过注册页自助注册。

## 🗄 数据库设计（8 张表）

```
users ─┬─ files ──────────── share_links（分享，1 文件多分享）
       ├─ space_applications（扩容申请，审批关联 admin）
       ├─ download_logs（下载明细，统计/审计）
       └─ system_config（注册上限/默认空间/月度流量，在线可改）

admin_log（管理员操作日志）      announcements（系统公告，广播）
```

设计要点：
- **秒传**：`files.file_sha256` + Redis 缓存；存储路径内容寻址，删除时按引用计数保护共享对象
- **逻辑删除**：`deleted_at` 标记回收站，`@TableLogic` 自动过滤；永久删除走自定义 SQL
- **流量限额**：`traffic_reset_time` 月份锚点判断跨月，定时任务 + 懒重置双保险

## 📄 相关文档

- [部署指南（Docker/备份/HTTPS）](DEPLOY.md)
- [前端说明](frontend/README.md)

## 📝 License

本项目仅用于学习交流。
