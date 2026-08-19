# ☁️ 云盘前端 (cloud-drive-web)

基于 Vue 3 + Element Plus + Vite 的蓝白风格网盘前端，配套 [cloud-drive](https://github.com/ly01-zz/CloudDrive) 后端使用。

## 技术栈

- **Vue 3** — 组合式 API + `<script setup>`
- **Vite 5** — 构建工具（unplugin 自动导入组件/API）
- **Element Plus** — UI 组件库
- **Pinia** — 状态管理
- **Vue Router 4** — 路由（history 模式）
- **Axios** — HTTP 请求（统一拦截器：token 附加、错误处理）
- **cos-js-sdk-v5** — 腾讯云 COS 直传
- **Web Crypto API** — 文件 SHA-256 计算（秒传）

## 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务器（默认端口 5173，/api 代理到 localhost:8080）
npm run dev

# 生产构建（使用 .env.production 配置）
npm run build
```

## 环境变量

| 文件 | 说明 |
|---|---|
| `.env.development` | 开发环境（`npm run dev` 加载） |
| `.env.production` | 生产环境（`npm run build` 加载） |

| 变量 | 说明 |
|------|------|
| `VITE_COS_BUCKET` | 腾讯云 COS 存储桶名称（格式 `bucket-appid`） |
| `VITE_COS_REGION` | 存储桶地域（如 `ap-guangzhou`） |

> ⚠️ 必须与后端 `application.yml` 中 `tencent.cos.bucket-name` 一致，否则直传失败。

## 页面路由

| 路径 | 说明 | 权限 |
|------|------|------|
| `/login` `/register` | 登录 / 注册 | 公开 |
| `/s/:shareCode` | 分享访问页（提取码 + 下载） | 公开 |
| `/app/files` | 全部文件（上传/秒传/下载/分享/删除） | 登录 |
| `/app/shares` | 我的分享管理 | 登录 |
| `/app/applications` | 我的扩容申请 | 登录 |
| `/app/recycle` | 回收站（恢复/永久删除） | 登录 |
| `/app/admin/dashboard` | 数据看板 | 管理员 |
| `/app/admin/users` | 用户管理（禁用/重置流量/调整配额） | 管理员 |
| `/app/admin/approvals` | 扩容审批 | 管理员 |
| `/app/admin/shares` | 分享管理（强制取消） | 管理员 |
| `/app/admin/files` | 文件治理（搜索/恢复/彻底删除） | 管理员 |
| `/app/admin/logs` | 操作日志 | 管理员 |
| `/app/admin/announcement` | 公告管理 | 管理员 |
| `/app/admin/config` | 系统配置 | 管理员 |

## 功能实现状态

| 功能 | 状态 |
|------|------|
| 注册 / 登录 / 修改密码 / 个人资料 | ✅ |
| 文件列表（列表 / 网格双视图） | ✅ |
| 新建文件夹 | ✅ |
| 文件上传（COS 直传 + 进度 + 失败清理回滚） | ✅ |
| **秒传**（SHA-256 检查，命中直接完成） | ✅ |
| 文件下载 / 删除（回收站）/ 恢复 / 永久删除 | ✅ |
| 分享（公开/私密 + 提取码 + 限制项） | ✅ |
| 空间扩容申请 / 审批 | ✅ |
| 管理后台（看板/用户/分享/文件/日志/公告/配置） | ✅ |
| 系统公告弹窗（登录后展示一次） | ✅ |
