package co.id.finease.repository;

import co.id.finease.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByApplicationIdAndApiKeyAndStatus(
           String applicationID, String apiKey, String status);

    Optional<Session> findByApplicationIdAndApiKey(String applicationId, String apiKey);
}
