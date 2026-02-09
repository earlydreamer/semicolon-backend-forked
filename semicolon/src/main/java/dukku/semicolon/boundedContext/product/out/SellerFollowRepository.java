package dukku.semicolon.boundedContext.product.out;

import dukku.semicolon.boundedContext.product.entity.SellerFollow;
import dukku.semicolon.shared.product.dto.follow.FollowedSellerCardResponse;
import dukku.semicolon.shared.product.dto.follow.FollowerUserCardResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SellerFollowRepository extends JpaRepository<SellerFollow, Integer> {

    boolean existsByUserUuidAndSellerUuid(UUID userUuid, UUID sellerUuid);

    void deleteByUserUuidAndSellerUuid(UUID userUuid, UUID sellerUuid);

    List<SellerFollow> findByUserUuid(UUID userUuid);

    List<SellerFollow> findBySellerUuid(UUID sellerUuid);

    long countBySellerUuid(UUID sellerUuid);

    // 내가 팔로우한 상점 목록 카드
    @Query("""
        select new dukku.semicolon.shared.product.dto.follow.FollowedSellerCardResponse(
            sf.sellerUuid,
            pu.nickname,
            ps.intro,
            (select count(sf2) from SellerFollow sf2 where sf2.sellerUuid = sf.sellerUuid),
            true
        )
        from SellerFollow sf
        join ProductUser pu on pu.userUuid = sf.sellerUuid
        left join ProductSeller ps on ps.userUuid = sf.sellerUuid
        where sf.userUuid = :userUuid
        order by sf.createdAt desc
    """)
    List<FollowedSellerCardResponse> findFollowedSellerCards(@Param("userUuid") UUID userUuid);

    // 특정 상점의 팔로워 목록 카드
    @Query("""
        select new dukku.semicolon.shared.product.dto.follow.FollowerUserCardResponse(
            sf.userUuid,
            pu.nickname
        )
        from SellerFollow sf
        join ProductUser pu on pu.userUuid = sf.userUuid
        where sf.sellerUuid = :sellerUuid
        order by sf.createdAt desc
    """)
    List<FollowerUserCardResponse> findFollowerUserCards(@Param("sellerUuid") UUID sellerUuid);
}
