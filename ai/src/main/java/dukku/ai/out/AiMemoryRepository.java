package dukku.ai.out;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dukku.ai.entity.AiMemory;

public interface AiMemoryRepository extends JpaRepository<AiMemory, Integer> {

    @Query(value = """
            SELECT * FROM ai_memory
            WHERE user_id = :userId
              AND memory_type = :memoryType
            ORDER BY importance_score DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<AiMemory> findTopByUserIdAndMemoryType(
            @Param("userId") UUID userId,
            @Param("memoryType") String memoryType,
            @Param("limit") int limit);

    @Query(value = """
            SELECT * FROM ai_memory
            WHERE user_id = :userId
              AND memory_type != 'PROFILE'
              AND 1 - (embedding <=> cast(:embedding AS vector)) > :threshold
            ORDER BY 1 - (embedding <=> cast(:embedding AS vector)) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<AiMemory> findSimilarMemories(
            @Param("userId") UUID userId,
            @Param("embedding") String embedding,
            @Param("threshold") double threshold,
            @Param("limit") int limit);

    @Query(value = """
            SELECT * FROM ai_memory
            WHERE user_id = :userId
              AND 1 - (embedding <=> cast(:embedding AS vector)) > :threshold
            ORDER BY 1 - (embedding <=> cast(:embedding AS vector)) DESC
            LIMIT 1
            """, nativeQuery = true)
    List<AiMemory> findDuplicateMemory(
            @Param("userId") UUID userId,
            @Param("embedding") String embedding,
            @Param("threshold") double threshold);
}
