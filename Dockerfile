# ==========================================
# GIAI ĐOẠN 1: BẢO DOCKER TỰ CHẠY MAVEN ĐỂ BUILD CODE
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
# Copy file cấu hình Maven và toàn bộ code vào
COPY pom.xml .
COPY src ./src
# Lệnh này tương đương với việc bạn bấm nút "Build" ở máy tính
RUN mvn clean package -DskipTests

# ==========================================
# GIAI ĐOẠN 2: CHẠY FILE JAR VỪA BUILD ĐƯỢC
# ==========================================
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
# Sang Giai đoạn 1 (build), lấy cái file .jar vừa tạo ra đem về đây
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]