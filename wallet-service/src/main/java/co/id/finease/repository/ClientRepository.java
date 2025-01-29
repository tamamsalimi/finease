package co.id.finease.repository;

import co.id.finease.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByClientNameAndStatus(String clientName, String status);

    Optional<Client> findByClientNameAndApplicationIdAndApiKeyAndStatus(
            String clientName, String applicationID, String apiKey, String status);

    Optional<Client> findByApplicationIdAndApiKey(String applicationId, String apiKey);
}
