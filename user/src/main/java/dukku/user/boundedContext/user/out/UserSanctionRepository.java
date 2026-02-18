package dukku.user.boundedContext.user.out;

import dukku.user.boundedContext.user.entity.User;
import dukku.user.boundedContext.user.entity.UserSanction;
import dukku.user.boundedContext.user.type.UserSanctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserSanctionRepository extends JpaRepository<UserSanction, Integer> {

    Optional<UserSanction> findByIdAndUser(Integer id, User user);

    List<UserSanction> findByUserOrderByCreatedAtDesc(User user);

    List<UserSanction> findByUserAndStatus(User user, UserSanctionStatus status);

    List<UserSanction> findByStatusAndEndAtBefore(UserSanctionStatus status, LocalDateTime endAt);
}
