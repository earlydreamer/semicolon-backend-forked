package dukku.semicolon.global;

import dukku.semicolon.boundedContext.deposit.app.DepositFacade;
import dukku.semicolon.boundedContext.user.app.user.UserFacade;
import dukku.semicolon.boundedContext.user.app.user.UserSupport;
import dukku.semicolon.boundedContext.user.entity.type.Role;
import dukku.semicolon.shared.user.dto.UserRegisterRequest;
import dukku.semicolon.shared.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;

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

    private static final String SYSTEM_DEPOSIT_EMAIL = "admin-deposit@dukku.shop";
    private static final String SYSTEM_DEPOSIT_NICKNAME = "시스템-예치금";

    @Bean
    public CommandLineRunner initSystemDeposit(
            UserFacade userFacade,
            UserSupport userSupport,
            DepositFacade depositFacade,
            Environment env) {
        return new CommandLineRunner() {
            @Override
            @Transactional
            public void run(String... args) throws Exception {
                // 이미 시스템 계정이 존재하면 스킵
                if (userSupport.findByEmail(SYSTEM_DEPOSIT_EMAIL).isPresent()) {
                    log.info("[SystemDepositInitData] 시스템 예치금 계정이 이미 존재합니다.");
                    return;
                }

                // 비밀번호 주입
                // 시스템 지갑이라 이 계정으로 직접 로그인할 일은 사실상 없긴 함
                // DTO에 @Builder annotation 안 붙어있어서 생성자 주입으로 생성
                String password = env.getProperty("system.admin-deposit.password");
                UserRegisterRequest request = new UserRegisterRequest(
                        SYSTEM_DEPOSIT_EMAIL,
                        password,
                        SYSTEM_DEPOSIT_NICKNAME);

                // 시스템 계정 생성
                UserResponse user = userFacade.registerUser(request, Role.SYSTEM);
                log.info("[SystemDepositInitData] 시스템 예치금 계정 생성 완료: {}", SYSTEM_DEPOSIT_EMAIL);

                // 시스템 예치금 계좌 명시적 생성
                // findDeposit 호출 시 Deposit이 없으면 생성 (Lazy 생성 유도)
                depositFacade.findDeposit(user.getUserUuid());
                log.info("[SystemDepositInitData] 시스템 예치금 계좌 생성 완료: userUuid={}", user.getUserUuid());
            }
        };
    }
}
