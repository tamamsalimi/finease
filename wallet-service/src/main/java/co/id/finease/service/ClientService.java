package co.id.finease.service;

import co.id.finease.dto.ClientRequest;
import co.id.finease.dto.ClientResponse;
import co.id.finease.entity.Client;
import co.id.finease.repository.ClientRepository;
import co.id.finease.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private static final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public ClientResponse createClient(ClientRequest request) {
        // Check if an active client with the same name exists
        clientRepository.findByClientNameAndStatus(request.getClientName(), Constants.STATUS_ACTIVE)
                .ifPresent(client -> {
                    // Throw an exception to be handled by the global handler
                    throw new IllegalArgumentException("An active client with the same name already exists.");
                });

        // Generate unique API Key & Application ID
        String apiKey = generateApiKey(); // Secure API Key
        String applicationId = generateApplicationId(); // Secure Application ID

        // Create and save the new client
        Client newClient = Client.builder()
                .clientName(request.getClientName())
                .apiKey(apiKey)
                .applicationId(applicationId)
                .status(Constants.STATUS_ACTIVE)
                .build();

        Client savedClient = clientRepository.save(newClient);

        // Return successful response
        return new ClientResponse(
                HttpStatus.OK.value(),
                "Client created successfully.",
                savedClient.getClientName(),
                savedClient.getApplicationId(),
                savedClient.getApiKey()
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
    public ClientResponse updateClientToInactive(ClientRequest request) {
        return clientRepository.findByClientNameAndApplicationIdAndApiKeyAndStatus(
                        request.getClientName(),request.getApplicationID(), request.getApiKey(), Constants.STATUS_ACTIVE)
                .map(client -> {
                    client.setStatus(Constants.STATUS_INACTIVE); // Set status to inactive
                    clientRepository.save(client); // Save the updated client
                    return new ClientResponse(
                            HttpStatus.OK.value(),
                            "Client updated to inactive successfully",
                            request.getClientName(),
                            request.getApplicationID(),
                            request.getApiKey()
                    );
                })
                .orElseThrow(() -> new RuntimeException("No active client found with the provided details"));
    }

    public Client findByApplicationIdAndApiKey(String applicationId, String apiKey) {
        // Use Optional to handle cases where client is not found
        Optional<Client> clientOptional = clientRepository.findByApplicationIdAndApiKey(applicationId, apiKey);
        return clientOptional.orElse(null);  // Return null if not found
    }

}


