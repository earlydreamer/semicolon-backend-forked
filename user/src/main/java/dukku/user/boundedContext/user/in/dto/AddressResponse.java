package dukku.user.boundedContext.user.in.dto;

import dukku.user.boundedContext.user.entity.Address;
import lombok.Getter;

@Getter
public class AddressResponse {
    private Long id;
    private String address;
    private String zonecode;
    private boolean isDefault;

    public static AddressResponse from(Address address) {
        AddressResponse res = new AddressResponse();
        res.id = address.getId();
        res.address = address.getAddress();
        res.zonecode = address.getZonecode();
        res.isDefault = address.isDefault();
        return res;
    }

}
