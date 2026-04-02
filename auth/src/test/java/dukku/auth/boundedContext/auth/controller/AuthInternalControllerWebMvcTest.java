package dukku.auth.boundedContext.auth.controller;

import dukku.auth.boundedContext.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthInternalControllerWebMvcTest {

    private static final UUID USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthInternalController authInternalController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(authInternalController).build();
    }

    @Test
    @DisplayName("내부 auth 세션 폐기 엔드포인트는 204를 반환하고 서비스에 위임한다")
    void revokesAllSessions() throws Exception {
        mockMvc.perform(delete("/api/v1/internal/auth/users/{userUuid}/sessions", USER_UUID))
                .andExpect(status().isNoContent());

        verify(authService).revokeAllSessions(USER_UUID);
    }
}
