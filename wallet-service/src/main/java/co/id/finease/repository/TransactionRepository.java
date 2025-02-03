package co.id.finease.repository;

import co.id.finease.entity.Account;
import co.id.finease.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query(value = "SELECT nextval('transaction_seq')", nativeQuery = true)
    Long getNextSequenceValue();
}
