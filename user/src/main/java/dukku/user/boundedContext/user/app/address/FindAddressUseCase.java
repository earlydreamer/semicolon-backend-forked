package dukku.user.boundedContext.user.app.address;

import dukku.user.boundedContext.user.in.dto.AddressResponse;
import dukku.user.boundedContext.user.out.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FindAddressUseCase {

    private final AddressRepository addressRepository;

    @Transactional(readOnly = true)
    public List<AddressResponse> execute(UUID userUuid) {
        return addressRepository.findByUser_UuidOrderByIsDefaultDescIdDesc(userUuid)
                .stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AddressResponse> execute(UUID userUuid,
            org.springframework.data.domain.Pageable pageable) {
        return addressRepository.findByUser_UuidOrderByIsDefaultDescIdDesc(userUuid, pageable)
                .map(AddressResponse::from);
    }
}
