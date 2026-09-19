package dukku.ai.app.service;

import java.util.List;

import com.google.genai.Client;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import dukku.ai.global.policy.AiSimilarityPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Gemini embedding 호출 경계. 요청 taskType을 명시하고 DB 저장 전에 벡터를 검증·정규화한다.
 */
@Service
public class GeminiEmbeddingService {

    private static final String RETRIEVAL_QUERY = "RETRIEVAL_QUERY";
    private static final String RETRIEVAL_DOCUMENT = "RETRIEVAL_DOCUMENT";
    private static final String RETRIEVAL_DOCUMENT_PROFILE = "retrieval-document";
    private static final String NORMALIZATION_VERSION = "normalization-v1";

    private final Client client;
    private final String model;
    private final int dimensions;
    private final String profile;

    public GeminiEmbeddingService(
            Client client,
            @Value("${ai.embedding.model:gemini-embedding-001}") String model,
            @Value("${ai.embedding.dimensions:1536}") int dimensions) {
        Assert.hasText(model, "Embedding model must not be blank");
        Assert.isTrue(dimensions == AiSimilarityPolicy.EMBEDDING_DIMENSION,
                "Embedding dimensions must match the database vector dimension of "
                        + AiSimilarityPolicy.EMBEDDING_DIMENSION);
        this.client = client;
        this.model = model;
        this.dimensions = dimensions;
        this.profile = profileFor(model, dimensions);
    }

    public float[] embedQuery(String text) {
        return embed(text, RETRIEVAL_QUERY);
    }

    public float[] embedDocument(String text) {
        return embed(text, RETRIEVAL_DOCUMENT);
    }

    public String profile() {
        return profile;
    }

    public String model() {
        return model;
    }

    public static String profileFor(String model, int dimensions) {
        Assert.hasText(model, "Embedding model must not be blank");
        Assert.isTrue(dimensions == AiSimilarityPolicy.EMBEDDING_DIMENSION,
                "Embedding dimensions must match the database vector dimension of "
                        + AiSimilarityPolicy.EMBEDDING_DIMENSION);
        String profile = "%s:%d:%s:%s".formatted(
                model, dimensions, RETRIEVAL_DOCUMENT_PROFILE, NORMALIZATION_VERSION);
        Assert.isTrue(profile.length() <= 160,
                "Embedding profile must fit the database embedding_profile column (160 characters)");
        return profile;
    }

    private float[] embed(String text, String taskType) {
        Assert.hasText(text, "Embedding input must not be blank");

        EmbedContentConfig config = EmbedContentConfig.builder()
                .taskType(taskType)
                .outputDimensionality(dimensions)
                .build();
        EmbedContentResponse response = client.models.embedContent(model, text, config);
        List<ContentEmbedding> embeddings = response.embeddings()
                .orElseThrow(() -> new IllegalStateException("Gemini returned no embedding"));
        if (embeddings.size() != 1) {
            throw new IllegalStateException("Gemini returned an unexpected embedding count: " + embeddings.size());
        }

        List<Float> values = embeddings.getFirst().values()
                .orElseThrow(() -> new IllegalStateException("Gemini returned an empty embedding"));
        return normalize(values, dimensions);
    }

    static float[] normalize(List<Float> values, int dimensions) {
        if (values == null || values.size() != dimensions) {
            int actualDimension = values == null ? 0 : values.size();
            throw new IllegalStateException("Gemini embedding dimension %d does not match configured dimension %d"
                    .formatted(actualDimension, dimensions));
        }

        double squaredNorm = 0.0;
        for (Float value : values) {
            if (value == null || !Float.isFinite(value)) {
                throw new IllegalStateException("Gemini returned a non-finite embedding value");
            }
            squaredNorm += (double) value * value;
        }
        double norm = Math.sqrt(squaredNorm);
        if (!Double.isFinite(norm) || norm <= 0.0) {
            throw new IllegalStateException("Gemini returned a zero or invalid embedding norm");
        }

        float[] normalized = new float[dimensions];
        for (int i = 0; i < dimensions; i++) {
            normalized[i] = (float) (values.get(i) / norm);
            if (!Float.isFinite(normalized[i])) {
                throw new IllegalStateException("Normalized embedding contains a non-finite value");
            }
        }
        return normalized;
    }
}
