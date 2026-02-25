package dukku.user.boundedContext.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 50, comment = "배송지명")
    private String name;

    @Column(length = 50, comment = "수령인")
    private String recipient;

    @Column(length = 50, comment = "연락처")
    private String phone;

    @Column(nullable = false)
    private String address;

    @Column
    private String detailAddress;

    @Column(nullable = false, length = 10)
    private String zonecode;

    @Column(nullable = false)
    private boolean isDefault;

    @Builder
    public Address(
            User user,
            String name,
            String recipient,
            String phone,
            String address,
            String detailAddress,
            String zonecode,
            boolean isDefault) {
        this.user = user;
        this.name = name;
        this.recipient = recipient;
        this.phone = phone;
        this.address = address;
        this.detailAddress = detailAddress;
        this.zonecode = zonecode;
        this.isDefault = isDefault;
    }

    public void changeDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void update(
            String name,
            String recipient,
            String phone,
            String address,
            String detailAddress,
            String zonecode) {
        this.name = name;
        this.recipient = recipient;
        this.phone = phone;
        this.address = address;
        this.detailAddress = detailAddress;
        this.zonecode = zonecode;
    }
}
