package dukku.coupon.boundedContext.coupon.out;

import dukku.common.shared.coupon.type.CouponStatus;
import dukku.coupon.boundedContext.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, Integer> {
    Optional<Coupon> findByUuid(UUID couponUuid);

    @Query("""
            select c
            from Coupon c
            where c.status = 'ACTIVE'
              and c.validFrom <= :now
              and c.issuedQuantity < c.totalQuantity
              and not exists (
                  select 1 from CouponUser cu
                  where cu.userUuid = :userUuid
                    and cu.coupon = c
              )
            """)
    List<Coupon> findIssuableCoupons(UUID userUuid, LocalDateTime now);

    List<Coupon> findByStatus(CouponStatus status);

    @Modifying
    @Query("UPDATE Coupon c SET c.issuedQuantity = c.issuedQuantity + 1 " +
            "WHERE c.uuid = :uuid AND c.issuedQuantity < c.totalQuantity")
    int decreaseQuantity(@Param("uuid") UUID uuid);
}
