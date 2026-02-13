package dukku.ai.entity;

import java.time.LocalDateTime;

import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_memory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemoryType memoryType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemorySubType subType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "vector(384)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 384)
    private float[] embedding;

    @Column(nullable = false)
    private Double importanceScore;

    @Column(nullable = false)
    private Double confidenceScore;

    @Column(nullable = false)
    private Integer accessCount;

    private String sourceMessageId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public AiMemory(Long userId, MemoryType memoryType, MemorySubType subType,
                    String content, float[] embedding, Double importanceScore,
                    Double confidenceScore, String sourceMessageId) {
        this.userId = userId;
        this.memoryType = memoryType;
        this.subType = subType;
        this.content = content;
        this.embedding = embedding;
        this.importanceScore = importanceScore;
        this.confidenceScore = confidenceScore;
        this.accessCount = 0;
        this.sourceMessageId = sourceMessageId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementAccessCount() {
        this.accessCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateConfidence(Double newConfidence) {
        this.confidenceScore = (this.confidenceScore + newConfidence) / 2.0;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateImportanceScore(Double score) {
        this.importanceScore = score;
        this.updatedAt = LocalDateTime.now();
    }
}
