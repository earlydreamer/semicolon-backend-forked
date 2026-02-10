package dukku.common.shared.settlement.dto;

import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

/**
 * 통계 조회 요청 DTO (공통)
 */
public record SettlementReportRequest(
        @PastOrPresent(message = "시작일은 현재 또는 과거 날짜여야 합니다")
        LocalDate startDate,

        @PastOrPresent(message = "종료일은 현재 또는 과거 날짜여야 합니다")
        LocalDate endDate
) {
    public SettlementReportRequest {
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
    }


}
