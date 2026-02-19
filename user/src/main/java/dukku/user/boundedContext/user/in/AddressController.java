package dukku.user.boundedContext.user.in;

import dukku.common.global.UserUtil;
import dukku.common.shared.user.docs.AddressApiDocs;
import dukku.user.boundedContext.user.app.address.AddAddressUseCase;
import dukku.user.boundedContext.user.app.address.FindAddressUseCase;
import dukku.user.boundedContext.user.in.dto.AddressRequest;
import dukku.user.boundedContext.user.in.dto.AddressResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
@AddressApiDocs.AddressTag
public class AddressController {
    private final AddAddressUseCase addAddressUseCase;
    private final FindAddressUseCase findAddressUseCase;

    @GetMapping
    @AddressApiDocs.GetMyAddresses
    public List<AddressResponse> getMyAddresses() {
        UUID userUuid = UserUtil.getUserId();
        return findAddressUseCase.execute(userUuid);
    }

    @PostMapping
    @AddressApiDocs.AddAddress
    public AddressResponse addAddress(
            @RequestBody @Valid AddressRequest request
    ) {
        UUID userUuid = UserUtil.getUserId();
        return addAddressUseCase.add(userUuid, request);
    }
}
