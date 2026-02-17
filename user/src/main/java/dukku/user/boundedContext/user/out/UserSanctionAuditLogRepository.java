package dukku.user.boundedContext.user.out;

import dukku.user.boundedContext.user.entity.UserSanctionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSanctionAuditLogRepository extends JpaRepository<UserSanctionAuditLog, Integer> {
}
