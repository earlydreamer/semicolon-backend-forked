package dukku.user.boundedContext.user.in.dto;

import dukku.user.boundedContext.user.entity.Address;
import lombok.Getter;

@Getter
public class AddressResponse {
    private Long id;
    private String name;
    private String recipient;
    private String phone;
    private String address;
    private String detailAddress;
    private String zonecode;
    @com.fasterxml.jackson.annotation.JsonProperty("isDefault")
    private boolean isDefault;

    public static AddressResponse from(Address address) {
        AddressResponse res = new AddressResponse();
        res.id = address.getId();
        res.name = address.getName();
        res.recipient = address.getRecipient();
        res.phone = address.getPhone();
        res.address = address.getAddress();
        res.detailAddress = address.getDetailAddress();
        res.zonecode = address.getZonecode();
        res.isDefault = address.isDefault();
        return res;
    }

}
