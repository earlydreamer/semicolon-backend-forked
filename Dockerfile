# [Stage 1] 빌드 단계
FROM eclipse-temurin:25-jdk AS builder

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

# 3. 빌드 실행 (MODULE_NAME을 인자로 받음)
ARG MODULE_NAME
RUN chmod +x ./gradlew
# clean 제거: 일부 모듈 빌드 시 의존성(common) 결과물이 삭제될 가능성 방지 및 캐싱 활용
RUN ./gradlew :${MODULE_NAME}:bootJar -x test

# plain JAR 제거 (COPY 시 모호성 방지)
RUN rm -f ${MODULE_NAME}/build/libs/*-plain.jar

# [Stage 2] 실행 단계
FROM eclipse-temurin:25-jre

WORKDIR /app

ARG MODULE_NAME

# 4. 빌드된 JAR 파일 복사
COPY --from=builder /app/${MODULE_NAME}/build/libs/*.jar app.jar

# 5. 실행
ENTRYPOINT ["java", "-jar", "app.jar"]