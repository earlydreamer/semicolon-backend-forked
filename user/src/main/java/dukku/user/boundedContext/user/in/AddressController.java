package dukku.user.boundedContext.user.in;

import dukku.common.global.UserUtil;
import dukku.common.shared.user.docs.AddressApiDocs;
import dukku.user.boundedContext.user.app.address.AddAddressUseCase;
import dukku.user.boundedContext.user.app.address.DeleteAddressUseCase;
import dukku.user.boundedContext.user.app.address.FindDefaultAddressUseCase;
import dukku.user.boundedContext.user.app.address.FindAddressUseCase;
import dukku.user.boundedContext.user.app.address.SetDefaultAddressUseCase;
import dukku.user.boundedContext.user.app.address.UpdateAddressUseCase;
import dukku.user.boundedContext.user.in.dto.AddressRequest;
import dukku.user.boundedContext.user.in.dto.AddressResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
@AddressApiDocs.AddressTag
public class AddressController {
    private final AddAddressUseCase addAddressUseCase;
    private final FindAddressUseCase findAddressUseCase;
    private final UpdateAddressUseCase updateAddressUseCase;
    private final DeleteAddressUseCase deleteAddressUseCase;
    private final SetDefaultAddressUseCase setDefaultAddressUseCase;
    private final FindDefaultAddressUseCase findDefaultAddressUseCase;

    @GetMapping
    @AddressApiDocs.GetMyAddresses
    public org.springframework.data.domain.Page<AddressResponse> getMyAddresses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userUuid = UserUtil.getUserId();
        return findAddressUseCase.execute(userUuid, org.springframework.data.domain.PageRequest.of(page, size));
    }

    @PostMapping
    @AddressApiDocs.AddAddress
    public AddressResponse addAddress(
            @RequestBody @Valid AddressRequest request) {
        UUID userUuid = UserUtil.getUserId();
        return addAddressUseCase.add(userUuid, request);
    }

    @PatchMapping("/{addressId}")
    @AddressApiDocs.UpdateAddress
    public AddressResponse updateAddress(
            @PathVariable Long addressId,
            @RequestBody @Valid AddressRequest request) {
        UUID userUuid = UserUtil.getUserId();
        return updateAddressUseCase.execute(userUuid, addressId, request);
    }

    @DeleteMapping("/{addressId}")
    @AddressApiDocs.DeleteAddress
    public void deleteAddress(@PathVariable Long addressId) {
        UUID userUuid = UserUtil.getUserId();
        deleteAddressUseCase.execute(userUuid, addressId);
    }

    @PatchMapping("/{addressId}/default")
    @AddressApiDocs.SetDefaultAddress
    public AddressResponse setDefaultAddress(@PathVariable Long addressId) {
        UUID userUuid = UserUtil.getUserId();
        return setDefaultAddressUseCase.execute(userUuid, addressId);
    }

    @GetMapping("/default")
    @AddressApiDocs.GetMyDefaultAddress
    public AddressResponse getMyDefaultAddress() {
        UUID userUuid = UserUtil.getUserId();
        return findDefaultAddressUseCase.execute(userUuid);
    }
}
