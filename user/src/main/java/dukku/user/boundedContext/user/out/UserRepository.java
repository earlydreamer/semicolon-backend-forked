package dukku.user.boundedContext.user.out;

import dukku.user.boundedContext.user.entity.User;
import dukku.common.shared.user.type.Role;
import dukku.common.shared.user.type.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByUuid(UUID userUuid);

    void deleteByUuid(UUID userUuid);

    Optional<User> findByUuidAndDeletedAtIsNull(UUID userUuid);

    Optional<User> findByRoleAndDeletedAtIsNull(Role role);

    boolean existsByEmailAndDeletedAtIsNullAndIdNot(String email, Integer id);

    List<User> findByStatusInAndDeletedAtBefore(
            List<UserStatus> statuses,
            LocalDateTime deletedAt
    );

    @Query("SELECT u FROM User u WHERE " +
           "(:keyword = '' OR u.email LIKE :keyword OR u.nickname LIKE :keyword) " +
           "AND (:status IS NULL OR u.status = :status)")
    Page<User> searchUsers(
            @Param("keyword") String keyword, 
            @Param("status") UserStatus status, 
            Pageable pageable
    );
}
