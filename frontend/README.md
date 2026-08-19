# 云盘前端 (cloud-drive-web)

基于 Vue 3 + Element Plus + Vite 的蓝白风格网盘前端，配套 [cloud-drive](https://github.com/your-repo/cloud-drive) 后端使用。

## 技术栈

- **Vue 3** — 组合式 API + `<script setup>`
- **Vite 5** — 构建工具
- **Element Plus** — UI 组件库（按需自动导入）
- **Pinia** — 状态管理
- **Vue Router 4** — 路由
- **Axios** — HTTP 请求
- **cos-js-sdk-v5** — 腾讯云 COS 直传

## 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务器（默认端口 5173）
npm run dev

# 生产构建
npm run build
```

## 环境变量

开发环境配置位于 `.env.development`：

| 变量 | 说明 |
|------|------|
| `VITE_COS_BUCKET` | 腾讯云 COS 存储桶名称（格式 `bucket-appid`） |
| `VITE_COS_REGION` | 存储桶地域（如 `ap-guangzhou`） |

> ⚠️ 必须与后端 `application.yml` 中 `tencent.cos.bucket-name` 一致。

## 代理配置

开发服务器通过 Vite 代理将 `/api` 转发到后端：

```
/api/user/login  →  http://localhost:8080/user/login
```

## 页面路由

| 路径 | 说明 | 权限 |
|------|------|------|
| `/login` | 登录 | 公开 |
| `/register` | 注册 | 公开 |
| `/app/files` | 全部文件（核心页面） | 登录 |
| `/app/applications` | 我的扩容申请 | 登录 |
| `/app/recycle` | 回收站（占位） | 登录 |
| `/app/admin/users` | 用户管理 | 管理员 |
| `/app/admin/approvals` | 扩容审批 | 管理员 |

## 功能实现状态

| 功能 | 状态 |
|------|------|
| 注册 / 登录 / 修改密码 | ✅ |
| 文件列表（列表 / 网格视图） | ✅ |
| 新建文件夹 | ✅ |
| 文件上传（COS 直传 + 进度） | ✅ |
| 文件下载（预签名 URL） | ✅ |
| 空间扩容申请 / 审批 | ✅ |
| 用户禁用 / 启用 | ✅ |
| 回收站 | ⏳ 后端待实现 |
| 文件删除 / 重命名 | ⏳ 后端待实现 |
