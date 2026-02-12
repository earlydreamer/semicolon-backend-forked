package dukku.deposit.global;

import dukku.common.shared.deposit.dto.DepositDto;
import dukku.deposit.boundedContext.deposit.app.DepositFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 시스템 예치금 관리를 위한 admin 계정 생성
 * User-Deposit 1:1 대응이므로 계정 생성 시 Deposit 함께 생성
 * 일단 임시로 초기화 코드를 이용해 밀어넣는 방식으로 구현, 추후 Admin을 타고 관리하는 방식으로 변경 고려
 */

@Slf4j
@Configuration
@RequiredArgsConstructor
@Order(3) // 다른 2개의 초기화 코드가 실행된 후 실행
public class SystemDepositInitData {

    // UserInitData에서 생성한 Admin UUID와 동일해야 함
    public static final UUID SYSTEM_USER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final Long INITIAL_CAPITAL = 1_000_000_000L; // 10억

    @Bean
    public CommandLineRunner initSystemDeposit(DepositFacade depositFacade) {
        return new CommandLineRunner() {
            @Override
            @Transactional
            public void run(String... args) throws Exception {
                log.info("🚀 [InitData] System Deposit Initialization Started");

                // 2. 시스템 예치금 계좌 확인 및 초기 자본금 주입
                // findDeposit 호출 시 Deposit이 없으면 생성됨 (Lazy)
                try {
                    DepositDto deposit = depositFacade.findDeposit(SYSTEM_USER_UUID);
                    if (deposit.getBalance() == 0L) {
                        depositFacade.injectSystemCapital(SYSTEM_USER_UUID, INITIAL_CAPITAL);
                        log.info("✅ [SystemDepositInitData] 시스템 예치금 초기 자본금 납입 완료: {} KRW", INITIAL_CAPITAL);
                    } else {
                        log.info("ℹ️ [SystemDepositInitData] 시스템 예치금 잔액이 이미 존재하여 자본금 납입을 건너뜁니다.");
                    }
                } catch (Exception e) {
                    log.error("❌ [SystemDepositInitData] Failed to initialize system deposit", e);
                }
            }
        };
    }
}
