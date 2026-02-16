// MongoDB 초기화 스크립트
db = db.getSiblingDB('log_service');

// 로그 컬렉션 생성 (명시적)
db.createCollection('service_logs');

// 인덱스는 Spring Data MongoDB의 @CompoundIndex, @Indexed가 자동 생성
print('MongoDB initialized: log_service database created');
