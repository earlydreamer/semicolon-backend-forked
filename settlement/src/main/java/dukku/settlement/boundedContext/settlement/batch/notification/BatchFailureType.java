package dukku.settlement.boundedContext.settlement.batch.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 배치 실패 유형 분류
 */
@Getter
@RequiredArgsConstructor
public enum BatchFailureType {

    SERVICE_UNAVAILABLE("외부 서비스 연결 실패", "해당 서비스가 동작 중인지 확인하세요."),
    SKIP_LIMIT_EXCEEDED("처리 실패 건수 초과", "skipLimit을 초과하는 오류가 발생했습니다. 데이터를 점검하세요."),
    DB_ERROR("데이터베이스 오류", "DB 연결 상태를 확인하세요."),
    UNKNOWN("알 수 없는 오류", "상세 로그를 확인하세요.");

    private final String title;
    private final String action;
}
