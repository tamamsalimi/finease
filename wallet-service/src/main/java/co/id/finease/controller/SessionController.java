package co.id.finease.controller;

import co.id.finease.config.ConfigProperties;
import co.id.finease.dto.SessionRequest;
import co.id.finease.dto.SessionResponse;
import co.id.finease.dto.ErrorResponse;
import co.id.finease.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/session")
@RequiredArgsConstructor
public class SessionController {

    private final ConfigProperties configProperties;
    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<?> createSession(
            @RequestHeader(value = "secret_key") String secretKey) {
        if (!configProperties.getSecretKey().equals(secretKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Invalid secret key"));
        }
        try {
            SessionResponse response = sessionService.generateSession();
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()));
        }

    }

    @PutMapping("/deactivate")
    public ResponseEntity<?> updatedSessionToInActive(
            @RequestHeader(value = "secret_key") String secretKey,
            @Valid @RequestBody SessionRequest request) {

        // Validate API key
        if (!configProperties.getSecretKey().equals(secretKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Invalid API key"));
        }

        if ((request.getApplicationID() == null || request.getApplicationID().isEmpty()) &&
                (request.getApiKey() == null || request.getApiKey().isEmpty())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Missing Request Param"));
        }

        try {
            SessionResponse response = sessionService.inActiveSession(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()));
        }
    }
}