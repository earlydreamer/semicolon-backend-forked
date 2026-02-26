package dukku.deposit.boundedContext.deposit.app;

import dukku.deposit.boundedContext.deposit.entity.Deposit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class FindDepositUseCaseUnitTest {

    @Mock
    private DepositSupport depositSupport;

    @InjectMocks
    private FindDepositUseCase findDepositUseCase;

    @Test
    @DisplayName("잠금 조회에서 예치금이 존재하면 해당 엔티티를 그대로 반환한다")
    void findOrCreateForUpdateReturnsExistingDeposit() {
        // given: 잠금 조회 결과가 존재하는 예치금인 상태다.
        UUID userUuid = UUID.randomUUID();
        Deposit existing = Deposit.create(userUuid);
        when(depositSupport.findByUserUuidForUpdate(userUuid)).thenReturn(Optional.of(existing));

        // when: 잠금 기반 조회/생성을 수행한다.
        Deposit result = findDepositUseCase.findOrCreateForUpdate(userUuid);

        // then: 신규 저장 없이 기존 엔티티를 반환한다.
        assertThat(result).isSameAs(existing);
        verify(depositSupport).findByUserUuidForUpdate(userUuid);
    }

    @Test
    @DisplayName("잠금 조회에서 예치금이 없으면 신규 생성 후 반환한다")
    void findOrCreateForUpdateCreatesNewDepositWhenAbsent() {
        // given: 잠금 조회 결과가 비어 있고, 저장은 성공하는 상태다.
        UUID userUuid = UUID.randomUUID();
        when(depositSupport.findByUserUuidForUpdate(userUuid)).thenReturn(Optional.empty());
        when(depositSupport.save(org.mockito.ArgumentMatchers.any(Deposit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when: 잠금 기반 조회/생성을 수행한다.
        Deposit result = findDepositUseCase.findOrCreateForUpdate(userUuid);

        // then: 신규 예치금이 생성되어 반환된다.
        assertThat(result.getUserUuid()).isEqualTo(userUuid);
        verify(depositSupport).save(org.mockito.ArgumentMatchers.any(Deposit.class));
    }

    @Test
    @DisplayName("동시 생성 경합으로 저장이 실패하면 잠금 재조회 결과를 반환한다")
    void findOrCreateForUpdateRefetchesAfterCreateConflict() {
        // given: 첫 잠금 조회는 비어 있고, 저장 시 무결성 예외가 나지만 재조회는 성공한다.
        UUID userUuid = UUID.randomUUID();
        Deposit refetched = Deposit.create(userUuid);
        when(depositSupport.findByUserUuidForUpdate(userUuid))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(refetched));
        when(depositSupport.save(org.mockito.ArgumentMatchers.any(Deposit.class)))
                .thenThrow(new DataIntegrityViolationException("동시 생성 경합"));

        // when: 잠금 기반 조회/생성을 수행한다.
        Deposit result = findDepositUseCase.findOrCreateForUpdate(userUuid);

        // then: 재조회된 예치금을 반환한다.
        assertThat(result).isSameAs(refetched);
        verify(depositSupport, times(2)).findByUserUuidForUpdate(userUuid);
        verify(depositSupport).save(org.mockito.ArgumentMatchers.any(Deposit.class));
    }
}
