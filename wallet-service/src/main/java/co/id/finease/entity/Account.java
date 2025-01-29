package co.id.finease.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private Integer clientId;
    
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

}