// DB 선택 (application.yml의 mongodb uri와 일치)
db = db.getSiblingDB('log_service');

// 컬렉션 생성
db.createCollection('service_logs');

// 1. traceId 인덱스 (분산 추적)
db.service_logs.createIndex(
  { "traceId": 1 },
  { name: "idx_traceId" }
);

// 2. 복합 인덱스 (서비스별 + 레벨별 + 시간순)
db.service_logs.createIndex(
  { "serviceName": 1, "level": 1, "timestamp": -1 },
  { name: "idx_service_level_time" }
);

// 3. TTL 인덱스 (30일 후 자동 삭제)
db.service_logs.createIndex(
  { "timestamp": 1 },
  { expireAfterSeconds: 2592000, name: "idx_ttl_30days" }
);

// 4. 텍스트 검색 인덱스 (메시지 검색용)
db.service_logs.createIndex(
  { "message": "text" },
  { name: "idx_message_text" }
);

print("=== log_service 컬렉션 및 인덱스 생성 완료 ===");
