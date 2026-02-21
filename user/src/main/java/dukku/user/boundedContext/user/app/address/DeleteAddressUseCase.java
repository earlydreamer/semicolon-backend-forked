package dukku.user.boundedContext.user.app.address;

import dukku.common.shared.user.exception.UserAddressNotFoundException;
import dukku.user.boundedContext.user.entity.Address;
import dukku.user.boundedContext.user.out.AddressRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeleteAddressUseCase {
    private final AddressRepository addressRepository;

    @Transactional
    public void execute(UUID userUuid, Long addressId) {
        Address address = addressRepository.findByIdAndUser_Uuid(addressId, userUuid)
                .orElseThrow(UserAddressNotFoundException::new);
        boolean wasDefault = address.isDefault();

        addressRepository.delete(address);

        if (wasDefault) {
            addressRepository.findFirstByUser_UuidOrderByIdAsc(userUuid)
                    .ifPresent(nextDefaultAddress -> nextDefaultAddress.changeDefault(true));
        }
    }
}
