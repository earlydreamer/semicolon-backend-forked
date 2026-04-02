package dukku.user.global;

import dukku.common.shared.user.type.Role;
import dukku.user.boundedContext.user.entity.User;
import dukku.user.boundedContext.user.out.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserInitDataTest {

    private static final UUID ADMIN_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Environment environment;

    @InjectMocks
    private UserInitData userInitData;

    @Test
    @DisplayName("고정 UUID 사용자가 이미 있으면 admin 이메일이 달라도 중복 생성을 건너뛴다")
    void skipsAdminCreationWhenFixedUuidAlreadyExists() throws Exception {
        when(environment.getRequiredProperty("INIT_ADMIN_EMAIL")).thenReturn("new-admin@company.com");
        when(environment.getRequiredProperty("INIT_ADMIN_PASSWORD")).thenReturn("AdminPass1!");
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUuid(ADMIN_UUID)).thenReturn(Optional.of(existingAdmin()));

        for (int seed = 1; seed <= 20; seed++) {
            UUID uuid = UUID.fromString(String.format("00000000-0000-0000-0000-%012d", seed));
            when(userRepository.findByUuid(uuid)).thenReturn(Optional.empty());
        }

        CommandLineRunner runner = userInitData.initUsers();

        runner.run();

        ArgumentCaptor<List<User>> usersCaptor = ArgumentCaptor.forClass(List.class);
        verify(userRepository).saveAll(usersCaptor.capture());

        assertThat(usersCaptor.getValue())
                .extracting(User::getUuid)
                .doesNotContain(ADMIN_UUID);
    }

    private User existingAdmin() {
        return User.builder()
                .uuid(ADMIN_UUID)
                .email("legacy-admin@company.com")
                .nickname("legacy-admin")
                .password("encoded-password")
                .role(Role.ADMIN)
                .status(dukku.common.shared.user.type.UserStatus.ACTIVE)
                .build();
    }
}
