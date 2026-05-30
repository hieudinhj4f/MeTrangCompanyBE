FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
# Chú ý: Dòng này sẽ lấy file jar trong mục target mà Hiếu build bằng Maven
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]