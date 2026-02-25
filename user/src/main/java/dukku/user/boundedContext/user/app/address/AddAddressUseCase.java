package dukku.user.boundedContext.user.app.address;

import dukku.user.boundedContext.user.entity.Address;
import dukku.user.boundedContext.user.entity.User;
import dukku.user.boundedContext.user.in.dto.AddressRequest;
import dukku.user.boundedContext.user.in.dto.AddressResponse;
import dukku.user.boundedContext.user.out.AddressRepository;
import dukku.user.boundedContext.user.out.UserRepository;
import dukku.common.shared.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AddAddressUseCase {
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional
    public AddressResponse add(UUID userUuid, AddressRequest request) {
        User user = userRepository.findByUuidAndDeletedAtIsNull(userUuid)
                .orElseThrow(UserNotFoundException::new);

        long addressCount = addressRepository.countByUser_Uuid(userUuid);
        boolean isFirstAddress = addressCount == 0;

        Address address = Address.builder()
                .user(user)
                .name(request.getName())
                .recipient(request.getRecipient())
                .phone(request.getPhone())
                .address(request.getAddress())
                .detailAddress(request.getDetailAddress())
                .zonecode(request.getZonecode())
                .isDefault(isFirstAddress)
                .build();

        return AddressResponse.from(addressRepository.save(address));
    }
}
