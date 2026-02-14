package dukku.ai.global.policy;

public final class AiSimilarityPolicy {

    private AiSimilarityPolicy() {
    }

    // 문서 검색
    public static final double DOCUMENT_SIMILARITY_THRESHOLD = 0.7;
    public static final int DOCUMENT_TOP_K = 5;

    // 기억 검색
    public static final double MEMORY_SIMILARITY_THRESHOLD = 0.3;
    public static final int MEMORY_PROFILE_LIMIT = 3;
    public static final int MEMORY_SIMILAR_LIMIT = 5;

    // 기억 중복 판단
    public static final double MEMORY_DUPLICATE_THRESHOLD = 0.92;

    // 임베딩
    public static final int EMBEDDING_DIMENSION = 384;
}
