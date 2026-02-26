package dukku.ai.global;

import dukku.common.shared.ai.dto.HybridSearchResult;
import dukku.ai.entity.AiUserMemory;
import dukku.ai.out.AiUserMemoryRepository;
import dukku.ai.out.HybridSearchRepository;
import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import dukku.ai.global.policy.AiSimilarityPolicy;

import java.util.List;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiInitData {

    private final AiUserMemoryRepository aiUserMemoryRepository;
    private final JdbcTemplate jdbcTemplate;
    private final HybridSearchRepository hybridSearchRepository;

    private static final UUID USER_1_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_2_UUID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    /**
     * 스키마 보정: 모든 프로필에서 실행 (비파괴적)
     */
    @Bean
    public CommandLineRunner ensureSchema() {
        return args -> {
            log.info("🔧 [AiInitData] Schema Initialization Started");
            ensureVectorDimensions();
            log.info(" [AiInitData] Schema Initialization Completed");
        };
    }

    /**
     * 샘플 데이터: ai.init.enabled=true일 때만 실행 (기본값: true)
     */
    @Bean
    @ConditionalOnProperty(name = "ai.init.enabled", havingValue = "true", matchIfMissing = true)
    public CommandLineRunner initAiMemories() {
        return new CommandLineRunner() {
            @Override
            @Transactional
            public void run(String... args) {
                log.info(" [AiInitData] Sample Data Initialization Started");
                initMemories();
                initProductData();
                log.info(" [AiInitData] Sample Data Initialization Completed");
            }
        };
    }

    private void ensureVectorDimensions() {
        int dim = AiSimilarityPolicy.EMBEDDING_DIMENSION;
        if (!isVectorTypeAvailable()) {
            log.warn("[AiInitData] vector 타입 미지원 DB 환경 - AI 벡터 스키마 보정을 건너뜁니다.");
            return;
        }

        // PGroonga 확장 (미설치 환경에서는 키워드 검색 없이 벡터 검색만 동작)
        boolean pgroongaAvailable = false;
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pgroonga");
            pgroongaAvailable = true;
            log.info("[AiInitData] PGroonga 확장 활성화 완료");
        } catch (Exception e) {
            log.warn("[AiInitData] PGroonga 확장 없음 — 키워드 검색 비활성 (벡터 검색만 동작): {}", e.getMessage());
        }

        // vector 확장 (pgvector)
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            log.info("[AiInitData] pgvector 확장 활성화 완료");
        } catch (Exception e) {
            log.warn("[AiInitData] pgvector 확장 활성화 실패: {}", e.getMessage());
        }

        // ai_user_memory: CREATE IF NOT EXISTS (비파괴적)
        jdbcTemplate.execute(String.format("""
                CREATE TABLE IF NOT EXISTS ai_user_memory (
                    id SERIAL PRIMARY KEY,
                    uuid UUID NOT NULL UNIQUE,
                    user_uuid UUID NOT NULL,
                    memory_type VARCHAR(50) NOT NULL,
                    sub_type VARCHAR(50) NOT NULL,
                    content TEXT NOT NULL,
                    embedding vector(%d),
                    importance_score DOUBLE PRECISION NOT NULL,
                    access_count INTEGER NOT NULL DEFAULT 0,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP
                )""", dim));
        
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_user_memory_user_uuid ON ai_user_memory(user_uuid)");
        jdbcTemplate.execute(String.format(
                "CREATE INDEX IF NOT EXISTS idx_ai_user_memory_embedding ON ai_user_memory USING HNSW (embedding vector_cosine_ops)"));
        log.info("[AiInitData] ai_user_memory 테이블 확인 완료 ({}차원)", dim);

        // product_search: CREATE IF NOT EXISTS (비파괴적)
        jdbcTemplate.execute(String.format("""
                CREATE TABLE IF NOT EXISTS product_search (
                    id UUID PRIMARY KEY,
                    content TEXT NOT NULL,
                    metadata JSONB,
                    embedding vector(%d)
                )""", dim));

        if (pgroongaAvailable) {
            try {
                jdbcTemplate.execute(
                        "CREATE INDEX IF NOT EXISTS idx_product_search_content ON product_search USING pgroonga (content)");
            } catch (Exception e) {
                log.warn("[AiInitData] PGroonga 인덱스 생성 실패 (무시): {}", e.getMessage());
            }
        }
        jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_product_search_embedding ON product_search USING HNSW (embedding vector_cosine_ops)");
        log.info("[AiInitData] product_search 테이블 확인 완료 ({}차원)", dim);

        // 레거시 컬럼 정리: confidence_score (제거된 컬럼의 NOT NULL 제약 해제)
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE ai_user_memory ALTER COLUMN confidence_score DROP NOT NULL");
            log.info("[AiInitData] ai_user_memory.confidence_score NOT NULL 제약 해제 완료");
        } catch (Exception e) {
            // 이미 nullable이거나 컬럼이 없는 경우 무시
        }

        // ai_user_memory: 차원 불일치 시에만 ALTER
        try {
            Integer currentDim = jdbcTemplate.queryForObject("""
                    SELECT atttypmod FROM pg_attribute
                    WHERE attrelid = 'ai_user_memory'::regclass
                      AND attname = 'embedding'
                    """, Integer.class);
            if (currentDim != null && currentDim != dim) {
                jdbcTemplate.execute("UPDATE ai_user_memory SET embedding = NULL WHERE embedding IS NOT NULL");
                jdbcTemplate.execute(String.format(
                        "ALTER TABLE ai_user_memory ALTER COLUMN embedding TYPE vector(%d)", dim));
                log.info("[AiInitData] ai_user_memory.embedding 차원 변경: {} → {}", currentDim, dim);
            } else {
                log.info("[AiInitData] ai_user_memory.embedding 차원 정상 ({}차원)", dim);
            }
        } catch (Exception e) {
            log.warn("[AiInitData] ai_user_memory.embedding 차원 확인 실패 (무시): {}", e.getMessage());
        }
    }

    private boolean isVectorTypeAvailable() {
        try {
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT 1 FROM pg_type WHERE typname = 'vector' LIMIT 1",
                    Integer.class
            );
            return exists != null && exists == 1;
        } catch (Exception e) {
            log.warn("[AiInitData] vector 타입 확인 실패 (무시): {}", e.getMessage());
            return false;
        }
    }

    private void initMemories() {
        if (aiUserMemoryRepository.count() > 0) {
            log.info("[AiInitData] 기존 AI 메모리 데이터 존재 — 초기화 스킵");
            return;
        }

        log.info("[AiInitData] AI 메모리 초기화 시작");

        // ===== User 1 (세미콜론) - Product 서비스의 u1과 동일한 UUID =====
        saveMemory(USER_1_UUID, MemoryType.PROFILE, MemorySubType.GENERAL,
                "30대 남성 개발자. 캠핑과 전자기기에 관심이 많음.",
                0.95);

        saveMemory(USER_1_UUID, MemoryType.PREFERENCE, MemorySubType.SHOPPING,
                "가성비 좋은 캠핑 용품을 선호함. 경량 제품 위주로 구매. 브랜드는 헬리녹스, 스노우피크 선호.",
                0.85);

        saveMemory(USER_1_UUID, MemoryType.RECOMMENDATION, MemorySubType.SHOPPING,
                "장바구니 추가 기반 추천 (기준: 헬리녹스 체어제로 블랙): 스노우피크 랜드록 텐트, 스노우피크 티타늄 싱글머그 450, 힐레베르그 아틀라스 4인용",
                0.7);

        saveMemory(USER_1_UUID, MemoryType.RECOMMENDATION, MemorySubType.SHOPPING,
                "장바구니 추가 기반 추천 (기준: 소니 WF-1000XM5 무선이어폰): 소니 WH-1000XM5 무선 헤드폰, 에어팟 맥스 스페이스그레이, 젠하이저 HD660S2",
                0.7);

        // ===== User 2 (테크마스터) - Product 서비스의 u2와 동일한 UUID =====
        saveMemory(USER_2_UUID, MemoryType.PROFILE, MemorySubType.GENERAL,
                "전자기기 덕후. 최신 IT 기기 리뷰를 즐겨봄.",
                0.9);

        saveMemory(USER_2_UUID, MemoryType.PREFERENCE, MemorySubType.SHOPPING,
                "애플 생태계 선호. 맥북, 아이패드, 아이폰 시리즈에 관심.",
                0.8);

        saveMemory(USER_2_UUID, MemoryType.RECOMMENDATION, MemorySubType.SHOPPING,
                "장바구니 추가 기반 추천 (기준: 맥북 프로 14인치 M3 Pro): 아이패드 프로 12.9 M2, 맥북 에어 M2, LG 그램 17인치 2024",
                0.7);

        log.info("[AiInitData] AI 메모리 초기화 완료 (총 {}건)", aiUserMemoryRepository.count());
    }

    private void initProductData() {
        // product_search 테이블 데이터 개수 확인
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product_search", Integer.class);
            if (count != null && count > 0) {
                log.info("[AiInitData] 기존 product_search 데이터 존재 — 초기화 스킵 ({}건)", count);
                return;
            }
        } catch (Exception e) {
            log.warn("[AiInitData] product_search 개수 확인 실패: {}", e.getMessage());
        }

        // [안내] 아래 초기화 코드는 초기 개발/테스트용 샘플 데이터입니다.
        // 실제 환경에서는 ProductVectorSyncService를 통한 이벤트 기반 동기화를 사용합니다.
        String baseUrl = "https://semicolon.shop/products/";
        // {content, slug, price}
        String[][] products = {
                {"스노우피크 랜드록 텐트 - 캠핑용 거실형 텐트, 4-6인용", "snowpeak-landrock", "1890000"},
                {"스노우피크 티타늄 싱글머그 450 - 초경량 캠핑용 머그컵", "snowpeak-titanium-mug", "45000"},
                {"힐레베르그 아틀라스 4인용 - 돔형 캠핑 텐트", "hilleberg-atlas", "2100000"},
                {"헬리녹스 체어제로 블랙 - 초경량 백패킹 캠핑 의자", "helinox-chair-zero", "189000"},
                {"소니 WH-1000XM5 무선 헤드폰 - 노이즈캔슬링, 블루투스 헤드셋", "sony-wh1000xm5", "349000"},
                {"소니 WF-1000XM5 무선이어폰 - 노이즈캔슬링, 인이어 이어폰", "sony-wf1000xm5", "279000"},
                {"에어팟 맥스 스페이스그레이 - 애플 오버이어 헤드폰, 노이즈캔슬링", "airpods-max", "769000"},
                {"젠하이저 HD660S2 - 오픈형 레퍼런스 유선 헤드폰", "sennheiser-hd660s2", "599000"},
                {"맥북 프로 14인치 M3 Pro - 애플 노트북, 영상 및 개발용", "macbook-pro-14-m3", "2390000"},
                {"맥북 에어 M2 - 애플 경량 노트북, 휴대용", "macbook-air-m2", "1390000"},
                {"아이패드 프로 12.9 M2 - 애플 태블릿, 드로잉 및 영상 감상용", "ipad-pro-129-m2", "1549000"},
                {"LG 그램 17인치 2024 - 초경량 대화면 노트북, 사무용", "lg-gram-17-2024", "1890000"},
        };

        log.info("[AiInitData] product_search 샘플 데이터 삽입 시작");
        for (String[] product : products) {
            String text = product[0];
            String slug = product[1];
            String price = product[2];
            UUID id = UUID.nameUUIDFromBytes(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            float[] embedding = hybridSearchRepository.embed(text);
            String metadata = "{\"productUrl\":\"" + baseUrl + slug + "\",\"price\":" + price + ",\"saleStatus\":\"ON_SALE\"}";
            hybridSearchRepository.upsert(id, text, metadata, embedding);
        }
        log.info("[AiInitData] product_search 샘플 데이터 {}건 삽입 완료", products.length);

        // 하이브리드 검색 검증
        List<HybridSearchResult> verify = hybridSearchRepository.search("캠핑 의자", 3, 0.0);
        log.info("[AiInitData] 하이브리드 검색 검증: '캠핑 의자' → {}건", verify.size());
        verify.forEach(r -> log.info("  - [rrf={}, vector={}, keyword={}] {}",
                String.format("%.4f", r.rrfScore()),
                String.format("%.4f", r.vectorScore()),
                String.format("%.4f", r.keywordScore()),
                r.contentWithUrl()));
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
        aiUserMemoryRepository.save(memory);
    }

}
