package dukku.semicolon.boundedContext.settlement.out.impl;

import com.querydsl.core.Tuple;
import dukku.semicolon.boundedContext.settlement.entity.Settlement;
import dukku.common.shared.settlement.dto.SettlementSearchCondition;
import dukku.common.shared.settlement.dto.SettlementStatisticsCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 정산 검색/동적 쿼리용 Repository
 * QueryDSL 사용 (동적 조건 처리에 최적)
 */
public interface SettlementRepositoryCustom {

    Page<Settlement> search(SettlementSearchCondition condition, Pageable pageable);

    Tuple getTotalStatistics();

    List<Tuple> getStatisticsByStatus();

    long countByCondition(SettlementStatisticsCondition condition);

    long sumSettlementAmountByCondition(SettlementStatisticsCondition condition);
}
