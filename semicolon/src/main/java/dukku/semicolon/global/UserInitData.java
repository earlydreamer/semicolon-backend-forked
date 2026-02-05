package dukku.semicolon.global;

import dukku.semicolon.boundedContext.user.entity.User;
import dukku.semicolon.boundedContext.user.entity.type.Role;
import dukku.semicolon.boundedContext.user.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Order(0)
public class UserInitData {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initUsers() {
        return args -> initUserData();
    }

    @Transactional
    public void initUserData() {
        if (userRepository.count() > 0) {
            return;
        }

        List<User> users = List.of(
                createUser("admin@semicolon.com", "Admin123!", "admin", Role.ADMIN),
                createUser("user1@semicolon.com", "User123!", "user1", Role.USER),
                createUser("user2@semicolon.com", "User123!", "user2", Role.USER),
                createUser("user3@semicolon.com", "User123!", "user3", Role.USER)
        );

        userRepository.saveAll(users);
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
