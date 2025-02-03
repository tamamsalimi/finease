package co.id.finease.repository;

import co.id.finease.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountRefAndSessionId(String accountRef, Long sessionId);

    Optional<Account> findBySessionIdAndAccountName(Long sessionId, String accountName);
    @Query(value = "SELECT nextval('account_sequence')", nativeQuery = true)
    Long getNextSequenceValue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountName = :accountName AND a.sessionId = :sessionId AND a.status = 'A'")
    Optional<Account> findActiveAccountNameWithLockAndSessionId(String accountName, Long sessionId);
}

