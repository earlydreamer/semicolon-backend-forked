package dukku.user.boundedContext.user.in.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class AddressRequest {
    private String name;

    @NotBlank
    private String recipient;

    @NotBlank
    private String phone;

    @NotBlank
    private String address;

    @NotBlank
    private String detailAddress;

    @NotBlank
    private String zonecode;

    private String addressType;
    private String bcode;
    private String bname;
    private String bname1;
    private String bname2;
    private String sido;
    private String sigungu;
    private String sigunguCode;
    private String roadname;
    private String roadnameCode;
    private String buildingCode;
    private String buildingName;
    private String apartment;
    private String jibunAddress;
    private String roadAddress;
    private String autoRoadAddress;
    private String autoJibunAddress;
    private String userSelectedType;
    private String noSelected;
    private String query;
}
