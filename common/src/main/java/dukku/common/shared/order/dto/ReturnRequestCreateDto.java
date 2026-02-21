package dukku.common.shared.order.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReturnRequestCreateDto {
    private String reason;
    private List<UUID> orderItemUuids;
}
