package dukku.coupon.boundedContext.coupon.out;

import dukku.common.shared.coupon.type.CouponUserStatus;
import dukku.coupon.boundedContext.coupon.entity.CouponUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CouponUserRepository extends JpaRepository<CouponUser, Integer> {
    boolean existsByUserUuidAndCoupon_Uuid(UUID userUuid, UUID couponUuid);

    Optional<CouponUser> findByUserUuidAndCoupon_Uuid(UUID userUuid, UUID couponUuid);

    List<CouponUser> findByUserUuidAndStatus(UUID userUuid, CouponUserStatus status);
}
