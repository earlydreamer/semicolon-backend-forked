package dukku.user.boundedContext.user.out;

import dukku.user.boundedContext.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUser_Uuid(UUID userUuid);

    Optional<Address> findByIdAndUser_Uuid(Long id, UUID userUuid);

    boolean existsByUser_UuidAndIsDefaultTrue(UUID userUuid);

    long countByUser_Uuid(UUID userUuid);
}
