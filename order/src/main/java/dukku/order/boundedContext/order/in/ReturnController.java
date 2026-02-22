package dukku.order.boundedContext.order.in;

import dukku.common.global.UserUtil;
import dukku.common.shared.order.dto.ReturnRejectDto;
import dukku.common.shared.order.dto.ReturnRequestCreateDto;
import dukku.common.shared.order.dto.ReturnResponse;
import dukku.common.shared.order.dto.ReturnTrackingRegisterDto;
import dukku.order.boundedContext.order.app.ApproveReturnUseCase;
import dukku.order.boundedContext.order.app.FinalRejectReturnUseCase;
import dukku.order.boundedContext.order.app.RegisterReturnTrackingUseCase;
import dukku.order.boundedContext.order.app.RequestReturnUseCase;
import dukku.order.boundedContext.order.app.SellerApproveReturnUseCase;
import dukku.order.boundedContext.order.app.SellerRejectReturnUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 반품 요청/처리 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final RequestReturnUseCase requestReturnUseCase;
    private final RegisterReturnTrackingUseCase registerReturnTrackingUseCase;
    private final SellerApproveReturnUseCase sellerApproveReturnUseCase;
    private final SellerRejectReturnUseCase sellerRejectReturnUseCase;
    private final ApproveReturnUseCase approveReturnUseCase;
    private final FinalRejectReturnUseCase finalRejectReturnUseCase;

    /**
     * 구매자 반품 신청 처리 API
     */
    @PostMapping("/orders/{orderUuid}")
    public ResponseEntity<ReturnResponse> requestReturn(
            @PathVariable UUID orderUuid,
            @RequestBody ReturnRequestCreateDto requestDto) {
        UUID userUuid = UserUtil.getUserId();
        ReturnResponse response = requestReturnUseCase.execute(userUuid, orderUuid, requestDto);
        return ResponseEntity.ok(response);
    }

    /**
     * 판매자 1차 승인 처리 API
     */
    @PostMapping("/{returnRequestUuid}/seller-approve")
    public ResponseEntity<ReturnResponse> approveBySeller(
            @PathVariable UUID returnRequestUuid) {
        UUID sellerUuid = UserUtil.getUserId();
        ReturnResponse response = sellerApproveReturnUseCase.execute(sellerUuid, returnRequestUuid);
        return ResponseEntity.ok(response);
    }

    /**
     * 판매자 1차 거절 처리 API
     */
    @PostMapping("/{returnRequestUuid}/seller-reject")
    public ResponseEntity<ReturnResponse> rejectBySeller(
            @PathVariable UUID returnRequestUuid,
            @RequestBody ReturnRejectDto requestDto) {
        UUID sellerUuid = UserUtil.getUserId();
        ReturnResponse response = sellerRejectReturnUseCase.execute(sellerUuid, returnRequestUuid, requestDto.getReason());
        return ResponseEntity.ok(response);
    }

    /**
     * 구매자 반품 운송장 등록 API
     */
    @PutMapping("/{returnRequestUuid}/tracking")
    public ResponseEntity<ReturnResponse> registerTrackingInfo(
            @PathVariable UUID returnRequestUuid,
            @RequestBody ReturnTrackingRegisterDto requestDto) {
        UUID userUuid = UserUtil.getUserId();
        ReturnResponse response = registerReturnTrackingUseCase.execute(userUuid, returnRequestUuid, requestDto);
        return ResponseEntity.ok(response);
    }

    /**
     * 판매자 최종 승인 처리 API
     */
    @PostMapping("/{returnRequestUuid}/final-approve")
    public ResponseEntity<ReturnResponse> approveReturn(
            @PathVariable UUID returnRequestUuid) {
        UUID sellerUuid = UserUtil.getUserId();
        ReturnResponse response = approveReturnUseCase.execute(sellerUuid, returnRequestUuid);
        return ResponseEntity.ok(response);
    }

    /**
     * 판매자 최종 거절 처리 API
     */
    @PostMapping("/{returnRequestUuid}/final-reject")
    public ResponseEntity<ReturnResponse> rejectFinalReturn(
            @PathVariable UUID returnRequestUuid,
            @RequestBody ReturnRejectDto requestDto) {
        UUID sellerUuid = UserUtil.getUserId();
        ReturnResponse response = finalRejectReturnUseCase.execute(sellerUuid, returnRequestUuid, requestDto.getReason());
        return ResponseEntity.ok(response);
    }
}
