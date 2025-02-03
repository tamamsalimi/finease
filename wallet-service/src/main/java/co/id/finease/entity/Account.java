package co.id.finease.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "account")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;
    
    @Column(unique = true, nullable = false)
    private String accountRef;
    
    @Column(nullable = false)
    private String accountName;
    
    @Column(nullable = false)
    private Long sessionId;
    
    @Column(nullable = false)
    private char status = 'I';
    
    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDatetime = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime updatedDatetime = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedDatetime = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY) // Force immediate loading
    private List<Transaction> transactions;

}