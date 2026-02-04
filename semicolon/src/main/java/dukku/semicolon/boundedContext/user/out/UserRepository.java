package dukku.semicolon.boundedContext.user.out;

import dukku.semicolon.boundedContext.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByUuid(UUID userUuid);

    Optional<User> findByUuidAndDeletedAtIsNull(UUID userUuid);

    boolean existsByEmailAndDeletedAtIsNullAndIdNot(String email, Integer id);

    java.util.List<User> findByStatusInAndDeletedAtBefore(
            java.util.List<dukku.semicolon.boundedContext.user.entity.type.UserStatus> statuses,
            java.time.LocalDateTime deletedAt
    );
}
