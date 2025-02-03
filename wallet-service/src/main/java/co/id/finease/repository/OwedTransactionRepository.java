package co.id.finease.repository;

import co.id.finease.entity.Account;
import co.id.finease.entity.OwedTransaction;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OwedTransactionRepository extends JpaRepository<OwedTransaction, Long> {
    List<OwedTransaction> findByPayFromAndStatusIn(Account payFrom, List<String> statuses, Sort sort);

    @Query(value = "SELECT nextval('owed_transaction_seq')", nativeQuery = true)
    Long getNextSequenceValue();


    @Query("SELECT a.accountRef AS recipientId, a.accountName AS recipientName, SUM(o.amount) AS totalAmount " +
            "FROM OwedTransaction o " +
            "JOIN Account a ON a.accountId = o.recipient.accountId " +  // Join with the Account entity on recipientId
            "WHERE o.payFrom = :accountId " +  // Filtering by the payer's account
            "AND o.status IN ('UNPAID', 'PARTIALLY_PAID') " +  // Only consider UNPAID and PARTIALLY_PAID status
            "GROUP BY 1,2 ")
    List<Object[]> findTotalOwedGroupedByRecipient(@Param("accountId") Account accountId);

    @Query("SELECT a.accountRef AS payerId, a.accountName AS payerName, SUM(o.amount) AS totalAmount " +
            "FROM OwedTransaction o " +
            "JOIN Account a ON a.accountId = o.payFrom.accountId " +  // Join with the Account entity on payFrom accountId
            "WHERE o.recipient = :accountId " +  // Filtering by the recipient's account
            "AND o.status IN ('UNPAID', 'PARTIALLY_PAID') " +  // Only consider UNPAID and PARTIALLY_PAID status
            "GROUP BY 1,2 ")
    List<Object[]> findTotalOwedGroupedByPayer(@Param("accountId") Account accountId);

    List<OwedTransaction> findByPayFromAndRecipientAndStatusIn(Account payFrom, Account recipient, List<String> statuses, Sort sort);


}
