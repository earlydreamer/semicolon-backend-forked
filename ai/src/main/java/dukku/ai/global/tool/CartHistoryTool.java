package dukku.ai.global.tool;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CartHistoryTool {

    @Tool(description = "이번 달 장바구니 이력이 있는 사용자 UUID 목록을 조회한다")
    public List<UUID> getCartHistoryCustomers() {
        log.info("[Tool 호출] 장바구니 이력 사용자 조회");
        // TODO: MSA 연동 - cart 모듈 API 호출
        return List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }
}
