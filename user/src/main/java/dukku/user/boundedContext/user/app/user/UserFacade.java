package dukku.user.boundedContext.user.app.user;

import dukku.common.shared.user.dto.PasswordUpdateRequest;
import dukku.common.shared.user.dto.UserRegisterRequest;
import dukku.common.shared.user.dto.UserResponse;
import dukku.common.shared.user.dto.UserUpdateRequest;
import dukku.user.boundedContext.user.entity.User;
import dukku.common.shared.user.type.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserFacade {

    private final RegisterUserUseCase registerUser;
    private final FindUserUseCase findUser;
    private final UpdateUserUseCase updateUser;
    private final ChangePasswordUseCase changePassword;
    private final WithdrawUserUseCase withdrawUserUseCase;
    private final RestoreWithdrawnUserUseCase restoreWithdrawnUserUseCase;

    public UserResponse registerUser(UserRegisterRequest req, Role role) {
        return User.toUserResponse(registerUser.execute(req, role));
    }

    @Transactional(readOnly = true)
    public UserResponse findUserByUserUuid(UUID userUuid) {
        return User.toUserResponse(findUser.execute(userUuid));
    }

    public UserResponse updateUserByUserUuid(UserUpdateRequest req, UUID userUuid) {
        return User.toUserResponse(updateUser.execute(req, userUuid));
    }

    public void updateUserPassword(PasswordUpdateRequest req) {
        changePassword.execute(req);
    }

    public void withdraw(UUID userUuid) {
        withdrawUserUseCase.withdraw(userUuid);
    }

    public void restoreWithdrawnUser(UUID userUuid, String newPassword) {
        restoreWithdrawnUserUseCase.restore(userUuid, newPassword);
    }
}
