package dukku.user.boundedContext.user.out;

import dukku.user.boundedContext.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserUuid(UUID userUuid);

    Optional<Address> findByIdAndUserUuid(Long id, UUID userUuid);

    boolean existsByUserUuidAndIsDefaultTrue(UUID userUuid);

    long countByUserUuid(UUID userUuid);
}
