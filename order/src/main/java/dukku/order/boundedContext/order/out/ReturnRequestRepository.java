package dukku.order.boundedContext.order.out;

import dukku.order.boundedContext.order.entity.ReturnRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 반품 요청 JPA 리포지토리.
 */
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    /**
     * 반품 요청 UUID로 단건 조회한다.
     * 반품 승인/거절 플로우에서 바로 쓰도록 주문/반품아이템/주문아이템을 함께 로딩한다.
     */
    @EntityGraph(attributePaths = {"order", "returnItems", "returnItems.orderItem"})
    Optional<ReturnRequest> findByUuid(UUID uuid);

    /**
     * 주문 UUID로 반품 요청 목록을 조회한다.
     * 환불 완료/실패 반영 시 반품아이템/주문아이템을 함께 로딩한다.
     */
    @EntityGraph(attributePaths = {"returnItems", "returnItems.orderItem"})
    List<ReturnRequest> findByOrderUuid(UUID orderUuid);

    /**
     * 판매자 UUID 기준으로 본인 상품에 접수된 반품 요청 목록을 최신순으로 조회한다.
     */
    @Query("""
            SELECT DISTINCT rr FROM ReturnRequest rr
            JOIN FETCH rr.returnItems ri
            JOIN FETCH ri.orderItem oi
            JOIN FETCH rr.order o
            WHERE oi.sellerUuid = :sellerUuid
            ORDER BY rr.createdAt DESC
            """)
    List<ReturnRequest> findAllBySellerUuid(@Param("sellerUuid") UUID sellerUuid);
}
