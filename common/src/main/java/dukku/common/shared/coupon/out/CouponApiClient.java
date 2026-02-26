package dukku.common.shared.coupon.out;

import dukku.common.global.auth.RequestAuthorizationHeaderResolver;
import dukku.common.shared.coupon.dto.CouponInternalResponse;
import dukku.common.shared.coupon.exception.CouponNotFoundException;
import dukku.common.shared.coupon.exception.CouponUseNotAllowedException;
import dukku.common.shared.payment.exception.AmountMismatchException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * 쿠폰 Internal API 클라이언트
 *
 * <p>
 * 쿠폰 서비스 내부 API 호출 및 HTTP 오류 코드 → 도메인 예외 변환 담당
 */
@Slf4j
@Component
public class CouponApiClient {

    private final RestClient restClient;

    public CouponApiClient(@Value("${custom.client.coupon.url:${custom.global.internalBackUrl:http://localhost:8080}}") String internalBackUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/internal/coupons")
                .build();
    }

    /**
     * 내부 API를 통한 쿠폰 정보 조회
     *
     * <p>
     * HTTP 상태 코드에 따른 도메인 예외 변환:
     * <ul>
     *   <li>404 → {@link CouponNotFoundException}</li>
     *   <li>409 → {@link CouponUseNotAllowedException}</li>
     *   <li>기타 4xx → {@link AmountMismatchException}</li>
     *   <li>5xx / 네트워크 오류 → 그대로 전파</li>
     * </ul>
     *
     * @param couponUuid 조회할 쿠폰 UUID
     * @return 쿠폰 내부 응답 DTO
     * @throws CouponNotFoundException      쿠폰이 존재하지 않을 때
     * @throws CouponUseNotAllowedException 쿠폰 사용이 불가능한 상태일 때
     * @throws AmountMismatchException      기타 4xx 오류 시
     */
    public CouponInternalResponse getCouponInfo(UUID couponUuid) {
        try {
            RestClient.RequestHeadersSpec<?> requestSpec = restClient.get()
                    .uri("/{couponUuid}", couponUuid);

            String authorization = RequestAuthorizationHeaderResolver.resolve();
            if (authorization != null) {
                requestSpec = requestSpec.header("Authorization", authorization);
            }

            CouponInternalResponse response = requestSpec.retrieve()
                    .body(CouponInternalResponse.class);

            if (response == null) {
                // 정상 응답이지만 바디가 null인 경우 (비정상 서버 응답)
                log.warn("쿠폰 조회 응답 바디가 null입니다. couponUuid={}", couponUuid);
                throw new CouponNotFoundException();
            }
            return response;
        } catch (HttpClientErrorException.NotFound e) {
            // 404: 존재하지 않는 쿠폰
            log.warn("쿠폰을 찾을 수 없습니다. couponUuid={}", couponUuid);
            throw new CouponNotFoundException();
        } catch (HttpClientErrorException.Conflict e) {
            // 409: 이미 사용됨, 만료 등 사용 불가 상태
            log.warn("사용 불가 상태의 쿠폰입니다. couponUuid={}", couponUuid);
            throw new CouponUseNotAllowedException();
        } catch (HttpClientErrorException e) {
            // 기타 4xx: 잘못된 요청 (UUID 형식 오류 등 내부 버그 가능성)
            log.warn("쿠폰 조회 중 클라이언트 오류 발생. couponUuid={}, status={}", couponUuid, e.getStatusCode().value());
            throw new AmountMismatchException(
                    "쿠폰 조회 요청이 유효하지 않습니다. status=" + e.getStatusCode().value());
        }
    }
}
