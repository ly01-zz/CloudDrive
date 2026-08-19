# 云盘 Docker 部署指南

## 一、服务器准备

1. 安装 Docker 与 Compose 插件：
   ```bash
   curl -fsSL https://get.docker.com | sh
   sudo systemctl enable --now docker
   docker compose version   # 确认可用
   ```
2. 开放端口：80（HTTP，HTTPS 时 443）

## 二、目录结构（服务器上）

```bash
mkdir -p /opt/cloud-drive/{mysql,backend,frontend}
cd /opt/cloud-drive
```

| 目录/文件 | 来源 |
|---|---|
| `docker-compose.yml` | 后端项目根目录 |
| `.env.example` → `.env` | 后端项目根目录，`cp .env.example .env` 后填写真实密钥 |
| `mysql/init.sql` | 后端项目 `mysql/` 目录 |
| `backend/` | 后端源码全部内容（含 `Dockerfile`、`pom.xml`、`src/`） |
| `frontend/` | 前端源码全部内容（含 `Dockerfile`、`nginx.conf`、`package.json`） |

推荐用 git 传输代码（服务器上 `git clone` 两个仓库），再单独维护 `.env`。

## 三、配置 .env（密钥不落 git）

```bash
vim .env   # 参考 .env.example 填写：
```
- `DB_PASSWORD` / `REDIS_PASSWORD`：随机强密码
- `JWT_SECRET`：`openssl rand -hex 32` 生成
- `COS_SECRET_ID` / `COS_SECRET_KEY`：腾讯云 API 密钥（控制台 → 访问管理 → API密钥管理）
- `COS_BUCKET_NAME`：`yunpan-1341779125`（如存储桶未变）

## 四、启动

```bash
docker compose up -d --build
docker compose ps                 # 4 个容器均应 Up
docker compose logs -f backend    # 跟踪后端日志
```

浏览器访问 `http://服务器IP`：
- 管理员：`13800000000` / `admin123`（**首次登录后立即改密码**）
- 测试：注册普通账号 → 上传/下载/秒传/分享 → 管理后台全流程

## 五、数据迁移（保留旧库数据时）

```bash
# 旧服务器/本机导出
mysqldump -h localhost -u root -p cloud_drive > cloud_drive_dump.sql
# 新环境导入（在 mysql 容器初始化完成后）
docker exec -i cloud-drive-mysql mysql -uroot -p"$DB_PASSWORD" cloud_drive < cloud_drive_dump.sql
```

## 六、备份

```bash
# 数据库
docker exec cloud-drive-mysql mysqldump -uroot -p"$DB_PASSWORD" cloud_drive > backup_$(date +%F).sql
# 定时（crontab 每周一次）：
# 0 3 * * 0  docker exec cloud-drive-mysql mysqldump ... > /opt/cloud-drive/backup/backup_$(date +\%F).sql
```

## 七、HTTPS（强烈建议，秒传依赖 crypto.subtle）

```bash
# 服务器安装 certbot 后：
certbot --nginx -d 你的域名
# 或手动在 nginx.conf 加证书（frontend 容器需挂载证书目录）
```

## 八、常用运维命令

```bash
docker compose logs -f <service>     # 日志
docker compose restart backend       # 重启后端
docker compose down                  # 停（保留数据卷）
docker compose down -v               # 停并删数据（慎用！）
docker compose build --no-cache backend   # 强制重新构建
```

## 九、定时任务（内置）

| 任务 | 触发时机 | 说明 |
|---|---|---|
| 月度流量清零 | 每月 1 号 00:00 | 批量清零所有用户 `used_download_traffic`，与下载时的懒重置双保险 |
| 回收站过期清理 | 每天 01:00 | 物理删除回收站中超 15 天的文件（含 COS 对象，秒传共享保护）|

## 十、已知边界

- 秒传依赖 Redis（容器内已配密码）；Redis 故障时自动降级为普通上传
- JWT 24 小时过期，无刷新机制，用户需每日重新登录
