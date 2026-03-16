# 🎨 DUKKU (덕쿠) - 덕질을 더하다

<div align="center">

![DUKKU Logo](https://img.shields.io/badge/Service-DUKKU-FF6B6B?style=for-the-badge)

**"나만의 굿즈를 더 가치 있게"** **DUKKU는 팬덤 문화를 위한 통합 굿즈 이커머스 및 커뮤니티 플랫폼 백엔드 서비스입니다.**

[서비스 상세 기획 (Notion)](https://www.notion.so/5-DUKKU-2fc15a01205480b0b573fd00f2e0ee23) | [배포 핸드오버](deployment_handover.md) | [변경 이력](docs/change-history.md)

</div>

---

## 📖 서비스 개요 (Overview)

**DUKKU(덕쿠)** 는 단순한 상품 판매를 넘어, 팬들이 진심을 다해 활동하는 '덕질'의 가치를 극대화하기 위해 기획되었습니다. 아티스트 굿즈 판매, 한정판 예약 구매, 그리고 팬들 간의 소통을 아우르는 고성능 마이크로서비스 아키텍처(MSA)를 지향합니다.

본 프로젝트는 **9개의 독립적인 마이크로서비스와 1개의 통합 모니터링 서비스**로 구성되어 있으며, 대규모 트래픽 처리와 안정적인 결제, 데이터 기반의 관찰 가능성(Observability)을 실천하는 데 중점을 두었습니다.

---

## ✨ 주요 기능 및 서비스 (Features & Services)

실제 구현된 도메인별 서비스와 인프라 스택의 핵심 기능입니다.

### 🛍️ 상품 및 주문 (Product & Order)
- **스마트 굿즈 카탈로그:** 아티스트별/카테고리별 필터링과 상세 재고 관리를 지원하는 상품 시스템입니다.
- **주문 처리 라이프사이클:** 선착순 및 예약 구매 시 발생하는 트래픽 집중을 고려하여 주문의 생성부터 이행까지의 전 과정을 안정적으로 처리합니다.

### 💳 결제 및 혜택 (Payment & Benefits)
- **통합 결제 및 예치금:** 결제 승인/환불 프로세스와 더불어 사용자별 예치금 충전 및 잔액 관리 기능을 제공합니다.
- **쿠폰 프로모션:** 등급별, 이벤트별 쿠폰 발행 및 사용 로직을 통해 유저 혜택을 극대화합니다.
- **자동화된 정산:** 입점사 및 파트너사 간의 복잡한 금융 트랜잭션을 자동화하여 정산 처리를 수행합니다.

### 🔐 보안 및 지능화 (Auth & AI)
- **사용자 인증 관리:** 전용 `auth` 서비스를 통해 보안 로그인, 회원가입 및 JWT 기반의 권한 체계를 운영합니다.
- **AI 개인화 서비스:** 사용자 활동 로그를 분석하여 취향에 맞는 아티스트나 굿즈를 제안하는 독립적인 AI 모듈을 통합했습니다.

### 🛠️ 인프라 및 관찰 가능성 (Infrastructure & Observability)
- **9+1 서비스 구조:** 9개의 비즈니스 마이크로서비스와 이들을 지원하는 **1개의 전용 모니터링 서비스** 체계입니다.
- **실시간 관찰성:** Prometheus, Grafana와 **MongoDB**를 연동하여 시스템 메트릭과 서비스 로그를 실시간으로 수집하고 분석합니다.
- **클라우드 네이티브:** Docker와 Kubernetes(K8s)를 활용하여 확장성과 회복 탄력성을 갖춘 환경에 배포됩니다.
---

## 🛠️ 기술 스택 (Tech Stack)

**Backend Core:**
![Java 25](https://img.shields.io/badge/Java_25-007396?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot 4](https://img.shields.io/badge/Spring_Boot_4-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)

**Database & Storage:**
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white)

**Event Streaming:**
![Redpanda](https://img.shields.io/badge/Redpanda-000000?style=for-the-badge&logo=redpanda&logoColor=white)

**Infrastructure & DevOps:**
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Docker Compose](https://img.shields.io/badge/Docker_Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white)
![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white)
![Grafana](https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white)
</div>

---

## 📁 프로젝트 구조 (Project Structure)

멀티 모듈 Gradle 구조를 채택하여 각 디렉토리가 독립적인 서비스 또는 공용 모듈을 나타냅니다.

```
semicolon-backend-forked/
├── ai/                            # AI 기능 마이크로서비스
├── auth/                          # 인증 및 인가 마이크로서비스
├── common/                        # 공통 유틸리티 및 설정 공유 모듈
├── coupon/                        # 쿠폰 관리 마이크로서비스
├── deposit/                       # 예치금 관리 마이크로서비스
├── docs/                          # 프로젝트 문서 및 사양서
├── docker-compose.local.*.yml     # 로컬 개발용 인프라 구성 (DB, 모니터링, Nginx 등)
├── k8s/                           # 클러스터 배포를 위한 Kubernetes 매니페스트
├── monitoring/                    # 모니터링 설정 (Prometheus, MongoDB 구성 등)
├── nginx-conf/                    # Nginx 서버 설정 파일
├── order/                         # 주문 관리 마이크로서비스
├── payment/                       # 결제 처리 마이크로서비스
├── product/                       # 상품 카탈로그 관리 마이크로서비스
├── settlement/                    # 정산 처리 마이크로서비스
├── user/                          # 사용자 관리 마이크로서비스
└── build.gradle                   # 전체 프로젝트 빌드 설정
```

---

## 🤝 기여 (Contributing)

Semicolon 프로젝트에 대한 기여를 환영합니다! 상세 지침은 내부 문서를 참조해 주세요.

---

<div align="center">



**나만의 덕질 파트너, DUKKU** Made with ❤️ by `prgrms-be-adv-devcourse Team 5 Semicolon`

</div>
