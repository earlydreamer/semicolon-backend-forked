package dukku.user.boundedContext.user.out;

import dukku.user.boundedContext.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUser_UuidOrderByIsDefaultDescIdDesc(UUID userUuid);

    org.springframework.data.domain.Page<Address> findByUser_UuidOrderByIsDefaultDescIdDesc(UUID userUuid,
            org.springframework.data.domain.Pageable pageable);

    Optional<Address> findByIdAndUser_Uuid(Long id, UUID userUuid);

    Optional<Address> findByUser_UuidAndIsDefaultTrue(UUID userUuid);

    Optional<Address> findFirstByUser_UuidOrderByIdAsc(UUID userUuid);

    boolean existsByUser_UuidAndIsDefaultTrue(UUID userUuid);

    long countByUser_Uuid(UUID userUuid);
}
