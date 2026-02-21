package dukku.user.boundedContext.user.app.address;

import dukku.common.shared.user.exception.UserAddressNotFoundException;
import dukku.user.boundedContext.user.entity.Address;
import dukku.user.boundedContext.user.in.dto.AddressRequest;
import dukku.user.boundedContext.user.in.dto.AddressResponse;
import dukku.user.boundedContext.user.out.AddressRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateAddressUseCase {
    private final AddressRepository addressRepository;

    @Transactional
    public AddressResponse execute(UUID userUuid, Long addressId, AddressRequest request) {
        Address address = addressRepository.findByIdAndUser_Uuid(addressId, userUuid)
                .orElseThrow(UserAddressNotFoundException::new);

        address.update(request.getAddress(), request.getZonecode());

        return AddressResponse.from(address);
    }
}
