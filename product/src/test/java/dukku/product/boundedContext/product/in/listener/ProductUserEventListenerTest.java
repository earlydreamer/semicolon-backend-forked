package dukku.product.boundedContext.product.in.listener;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.user.event.UserDepositInitializedEvent;
import dukku.product.boundedContext.product.entity.ProductSeller;
import dukku.product.boundedContext.product.entity.ProductUser;
import dukku.product.boundedContext.product.out.ProductSellerRepository;
import dukku.product.boundedContext.product.out.ProductUserRepository;
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
class ProductUserEventListenerTest {

    @Mock
    private ProductUserRepository productUserRepository;

    @Mock
    private ProductSellerRepository productSellerRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ProductUserEventListener listener;

    @Test
    @DisplayName("ProductUser가 이미 있어도 ProductSeller가 없으면 상점을 생성한다")
    void createsSellerEvenWhenUserAlreadyExists() {
        UUID userUuid = UUID.randomUUID();
        UserDepositInitializedEvent event = new UserDepositInitializedEvent(userUuid, "seller");

        when(productUserRepository.existsById(userUuid)).thenReturn(true);
        when(productSellerRepository.findByUserUuid(userUuid)).thenReturn(Optional.empty());

        listener.handleDepositInitialized(event);

        verify(productUserRepository, never()).save(any(ProductUser.class));
        verify(productSellerRepository).save(any(ProductSeller.class));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("이벤트 닉네임이 비어있으면 ProductUser를 이름없음으로 생성한다")
    void savesDefaultNicknameWhenEventNicknameBlank() {
        UUID userUuid = UUID.randomUUID();
        UserDepositInitializedEvent event = new UserDepositInitializedEvent(userUuid, " ");

        when(productUserRepository.existsById(userUuid)).thenReturn(false);
        when(productSellerRepository.findByUserUuid(userUuid)).thenReturn(Optional.empty());

        listener.handleDepositInitialized(event);

        ArgumentCaptor<ProductUser> captor = ArgumentCaptor.forClass(ProductUser.class);
        verify(productUserRepository).save(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo("이름없음");
    }
}

