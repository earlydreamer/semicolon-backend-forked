package dukku.semicolon.boundedContext.product.entity.tag;

import dukku.common.global.jpa.entity.BaseIdAndTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Tag extends BaseIdAndTime {
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    public static Tag create(String name) {
        return Tag.builder().name(name).build();
    }
}
