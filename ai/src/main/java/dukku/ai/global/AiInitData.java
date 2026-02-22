package dukku.ai.global;

import dukku.ai.entity.AiMemory;
import dukku.ai.out.AiMemoryRepository;
import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiInitData {

    private final AiMemoryRepository aiMemoryRepository;
    private final VectorStore vectorStore;

    private static final UUID USER_1_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_2_UUID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Bean
    public CommandLineRunner initAiMemories() {
        return new CommandLineRunner() {
            @Override
            @Transactional
            public void run(String... args) {
                if (aiMemoryRepository.count() > 0) {
                    log.info("📌 [AiInitData] 기존 AI 메모리 데이터 존재 — 초기화 스킵");
                    return;
                }

                log.info("🚀 [AiInitData] AI 메모리 초기화 시작");

                // ===== User 1 (세미콜론) =====
                // 프로필
                saveMemory(USER_1_UUID, MemoryType.PROFILE, MemorySubType.GENERAL,
                        "30대 남성 개발자. 캠핑과 전자기기에 관심이 많음.",
                        0.95, 0.9);

                // 쇼핑 선호도
                saveMemory(USER_1_UUID, MemoryType.PREFERENCE, MemorySubType.SHOPPING,
                        "가성비 좋은 캠핑 용품을 선호함. 경량 제품 위주로 구매. 브랜드는 헬리녹스, 스노우피크 선호.",
                        0.85, 0.8);

                // 추천 결과 (장바구니 기반)
                saveMemory(USER_1_UUID, MemoryType.RECOMMENDATION, MemorySubType.SHOPPING,
                        "장바구니 추가 기반 추천 (기준: 헬리녹스 체어제로 블랙): 스노우피크 랜드록 텐트, 스노우피크 티타늄 싱글머그 450, 힐레베르그 아틀라스 4인용",
                        0.7, 0.8);

                saveMemory(USER_1_UUID, MemoryType.RECOMMENDATION, MemorySubType.SHOPPING,
                        "장바구니 추가 기반 추천 (기준: 소니 WF-1000XM5 무선이어폰): 소니 WH-1000XM5 무선 헤드폰, 에어팟 맥스 스페이스그레이, 젠하이저 HD660S2",
                        0.7, 0.8);

                // ===== User 2 (테크마스터) =====
                saveMemory(USER_2_UUID, MemoryType.PROFILE, MemorySubType.GENERAL,
                        "전자기기 덕후. 최신 IT 기기 리뷰를 즐겨봄.",
                        0.9, 0.85);

                saveMemory(USER_2_UUID, MemoryType.PREFERENCE, MemorySubType.SHOPPING,
                        "애플 생태계 선호. 맥북, 아이패드, 아이폰 시리즈에 관심.",
                        0.8, 0.75);

                saveMemory(USER_2_UUID, MemoryType.RECOMMENDATION, MemorySubType.SHOPPING,
                        "장바구니 추가 기반 추천 (기준: 맥북 프로 14인치 M3 Pro): 아이패드 프로 12.9 M2, 맥북 에어 M2, LG 그램 17인치 2024",
                        0.7, 0.8);

                log.info(" [AiInitData] AI 메모리 초기화 완료 (총 {}건)", aiMemoryRepository.count());

                // ===== VectorStore =====
                log.info(" [AiInitData] VectorStore 상품 데이터 초기화 시작");
                List<Document> products = List.of(
                        new Document("스노우피크 랜드록 텐트 - 캠핑용 거실형 텐트, 4-6인용"),
                        new Document("스노우피크 티타늄 싱글머그 450 - 초경량 캠핑용 머그컵"),
                        new Document("힐레베르그 아틀라스 4인용 - 돔형 캠핑 텐트"),
                        new Document("헬리녹스 체어제로 블랙 - 초경량 백패킹 캠핑 의자"),
                        new Document("소니 WH-1000XM5 무선 헤드폰 - 노이즈캔슬링, 블루투스 헤드셋"),
                        new Document("소니 WF-1000XM5 무선이어폰 - 노이즈캔슬링, 인이어 이어폰"),
                        new Document("에어팟 맥스 스페이스그레이 - 애플 오버이어 헤드폰, 노이즈캔슬링"),
                        new Document("젠하이저 HD660S2 - 오픈형 레퍼런스 유선 헤드폰"),
                        new Document("맥북 프로 14인치 M3 Pro - 애플 노트북, 영상 및 개발용"),
                        new Document("맥북 에어 M2 - 애플 경량 노트북, 휴대용"),
                        new Document("아이패드 프로 12.9 M2 - 애플 태블릿, 드로잉 및 영상 감상용"),
                        new Document("LG 그램 17인치 2024 - 초경량 대화면 노트북, 사무용")
                );
                vectorStore.add(products);
                log.info(" [AiInitData] VectorStore 상품 데이터 {}건 저장 완료", products.size());
            }
        };
    }

    private void saveMemory(UUID userUuid, MemoryType memoryType, MemorySubType subType,
                            String content, double importanceScore, double confidenceScore) {
        AiMemory memory = AiMemory.builder()
                .userUuid(userUuid)
                .memoryType(memoryType)
                .subType(subType)
                .content(content)
                .importanceScore(importanceScore)
                .confidenceScore(confidenceScore)
                .build();
        aiMemoryRepository.save(memory);
    }
}
