package dukku.user.boundedContext.user.app.address;

import dukku.common.shared.user.exception.UserAddressNotFoundException;
import dukku.user.boundedContext.user.entity.Address;
import dukku.user.boundedContext.user.in.dto.AddressResponse;
import dukku.user.boundedContext.user.out.AddressRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SetDefaultAddressUseCase {
    private final AddressRepository addressRepository;

    @Transactional
    public AddressResponse execute(UUID userUuid, Long addressId) {
        Address targetAddress = addressRepository.findByIdAndUser_Uuid(addressId, userUuid)
                .orElseThrow(UserAddressNotFoundException::new);

        List<Address> addresses = addressRepository.findByUser_UuidOrderByIsDefaultDescIdDesc(userUuid);
        for (Address address : addresses) {
            address.changeDefault(address.getId().equals(targetAddress.getId()));
        }

        return AddressResponse.from(targetAddress);
    }
}
