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

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, length = 10)
    private String zonecode;

    @Column(nullable = false)
    private boolean isDefault;

    @Builder
    public Address(
            User user,
            String address,
            String zonecode,
            boolean isDefault
    ) {
        this.user = user;
        this.address = address;
        this.zonecode = zonecode;
        this.isDefault = isDefault;
    }

    public void changeDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void update(
            String address,
            String zonecode
    ) {
        this.address = address;
        this.zonecode = zonecode;
    }
}
