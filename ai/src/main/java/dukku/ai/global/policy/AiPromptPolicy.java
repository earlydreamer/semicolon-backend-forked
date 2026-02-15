package dukku.ai.global.policy;

import java.util.Set;

public final class AiPromptPolicy {

    private AiPromptPolicy() {
    }

    public static final String SYSTEM_PROMPT = "당신은 사용자 정보 기반 상품 추천 모델입니다";

    public static final String MEMORY_EXTRACTION_PROMPT = """
            다음 대화에서 사용자에 대해 기억할 만한 정보를 추출하세요.
            각 항목을 JSON 배열로 반환하세요. 기억할 정보가 없으면 빈 배열 []을 반환하세요.

            형식:
            [
              {
                "memoryType": "PROFILE|PREFERENCE",
                "subType": "TECH|SHOPPING|GENERAL",
                "content": "기억할 내용",
                "confidence": 0.0~1.0
              }
            ]

            memoryType 기준:
            - PROFILE: 이름, 나이, 직업 등 기본 정보
            - PREFERENCE: 좋아하는 것, 싫어하는 것, 선호도

            JSON 배열만 반환하고 다른 텍스트는 포함하지 마세요.

            대화 내용:
            사용자: %s
            AI: %s
            """;

    public static final String TOOL_USAGE_PROMPT = """

            필요한 경우 제공된 Tool을 활용하여 작업을 수행하세요.
            각 단계에서 필요한 Tool을 순서대로 호출하고, 결과를 기반으로 다음 단계를 진행하세요.
            Tool 호출 결과를 사용자에게 자연스럽게 설명하세요.
            """;

    public static final Set<String> DOCUMENT_RETRIEVAL_KEYWORDS = Set.of(
            "상품", "추천", "환불", "정책", "배송", "교환", "가격", "할인",
            "쿠폰", "결제", "주문", "반품", "사이즈", "재고", "품절"
    );
}
