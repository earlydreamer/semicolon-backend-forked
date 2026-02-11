package dukku.product.boundedContext.product.out;

import dukku.product.boundedContext.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Optional<Category> findByCategoryName(String name);

    // [핵심] 특정 카테고리의 모든 부모 ID + 내 ID를 리스트로 반환 (Leaf -> Root)
    @Query(value = """
        WITH RECURSIVE category_path AS (
            SELECT id, parent_id
            FROM categories
            WHERE id = :categoryId
            
            UNION ALL
            
            SELECT c.id, c.parent_id
            FROM categories c
            INNER JOIN category_path cp ON c.id = cp.parent_id
        )
        SELECT id FROM category_path
    """, nativeQuery = true)
    List<Integer> findCategoryPathIds(@Param("categoryId") int categoryId);
}
