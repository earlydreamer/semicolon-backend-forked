package dukku.user.boundedContext.user.app.address;

import dukku.common.shared.user.exception.UserAddressNotFoundException;
import dukku.user.boundedContext.user.entity.Address;
import dukku.user.boundedContext.user.in.dto.AddressResponse;
import dukku.user.boundedContext.user.out.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FindDefaultAddressUseCase {
    private final AddressRepository addressRepository;

    @Transactional(readOnly = true)
    public AddressResponse execute(UUID userUuid) {
        Address defaultAddress = addressRepository.findByUser_UuidAndIsDefaultTrue(userUuid)
                .or(() -> addressRepository.findFirstByUser_UuidOrderByIdAsc(userUuid))
                .orElseThrow(UserAddressNotFoundException::new);

        return AddressResponse.from(defaultAddress);
    }
}
