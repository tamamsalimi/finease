package co.id.finease.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "ref_transaction_id", nullable = false)
    private String refTransactionId;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne
    @JoinColumn(name = "recipient_id")
    private Account recipientId;

    @Column(name = "transaction_type", nullable = false)
    private char transactionType; // 'D' for Debit, 'C' for Credit,'T' for Transfer, 'O' for Owed Transfer

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "created_datetime", updatable = false)
    private LocalDateTime createdDatetime;

    @PrePersist
    protected void onCreate() {
        if (createdDatetime == null) {
            createdDatetime = LocalDateTime.now(); // Set default timestamp on insert
        }
    }

}
