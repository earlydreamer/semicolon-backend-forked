package dukku.user.boundedContext.user.app.address;

import dukku.common.shared.user.exception.UserAddressLimitExceededException;
import dukku.user.boundedContext.user.entity.Address;
import dukku.user.boundedContext.user.entity.User;
import dukku.user.boundedContext.user.in.dto.AddressRequest;
import dukku.user.boundedContext.user.in.dto.AddressResponse;
import dukku.user.boundedContext.user.out.AddressRepository;
import dukku.user.boundedContext.user.out.UserRepository;
import dukku.common.shared.user.exception.UserNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AddAddressUseCase {
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    @Value("${custom.user.address.max-count:4}")
    private int maxAddressCount;

    @PostConstruct
    void validateAddressLimitConfig() {
        if (maxAddressCount < 3 || maxAddressCount > 4) {
            throw new IllegalStateException("custom.user.address.max-count must be 3 or 4.");
        }
    }

    @Transactional
    public AddressResponse add(UUID userUuid, AddressRequest request) {
        User user = userRepository.findByUuidAndDeletedAtIsNull(userUuid)
                .orElseThrow(UserNotFoundException::new);

        long addressCount = addressRepository.countByUser_Uuid(userUuid);
        if (addressCount >= maxAddressCount) {
            throw new UserAddressLimitExceededException(maxAddressCount);
        }

        boolean isFirstAddress = addressCount == 0;

        Address address = Address.builder()
                .user(user)
                .address(request.getAddress())
                .zonecode(request.getZonecode())
                .isDefault(isFirstAddress)
                .build();

        return AddressResponse.from(addressRepository.save(address));
    }
}
