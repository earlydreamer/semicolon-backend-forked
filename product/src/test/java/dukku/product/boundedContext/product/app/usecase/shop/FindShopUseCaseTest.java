package dukku.product.boundedContext.product.app.usecase.shop;

import dukku.common.shared.product.dto.shop.ShopResponse;
import dukku.common.shared.user.dto.UserProfileResponse;
import dukku.common.shared.user.out.UserApiClient;
import dukku.product.boundedContext.product.entity.ProductSeller;
import dukku.product.boundedContext.product.entity.ProductUser;
import dukku.product.boundedContext.product.out.ProductSellerRepository;
import dukku.product.boundedContext.product.out.ProductUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindShopUseCaseTest {

    @Mock
    private ProductSellerRepository productSellerRepository;

    @Mock
    private ProductUserRepository productUserRepository;

    @Mock
    private UserApiClient userApiClient;

    @InjectMocks
    private FindShopUseCase useCase;

    @Test
    @DisplayName("ProductUser가 없으면 User API로 닉네임을 조회해 반환한다")
    void resolvesNicknameFromUserApiWhenProductUserMissing() {
        UUID userUuid = UUID.randomUUID();
        UUID shopUuid = UUID.randomUUID();

        ProductSeller seller = org.mockito.Mockito.mock(ProductSeller.class);
        when(seller.getUserUuid()).thenReturn(userUuid);
        when(seller.getUuid()).thenReturn(shopUuid);
        when(seller.getIntro()).thenReturn("intro");
        when(seller.getSalesCount()).thenReturn(0);
        when(seller.getActiveListingCount()).thenReturn(0);
        when(seller.getAverageRating()).thenReturn(java.math.BigDecimal.ZERO);
        when(seller.getReviewCount()).thenReturn(0);

        when(productSellerRepository.findByUuid(shopUuid)).thenReturn(Optional.of(seller));
        when(productUserRepository.findById(userUuid)).thenReturn(Optional.empty());
        when(productUserRepository.existsById(userUuid)).thenReturn(true);
        when(userApiClient.getUserProfile(userUuid))
                .thenReturn(UserProfileResponse.builder().userUuid(userUuid).nickname("seller-name").build());

        ShopResponse response = useCase.execute(shopUuid);

        assertThat(response.getNickname()).isEqualTo("seller-name");
        verify(userApiClient).getUserProfile(userUuid);
    }
}

