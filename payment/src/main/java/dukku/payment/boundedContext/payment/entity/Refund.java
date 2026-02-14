package dukku.payment.boundedContext.payment.entity;

import dukku.common.global.jpa.entity.BaseIdAndUUIDAndTime;
import dukku.common.shared.payment.type.RefundStatus;
import dukku.common.shared.payment.dto.PaymentRefundResponse;
import dukku.common.shared.payment.dto.RefundDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 환불 정보 엔티티
 *
 * <p>
 * 특정 {@code Payment}에 대한 환불 처리를 기록한다.
 * 총 환불 금액과 예치금으로 환불된 금액을 나누어 관리한다.
 *
 * <h3>금액 필드 설명</h3>
 * <ul>
 * <li>{@code refundAmountTotal} - 총 환불 금액 (PG 취소액 + 예치금 환불액)</li>
 * <li>{@code refundDepositTotal} - 이 중 예치금으로 복구된 금액</li>
 * </ul>
 */
@Entity
@Table(name = "refunds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class Refund extends BaseIdAndUUIDAndTime {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Payment payment;

    @Column(nullable = false, comment = "총 환불 금액")
    private Long refundAmountTotal;

    @Column(nullable = false, comment = "예치금으로 복구된 금액")
    private Long refundDepositTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, comment = "환불 상태")
    private RefundStatus refundStatus;

    @Column(comment = "환불 승인일")
    private LocalDateTime approvedAt;

    @Column(unique = true, length = 100, comment = "멱등성 키 (중복 환불 방지)")
    private String idempotencyKey;

    @Builder.Default
    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefundItem> items = new ArrayList<>();

    // === 정적 팩토리 메서드 ===

    public static Refund create(Payment payment, Long amount, Long depositAmount, String idempotencyKey) {
        return Refund.builder()
                .payment(payment)
                .refundAmountTotal(amount)
                .refundDepositTotal(depositAmount)
                .refundStatus(RefundStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();
    }

    // === 도메인 로직 ===

    public void complete() {
        if (this.refundStatus == RefundStatus.COMPLETED) {
            return;
        }
        this.refundStatus = RefundStatus.COMPLETED;
        this.approvedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.refundStatus = RefundStatus.CANCELED;
    }

    public void addRefundItem(RefundItem item) {
        this.items.add(item);
    }

    // === DTO 변환 ===

    public PaymentRefundResponse toPaymentRefundResponse(Long pgRefundAmount, String tossOrderId) {
        boolean completed = this.refundStatus == RefundStatus.COMPLETED;
        String responseCode = completed ? "REFUND_COMPLETED" : "REFUND_PENDING";
        String responseMessage = completed ? "환불이 완료되었습니다." : "환불 요청이 접수되었습니다.";

        return PaymentRefundResponse.builder()
                .success(true)
                .code(responseCode)
                .message(responseMessage)
                .data(PaymentRefundResponse.RefundData.builder()
                        .refundId(this.getUuid())
                        .paymentId(this.payment.getUuid())
                        .orderUuid(this.payment.getOrderUuid())
                        .status(this.payment.getPaymentStatus())
                        .amounts(PaymentRefundResponse.RefundAmountInfo.builder()
                                .requestedRefundAmount(this.refundAmountTotal)
                                .depositRefundAmount(this.refundDepositTotal)
                                .pgRefundAmount(pgRefundAmount)
                                .build())
                        .pg(PaymentRefundResponse.PgInfo.builder()
                                .provider("TOSS_PAYMENTS")
                                .tossOrderId(tossOrderId)
                                .cancelTransactionKey("CANCEL_" + this.getUuid()
                                        .toString().substring(0, 8))
                                .build())
                        .createdAt(this.getCreatedAt())
                        .completedAt(this.approvedAt)
                        .build())
                .build();
    }

    public RefundDto toDto() {
        return RefundDto.builder()
                .id(this.getId())
                .uuid(this.getUuid())
                .refundAmountTotal(this.refundAmountTotal)
                .refundDepositTotal(this.refundDepositTotal)
                .refundStatus(this.refundStatus)
                .approvedAt(this.approvedAt)
                .createdAt(this.getCreatedAt())
                .items(this.items.stream().map(RefundItem::toDto).toList())
                .build();
    }
}
