# 1단계: 빌드
FROM gradle:8-jdk17 AS build
WORKDIR /app
COPY .. .
RUN gradle bootJar --no-daemon

# 2단계: 실행 (가벼운 이미지로 실제 구동)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]