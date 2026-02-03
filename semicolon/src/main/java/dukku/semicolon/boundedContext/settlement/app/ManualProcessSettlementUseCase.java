package dukku.semicolon.boundedContext.settlement.app;

import dukku.semicolon.boundedContext.settlement.entity.Settlement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 정산 수동 예치금 충전 요청 UseCase
 * - Settlement 상태: PENDING → PROCESSING
 * - 관리자가 정산을 수동으로 예치금 충전 요청 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ManualProcessSettlementUseCase {

    //TODO: deposit api client 구현되면 코드 작성

}
