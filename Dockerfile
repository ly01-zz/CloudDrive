# ============================================================
# 云盘后端镜像（多阶段构建）
# 阶段一：Maven 构建 jar
# 阶段二：JRE 运行（镜像小）
# 配置通过环境变量注入（见 docker-compose.yml / .env）
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 先拷贝 pom 并预下载依赖（利用构建缓存，源码改动不重下依赖）
COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
