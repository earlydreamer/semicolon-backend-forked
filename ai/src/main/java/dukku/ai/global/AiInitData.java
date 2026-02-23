package dukku.ai.global;

import dukku.ai.entity.AiUserMemory;
import dukku.ai.out.AiMemoryRepository;
import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import dukku.ai.global.policy.AiSimilarityPolicy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiInitData {

    private final AiMemoryRepository aiMemoryRepository;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    private static final UUID USER_1_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_2_UUID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Bean
    @Profile("dev")
    public CommandLineRunner initAiMemories() {
        return new CommandLineRunner() {
            @Override
            @Transactional
            public void run(String... args) {
                ensureVectorDimensions();

                if (aiMemoryRepository.count() > 0) {
                    log.info("📌 [AiInitData] 기존 AI 메모리 데이터 존재 — 초기화 스킵");
                    return;
                }

                log.info("🚀 [AiInitData] AI 메모리 초기화 시작");

                // ===== User 1 (세미콜론) =====
                // 프로필
                saveMemory(USER_1_UUID, MemoryType.PROFILE, MemorySubType.GENERAL,
                        "30대 남성 개발자. 캠핑과 전자기기에 관심이 많음.",
                        0.95);

                // 쇼핑 선호도
                saveMemory(USER_1_UUID, MemoryType.PREFERENCE, MemorySubType.SHOPPING,
                        "가성비 좋은 캠핑 용품을 선호함. 경량 제품 위주로 구매. 브랜드는 헬리녹스, 스노우피크 선호.",
                        0.85);

                // 추천 결과 (장바구니 기반)
                saveMemory(USER_1_UUID, MemoryType.RECOMMENDATION, MemorySubType.SHOPPING,
                        "장바구니 추가 기반 추천 (기준: 헬리녹스 체어제로 블랙): 스노우피크 랜드록 텐트, 스노우피크 티타늄 싱글머그 450, 힐레베르그 아틀라스 4인용",
                        0.7);

                saveMemory(USER_1_UUID, MemoryType.RECOMMENDATION, MemorySubType.SHOPPING,
                        "장바구니 추가 기반 추천 (기준: 소니 WF-1000XM5 무선이어폰): 소니 WH-1000XM5 무선 헤드폰, 에어팟 맥스 스페이스그레이, 젠하이저 HD660S2",
                        0.7);

                // ===== User 2 (테크마스터) =====
                saveMemory(USER_2_UUID, MemoryType.PROFILE, MemorySubType.GENERAL,
                        "전자기기 덕후. 최신 IT 기기 리뷰를 즐겨봄.",
                        0.9);

                saveMemory(USER_2_UUID, MemoryType.PREFERENCE, MemorySubType.SHOPPING,
                        "애플 생태계 선호. 맥북, 아이패드, 아이폰 시리즈에 관심.",
                        0.8);

                saveMemory(USER_2_UUID, MemoryType.RECOMMENDATION, MemorySubType.SHOPPING,
                        "장바구니 추가 기반 추천 (기준: 맥북 프로 14인치 M3 Pro): 아이패드 프로 12.9 M2, 맥북 에어 M2, LG 그램 17인치 2024",
                        0.7);

                log.info(" [AiInitData] AI 메모리 초기화 완료 (총 {}건)", aiMemoryRepository.count());

                // ===== VectorStore =====
                // [안내] 아래 초기화 코드는 초기 개발/테스트용 샘플 데이터입니다.
                // 실제 환경에서는 ProductVectorSyncService를 통한 이벤트 기반 동기화를 사용합니다.
                log.info(" [AiInitData] VectorStore 상품 데이터 초기화 시작");
                List<Document> products = List.of(
                        productDocument("스노우피크 랜드록 텐트 - 캠핑용 거실형 텐트, 4-6인용"),
                        productDocument("스노우피크 티타늄 싱글머그 450 - 초경량 캠핑용 머그컵"),
                        productDocument("힐레베르그 아틀라스 4인용 - 돔형 캠핑 텐트"),
                        productDocument("헬리녹스 체어제로 블랙 - 초경량 백패킹 캠핑 의자"),
                        productDocument("소니 WH-1000XM5 무선 헤드폰 - 노이즈캔슬링, 블루투스 헤드셋"),
                        productDocument("소니 WF-1000XM5 무선이어폰 - 노이즈캔슬링, 인이어 이어폰"),
                        productDocument("에어팟 맥스 스페이스그레이 - 애플 오버이어 헤드폰, 노이즈캔슬링"),
                        productDocument("젠하이저 HD660S2 - 오픈형 레퍼런스 유선 헤드폰"),
                        productDocument("맥북 프로 14인치 M3 Pro - 애플 노트북, 영상 및 개발용"),
                        productDocument("맥북 에어 M2 - 애플 경량 노트북, 휴대용"),
                        productDocument("아이패드 프로 12.9 M2 - 애플 태블릿, 드로잉 및 영상 감상용"),
                        productDocument("LG 그램 17인치 2024 - 초경량 대화면 노트북, 사무용")
                );
                vectorStore.add(products);
                log.info(" [AiInitData] VectorStore 상품 데이터 {}건 upsert 완료", products.size());

                // VectorStore 검증
                List<Document> verify = vectorStore.similaritySearch(
                        SearchRequest.builder().query("캠핑 의자").topK(3).similarityThreshold(0.0).build()
                );
                log.info("[AiInitData] VectorStore 검증: '캠핑 의자' 검색 → {}건 (threshold=0.0)", verify.size());
                verify.forEach(doc -> log.info("  - [score={}] {}", String.format("%.4f", doc.getScore()), doc.getText()));
            }
        };
    }

    private void ensureVectorDimensions() {
        int dim = AiSimilarityPolicy.EMBEDDING_DIMENSION;

        // vector_store: PgVectorStore의 initialize-schema는 CREATE IF NOT EXISTS만 실행하므로
        // 차원 변경 시 기존 테이블을 drop 후 재생성 필요
        jdbcTemplate.execute("DROP TABLE IF EXISTS vector_store");
        jdbcTemplate.execute(String.format("""
                CREATE TABLE vector_store (
                    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
                    content text,
                    metadata json,
                    embedding vector(%d)
                )""", dim));
        jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS spring_ai_vector_index ON vector_store USING HNSW (embedding vector_cosine_ops)");
        log.info("[AiInitData] vector_store 테이블 재생성 ({}차원)", dim);

        // ai_memory: Hibernate ddl-auto=update는 기존 컬럼 타입을 변경하지 않으므로 수동 ALTER
        try {
            jdbcTemplate.execute("UPDATE ai_memory SET embedding = NULL WHERE embedding IS NOT NULL");
            jdbcTemplate.execute(String.format(
                    "ALTER TABLE ai_memory ALTER COLUMN embedding TYPE vector(%d)", dim));
            log.info("[AiInitData] ai_memory.embedding 차원 업데이트 ({}차원)", dim);
        } catch (Exception e) {
            // 첫 실행 시 테이블 미존재 또는 이미 올바른 차원 — 무시
        }
    }

    private void saveMemory(UUID userUuid, MemoryType memoryType, MemorySubType subType,
                            String content, double importanceScore) {
        AiUserMemory memory = AiUserMemory.builder()
                .userUuid(userUuid)
                .memoryType(memoryType)
                .subType(subType)
                .content(content)
                .importanceScore(importanceScore)
                .build();
        aiMemoryRepository.save(memory);
    }

    private static Document productDocument(String text) {
        String id = UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8)).toString();
        return new Document(id, text, Map.of());
    }
}
