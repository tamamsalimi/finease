package co.id.finease.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
public class OwedTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "owed_transaction_id", nullable = false)
    private String owedTransactionID;

    @ManyToOne
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account payFrom;

    @ManyToOne
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account recipient;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    private String status; // UNPAID, PARTIALLY_PAID, PAID

    @ManyToOne
    @JoinColumn(name = "transaction_id", referencedColumnName = "id")
    private Transaction transaction;


    @Column(name = "created_datetime", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_datetime")
    private LocalDateTime UpdateAt;
}
