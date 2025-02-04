package co.id.finease.service;

import co.id.finease.config.SessionAuthenticationToken;
import co.id.finease.dto.SessionRequest;
import co.id.finease.dto.SessionResponse;
import co.id.finease.entity.Account;
import co.id.finease.entity.Session;
import co.id.finease.repository.SessionRepository;
import co.id.finease.utils.Constants;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private static final SecureRandom secureRandom = new SecureRandom();

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public SessionResponse generateSession() {
        // Generate unique API Key & Application ID
        String apiKey = generateApiKey(); // Secure API Key
        String applicationId = generateApplicationId(); // Secure Application ID
        // Create and save the new Session
        Session newSession = Session.builder()
                .apiKey(apiKey)
                .applicationId(applicationId)
                .status(Constants.STATUS_ACTIVE)
                .build();
        Session savedSession = sessionRepository.save(newSession);
        // Return successful response
        return new SessionResponse(
                HttpStatus.OK.value(),
                "Session created successfully.",
                savedSession.getApplicationId(),
                savedSession.getApiKey()
        );
    }
    // Generate a secure API Key (Long format with alphanumeric characters)
    private String generateApiKey() {
        return UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", ""); // 64-character long key
    }

    // Generate a secure Application ID (Base64 Encoded Shorter Key)
    private String generateApplicationId() {
        byte[] randomBytes = new byte[16]; // 16 bytes = 128 bits
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes); // Encoded key
    }

    @Transactional
    public SessionResponse inActiveSession(SessionRequest request) {
        return sessionRepository.findByApplicationIdAndApiKeyAndStatus(
                        request.getApplicationID(), request.getApiKey(), Constants.STATUS_ACTIVE)
                .map(session -> {
                    session.setStatus(Constants.STATUS_INACTIVE); // Set status to inactive
                    sessionRepository.save(session); // Save the updated Session
                    return new SessionResponse(
                            HttpStatus.OK.value(),
                            "Session updated to inactive successfully",
                            request.getApplicationID(),
                            request.getApiKey()
                    );
                })
                .orElseThrow(() -> new RuntimeException("No active Session found with the provided details"));
    }

    public Session findByApplicationIdAndApiKey(String applicationId, String apiKey) {
        // Use Optional to handle cases where Session is not found
        Optional<Session> SessionOptional = sessionRepository.findByApplicationIdAndApiKey(applicationId, apiKey);
        return SessionOptional.orElse(null);  // Return null if not found
    }

    public Long getSessionIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof SessionAuthenticationToken token) {
            return Long.valueOf(token.getCredentials().toString());
        }
        throw new SecurityException("Session not authenticated");
    }

    public Account getAccountIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof SessionAuthenticationToken token) {
            return (Account) token.getDetails();
        }
        throw new SecurityException("Session not authenticated");
    }

}


