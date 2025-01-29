package co.id.finease.repository;

import co.id.finease.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountRef(String accountRef);

    Optional<Account> findByClientIdAndAccountName(Integer clientId, String accountName);
}

