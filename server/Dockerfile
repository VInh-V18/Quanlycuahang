# Multi-stage build (Phase 12) — stage 1 bien dich bang Maven, stage 2 chi mang theo JRE + jar da
# build de image chay that gon nhe, khong keo theo toan bo JDK/Maven cache vao production image.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy rieng pom.xml truoc de tan dung Docker layer cache — chi tai lai dependency khi pom.xml
# doi, khong phai moi lan sua code Java.
COPY pom.xml .
RUN mvn -B dependency:go-offline -q

COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S erp && adduser -S erp -G erp
COPY --from=build /app/target/server-*.jar app.jar
RUN mkdir -p /app/uploads && chown -R erp:erp /app
USER erp

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

# JDK 21 tu bat container-aware ergonomics (MaxRAMPercentage mac dinh 25%) — voi container 1g
# rieng (docker-compose.prod.yml), 25% chi ~256MB heap cho ca Hikari pool + Hibernate + Jackson,
# kha eo hep khi khong co gi khac chia se container nay (phat hien qua audit production readiness
# 2026-07-17). Nang len 75% de tan dung tot hon ngan sach bo nho da danh rieng cho container. Dat
# qua JAVA_TOOL_OPTIONS (khong hardcode vao ENTRYPOINT) de van override duoc qua bien moi truong
# neu can chinh lai o 1 lan trien khai cu the ma khong phai build lai image.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java", "-jar", "app.jar"]
