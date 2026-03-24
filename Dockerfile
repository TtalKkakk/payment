FROM eclipse-temurin:17-jre

WORKDIR /app

# 운영 설정(application-prod.yml)이 /var/log/pg/pg.log 로 기록합니다.
RUN mkdir -p /var/log/pg

# Jenkins가 미리 생성해둔 Spring Boot jar를 Docker 빌드 컨텍스트(build/libs/*.jar)에서 복사합니다.
# (Docker 빌드 시점에는 소스/gradle이 필요하지 않습니다.)
COPY build/libs/*.jar app.jar

# 애플리케이션은 application-prod.yml의 server.port를 따릅니다.
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]

