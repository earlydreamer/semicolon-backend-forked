package dukku.settlement.boundedContext.settlement.app;

import dukku.common.shared.settlement.dto.SettlementSearchCondition;
import dukku.settlement.boundedContext.settlement.entity.Settlement;
import dukku.settlement.boundedContext.settlement.out.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetSettlementListUseCase {

    private final SettlementRepository settlementRepository;

    /**
     * 정산 목록 조회 (검색 조건 + 페이징)
     */
    @Transactional(readOnly = true)
    public Page<Settlement> execute(SettlementSearchCondition condition, Pageable pageable) {
        return settlementRepository.search(condition, pageable);
    }
}
