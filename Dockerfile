# 使用多阶段构建
# 第一阶段：构建环境
FROM maven:3.8-amazoncorretto-8 AS builder

# 设置工作目录
WORKDIR /app

# 复制 pom.xml
COPY pom.xml .

# 复制源代码
COPY src ./src

# 构建应用
RUN mvn clean package

# 第二阶段：运行环境
FROM amazoncorretto:8

# 设置工作目录
WORKDIR /app

# 创建存放图片和提示文件的目录
RUN mkdir -p /app/data

# 从构建阶段复制构建好的 jar 文件
COPY --from=builder /app/target/BedrockRuntime-1.0-SNAPSHOT.jar app.jar

# 复制需要的资源文件
COPY src/main/java/com/example/bedrockruntime/models/anthropicClaude/prompt.txt /app/data/
COPY test.png /app/data/

# 设置环境变量
ENV AWS_REGION=eu-central-1
ENV PROMPT_PATH=/app/data/prompt.txt
ENV IMAGE_PATH=/app/data/test.png

# 暴露端口（如果需要）
# EXPOSE 8080

# 设置容器启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
