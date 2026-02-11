package dukku.user.global;

import dukku.user.boundedContext.user.entity.User;
import dukku.common.shared.user.type.Role;
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
            String[] activeProfiles = env.getActiveProfiles();
            boolean isDev = false;
            for (String profile : activeProfiles) {
                if (profile.equals("dev") || profile.equals("local")) {
                    isDev = true;
                    break;
                }
            }

            // [변경] 개발 환경에서 고정된 테스트 데이터 생성 (u1 ~ u20)
            if (isDev) {
                createFixedUsers();
            }
        };
    }

    private void createFixedUsers() {
        List<User> users = new ArrayList<>();

        // 1. 관리자 (UUID ...0000)
        users.add(createUser("admin@semicolon.com", "Admin123!", "admin", Role.ADMIN, 0));

        // 2. 일반 유저 (u1 ~ u20 -> UUID ...0001 ~ ...0020)
        for (int i = 1; i <= 20; i++) {
            users.add(createUser("u" + i + "@company.com", "TestUser123!", "u" + i, Role.USER, i));
        }

        userRepository.saveAll(users);
        log.info("✅ 고정 테스트 유저 (admin, u1~u20) 생성 완료");
    }

    private User createUser(String email, String rawPassword, String nickname, Role role, int seed) {
        // 이미 존재하면 생성 안 함
        if (userRepository.findByEmail(email).isPresent()) {
            return null;
        }

        // 고정 UUID 생성 (00000000-0000-0000-0000-0000000000xx)
        String uuidStr = String.format("00000000-0000-0000-0000-%012d", seed);
        java.util.UUID uuid = java.util.UUID.fromString(uuidStr);

        return User.builder()
                .uuid(uuid) // SourceUser 필드에 직접 주입
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .nickname(nickname)
                .role(role)
                .status(dukku.common.shared.user.type.UserStatus.ACTIVE)
                .build();
    }
}