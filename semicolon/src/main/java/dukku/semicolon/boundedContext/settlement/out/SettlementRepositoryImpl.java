package dukku.semicolon.boundedContext.settlement.out;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import dukku.semicolon.boundedContext.settlement.entity.Settlement;
import dukku.semicolon.boundedContext.settlement.entity.type.SettlementStatus;
import dukku.semicolon.shared.settlement.dto.FinancialStatisticsResponse;
import dukku.semicolon.shared.settlement.dto.SellerStatisticsResponse.SellerSettlementSummary;
import dukku.semicolon.shared.settlement.dto.SettlementSearchCondition;
import dukku.semicolon.shared.settlement.dto.SettlementStatisticsCondition;
import dukku.semicolon.shared.settlement.dto.TrendStatisticsResponse.DailyTrend;
import dukku.semicolon.shared.settlement.dto.TrendStatisticsResponse.MonthlyTrend;
import dukku.semicolon.shared.settlement.dto.TrendStatisticsResponse.ProcessingTimeStats;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static dukku.semicolon.boundedContext.settlement.entity.QSettlement.settlement;

@RequiredArgsConstructor
public class SettlementRepositoryImpl implements SettlementRepositoryCustom {

    private static final double SECONDS_PER_HOUR = 3600.0;

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Settlement> search(SettlementSearchCondition condition, Pageable pageable) {
        List<Settlement> content = queryFactory
                .selectFrom(settlement)
                .where(
                        statusEq(condition.status()),
                        sellerUuidEq(condition.sellerUuid()),
                        createdAtGoe(condition.startDate()),
                        createdAtLt(condition.endDate())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(settlement.createdAt.desc())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(settlement.count())
                .from(settlement)
                .where(
                        statusEq(condition.status()),
                        sellerUuidEq(condition.sellerUuid()),
                        createdAtGoe(condition.startDate()),
                        createdAtLt(condition.endDate())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Tuple getTotalStatistics() {
        return queryFactory
                .select(
                        settlement.count(),
                        settlement.totalAmount.sum().coalesce(0L),
                        settlement.settlementAmount.sum().coalesce(0L),
                        settlement.feeAmount.sum().coalesce(0L)
                )
                .from(settlement)
                .fetchOne();
    }

    @Override
    public List<Tuple> getStatisticsByStatus() {
        return queryFactory
                .select(
                        settlement.settlementStatus,
                        settlement.count(),
                        settlement.settlementAmount.sum().coalesce(0L)
                )
                .from(settlement)
                .groupBy(settlement.settlementStatus)
                .fetch();
    }

    @Override
    public long countByCondition(SettlementStatisticsCondition condition) {
        Long count = queryFactory
                .select(settlement.count())
                .from(settlement)
                .where(
                        statusEq(condition.status()),
                        sellerUuidEq(condition.sellerUuid()),
                        completedAtGoe(condition.startDate()),
                        completedAtLt(condition.endDate())
                )
                .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public long sumSettlementAmountByCondition(SettlementStatisticsCondition condition) {
        Long sum = queryFactory
                .select(settlement.settlementAmount.sum().longValue())
                .from(settlement)
                .where(
                        statusEq(condition.status()),
                        sellerUuidEq(condition.sellerUuid()),
                        completedAtGoe(condition.startDate()),
                        completedAtLt(condition.endDate())
                )
                .fetchOne();

        return sum != null ? sum : 0L;
    }

    // ===== 리포트용 구현 (DTO 직접 반환) =====

    @Override
    public FinancialStatisticsResponse getFinancialStatistics() {
        Tuple result = queryFactory
                .select(
                        feeAmountWhen(SettlementStatus.SUCCESS).sum().coalesce(0L),
                        settlementAmountWhen(SettlementStatus.PENDING).sum().coalesce(0L),
                        settlementAmountWhen(SettlementStatus.PROCESSING).sum().coalesce(0L),
                        settlementAmountWhen(SettlementStatus.FAILED).sum().coalesce(0L),
                        totalAmountWhen(SettlementStatus.SUCCESS).sum().coalesce(0L),
                        settlementAmountWhen(SettlementStatus.SUCCESS).sum().coalesce(0L),
                        settlement.fee.avg()
                )
                .from(settlement)
                .fetchOne();

        if (result == null) {
            return new FinancialStatisticsResponse(0L, 0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO);
        }

        return new FinancialStatisticsResponse(
                result.get(0, Long.class),
                result.get(1, Long.class),
                result.get(2, Long.class),
                result.get(3, Long.class),
                result.get(4, Long.class),
                result.get(5, Long.class),
                toAvgFeeRate(result.get(6, Double.class))
        );
    }

    @Override
    public List<SellerSettlementSummary> getSellerStatistics(Pageable pageable) {
        List<Tuple> results = queryFactory
                .select(
                        settlement.sellerUuid,
                        settlement.count(),
                        countWhen(SettlementStatus.SUCCESS).sum().coalesce(0L),
                        countWhen(SettlementStatus.FAILED).sum().coalesce(0L),
                        countWhen(SettlementStatus.PENDING).sum().coalesce(0L),
                        settlementAmountWhen(SettlementStatus.SUCCESS).sum().coalesce(0L),
                        feeAmountWhen(SettlementStatus.SUCCESS).sum().coalesce(0L)
                )
                .from(settlement)
                .groupBy(settlement.sellerUuid)
                .orderBy(settlement.count().desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return results.stream()
                .map(this::toSellerSummary)
                .toList();
    }

    @Override
    public long countDistinctSellers() {
        Long count = queryFactory
                .select(settlement.sellerUuid.countDistinct())
                .from(settlement)
                .fetchOne();
        return count != null ? count : 0L;
    }

    @Override
    public List<DailyTrend> getDailyTrend(LocalDate startDate, LocalDate endDate) {
        return queryFactory
                .select(Projections.constructor(DailyTrend.class,
                        completedDate(),
                        settlement.count(),
                        settlement.settlementAmount.sum().coalesce(0L),
                        settlement.feeAmount.sum().coalesce(0L),
                        settlement.totalAmount.sum().coalesce(0L)
                ))
                .from(settlement)
                .where(
                        isSuccess(),
                        completedAtFrom(startDate),
                        completedAtTo(endDate)
                )
                .groupBy(completedDate())
                .orderBy(completedDate().desc())
                .fetch();
    }

    @Override
    public List<MonthlyTrend> getMonthlyTrend(LocalDate startDate, LocalDate endDate) {
        return queryFactory
                .select(Projections.constructor(MonthlyTrend.class,
                        completedYear(),
                        completedMonth(),
                        settlement.count(),
                        settlement.settlementAmount.sum().coalesce(0L),
                        settlement.feeAmount.sum().coalesce(0L),
                        settlement.totalAmount.sum().coalesce(0L)
                ))
                .from(settlement)
                .where(
                        isSuccess(),
                        completedAtFrom(startDate),
                        completedAtTo(endDate)
                )
                .groupBy(completedYear(), completedMonth())
                .orderBy(completedYear().desc(), completedMonth().desc())
                .fetch();
    }

    @Override
    public ProcessingTimeStats getProcessingTimeStats() {
        Tuple result = queryFactory
                .select(
                        reservationToCompletionSeconds().avg(),
                        creationToCompletionSeconds().avg(),
                        creationToCompletionSeconds().min(),
                        creationToCompletionSeconds().max()
                )
                .from(settlement)
                .where(isSuccess(), completedAtNotNull())
                .fetchOne();

        if (result == null) {
            return new ProcessingTimeStats(0.0, 0.0, 0.0, 0.0);
        }

        return new ProcessingTimeStats(
                toHours(result.get(0, Double.class)),
                toHours(result.get(1, Double.class)),
                toHours(result.get(2, Double.class)),
                toHours(result.get(3, Double.class))
        );
    }

    // ===== Private 헬퍼 메서드 =====

    private BooleanExpression statusEq(SettlementStatus status) {
        return status != null ? settlement.settlementStatus.eq(status) : null;
    }

    private BooleanExpression sellerUuidEq(UUID sellerUuid) {
        return sellerUuid != null ? settlement.sellerUuid.eq(sellerUuid) : null;
    }

    private BooleanExpression createdAtGoe(LocalDate startDate) {
        return startDate != null ? settlement.createdAt.goe(startDate.atStartOfDay()) : null;
    }

    private BooleanExpression createdAtLt(LocalDate endDate) {
        return endDate != null ? settlement.createdAt.lt(endDate.plusDays(1).atStartOfDay()) : null;
    }

    private BooleanExpression completedAtGoe(LocalDate startDate) {
        return startDate != null ? settlement.completedAt.goe(startDate.atStartOfDay()) : null;
    }

    private BooleanExpression completedAtLt(LocalDate endDate) {
        return endDate != null ? settlement.completedAt.lt(endDate.plusDays(1).atStartOfDay()) : null;
    }

    private SellerSettlementSummary toSellerSummary(Tuple tuple) {
        return SellerSettlementSummary.of(
                tuple.get(0, UUID.class),
                tuple.get(1, Long.class),
                tuple.get(2, Long.class),
                tuple.get(3, Long.class),
                tuple.get(4, Long.class),
                tuple.get(5, Long.class),
                tuple.get(6, Long.class)
        );
    }

    private BigDecimal toAvgFeeRate(Double value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private double toHours(Double seconds) {
        if (seconds == null) {
            return 0.0;
        }
        return Math.round(seconds / SECONDS_PER_HOUR * 100.0) / 100.0;
    }

    // ===== 상태별 금액 표현식 =====

    private NumberExpression<Long> settlementAmountWhen(SettlementStatus status) {
        return settlement.settlementStatus
                .when(status).then(settlement.settlementAmount)
                .otherwise(0L);
    }

    private NumberExpression<Long> feeAmountWhen(SettlementStatus status) {
        return settlement.settlementStatus
                .when(status).then(settlement.feeAmount)
                .otherwise(0L);
    }

    private NumberExpression<Long> totalAmountWhen(SettlementStatus status) {
        return settlement.settlementStatus
                .when(status).then(settlement.totalAmount)
                .otherwise(0L);
    }

    private NumberExpression<Long> countWhen(SettlementStatus status) {
        return settlement.settlementStatus
                .when(status).then(1L)
                .otherwise(0L);
    }

    // ===== 날짜/시간 표현식 =====

    private StringExpression completedDate() {
        return Expressions.stringTemplate("DATE({0})", settlement.completedAt);
    }

    private NumberExpression<Integer> completedYear() {
        return Expressions.numberTemplate(Integer.class, "YEAR({0})", settlement.completedAt);
    }

    private NumberExpression<Integer> completedMonth() {
        return Expressions.numberTemplate(Integer.class, "MONTH({0})", settlement.completedAt);
    }

    private NumberExpression<Double> reservationToCompletionSeconds() {
        return Expressions.numberTemplate(Double.class,
                "TIMESTAMPDIFF(SECOND, {0}, {1})",
                settlement.settlementReservationDate, settlement.completedAt);
    }

    private NumberExpression<Double> creationToCompletionSeconds() {
        return Expressions.numberTemplate(Double.class,
                "TIMESTAMPDIFF(SECOND, {0}, {1})",
                settlement.createdAt, settlement.completedAt);
    }

    // ===== 조건 표현식 =====

    private BooleanExpression completedAtFrom(LocalDate startDate) {
        return startDate != null ? settlement.completedAt.goe(startDate.atStartOfDay()) : null;
    }

    private BooleanExpression completedAtTo(LocalDate endDate) {
        return endDate != null ? settlement.completedAt.lt(endDate.plusDays(1).atStartOfDay()) : null;
    }

    private BooleanExpression completedAtNotNull() {
        return settlement.completedAt.isNotNull();
    }

    private BooleanExpression isSuccess() {
        return settlement.settlementStatus.eq(SettlementStatus.SUCCESS);
    }
}
