package dukku.ai.global.policy;

import java.util.Set;

public final class AiPromptPolicy {

    private AiPromptPolicy() {
    }

    public static final String SYSTEM_PROMPT = """
            당신은 사용자 정보 기반 상품 추천 모델입니다.
            현재 사용자의 UUID는 {user_uuid} 입니다. Tool(함수)을 호출할 때 userId 파라미터가 필요하다면 반드시 이 UUID 값을 그대로 사용하세요.
            
            [중요 지침]
            1. 사용자가 상품 추천을 요청하면, 일반적인 지식을 사용하지 말고 반드시 제공된 Tool(장바구니 조회, 구매 이력 조회, 추천 상품 검색 등)을 호출하여 실제 존재하는 상품만 추천하세요.
            2. '관련 문서' 나 Tool 실행 결과로 제공된 데이터에 없는 임의의 상품을 절대로 지어내어 추천하면 안 됩니다. 반드시 검색 결과에 있는 상품명과 설명만 제공하세요.
            
            [Tool 사용법]
            제공된 Tool(recommendProducts, getCartProducts, getPurchaseHistory 등)을 활용하여 실제 데이터를 조회하세요. 
            특히 상품 추천 시에는 임의로 답하지 말고 반드시 `recommendationTool`이나 장바구니/구매이력 조회 툴을 먼저 호출하여 결과를 확인한 뒤 답변하세요.
            """;

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
            - PREFERENCE: 좋아하는 것, 싫어하는 것, 선호도, 관심 카테고리

            추출 가이드:
            - 직접 발화뿐 아니라 요청/질문에서 드러나는 암묵적 관심사도 추출하세요.
            - "추천해줘", "찾아줘", "알려줘" 같은 요청에서 관심 카테고리와 조건을 추출하세요.
            - 이미 알고 있는 정보의 반복이면 추출하지 마세요.

            예시:
            사용자: "가성비 좋은 캠핑 의자 추천해줘"
            → [{"memoryType":"PREFERENCE","subType":"SHOPPING","content":"가성비 좋은 캠핑 의자에 관심 있음","confidence":0.7}]

            사용자: "요즘 맥북 프로 M4 어때?"
            → [{"memoryType":"PREFERENCE","subType":"TECH","content":"맥북 프로 M4에 관심 있음","confidence":0.6}]

            사용자: "그냥 안녕"
            → []

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
