package dukku.ai.app.usecase;

import dukku.ai.entity.AiUserMemory;
import dukku.ai.out.AiUserMemoryRepository;
import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InitializeAiUserMemoryUseCase {

    private final AiUserMemoryRepository aiUserMemoryRepository;


    @Transactional
    public void execute(UUID userUuid, String nickname, String email) {
        if (aiUserMemoryRepository.existsByUserUuidAndMemoryType(userUuid, MemoryType.PROFILE)) {
            return;
        }

        AiUserMemory profileMemory = AiUserMemory.builder()
                .userUuid(userUuid)
                .memoryType(MemoryType.PROFILE)
                .subType(MemorySubType.GENERAL)
                .content("유저 프로필 초기화- " + "유저 이름:" + nickname + "유저 이메일:" + email)
                .importanceScore(1.0)
                .build();

        aiUserMemoryRepository.save(profileMemory);
    }
}