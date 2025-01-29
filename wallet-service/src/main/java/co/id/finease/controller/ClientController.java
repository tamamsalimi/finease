package co.id.finease.controller;

import co.id.finease.config.ConfigProperties;
import co.id.finease.dto.ClientRequest;
import co.id.finease.dto.ClientResponse;
import co.id.finease.dto.ErrorResponse;
import co.id.finease.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ConfigProperties configProperties;
    private final ClientService clientService;

    @PostMapping
    public ResponseEntity<?> createClient(
            @RequestHeader(value = "secret_key") String secretKey,
            @RequestBody @Valid ClientRequest request) {


        if (!configProperties.getSecretKey().equals(secretKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Invalid secret key"));
        }

        try {
            ClientResponse response = clientService.createClient(request);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()));
        }

    }

    @PutMapping("/deactivate")
    public ResponseEntity<?> updateClientToInactive(
            @RequestHeader(value = "secret_key") String secretKey,
            @Valid @RequestBody ClientRequest request) {

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
            ClientResponse response = clientService.updateClientToInactive(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()));
        }
    }
}