package dukku.semicolon.boundedContext.user.out;

import dukku.semicolon.boundedContext.user.entity.User;
import dukku.common.shared.user.type.Role;
import dukku.common.shared.user.type.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByUuid(UUID userUuid);

    Optional<User> findByUuidAndDeletedAtIsNull(UUID userUuid);

    Optional<User> findByRoleAndDeletedAtIsNull(Role role);

    boolean existsByEmailAndDeletedAtIsNullAndIdNot(String email, Integer id);

    List<User> findByStatusInAndDeletedAtBefore(
            List<UserStatus> statuses,
            LocalDateTime deletedAt
    );
}
