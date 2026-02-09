package dukku.semicolon.boundedContext.coupon.out;

import dukku.semicolon.boundedContext.coupon.entity.CouponUser;
import dukku.semicolon.boundedContext.coupon.entity.type.CouponUserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CouponUserRepository extends JpaRepository<CouponUser, Integer> {
    boolean existsByUserUuidAndCoupon_Uuid(UUID userUuid, UUID couponUuid);

    Optional<CouponUser> findByUserUuidAndCoupon_Uuid(UUID userUuid, UUID couponUuid);

    List<CouponUser> findByUserUuidAndStatus(UUID userUuid, CouponUserStatus status);
}
