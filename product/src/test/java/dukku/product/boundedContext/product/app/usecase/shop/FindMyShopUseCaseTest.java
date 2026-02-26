package dukku.product.boundedContext.product.app.usecase.shop;

import dukku.common.shared.product.dto.shop.ShopResponse;
import dukku.common.shared.user.dto.UserProfileResponse;
import dukku.common.shared.user.out.UserApiClient;
import dukku.product.boundedContext.product.entity.ProductSeller;
import dukku.product.boundedContext.product.entity.ProductUser;
import dukku.product.boundedContext.product.out.ProductSellerRepository;
import dukku.product.boundedContext.product.out.ProductUserRepository;
import dukku.product.boundedContext.product.out.SellerReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindMyShopUseCaseTest {

    @Mock
    private ProductSellerRepository productSellerRepository;

    @Mock
    private ProductUserRepository productUserRepository;

    @Mock
    private UserApiClient userApiClient;

    @Mock
    private SellerReviewRepository sellerReviewRepository;

    @InjectMocks
    private FindMyShopUseCase useCase;

    @Test
    @DisplayName("ProductUser가 있으면 저장된 닉네임을 사용하고 외부 조회를 하지 않는다")
    void usesExistingNicknameWithoutApiCall() {
        UUID userUuid = UUID.randomUUID();
        ProductSeller seller = ProductSeller.create(userUuid, "intro");
        ProductUser productUser = ProductUser.create(userUuid, "stored-name");

        when(productSellerRepository.findByUserUuid(userUuid)).thenReturn(Optional.of(seller));
        when(productUserRepository.findById(userUuid)).thenReturn(Optional.of(productUser));
        when(sellerReviewRepository.countBySellerUuidAndDeletedAtIsNull(seller.getSellerUuid())).thenReturn(0L);
        when(sellerReviewRepository.avgRating(seller.getSellerUuid())).thenReturn(0.0);

        ShopResponse response = useCase.execute(userUuid);

        assertThat(response.getNickname()).isEqualTo("stored-name");
        verify(userApiClient, never()).getUserProfile(any());
    }

    @Test
    @DisplayName("ProductUser가 없으면 User API 닉네임으로 백필 저장 후 반환한다")
    void backfillsNicknameFromUserApiWhenMissingProductUser() {
        UUID userUuid = UUID.randomUUID();
        ProductSeller seller = ProductSeller.create(userUuid, "intro");

        when(productSellerRepository.findByUserUuid(userUuid)).thenReturn(Optional.of(seller));
        when(productUserRepository.findById(userUuid)).thenReturn(Optional.empty());
        when(userApiClient.getUserProfile(userUuid))
                .thenReturn(UserProfileResponse.builder().userUuid(userUuid).nickname("api-name").build());
        when(productUserRepository.existsById(userUuid)).thenReturn(false);
        when(sellerReviewRepository.countBySellerUuidAndDeletedAtIsNull(seller.getSellerUuid())).thenReturn(0L);
        when(sellerReviewRepository.avgRating(seller.getSellerUuid())).thenReturn(0.0);

        ShopResponse response = useCase.execute(userUuid);

        assertThat(response.getNickname()).isEqualTo("api-name");
        ArgumentCaptor<ProductUser> captor = ArgumentCaptor.forClass(ProductUser.class);
        verify(productUserRepository).save(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo("api-name");
    }
}

