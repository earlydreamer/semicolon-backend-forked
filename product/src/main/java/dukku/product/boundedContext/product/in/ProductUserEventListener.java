package dukku.product.boundedContext.product.in;

import dukku.common.shared.user.event.UserJoinedEvent;
import dukku.product.boundedContext.product.entity.ProductUser;
import dukku.product.boundedContext.product.out.ProductUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductUserEventListener {

    private static final int NICKNAME_MAX_LENGTH = 50;

    private final ProductUserRepository productUserRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @KafkaListener(topics = "user.joined", groupId = "${spring.application.name}-group")
    public void handleUserJoined(String eventJson) {
        try {
            UserJoinedEvent event = objectMapper.readValue(eventJson, UserJoinedEvent.class);
            UUID userUuid = event.member().userUuid();

            if (productUserRepository.existsById(userUuid)) {
                return;
            }

            String nickname = normalizeNickname(event.member().nickname(), userUuid);
            productUserRepository.save(ProductUser.create(userUuid, nickname));
            log.info("[UserJoinedEvent] product user initialized. userUuid={}", userUuid);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Nickname conflict while syncing product user from user.joined", e);
        } catch (Exception e) {
            log.error("Failed to process user.joined event", e);
        }
    }

    private String normalizeNickname(String nickname, UUID userUuid) {
        if (nickname == null || nickname.isBlank()) {
            return fallbackNickname(userUuid);
        }

        String trimmed = nickname.trim();
        if (trimmed.length() <= NICKNAME_MAX_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, NICKNAME_MAX_LENGTH);
    }

    private String fallbackNickname(UUID userUuid) {
        return "user-" + userUuid.toString().substring(0, 8);
    }
}
