package dukku.ai.out;

import java.util.List;
import java.util.UUID;

import dukku.ai.entity.AiUserMemory;
import dukku.common.shared.ai.type.MemoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiUserMemoryRepository extends JpaRepository<AiUserMemory, Integer> {
    boolean existsByUserUuidAndMemoryType(UUID userUuid, MemoryType memoryType);

    @Query(value = """
            SELECT * FROM ai_memory
            WHERE user_uuid = :userUuid
              AND memory_type = :memoryType
            ORDER BY importance_score DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<AiUserMemory> findTopByUserIdAndMemoryType(
            @Param("userUuid") UUID userUuid,
            @Param("memoryType") String memoryType,
            @Param("limit") int limit);

    @Query(value = """
            SELECT * FROM ai_memory
            WHERE user_uuid = :userUuid
              AND memory_type != 'PROFILE'
              AND 1 - (embedding <=> cast(:embedding AS vector)) > :threshold
            ORDER BY 1 - (embedding <=> cast(:embedding AS vector)) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<AiUserMemory> findSimilarMemories(
            @Param("userUuid") UUID userUuid,
            @Param("embedding") String embedding,
            @Param("threshold") double threshold,
            @Param("limit") int limit);

    @Query(value = """
            SELECT * FROM ai_memory
            WHERE user_uuid = :userUuid
              AND 1 - (embedding <=> cast(:embedding AS vector)) > :threshold
            ORDER BY 1 - (embedding <=> cast(:embedding AS vector)) DESC
            LIMIT 1
            """, nativeQuery = true)
    List<AiUserMemory> findDuplicateMemory(
            @Param("userUuid") UUID userUuid,
            @Param("embedding") String embedding,
            @Param("threshold") double threshold);
}
