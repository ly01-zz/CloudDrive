# ============================================================
# 云盘前端镜像（多阶段构建）
# 阶段一：Node 构建 dist（生产配置来自 .env.production）
# 阶段二：Nginx 托管 + /api 反向代理到后端容器
# ============================================================
FROM node:20-alpine AS build
WORKDIR /app

# 先拷贝依赖清单并安装（利用构建缓存）
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---------- 运行阶段 ----------
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
