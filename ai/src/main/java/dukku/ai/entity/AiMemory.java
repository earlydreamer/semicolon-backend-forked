package dukku.ai.entity;

import java.util.UUID;

import dukku.common.global.jpa.entity.BaseIdAndUUIDAndTime;
import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "ai_memory")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class AiMemory extends BaseIdAndUUIDAndTime {

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(nullable = false)
    private UUID userUuid;

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

    @Builder.Default
    @Column(nullable = false)
    private Integer accessCount = 0;

    private String sourceMessageId;

    public void incrementAccessCount() {
        this.accessCount++;
    }

    public void updateConfidence(Double newConfidence) {
        this.confidenceScore = (this.confidenceScore + newConfidence) / 2.0;
    }

    public void updateImportanceScore(Double score) {
        this.importanceScore = score;
    }
}
