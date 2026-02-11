package dukku.semicolon.global;

import dukku.semicolon.boundedContext.user.entity.User;
import dukku.semicolon.boundedContext.user.entity.type.Role;
import dukku.semicolon.boundedContext.user.out.UserRepository;
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
                if (profile.equals("dev")) {
                    isDev = true;
                    break;
                }
            }

            int userCount = isDev ? 1000 : 10; // dev=1000, release=10

            if (userRepository.count() >= userCount) {
                log.info("유저 데이터가 이미 존재하여 초기화를 건너뜁니다.");
                return;
            }

            List<User> users = new ArrayList<>();

            // 관리자
            users.add(createUser("admin@semicolon.com", "Admin123!", "admin", Role.ADMIN));

            // 일반 유저
            for (int i = 1; i <= userCount; i++) {
                users.add(createUser("user" + i + "@semicolon.com", "User123!", "user" + i, Role.USER));
            }

            userRepository.saveAll(users);
            log.info("✅ {}명의 유저 생성 완료", userCount + 1);
        };
    }

    private User createUser(String email, String rawPassword, String nickname, Role role) {
        return User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .nickname(nickname)
                .role(role)
                .build();
    }
}