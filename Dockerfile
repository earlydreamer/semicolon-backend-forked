# syntax=docker/dockerfile:1
# [Stage 1] 빌드 단계
FROM public.ecr.aws/docker/library/eclipse-temurin:25-jdk AS builder

WORKDIR /app

# 1. Gradle 래퍼 및 설정 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 2. 모든 모듈 소스 복사 (필요한 모듈만 빌드되도록 최적화 가능하지만, 구조상 전체 복사가 안전)
COPY common common
COPY auth auth
COPY user user
COPY product product
COPY order order
COPY payment payment
COPY deposit deposit
COPY settlement settlement
COPY coupon coupon
COPY ai ai
COPY monitoring monitoring

# 3. 빌드 실행 (MODULE_NAME을 인자로 받음)
ARG MODULE_NAME
RUN chmod +x ./gradlew
RUN ./gradlew :${MODULE_NAME}:bootJar -x test --no-daemon

# 빌드된 JAR 파일을 고정된 위치로 복사 (서브 모듈 경로 대응)
RUN find . -path "*/build/libs/*.jar" ! -name "*-plain.jar" -exec cp {} /app/app.jar \;

# [Stage 2] 실행 단계
FROM public.ecr.aws/docker/library/eclipse-temurin:25-jre

WORKDIR /app

# 4. 빌드된 JAR 파일 복사
COPY --from=builder /app/app.jar app.jar

# 5. 실행
ENTRYPOINT ["java", "-jar", "app.jar"]

# trigger full arm64 rebuild
