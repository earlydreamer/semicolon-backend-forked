package dukku.user.boundedContext.user.app.user;

import dukku.user.boundedContext.user.entity.User;
import dukku.user.boundedContext.user.out.UserRepository;
import dukku.common.shared.user.type.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindAdminUserListUseCase {

    private final UserRepository userRepository;

    public Page<User> execute(String keyword, UserStatus status, Pageable pageable) {
        return userRepository.searchUsers(keyword, status, pageable);
    }
}
