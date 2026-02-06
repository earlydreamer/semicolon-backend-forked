package dukku.semicolon.boundedContext.product.app.usecase.follow;

import dukku.semicolon.boundedContext.product.out.SellerFollowRepository;
import dukku.semicolon.shared.product.exception.SelfFollowNotAllowedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UnfollowSellerUseCase {

    private final SellerFollowRepository sellerFollowRepository;

    @Transactional
    public void execute(UUID userUuid, UUID sellerUuid) {

        if (userUuid.equals(sellerUuid)) {
            throw new SelfFollowNotAllowedException();
        }

        // 멱등: 없으면 그냥 종료
        if (!sellerFollowRepository.existsByUserUuidAndSellerUuid(userUuid, sellerUuid)) {
            return;
        }

        sellerFollowRepository.deleteByUserUuidAndSellerUuid(userUuid, sellerUuid);
    }
}
