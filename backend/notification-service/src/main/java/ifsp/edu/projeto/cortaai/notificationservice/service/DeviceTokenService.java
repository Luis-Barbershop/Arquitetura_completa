package ifsp.edu.projeto.cortaai.notificationservice.service;

import ifsp.edu.projeto.cortaai.notificationservice.model.DeviceToken;
import ifsp.edu.projeto.cortaai.notificationservice.model.PushPlatform;
import ifsp.edu.projeto.cortaai.notificationservice.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional
    public void registerToken(UUID userId, String token, PushPlatform platform) {
        String normalized = normalizeToken(token);

        DeviceToken deviceToken = deviceTokenRepository.findByToken(normalized)
                .map(existing -> {
                    existing.setUserId(userId);
                    existing.setPlatform(platform);
                    existing.setActive(true);
                    return existing;
                })
                .orElseGet(() -> DeviceToken.builder()
                        .userId(userId)
                        .token(normalized)
                        .platform(platform)
                        .active(true)
                        .build());

    deviceTokenRepository.save(Objects.requireNonNull(deviceToken));
        log.info("event=push-token-registered userId={} platform={}", userId, platform);
    }

    @Transactional
    public void deactivateToken(UUID userId, String token) {
        String normalized = normalizeToken(token);
        deviceTokenRepository.findByToken(normalized).ifPresent(existing -> {
            if (existing.getUserId().equals(userId)) {
                existing.setActive(false);
                deviceTokenRepository.save(existing);
                log.info("event=push-token-deactivated userId={}", userId);
            }
        });
    }

    @Transactional
    public void deactivateTokenByValue(String token) {
        String normalized = normalizeToken(token);
        deviceTokenRepository.findByToken(normalized).ifPresent(existing -> {
            existing.setActive(false);
            deviceTokenRepository.save(existing);
            log.info("event=push-token-deactivated-invalid tokenSuffix={}***", maskSuffix(normalized));
        });
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token de push é obrigatório");
        }
        return token.trim();
    }

    private String maskSuffix(String token) {
        if (token.length() <= 6) {
            return token;
        }
        return token.substring(token.length() - 6);
    }
}
