package dukku.order.boundedContext.order.in;

import dukku.common.shared.order.dto.ReturnRequestCreateDto;
import dukku.common.shared.order.dto.ReturnResponse;
import dukku.common.shared.order.dto.ReturnTrackingRegisterDto;
import dukku.common.global.UserUtil;
import dukku.order.boundedContext.order.app.ApproveReturnUseCase;
import dukku.order.boundedContext.order.app.RegisterReturnTrackingUseCase;
import dukku.order.boundedContext.order.app.RequestReturnUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final RequestReturnUseCase requestReturnUseCase;
    private final RegisterReturnTrackingUseCase registerReturnTrackingUseCase;
    private final ApproveReturnUseCase approveReturnUseCase;

    @PostMapping("/orders/{orderUuid}")
    public ResponseEntity<ReturnResponse> requestReturn(
            @PathVariable UUID orderUuid,
            @RequestBody ReturnRequestCreateDto requestDto) {
        UUID userUuid = UserUtil.getUserId();
        ReturnResponse response = requestReturnUseCase.execute(userUuid, orderUuid, requestDto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{returnRequestUuid}/tracking")
    public ResponseEntity<ReturnResponse> registerTrackingInfo(
            @PathVariable UUID returnRequestUuid,
            @RequestBody ReturnTrackingRegisterDto requestDto) {
        UUID userUuid = UserUtil.getUserId();
        ReturnResponse response = registerReturnTrackingUseCase.execute(userUuid, returnRequestUuid, requestDto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{returnRequestUuid}/approve")
    public ResponseEntity<ReturnResponse> approveReturn(
            @PathVariable UUID returnRequestUuid) {
        UUID sellerUuid = UserUtil.getUserId();
        ReturnResponse response = approveReturnUseCase.execute(sellerUuid, returnRequestUuid);
        return ResponseEntity.ok(response);
    }
}
