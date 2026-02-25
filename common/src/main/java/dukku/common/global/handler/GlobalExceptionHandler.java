package dukku.common.global.handler;

import dukku.common.global.exception.BaseException;
import dukku.common.global.exception.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

/**
 * 전역 예외 처리 클래스.
 * 컨트롤러에서 발생하는 예외를 공통 형식의 JSON 응답으로 변환하여 반환한다.
 *
 * 에러 응답 JSON 로깅은 GlobalResponseWrapper에서 처리됩니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 유효성 검사 실패 시 발생하는 예외 처리.
     * MethodArgumentNotValidException 또는 BindException을 처리하며,
     * 필드별 오류 정보를 수집하여 표준 에러 응답을 생성한다.
     *
     * @param ex MethodArgumentNotValidException 또는 BindException 인스턴스
     * @return HTTP 400 상태와 표준화된 에러 응답
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> handleValidationExceptions(Exception ex) {
        log.error("ValidationException: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "입력 데이터에 오류가 있습니다.", HttpStatus.BAD_REQUEST.value(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * 필수 헤더 누락 예외 처리.
     *
     * @param ex MissingRequestHeaderException 인스턴스
     * @return HTTP 400 상태와 누락 헤더 정보가 포함된 에러 응답
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(
            MissingRequestHeaderException ex
    ) {
        log.error("MissingRequestHeaderException: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "필수 요청 헤더가 누락되었습니다.",
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestPartException(
            MissingServletRequestPartException ex
    ) {
        log.error("MissingServletRequestPartException: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "필수 파일 파트가 누락되었습니다.",
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex
    ) {
        log.error("MaxUploadSizeExceededException: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase(),
                "업로드 가능한 파일 크기를 초과했습니다.",
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(errorResponse);
    }

    /**
     * BaseException 계열 예외 처리.
     * 커스텀 예외에서 제공하는 상태 코드, 에러 코드, 메시지, 상세 정보를 포함하여 응답 생성.
     *
     * @param ex BaseException 인스턴스
     * @return 표준화된 에러 응답과 HTTP 상태 코드
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        log.error("BaseException [{}]: {}", ex.getCode(), ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getStatus().value(), ex.getDetails());

        return ResponseEntity.status(ex.getStatus())
                .body(errorResponse);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        log.error("ResponseStatusException [{}]: {}", status.value(), ex.getReason(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                status.getReasonPhrase(),
                ex.getReason() == null ? "요청 처리 중 오류가 발생했습니다." : ex.getReason(),
                status.value()
        );

        return ResponseEntity.status(status)
                .body(errorResponse);
    }

    /**
     * 알 수 없는 예외 처리.
     * 내부 로깅 후, 클라이언트에는 일반화된 서버 오류 메시지(INTERNAL_SERVER_ERROR)로 응답 반환.
     *
     * @param ex Exception 인스턴스
     * @return HTTP 500 상태와 에러 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknownException(Exception ex) {
        log.error("UnhandledException: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

}
