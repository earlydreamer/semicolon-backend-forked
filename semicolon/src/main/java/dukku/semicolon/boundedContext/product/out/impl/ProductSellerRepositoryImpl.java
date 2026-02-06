package dukku.semicolon.boundedContext.product.out.impl;

import dukku.semicolon.boundedContext.product.out.CustomProductSellerRepository;
import dukku.semicolon.shared.product.dto.cqrs.SellerReviewStatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Arrays;

@Repository
@RequiredArgsConstructor
public class ProductSellerRepositoryImpl implements CustomProductSellerRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int[] bulkUpdateReviewSummary(List<SellerReviewStatDto> items) {
        if (items == null || items.isEmpty()) return new int[0];

        // Postgres 기준: updated_at을 NOW()로 직접 갱신
        // + 불필요 UPDATE 방지(재시도/clean 실패 대비): IS DISTINCT FROM 사용
        final String sql = """
            UPDATE product_sellers
               SET review_count = ?,
                   average_rating = ?,
                   updated_at = NOW()
             WHERE seller_uuid = ?
               AND (
                   review_count IS DISTINCT FROM ?
                OR average_rating IS DISTINCT FROM ?
               )
        """;

        int[][] results = jdbcTemplate.batchUpdate(sql, items, items.size(),
                (PreparedStatement ps, SellerReviewStatDto dto) -> {
                    ps.setInt(1, dto.reviewCount());
                    ps.setBigDecimal(2, dto.averageRating());
                    ps.setObject(3, dto.sellerUuid());

                    // WHERE 비교용(조건부 update)
                    ps.setInt(4, dto.reviewCount());
                    ps.setBigDecimal(5, dto.averageRating());
                });

        // Spring 6+의 Collection 기반 batchUpdate는 int[][]를 반환(배치 chunk 단위)
        return Arrays.stream(results)
                .flatMapToInt(Arrays::stream)
                .toArray();
    }
}
