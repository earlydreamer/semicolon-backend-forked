package dukku.user.global;
import dukku.common.shared.user.type.Role;
import dukku.user.boundedContext.user.entity.User;
import dukku.user.boundedContext.user.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Order(0)
public class UserInitData {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment env;

    @Bean
    public CommandLineRunner initUsers() {
        return args -> {
            log.info("🚀 [UserInitData] 데이터 초기화 시작");
            createFixedUsers();
        };
    }

    private void createFixedUsers() {
        List<User> users = new ArrayList<>();

        addIfPresent(users, createAdminUser());

        for (int i = 1; i <= 20; i++) {
            addIfPresent(users, createUser("u" + i + "@company.com", "TestUser123!", "u" + i, Role.USER, i));
        }

        if (users.isEmpty()) {
            log.info("고정 테스트 사용자 생성 스킵: 이미 모든 계정이 존재함");
            return;
        }

        userRepository.saveAll(users);
        log.info("고정 테스트 사용자 {}명 생성 완료", users.size());
    }

    private User createAdminUser() {
        return createUser(
                env.getRequiredProperty("INIT_ADMIN_EMAIL"),
                env.getRequiredProperty("INIT_ADMIN_PASSWORD"),
                "admin",
                Role.ADMIN,
                0
        );
    }

    private void addIfPresent(List<User> users, User user) {
        if (user != null) {
            users.add(user);
        }
    }

    private User createUser(String email, String rawPassword, String nickname, Role role, int seed) {
        if (userRepository.findByEmail(email).isPresent()) {
            return null;
        }

        String uuidStr = String.format("00000000-0000-0000-0000-%012d", seed);
        java.util.UUID uuid = java.util.UUID.fromString(uuidStr);

        return User.builder()
                .uuid(uuid)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .nickname(nickname)
                .role(role)
                .status(dukku.common.shared.user.type.UserStatus.ACTIVE)
                .build();
    }
}
