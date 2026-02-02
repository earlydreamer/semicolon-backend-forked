package dukku.semicolon.boundedContext.settlement.in;

import dukku.common.shared.deposit.event.DepositChargeFailedEvent;
import dukku.common.shared.deposit.event.DepositChargeSucceededEvent;
import dukku.common.shared.order.event.OrderItemConfirmedEvent;
import dukku.semicolon.boundedContext.settlement.app.SettlementFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;


/**
 * 정산 이벤트 리스너
 *  */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementEventListener {

    private final SettlementFacade settlementFacade;



}
